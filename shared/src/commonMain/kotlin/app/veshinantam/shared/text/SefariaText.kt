package app.veshinantam.shared.text

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

data class SefariaTextRequest(
    val reference: String,
    val versions: List<String> = listOf("source", "translation"),
) {
    val cacheKey: String get() = (listOf(reference) + versions).joinToString("|")
}

sealed interface SefariaTextLookup {
    data class Available(val request: SefariaTextRequest) : SefariaTextLookup
    data class Unavailable(val messageEnglish: String, val messageHebrew: String) : SefariaTextLookup
}

data class SefariaTextVersion(
    val language: String,
    val title: String,
    val license: String,
    val sourceUrl: String?,
    val segments: List<String>,
) {
    val mayCache: Boolean get() {
        val normalized = license.trim().uppercase()
        return normalized == "PUBLIC DOMAIN" || normalized == "CC0" || normalized.startsWith("CC-")
    }
}

data class SefariaTextContent(
    val reference: String,
    val hebrewReference: String,
    val versions: List<SefariaTextVersion>,
) {
    val hebrew: SefariaTextVersion? get() = versions.firstOrNull { it.language == "he" }
    val english: SefariaTextVersion? get() = versions.firstOrNull { it.language == "en" }
    val mayCache: Boolean get() = versions.isNotEmpty() && versions.all { it.mayCache }
}

object SefariaReferenceMapper {
    private val mishnahBerurahReference = Regex(
        """^Mishnah Berurah, chelek \d+ siman (\d+)(?: seif (\d+))?$""",
        RegexOption.IGNORE_CASE,
    )

    fun lookup(referenceEnglish: String, materialType: String, presetId: String?): SefariaTextLookup {
        if (presetId == "yerushalmi-yomi-schottenstein") {
            return unavailable(
                "The Schottenstein edition and pagination are proprietary and are not available through the open-text reader.",
                "מהדורת שוטנשטיין והעימוד שלה מוגנים בזכויות יוצרים ואינם זמינים בקורא הטקסט הפתוח.",
            )
        }
        if (presetId == "yerushalmi-yomi-vilna" || referenceEnglish.startsWith("Yerushalmi ")) {
            return unavailable(
                "Yerushalmi daf references need a verified Vilna-page map before the correct passage can be shown.",
                "מראי המקום של דפי הירושלמי דורשים מיפוי מאומת לעימוד וילנא לפני הצגת הקטע הנכון.",
            )
        }
        if (materialType == "PAGE" && referenceEnglish.startsWith("Mishnah Berurah, chelek", ignoreCase = true)) {
            return unavailable(
                "This Mishnah Berurah page reference does not map safely to Sefaria's siman-and-se'if structure.",
                "מראה מקום זה במשנה ברורה מבוסס על עמודים ואינו מתאים בבטחה למבנה הסימנים והסעיפים בספריא.",
            )
        }
        if (referenceEnglish.startsWith("Chofetz Chaim,")) {
            return unavailable(
                "This Chofetz Chaim daily portion needs a verified section map before the correct passage can be shown.",
                "הקטע היומי בחפץ חיים דורש מיפוי מאומת של הסעיפים לפני הצגת הקטע הנכון.",
            )
        }
        if (referenceEnglish.startsWith("Pele Yoetz, Volume ")) {
            return unavailable(
                "This Hachzek Pele Yoetz assignment is identified by its opening words so it works across editions; it does not map safely to Sefaria's section numbering.",
                "משימת חזק פלא יועץ מזוהה לפי המילים הפותחות כדי שתתאים למהדורות שונות; אין לה מיפוי בטוח למספור הקטעים בספריא.",
            )
        }

        if (materialType == "CUSTOM_UNIT" &&
            !referenceEnglish.startsWith("Pele Yoetz, Day ") &&
            !referenceEnglish.startsWith("Kitzur Shulchan Aruch ")
        ) {
            return unavailable(
                "This custom label is not a precise sefer reference, so the reader cannot safely choose a passage.",
                "תווית מותאמת זו אינה מראה מקום מדויק, ולכן הקורא אינו יכול לבחור קטע בבטחה.",
            )
        }

        val mishnahBerurah = mishnahBerurahReference.matchEntire(referenceEnglish)
        val mapped = when {
            mishnahBerurah != null -> buildString {
                append("Mishnah Berurah ")
                append(mishnahBerurah.groupValues[1])
                mishnahBerurah.groupValues[2].takeIf(String::isNotEmpty)?.let { append(":$it") }
            }
            referenceEnglish.startsWith("Rambam, ") -> referenceEnglish.replaceFirst("Rambam, ", "Mishneh Torah, ")
            referenceEnglish.startsWith("Tehillim ") -> referenceEnglish.replaceFirst("Tehillim ", "Psalms ")
            referenceEnglish.startsWith("Kitzur Shulchan Aruch ") ->
                referenceEnglish.replaceFirst("Kitzur Shulchan Aruch ", "Kitzur Shulchan Arukh ")
            referenceEnglish.startsWith("Pele Yoetz, Day ") ->
                referenceEnglish.replaceFirst("Pele Yoetz, Day ", "Pele Yoetz ")
            (materialType == "MISHNAH" || materialType == "PEREK") &&
                !referenceEnglish.startsWith("Mishnah ") &&
                !referenceEnglish.startsWith("Mishneh Torah, ") &&
                !referenceEnglish.startsWith("Psalms ") -> "Mishnah $referenceEnglish"
            else -> referenceEnglish
        }
        if (mapped.isBlank()) return unavailable("No text reference is available for this task.", "אין מראה מקום זמין למשימה זו.")

        val bavli = (materialType == "DAF" || materialType == "AMUD") &&
            !mapped.startsWith("Jerusalem Talmud ")
        val versions = if (bavli) {
            listOf("hebrew|Wikisource Talmud Bavli", "english|William Davidson Edition - English")
        } else {
            listOf("source", "translation")
        }
        return SefariaTextLookup.Available(SefariaTextRequest(mapped, versions))
    }

    private fun unavailable(english: String, hebrew: String) = SefariaTextLookup.Unavailable(english, hebrew)
}

object SefariaTextParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(raw: String): SefariaTextContent {
        val root = json.parseToJsonElement(raw).jsonObject
        val versions = root["versions"]?.jsonArray.orEmpty().mapNotNull(::parseVersion)
            .filter { it.segments.isNotEmpty() }
        require(versions.isNotEmpty()) { warningMessage(root) ?: "Sefaria returned no text for this reference." }
        return SefariaTextContent(
            reference = root.string("ref").orEmpty(),
            hebrewReference = root.string("heRef").orEmpty(),
            versions = versions,
        )
    }

    private fun parseVersion(element: JsonElement): SefariaTextVersion? {
        val value = element as? JsonObject ?: return null
        val segments = flatten(value["text"] ?: return null)
        return SefariaTextVersion(
            language = value.string("language").orEmpty(),
            title = value.string("versionTitle").orEmpty(),
            license = value.string("license").orEmpty(),
            sourceUrl = value.string("versionSource"),
            segments = segments,
        )
    }

    private fun flatten(element: JsonElement): List<String> = when (element) {
        is JsonArray -> element.flatMap(::flatten)
        is JsonPrimitive -> listOfNotNull(element.contentOrNull?.trim()?.takeIf(String::isNotEmpty))
        else -> emptyList()
    }

    private fun warningMessage(root: JsonObject): String? = (root["warnings"] as? JsonObject)
        ?.values
        ?.firstNotNullOfOrNull { warning -> (warning as? JsonObject)?.string("message") }

    private fun JsonObject.string(name: String): String? = (get(name) as? JsonPrimitive)?.contentOrNull
}
