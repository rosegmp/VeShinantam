package app.veshinantam.domain.material

import app.veshinantam.domain.model.MaterialType
import java.time.DayOfWeek
import java.time.LocalDate
import app.veshinantam.localization.HebrewNumerals

data class PresetProgram(
    val id: String,
    val nameEnglish: String,
    val nameHebrew: String,
    val materialType: MaterialType,
    val dailyQuantity: Int,
    val selectedWeekdays: Set<DayOfWeek>,
    val excludedDates: Set<LocalDate> = emptySet(),
    val units: List<UnitReference>,
    val currentIndex: Int,
) {
    val currentReference: UnitReference get() = units[currentIndex]
    val selectableStartingUnits: List<UnitReference> get() = units.subList(0, currentIndex + 1)
}

/** Bundled reference-only preset catalog. It contains no sefer text. */
object PresetCatalog {
    const val BUNDLED_VERSION = "2026.09.10-8"
    const val BUNDLED_SEQUENCE = 8L
    val bundledPositionAsOf: LocalDate = LocalDate.of(2026, 9, 10)
    private data class ActiveUpdate(val version: String, val sequence: Long, val positionAsOf: LocalDate, val programs: List<PresetProgram>)
    @Volatile private var activeUpdate: ActiveUpdate? = null

    val VERSION: String get() = activeUpdate?.version ?: BUNDLED_VERSION
    val activeSequence: Long get() = activeUpdate?.sequence ?: BUNDLED_SEQUENCE
    val positionAsOf: LocalDate get() = activeUpdate?.positionAsOf ?: bundledPositionAsOf

    private val everyDay = DayOfWeek.entries.toSet()
    private val sundayThroughThursday = setOf(
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
    )
    private val sundayThroughFriday = sundayThroughThursday + DayOfWeek.FRIDAY
    private val dafUnits by lazy {
        MaterialCatalog.gemara.flatMap { masechta ->
            when (masechta.english) {
                "Kinnim" -> numberedDafUnits(masechta, 22..25)
                "Tamid" -> numberedDafUnits(masechta, 26..33)
                "Middos" -> numberedDafUnits(masechta, 34..37)
                else -> MaterialCatalog.gemaraUnits(listOf(masechta), 2, masechta.lastLocation, GemaraUnit.DAF)
            }
        }
    }
    private val amudUnits by lazy {
        MaterialCatalog.gemara.flatMap(::amudUnitsFor)
    }
    private val oraysaAmudUnits by lazy {
        val openingOrder = listOf(
            "Berachos", "Shabbos", "Eruvin", "Pesachim", "Rosh Hashanah", "Yoma",
            "Sukkah", "Beitzah", "Megillah", "Taanis", "Moed Katan", "Chagigah",
        )
        val opening = openingOrder.map { name -> MaterialCatalog.gemara.first { it.english == name } }
        val remainder = MaterialCatalog.gemara.filter { it.english !in openingOrder && it.english != "Shekalim" }
        (opening + remainder).flatMap(::amudUnitsFor)
    }
    private val mishnahUnits by lazy {
        MaterialCatalog.mishnah.flatMap { MaterialCatalog.mishnahUnits(it, MishnahUnit.MISHNAH) }
    }
    private val mishnahBerurahPages by lazy {
        (1..MaterialCatalog.mishnahBerurahChelakim.size).flatMap {
            MaterialCatalog.mishnahBerurahUnitOptions(it, MishnahBerurahUnit.PAGE)
        }
    }
    private val yerushalmiUnits by lazy { MaterialCatalog.yerushalmi.flatMap(MaterialCatalog::yerushalmiUnits) }
    private val yerushalmiSchottensteinUnits by lazy {
        MaterialCatalog.yerushalmiSchottenstein.flatMap(MaterialCatalog::yerushalmiUnits)
    }
    private val rambamThreeChapterUnits by lazy {
        MaterialCatalog.rambam.flatMap { section ->
            when (section.english) {
                "The Order of Prayer" -> (1..5).map { chapter -> rambamReference(section, chapter.toString()) }
                "Leavened and Unleavened Bread" -> (1..8).map { chapter ->
                    rambamReference(section, if (chapter == 8) "8-9" else chapter.toString())
                }
                else -> MaterialCatalog.rambamUnits(section)
            }
        }
    }
    private val yerushalmiFastDays = setOf(
        LocalDate.of(2026, 9, 21),
        LocalDate.of(2027, 8, 12), LocalDate.of(2027, 10, 11),
        LocalDate.of(2028, 8, 1), LocalDate.of(2028, 9, 30),
        LocalDate.of(2029, 7, 22), LocalDate.of(2029, 9, 19),
        LocalDate.of(2030, 8, 8), LocalDate.of(2030, 10, 7),
        LocalDate.of(2031, 7, 29), LocalDate.of(2031, 9, 27),
    )
    private val hachzekPeleYoetzUnits by lazy {
        (1..245).map { day ->
            UnitReference(
                english = "Pele Yoetz, Day $day",
                hebrew = "פלא יועץ, יום ${HebrewNumerals.format(day)}",
            )
        }
    }
    private val hachzekPeleYoetzReviewDates = setOf(
        LocalDate.of(2026, 5, 22), // Shavuos
        LocalDate.of(2026, 7, 23), // Tisha B'Av
        LocalDate.of(2026, 9, 13), // Rosh Hashanah
        LocalDate.of(2026, 9, 21), // Yom Kippur
        LocalDate.of(2026, 9, 27), // Sukkos
        LocalDate.of(2026, 10, 4), // Simchas Torah
    )

    private val bundledPrograms: List<PresetProgram> by lazy {
        listOf(
            program("daf-yomi-bavli", "Daf Yomi Bavli", "דף יומי בבלי", MaterialType.DAF, 1, everyDay, dafUnits, "Chullin 133"),
            program("oraysa", "Oraysa", "אורייתא", MaterialType.AMUD, 1, sundayThroughThursday, oraysaAmudUnits, "Yevamos 105a"),
            program("amud-yomi", "Dirshu Amud Yomi", "דרשו עמוד יומי", MaterialType.AMUD, 1, everyDay, amudUnits, "Yoma 69a"),
            program("mishnah-yomis", "Mishnah Yomis", "משנה יומית", MaterialType.MISHNAH, 2, everyDay, mishnahUnits, "Kelim 30:2"),
            program(
                "dirshu-mishnah-berurah",
                "Dirshu Mishnah Berurah",
                "דרשו משנה ברורה",
                MaterialType.PAGE,
                1,
                sundayThroughThursday,
                mishnahBerurahPages,
                "Mishnah Berurah, chelek 5 page 7a",
            ),
            program(
                "yerushalmi-yomi-vilna", "Yerushalmi Yomi (Vilna)", "ירושלמי יומי (וילנא)", MaterialType.DAF, 1,
                everyDay, yerushalmiUnits, "Yerushalmi Shevuos 15", yerushalmiFastDays,
            ),
            program(
                "yerushalmi-yomi-schottenstein", "Yerushalmi Yomi (Schottenstein)", "ירושלמי יומי (שוטנשטיין)", MaterialType.DAF, 1,
                everyDay, yerushalmiSchottensteinUnits, "Yerushalmi Yevamos 33",
            ),
            program(
                "rambam-three-chapters", "Rambam – Three Chapters Daily", "רמב״ם – שלושה פרקים ליום",
                MaterialType.PEREK, 3, everyDay, rambamThreeChapterUnits, "Rambam, Other Sources of Defilement 9",
            ),
            program(
                "chofetz-chaim", "Chofetz Chaim Yomi", "חפץ חיים יומי", MaterialType.CUSTOM_UNIT, 1, everyDay,
                MaterialCatalog.chofetzChaimUnits, "Chofetz Chaim, Tziyurim 8-9",
            ),
            program(
                "tehillim-monthly", "Monthly Tehillim", "תהילים חודשי", MaterialType.PEREK, 1, everyDay,
                MaterialCatalog.monthlyTehillimUnits, "Tehillim 120-134",
            ),
            program(
                "hachzek-pele-yoetz", "Hachzek Pele Yoetz", "חזק פלא יועץ", MaterialType.CUSTOM_UNIT, 1,
                sundayThroughFriday, hachzekPeleYoetzUnits, "Pele Yoetz, Day 103", hachzekPeleYoetzReviewDates,
            ),
            program(
                "kitzur-yomi", "Kitzur Shulchan Aruch Yomi", "קיצור שולחן ערוך יומי", MaterialType.CUSTOM_UNIT, 1,
                everyDay, KitzurYomiData.units, "Kitzur Shulchan Aruch 133:1-8",
            ),
        )
    }

    val programs: List<PresetProgram> get() = activeUpdate?.programs ?: bundledPrograms

    fun validateVerifiedUpdate(version: String, sequence: Long, asOf: LocalDate, currentReferences: Map<String, String>) {
        require(version.isNotBlank())
        require(sequence > activeSequence)
        require(asOf >= bundledPositionAsOf)
        require(currentReferences.keys == bundledPrograms.mapTo(mutableSetOf()) { it.id })
        bundledPrograms.forEach { program ->
            val reference = requireNotNull(currentReferences[program.id])
            val index = program.units.indexOfFirst { it.english == reference }
            require(index >= 0) { "Unknown position for ${program.id}: $reference" }
        }
    }

    fun applyVerifiedUpdate(version: String, sequence: Long, asOf: LocalDate, currentReferences: Map<String, String>) {
        validateVerifiedUpdate(version, sequence, asOf, currentReferences)
        val updated = bundledPrograms.map { program ->
            program.copy(currentIndex = program.units.indexOfFirst { it.english == currentReferences.getValue(program.id) })
        }
        activeUpdate = ActiveUpdate(version, sequence, asOf, updated)
    }

    internal fun clearVerifiedUpdate() {
        activeUpdate = null
    }

    fun scheduledDate(program: PresetProgram, unitIndex: Int): LocalDate {
        require(unitIndex in 0..program.currentIndex)
        var learningDaysBack = (program.currentIndex - unitIndex + program.dailyQuantity - 1) / program.dailyQuantity
        var date = positionAsOf
        while (learningDaysBack > 0) {
            date = date.minusDays(1)
            if (date.dayOfWeek in program.selectedWeekdays && date !in program.excludedDates) learningDaysBack--
        }
        return date
    }

    fun currentMasechtaStartIndex(program: PresetProgram): Int? {
        val prefix = when (program.id) {
            "daf-yomi-bavli", "oraysa", "amud-yomi" -> MaterialCatalog.gemara
                .firstOrNull { program.currentReference.english.startsWith("${it.english} ") }
                ?.let { "${it.english} " }
            "mishnah-yomis" -> MaterialCatalog.mishnah
                .firstOrNull { program.currentReference.english.startsWith("${it.english} ") }
                ?.let { "${it.english} " }
            "yerushalmi-yomi-vilna" -> MaterialCatalog.yerushalmi
                .firstOrNull { program.currentReference.english.startsWith("Yerushalmi ${it.english} ") }
                ?.let { "Yerushalmi ${it.english} " }
            "yerushalmi-yomi-schottenstein" -> MaterialCatalog.yerushalmiSchottenstein
                .firstOrNull { program.currentReference.english.startsWith("Yerushalmi ${it.english} ") }
                ?.let { "Yerushalmi ${it.english} " }
            else -> null
        } ?: return null
        return program.units.indexOfFirst { it.english.startsWith(prefix) }.takeIf { it >= 0 }
    }

    private fun program(
        id: String,
        english: String,
        hebrew: String,
        materialType: MaterialType,
        dailyQuantity: Int,
        weekdays: Set<DayOfWeek>,
        units: List<UnitReference>,
        currentEnglishReference: String,
        excludedDates: Set<LocalDate> = emptySet(),
    ): PresetProgram {
        val currentIndex = units.indexOfFirst { it.english == currentEnglishReference }
        check(currentIndex >= 0) { "Missing current preset position: $currentEnglishReference" }
        return PresetProgram(id, english, hebrew, materialType, dailyQuantity, weekdays, excludedDates, units, currentIndex)
    }

    private fun rambamReference(section: Masechta, chapter: String): UnitReference = UnitReference(
        "Rambam, ${section.english} $chapter",
        "רמב״ם, ${section.hebrew} פרק ${chapter.split('-').joinToString("–") { HebrewNumerals.format(it.toInt()) }}",
    )

    private fun numberedDafUnits(masechta: Masechta, range: IntRange): List<UnitReference> = range.map { daf ->
        UnitReference("${masechta.english} $daf", "${masechta.hebrew} דף ${HebrewNumerals.format(daf)}.")
    }

    private fun amudUnitsFor(masechta: Masechta): List<UnitReference> = when (masechta.english) {
        "Kinnim" -> numberedAmudUnits(masechta, 22, 'a', 25, 'a')
        "Tamid" -> numberedAmudUnits(masechta, 25, 'b', 33, 'b')
        "Middos" -> numberedAmudUnits(masechta, 34, 'a', 37, 'b')
        else -> MaterialCatalog.gemaraUnits(listOf(masechta), 2, masechta.lastLocation, GemaraUnit.AMUD)
    }

    private fun numberedAmudUnits(
        masechta: Masechta,
        firstDaf: Int,
        firstSide: Char,
        lastDaf: Int,
        lastSide: Char,
    ): List<UnitReference> = (firstDaf..lastDaf).flatMap { daf ->
        listOf('a', 'b').filter { side ->
            (daf > firstDaf || side >= firstSide) && (daf < lastDaf || side <= lastSide)
        }.map { side ->
            val punctuation = if (side == 'a') "." else ":"
            UnitReference("${masechta.english} $daf$side", "${masechta.hebrew} דף ${HebrewNumerals.format(daf)}$punctuation")
        }
    }
}
