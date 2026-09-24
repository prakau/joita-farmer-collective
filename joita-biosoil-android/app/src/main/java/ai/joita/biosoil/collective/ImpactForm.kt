package ai.joita.biosoil.collective

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun ImpactForm(farmer: Farmer, fields: List<FarmField>, close: () -> Unit, save: suspend (ImpactAssessment) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("impact_drafts", android.content.Context.MODE_PRIVATE) }
    val draftKey = "farmer-${farmer.id}"
    val initial = remember(farmer.id) {
        prefs.getString(draftKey, null) ?: JSONObject().apply {
            put("farmerRef", "JOITA-${farmer.id}"); put("farmerName", farmer.name); put("phone", farmer.phone); put("village", farmer.village)
            put("leadFarmer", if (farmer.leadFarmer) "हाँ" else "नहीं")
            put("gps", listOf(farmer.latitude, farmer.longitude).filter { it.isNotBlank() }.joinToString(", "))
            put("assessmentDate", CollectiveRules.today()); put("officer", farmer.recordedBy)
            if (fields.size == 1) { put("plotRef", "JOITA-FIELD-${fields[0].id}"); put("projectAcres", fields[0].acreage.toString()); put("cropStage", fields[0].crop) }
        }.toString()
    }
    var raw by rememberSaveable(farmer.id) { mutableStateOf(initial) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var consent by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var photoBusy by remember { mutableStateOf(setOf<String>()) }
    var error by remember { mutableStateOf("") }
    val values = remember(raw) { JSONObject(raw) }
    fun value(key: String) = values.optString(key)
    fun change(key: String, text: String) {
        raw = JSONObject(raw).put(key, text).toString()
        prefs.edit().putString(draftKey, raw).apply()
        // A changed response must be read back again before acknowledgement.
        consent = false
    }
    FormScreen("प्रभाव आकलन", farmer.name, close, busy || photoBusy.isNotEmpty(), {
        val answers = ImpactSchema.sections.flatMap { it.questions }.associate { it.key to value(it.key).trim() }
        val date = value("assessmentDate").trim(); val officer = value("officer").trim()
        error = ImpactSchema.validate(answers, date, officer).orEmpty()
        if (error.isEmpty()) scope.launch {
            busy = true
            try {
                save(ImpactAssessment(farmerId = farmer.id, date = date, officer = officer, answers = answers,
                    photos = ImpactSchema.photoLabels.keys.associateWith { value("photo_$it") }.filterValues { it.isNotBlank() },
                    consentedAt = if (consent) System.currentTimeMillis() else 0))
                prefs.edit().remove(draftKey).apply()
            } catch (_: Exception) { error = "सहेज नहीं सके। मसौदा इस फोन पर सुरक्षित है; फिर कोशिश करें।" }
            finally { busy = false }
        }
    }, scrollKey = page) {
        Text("आपके दिए JOITA / CCF प्रपत्र के अनुसार • ${page + 1}/6", color = Forest, fontSize = 12.sp)
        Text("मसौदा अपने-आप इसी फोन पर सहेजता है। अज्ञात उत्तर खाली छोड़ें। सहेजे गए आकलन को बदलने के लिए नया ‘सुधार’ रिकॉर्ड बनाएँ।", color = Muted, fontSize = 13.sp)
        if (fields.isEmpty()) {
            Surface(color = Color(0xFFFFF1D6), shape = RoundedCornerShape(14.dp)) {
                Text("इस किसान का कोई field अभी नहीं है। A सेक्शन में Project Acres, Plot ID और Crop / Stage खुद भरें, या पहले किसान प्रोफाइल से Add field करें। इनके बिना impact report सहेजी नहीं जाएगी।", Modifier.padding(12.dp), color = Color(0xFF694619), fontSize = 12.sp)
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ImpactSchema.sections.forEachIndexed { index, section -> FilterChip(page == index, { page = index }, label = { Text(section.title.substringBefore('.')) }) }
        }
        LinearProgressIndicator(progress = { (page + 1) / 6f }, modifier = Modifier.fillMaxWidth())
        if (error.isNotBlank()) ErrorText(error)
        FormSection(ImpactSchema.sections[page].title)
        if (page == 0) {
            DateInput("आकलन तिथि", value("assessmentDate")) { change("assessmentDate", it) }
            Input("फील्ड कर्मी का नाम", value("officer"), { change("officer", it) }, required = true)
            Text("प्रोफाइल से आई जानकारी की पुष्टि करें। कई खेत होने पर संबंधित Plot ID और आकलित क्षेत्र भरें।", fontSize = 12.sp, color = Muted)
        }
        if (page == 2) Text("वास्तविक यंत्र / लैब रीडिंग भरें; अनुमान न लगाएँ। EC, लवणता और N/P/K की इकाई की पुष्टि करें।", color = Muted, fontSize = 13.sp)
        if (page == 4) Text("किसान के बताए प्रभाव और मापा गया प्रभाव अलग हो सकते हैं। तुलना का आधार लिखें। अंतिम CO₂e गणना JOITA द्वारा अलग से की जाएगी।", color = Muted, fontSize = 13.sp)
        ImpactSchema.sections[page].questions.forEach { question -> key(question.key) {
            val answer = value(question.key)
            when (question.kind) {
                "choice", "multi" -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(question.label, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    question.options.forEach { option ->
                        val selected = if (question.kind == "multi") option in answer.split(" | ") else answer == option
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Checkbox(selected, {
                                val next = if (question.kind == "multi") {
                                    val entries = answer.split(" | ").filter { it.isNotBlank() }.toMutableSet()
                                    val unknown = setOf("अभी स्पष्ट नहीं", "कोई बदलाव स्पष्ट नहीं")
                                    if (!selected) { if (option in unknown) entries.clear() else entries.removeAll(unknown) }
                                    if (selected) entries.remove(option) else entries.add(option)
                                    entries.joinToString(" | ")
                                } else if (selected) "" else option
                                change(question.key, next)
                            }, modifier = Modifier.testTag("impact-${question.key}-$option"))
                            Text(option, fontSize = 14.sp)
                        }
                    }
                }
                "date" -> DateInput(question.label, answer) { change(question.key, it) }
                else -> Input(question.label, answer, { change(question.key, it) },
                    keyboard = if (question.kind == "number") KeyboardType.Decimal else KeyboardType.Text,
                    lines = if (question.key in listOf("fieldNotes", "feedback", "savingDetails", "correctionRef")) 3 else 1)
            }
        } }
        if (page == 5) {
            ImpactSchema.photoLabels.forEach { (key, label) ->
                FormSection(label)
                PhotoInput(value("photo_$key"), { change("photo_$key", it) }, { photoBusy = if (it) photoBusy + key else photoBusy - key })
            }
            FormSection("किसान की स्वैच्छिक सहमति")
            Text(ImpactSchema.consentText, fontSize = 14.sp)
            Text("पहले किसान को पूरा विवरण पढ़कर सुनाएँ। यह फील्ड कर्मी का रिकॉर्ड है, डिजिटल हस्ताक्षर नहीं। जरूरी हो तो कागज पर हस्ताक्षर / अंगूठा लेकर ऊपर फोटो जोड़ें।", color = Muted, fontSize = 13.sp)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(consent, { consent = it })
                Text("किसान ने जानकारी सुनी / देखी और स्वेच्छा से सहमति दी", fontSize = 14.sp)
            }
            if (!consent) Text("सहमति दर्ज नहीं: रिकॉर्ड में यही लिखा जाएगा। साझा करने से पहले अनुमति सुनिश्चित करें।", color = Amber, fontSize = 12.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton({ page-- }, enabled = page > 0) { Text("पिछला") }
            if (page < 5) Button({ page++ }) { Text("अगला") }
            else Text("ऊपर ‘सहेजें’ दबाएँ", color = Forest, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun ImpactHistory(assessments: List<ImpactAssessment>, add: () -> Unit) {
    var expanded by rememberSaveable { mutableLongStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(add, Modifier.fillMaxWidth().testTag("add-impact")) { Text("नया प्रभाव आकलन भरें") }
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
