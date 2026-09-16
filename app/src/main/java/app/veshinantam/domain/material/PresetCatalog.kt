package app.veshinantam.domain.material

import app.veshinantam.domain.model.MaterialType
import app.veshinantam.shared.CanonicalMaterialType
import app.veshinantam.shared.preset.SharedPresetCatalog
import java.time.DayOfWeek
import java.time.LocalDate

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
    const val BUNDLED_VERSION = SharedPresetCatalog.VERSION
    const val BUNDLED_SEQUENCE = SharedPresetCatalog.SEQUENCE
    val bundledPositionAsOf: LocalDate = SharedPresetCatalog.positionAsOf.let { LocalDate.of(it.year, it.month, it.day) }
    private data class ActiveUpdate(val version: String, val sequence: Long, val positionAsOf: LocalDate, val programs: List<PresetProgram>)
    @Volatile private var activeUpdate: ActiveUpdate? = null

    val VERSION: String get() = activeUpdate?.version ?: BUNDLED_VERSION
    val activeSequence: Long get() = activeUpdate?.sequence ?: BUNDLED_SEQUENCE
    val positionAsOf: LocalDate get() = activeUpdate?.positionAsOf ?: bundledPositionAsOf

    private val bundledPrograms: List<PresetProgram> by lazy {
        SharedPresetCatalog.programs.map { shared ->
            PresetProgram(
                id = shared.id,
                nameEnglish = shared.nameEnglish,
                nameHebrew = shared.nameHebrew,
                materialType = shared.materialType.toAndroidMaterialType(),
                dailyQuantity = shared.dailyQuantity,
                selectedWeekdays = shared.selectedWeekdays.mapTo(mutableSetOf(), ::dayOfWeek),
                excludedDates = shared.excludedDates.mapTo(mutableSetOf()) { LocalDate.of(it.year, it.month, it.day) },
                units = shared.units,
                currentIndex = shared.currentIndex,
            )
        }
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

    private fun CanonicalMaterialType.toAndroidMaterialType(): MaterialType = MaterialType.valueOf(name)

    private fun dayOfWeek(value: Int): DayOfWeek = when (value) {
        0 -> DayOfWeek.SUNDAY
        1 -> DayOfWeek.MONDAY
        2 -> DayOfWeek.TUESDAY
        3 -> DayOfWeek.WEDNESDAY
        4 -> DayOfWeek.THURSDAY
        5 -> DayOfWeek.FRIDAY
        6 -> DayOfWeek.SATURDAY
        else -> error("Unsupported weekday: $value")
    }
}
