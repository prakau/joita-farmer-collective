package ai.joita.biosoil.collective

import android.content.Intent
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.app.DatePickerDialog
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

internal val Forest = Color(0xFF164B3B)
internal val Moss = Color(0xFFE8F0E9)
internal val Paper = Color(0xFFF5F6F0)
internal val Amber = Color(0xFF986026)
internal val Muted = Color(0xFF65736D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectiveApp() {
    val context = LocalContext.current
    val repo = remember { CollectiveRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var data by remember { mutableStateOf(CollectiveSnapshot()) }
    var loaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var farmerId by rememberSaveable { mutableLongStateOf(0) }
    var fieldId by rememberSaveable { mutableLongStateOf(0) }
    var form by rememberSaveable { mutableStateOf("") }
    var editId by rememberSaveable { mutableLongStateOf(0) }
    var reportFarmerId by rememberSaveable { mutableLongStateOf(0) }
    var exportBusy by remember { mutableStateOf(false) }
    var reportPath by rememberSaveable { mutableStateOf("") }

    suspend fun refresh() {
        data = withContext(Dispatchers.IO) { repo.snapshot() }
        loaded = true
        loadError = false
    }
    fun notify(message: String) { scope.launch { snackbar.showSnackbar(message) } }
    fun reload() {
        scope.launch {
            try { refresh() } catch (_: Exception) { loadError = true }
        }
    }
    LaunchedEffect(Unit) { reload() }
    val farmer = data.farmers.find { it.id == farmerId }
    val field = data.fields.find { it.id == fieldId }
    val back: () -> Unit = { if (fieldId != 0L) fieldId = 0 else farmerId = 0 }
    BackHandler(form.isEmpty() && farmerId != 0L, back)
    fun openFarmer(value: Farmer) { farmerId = value.id; fieldId = 0 }
    fun openVisit(visit: FieldVisit) {
        data.fields.find { it.id == visit.fieldId }?.let { farmerId = it.farmerId; fieldId = it.id }
    }
    fun edit(kind: String, id: Long = 0) { editId = id; form = kind }

    val savePdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) scope.launch {
            exportBusy = true
            try {
                withContext(Dispatchers.IO) {
                    val output = requireNotNull(context.contentResolver.openOutputStream(uri))
                    output.use { out -> File(reportPath).inputStream().use { it.copyTo(out) } }
                }
                notify("PDF चुने हुए फ़ोल्डर में सहेजा गया")
            } catch (_: Exception) { notify("PDF नहीं सहेज सके। दूसरा फ़ोल्डर चुनकर फिर कोशिश करें।") }
            finally { exportBusy = false }
        }
    }
    val saveEvidence = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            exportBusy = true
            try {
                withContext(Dispatchers.IO) { requireNotNull(context.contentResolver.openOutputStream(uri)).use { out -> File(reportPath).inputStream().use { it.copyTo(out) } } }
                notify("प्रमाण फ़ोल्डर सहेजा गया")
            } catch (_: Exception) { notify("सहेज नहीं सके। दूसरा फ़ोल्डर चुनें।") }
            finally { exportBusy = false }
        }
    }
    fun export(value: Farmer, share: Boolean, evidence: Boolean = false) {
        if (exportBusy) return
        scope.launch {
            exportBusy = true
            try {
                val file = withContext(Dispatchers.IO) {
                    val fields = data.fieldsFor(value.id)
                    val visits = fields.associate { it.id to data.visitsFor(it.id) }
                    val impacts = data.impactsFor(value.id)
                    val pdf = CollectivePdfService.createFarmerReport(context, value, fields, visits, impacts)
                    if (evidence) CollectiveEvidence.create(context, value, fields, visits, pdf, impacts) else pdf
                }
                reportPath = file.absolutePath
                if (evidence) saveEvidence.launch(file.name) else if (share) CollectivePdfService.share(context, file, value) else savePdf.launch(file.name)
            } catch (_: Exception) { notify("रिपोर्ट नहीं बन सकी। आपके रिकॉर्ड सुरक्षित हैं।") }
            finally { exportBusy = false }
        }
    }

    MaterialTheme(colorScheme = lightColorScheme(
        primary = Forest, secondary = Amber, background = Paper, surface = Color.White,
        primaryContainer = Moss, onPrimaryContainer = Forest, surfaceVariant = Moss,
        onSurfaceVariant = Muted, outline = Color(0xFFB8C8BD),
    )) {
        Scaffold(
            containerColor = Paper,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                if (farmer != null) TopAppBar(
                    title = { Column {
                        Text(field?.crop ?: farmer.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (field == null) farmer.village else farmer.name, fontSize = 12.sp, color = Muted)
                    } },
                    navigationIcon = { IconButton(back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "वापस") } },
                    actions = { IconButton({ if (field == null) edit("farmer", farmer.id) else edit("field", field.id) }) { Icon(Icons.Rounded.Edit, "रिकॉर्ड संपादित करें") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper),
                )
            },
            bottomBar = {
                if (farmerId == 0L) NavigationBar(containerColor = Color.White) {
                    listOf("होम" to Icons.Rounded.SpaceDashboard, "किसान" to Icons.Rounded.Groups,
                        "विजिट" to Icons.Rounded.EventNote, "रिपोर्ट" to Icons.Rounded.Description).forEachIndexed { index, item ->
                        NavigationBarItem(tab == index, { tab = index }, { Icon(item.second, null) }, label = { Text(item.first) })
                    }
                }
            },
            floatingActionButton = {
                if (loaded && farmerId == 0L && tab < 2) ExtendedFloatingActionButton(
                    modifier = Modifier.testTag("add-farmer").semantics { contentDescription = "किसान जोड़ें" },
                    onClick = { edit("farmer") }, containerColor = Forest, contentColor = Color.White,
                    icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("किसान जोड़ें") },
                )
            },
        ) { padding ->
            when {
                loadError -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("रिकॉर्ड नहीं खोल सके")
                        Button({ reload() }) { Text("फिर कोशिश करें") }
                    }
                }
                !loaded -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                farmer != null && field != null -> FieldScreen(data, field, padding, { edit("visit") }, { edit("visit", it.id) })
                farmer != null -> ProfileScreen(data, farmer, padding,
                    { fieldId = it.id }, { edit("field") }, { reportFarmerId = farmer.id },
                    { edit("impact") },
                    { intent -> runCatching { context.startActivity(intent) }.onFailure { notify("इस काम के लिए कोई ऐप उपलब्ध नहीं है") } })
                tab == 0 -> Dashboard(data, padding, { tab = 1 }, ::openFarmer, ::openVisit)
                tab == 1 -> FarmerList(data, padding, ::openFarmer)
                tab == 2 -> VisitList(data, padding, ::openVisit)
                else -> ReportsScreen(data, padding, { reportFarmerId = it.id })
            }
        }
        val reportFarmer = data.farmers.find { it.id == reportFarmerId }
        if (reportFarmer != null) AlertDialog(
            onDismissRequest = { if (!exportBusy) reportFarmerId = 0 },
            icon = { Icon(Icons.Rounded.PictureAsPdf, null) },
            title = { Text("किसान की रिपोर्ट") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(reportFarmer.name, fontWeight = FontWeight.Bold)
                Text("प्रोफाइल, खेत, विजिट, प्रभाव आकलन और फोटो एक PDF में।")
                if (exportBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Button({ export(reportFarmer, false) }, Modifier.fillMaxWidth(), enabled = !exportBusy) { Text("फोन में PDF सहेजें") }
                OutlinedButton({ export(reportFarmer, true) }, Modifier.fillMaxWidth(), enabled = !exportBusy) { Text("PDF साझा करें") }
                OutlinedButton({ export(reportFarmer, false, true) }, Modifier.fillMaxWidth(), enabled = !exportBusy) { Text("प्रमाण फ़ोल्डर (.zip) सहेजें") }
            } },
            confirmButton = { TextButton({ reportFarmerId = 0 }, enabled = !exportBusy) { Text("बंद करें") } },
        )
        when (form) {
            "impact" -> if (farmer != null) ImpactForm(farmer, data.fieldsFor(farmer.id), { form = "" }) { value ->
                withContext(Dispatchers.IO) { repo.addImpact(value) }
                refresh(); form = ""; notify("प्रभाव आकलन इसी फोन पर सहेजा गया")
            }
            "farmer" -> FarmerForm(data.farmers.find { it.id == editId }, { form = "" }) { value ->
                val id = withContext(Dispatchers.IO) { repo.addFarmer(value) }
                refresh(); farmerId = id; fieldId = 0; form = ""; notify("किसान इसी फोन पर सहेजा गया")
            }
            "field" -> if (farmer != null) FieldForm(farmer.id, data.fields.find { it.id == editId }, { form = "" }) { value ->
                val id = withContext(Dispatchers.IO) { repo.addField(value) }
                refresh(); fieldId = id; form = ""; notify("खेत सहेजा गया")
            }
            "visit" -> if (field != null) VisitForm(field.id, data.visits.find { it.id == editId }, { form = "" }) { value ->
                withContext(Dispatchers.IO) { repo.addVisit(value) }
                refresh(); form = ""; notify("विजिट सहेजी गई")
            }
        }
    }
}

@Composable
private fun Dashboard(data: CollectiveSnapshot, padding: PaddingValues, all: () -> Unit, open: (Farmer) -> Unit, visit: (FieldVisit) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("JOITA", letterSpacing = 3.sp, fontWeight = FontWeight.ExtraBold, color = Forest); Text("किसान समूह", fontSize = 13.sp, color = Muted) }
            BadgeLabel("इसी फोन पर", Icons.Rounded.OfflinePin)
        } }
        item { Card(shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Forest, Color(0xFF287258)))).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.Spa, null, tint = Color(0xFFB9D9B1), modifier = Modifier.size(32.dp))
                Text("हर किसान,\nबेहतर भविष्य।", color = Color.White, fontSize = 30.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold)
                Text("किसान, खेत और असर का रिकॉर्ड।\nऑफलाइन भी, हमेशा साथ।", color = Color(0xFFD1E6D8), fontSize = 14.sp)
                Text(CollectiveRules.today(), color = Color(0xFFD1E6D8), fontSize = 12.sp)
            }
        } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("किसान", data.farmers.size.toString(), Icons.Rounded.Groups, Modifier.weight(1f))
            Metric("एकड़", CollectiveRules.acres(data.acres), Icons.Rounded.Landscape, Modifier.weight(1f))
            Metric("किसान साथी", data.farmers.count { it.leadFarmer }.toString(), Icons.Rounded.Stars, Modifier.weight(1f))
        } }
        item { Surface(shape = RoundedCornerShape(20.dp), color = Moss) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("साथ मिलकर प्रगति", fontWeight = FontWeight.Bold, color = Forest)
            Text("लक्ष्य · 200 किसान / 200 एकड़ / 50 किसान साथी", fontSize = 12.sp, color = Muted)
            LinearProgressIndicator(progress = { (data.farmers.size / 200f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = Forest)
            Text("${data.fields.size} खेत · ${data.visits.size} विजिट दर्ज", fontSize = 13.sp)
        } } }
        if (data.fields.any { field -> data.visitsFor(field.id).isEmpty() }) item {
            Text("${data.fields.count { data.visitsFor(it.id).isEmpty() }} खेतों की पहली विजिट बाकी", color = Amber, fontWeight = FontWeight.Medium)
        }
        item { SectionTitle("आपके किसान", "${data.farmers.size} पंजीकृत", all) }
        if (data.farmers.isEmpty()) item { EmptyState("पहला किसान जोड़ें", "किसान जोड़ें दबाएँ। इंटरनेट के बिना भी रिकॉर्ड उपलब्ध हैं।", Icons.Rounded.Groups) }
        items(data.farmers.sortedByDescending { it.createdAt }.take(3), key = { "f${it.id}" }) { FarmerCard(it, data, { open(it) }) }
        if (data.visits.isNotEmpty()) {
            item { Text("हाल की फील्ड गतिविधि", style = MaterialTheme.typography.titleLarge) }
            items(data.recentVisits().take(3), key = { "v${it.id}" }) { ActivityCard(it, data, { visit(it) }) }
        }
        item { Spacer(Modifier.height(78.dp)) }
    }
}

@Composable
private fun FarmerList(data: CollectiveSnapshot, padding: PaddingValues, open: (Farmer) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("सभी किसान") }
    val farmers = data.farmers.filter { f ->
        val fields = data.fieldsFor(f.id)
        (query.isBlank() || listOf(f.name, f.village, f.phone).plus(fields.map { it.crop }).any { it.contains(query.trim(), true) }) &&
            (filter != "किसान साथी" || f.leadFarmer) && (filter != "खेत नहीं जोड़ा" || fields.isEmpty())
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
        PageTitle("किसान रजिस्टर", "किसान और उनकी जानकारी")
        SearchBox(query, { query = it }, "नाम, गाँव, फोन या फसल")
        ChipRow(listOf("सभी किसान", "किसान साथी", "खेत नहीं जोड़ा"), filter) { filter = it }
        Text("${farmers.size} किसान", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 110.dp)) {
            if (farmers.isEmpty()) item { EmptyState("किसान नहीं मिले", "दूसरा नाम खोजें या पहला किसान जोड़ें।", Icons.Rounded.Search) }
            items(farmers, key = { it.id }) { FarmerCard(it, data, { open(it) }) }
        }
    }
}

@Composable
private fun FarmerCard(f: Farmer, data: CollectiveSnapshot, open: () -> Unit) {
    Card(onClick = open, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(Moss, CircleShape), contentAlignment = Alignment.Center) {
                Text(f.name.take(1).uppercase(), fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Forest)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(f.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(f.village, color = Muted, fontSize = 13.sp)
                val fields = data.fieldsFor(f.id)
                Text("${fields.size} खेत · ${CollectiveRules.acres(fields.sumOf { it.acreage })} एकड़", color = Forest, fontSize = 12.sp)
            }
            if (f.leadFarmer) Icon(Icons.Rounded.Stars, "किसान साथी", tint = Amber, modifier = Modifier.size(20.dp))
            Icon(Icons.Rounded.ChevronRight, null, tint = Muted)
        }
    }
}

@Composable
private fun ProfileScreen(data: CollectiveSnapshot, farmer: Farmer, padding: PaddingValues, open: (FarmField) -> Unit, add: () -> Unit, report: () -> Unit, impact: () -> Unit, launch: (Intent) -> Unit) {
    val fields = data.fieldsFor(farmer.id)
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Surface(color = Forest, shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (farmer.leadFarmer) "किसान साथी" else "किसान प्रोफाइल", color = Color(0xFFCCDFBE), letterSpacing = 2.sp, fontSize = 11.sp)
                Text(farmer.name, fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("${fields.size} खेत    /    ${CollectiveRules.acres(fields.sumOf { it.acreage })} एकड़", color = Color.White)
            }
        } }
        item { ImpactHistory(data.impactsFor(farmer.id), impact) }
        item { DetailCard(listOf("गाँव" to farmer.village, "फोन" to farmer.phone.ifBlank { "दर्ज नहीं" }, "भूमि व्यवस्था" to farmer.tenure, "परिवार / खेती की जानकारी" to farmer.notes)) }
        if (farmer.photoPath.isNotBlank()) item { PhotoPreview(farmer.photoPath) }
        item { DetailCard(listOf("पंजीकरण संदर्भ" to "JOITA-${farmer.id}", "दर्ज करने वाले कर्मी" to farmer.recordedBy.ifBlank { "दर्ज नहीं" }, "सहमति रिकॉर्ड" to if (farmer.acknowledgedAt > 0) "कर्मी ने किसान की सहमति दर्ज की: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(farmer.acknowledgedAt))}" else "दर्ज नहीं")) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (farmer.phone.isNotBlank()) OutlinedButton({ launch(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", farmer.phone, null))) }) { Icon(Icons.Rounded.Phone, null, Modifier.size(18.dp)); Text(" कॉल") }
            if (farmer.latitude.isNotBlank() && CollectiveRules.validCoordinates(farmer.latitude, farmer.longitude)) OutlinedButton({
                launch(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${farmer.latitude},${farmer.longitude}?q=${farmer.latitude},${farmer.longitude}")))
            }) { Icon(Icons.Rounded.LocationOn, null, Modifier.size(18.dp)); Text(" नक्शा") }
        } }
        item { Button(report, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Icon(Icons.Rounded.PictureAsPdf, null); Spacer(Modifier.width(8.dp)); Text("किसान PDF सहेजें / साझा करें") } }
        item { SectionTitle("खेत", "फसल और भूमि रिकॉर्ड", add, "खेत जोड़ें") }
        if (fields.isEmpty()) item { EmptyState("पहला खेत जोड़ें", "क्षेत्र, फसल और खेती की स्थिति दर्ज करें।", Icons.Rounded.Landscape) }
        items(fields, key = { it.id }) { field ->
            Card(onClick = { open(field) }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Grass, null, tint = Forest)
                        Text(field.crop, Modifier.weight(1f).padding(horizontal = 10.dp), style = MaterialTheme.typography.titleLarge)
                        Text("${CollectiveRules.acres(field.acreage)} एकड़", color = Forest, fontWeight = FontWeight.Bold)
                    }
                    Text(listOf(field.variety, field.season).filter { it.isNotBlank() }.joinToString(" · "), color = Muted)
                    val visits = data.visitsFor(field.id)
                    Text(if (visits.isEmpty()) "पहली विजिट दर्ज नहीं" else "${visits.size} विजिट · अंतिम ${visits.first().date}", fontSize = 12.sp, color = if (visits.isEmpty()) Amber else Forest)
                }
            }
        }
    }
}

@Composable
private fun FieldScreen(data: CollectiveSnapshot, field: FarmField, padding: PaddingValues, add: () -> Unit, edit: (FieldVisit) -> Unit) {
    val visits = data.visitsFor(field.id)
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metric("एकड़", CollectiveRules.acres(field.acreage), Icons.Rounded.Landscape, Modifier.weight(1f))
            Metric("विजिट", visits.size.toString(), Icons.Rounded.EventNote, Modifier.weight(1f))
        } }
        item { DetailCard(listOf("मौसम" to field.season, "किस्म" to field.variety, "बुवाई तिथि" to field.sowingDate, "मिट्टी का प्रकार" to field.soilType, "सिंचाई" to field.irrigation, "खाद / इनपुट" to field.inputs, "स्थान / सीमा" to field.boundaryNotes)) }
        item { Button(add, Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Icon(Icons.Rounded.Add, null); Text(" फील्ड विजिट दर्ज करें") } }
        item { Text("फील्ड डायरी", style = MaterialTheme.typography.titleLarge) }
        if (visits.isEmpty()) item { EmptyState("खेत का रिकॉर्ड यहाँ शुरू करें", "अगली विजिट में फसल, अवलोकन और सलाह दर्ज करें।", Icons.Rounded.EventNote) }
        items(visits, key = { it.id }) { v ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(v.date, fontWeight = FontWeight.Bold, color = Forest); Text(v.officer, color = Muted, fontSize = 12.sp) }
                        IconButton({ edit(v) }) { Icon(Icons.Rounded.Edit, "विजिट संपादित करें") }
                    }
                    if (v.cropStage.isNotBlank()) BadgeLabel(v.cropStage, Icons.Rounded.Grass)
                    listOf("अवलोकन" to v.observations, "कीट / रोग" to v.issues, "सलाह" to v.recommendations, "उपज / कटाई" to v.yieldData, "नोट" to v.notes).filter { it.second.isNotBlank() }.forEach { Detail(it.first, it.second) }
                    if (v.photoPath.isNotBlank()) PhotoPreview(v.photoPath)
                }
            }
        }
    }
}

@Composable
private fun VisitList(data: CollectiveSnapshot, padding: PaddingValues, open: (FieldVisit) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("सभी विजिट") }
    val visits = data.recentVisits().filter { v ->
        val f = data.farmerFor(v)
        listOf(v.officer, v.observations, v.issues, f?.name.orEmpty(), f?.village.orEmpty()).any { it.contains(query, true) } &&
            (filter != "समस्या दर्ज" || v.issues.isNotBlank()) && (filter != "फोटो सहित" || v.photoPath.isNotBlank())
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
        PageTitle("फील्ड डायरी", "समूह में ${data.visits.size} विजिट")
        SearchBox(query, { query = it }, "किसान, कर्मी या अवलोकन")
        ChipRow(listOf("सभी विजिट", "समस्या दर्ज", "फोटो सहित"), filter) { filter = it }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
            if (visits.isEmpty()) item { EmptyState("कोई विजिट नहीं", "पहली विजिट के लिए किसान का खेत खोलें।", Icons.Rounded.EventNote) }
            items(visits, key = { it.id }) { ActivityCard(it, data, { open(it) }) }
        }
    }
}

@Composable
private fun ActivityCard(v: FieldVisit, data: CollectiveSnapshot, open: () -> Unit) {
    Card(onClick = open, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.EventNote, null, tint = Forest)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(data.farmerFor(v)?.name ?: "फील्ड विजिट", fontWeight = FontWeight.SemiBold)
                Text("${v.date} · ${v.officer}", color = Muted, fontSize = 12.sp)
                Text(v.observations.ifBlank { v.cropStage.ifBlank { "विजिट विवरण खोलें" } }, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (v.issues.isNotBlank()) Text("समस्या दर्ज", fontSize = 12.sp, color = Amber, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Muted)
        }
    }
}

@Composable
private fun ReportsScreen(data: CollectiveSnapshot, padding: PaddingValues, report: (Farmer) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageTitle("रिपोर्ट", "फील्ड कार्य की साझा करने योग्य रिपोर्ट") }
        item { Surface(color = Moss, shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.PictureAsPdf, null, tint = Forest, modifier = Modifier.size(32.dp))
            Text("किसान का पूरा रिकॉर्ड", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Forest)
            Text("PDF फोन में सहेजें या टीम को भेजें। खेत, विजिट, हिन्दी प्रभाव आकलन और फोटो शामिल हैं।", color = Muted)
        } } }
        item { SearchBox(query, { query = it }, "रिपोर्ट के लिए किसान खोजें") }
        if (data.farmers.isEmpty()) item { EmptyState("रिकॉर्ड से रिपोर्ट बनती है", "पहली PDF के लिए किसान और खेत जोड़ें।", Icons.Rounded.Description) }
        items(data.farmers.filter { it.name.contains(query, true) || it.village.contains(query, true) }, key = { it.id }) { f ->
            Card(onClick = { report(f) }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(f.name, fontWeight = FontWeight.Bold); Text("${data.fieldsFor(f.id).size} खेत · ${f.village}", color = Muted, fontSize = 12.sp) }
                    Icon(Icons.Rounded.FileDownload, "PDF बनाएँ", tint = Forest)
                }
            }
        }
        item { Text("संस्करण 1.1.0 · इसी फोन पर सहेजा गया\nक्लाउड सिंक नहीं है। फोन बदलने से पहले PDF और ZIP बाहर सुरक्षित रखें।", fontSize = 12.sp, color = Muted, modifier = Modifier.padding(vertical = 16.dp)) }
    }
}

@Composable private fun Metric(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = Color.White) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(icon, null, tint = Forest, modifier = Modifier.size(23.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, color = Muted, fontSize = 12.sp)
        }
    }
}
@Composable internal fun PageTitle(title: String, subtitle: String) { Column(Modifier.padding(top = 22.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, fontSize = 13.sp) } }
@Composable private fun SectionTitle(title: String, subtitle: String, action: () -> Unit, label: String = "सभी देखें") {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleLarge); Text(subtitle, fontSize = 12.sp, color = Muted) }; TextButton(action) { Text(label) } }
}
@Composable internal fun BadgeLabel(text: String, icon: ImageVector) {
    Surface(color = Moss, shape = RoundedCornerShape(10.dp)) { Row(Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) { Icon(icon, null, Modifier.size(14.dp), tint = Forest); Text(text, fontSize = 11.sp, color = Forest) } }
}
@Composable internal fun Detail(label: String, value: String) { Column(verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(label.uppercase(), color = Muted, fontSize = 10.sp, letterSpacing = 1.sp); Text(value, fontSize = 15.sp) } }
@Composable private fun DetailCard(values: List<Pair<String, String>>) {
    Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) { Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { values.filter { it.second.isNotBlank() }.forEach { Detail(it.first, it.second) } } }
}
@Composable private fun EmptyState(title: String, body: String, icon: ImageVector) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(36.dp), tint = Forest); Text(title, fontWeight = FontWeight.Bold); Text(body, color = Muted, fontSize = 14.sp)
    }
}
@Composable private fun SearchBox(query: String, change: (String) -> Unit, hint: String) {
    OutlinedTextField(query, change, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp),
        placeholder = { Text(hint, fontSize = 13.sp) }, leadingIcon = { Icon(Icons.Rounded.Search, null) },
        trailingIcon = { if (query.isNotEmpty()) IconButton({ change("") }) { Icon(Icons.Rounded.Close, "खोज हटाएँ") } })
}
@Composable internal fun ChipRow(values: List<String>, selected: String, choose: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { values.forEach { FilterChip(selected == it, { choose(it) }, { Text(it, fontSize = 12.sp) }) } }
}

@Composable
private fun FarmerForm(initial: Farmer?, close: () -> Unit, save: suspend (Farmer) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var phone by rememberSaveable { mutableStateOf(initial?.phone.orEmpty()) }
    var village by rememberSaveable { mutableStateOf(initial?.village.orEmpty()) }
    var lat by rememberSaveable { mutableStateOf(initial?.latitude.orEmpty()) }
    var lng by rememberSaveable { mutableStateOf(initial?.longitude.orEmpty()) }
    var lead by rememberSaveable { mutableStateOf(initial?.leadFarmer ?: false) }
    var notes by rememberSaveable { mutableStateOf(initial?.notes.orEmpty()) }
    var tenure by rememberSaveable { mutableStateOf(initial?.tenure?.let { mapOf("Owned" to "स्वामित्व", "Tenant" to "किरायेदार", "Shared" to "साझा")[it] ?: it } ?: "स्वामित्व") }
    var photo by rememberSaveable { mutableStateOf(initial?.photoPath.orEmpty()) }
    var recordedBy by rememberSaveable { mutableStateOf(initial?.recordedBy.orEmpty()) }
    var acknowledged by rememberSaveable { mutableStateOf(false) }
    var photoBusy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    fun readLocation() {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = manager.getProviders(true).mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }.filter { android.os.SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos in 0..120_000_000_000L }.maxByOrNull { it.time }
        if (location == null) error = "हाल का GPS नहीं मिला। Maps खोलकर स्थान मिलने दें, फिर कोशिश करें या निर्देशांक भरें।"
        else { lat = "%.6f".format(java.util.Locale.US, location.latitude); lng = "%.6f".format(java.util.Locale.US, location.longitude); error = "" }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) readLocation() else error = "स्थान की अनुमति नहीं मिली। निर्देशांक खुद भर सकते हैं।"
    }
    FormScreen(if (initial == null) "किसान जोड़ें" else "किसान संपादित करें", "किसान प्रोफाइल", close, busy || photoBusy, {
        error = when {
            name.isBlank() -> "किसान का नाम भरें।"
            village.isBlank() -> "गाँव भरें।"
            !CollectiveRules.validPhone(phone) -> "सही फोन नंबर भरें।"
            !CollectiveRules.validCoordinates(lat, lng) -> "दोनों सही निर्देशांक भरें।"
            acknowledged && recordedBy.isBlank() -> "सहमति दर्ज करने वाले कर्मी का नाम भरें।"
            else -> ""
        }
        if (error.isEmpty()) scope.launch { busy = true; try { save(Farmer(initial?.id ?: 0, name.trim(), phone.trim(), village.trim(), lat.trim(), lng.trim(), lead, notes.trim(), tenure, initial?.createdAt ?: System.currentTimeMillis(), photo, recordedBy.trim(), if (acknowledged) System.currentTimeMillis() else 0)) } catch (_: Exception) { error = "सहेज नहीं सके। फिर कोशिश करें।" } finally { busy = false } }
    }) {
        if (error.isNotEmpty()) ErrorText(error)
        FormSection("पहचान और संपर्क")
        Input("किसान का नाम", name, { name = it }, required = true)
        Input("मोबाइल नंबर", phone, { phone = it }, keyboard = KeyboardType.Phone)
        Input("गाँव", village, { village = it }, required = true)
        FormSection("किसान का फोटो")
        Text("फोटो लेने / सहेजने से पहले किसान की अनुमति लें।", fontSize = 13.sp, color = Muted)
        PhotoInput(photo, { photo = it }, { photoBusy = it })
        Input("पंजीकरण करने वाले कर्मी", recordedBy, { recordedBy = it })
        FormSection("भूमि और परियोजना")
        Choice("भूमि व्यवस्था", listOf("स्वामित्व", "किरायेदार", "साझा"), tenure) { tenure = it }
        Row(verticalAlignment = Alignment.CenterVertically) { Switch(lead, { lead = it }); Column(Modifier.padding(start = 12.dp)) { Text("किसान साथी", fontWeight = FontWeight.Medium); Text("आसपास के किसानों का सहयोग करते हैं", fontSize = 12.sp, color = Muted) } }
        FormSection("स्थान")
        OutlinedButton(onClick = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) readLocation()
            else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.MyLocation, null); Text(if (lat.isBlank()) " वर्तमान स्थान लें" else " स्थान दोबारा लें") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.weight(1f)) { Input("अक्षांश", lat, { lat = it }, keyboard = KeyboardType.Decimal) }; Box(Modifier.weight(1f)) { Input("देशांतर", lng, { lng = it }, keyboard = KeyboardType.Decimal) } }
        FormSection("अन्य जानकारी")
        Input("परिवार और खेती की जानकारी", notes, { notes = it }, lines = 4)
        FormSection("किसान की सहमति")
        Text("जानकारी किसान को पढ़कर सुनाएँ और समझाएँ कि Joita पंजीकरण के लिए विवरण / फोटो इस फोन पर रखता है। सहमति मिलने पर ही टिक करें। यह कर्मी का रिकॉर्ड है, डिजिटल हस्ताक्षर नहीं।", fontSize = 13.sp, color = Muted)
        if (initial?.acknowledgedAt != null && initial.acknowledgedAt > 0) Text("बदली जानकारी सहेजते समय किसान की सहमति फिर दर्ज करें।", fontSize = 13.sp, color = Amber)
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(acknowledged, { acknowledged = it }); Text("किसान ने जानकारी जाँची और पंजीकरण के लिए सहमति दी", fontSize = 14.sp) }
        if (error.isNotEmpty()) ErrorText(error)
    }
}

@Composable
private fun FieldForm(farmerId: Long, initial: FarmField?, close: () -> Unit, save: suspend (FarmField) -> Unit) {
    val scope = rememberCoroutineScope()
    var area by rememberSaveable { mutableStateOf(initial?.let { java.math.BigDecimal.valueOf(it.acreage).stripTrailingZeros().toPlainString() }.orEmpty()) }
    var crop by rememberSaveable { mutableStateOf(initial?.crop.orEmpty()) }
    var variety by rememberSaveable { mutableStateOf(initial?.variety.orEmpty()) }
    var season by rememberSaveable { mutableStateOf(initial?.season?.let { mapOf("Kharif" to "खरीफ", "Rabi" to "रबी", "Zaid" to "जायद", "Perennial" to "बहुवर्षीय")[it] ?: it } ?: "खरीफ") }
    var sowing by rememberSaveable { mutableStateOf(initial?.sowingDate.orEmpty()) }
    var soil by rememberSaveable { mutableStateOf(initial?.soilType.orEmpty()) }
    var irrigation by rememberSaveable { mutableStateOf(initial?.irrigation.orEmpty()) }
    var inputs by rememberSaveable { mutableStateOf(initial?.inputs.orEmpty()) }
    var boundary by rememberSaveable { mutableStateOf(initial?.boundaryNotes.orEmpty()) }
    var error by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
    FormScreen(if (initial == null) "खेत जोड़ें" else "खेत संपादित करें", "फसल और भूमि", close, busy, {
        error = when { !CollectiveRules.validArea(area) -> "शून्य से अधिक सही क्षेत्र भरें।"; crop.isBlank() -> "फसल का नाम भरें।"; sowing.isNotBlank() && CollectiveRules.dateMillis(sowing) == null -> "बुवाई तिथि DD-MM-YYYY में भरें।"; else -> "" }
        if (error.isEmpty()) scope.launch { busy = true; try { save(FarmField(initial?.id ?: 0, farmerId, area.toDouble(), crop.trim(), variety.trim(), season, sowing.trim(), soil.trim(), irrigation.trim(), inputs.trim(), boundary.trim(), initial?.createdAt ?: System.currentTimeMillis())) } catch (_: Exception) { error = "सहेज नहीं सके। फिर कोशिश करें।" } finally { busy = false } }
    }) {
        FormSection("खेत की पहचान")
        Input("क्षेत्र (एकड़)", area, { area = it }, required = true, keyboard = KeyboardType.Decimal)
        Input("फसल", crop, { crop = it }, required = true)
        Input("किस्म", variety, { variety = it })
        Choice("मौसम", listOf("खरीफ", "रबी", "जायद", "बहुवर्षीय"), season) { season = it }
        DateInput("बुवाई तिथि", sowing, { sowing = it })
        FormSection("खेती की स्थिति")
        Input("मिट्टी का प्रकार", soil, { soil = it })
        Input("सिंचाई", irrigation, { irrigation = it })
        Input("इस्तेमाल किए खाद / इनपुट", inputs, { inputs = it }, lines = 3)
        Input("स्थान और सीमा का विवरण", boundary, { boundary = it }, lines = 3)
        if (error.isNotEmpty()) ErrorText(error)
    }
}

@Composable
private fun VisitForm(fieldId: Long, initial: FieldVisit?, close: () -> Unit, save: suspend (FieldVisit) -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var date by rememberSaveable { mutableStateOf(initial?.date ?: CollectiveRules.today()) }
    var officer by rememberSaveable { mutableStateOf(initial?.officer.orEmpty()) }
    var stage by rememberSaveable { mutableStateOf(initial?.cropStage.orEmpty()) }
    var observations by rememberSaveable { mutableStateOf(initial?.observations.orEmpty()) }
    var issues by rememberSaveable { mutableStateOf(initial?.issues.orEmpty()) }
    var recommendations by rememberSaveable { mutableStateOf(initial?.recommendations.orEmpty()) }
    var yieldData by rememberSaveable { mutableStateOf(initial?.yieldData.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(initial?.notes.orEmpty()) }
    var photo by rememberSaveable { mutableStateOf(initial?.photoPath.orEmpty()) }
    var error by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
    var photoBusy by remember { mutableStateOf(false) }
    FormScreen(if (initial == null) "विजिट दर्ज करें" else "विजिट संपादित करें", "फील्ड डायरी", close, busy || photoBusy, {
        error = when { CollectiveRules.dateMillis(date) == null -> "विजिट तिथि DD-MM-YYYY में भरें।"; officer.isBlank() -> "फील्ड कर्मी का नाम भरें।"; else -> "" }
        if (error.isEmpty()) scope.launch { busy = true; try { save(FieldVisit(initial?.id ?: 0, fieldId, date, officer.trim(), stage.trim(), observations.trim(), issues.trim(), recommendations.trim(), yieldData.trim(), notes.trim(), photo, initial?.createdAt ?: System.currentTimeMillis())) } catch (_: Exception) { error = "सहेज नहीं सके। फिर कोशिश करें।" } finally { busy = false } }
    }) {
        FormSection("विजिट विवरण")
        DateInput("विजिट तिथि", date, { date = it })
        Input("फील्ड कर्मी", officer, { officer = it }, required = true)
        Input("फसल की अवस्था", stage, { stage = it })
        FormSection("फील्ड अवलोकन")
        Input("अवलोकन", observations, { observations = it }, lines = 4)
        Input("कीट या रोग की समस्या", issues, { issues = it }, lines = 3)
        Input("सलाह", recommendations, { recommendations = it }, lines = 4)
        Input("उपज / कटाई की जानकारी", yieldData, { yieldData = it }, lines = 3)
        Input("जलवायु गतिविधि और प्रमाण नोट", notes, { notes = it }, lines = 4)
        Text("गतिविधि / तिथि, मात्रा / इकाई, स्रोत / बैच, तरीका, खेत संदर्भ और गवाह लिखें। फोटो जोड़ें और रसीदें अलग सुरक्षित रखें।", fontSize = 13.sp, color = Muted)
        FormSection("फोटो प्रमाण")
        PhotoInput(photo, { photo = it }, { photoBusy = it })
        if (error.isNotEmpty()) ErrorText(error)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormScreen(title: String, eyebrow: String, close: () -> Unit, busy: Boolean, save: () -> Unit, scrollKey: Any? = null, content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberLazyListState()
    LaunchedEffect(scrollKey) { scroll.scrollToItem(0) }
    Dialog(onDismissRequest = { if (!busy) close() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)) {
        Surface(Modifier.fillMaxSize(), color = Paper) {
            Scaffold(containerColor = Paper, topBar = { TopAppBar(title = { Column { Text(eyebrow, color = Forest, fontSize = 11.sp, letterSpacing = 1.5.sp); Text(title, fontWeight = FontWeight.Bold) } }, navigationIcon = { IconButton({ if (!busy) close() }) { Icon(Icons.Rounded.Close, "बंद करें") } }, actions = { TextButton(save, enabled = !busy) { Text("सहेजें", fontWeight = FontWeight.Bold) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)) }) { padding ->
                LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding(), state = scroll, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = content) }; item { Spacer(Modifier.height(30.dp)) } }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = padding.calculateTopPadding()))
            }
        }
    }
}

@Composable internal fun FormSection(value: String) { Text(value.uppercase(), color = Forest, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.4.sp, modifier = Modifier.padding(top = 7.dp)) }
@Composable internal fun Input(label: String, value: String, change: (String) -> Unit, required: Boolean = false, lines: Int = 1, keyboard: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(value, change, Modifier.fillMaxWidth(), label = { Text(label + if (required) " *" else "") }, singleLine = lines == 1, minLines = lines, keyboardOptions = KeyboardOptions(keyboardType = keyboard), shape = RoundedCornerShape(14.dp))
}
@Composable private fun Choice(label: String, values: List<String>, selected: String, change: (String) -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(label, fontSize = 13.sp, color = Muted); ChipRow(values, selected, change) } }
@Composable internal fun ErrorText(value: String) { Text(value, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
@Composable internal fun PhotoPreview(path: String) {
    var bitmap by remember(path) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(path) { bitmap = withContext(Dispatchers.IO) { CollectivePhotos.decode(path) } }
    Surface(shape = RoundedCornerShape(16.dp), color = Moss, modifier = Modifier.fillMaxWidth().height(220.dp)) {
        if (bitmap == null) Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Image, "फील्ड फोटो", tint = Forest) }
        else androidx.compose.foundation.Image(bitmap!!.asImageBitmap(), "फील्ड फोटो", Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
    }
    // Compose may draw a previous frame after disposal; allow GC to release these bounded thumbnails.
}

@Composable internal fun DateInput(label: String, value: String, change: (String) -> Unit) {
    val context = LocalContext.current
    OutlinedTextField(value, change, Modifier.fillMaxWidth(), label = { Text("$label (DD-MM-YYYY)") }, singleLine = true, shape = RoundedCornerShape(14.dp), trailingIcon = {
        IconButton({
            val date = Calendar.getInstance().apply { CollectiveRules.dateMillis(value)?.let { timeInMillis = it } }
            DatePickerDialog(context, { _, year, month, day -> change(String.format(Locale.US, "%02d-%02d-%04d", day, month + 1, year)) }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show()
        }) { Icon(Icons.Rounded.DateRange, "तिथि चुनें") }
    })
}

@Composable internal fun PhotoInput(photo: String, change: (String) -> Unit, onBusy: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by rememberSaveable { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    fun import(uri: Uri, source: File? = null) {
        working = true; onBusy(true)
        scope.launch {
            try { change(withContext(Dispatchers.IO) { CollectivePhotos.import(context, uri, if (source == null) "Selected image" else "Camera capture") }); error = "" }
            catch (_: Exception) { error = "फोटो नहीं पढ़ सके। 25 MB से छोटा सही फोटो चुनें।" }
            finally { source?.delete(); working = false; onBusy(false) }
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let { uri -> import(uri) } }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (pending.isNotBlank()) {
            val file = File(pending)
            if (success) import(FileProvider.getUriForFile(context, "${context.packageName}.files", file), file) else file.delete()
        }
        pending = ""
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) runCatching {
            val dir = File(context.cacheDir, "evidence").apply { mkdirs() }
            val file = File.createTempFile("capture-", ".jpg", dir)
            pending = file.absolutePath
            camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.files", file))
        }.onFailure { error = "कैमरा उपलब्ध नहीं। गैलरी से फोटो चुनें।" }
        else error = "कैमरे की अनुमति नहीं मिली। गैलरी से फोटो चुन सकते हैं।"
    }
    if (photo.isNotBlank()) PhotoPreview(photo)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton({ permission.launch(Manifest.permission.CAMERA) }, Modifier.weight(1f), enabled = !working) { Icon(Icons.Rounded.PhotoCamera, null); Text(" कैमरा") }
        OutlinedButton({ runCatching { gallery.launch("image/*") }.onFailure { error = "फोटो चुनने वाला ऐप उपलब्ध नहीं।" } }, Modifier.weight(1f), enabled = !working) { Icon(Icons.Rounded.PhotoLibrary, null); Text(" चुनें") }
    }
    if (working) Text("फोटो इसी फोन पर सहेज रहे हैं…", color = Forest, fontSize = 13.sp)
    if (photo.isNotBlank()) TextButton({ change("") }, enabled = !working) { Text("इस रिकॉर्ड से फोटो हटाएँ") }
    if (error.isNotBlank()) ErrorText(error)
}
