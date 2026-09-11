package app.veshinantam.web

import app.veshinantam.shared.LearningSchedule
import app.veshinantam.shared.LearningTask
import app.veshinantam.shared.LearningTaskType
import kotlinx.serialization.Serializable

@Serializable
data class StoredSchedule(
    val id: String,
    val name: String,
    val material: String,
    val pace: Int,
    val active: Boolean = true,
) {
    fun domain() = LearningSchedule(id, name, material, pace, active)
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

interface BrowserStore {
    fun load(): WebAppState
    fun save(state: WebAppState)
}
