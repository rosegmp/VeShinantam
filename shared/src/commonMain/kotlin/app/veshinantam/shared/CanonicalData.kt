package app.veshinantam.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val CANONICAL_DATA_VERSION = 1

@Serializable
enum class CanonicalScheduleKind { PRESET, CUSTOM }

@Serializable
enum class CanonicalMaterialType { DAF, AMUD, MISHNAH, PEREK, PAGE, SEIF, SIMAN, CUSTOM_UNIT }

@Serializable
enum class CanonicalMissedWorkBehavior { SHIFT_FORWARD, KEEP_FIXED_OVERDUE }

@Serializable
enum class CanonicalScheduleState { ACTIVE, PAUSED, ARCHIVED }

@Serializable
enum class CanonicalTaskType { LEARNING, CHAZARAH }

@Serializable
enum class CanonicalSefarimLanguage { ENGLISH, HEBREW, BOTH }

@Serializable
enum class CanonicalPrimaryCalendar { GREGORIAN, HEBREW }

@Serializable
data class CanonicalSchedule(
    val id: String,
    val nameEnglish: String,
    val nameHebrew: String = "",
    val kind: CanonicalScheduleKind = CanonicalScheduleKind.CUSTOM,
    val sourceType: String = "CUSTOM",
    val materialType: CanonicalMaterialType = CanonicalMaterialType.CUSTOM_UNIT,
    val presetId: String? = null,
    val presetVersion: Int? = null,
    val startDate: String,
    val targetDate: String? = null,
    val dailyQuantity: Int = 1,
    val selectedWeekdays: Set<Int> = (0..6).toSet(),
    val chazarahDayOffsets: List<Int> = listOf(1, 7),
    val repeatsAnnually: Boolean = false,
    val officialOraysaChazarah: Boolean = false,
    val missedWorkBehavior: CanonicalMissedWorkBehavior = CanonicalMissedWorkBehavior.KEEP_FIXED_OVERDUE,
    val state: CanonicalScheduleState = CanonicalScheduleState.ACTIVE,
    val generationRevision: Int = 1,
    val createdAt: String,
    val updatedAt: String,
    val revision: Long = 0,
    val deleted: Boolean = false,
)

@Serializable
data class CanonicalMaterialUnit(
    val id: String,
    val scheduleId: String,
    val ordinal: Int,
    val labelEnglish: String,
    val labelHebrew: String,
    val materialType: CanonicalMaterialType,
    val quantity: Double = 1.0,
    val coordinates: Map<String, String> = emptyMap(),
    val updatedAt: String,
    val revision: Long = 0,
    val deleted: Boolean = false,
)

@Serializable
data class CanonicalTask(
    val id: String,
    val stableKey: String,
    val scheduleId: String,
    val materialUnitId: String? = null,
    val type: CanonicalTaskType,
    val labelEnglish: String,
    val labelHebrew: String,
    val materialType: CanonicalMaterialType = CanonicalMaterialType.CUSTOM_UNIT,
    val quantity: Double = 1.0,
    val plannedDate: String,
    val originalLearningDate: String,
    val reviewIdentity: String? = null,
    val generationRevision: Int = 1,
    val completedAt: String? = null,
    val completionLocalDate: String? = null,
    val completionZoneId: String? = null,
    val updatedAt: String,
    val revision: Long = 0,
    val deleted: Boolean = false,
)

@Serializable
data class CanonicalExclusion(
    val scheduleId: String,
    val date: String,
    val updatedAt: String,
    val revision: Long = 0,
    val deleted: Boolean = false,
) {
    val id: String get() = "$scheduleId:$date"
}

@Serializable
data class CanonicalGoal(
    val id: String,
    val scheduleId: String? = null,
    val kind: String,
    val target: Double,
    val targetDate: String? = null,
    val completed: Boolean = false,
    val updatedAt: String,
    val revision: Long = 0,
    val deleted: Boolean = false,
)

@Serializable
data class CanonicalPreferences(
    val appLanguage: String = "en",
    val sefarimLanguage: CanonicalSefarimLanguage = CanonicalSefarimLanguage.BOTH,
    val primaryCalendar: CanonicalPrimaryCalendar = CanonicalPrimaryCalendar.GREGORIAN,
    val defaultChazarahOffsets: List<Int> = listOf(1, 7, 30, 90),
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val todaySortOrder: String = "SCHEDULE",
    val automaticPresetUpdates: Boolean = false,
    val updatedAt: String,
    val revision: Long = 0,
)

@Serializable
data class CanonicalDataSet(
    val format: String = FORMAT,
    val version: Int = CANONICAL_DATA_VERSION,
    val schedules: List<CanonicalSchedule> = emptyList(),
    val materialUnits: List<CanonicalMaterialUnit> = emptyList(),
    val tasks: List<CanonicalTask> = emptyList(),
    val exclusions: List<CanonicalExclusion> = emptyList(),
    val goals: List<CanonicalGoal> = emptyList(),
    val preferences: CanonicalPreferences,
    val syncCursor: Long = 0,
) {
    companion object {
        const val FORMAT = "app.veshinantam.canonical"
    }
}

@Serializable
enum class SyncEntityType { SCHEDULE, MATERIAL_UNIT, TASK, EXCLUSION, GOAL, PREFERENCES }

@Serializable
data class SyncMutation(
    val mutationId: String,
    val entityType: SyncEntityType,
    val entityId: String,
    val baseRevision: Long,
    val payload: String? = null,
    val deleted: Boolean = false,
    val createdAt: String,
)

@Serializable
data class SyncRecord(
    val entityType: SyncEntityType,
    val entityId: String,
    val revision: Long,
    val payload: String? = null,
    val deleted: Boolean,
    val updatedAt: String,
)

object CanonicalDataCodec {
    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun encode(value: CanonicalDataSet): String = json.encodeToString(value)

    fun decode(value: String): CanonicalDataSet = json.decodeFromString<CanonicalDataSet>(value).also {
        require(it.format == CanonicalDataSet.FORMAT) { "Unsupported data format" }
        require(it.version in 1..CANONICAL_DATA_VERSION) { "Unsupported data version ${it.version}" }
        val errors = CanonicalDataValidator.validate(it)
        require(errors.isEmpty()) { errors.joinToString("; ") }
    }
}

object CanonicalDataValidator {
    fun validate(data: CanonicalDataSet): List<String> = buildList {
        duplicateIds("schedule", data.schedules.map { it.id })?.let(::add)
        duplicateIds("material unit", data.materialUnits.map { it.id })?.let(::add)
        duplicateIds("task", data.tasks.map { it.id })?.let(::add)
        duplicateIds("goal", data.goals.map { it.id })?.let(::add)
        duplicateIds("exclusion", data.exclusions.map { it.id })?.let(::add)

        val scheduleIds = data.schedules.filterNot { it.deleted }.map { it.id }.toSet()
        data.schedules.forEach { schedule ->
            if (schedule.id.isBlank() || schedule.nameEnglish.isBlank() && schedule.nameHebrew.isBlank()) add("Invalid schedule")
            if (IsoDate.parse(schedule.startDate) == null || schedule.targetDate?.let(IsoDate::parse) == null && schedule.targetDate != null) add("Invalid schedule date: ${schedule.id}")
            if (schedule.dailyQuantity < 0 || schedule.generationRevision < 1) add("Invalid schedule quantity or revision: ${schedule.id}")
            if (schedule.selectedWeekdays.isEmpty() || schedule.selectedWeekdays.any { it !in 0..6 }) add("Invalid weekdays: ${schedule.id}")
            if (schedule.chazarahDayOffsets.any { it <= 0 } || schedule.chazarahDayOffsets.distinct().size != schedule.chazarahDayOffsets.size) add("Invalid chazarah offsets: ${schedule.id}")
        }
        data.tasks.forEach { task ->
            if (!task.deleted && task.scheduleId !in scheduleIds) add("Task references missing schedule: ${task.id}")
            if (task.id.isBlank() || task.stableKey.isBlank() || task.quantity <= 0.0 || task.generationRevision < 1) add("Invalid task: ${task.id}")
            if (IsoDate.parse(task.plannedDate) == null || IsoDate.parse(task.originalLearningDate) == null) add("Invalid task date: ${task.id}")
            val completionParts = listOf(task.completedAt, task.completionLocalDate, task.completionZoneId)
            if (completionParts.count { it != null } !in setOf(0, 3)) add("Incomplete completion metadata: ${task.id}")
            if (task.completionLocalDate?.let(IsoDate::parse) == null && task.completionLocalDate != null) add("Invalid completion date: ${task.id}")
        }
        data.materialUnits.forEach { unit ->
            if (!unit.deleted && unit.scheduleId !in scheduleIds) add("Material unit references missing schedule: ${unit.id}")
            if (unit.id.isBlank() || unit.ordinal < 0 || unit.quantity <= 0.0) add("Invalid material unit: ${unit.id}")
        }
        data.exclusions.forEach { exclusion ->
            if (!exclusion.deleted && exclusion.scheduleId !in scheduleIds) add("Exclusion references missing schedule: ${exclusion.id}")
            if (IsoDate.parse(exclusion.date) == null) add("Invalid exclusion date: ${exclusion.id}")
        }
        data.goals.forEach { goal ->
            if (!goal.deleted && goal.scheduleId != null && goal.scheduleId !in scheduleIds) add("Goal references missing schedule: ${goal.id}")
            if (goal.id.isBlank() || goal.kind.isBlank() || !goal.target.isFinite() || goal.target <= 0.0) add("Invalid goal: ${goal.id}")
        }
        if (data.preferences.appLanguage !in setOf("en", "he")) add("Invalid app language")
        if (data.preferences.defaultChazarahOffsets.isEmpty() || data.preferences.defaultChazarahOffsets.any { it <= 0 }) add("Invalid default chazarah offsets")
        if (data.preferences.reminderHour !in 0..23 || data.preferences.reminderMinute !in 0..59) add("Invalid reminder time")
    }.distinct()

    private fun duplicateIds(label: String, ids: List<String>): String? =
        if (ids.any { it.isBlank() } || ids.distinct().size != ids.size) "Invalid or duplicate $label IDs" else null
}
