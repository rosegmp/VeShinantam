package app.veshinantam.domain.scheduling

import app.veshinantam.domain.model.BilingualLabel
import app.veshinantam.domain.model.ChazarahPattern
import app.veshinantam.domain.model.MaterialUnit
import app.veshinantam.domain.model.ScheduleRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ScheduleEngineTest {
    private val engine = ScheduleEngine()
    private val weekdays = ScheduleRules(
        selectedWeekdays = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        ),
    )

    @Test
    fun `official Oraysa chazarah includes daily reviews and weekend ranges`() {
        val oraysaRules = ScheduleRules(
            setOf(
                DayOfWeek.SUNDAY,
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
            ),
        )
        val learning = engine.generateByDailyQuantity(
            units(5),
            LocalDate.of(2026, 9, 6),
            1,
            oraysaRules,
        )

        val reviews = engine.generateOfficialOraysaChazarah(learning, oraysaRules)
        val daily = reviews.filter { it.reviewIdentity == "oraysa:daily" }
        val friday = reviews.single { it.reviewIdentity == "weekend:weekly:friday" }
        val shabbos = reviews.single { it.reviewIdentity == "weekend:weekly:shabbos" }

        assertEquals(5, daily.size)
        assertEquals(LocalDate.of(2026, 9, 11), friday.plannedDate)
        assertEquals(2.0, friday.material.quantity, 0.0)
        assertEquals("Unit 1 – Unit 2", friday.material.label.english)
        assertEquals(LocalDate.of(2026, 9, 12), shabbos.plannedDate)
        assertEquals(3.0, shabbos.material.quantity, 0.0)
        assertEquals("Unit 3 – Unit 5", shabbos.material.label.english)
        assertEquals(LocalDate.of(2026, 9, 13), daily.last().plannedDate)
    }

    @Test
    fun `weekend chazarah can be generated without Oraysa daily reviews`() {
        val learning = engine.generateByDailyQuantity(
            units(5),
            LocalDate.of(2026, 9, 6),
            1,
            ScheduleRules(DayOfWeek.entries.toSet()),
        )

        val reviews = engine.generateWeekendChazarah(learning)

        assertEquals(2, reviews.size)
        assertEquals(listOf(LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 12)), reviews.map { it.plannedDate })
        assertEquals(listOf(2.0, 3.0), reviews.map { it.material.quantity })
    }

    @Test
    fun `daily generation skips rest days and exclusions`() {
        val rules = weekdays.copy(excludedDates = setOf(LocalDate.of(2026, 9, 7)))
        val tasks = engine.generateByDailyQuantity(units(2), LocalDate.of(2026, 9, 5), 1, rules)

        assertEquals(LocalDate.of(2026, 9, 8), tasks[0].plannedDate)
        assertEquals(LocalDate.of(2026, 9, 9), tasks[1].plannedDate)
    }

    @Test
    fun `completion date assigns larger workloads to earliest days`() {
        val rules = ScheduleRules(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY))
        val tasks = engine.generateByCompletionDate(
            units = units(8),
            startDate = LocalDate.of(2026, 9, 7),
            targetDate = LocalDate.of(2026, 9, 9),
            rules = rules,
        )

        assertEquals(listOf(3, 3, 2), tasks.groupingBy { it.plannedDate }.eachCount().values.toList())
    }

    @Test
    fun `completion date spreads sparse units across the full range`() {
        val rules = ScheduleRules(DayOfWeek.entries.toSet())
        val tasks = engine.generateByCompletionDate(
            units = units(3),
            startDate = LocalDate.of(2026, 9, 1),
            targetDate = LocalDate.of(2026, 9, 7),
            rules = rules,
        )

        assertEquals(
            listOf(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 7)),
            tasks.map { it.plannedDate },
        )
    }

    @Test
    fun `successive offsets are gaps after the preceding review`() {
        val rules = ScheduleRules(setOf(DayOfWeek.MONDAY))
        val learning = engine.generateByDailyQuantity(units(1), LocalDate.of(2026, 9, 7), 1, rules)
        val reviews = engine.generateChazarah(
            learning,
            ChazarahPattern(dayOffsets = listOf(1, 2, 7), repeatsAnnually = false),
            rules,
            annualReviewsThroughYear = 2026,
        )

        assertEquals(
            listOf(LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 28), LocalDate.of(2026, 11, 16)),
            reviews.map { it.plannedDate },
        )
        assertNotEquals(reviews[0].stableKey, reviews[1].stableKey)
    }

    @Test
    fun `each interval counts eligible learning slots after the preceding review`() {
        val sundayThroughThursday = ScheduleRules(
            setOf(
                DayOfWeek.SUNDAY,
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
            ),
        )
        val learning = engine.generateByDailyQuantity(
            units(1),
            LocalDate.of(2026, 9, 10),
            1,
            sundayThroughThursday,
        )
        val reviews = engine.generateChazarah(
            learning,
            ChazarahPattern(dayOffsets = listOf(1, 7), repeatsAnnually = false),
            sundayThroughThursday,
            annualReviewsThroughYear = 2026,
        )

        assertEquals(
            listOf(LocalDate.of(2026, 9, 13), LocalDate.of(2026, 9, 22)),
            reviews.map { it.plannedDate },
        )
    }

    @Test
    fun `later learning sets retain the same review slot lags`() {
        val sundayThroughThursday = ScheduleRules(
            setOf(
                DayOfWeek.SUNDAY,
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
            ),
        )
        val learning = engine.generateByDailyQuantity(
            units(2),
            LocalDate.of(2026, 9, 10),
            1,
            sundayThroughThursday,
        )
        val reviews = engine.generateChazarah(
            learning,
            ChazarahPattern(dayOffsets = listOf(1, 7), repeatsAnnually = false),
            sundayThroughThursday,
            annualReviewsThroughYear = 2026,
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 9, 13),
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 22),
                LocalDate.of(2026, 9, 23),
            ),
            reviews.map { it.plannedDate },
        )
    }

    @Test
    fun `corrected spreadsheet chains one seven and thirty learning slot gaps`() {
        val sundayThroughThursday = ScheduleRules(
            setOf(
                DayOfWeek.SUNDAY,
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
            ),
        )
        val learning = engine.generateByDailyQuantity(
            units(5),
            LocalDate.of(2025, 11, 24),
            1,
            sundayThroughThursday,
        )
        val reviews = engine.generateChazarah(
            learning,
            ChazarahPattern(dayOffsets = listOf(1, 7, 30), repeatsAnnually = false),
            sundayThroughThursday,
            annualReviewsThroughYear = 2026,
        )

        assertEquals(
            listOf(
                LocalDate.of(2025, 11, 25),
                LocalDate.of(2025, 12, 4),
                LocalDate.of(2026, 1, 15),
            ),
            reviews.filter { it.material.id == "unit-1" }.map { it.plannedDate },
        )
        assertEquals(
            listOf(
                LocalDate.of(2025, 12, 1),
                LocalDate.of(2025, 12, 10),
                LocalDate.of(2026, 1, 21),
            ),
            reviews.filter { it.material.id == "unit-5" }.map { it.plannedDate },
        )
    }

    @Test
    fun `default pattern keeps one stream per review without collisions`() {
        val learning = engine.generateByDailyQuantity(units(2), LocalDate.of(2026, 1, 1), 1, weekdays)
        val reviews = engine.generateChazarah(
            learning,
            ChazarahPattern(dayOffsets = listOf(1, 7, 30, 180), repeatsAnnually = false),
            weekdays,
            annualReviewsThroughYear = 2026,
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 2),
                LocalDate.of(2026, 1, 13),
                LocalDate.of(2026, 2, 24),
                LocalDate.of(2026, 11, 3),
            ),
            reviews.filter { it.material.id == "unit-1" }.map { it.plannedDate },
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 5),
                LocalDate.of(2026, 1, 14),
                LocalDate.of(2026, 2, 25),
                LocalDate.of(2026, 11, 4),
            ),
            reviews.filter { it.material.id == "unit-2" }.map { it.plannedDate },
        )
    }

    @Test
    fun `review offsets remain unchanged when every date is a learning day`() {
        val allDays = ScheduleRules(DayOfWeek.entries.toSet())
        val learning = engine.generateByDailyQuantity(units(1), LocalDate.of(2026, 9, 7), 1, allDays)
        val reviews = engine.generateChazarah(
            learning,
            ChazarahPattern(dayOffsets = listOf(1, 7, 30), repeatsAnnually = false),
            allDays,
            annualReviewsThroughYear = 2026,
        )

        assertEquals(
            listOf(LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 15), LocalDate.of(2026, 10, 15)),
            reviews.map { it.plannedDate },
        )
    }

    @Test
    fun `annual reviews use their own learning slot lag`() {
        val learning = engine.generateByDailyQuantity(units(1), LocalDate.of(2026, 9, 10), 1, weekdays)
        val reviews = engine.generateChazarah(
            learning,
            ChazarahPattern(dayOffsets = listOf(2), repeatsAnnually = true),
            weekdays,
            annualReviewsThroughYear = 2027,
        )

        assertEquals(
            listOf(LocalDate.of(2026, 9, 14), LocalDate.of(2027, 9, 10)),
            reviews.map { it.plannedDate },
        )
    }

    @Test
    fun `overlapping day-offset and annual reviews use separate days`() {
        val allDays = ScheduleRules(DayOfWeek.entries.toSet())
        val learning = engine.generateByDailyQuantity(units(1), LocalDate.of(2023, 1, 1), 1, allDays)
        val reviews = engine.generateChazarah(
            learning,
            ChazarahPattern(dayOffsets = listOf(365), repeatsAnnually = true),
            allDays,
            annualReviewsThroughYear = 2024,
        )

        assertEquals(
            listOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2)),
            reviews.map { it.plannedDate },
        )
    }

    @Test
    fun `leap day annual review uses february 28 then selected day`() {
        val allDays = ScheduleRules(DayOfWeek.entries.toSet())
        val learning = engine.generateByDailyQuantity(units(1), LocalDate.of(2024, 2, 29), 1, allDays)
        val review = engine.generateChazarah(
            learning,
            ChazarahPattern(dayOffsets = emptyList(), repeatsAnnually = true),
            allDays,
            annualReviewsThroughYear = 2025,
        ).single()

        assertEquals(LocalDate.of(2025, 2, 28), review.plannedDate)
    }

    private fun units(count: Int) = (1..count).map {
        MaterialUnit("unit-$it", it, BilingualLabel("Unit $it", "יחידה $it"))
    }
}
