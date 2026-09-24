package ai.joita.biosoil.collective

data class ImpactAssessment(
    val id: Long = 0,
    val farmerId: Long,
    val date: String,
    val officer: String,
    val answers: Map<String, String>,
    val photos: Map<String, String> = emptyMap(),
    val consentedAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

data class ImpactQuestion(
    val key: String, val label: String, val kind: String = "text",
    val options: List<String> = emptyList(), val min: Double? = null, val max: Double? = null,
)
data class ImpactSection(val title: String, val questions: List<ImpactQuestion>)

/** Stable keys keep exports compatible when Hindi labels evolve. No answers are inferred. */
object ImpactSchema {
    private fun q(key: String, label: String) = ImpactQuestion(key, label)
    private fun n(key: String, label: String, min: Double? = 0.0, max: Double? = null) = ImpactQuestion(key, label, "number", min = min, max = max)
    private fun c(key: String, label: String, vararg options: String) = ImpactQuestion(key, label, "choice", options.toList())
    private fun m(key: String, label: String, vararg options: String) = ImpactQuestion(key, label, "multi", options.toList())
    private fun d(key: String, label: String) = ImpactQuestion(key, label, "date")
    val stages = listOf("बेसलाइन", "फॉलो-अप", "अंतिम आकलन", "सुधार / स्पष्टीकरण")
    val sections = listOf(
        ImpactSection("A. किसान एवं खेत", listOf(
            c("stage", "आकलन का चरण", *stages.toTypedArray()),
            q("farmerRef", "परियोजना किसान ID (सभी फोन पर एक ही ID रखें)"),
            q("farmerName", "किसान का नाम"), q("phone", "मोबाइल"), q("village", "गाँव / क्लस्टर"),
            q("plotRef", "FarmTrace / खेत / Plot ID"), q("gps", "GPS Pin / निर्देशांक"), q("geoPhotoRef", "Geo-photo संदर्भ"),
            n("projectAcres", "परियोजना क्षेत्र (एकड़)"), q("cropStage", "फसल / अवस्था"),
            c("irrigationSource", "सिंचाई स्रोत", "ट्यूबवेल", "नहर", "वर्षा आधारित", "अन्य"), q("irrigationOther", "अन्य सिंचाई स्रोत"),
            n("irrigationCount", "अब तक सिंचाई (बार)"), c("leadFarmer", "किसान साथी", "हाँ", "नहीं"),
        )),
        ImpactSection("B. FarmAssist सलाह", listOf(
            c("farmAssistUse", "FarmAssist उपयोग", "हाँ", "आंशिक", "नहीं"),
            m("advice", "मिली सलाह (एक से अधिक चुन सकते हैं)", "सिंचाई", "पोषण", "कीट / रोग", "अगली फसल"),
            m("benefits", "मुख्य लाभ", "पानी बचा", "खाद / इनपुट बचा", "समस्या पहचान में मदद", "अभी स्पष्ट नहीं"),
            c("irrigationsSaved", "अनुमानित सिंचाई बची", "0", "1", "2+", "पता नहीं"),
            c("costSaving", "इनपुट / खर्च में बचत", "नहीं", "थोड़ी", "अच्छी", "पता नहीं"),
            q("savingDetails", "बचत का विवरण / तुलना का आधार"),
        )),
        ImpactSection("C. Soil Saathi रीडिंग", listOf(
            n("ph", "pH (0–14)", 0.0, 14.0), n("ec", "EC"), q("ecUnit", "EC की इकाई (यंत्र / रिपोर्ट के अनुसार)"),
            n("salinity", "Salinity / लवणता"), q("salinityUnit", "लवणता की इकाई"),
            n("moisture", "नमी (%)", 0.0, 100.0), n("temperature", "तापमान (°C)", -50.0, 100.0),
            n("nitrogen", "N / नाइट्रोजन"), n("phosphorus", "P / फॉस्फोरस"), n("potassium", "K / पोटैशियम"),
            q("npkUnit", "N / P / K की इकाई (जैसे mg/kg; पुष्टि करके लिखें)"),
            q("readingSource", "यंत्र ID / रीडिंग समय / लैब या माप का स्रोत"),
            m("soilHelp", "Soil Saathi से मदद", "मिट्टी की स्थिति समझी", "सिंचाई निर्णय", "खाद निर्णय", "अभी स्पष्ट नहीं"),
            q("sampleId", "मिट्टी नमूना / लैब Sample ID"), c("labStatus", "नमूना / लैब स्थिति", "जमा", "रिपोर्ट मिली", "लागू नहीं"),
        )),
        ImpactSection("D. BioSynth Nano", listOf(
            c("nanoReceived", "फील्ड पैक मिला?", "मिला", "नहीं", "लागू नहीं"), q("batch", "Batch / बैच संदर्भ"),
            n("nanoQuantity", "प्रयोग की मात्रा"), q("nanoUnit", "मात्रा की इकाई"), d("applicationDate", "प्रयोग तिथि"),
            c("trialType", "फील्ड रिकॉर्ड", "Treatment / उपचार", "Control / तुलनात्मक खेत", "Observation only / केवल अवलोकन", "लागू नहीं"),
            q("comparatorRef", "तुलनात्मक खेत / उपचार विधि / क्षेत्र"), d("followupDate", "अगली फॉलो-अप तिथि"),
        )),
        ImpactSection("E. प्रभाव एवं प्रतिक्रिया", listOf(
            m("waterInputImpact", "पानी / इनपुट प्रभाव", "पानी कम लगा", "खाद / इनपुट कम लगा", "कोई बदलाव स्पष्ट नहीं"),
            n("pumpHoursSaved", "पम्प घंटे बचे (अनुमान)"),
            q("comparisonPeriod", "तुलना अवधि / मौसम / समान क्षेत्र का आधार"),
            n("baselinePumpHours", "पहले पम्प घंटे"), n("currentPumpHours", "अब पम्प घंटे"),
            n("baselineInput", "पहले खाद / इनपुट मात्रा"), n("currentInput", "अब खाद / इनपुट मात्रा"), q("inputUnit", "इनपुट नाम और इकाई"),
            c("noBurning", "पराली / अवशेष नहीं जलाया / नहीं जलाऊँगा", "हाँ", "नहीं", "लागू नहीं"),
            m("residueUse", "फसल अवशेष (CRM) उपयोग", "मिट्टी में मिलाया", "चारा", "अन्य"), q("residueDetails", "अवशेष मात्रा / इकाई / विवरण"),
            m("co2Basis", "CO₂e के लिए डेटा का आधार (यह गणना नहीं है)", "पानी / पम्प", "इनपुट / उर्वरक", "CRM"),
            c("satisfaction", "कुल संतुष्टि", "बहुत संतुष्ट", "संतुष्ट", "सामान्य", "असंतुष्ट"), q("feedback", "मुख्य लाभ / किसान की टिप्पणी"),
            c("trainingPast", "पिछले 12 माह में KVK / विश्वविद्यालय प्रशिक्षण / विजिट", "हाँ", "नहीं"),
            c("trainingInterest", "आगे प्रशिक्षण में रुचि", "हाँ", "नहीं"),
        )),
        ImpactSection("F. फोटो एवं फॉलो-अप", listOf(
            q("farmTraceRef", "FarmTrace / Geo-photo संदर्भ"), q("fieldNotes", "मुख्य फील्ड नोट / प्रमाण / गवाह"),
            q("correctionRef", "यदि सुधार है: पुराने आकलन का ID और कारण"),
        )),
    )
    val photoLabels = linkedMapOf("baseline" to "बेसलाइन फोटो", "soil" to "Soil Saathi फोटो", "demo" to "फील्ड डेमो फोटो", "followup" to "फॉलो-अप फोटो", "consent" to "हस्ताक्षरित कागजी सहमति का फोटो (वैकल्पिक)")
    const val consentText = "मैंने ऊपर दी गई जानकारी अपने अनुभव के अनुसार दी है। मैं स्वेच्छा से परियोजना में भाग ले रहा/रही हूँ और खेत/फसल, फोटो, Soil Saathi रीडिंग, FarmAssist सलाह, BioSynth Nano/फील्ड रिकॉर्ड तथा फॉलो-अप डेटा को परियोजना विश्लेषण और रिपोर्टिंग हेतु उपयोग करने की सहमति देता/देती हूँ।"

    fun validate(answers: Map<String, String>, date: String, officer: String): String? {
        if (CollectiveRules.dateMillis(date) == null) return "सही आकलन तिथि भरें (DD-MM-YYYY)।"
        if (officer.isBlank()) return "फील्ड कर्मी का नाम भरें।"
        if (answers["stage"] !in stages) return "आकलन का चरण चुनें।"
        for (question in sections.flatMap { it.questions }) {
            val value = answers[question.key].orEmpty().trim()
            if (value.isEmpty()) continue
            if (question.kind == "date" && CollectiveRules.dateMillis(value) == null) return "${question.label}: सही तिथि भरें।"
            if (question.kind == "number") {
                val number = value.toDoubleOrNull()
                if (number == null || !number.isFinite() || question.min?.let { number < it } == true || question.max?.let { number > it } == true)
                    return "${question.label}: मान जाँचें।"
                if (question.key == "irrigationCount" && number % 1 != 0.0) return "सिंचाई की संख्या पूर्ण अंक में भरें।"
                if (question.key == "projectAcres" && number <= 0) return "परियोजना क्षेत्र शून्य से अधिक होना चाहिए।"
            }
        }
        val units = mapOf("ec" to "ecUnit", "salinity" to "salinityUnit", "nitrogen" to "npkUnit", "phosphorus" to "npkUnit", "potassium" to "npkUnit", "nanoQuantity" to "nanoUnit", "baselineInput" to "inputUnit", "currentInput" to "inputUnit")
        units.forEach { (value, unit) -> if (!answers[value].isNullOrBlank() && answers[unit].isNullOrBlank()) return "मात्रा / रीडिंग के साथ उसकी इकाई भी भरें।" }
        return null
    }
}
