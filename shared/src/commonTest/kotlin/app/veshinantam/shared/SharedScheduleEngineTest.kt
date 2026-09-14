package app.veshinantam.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class SharedScheduleEngineTest {
    private val engine = SharedScheduleEngine()
    private val weekdays = SharedScheduleRules(setOf(1, 2, 3, 4, 5))
    private val units = (1..5).map { SharedMaterialUnit("u$it", it, "Unit $it", "יחידה $it") }

    @Test
    fun dailyQuantitySkipsWeekendAndPreservesOrder() {
        val tasks = engine.generateByDailyQuantity(units, IsoDate(2026, 9, 18), 2, weekdays)
        assertEquals(listOf("2026-09-18", "2026-09-18", "2026-09-21", "2026-09-21", "2026-09-22"), tasks.map { it.plannedDate.toString() })
        assertEquals(units.map { it.id }, tasks.map { it.material.id })
    }

    @Test
    fun finishByDistributesRemainderToEarlierEligibleDays() {
        val tasks = engine.generateByCompletionDate(units, IsoDate(2026, 9, 14), IsoDate(2026, 9, 16), weekdays)
        assertEquals(listOf("2026-09-14", "2026-09-14", "2026-09-15", "2026-09-15", "2026-09-16"), tasks.map { it.plannedDate.toString() })
    }

    @Test
    fun chazarahOffsetsUseSuccessiveEligibleSlots() {
        val learning = engine.generateByDailyQuantity(units.take(1), IsoDate(2026, 9, 18), 1, weekdays)
        val reviews = engine.generateChazarah(learning, listOf(1, 7), false, weekdays, 2026)
        assertEquals(listOf("2026-09-21", "2026-09-30"), reviews.map { it.plannedDate.toString() })
        assertEquals(listOf("day:1", "day:7"), reviews.map { it.reviewIdentity })
    }

    @Test
    fun leapDayAnnualReviewUsesFebruaryTwentyEight() {
        val everyDay = SharedScheduleRules((0..6).toSet())
        val learning = engine.generateByDailyQuantity(units.take(1), IsoDate(2024, 2, 29), 1, everyDay)
        val reviews = engine.generateChazarah(learning, emptyList(), true, everyDay, 2025)
        assertEquals("2025-02-28", reviews.single().plannedDate.toString())
    }
}
