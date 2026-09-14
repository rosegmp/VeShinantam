package app.veshinantam.web

import app.veshinantam.shared.LearningSchedule
import app.veshinantam.shared.LearningTask
import app.veshinantam.shared.LearningTaskType
import app.veshinantam.shared.IsoDate
import app.veshinantam.shared.CanonicalDataSet
import app.veshinantam.shared.CanonicalDataValidator
import app.veshinantam.shared.CanonicalMaterialType
import app.veshinantam.shared.CanonicalMissedWorkBehavior
import app.veshinantam.shared.CanonicalPreferences
import app.veshinantam.shared.CanonicalPrimaryCalendar
import app.veshinantam.shared.CanonicalSchedule
import app.veshinantam.shared.CanonicalScheduleKind
import app.veshinantam.shared.CanonicalScheduleState
import app.veshinantam.shared.CanonicalSefarimLanguage
import app.veshinantam.shared.CanonicalTask
import app.veshinantam.shared.CanonicalTaskType
import kotlinx.serialization.Serializable

@Serializable
data class StoredSchedule(
    val id: String,
    val name: String,
    val material: String,
    val pace: Int,
    val weekdays: Set<Int> = (0..6).toSet(),
    val chazarahOffsets: List<Int> = listOf(1, 7),
    val active: Boolean = true,
    val archived: Boolean = false,
    val nameHebrew: String = "",
    val presetId: String? = null,
    val startDate: String? = null,
    val targetDate: String? = null,
    val missedWorkBehavior: String = "KEEP_FIXED_OVERDUE",
    val repeatsAnnually: Boolean = false,
    val officialOraysaChazarah: Boolean = false,
    val generationRevision: Int = 1,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val revision: Long = 0,
) {
    fun domain() = LearningSchedule(id, name, material, pace, weekdays, chazarahOffsets, active, archived)
}

@Serializable
data class StoredTask(
    val id: String,
    val scheduleId: String,
    val referenceEnglish: String,
    val referenceHebrew: String,
    val dueDate: String,
    val type: String,
    val completed: Boolean = false,
    val stableKey: String = id,
    val materialType: String = "CUSTOM_UNIT",
    val quantity: Double = 1.0,
    val originalLearningDate: String = dueDate,
    val reviewIdentity: String? = null,
    val generationRevision: Int = 1,
    val completedAt: String? = null,
    val completionLocalDate: String? = null,
    val completionZoneId: String? = null,
    val updatedAt: String? = null,
    val revision: Long = 0,
) {
    fun domain() = LearningTask(
        id, scheduleId, referenceEnglish, referenceHebrew, dueDate,
        LearningTaskType.valueOf(type), completed,
    )
}

@Serializable
data class WebAppState(
    val schedules: List<StoredSchedule> = emptyList(),
    val tasks: List<StoredTask> = emptyList(),
    val language: String = "en",
    val sefarimLanguage: String = "BOTH",
    val primaryCalendar: String = "GREGORIAN",
    val defaultChazarahOffsets: List<Int> = listOf(1, 7, 30, 90),
    val preferencesRevision: Long = 0,
) {
    companion object {
        fun sample(today: String) = WebAppState(
            schedules = listOf(
                StoredSchedule("daf-yomi", "Daf Yomi Bavli", "Gemara", 1),
                StoredSchedule("mishnah", "Mishnah Yomis", "Mishnah", 2),
            ),
            tasks = listOf(
                StoredTask("daf-today", "daf-yomi", "Berachos 18", "ברכות דף י״ח", today, "LEARNING"),
                StoredTask("daf-review", "daf-yomi", "Berachos 17", "ברכות דף י״ז", today, "CHAZARAH"),
                StoredTask("mishnah-today", "mishnah", "Peah 2:3–4", "פאה ב׳:ג׳–ד׳", today, "LEARNING"),
                StoredTask("mishnah-review", "mishnah", "Peah 2:1–2", "פאה ב׳:א׳–ב׳", today, "CHAZARAH", true),
            ),
        )
    }
}

@Serializable
data class WebBackup(
    val version: Int = 2,
    val state: WebAppState,
    val canonical: CanonicalDataSet? = null,
)

sealed interface BackupImportResult {
    data object None : BackupImportResult
    data object Invalid : BackupImportResult
    data class Ready(val state: WebAppState) : BackupImportResult
}

fun WebBackup.validStateOrNull(): WebAppState? {
    if (version !in 1..2 || state.language !in setOf("en", "he")) return null
    if (state.sefarimLanguage !in CanonicalSefarimLanguage.entries.map { it.name } ||
        state.primaryCalendar !in CanonicalPrimaryCalendar.entries.map { it.name } ||
        state.defaultChazarahOffsets.isEmpty() || state.defaultChazarahOffsets.any { it <= 0 }
    ) return null
    if (version >= 2 && (canonical == null || CanonicalDataValidator.validate(canonical).isNotEmpty())) return null
    if (state.schedules.size > 500 || state.tasks.size > 50_000) return null
    val scheduleIds = state.schedules.map { it.id }
    if (scheduleIds.any { it.isBlank() } || scheduleIds.distinct().size != scheduleIds.size) return null
    if (state.schedules.any { schedule ->
            schedule.name.isBlank() || schedule.material.isBlank() || schedule.pace !in 1..20 ||
                schedule.weekdays.isEmpty() || schedule.weekdays.any { it !in 0..6 } ||
                schedule.chazarahOffsets.any { it !in 1..3650 }
        }
    ) return null
    val taskIds = state.tasks.map { it.id }
    if (taskIds.any { it.isBlank() } || taskIds.distinct().size != taskIds.size) return null
    val knownSchedules = scheduleIds.toSet()
    if (state.tasks.any { task ->
            task.scheduleId !in knownSchedules || task.referenceEnglish.length > 500 || task.referenceHebrew.length > 500 ||
                task.type !in LearningTaskType.entries.map { it.name } || IsoDate.parse(task.dueDate) == null
        }
    ) return null
    if (version >= 2 && canonical?.let { value ->
            value.schedules.map { it.id }.toSet() != scheduleIds.toSet() ||
                value.tasks.map { it.id }.toSet() != taskIds.toSet()
        } == true
    ) return null
    return state
}

fun WebAppState.toCanonical(now: String, today: String): CanonicalDataSet {
    val tasksBySchedule = tasks.groupBy { it.scheduleId }
    return CanonicalDataSet(
        schedules = schedules.map { schedule ->
            val scheduleTasks = tasksBySchedule[schedule.id].orEmpty()
            CanonicalSchedule(
                id = schedule.id,
                nameEnglish = schedule.name,
                nameHebrew = schedule.nameHebrew,
                kind = if (schedule.presetId == null) CanonicalScheduleKind.CUSTOM else CanonicalScheduleKind.PRESET,
                sourceType = schedule.material,
                materialType = canonicalMaterialType(schedule.material),
                presetId = schedule.presetId,
                startDate = schedule.startDate ?: scheduleTasks.minOfOrNull { it.dueDate } ?: today,
                targetDate = schedule.targetDate ?: scheduleTasks.filter { it.type == "LEARNING" }.maxOfOrNull { it.dueDate },
                dailyQuantity = schedule.pace,
                selectedWeekdays = schedule.weekdays,
                chazarahDayOffsets = schedule.chazarahOffsets,
                repeatsAnnually = schedule.repeatsAnnually,
                officialOraysaChazarah = schedule.officialOraysaChazarah,
                missedWorkBehavior = runCatching { CanonicalMissedWorkBehavior.valueOf(schedule.missedWorkBehavior) }
                    .getOrDefault(CanonicalMissedWorkBehavior.KEEP_FIXED_OVERDUE),
                state = when {
                    schedule.archived -> CanonicalScheduleState.ARCHIVED
                    schedule.active -> CanonicalScheduleState.ACTIVE
                    else -> CanonicalScheduleState.PAUSED
                },
                generationRevision = schedule.generationRevision,
                createdAt = schedule.createdAt ?: now,
                updatedAt = schedule.updatedAt ?: now,
                revision = schedule.revision,
            )
        },
        tasks = tasks.map { task ->
            val completionDate = task.completionLocalDate ?: task.dueDate.takeIf { task.completed }
            CanonicalTask(
                id = task.id,
                stableKey = task.stableKey,
                scheduleId = task.scheduleId,
                type = runCatching { CanonicalTaskType.valueOf(task.type) }.getOrDefault(CanonicalTaskType.LEARNING),
                labelEnglish = task.referenceEnglish,
                labelHebrew = task.referenceHebrew,
                materialType = runCatching { CanonicalMaterialType.valueOf(task.materialType) }
                    .getOrDefault(CanonicalMaterialType.CUSTOM_UNIT),
                quantity = task.quantity,
                plannedDate = task.dueDate,
                originalLearningDate = task.originalLearningDate,
                reviewIdentity = task.reviewIdentity,
                generationRevision = task.generationRevision,
                completedAt = task.completedAt ?: now.takeIf { task.completed },
                completionLocalDate = completionDate,
                completionZoneId = task.completionZoneId ?: "browser-local".takeIf { task.completed },
                updatedAt = task.updatedAt ?: now,
                revision = task.revision,
            )
        },
        preferences = CanonicalPreferences(
            appLanguage = language,
            sefarimLanguage = runCatching { CanonicalSefarimLanguage.valueOf(sefarimLanguage) }
                .getOrDefault(CanonicalSefarimLanguage.BOTH),
            primaryCalendar = runCatching { CanonicalPrimaryCalendar.valueOf(primaryCalendar) }
                .getOrDefault(CanonicalPrimaryCalendar.GREGORIAN),
            defaultChazarahOffsets = defaultChazarahOffsets,
            updatedAt = now,
            revision = preferencesRevision,
        ),
    )
}

private fun canonicalMaterialType(value: String): CanonicalMaterialType = when (value.lowercase()) {
    "gemara", "daf" -> CanonicalMaterialType.DAF
    "amud" -> CanonicalMaterialType.AMUD
    "mishnah", "mishnayos" -> CanonicalMaterialType.MISHNAH
    "perek", "perakim" -> CanonicalMaterialType.PEREK
    "page" -> CanonicalMaterialType.PAGE
    "seif" -> CanonicalMaterialType.SEIF
    "siman", "kitzur" -> CanonicalMaterialType.SIMAN
    else -> CanonicalMaterialType.CUSTOM_UNIT
}

interface BrowserStore {
    fun load(): WebAppState
    fun save(state: WebAppState)
    fun currentInstant(): String
    fun currentZoneId(): String
    fun exportBackup(state: WebAppState)
    fun requestBackupImport()
    fun consumeBackupImport(): BackupImportResult
}

@Serializable
data class CloudAccountState(
    val configured: Boolean,
    val email: String? = null,
    val status: String? = null,
    val conflict: Boolean = false,
)

interface CloudAccount {
    fun state(): CloudAccountState
    fun requestMagicLink(email: String)
    fun sync(state: WebAppState)
    fun useCloudCopy()
    fun replaceCloudCopy(state: WebAppState)
    fun signOut()
}
