package ai.joita.biosoil.collective

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImpactForm(farmer: Farmer, fields: List<FarmField>, close: () -> Unit, save: suspend (ImpactAssessment) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("impact_drafts", android.content.Context.MODE_PRIVATE) }
    val draftKey = "farmer-${farmer.id}"
    val initial = remember(farmer.id) {
        prefs.getString(draftKey, null) ?: JSONObject().apply {
            put("farmerName", farmer.name); put("phone", farmer.phone); put("village", farmer.village)
            put("leadFarmer", if (farmer.leadFarmer) "हाँ" else "नहीं")
            put("gps", listOf(farmer.latitude, farmer.longitude).filter { it.isNotBlank() }.joinToString(", "))
            put("assessmentDate", CollectiveRules.today()); put("officer", farmer.recordedBy)
            if (fields.size == 1) { put("projectAcres", fields[0].acreage.toString()); put("cropStage", fields[0].crop) }
        }.toString()
    }
    // Every reopen reads the newest SharedPreferences draft. The form can be
    // dismissed and reopened without a stale rememberSaveable snapshot winning.
    var raw by remember(farmer.id) { mutableStateOf(initial) }
    var page by rememberSaveable(farmer.id) { mutableIntStateOf(0) }
    var consent by rememberSaveable(farmer.id) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var photoBusy by remember { mutableStateOf(setOf<String>()) }
    var error by remember { mutableStateOf("") }
    val values = remember(raw) { JSONObject(raw) }
    fun value(key: String) = values.optString(key)
    fun change(key: String, text: String) {
        raw = JSONObject(raw).put(key, text).toString()
        prefs.edit().putString(draftKey, raw).apply()
        consent = false
    }
    fun saveAssessment() {
        val answers = ImpactSchema.sections.flatMap { it.questions }.associate { it.key to value(it.key).trim() }
        val date = value("assessmentDate").trim(); val officer = value("officer").trim()
        error = ImpactSchema.validate(answers, date, officer).orEmpty()
        if (error.isNotEmpty()) {
            page = when {
                error.contains("तिथि") || error.contains("कर्मी") || error.contains("ID") || error.contains("क्षेत्र") || error.contains("फसल") -> 0
                error.contains("पम्प") || error.contains("मात्रा") || error.contains("इकाई") || error.contains("मान") -> ImpactSchema.sections.indexOfFirst { section -> section.questions.any { error.startsWith(it.label) || (error.contains("इकाई") && it.key in listOf("ecUnit", "salinityUnit", "npkUnit", "nanoUnit", "inputUnit")) } }.coerceAtLeast(0)
                else -> page
            }
            return
        }
        scope.launch {
            busy = true
            try {
                save(ImpactAssessment(farmerId = farmer.id, date = date, officer = officer, answers = answers,
                    photos = ImpactSchema.photoLabels.keys.associateWith { value("photo_$it") }.filterValues { it.isNotBlank() },
                    consentedAt = if (consent) System.currentTimeMillis() else 0))
                prefs.edit().remove(draftKey).apply()
            } catch (_: Exception) { error = "सहेज नहीं सके। मसौदा इस फोन पर सुरक्षित है; फिर कोशिश करें।" }
            finally { busy = false }
        }
    }

    val scroll = rememberScrollState()
    LaunchedEffect(page) { scroll.scrollTo(0) }
    Dialog(onDismissRequest = { if (!busy && photoBusy.isEmpty()) close() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)) {
        Surface(Modifier.fillMaxSize(), color = Paper) {
            Scaffold(
                containerColor = Paper,
                topBar = { TopAppBar(
                    title = { Column { Text("JOITA • CCF", color = Forest, fontSize = 11.sp, letterSpacing = 1.5.sp); Text("प्रभाव आकलन", fontWeight = FontWeight.Bold) } },
                    navigationIcon = { IconButton({ if (!busy && photoBusy.isEmpty()) close() }) { Icon(Icons.Rounded.Close, "बंद करें") } },
                    actions = { TextButton(::saveAssessment, enabled = !busy && photoBusy.isEmpty(), modifier = Modifier.testTag("impact-save")) { Text(if (busy) "सहेज रहे…" else "सहेजें", fontWeight = FontWeight.Bold) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                ) },
                bottomBar = { Surface(color = Color.White, shadowElevation = 10.dp) {
                    Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { if (page > 0) page-- }, enabled = page > 0, modifier = Modifier.testTag("impact-prev")) { Text("← पिछला") }
                        Text("${page + 1} / 6", color = Muted, fontSize = 12.sp)
                        if (page < 5) Button(onClick = { page++ }, modifier = Modifier.testTag("impact-next")) { Text("अगला →") }
                        else Button(onClick = ::saveAssessment, enabled = !busy && photoBusy.isEmpty(), modifier = Modifier.testTag("impact-review")) { Text("जाँचें और सहेजें") }
                    }
                } }
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${farmer.name} • ${farmer.village}", color = Muted, fontSize = 12.sp)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ImpactSchema.sections.forEachIndexed { index, section ->
                                val marker = section.title.substringBefore('.').trim()
                                FilterChip(selected = page == index, onClick = { page = index }, label = { Text(marker, fontWeight = FontWeight.SemiBold) }, modifier = Modifier.testTag("impact-section-$index"))
                            }
                        }
                        LinearProgressIndicator(progress = { (page + 1) / 6f }, modifier = Modifier.fillMaxWidth())
                    }
                    Column(Modifier.fillMaxSize().imePadding().verticalScroll(scroll).padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text("${ImpactSchema.sections[page].title}  •  ${page + 1}/6", color = Forest, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Text("मसौदा इसी फोन पर अपने-आप सहेजता है। उत्तर न मालूम हो तो खाली छोड़ें—अनुमान न लगाएँ।", color = Muted, fontSize = 13.sp)
                                if (fields.isEmpty() && page == 0) Surface(color = Color(0xFFFFF1D6), shape = RoundedCornerShape(14.dp)) {
                                    Text("इस किसान का खेत अभी दर्ज नहीं है। रिपोर्ट के लिए Farmer ID, Plot ID, क्षेत्र और फसल भरें; अगली बार खेत प्रोफाइल जोड़ें।", Modifier.padding(12.dp), color = Color(0xFF694619), fontSize = 12.sp)
                                }
                                if (page == 0) {
                                    val stage = ImpactSchema.sections.first().questions.first { it.key == "stage" }
                                    ImpactOptions(stage, value("stage")) { change("stage", it) }
                                }
                                if (page == 0) {
                                    DateInput("आकलन तिथि", value("assessmentDate")) { change("assessmentDate", it) }
                                    Input("फील्ड कर्मी का नाम", value("officer"), { change("officer", it) }, required = true)
                                    Text("किसान और खेत की पहचान जाँचें। Farmer ID / Plot ID परियोजना की साझा ID होनी चाहिए—फोन का स्थानीय क्रमांक नहीं। एक से अधिक खेत हों तो इसी आकलन वाले खेत का Plot ID और क्षेत्र भरें।", fontSize = 12.sp, color = Muted)
                                }
                                if (page == 2) Text("Soil Saathi की वास्तविक यंत्र / लैब रीडिंग ही भरें। इकाई स्रोत से देखकर लिखें; अनुमानित मान न डालें।", color = Muted, fontSize = 13.sp)
                                if (page == 4) Text("किसान का बताया प्रभाव और मापा गया प्रभाव अलग रखें। तुलना का आधार लिखें; CO₂e की गणना बाद में JOITA करेगा।", color = Muted, fontSize = 13.sp)
                                if (error.isNotBlank()) ErrorText(error)
                                ImpactSchema.sections[page].questions.filterNot { page == 0 && it.key == "stage" }.forEach { question ->
                                    key(question.key) {
                                        val answer = value(question.key)
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            when (question.kind) {
                                                "choice", "multi" -> {
                                                    ImpactOptions(question, answer, { change(question.key, it) })
                                                }
                                                "date" -> DateInput(question.label, answer) { change(question.key, it) }
                                                else -> {
                                                    Input(question.label, answer, { change(question.key, it) },
                                                        required = question.key in setOf("farmerRef", "plotRef", "projectAcres", "cropStage"),
                                                        keyboard = if (question.kind == "number") KeyboardType.Decimal else KeyboardType.Text,
                                                        lines = if (question.key in listOf("fieldNotes", "feedback", "savingDetails", "correctionRef", "readingSource")) 3 else 1,
                                                        modifier = Modifier.testTag("impact-input-${question.key}"))
                                                    quickAnswers(question.key).takeIf { it.isNotEmpty() }?.let { choices ->
                                                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            choices.forEachIndexed { index, choice -> AssistChip(onClick = { change(question.key, choice) }, label = { Text("＋ $choice", fontSize = 11.sp) }, modifier = Modifier.testTag("impact-quick-${question.key}-$index")) }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (page == 5) {
                                    Text("फोटो फील्ड का प्रमाण हैं। केवल किसान की अनुमति से लें; पहचान-पत्र / संवेदनशील निजी दस्तावेज़ न जोड़ें।", color = Muted, fontSize = 13.sp)
                                    ImpactSchema.photoLabels.forEach { (key, label) ->
                                        FormSection(label)
                                        PhotoInput(value("photo_$key"), { change("photo_$key", it) }, { photoBusy = if (it) photoBusy + key else photoBusy - key })
                                    }
                                    FormSection("किसान की स्वैच्छिक सहमति")
                                    Text(ImpactSchema.consentText, fontSize = 14.sp)
                                    Text("किसान को पढ़कर सुनाएँ। यह कर्मी का रिकॉर्ड है, डिजिटल हस्ताक्षर नहीं। चाहें तो अलग कागजी हस्ताक्षर / अंगूठा प्रमाण फोटो जोड़ें।", color = Muted, fontSize = 13.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(consent, { consent = it }, modifier = Modifier.testTag("impact-consent"))
                                        Text("किसान ने जानकारी सुनी / देखी और स्वेच्छा से सहमति दी", fontSize = 14.sp)
                                    }
                                    if (!consent) Text("सहमति दर्ज नहीं: रिकॉर्ड में यही लिखा जाएगा। साझा करने से पहले अनुमति सुनिश्चित करें।", color = Amber, fontSize = 12.sp)
                                }
                                Text("जवाब कभी भी बदलें—ऊपर के सेक्शन चुनें। ‘सहेजें’ इस आकलन को इस फोन के इतिहास में जोड़ता है।", color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImpactOptions(question: ImpactQuestion, answer: String, change: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(question.label + if (question.key == "stage") " *" else "", fontWeight = FontWeight.Medium, fontSize = 14.sp)
        Text(if (question.kind == "multi") "लागू सभी विकल्प चुनें" else "एक विकल्प चुनें", color = Muted, fontSize = 11.sp)
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            question.options.chunked(2).forEach { rowOptions ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowOptions.forEach { option ->
                        val selected = if (question.kind == "multi") option in answer.split(" | ") else answer == option
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val next = if (question.kind == "multi") {
                                    val entries = answer.split(" | ").filter { it.isNotBlank() }.toMutableSet()
                                    val unknown = setOf("अभी स्पष्ट नहीं", "कोई बदलाव स्पष्ट नहीं", "पता नहीं")
                                    if (selected) entries.remove(option) else {
                                        if (option in unknown) entries.clear() else entries.removeAll(unknown)
                                        entries.add(option)
                                    }
                                    entries.joinToString(" | ")
                                } else if (selected) "" else option
                                change(next)
                            },
                            label = { Text(option, fontSize = 12.sp) },
                            modifier = Modifier.testTag("impact-${question.key}-$option")
                        )
                    }
                }
            }
        }
    }
}

private fun quickAnswers(key: String): List<String> = when (key) {
    "cropStage" -> listOf("धान / रोपाई", "गेहूँ / बढ़वार", "चना / फूल")
    "irrigationOther" -> listOf("तालाब", "सौर पम्प", "अन्य")
    "ecUnit" -> listOf("dS/m", "µS/cm", "रिपोर्ट अनुसार")
    "salinityUnit" -> listOf("dS/m", "ppm", "रिपोर्ट अनुसार")
    "npkUnit" -> listOf("mg/kg", "kg/ha", "रिपोर्ट अनुसार")
    "nanoUnit" -> listOf("ml/एकड़", "L/एकड़", "kg/एकड़")
    "inputUnit" -> listOf("kg/एकड़", "kg/हेक्टेयर", "रु./एकड़")
    "comparisonPeriod" -> listOf("पिछला मौसम", "इसी खेत का बेसलाइन", "समान क्षेत्र")
    "farmTraceRef", "geoPhotoRef" -> listOf("GPS के साथ फोटो", "FarmTrace संदर्भ")
    else -> emptyList()
}

@Composable
internal fun ImpactHistory(assessments: List<ImpactAssessment>, add: () -> Unit) {
    var expanded by rememberSaveable { mutableLongStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(add, Modifier.fillMaxWidth().testTag("add-impact")) { Text("नया प्रभाव आकलन भरें / Fill A–F form") }
        Text("प्रभाव आकलन इतिहास (${assessments.size})", fontWeight = FontWeight.Bold, color = Forest)
        if (assessments.isEmpty()) Text("पहले बेसलाइन भरें; बाद में नया फॉलो-अप जोड़ें।", color = Muted, fontSize = 13.sp)
        assessments.forEach { a -> Card(onClick = { expanded = if (expanded == a.id) 0 else a.id }) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("#${a.id} • ${a.answers["stage"]} • ${a.date}", fontWeight = FontWeight.Bold)
                Text("फील्ड कर्मी: ${a.officer}", fontSize = 13.sp)
                Text(if (a.consentedAt > 0) "कर्मी ने सहमति दर्ज की" else "सहमति दर्ज नहीं", fontSize = 12.sp, color = Muted)
                if (expanded == a.id) {
                    ImpactSchema.sections.forEach { section ->
                        FormSection(section.title)
                        section.questions.forEach { q -> Detail(q.label, a.answers[q.key].orEmpty().ifBlank { "दर्ज नहीं" }) }
                    }
                    a.photos.forEach { (key, path) -> Text(ImpactSchema.photoLabels[key].orEmpty()); PhotoPreview(path) }
                } else Text("पूरा विवरण देखने के लिए दबाएँ", fontSize = 12.sp, color = Forest)
            }
        } }
    }
}
