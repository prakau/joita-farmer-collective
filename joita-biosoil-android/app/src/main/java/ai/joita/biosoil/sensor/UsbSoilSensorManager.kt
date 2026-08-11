package ai.joita.biosoil.sensor

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import ai.joita.biosoil.model.SoilReading
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.Ch34xSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.io.IOException
import java.util.Locale

sealed interface SensorState {
    data object Unsupported : SensorState
    data object NoDevice : SensorState
    data class UnsupportedDevice(val details: String) : SensorState
    data class PermissionRequired(val device: UsbDevice) : SensorState
    data object Connecting : SensorState
    data object Connected : SensorState
    data object Reading : SensorState
    data class Complete(val reading: SoilReading) : SensorState
    data class Error(val message: String) : SensorState
}

class UsbSoilSensorManager(
    context: Context,
    private val onState: (SensorState) -> Unit,
) : SerialInputOutputManager.Listener {
    private val appContext = context.applicationContext
    private val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val frameBuffer = SoilProbeFrameBuffer()
    private var port: UsbSerialPort? = null
    private var connection: UsbDeviceConnection? = null
    private var ioManager: SerialInputOutputManager? = null
    private var nonce = 0
    private var permissionRequestDeviceId: Int? = null

    private val permissionAction = "${appContext.packageName}.USB_PERMISSION"
    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != permissionAction) return
            val device = IntentCompatUsb.device(intent)
            permissionRequestDeviceId = null
            if (device != null && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                connect()
            } else {
                emit(SensorState.Error("USB access was not granted${device?.let { " for ${describe(it)}" }.orEmpty()}"))
            }
        }
    }
    private val attachReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> connect()
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    permissionRequestDeviceId = null
                    closePort()
                    connect()
                }
            }
        }
    }

    private val poll = object : Runnable {
        override fun run() {
            val activePort = port ?: return
            try {
                activePort.write(SoilProbeProtocol.pollCommand((nonce++ and 0xFF).toByte()), 1_000)
                emit(SensorState.Reading)
                mainHandler.postDelayed(this, 1_000)
            } catch (error: Exception) {
                emit(SensorState.Error(error.message ?: "Sensor communication stopped"))
                closePort()
            }
        }
    }

    init {
        ContextCompat.registerReceiver(
            appContext,
            permissionReceiver,
            IntentFilter(permissionAction),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        ContextCompat.registerReceiver(
            appContext,
            attachReceiver,
            IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            },
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    @Synchronized
    fun connect() {
        if (!appContext.packageManager.hasSystemFeature("android.hardware.usb.host")) {
            emit(SensorState.Unsupported)
            return
        }
        closePort()
        val attachedDevices = usbManager.deviceList.values.toList()
        if (attachedDevices.isEmpty()) {
            emit(SensorState.NoDevice)
            return
        }
        val drivers = findDrivers(attachedDevices)
        if (drivers.isEmpty()) {
            emit(SensorState.UnsupportedDevice(attachedDevices.joinToString { describe(it) }))
            return
        }
        val preferred = drivers.first()
        if (!usbManager.hasPermission(preferred.device)) {
            emit(SensorState.PermissionRequired(preferred.device))
            requestPermission(preferred.device)
            return
        }
        val errors = mutableListOf<String>()
        drivers.filter { usbManager.hasPermission(it.device) }.forEach { driver ->
            emit(SensorState.Connecting)
            runCatching { open(driver) }
                .onSuccess { return }
                .onFailure { errors += "${describe(driver.device)}: ${it.message ?: it.javaClass.simpleName}" }
        }
        closePort()
        emit(SensorState.Error(errors.joinToString("; ").ifBlank { "Could not open a supported USB sensor" }))
    }

    @Synchronized
    fun requestPermission(device: UsbDevice) {
        if (usbManager.hasPermission(device)) {
            connect()
            return
        }
        if (permissionRequestDeviceId == device.deviceId) return
        permissionRequestDeviceId = device.deviceId
        val intent = PendingIntent.getBroadcast(
            appContext,
            device.deviceId,
            Intent(permissionAction).setPackage(appContext.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        usbManager.requestPermission(device, intent)
    }

    override fun onNewData(data: ByteArray) {
        frameBuffer.append(data).lastOrNull()?.let { emit(SensorState.Complete(it)) }
    }

    override fun onRunError(error: Exception) {
        emit(SensorState.Error(error.message ?: "Sensor disconnected"))
        closePort()
    }

    fun close() {
        closePort()
        runCatching { appContext.unregisterReceiver(permissionReceiver) }
        runCatching { appContext.unregisterReceiver(attachReceiver) }
    }

    private fun findDrivers(devices: List<UsbDevice>): List<UsbSerialDriver> {
        val knownProber = UsbSerialProber(ProbeTable().apply {
            SoilProbeUsbCatalog.productIds.forEach { productId ->
                addProduct(SoilProbeUsbCatalog.vendorId, productId, Ch34xSerialDriver::class.java)
            }
        })
        val defaultProber = UsbSerialProber.getDefaultProber()
        return devices
            .sortedWith(
                compareByDescending<UsbDevice> { SoilProbeUsbCatalog.isKnown(it.vendorId, it.productId) }
                    .thenBy(UsbDevice::getDeviceId),
            )
            .flatMap { device ->
                buildList {
                    knownProber.probeDevice(device)?.let(::add)
                    defaultProber.probeDevice(device)?.let(::add)
                    if (device.hasCdcInterface()) add(CdcAcmSerialDriver(device))
                }
            }
            .distinctBy { "${it.device.deviceId}:${it.javaClass.name}" }
    }

    private fun open(driver: UsbSerialDriver) {
        var openedConnection: UsbDeviceConnection? = null
        var openedPort: UsbSerialPort? = null
        try {
            openedConnection = usbManager.openDevice(driver.device)
                ?: throw IOException("Could not open the USB sensor")
            openedPort = driver.ports.firstOrNull()
                ?: throw IOException("The USB sensor has no serial port")
            openedPort.open(openedConnection)
            openedPort.setParameters(
                9_600,
                8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE,
            )
            connection = openedConnection
            port = openedPort
            ioManager = SerialInputOutputManager(openedPort, this).also { it.start() }
            emit(SensorState.Connected)
            mainHandler.post(poll)
        } catch (error: Exception) {
            runCatching { openedPort?.close() }
            runCatching { openedConnection?.close() }
            throw error
        }
    }

    private fun emit(state: SensorState) = mainHandler.post { onState(state) }

    private fun closePort() {
        mainHandler.removeCallbacks(poll)
        ioManager?.stop()
        ioManager = null
        runCatching { port?.close() }
        runCatching { connection?.close() }
        port = null
        connection = null
    }

    private fun UsbDevice.hasCdcInterface(): Boolean =
        (0 until interfaceCount).any { index ->
            getInterface(index).interfaceClass in setOf(UsbConstants.USB_CLASS_COMM, UsbConstants.USB_CLASS_CDC_DATA)
        }

    private fun describe(device: UsbDevice): String = String.format(
        Locale.US,
        "VID 0x%04X / PID 0x%04X",
        device.vendorId,
        device.productId,
    )
}

internal object SoilProbeUsbCatalog {
    const val vendorId = 6_790
    val productIds = setOf(29_987, 21_795, 21_778)

    fun isKnown(vendorId: Int, productId: Int): Boolean =
        vendorId == this.vendorId && productId in productIds
}

private object IntentCompatUsb {
    @Suppress("DEPRECATION")
    fun device(intent: Intent): UsbDevice? =
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
}
