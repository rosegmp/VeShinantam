package app.veshinantam.shared.preset

import app.veshinantam.shared.CanonicalMaterialType
import app.veshinantam.shared.IsoDate
import kotlinx.serialization.Serializable

@Serializable
data class RemotePresetUnit(val english: String, val hebrew: String)

/** A complete definition for a new preset, or selected replacement fields for a bundled preset. */
@Serializable
data class RemotePresetPatch(
    val id: String,
    val nameEnglish: String? = null,
    val nameHebrew: String? = null,
    val materialType: String? = null,
    val dailyQuantity: Int? = null,
    val selectedWeekdays: List<Int>? = null,
    val excludedDates: List<String>? = null,
    val units: List<RemotePresetUnit>? = null,
)

@Serializable
data class RemotePresetCatalog(
    val schemaVersion: Int,
    val catalogVersion: String,
    val sequence: Long,
    val positionAsOf: String,
    val positions: Map<String, String> = emptyMap(),
    val programs: List<RemotePresetPatch> = emptyList(),
)

/** Validates a signed v2 payload and returns an immutable, complete catalog. */
object RemotePresetCatalogValidator {
    private val idPattern = Regex("[a-z][a-z0-9-]{0,63}")

    fun programs(update: RemotePresetCatalog): List<SharedPresetProgram> {
        require(update.schemaVersion == 2) { "Unsupported catalog schema" }
        require(update.catalogVersion.isNotBlank() && update.catalogVersion.length <= 80)
        require(update.sequence > SharedPresetCatalog.SEQUENCE)
        val asOf = requireNotNull(IsoDate.parse(update.positionAsOf)) { "Invalid position date" }
        require(asOf >= SharedPresetCatalog.positionAsOf)
        require(update.programs.size <= 100)

        val result = LinkedHashMap<String, SharedPresetProgram>()
        SharedPresetCatalog.programs.forEach { bundled ->
            result[bundled.id] = SharedPresetCatalog.programAtDate(bundled, asOf)
        }
        val changed = mutableSetOf<String>()
        update.programs.forEach { patch ->
            require(idPattern.matches(patch.id) && changed.add(patch.id)) { "Duplicate or invalid preset ID" }
            val base = result[patch.id]
            val units = patch.units?.map { UnitReference(it.english, it.hebrew) } ?: base?.units
            require(!units.isNullOrEmpty() && units.size <= 20_000) { "Invalid preset units" }
            require(units.all { it.english.isNotBlank() && it.english.length <= 240 && it.hebrew.isNotBlank() && it.hebrew.length <= 240 })
            require(units.map { it.english }.toSet().size == units.size) { "Duplicate preset unit" }
            val weekdays = patch.selectedWeekdays?.toSet() ?: base?.selectedWeekdays
            require(!weekdays.isNullOrEmpty() && weekdays.all { it in 0..6 }) { "Invalid preset weekdays" }
            val exclusions = patch.excludedDates?.map { requireNotNull(IsoDate.parse(it)) { "Invalid exclusion date" } }?.toSet()
                ?: base?.excludedDates ?: emptySet()
            require(exclusions.size <= 2_000)
            val material = patch.materialType?.let(CanonicalMaterialType::valueOf) ?: base?.materialType
            requireNotNull(material) { "Missing material type" }
            val quantity = patch.dailyQuantity ?: base?.dailyQuantity
            require(quantity != null && quantity in 1..100)
            val nameEnglish = patch.nameEnglish ?: base?.nameEnglish
            val nameHebrew = patch.nameHebrew ?: base?.nameHebrew
            require(!nameEnglish.isNullOrBlank() && nameEnglish.length <= 120)
            require(!nameHebrew.isNullOrBlank() && nameHebrew.length <= 120)
            val defaultReference = base?.currentReference?.english
            val reference = update.positions[patch.id] ?: defaultReference
            requireNotNull(reference) { "Missing starting position for ${patch.id}" }
            val index = units.indexOfFirst { it.english == reference }
            require(index >= 0) { "Unknown position for ${patch.id}: $reference" }
            result[patch.id] = SharedPresetProgram(
                patch.id, nameEnglish, nameHebrew, material, quantity, weekdays, exclusions, units, index,
            )
        }
        require(update.positions.keys.all { it in result }) { "Unknown preset position" }
        update.positions.forEach { (id, reference) ->
            val program = result.getValue(id)
            val index = program.units.indexOfFirst { it.english == reference }
            require(index >= 0) { "Unknown position for $id: $reference" }
            result[id] = program.copy(currentIndex = index)
        }
        return result.values.toList()
    }
}
