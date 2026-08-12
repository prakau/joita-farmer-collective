package ai.joita.biosoil.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import ai.joita.biosoil.R
import ai.joita.biosoil.domain.SoilAdvisor
import ai.joita.biosoil.model.FieldProfile
import ai.joita.biosoil.model.ParameterStatus
import ai.joita.biosoil.model.ReadingSource
import ai.joita.biosoil.model.SoilReading
import ai.joita.biosoil.model.SoilStatus
import ai.joita.biosoil.model.SoilTestRecord
import ai.joita.biosoil.report.ReportService
import ai.joita.biosoil.sensor.SensorState
import ai.joita.biosoil.sensor.UsbSoilSensorManager
import java.io.File

private val SampleReading = SoilReading(
    moisturePercent = 38.6,
    temperatureCelsius = 27.4,
    ecUsCm = 620,
    ph = 6.8,
    nitrogenMgKg = 168,
    phosphorusMgKg = 31,
    potassiumMgKg = 192,
    fertilityMgKg = 515,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TestWizard(
    onSaved: (SoilTestRecord) -> Unit,
) {
    val context = LocalContext.current
    val quickTestLabel = stringResource(R.string.quick_test_label)
    val pdfDownloadedMessage = stringResource(R.string.pdf_downloaded)
    val pdfDownloadFailedMessage = stringResource(R.string.pdf_download_failed)
    var source by remember { mutableStateOf(ReadingSource.USB) }
    var usbReading by remember { mutableStateOf<SoilReading?>(null) }
    var manualReading by remember { mutableStateOf<SoilReading?>(null) }
    var usbSamples by remember { mutableStateOf<List<SoilReading>>(emptyList()) }
    var sourceNote by remember { mutableStateOf("") }
    var savedTest by remember { mutableStateOf<SoilTestRecord?>(null) }
    var pendingDownload by remember { mutableStateOf<File?>(null) }
    var sensorState by remember { mutableStateOf<SensorState>(SensorState.NoDevice) }
    var guideVisible by remember { mutableStateOf(false) }
    val systemLanguage = ConfigurationCompat.getLocales(LocalConfiguration.current)[0]?.language ?: "en"
    val currentLanguage = AppCompatDelegate.getApplicationLocales()[0]?.language
        ?: systemLanguage
    val sensorManager = remember {
        UsbSoilSensorManager(context) {
            sensorState = it
            if (it is SensorState.Complete) {
                val nextSamples = (usbSamples + it.reading).takeLast(5)
                usbSamples = nextSamples
                usbReading = averageReadings(nextSamples)
                savedTest = null
            }
        }
    }
    DisposableEffect(Unit) { onDispose(sensorManager::close) }
    LaunchedEffect(sensorManager) { sensorManager.connect() }
    val downloadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri: Uri? ->
        val sourceFile = pendingDownload
        val saved = uri != null && sourceFile != null && runCatching {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                sourceFile.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Could not open the selected file")
        }.isSuccess
        Toast.makeText(
            context,
            if (saved) pdfDownloadedMessage else pdfDownloadFailedMessage,
            Toast.LENGTH_LONG,
        ).show()
        pendingDownload = null
    }

    val reading = when (source) {
        ReadingSource.USB -> usbReading
        ReadingSource.MANUAL -> manualReading
        ReadingSource.SAMPLE -> SampleReading
    }

    fun saveReading(share: Boolean) {
        val actualReading = reading ?: return
        val test = savedTest?.takeIf { it.source == source && it.reading == actualReading } ?: run {
            val advisory = SoilAdvisor.assess(actualReading)
            SoilTestRecord(
                fieldId = null,
                fieldLabel = quickTestLabel,
                crop = "",
                source = source,
                reading = actualReading,
                score = advisory.score,
                status = advisory.status,
                sourceNote = sourceNote,
            ).also {
                onSaved(it)
                savedTest = it
            }
        }
        if (share) {
            ReportService.sharePdf(context, ReportService.createPdf(context, test), test)
        }
    }

    fun downloadReading() {
        val actualReading = reading ?: return
        val advisory = SoilAdvisor.assess(actualReading)
        val test = savedTest?.takeIf { it.source == source && it.reading == actualReading } ?: SoilTestRecord(
            fieldId = null,
            fieldLabel = quickTestLabel,
            crop = "",
            source = source,
            reading = actualReading,
            score = advisory.score,
            status = advisory.status,
            sourceNote = sourceNote,
        ).also {
            onSaved(it)
            savedTest = it
        }
        pendingDownload = ReportService.createPdf(context, test)
        downloadLauncher.launch("JOITA-Soil-Saathi-${test.id.take(8)}.pdf")
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.soil_detector), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.publisher), style = MaterialTheme.typography.labelSmall, color = InkMuted)
                    }
                },
                actions = {
                    IconButton(onClick = { guideVisible = true }) {
                        Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = stringResource(R.string.sensor_guide))
                    }
                    TextButton(
                        onClick = {
                            val language = if (currentLanguage == "hi") "en" else "hi"
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
                        },
                    ) {
                        Text(if (currentLanguage == "hi") "EN" else "हिंदी", fontWeight = FontWeight.Bold)
                    }
                },
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth().imePadding().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                savedTest?.let {
                    Text(stringResource(R.string.test_saved), color = JoitaGreenDark, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { saveReading(share = false) },
                        enabled = reading != null,
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) { Text(stringResource(R.string.save_soil_test)) }
                    OutlinedButton(
                        onClick = ::downloadReading,
                        enabled = reading != null,
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.download_pdf))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { saveReading(share = true) },
                    enabled = reading != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.share_email_whatsapp))
                }
            }
        },
    ) { padding ->
        QuickReadingScreen(
            source = source,
            onSource = {
                source = it
                savedTest = null
                if (it == ReadingSource.USB) sensorManager.connect()
            },
            reading = reading,
            sourceNote = sourceNote,
            onSourceNote = {
                sourceNote = it
                savedTest = null
            },
            onManualReading = {
                manualReading = it
                savedTest = null
            },
            sensorState = sensorState,
            sampleCount = if (source == ReadingSource.USB) usbSamples.size else 0,
            onSensorAction = {
                usbSamples = emptyList()
                usbReading = null
                when (val state = sensorState) {
                    is SensorState.PermissionRequired -> sensorManager.requestPermission(state.device)
                    else -> sensorManager.connect()
                }
            },
            padding = padding,
        )
    }
    if (guideVisible) SensorGuideDialog(onDismiss = { guideVisible = false })
}

@Composable
private fun QuickReadingScreen(
    source: ReadingSource,
    onSource: (ReadingSource) -> Unit,
    reading: SoilReading?,
    sourceNote: String,
    onSourceNote: (String) -> Unit,
    onManualReading: (SoilReading) -> Unit,
    sensorState: SensorState,
    sampleCount: Int,
    onSensorAction: () -> Unit,
    padding: PaddingValues,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.quick_test_body), color = InkMuted)
        SourceBadge(source)
        when (source) {
            ReadingSource.MANUAL -> {
                TextButton(onClick = { onSource(ReadingSource.USB) }) { Text(stringResource(R.string.use_usb_sensor)) }
                ManualReadingForm(sourceNote, onSourceNote, onManualReading)
            }
            ReadingSource.SAMPLE -> MetricGrid(SampleReading)
            ReadingSource.USB -> {
                val stateIsError = sensorState is SensorState.Error || sensorState is SensorState.UnsupportedDevice
                Text(sensorStateText(sensorState), color = if (stateIsError) MaterialTheme.colorScheme.error else InkMuted)
                if (sensorState is SensorState.Error && sensorState.message.isNotBlank()) {
                    Text(sensorState.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                if (reading == null || stateIsError || sensorState is SensorState.PermissionRequired) {
                    Button(onClick = onSensorAction, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text(stringResource(if (sensorState is SensorState.PermissionRequired) R.string.allow_usb_access else R.string.try_again))
                    }
                }
                MetricGrid(reading)
                if (reading == null) {
                    Text(stringResource(R.string.values_waiting), color = InkMuted)
                } else {
                    Text(
                        stringResource(R.string.live_samples_averaged, sampleCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = JoitaGreenDark,
                    )
                    LiveFarmerAdvice(reading)
                    OutlinedButton(onClick = onSensorAction, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.new_spot_reset))
                    }
                }
                TextButton(onClick = { onSource(ReadingSource.MANUAL) }) { Text(stringResource(R.string.enter_manually)) }
            }
        }
    }
}

@Composable
private fun SourceStep(
    fields: List<FieldProfile>,
    selected: FieldProfile?,
    onField: (FieldProfile) -> Unit,
    source: ReadingSource?,
    onSource: (ReadingSource) -> Unit,
    padding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(stringResource(R.string.select_field), style = MaterialTheme.typography.titleLarge) }
        if (fields.isEmpty()) {
            item { Text(stringResource(R.string.no_fields_body), color = InkMuted) }
        } else {
            items(fields.size) { index ->
                val field = fields[index]
                Card(
                    modifier = Modifier.fillMaxWidth().selectable(selected == field, onClick = { onField(field) }),
                    colors = CardDefaults.cardColors(containerColor = if (selected == field) LeafLight else MaterialTheme.colorScheme.surface),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected == field, onClick = { onField(field) })
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(field.fieldName, fontWeight = FontWeight.SemiBold)
                            Text("${field.farmerName} • ${field.village} • ${field.crop}", color = InkMuted)
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.select_source), style = MaterialTheme.typography.titleLarge)
        }
        item { SourceOption(ReadingSource.USB, R.string.connect_usb_sensor, R.string.connect_usb_sensor_body, source, onSource, Icons.Rounded.Usb) }
        item { SourceOption(ReadingSource.MANUAL, R.string.enter_manually, R.string.enter_manually_body, source, onSource, Icons.Rounded.CheckCircle) }
        item { SourceOption(ReadingSource.SAMPLE, R.string.try_sample_reading, R.string.try_sample_reading_body, source, onSource, Icons.Rounded.CheckCircle) }
    }
}

@Composable
private fun SourceOption(
    option: ReadingSource,
    title: Int,
    body: Int,
    selected: ReadingSource?,
    onSelect: (ReadingSource) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Card(
        modifier = Modifier.fillMaxWidth().selectable(selected == option, onClick = { onSelect(option) }),
        colors = CardDefaults.cardColors(containerColor = if (selected == option) LeafLight else MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = JoitaGreen, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(stringResource(title), fontWeight = FontWeight.SemiBold)
                Text(stringResource(body), color = InkMuted)
            }
            RadioButton(selected == option, onClick = { onSelect(option) })
        }
    }
}

@Composable
private fun ReadingStep(
    source: ReadingSource,
    reading: SoilReading?,
    sourceNote: String,
    onSourceNote: (String) -> Unit,
    onReading: (SoilReading) -> Unit,
    sensorState: SensorState,
    onSensorAction: () -> Unit,
    padding: PaddingValues,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SourceBadge(source)
        when (source) {
            ReadingSource.MANUAL -> ManualReadingForm(sourceNote, onSourceNote, onReading)
            ReadingSource.SAMPLE -> {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE2A8))) {
                    Text(stringResource(R.string.sample_warning), modifier = Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold)
                }
                MetricGrid(SampleReading)
            }
            ReadingSource.USB -> {
                Text(sensorStateText(sensorState), color = if (sensorState is SensorState.Error) MaterialTheme.colorScheme.error else InkMuted)
                Button(onClick = onSensorAction, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(
                        stringResource(
                            if (sensorState is SensorState.PermissionRequired) R.string.allow_usb_access else R.string.take_reading,
                        ),
                    )
                }
                if (reading == null) {
                    Text(stringResource(R.string.values_waiting), color = InkMuted)
                } else MetricGrid(reading)
            }
        }
    }
}

@Composable
private fun ManualReadingForm(sourceNote: String, onSourceNote: (String) -> Unit, onReading: (SoilReading) -> Unit) {
    var moisture by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }
    var ec by remember { mutableStateOf("") }
    var ph by remember { mutableStateOf("") }
    var nitrogen by remember { mutableStateOf("") }
    var phosphorus by remember { mutableStateOf("") }
    var potassium by remember { mutableStateOf("") }
    var fertility by remember { mutableStateOf("") }

    LaunchedEffect(moisture, temperature, ec, ph, nitrogen, phosphorus, potassium, fertility) {
        val values = listOf(
            moisture.toDoubleOrNull(), temperature.toDoubleOrNull(), ec.toIntOrNull(), ph.toDoubleOrNull(),
            nitrogen.toIntOrNull(), phosphorus.toIntOrNull(), potassium.toIntOrNull(), fertility.toIntOrNull(),
        )
        if (values.all { it != null }) {
            onReading(
                SoilReading(
                    moisture.toDouble(), temperature.toDouble(), ec.toInt(), ph.toDouble(),
                    nitrogen.toInt(), phosphorus.toInt(), potassium.toInt(), fertility.toInt(),
                ),
            )
        }
    }

    NumberField(moisture, { moisture = it }, R.string.metric_moisture, R.string.unit_percent)
    NumberField(temperature, { temperature = it }, R.string.metric_temperature, R.string.unit_celsius)
    NumberField(ec, { ec = it }, R.string.metric_ec, R.string.unit_ec)
    NumberField(ph, { ph = it }, R.string.metric_ph, null)
    NumberField(nitrogen, { nitrogen = it }, R.string.metric_nitrogen, R.string.unit_mgkg)
    NumberField(phosphorus, { phosphorus = it }, R.string.metric_phosphorus, R.string.unit_mgkg)
    NumberField(potassium, { potassium = it }, R.string.metric_potassium, R.string.unit_mgkg)
    NumberField(fertility, { fertility = it }, R.string.metric_fertility, R.string.unit_mgkg)
    OutlinedTextField(
        value = sourceNote,
        onValueChange = onSourceNote,
        label = { Text(stringResource(R.string.manual_source_note)) },
        placeholder = { Text(stringResource(R.string.manual_source_hint)) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NumberField(value: String, onValue: (String) -> Unit, label: Int, unit: Int?) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(stringResource(label)) },
        suffix = unit?.let { { Text(stringResource(it)) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun EvidenceStep(
    location: Location?,
    onLocation: (Location) -> Unit,
    photoUri: String?,
    onPhoto: (String) -> Unit,
    padding: PaddingValues,
) {
    val context = LocalContext.current
    var pendingPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingPhotoUri?.toString()?.let(onPhoto)
    }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            captureLocation(context, onLocation)
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.capture_evidence), style = MaterialTheme.typography.headlineMedium)
        Card {
            Column(Modifier.padding(16.dp)) {
                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = InfoBlue)
                Text(stringResource(R.string.capture_location), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.location_permission_body), color = InkMuted)
                location?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("${"%.5f".format(it.latitude)}, ${"%.5f".format(it.longitude)}")
                    Text(stringResource(R.string.accuracy_format, it.accuracy), color = InkMuted)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        ) captureLocation(context, onLocation)
                        else locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(stringResource(R.string.capture_location)) }
            }
        }
        Card {
            Column(Modifier.padding(16.dp)) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = JoitaGreen)
                Text(stringResource(R.string.capture_photo), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.photo_permission_body), color = InkMuted)
                if (photoUri != null) Text(stringResource(R.string.photo_captured), color = JoitaGreenDark, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        val directory = File(context.filesDir, "photos").apply { mkdirs() }
                        val file = File(directory, "soil-${System.currentTimeMillis()}.jpg")
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                        pendingPhotoUri = uri
                        cameraLauncher.launch(uri)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(stringResource(R.string.capture_photo)) }
            }
        }
        Text(stringResource(R.string.evidence_optional), color = InkMuted)
    }
}

@Composable
private fun ReviewStep(
    field: FieldProfile?,
    source: ReadingSource?,
    reading: SoilReading?,
    sourceNote: String,
    location: Location?,
    photoUri: String?,
    padding: PaddingValues,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.review_test), style = MaterialTheme.typography.headlineMedium)
        if (source != null) SourceBadge(source)
        Card {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.field_context), style = MaterialTheme.typography.titleLarge)
                Text(field?.let { "${it.farmerName} — ${it.fieldName}" } ?: "—")
                Text(field?.let { "${it.village}, ${it.district}, ${it.state} • ${it.crop}" } ?: "—", color = InkMuted)
                if (sourceNote.isNotBlank()) Text(sourceNote, color = InkMuted)
            }
        }
        Card {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.observed_values), style = MaterialTheme.typography.titleLarge)
                if (reading != null) MetricGrid(reading) else Text("—")
            }
        }
        Card {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.missing_evidence), style = MaterialTheme.typography.titleLarge)
                if (location == null) Text("• ${stringResource(R.string.location_not_captured)}")
                if (photoUri == null) Text("• ${stringResource(R.string.photo_not_captured)}")
                if (location != null && photoUri != null) Text(stringResource(R.string.evidence_complete), color = JoitaGreenDark)
            }
        }
        Text(stringResource(R.string.advisory_disclaimer), color = SoilBrown, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ResultScreen(test: SoilTestRecord, onBack: () -> Unit) {
    val context = LocalContext.current
    val advisory = remember(test) { SoilAdvisor.assess(test.reading) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.soil_result)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back_action))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = LeafLight), shape = MaterialTheme.shapes.extraLarge) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.advisory_label), color = JoitaGreenDark)
                        Text("${test.score}/100", style = MaterialTheme.typography.headlineMedium, color = JoitaGreenDark)
                        Text(statusLabel(test.status), style = MaterialTheme.typography.titleLarge, color = JoitaGreenDark)
                        Spacer(Modifier.height(8.dp))
                        SourceBadge(test.source)
                    }
                }
            }
            item {
                Text(stringResource(R.string.observed_values), style = MaterialTheme.typography.titleLarge)
                MetricGrid(test.reading)
            }
            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.what_this_means), style = MaterialTheme.typography.titleLarge)
                        advisory.assessments.forEach { assessment ->
                            val status = when (assessment.status) {
                                ParameterStatus.GOOD -> stringResource(R.string.status_good)
                                ParameterStatus.LOW -> stringResource(R.string.status_low)
                                ParameterStatus.HIGH -> stringResource(R.string.status_high)
                            }
                            Text("${assessmentLabel(assessment.key)}: ${assessment.value} — $status")
                        }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE2A8))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.next_field_action), style = MaterialTheme.typography.titleLarge)
                        advisory.immediateActions.forEach { Text("• ${adviceLabel(it)}") }
                        Text("• ${stringResource(R.string.advice_retest)}")
                        Text("• ${stringResource(R.string.advice_lab)}")
                    }
                }
            }
            item {
                Text(stringResource(R.string.advisory_disclaimer), color = SoilBrown, fontWeight = FontWeight.SemiBold)
            }
            item {
                Button(
                    onClick = { ReportService.sharePdf(context, ReportService.createPdf(context, test), test) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.share_report))
                }
            }
            item {
                OutlinedButton(
                    onClick = { ReportService.sharePdf(context, ReportService.createPdf(context, test), test) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.save_pdf))
                }
            }
        }
    }
}

@Composable
private fun MetricGrid(reading: SoilReading?) {
    val assessmentByKey = remember(reading) {
        reading?.let { SoilAdvisor.assess(it).assessments.associateBy { assessment -> assessment.key } }.orEmpty()
    }
    val metrics = listOf(
        MetricDisplay("temperature", stringResource(R.string.metric_temperature), reading?.let { "${it.temperatureCelsius} °C" }, Color(0xFFFFE5E0)),
        MetricDisplay("moisture", stringResource(R.string.metric_moisture), reading?.let { "${it.moisturePercent} %" }, Color(0xFFDDF6F6)),
        MetricDisplay("ec", stringResource(R.string.metric_ec), reading?.let { "${it.ecUsCm} µS/cm" }, Color(0xFFE2ECFF)),
        MetricDisplay("ph", stringResource(R.string.metric_ph), reading?.ph?.toString(), Color(0xFFFFF0C9)),
        MetricDisplay("nitrogen", stringResource(R.string.metric_nitrogen), reading?.let { "${it.nitrogenMgKg} mg/kg" }, Color(0xFFDCF5DF)),
        MetricDisplay("phosphorus", stringResource(R.string.metric_phosphorus), reading?.let { "${it.phosphorusMgKg} mg/kg" }, Color(0xFFFFE0EA)),
        MetricDisplay("potassium", stringResource(R.string.metric_potassium), reading?.let { "${it.potassiumMgKg} mg/kg" }, Color(0xFFEDE1FF)),
        MetricDisplay("fertility", stringResource(R.string.metric_fertility), reading?.let { "${it.fertilityMgKg} mg/kg" }, Color(0xFFFFE6D8)),
    )
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 360.dp) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            metrics.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { metric ->
                        val assessment = assessmentByKey[metric.key]
                        Card(colors = CardDefaults.cardColors(containerColor = metric.background), modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(metric.label, style = MaterialTheme.typography.bodySmall, color = InkMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Spacer(Modifier.height(8.dp))
                                Text(metric.value ?: "—", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                if (assessment != null) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        parameterStatusLabel(assessment.status),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = parameterStatusColor(assessment.status),
                                    )
                                }
                            }
                        }
                    }
                    if (row.size < columns) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private data class MetricDisplay(
    val key: String,
    val label: String,
    val value: String?,
    val background: Color,
)

@Composable
private fun LiveFarmerAdvice(reading: SoilReading) {
    val advisory = remember(reading) { SoilAdvisor.assess(reading) }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (reading.moisturePercent < 15.0) Color(0xFFFFE2A8) else LeafLight,
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.farmer_advice_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.quick_indicator_format, advisory.score, statusLabel(advisory.status)),
                color = JoitaGreenDark,
                fontWeight = FontWeight.SemiBold,
            )
            advisory.immediateActions.forEach { action ->
                Text("• ${adviceLabel(action)}", style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider(color = JoitaGreen.copy(alpha = 0.2f))
            Text(stringResource(R.string.advice_retest_points), style = MaterialTheme.typography.bodySmall, color = InkMuted)
            Text(stringResource(R.string.sensor_advice_limit), style = MaterialTheme.typography.bodySmall, color = SoilBrown, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SensorGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sensor_guide)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GuideSection(
                    R.string.guide_prepare_title,
                    listOf(R.string.guide_prepare_moist, R.string.guide_prepare_depth, R.string.guide_prepare_debris),
                )
                GuideSection(
                    R.string.guide_measure_title,
                    listOf(R.string.guide_measure_insert, R.string.guide_measure_wait, R.string.guide_measure_repeat),
                )
                GuideSection(
                    R.string.guide_understand_title,
                    listOf(R.string.guide_understand_ec, R.string.guide_understand_npk, R.string.guide_understand_fertility),
                )
                GuideSection(
                    R.string.guide_after_title,
                    listOf(R.string.guide_after_clean, R.string.guide_after_protect),
                )
                Text(stringResource(R.string.sensor_advice_limit), color = SoilBrown, fontWeight = FontWeight.SemiBold)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}

@Composable
private fun GuideSection(title: Int, items: List<Int>) {
    Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = JoitaGreenDark, fontWeight = FontWeight.Bold)
    items.forEach { Text("• ${stringResource(it)}") }
}

private fun averageReadings(readings: List<SoilReading>): SoilReading? {
    if (readings.isEmpty()) return null
    return SoilReading(
        moisturePercent = readings.map { it.moisturePercent }.average().roundOneDecimal(),
        temperatureCelsius = readings.map { it.temperatureCelsius }.average().roundOneDecimal(),
        ecUsCm = readings.map { it.ecUsCm }.average().toInt(),
        ph = readings.map { it.ph }.average().roundOneDecimal(),
        nitrogenMgKg = readings.map { it.nitrogenMgKg }.average().toInt(),
        phosphorusMgKg = readings.map { it.phosphorusMgKg }.average().toInt(),
        potassiumMgKg = readings.map { it.potassiumMgKg }.average().toInt(),
        fertilityMgKg = readings.map { it.fertilityMgKg }.average().toInt(),
    )
}

private fun Double.roundOneDecimal(): Double = kotlin.math.round(this * 10.0) / 10.0

@Composable
private fun parameterStatusLabel(status: ParameterStatus): String = stringResource(
    when (status) {
        ParameterStatus.GOOD -> R.string.status_good
        ParameterStatus.LOW -> R.string.status_low
        ParameterStatus.HIGH -> R.string.status_high
    },
)

private fun parameterStatusColor(status: ParameterStatus): Color = when (status) {
    ParameterStatus.GOOD -> JoitaGreenDark
    ParameterStatus.LOW -> InfoBlue
    ParameterStatus.HIGH -> Color(0xFFB3261E)
}

@Composable
private fun stepTitle(step: Int) = stringResource(
    when (step) {
        0 -> R.string.select_source
        1 -> R.string.take_reading
        2 -> R.string.capture_evidence
        else -> R.string.review_test
    },
)

@Composable
private fun sensorStateText(state: SensorState) = when (state) {
    SensorState.Unsupported -> stringResource(R.string.usb_unsupported)
    SensorState.NoDevice -> stringResource(R.string.usb_no_device)
    is SensorState.UnsupportedDevice -> stringResource(R.string.usb_unsupported_device, state.details)
    is SensorState.PermissionRequired -> stringResource(R.string.usb_permission_needed)
    SensorState.Connecting -> stringResource(R.string.usb_connecting)
    SensorState.Connected -> stringResource(R.string.sensor_connected)
    SensorState.Reading -> stringResource(R.string.usb_reading)
    is SensorState.Complete -> stringResource(R.string.reading_complete)
    is SensorState.Error -> stringResource(R.string.usb_error)
}

@Composable
private fun statusLabel(status: SoilStatus) = stringResource(
    when (status) {
        SoilStatus.GOOD -> R.string.status_good
        SoilStatus.NEEDS_ATTENTION -> R.string.status_attention
        SoilStatus.URGENT -> R.string.status_urgent
    },
)

@Composable
private fun assessmentLabel(key: String) = stringResource(
    when (key) {
        "moisture" -> R.string.metric_moisture
        "temperature" -> R.string.metric_temperature
        "ec" -> R.string.metric_ec
        "ph" -> R.string.metric_ph
        "nitrogen" -> R.string.metric_nitrogen
        "phosphorus" -> R.string.metric_phosphorus
        "potassium" -> R.string.metric_potassium
        else -> R.string.metric_fertility
    },
)

@Composable
private fun adviceLabel(key: String) = stringResource(
    when (key) {
        "prepare_moist_soil" -> R.string.advice_prepare_moist_soil
        "irrigate" -> R.string.advice_irrigate
        "improve_drainage" -> R.string.advice_drainage
        "acidic_ph" -> R.string.advice_acidic_ph
        "alkaline_ph" -> R.string.advice_alkaline_ph
        "high_salinity" -> R.string.advice_high_salinity
        "low_nutrients" -> R.string.advice_low_nutrients
        "high_nutrients" -> R.string.advice_high_nutrients
        else -> R.string.advice_maintain
    },
)

@Suppress("MissingPermission")
private fun captureLocation(context: Context, onLocation: (Location) -> Unit) {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
    providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull(Location::getTime)
        ?.let(onLocation)
    val provider = providers.firstOrNull() ?: return
    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            onLocation(location)
            manager.removeUpdates(this)
        }
        @Deprecated("Deprecated in Android")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
    }
    manager.requestSingleUpdate(provider, listener, null)
}
