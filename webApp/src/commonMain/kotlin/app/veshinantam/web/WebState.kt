package app.veshinantam.web

import app.veshinantam.shared.LearningSchedule
import app.veshinantam.shared.LearningTask
import app.veshinantam.shared.LearningTaskType
import app.veshinantam.shared.IsoDate
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
    val version: Int = 1,
    val state: WebAppState,
)

sealed interface BackupImportResult {
    data object None : BackupImportResult
    data object Invalid : BackupImportResult
    data class Ready(val state: WebAppState) : BackupImportResult
}

fun WebBackup.validStateOrNull(): WebAppState? {
    if (version != 1 || state.language !in setOf("en", "he")) return null
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
    return state
}

interface BrowserStore {
    fun load(): WebAppState
    fun save(state: WebAppState)
    fun exportBackup(state: WebAppState)
    fun requestBackupImport()
    fun consumeBackupImport(): BackupImportResult
}
