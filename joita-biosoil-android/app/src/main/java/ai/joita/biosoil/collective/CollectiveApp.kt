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
                notify("PDF saved to your chosen folder")
            } catch (_: Exception) { notify("Could not save PDF. Choose another folder and retry.") }
            finally { exportBusy = false }
        }
    }
    val saveEvidence = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            exportBusy = true
            try {
                withContext(Dispatchers.IO) { requireNotNull(context.contentResolver.openOutputStream(uri)).use { out -> File(reportPath).inputStream().use { it.copyTo(out) } } }
                notify("Evidence folder saved")
            } catch (_: Exception) { notify("Could not save. Please choose another folder.") }
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
                    val pdf = CollectivePdfService.createFarmerReport(context, value, fields, visits)
                    if (evidence) CollectiveEvidence.create(context, value, fields, visits, pdf) else pdf
                }
                reportPath = file.absolutePath
                if (evidence) saveEvidence.launch(file.name) else if (share) CollectivePdfService.share(context, file, value) else savePdf.launch(file.name)
            } catch (_: Exception) { notify("Could not prepare the report. Your records are still saved.") }
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
                    navigationIcon = { IconButton(back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                    actions = { IconButton({ if (field == null) edit("farmer", farmer.id) else edit("field", field.id) }) { Icon(Icons.Rounded.Edit, "Edit record") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper),
                )
            },
            bottomBar = {
                if (farmerId == 0L) NavigationBar(containerColor = Color.White) {
                    listOf("Home" to Icons.Rounded.SpaceDashboard, "Farmers" to Icons.Rounded.Groups,
                        "Visits" to Icons.Rounded.EventNote, "Reports" to Icons.Rounded.Description).forEachIndexed { index, item ->
                        NavigationBarItem(tab == index, { tab = index }, { Icon(item.second, null) }, label = { Text(item.first) })
                    }
                }
            },
            floatingActionButton = {
                if (loaded && farmerId == 0L && tab < 2) ExtendedFloatingActionButton(
                    onClick = { edit("farmer") }, containerColor = Forest, contentColor = Color.White,
                    icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Add farmer") },
                )
            },
        ) { padding ->
            when {
                loadError -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Could not open your records")
                        Button({ reload() }) { Text("Retry") }
                    }
                }
                !loaded -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                farmer != null && field != null -> FieldScreen(data, field, padding, { edit("visit") }, { edit("visit", it.id) })
                farmer != null -> ProfileScreen(data, farmer, padding,
                    { fieldId = it.id }, { edit("field") }, { reportFarmerId = farmer.id },
                    { intent -> runCatching { context.startActivity(intent) }.onFailure { notify("No app is available for this action") } })
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
            title = { Text("Farmer report") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(reportFarmer.name, fontWeight = FontWeight.Bold)
                Text("Profile, all linked fields, visit history and field photos in one PDF.")
                if (exportBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Button({ export(reportFarmer, false) }, Modifier.fillMaxWidth(), enabled = !exportBusy) { Text("Save PDF to phone") }
                OutlinedButton({ export(reportFarmer, true) }, Modifier.fillMaxWidth(), enabled = !exportBusy) { Text("Share PDF") }
                OutlinedButton({ export(reportFarmer, false, true) }, Modifier.fillMaxWidth(), enabled = !exportBusy) { Text("Save evidence folder (.zip)") }
            } },
            confirmButton = { TextButton({ reportFarmerId = 0 }, enabled = !exportBusy) { Text("Done") } },
        )
        when (form) {
            "farmer" -> FarmerForm(data.farmers.find { it.id == editId }, { form = "" }) { value ->
                val id = withContext(Dispatchers.IO) { repo.addFarmer(value) }
                refresh(); farmerId = id; fieldId = 0; form = ""; notify("Farmer saved on this phone")
            }
            "field" -> if (farmer != null) FieldForm(farmer.id, data.fields.find { it.id == editId }, { form = "" }) { value ->
                val id = withContext(Dispatchers.IO) { repo.addField(value) }
                refresh(); fieldId = id; form = ""; notify("Field saved")
            }
            "visit" -> if (field != null) VisitForm(field.id, data.visits.find { it.id == editId }, { form = "" }) { value ->
                withContext(Dispatchers.IO) { repo.addVisit(value) }
                refresh(); form = ""; notify("Visit saved")
            }
        }
    }
}

@Composable
private fun Dashboard(data: CollectiveSnapshot, padding: PaddingValues, all: () -> Unit, open: (Farmer) -> Unit, visit: (FieldVisit) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("JOITA", letterSpacing = 3.sp, fontWeight = FontWeight.ExtraBold, color = Forest); Text("Farmer Collective", fontSize = 13.sp, color = Muted) }
            BadgeLabel("On this phone", Icons.Rounded.OfflinePin)
        } }
        item { Card(shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Forest, Color(0xFF287258)))).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.Spa, null, tint = Color(0xFFB9D9B1), modifier = Modifier.size(32.dp))
                Text("Rooted in your\ncommunity.", color = Color.White, fontSize = 30.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold)
                Text("Every farmer. Every field.\nA clearer picture, one visit at a time.", color = Color(0xFFD1E6D8), fontSize = 14.sp)
                Text(CollectiveRules.today(), color = Color(0xFFD1E6D8), fontSize = 12.sp)
            }
        } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("Farmers", data.farmers.size.toString(), Icons.Rounded.Groups, Modifier.weight(1f))
            Metric("Acres", CollectiveRules.acres(data.acres), Icons.Rounded.Landscape, Modifier.weight(1f))
            Metric("Leads", data.farmers.count { it.leadFarmer }.toString(), Icons.Rounded.Stars, Modifier.weight(1f))
        } }
        item { Surface(shape = RoundedCornerShape(20.dp), color = Moss) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Growing together", fontWeight = FontWeight.Bold, color = Forest)
            Text("Program goals · 200 farmers / 200 acres / 50 leads", fontSize = 12.sp, color = Muted)
            LinearProgressIndicator(progress = { (data.farmers.size / 200f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = Forest)
            Text("${data.fields.size} fields · ${data.visits.size} visits recorded", fontSize = 13.sp)
        } } }
        if (data.fields.any { field -> data.visitsFor(field.id).isEmpty() }) item {
            Text("${data.fields.count { data.visitsFor(it.id).isEmpty() }} fields awaiting their first visit", color = Amber, fontWeight = FontWeight.Medium)
        }
        item { SectionTitle("Your farmers", "${data.farmers.size} registered", all) }
        if (data.farmers.isEmpty()) item { EmptyState("Start with one farmer", "Tap Add farmer to begin. Records are available even without a signal.", Icons.Rounded.Groups) }
        items(data.farmers.sortedByDescending { it.createdAt }.take(3), key = { "f${it.id}" }) { FarmerCard(it, data, { open(it) }) }
        if (data.visits.isNotEmpty()) {
            item { Text("Latest field activity", style = MaterialTheme.typography.titleLarge) }
            items(data.recentVisits().take(3), key = { "v${it.id}" }) { ActivityCard(it, data, { visit(it) }) }
        }
        item { Spacer(Modifier.height(78.dp)) }
    }
}

@Composable
private fun FarmerList(data: CollectiveSnapshot, padding: PaddingValues, open: (Farmer) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All farmers") }
    val farmers = data.farmers.filter { f ->
        val fields = data.fieldsFor(f.id)
        (query.isBlank() || listOf(f.name, f.village, f.phone).plus(fields.map { it.crop }).any { it.contains(query.trim(), true) }) &&
            (filter != "Lead farmers" || f.leadFarmer) && (filter != "No fields yet" || fields.isEmpty())
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
        PageTitle("Farmer register", "People at the heart of the program")
        SearchBox(query, { query = it }, "Name, village, phone or crop")
        ChipRow(listOf("All farmers", "Lead farmers", "No fields yet"), filter) { filter = it }
        Text("${farmers.size} farmers", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 110.dp)) {
            if (farmers.isEmpty()) item { EmptyState("No farmers found", "Try a different search or add your first farmer.", Icons.Rounded.Search) }
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
                Text("${fields.size} fields · ${CollectiveRules.acres(fields.sumOf { it.acreage })} acres", color = Forest, fontSize = 12.sp)
            }
            if (f.leadFarmer) Icon(Icons.Rounded.Stars, "Lead farmer", tint = Amber, modifier = Modifier.size(20.dp))
            Icon(Icons.Rounded.ChevronRight, null, tint = Muted)
        }
    }
}

@Composable
private fun ProfileScreen(data: CollectiveSnapshot, farmer: Farmer, padding: PaddingValues, open: (FarmField) -> Unit, add: () -> Unit, report: () -> Unit, launch: (Intent) -> Unit) {
    val fields = data.fieldsFor(farmer.id)
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Surface(color = Forest, shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (farmer.leadFarmer) "LEAD FARMER" else "FARMER PROFILE", color = Color(0xFFCCDFBE), letterSpacing = 2.sp, fontSize = 11.sp)
                Text(farmer.name, fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("${fields.size} fields    /    ${CollectiveRules.acres(fields.sumOf { it.acreage })} acres", color = Color.White)
            }
        } }
        item { DetailCard(listOf("Village" to farmer.village, "Phone" to farmer.phone.ifBlank { "Not recorded" }, "Land arrangement" to farmer.tenure, "Family & farming notes" to farmer.notes)) }
        if (farmer.photoPath.isNotBlank()) item { PhotoPreview(farmer.photoPath) }
        item { DetailCard(listOf("Registration reference" to "JOITA-${farmer.id}", "Recorded by" to farmer.recordedBy.ifBlank { "Not recorded" }, "Acknowledgement" to if (farmer.acknowledgedAt > 0) "Officer recorded farmer agreement on ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(farmer.acknowledgedAt))}" else "Not recorded")) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (farmer.phone.isNotBlank()) OutlinedButton({ launch(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", farmer.phone, null))) }) { Icon(Icons.Rounded.Phone, null, Modifier.size(18.dp)); Text(" Call") }
            if (farmer.latitude.isNotBlank() && CollectiveRules.validCoordinates(farmer.latitude, farmer.longitude)) OutlinedButton({
                launch(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${farmer.latitude},${farmer.longitude}?q=${farmer.latitude},${farmer.longitude}")))
            }) { Icon(Icons.Rounded.LocationOn, null, Modifier.size(18.dp)); Text(" Map") }
        } }
        item { Button(report, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Icon(Icons.Rounded.PictureAsPdf, null); Spacer(Modifier.width(8.dp)); Text("Save or share farmer PDF") } }
        item { SectionTitle("Fields", "Crop and land records", add, "Add field") }
        if (fields.isEmpty()) item { EmptyState("Add the first field", "Capture acreage, crop and growing conditions.", Icons.Rounded.Landscape) }
        items(fields, key = { it.id }) { field ->
            Card(onClick = { open(field) }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Grass, null, tint = Forest)
                        Text(field.crop, Modifier.weight(1f).padding(horizontal = 10.dp), style = MaterialTheme.typography.titleLarge)
                        Text("${CollectiveRules.acres(field.acreage)} ac", color = Forest, fontWeight = FontWeight.Bold)
                    }
                    Text(listOf(field.variety, field.season).filter { it.isNotBlank() }.joinToString(" · "), color = Muted)
                    val visits = data.visitsFor(field.id)
                    Text(if (visits.isEmpty()) "First visit not recorded" else "${visits.size} visits · Last ${visits.first().date}", fontSize = 12.sp, color = if (visits.isEmpty()) Amber else Forest)
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
            Metric("Acres", CollectiveRules.acres(field.acreage), Icons.Rounded.Landscape, Modifier.weight(1f))
            Metric("Visits", visits.size.toString(), Icons.Rounded.EventNote, Modifier.weight(1f))
        } }
        item { DetailCard(listOf("Season" to field.season, "Variety" to field.variety, "Sowing date" to field.sowingDate, "Soil type" to field.soilType, "Irrigation" to field.irrigation, "Inputs" to field.inputs, "Location / boundary" to field.boundaryNotes)) }
        item { Button(add, Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Icon(Icons.Rounded.Add, null); Text(" Record field visit") } }
        item { Text("Field journal", style = MaterialTheme.typography.titleLarge) }
        if (visits.isEmpty()) item { EmptyState("Your field story starts here", "Record growth, observations and recommendations from your next visit.", Icons.Rounded.EventNote) }
        items(visits, key = { it.id }) { v ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(v.date, fontWeight = FontWeight.Bold, color = Forest); Text(v.officer, color = Muted, fontSize = 12.sp) }
                        IconButton({ edit(v) }) { Icon(Icons.Rounded.Edit, "Edit visit") }
                    }
                    if (v.cropStage.isNotBlank()) BadgeLabel(v.cropStage, Icons.Rounded.Grass)
                    listOf("Observations" to v.observations, "Pest / disease" to v.issues, "Recommendations" to v.recommendations, "Yield / harvest" to v.yieldData, "Notes" to v.notes).filter { it.second.isNotBlank() }.forEach { Detail(it.first, it.second) }
                    if (v.photoPath.isNotBlank()) PhotoPreview(v.photoPath)
                }
            }
        }
    }
}

@Composable
private fun VisitList(data: CollectiveSnapshot, padding: PaddingValues, open: (FieldVisit) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All visits") }
    val visits = data.recentVisits().filter { v ->
        val f = data.farmerFor(v)
        listOf(v.officer, v.observations, v.issues, f?.name.orEmpty(), f?.village.orEmpty()).any { it.contains(query, true) } &&
            (filter != "Issues recorded" || v.issues.isNotBlank()) && (filter != "With photos" || v.photoPath.isNotBlank())
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
        PageTitle("Field journal", "${data.visits.size} visits across your collective")
        SearchBox(query, { query = it }, "Farmer, officer or observation")
        ChipRow(listOf("All visits", "Issues recorded", "With photos"), filter) { filter = it }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
            if (visits.isEmpty()) item { EmptyState("No visits to show", "Open a farmer’s field to record your first visit.", Icons.Rounded.EventNote) }
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
                Text(data.farmerFor(v)?.name ?: "Field visit", fontWeight = FontWeight.SemiBold)
                Text("${v.date} · ${v.officer}", color = Muted, fontSize = 12.sp)
                Text(v.observations.ifBlank { v.cropStage.ifBlank { "Open visit details" } }, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (v.issues.isNotBlank()) Text("Issue recorded", fontSize = 12.sp, color = Amber, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Muted)
        }
    }
}

@Composable
private fun ReportsScreen(data: CollectiveSnapshot, padding: PaddingValues, report: (Farmer) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageTitle("Reports", "Your field work, ready to share") }
        item { Surface(color = Moss, shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.PictureAsPdf, null, tint = Forest, modifier = Modifier.size(32.dp))
            Text("A complete farmer record", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Forest)
            Text("Save a PDF to Downloads or share it with your team. Includes field details, every visit and photos.", color = Muted)
        } } }
        item { SearchBox(query, { query = it }, "Find a farmer for their report") }
        if (data.farmers.isEmpty()) item { EmptyState("Reports grow with your records", "Add a farmer and field to prepare your first PDF.", Icons.Rounded.Description) }
        items(data.farmers.filter { it.name.contains(query, true) || it.village.contains(query, true) }, key = { it.id }) { f ->
            Card(onClick = { report(f) }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(f.name, fontWeight = FontWeight.Bold); Text("${data.fieldsFor(f.id).size} fields · ${f.village}", color = Muted, fontSize = 12.sp) }
                    Icon(Icons.Rounded.FileDownload, "Create PDF", tint = Forest)
                }
            }
        }
        item { Text("Version 1.1.0 · Saved on this phone\nCloud sync is not configured. Export important reports before changing phones.", fontSize = 12.sp, color = Muted, modifier = Modifier.padding(vertical = 16.dp)) }
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
@Composable private fun SectionTitle(title: String, subtitle: String, action: () -> Unit, label: String = "View all") {
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
        trailingIcon = { if (query.isNotEmpty()) IconButton({ change("") }) { Icon(Icons.Rounded.Close, "Clear search") } })
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
    var tenure by rememberSaveable { mutableStateOf(initial?.tenure ?: "Owned") }
    var photo by rememberSaveable { mutableStateOf(initial?.photoPath.orEmpty()) }
    var recordedBy by rememberSaveable { mutableStateOf(initial?.recordedBy.orEmpty()) }
    var acknowledged by rememberSaveable { mutableStateOf(false) }
    var photoBusy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    fun readLocation() {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = manager.getProviders(true).mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }.filter { android.os.SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos in 0..120_000_000_000L }.maxByOrNull { it.time }
        if (location == null) error = "No recent GPS fix. Open Maps to get a fix, then retry, or enter coordinates manually."
        else { lat = "%.6f".format(java.util.Locale.US, location.latitude); lng = "%.6f".format(java.util.Locale.US, location.longitude); error = "" }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) readLocation() else error = "Location permission was not granted. You can enter coordinates manually."
    }
    FormScreen(if (initial == null) "Add farmer" else "Edit farmer", "Farmer profile", close, busy || photoBusy, {
        error = when {
            name.isBlank() -> "Enter the farmer’s name."
            village.isBlank() -> "Enter the village."
            !CollectiveRules.validPhone(phone) -> "Enter a valid phone number."
            !CollectiveRules.validCoordinates(lat, lng) -> "Enter both coordinates in valid ranges."
            acknowledged && recordedBy.isBlank() -> "Enter the officer’s name to record acknowledgement."
            else -> ""
        }
        if (error.isEmpty()) scope.launch { busy = true; try { save(Farmer(initial?.id ?: 0, name.trim(), phone.trim(), village.trim(), lat.trim(), lng.trim(), lead, notes.trim(), tenure, initial?.createdAt ?: System.currentTimeMillis(), photo, recordedBy.trim(), if (acknowledged) System.currentTimeMillis() else 0)) } catch (_: Exception) { error = "Could not save. Please try again." } finally { busy = false } }
    }) {
        if (error.isNotEmpty()) ErrorText(error)
        FormSection("Identity and contact")
        Input("Farmer name", name, { name = it }, required = true)
        Input("Phone number", phone, { phone = it }, keyboard = KeyboardType.Phone)
        Input("Village", village, { village = it }, required = true)
        FormSection("Farmer photo")
        Text("Ask the farmer before taking or storing their photo.", fontSize = 13.sp, color = Muted)
        PhotoInput(photo, { photo = it }, { photoBusy = it })
        Input("Registering officer", recordedBy, { recordedBy = it })
        FormSection("Land and program")
        Choice("Land arrangement", listOf("Owned", "Tenant", "Shared"), tenure) { tenure = it }
        Row(verticalAlignment = Alignment.CenterVertically) { Switch(lead, { lead = it }); Column(Modifier.padding(start = 12.dp)) { Text("Lead farmer", fontWeight = FontWeight.Medium); Text("Supports nearby farmers", fontSize = 12.sp, color = Muted) } }
        FormSection("Location")
        OutlinedButton(onClick = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) readLocation()
            else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.MyLocation, null); Text(if (lat.isBlank()) " Capture current location" else " Update current location") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.weight(1f)) { Input("Latitude", lat, { lat = it }, keyboard = KeyboardType.Decimal) }; Box(Modifier.weight(1f)) { Input("Longitude", lng, { lng = it }, keyboard = KeyboardType.Decimal) } }
        FormSection("Context")
        Input("Family and farming notes", notes, { notes = it }, lines = 4)
        FormSection("Farmer acknowledgement")
        Text("Read back the profile and explain that Joita stores these details and photos on this phone for program registration. Tick only after the farmer agrees. This is an officer’s record, not a digital signature.", fontSize = 13.sp, color = Muted)
        if (initial?.acknowledgedAt != null && initial.acknowledgedAt > 0) Text("Saving edits requires recording agreement again for the updated profile.", fontSize = 13.sp, color = Amber)
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(acknowledged, { acknowledged = it }); Text("Farmer reviewed the profile and agreed to this registration", fontSize = 14.sp) }
        if (error.isNotEmpty()) ErrorText(error)
    }
}

@Composable
private fun FieldForm(farmerId: Long, initial: FarmField?, close: () -> Unit, save: suspend (FarmField) -> Unit) {
    val scope = rememberCoroutineScope()
    var area by rememberSaveable { mutableStateOf(initial?.let { java.math.BigDecimal.valueOf(it.acreage).stripTrailingZeros().toPlainString() }.orEmpty()) }
    var crop by rememberSaveable { mutableStateOf(initial?.crop.orEmpty()) }
    var variety by rememberSaveable { mutableStateOf(initial?.variety.orEmpty()) }
    var season by rememberSaveable { mutableStateOf(initial?.season ?: "Kharif") }
    var sowing by rememberSaveable { mutableStateOf(initial?.sowingDate.orEmpty()) }
    var soil by rememberSaveable { mutableStateOf(initial?.soilType.orEmpty()) }
    var irrigation by rememberSaveable { mutableStateOf(initial?.irrigation.orEmpty()) }
    var inputs by rememberSaveable { mutableStateOf(initial?.inputs.orEmpty()) }
    var boundary by rememberSaveable { mutableStateOf(initial?.boundaryNotes.orEmpty()) }
    var error by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
    FormScreen(if (initial == null) "Add field" else "Edit field", "Crop and land", close, busy, {
        error = when { !CollectiveRules.validArea(area) -> "Enter a valid acreage greater than zero."; crop.isBlank() -> "Enter the crop name."; sowing.isNotBlank() && CollectiveRules.dateMillis(sowing) == null -> "Use DD-MM-YYYY for the sowing date."; else -> "" }
        if (error.isEmpty()) scope.launch { busy = true; try { save(FarmField(initial?.id ?: 0, farmerId, area.toDouble(), crop.trim(), variety.trim(), season, sowing.trim(), soil.trim(), irrigation.trim(), inputs.trim(), boundary.trim(), initial?.createdAt ?: System.currentTimeMillis())) } catch (_: Exception) { error = "Could not save. Please try again." } finally { busy = false } }
    }) {
        FormSection("Field identity")
        Input("Acreage", area, { area = it }, required = true, keyboard = KeyboardType.Decimal)
        Input("Crop", crop, { crop = it }, required = true)
        Input("Variety", variety, { variety = it })
        Choice("Season", listOf("Kharif", "Rabi", "Zaid", "Perennial"), season) { season = it }
        DateInput("Sowing date", sowing, { sowing = it })
        FormSection("Growing conditions")
        Input("Soil type", soil, { soil = it })
        Input("Irrigation", irrigation, { irrigation = it })
        Input("Inputs used", inputs, { inputs = it }, lines = 3)
        Input("Location and boundary notes", boundary, { boundary = it }, lines = 3)
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
    FormScreen(if (initial == null) "Record visit" else "Edit visit", "Field journal", close, busy || photoBusy, {
        error = when { CollectiveRules.dateMillis(date) == null -> "Use DD-MM-YYYY for the visit date."; officer.isBlank() -> "Enter the field officer’s name."; else -> "" }
        if (error.isEmpty()) scope.launch { busy = true; try { save(FieldVisit(initial?.id ?: 0, fieldId, date, officer.trim(), stage.trim(), observations.trim(), issues.trim(), recommendations.trim(), yieldData.trim(), notes.trim(), photo, initial?.createdAt ?: System.currentTimeMillis())) } catch (_: Exception) { error = "Could not save. Please try again." } finally { busy = false } }
    }) {
        FormSection("Visit details")
        DateInput("Visit date", date, { date = it })
        Input("Field officer", officer, { officer = it }, required = true)
        Input("Crop stage", stage, { stage = it })
        FormSection("Field observations")
        Input("Observations", observations, { observations = it }, lines = 4)
        Input("Pest or disease issues", issues, { issues = it }, lines = 3)
        Input("Recommendations", recommendations, { recommendations = it }, lines = 4)
        Input("Yield or harvest data", yieldData, { yieldData = it }, lines = 3)
        Input("Climate activity and evidence notes", notes, { notes = it }, lines = 4)
        Text("Record the activity/date, material quantity and unit, source or batch, method, plot reference and witness. Add photos and keep receipts separately.", fontSize = 13.sp, color = Muted)
        FormSection("Photo evidence")
        PhotoInput(photo, { photo = it }, { photoBusy = it })
        if (error.isNotEmpty()) ErrorText(error)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormScreen(title: String, eyebrow: String, close: () -> Unit, busy: Boolean, save: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = { if (!busy) close() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)) {
        Surface(Modifier.fillMaxSize(), color = Paper) {
            Scaffold(containerColor = Paper, topBar = { TopAppBar(title = { Column { Text(eyebrow, color = Forest, fontSize = 11.sp, letterSpacing = 1.5.sp); Text(title, fontWeight = FontWeight.Bold) } }, navigationIcon = { IconButton({ if (!busy) close() }) { Icon(Icons.Rounded.Close, "Close") } }, actions = { TextButton(save, enabled = !busy) { Text("SAVE", fontWeight = FontWeight.Bold) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)) }) { padding ->
                LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = content) }; item { Spacer(Modifier.height(30.dp)) } }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = padding.calculateTopPadding()))
            }
        }
    }
}

@Composable private fun FormSection(value: String) { Text(value.uppercase(), color = Forest, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.4.sp, modifier = Modifier.padding(top = 7.dp)) }
@Composable private fun Input(label: String, value: String, change: (String) -> Unit, required: Boolean = false, lines: Int = 1, keyboard: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(value, change, Modifier.fillMaxWidth(), label = { Text(label + if (required) " *" else "") }, singleLine = lines == 1, minLines = lines, keyboardOptions = KeyboardOptions(keyboardType = keyboard), shape = RoundedCornerShape(14.dp))
}
@Composable private fun Choice(label: String, values: List<String>, selected: String, change: (String) -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(label, fontSize = 13.sp, color = Muted); ChipRow(values, selected, change) } }
@Composable private fun ErrorText(value: String) { Text(value, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
@Composable private fun PhotoPreview(path: String) {
    var bitmap by remember(path) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(path) { bitmap = withContext(Dispatchers.IO) { CollectivePhotos.decode(path) } }
    Surface(shape = RoundedCornerShape(16.dp), color = Moss, modifier = Modifier.fillMaxWidth().height(220.dp)) {
        if (bitmap == null) Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Image, "Field photo", tint = Forest) }
        else androidx.compose.foundation.Image(bitmap!!.asImageBitmap(), "Field photo", Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
    }
    // Compose may draw a previous frame after disposal; allow GC to release these bounded thumbnails.
}

@Composable private fun DateInput(label: String, value: String, change: (String) -> Unit) {
    val context = LocalContext.current
    OutlinedTextField(value, change, Modifier.fillMaxWidth(), label = { Text("$label (DD-MM-YYYY)") }, singleLine = true, shape = RoundedCornerShape(14.dp), trailingIcon = {
        IconButton({
            val date = Calendar.getInstance().apply { CollectiveRules.dateMillis(value)?.let { timeInMillis = it } }
            DatePickerDialog(context, { _, year, month, day -> change(String.format(Locale.US, "%02d-%02d-%04d", day, month + 1, year)) }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show()
        }) { Icon(Icons.Rounded.DateRange, "Choose date") }
    })
}

@Composable private fun PhotoInput(photo: String, change: (String) -> Unit, onBusy: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by rememberSaveable { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    fun import(uri: Uri, source: File? = null) {
        working = true; onBusy(true)
        scope.launch {
            try { change(withContext(Dispatchers.IO) { CollectivePhotos.import(context, uri, if (source == null) "Selected image" else "Camera capture") }); error = "" }
            catch (_: Exception) { error = "Could not read this photo. Choose a readable image smaller than 25 MB." }
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
        }.onFailure { error = "Camera is unavailable. Use Choose photo instead." }
        else error = "Camera access was not granted. You can choose a photo instead."
    }
    if (photo.isNotBlank()) PhotoPreview(photo)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton({ permission.launch(Manifest.permission.CAMERA) }, Modifier.weight(1f), enabled = !working) { Icon(Icons.Rounded.PhotoCamera, null); Text(" Camera") }
        OutlinedButton({ runCatching { gallery.launch("image/*") }.onFailure { error = "No photo chooser is installed." } }, Modifier.weight(1f), enabled = !working) { Icon(Icons.Rounded.PhotoLibrary, null); Text(" Choose") }
    }
    if (working) Text("Saving photo on this phone…", color = Forest, fontSize = 13.sp)
    if (photo.isNotBlank()) TextButton({ change("") }, enabled = !working) { Text("Remove photo from record") }
    if (error.isNotBlank()) ErrorText(error)
}
