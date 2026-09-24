package app.veshinantam.shared.preset

import app.veshinantam.shared.CanonicalMaterialType
import app.veshinantam.shared.IsoDate

data class SharedPresetProgram(
    val id: String,
    val nameEnglish: String,
    val nameHebrew: String,
    val materialType: CanonicalMaterialType,
    val dailyQuantity: Int,
    val selectedWeekdays: Set<Int>,
    val excludedDates: Set<IsoDate> = emptySet(),
    val units: List<UnitReference>,
    val currentIndex: Int,
) {
    val currentReference: UnitReference get() = units[currentIndex]
    val selectableStartingUnits: List<UnitReference> get() = units.subList(0, currentIndex + 1)
}

/** Platform-neutral, reference-only catalog shared by Android and the web app. */
object SharedPresetCatalog {
    const val VERSION = "2026.09.22-10"
    const val SEQUENCE = 10L
    val positionAsOf = IsoDate(2026, 9, 10)

    private val everyDay = (0..6).toSet()
    private val sundayThroughThursday = (0..4).toSet()
    private val sundayThroughFriday = (0..5).toSet()
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
    private val amudUnits by lazy { MaterialCatalog.gemara.flatMap(::amudUnitsFor) }
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
        IsoDate(2026, 9, 21),
        IsoDate(2027, 8, 12), IsoDate(2027, 10, 11),
        IsoDate(2028, 8, 1), IsoDate(2028, 9, 30),
        IsoDate(2029, 7, 22), IsoDate(2029, 9, 19),
        IsoDate(2030, 8, 8), IsoDate(2030, 10, 7),
        IsoDate(2031, 7, 29), IsoDate(2031, 9, 27),
    )
    private val hachzekPeleYoetzUnits by lazy { PeleYoetzSchedule.units }
    private val hachzekPeleYoetzReviewDates = setOf(
        IsoDate(2025, 8, 3),
        IsoDate(2025, 9, 23), IsoDate(2025, 9, 24),
        IsoDate(2025, 10, 2),
        IsoDate(2025, 10, 7), IsoDate(2025, 10, 8),
        IsoDate(2025, 10, 14), IsoDate(2025, 10, 15),
        IsoDate(2026, 3, 3),
        IsoDate(2026, 4, 2), IsoDate(2026, 4, 3),
        IsoDate(2026, 4, 8), IsoDate(2026, 4, 9),
        IsoDate(2026, 5, 22),
        IsoDate(2026, 7, 23),
        IsoDate(2026, 9, 13),
        IsoDate(2026, 9, 21),
        IsoDate(2026, 9, 27),
        IsoDate(2026, 10, 4),
    )

    val programs: List<SharedPresetProgram> by lazy {
        listOf(
            program("daf-yomi-bavli", "Daf Yomi Bavli", "דף יומי בבלי", CanonicalMaterialType.DAF, 1, everyDay, dafUnits, "Chullin 133"),
            program("oraysa", "Oraysa", "אורייתא", CanonicalMaterialType.AMUD, 1, sundayThroughThursday, oraysaAmudUnits, "Yevamos 105a"),
            program("amud-yomi", "Dirshu Amud Yomi", "דרשו עמוד יומי", CanonicalMaterialType.AMUD, 1, everyDay, amudUnits, "Yoma 69a"),
            program("mishnah-yomis", "Mishnah Yomis", "משנה יומית", CanonicalMaterialType.MISHNAH, 2, everyDay, mishnahUnits, "Kelim 30:2"),
            program(
                "dirshu-mishnah-berurah", "Dirshu Mishnah Berurah", "דרשו משנה ברורה",
                CanonicalMaterialType.PAGE, 1, sundayThroughThursday, mishnahBerurahPages,
                "Mishnah Berurah, chelek 5 page 7a",
            ),
            program(
                "yerushalmi-yomi-vilna", "Yerushalmi Yomi (Vilna)", "ירושלמי יומי (וילנא)",
                CanonicalMaterialType.DAF, 1, everyDay, yerushalmiUnits, "Yerushalmi Shevuos 15", yerushalmiFastDays,
            ),
            program(
                "yerushalmi-yomi-schottenstein", "Yerushalmi Yomi (Schottenstein)", "ירושלמי יומי (שוטנשטיין)",
                CanonicalMaterialType.DAF, 1, everyDay, yerushalmiSchottensteinUnits, "Yerushalmi Yevamos 33",
            ),
            program(
                "rambam-three-chapters", "Rambam – Three Chapters Daily", "רמב״ם – שלושה פרקים ליום",
                CanonicalMaterialType.PEREK, 3, everyDay, rambamThreeChapterUnits, "Rambam, Other Sources of Defilement 9",
            ),
            program(
                "chofetz-chaim", "Chofetz Chaim Yomi", "חפץ חיים יומי", CanonicalMaterialType.CUSTOM_UNIT, 1,
                everyDay, MaterialCatalog.chofetzChaimUnits, "Chofetz Chaim, Tziyurim 8-9",
            ),
            program(
                "tehillim-monthly", "Monthly Tehillim", "תהילים חודשי", CanonicalMaterialType.PEREK, 1,
                everyDay, MaterialCatalog.monthlyTehillimUnits, "Tehillim 120-134",
            ),
            program(
                "hachzek-pele-yoetz", "Hachzek Pele Yoetz", "חזק פלא יועץ", CanonicalMaterialType.CUSTOM_UNIT, 1,
                sundayThroughFriday, hachzekPeleYoetzUnits, hachzekPeleYoetzUnits[254 + 102].english, hachzekPeleYoetzReviewDates,
            ),
            program(
                "kitzur-yomi", "Kitzur Shulchan Aruch Yomi", "קיצור שולחן ערוך יומי", CanonicalMaterialType.CUSTOM_UNIT, 1,
                everyDay, MaterialCatalog.kitzurYomiUnits, "Kitzur Shulchan Aruch 133:1-8",
            ),
        )
    }

    /** Official preset cycles do not add interval chazarah unless the learner opts in. */
    fun defaultAdditionalChazarahOffsets(programId: String): List<Int> {
        return emptyList()
    }

    fun programAtDate(program: SharedPresetProgram, date: IsoDate): SharedPresetProgram =
        program.copy(currentIndex = positionIndexOn(program, date))

    fun positionIndexOn(program: SharedPresetProgram, date: IsoDate): Int {
        require(date >= positionAsOf) { "Preset positions before $positionAsOf use scheduledDate" }
        var index = program.currentIndex
        var cursor = positionAsOf
        while (cursor < date && index < program.units.lastIndex) {
            cursor = cursor.plusDays(1)
            val weekday = app.veshinantam.shared.GregorianCalendar.dayOfWeek(cursor.year, cursor.month, cursor.day)
            if (weekday in program.selectedWeekdays && cursor !in program.excludedDates) {
                index = (index + program.dailyQuantity).coerceAtMost(program.units.lastIndex)
            }
        }
        return index
    }

    fun scheduledDate(program: SharedPresetProgram, unitIndex: Int, anchorDate: IsoDate = positionAsOf): IsoDate {
        require(unitIndex in 0..program.currentIndex)
        var learningDaysBack = (program.currentIndex - unitIndex + program.dailyQuantity - 1) / program.dailyQuantity
        var date = anchorDate
        while (learningDaysBack > 0) {
            date = date.minusDays(1)
            val weekday = app.veshinantam.shared.GregorianCalendar.dayOfWeek(date.year, date.month, date.day)
            if (weekday in program.selectedWeekdays && date !in program.excludedDates) learningDaysBack--
        }
        return date
    }

    fun currentMasechtaStartIndex(program: SharedPresetProgram): Int? {
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
        materialType: CanonicalMaterialType,
        dailyQuantity: Int,
        weekdays: Set<Int>,
        units: List<UnitReference>,
        currentEnglishReference: String,
        excludedDates: Set<IsoDate> = emptySet(),
    ): SharedPresetProgram {
        val currentIndex = units.indexOfFirst { it.english == currentEnglishReference }
        check(currentIndex >= 0) { "Missing current preset position: $currentEnglishReference" }
        return SharedPresetProgram(id, english, hebrew, materialType, dailyQuantity, weekdays, excludedDates, units, currentIndex)
    }

    private fun rambamReference(section: Masechta, chapter: String): UnitReference = UnitReference(
        "Rambam, ${section.english} $chapter",
        "רמב״ם, ${section.hebrew} פרק ${chapter.split('-').joinToString("–") { PresetHebrewNumerals.format(it.toInt()) }}",
    )

    private fun numberedDafUnits(masechta: Masechta, range: IntRange): List<UnitReference> = range.map { daf ->
        UnitReference("${masechta.english} $daf", "${masechta.hebrew} דף ${PresetHebrewNumerals.format(daf)}.")
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
            UnitReference("${masechta.english} $daf$side", "${masechta.hebrew} דף ${PresetHebrewNumerals.format(daf)}$punctuation")
        }
    }
}
