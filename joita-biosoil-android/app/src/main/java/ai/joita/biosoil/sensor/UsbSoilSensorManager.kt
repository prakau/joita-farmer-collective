package ai.joita.biosoil.sensor

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import ai.joita.biosoil.model.SoilReading
import com.hoho.android.usbserial.driver.Ch34xSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.io.IOException

sealed interface SensorState {
    data object Unsupported : SensorState
    data object NoDevice : SensorState
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

    private val permissionAction = "${appContext.packageName}.USB_PERMISSION"
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                permissionAction -> {
                    val device = IntentCompatUsb.device(intent)
                    if (device != null && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        connect()
                    } else {
                        emit(SensorState.Error("USB access was not granted"))
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    closePort()
                    emit(SensorState.NoDevice)
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> connect()
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
            receiver,
            IntentFilter().apply {
                addAction(permissionAction)
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    fun connect() {
        if (!appContext.packageManager.hasSystemFeature("android.hardware.usb.host")) {
            emit(SensorState.Unsupported)
            return
        }
        closePort()
        val driver = findDriver()
        if (driver == null) {
            emit(SensorState.NoDevice)
            return
        }
        if (!usbManager.hasPermission(driver.device)) {
            emit(SensorState.PermissionRequired(driver.device))
            return
        }
        emit(SensorState.Connecting)
        try {
            val openedConnection = usbManager.openDevice(driver.device)
                ?: throw IOException("Could not open the USB sensor")
            val openedPort = driver.ports.firstOrNull()
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
            closePort()
            emit(SensorState.Error(error.message ?: "Could not connect to the sensor"))
        }
    }

    fun requestPermission(device: UsbDevice) {
        val intent = PendingIntent.getBroadcast(
            appContext,
            0,
            Intent(permissionAction).setPackage(appContext.packageName),
            PendingIntent.FLAG_IMMUTABLE,
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
        runCatching { appContext.unregisterReceiver(receiver) }
    }

    private fun findDriver() = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager).firstOrNull()
        ?: UsbSerialProber(ProbeTable().apply {
            addProduct(6_790, 29_987, Ch34xSerialDriver::class.java)
            addProduct(6_790, 21_795, Ch34xSerialDriver::class.java)
            addProduct(6_790, 21_778, Ch34xSerialDriver::class.java)
        }).findAllDrivers(usbManager).firstOrNull()

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
