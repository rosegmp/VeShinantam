package app.veshinantam.data.pdf

import app.veshinantam.data.local.TodayTaskRow
import app.veshinantam.domain.model.TaskType
import app.veshinantam.localization.AppLanguage
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class PrintableScheduleRulesTest {
    @Test
    fun `selected day count includes today as first day`() {
        assertEquals(
            LocalDate.parse("2026-10-07"),
            PrintableScheduleRules.endDate(LocalDate.parse("2026-09-08"), 30),
        )
    }

    @Test
    fun `schedule name follows UI language with single-language fallback`() {
        val bilingual = task("Daf Yomi", "דף יומי")
        val hebrewOnly = task("", "משנה יומית")

        assertEquals("Daf Yomi", PrintableScheduleRules.scheduleName(bilingual, AppLanguage.ENGLISH))
        assertEquals("דף יומי", PrintableScheduleRules.scheduleName(bilingual, AppLanguage.HEBREW))
        assertEquals("משנה יומית", PrintableScheduleRules.scheduleName(hebrewOnly, AppLanguage.ENGLISH))
    }

    private fun task(english: String, hebrew: String) = TodayTaskRow(
        taskId = "task", scheduleId = "schedule", scheduleNameEnglish = english, scheduleNameHebrew = hebrew,
        scheduleCreatedAt = Instant.EPOCH, type = TaskType.LEARNING, labelEnglish = "Berachos 2a", labelHebrew = "ברכות ב.",
        plannedDate = LocalDate.parse("2026-09-08"), completedAt = null,
    )
}
