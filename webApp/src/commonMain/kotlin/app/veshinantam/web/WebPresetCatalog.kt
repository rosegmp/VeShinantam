package app.veshinantam.web

import app.veshinantam.shared.IsoDate
import app.veshinantam.shared.preset.MaterialCatalog
import app.veshinantam.shared.preset.SharedPresetCatalog
import app.veshinantam.shared.preset.SharedPresetProgram
import app.veshinantam.shared.preset.RemotePresetCatalog
import app.veshinantam.shared.preset.RemotePresetCatalogValidator
import app.veshinantam.shared.preset.RemotePresetPatch
import kotlinx.serialization.Serializable

@Serializable
data class WebPresetCatalogUpdate(
    val catalogVersion: String,
    val sequence: Long,
    val positionAsOf: String,
    val positions: Map<String, String>,
    val schemaVersion: Int = 1,
    val programs: List<RemotePresetPatch> = emptyList(),
)

/** Browser equivalent of Android's verified, bundled-fallback preset catalog. */
object WebPresetCatalog {
    private data class ActiveUpdate(
        val version: String,
        val sequence: Long,
        val positionAsOf: IsoDate,
        val programs: List<SharedPresetProgram>,
    )

    private var activeUpdate: ActiveUpdate? = null

    val version: String get() = activeUpdate?.version ?: SharedPresetCatalog.VERSION
    val sequence: Long get() = activeUpdate?.sequence ?: SharedPresetCatalog.SEQUENCE
    val positionAsOf: IsoDate get() = activeUpdate?.positionAsOf ?: SharedPresetCatalog.positionAsOf
    val programs: List<SharedPresetProgram> get() = activeUpdate?.programs ?: SharedPresetCatalog.programs

    fun applyVerifiedUpdate(update: WebPresetCatalogUpdate) {
        require(update.catalogVersion.isNotBlank())
        require(update.sequence > sequence)
        val asOf = parseIsoDate(update.positionAsOf)
        require(asOf >= SharedPresetCatalog.positionAsOf)
        if (update.schemaVersion == 2) {
            val programs = RemotePresetCatalogValidator.programs(RemotePresetCatalog(
                update.schemaVersion, update.catalogVersion, update.sequence, update.positionAsOf,
                update.positions, update.programs,
            ))
            activeUpdate = ActiveUpdate(update.catalogVersion, update.sequence, asOf, programs)
            return
        }
        require(update.schemaVersion == 1)
        require(update.positions.keys == SharedPresetCatalog.programs.mapTo(mutableSetOf()) { it.id })
        val updated = SharedPresetCatalog.programs.map { program ->
            val reference = update.positions.getValue(program.id)
            val index = program.units.indexOfFirst { it.english == reference }
            require(index >= 0) { "Unknown position for ${program.id}: $reference" }
            program.copy(currentIndex = index)
        }
        activeUpdate = ActiveUpdate(update.catalogVersion, update.sequence, asOf, updated)
    }

    internal fun clearVerifiedUpdate() {
        activeUpdate = null
    }

    fun programAtDate(program: SharedPresetProgram, date: IsoDate): SharedPresetProgram =
        program.copy(currentIndex = positionIndexOn(program, date))

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
                .firstOrNull { program.currentReference.english.startsWith("${it.english} ") }?.let { "${it.english} " }
            "mishnah-yomis" -> MaterialCatalog.mishnah
                .firstOrNull { program.currentReference.english.startsWith("${it.english} ") }?.let { "${it.english} " }
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

    private fun positionIndexOn(program: SharedPresetProgram, date: IsoDate): Int {
        require(date >= positionAsOf)
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

    private fun parseIsoDate(value: String): IsoDate {
        return requireNotNull(IsoDate.parse(value)) { "Invalid position date" }
    }
}
