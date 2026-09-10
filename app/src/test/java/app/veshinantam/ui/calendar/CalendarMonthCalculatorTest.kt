package app.veshinantam.ui.calendar

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarMonthCalculatorTest {
    @Test
    fun `month grid starts on Sunday and fills complete weeks`() {
        val cells = CalendarMonthCalculator.build(YearMonth.of(2026, 9), emptyList())

        assertEquals(2, cells.indexOfFirst { it?.date == LocalDate.of(2026, 9, 1) })
        assertEquals(35, cells.size)
        assertEquals(LocalDate.of(2026, 9, 30), cells.filterNotNull().last().date)
    }

    @Test
    fun `day status distinguishes incomplete partial and complete work`() {
        val month = YearMonth.of(2026, 9)
        val cells = CalendarMonthCalculator.build(
            month,
            listOf(
                CalendarTaskStatus(LocalDate.of(2026, 9, 1), false),
                CalendarTaskStatus(LocalDate.of(2026, 9, 2), true),
                CalendarTaskStatus(LocalDate.of(2026, 9, 2), false),
                CalendarTaskStatus(LocalDate.of(2026, 9, 3), true),
            ),
        ).filterNotNull().associateBy { it.date }

        assertEquals(CalendarDayStatus.INCOMPLETE, cells.getValue(LocalDate.of(2026, 9, 1)).status)
        assertEquals(CalendarDayStatus.PARTIAL, cells.getValue(LocalDate.of(2026, 9, 2)).status)
        assertEquals(CalendarDayStatus.COMPLETE, cells.getValue(LocalDate.of(2026, 9, 3)).status)
        assertEquals(CalendarDayStatus.NONE, cells.getValue(LocalDate.of(2026, 9, 4)).status)
    }

    @Test
    fun `calendar range supports lunar months crossing Gregorian boundaries`() {
        val start = LocalDate.of(2026, 9, 12)
        val end = LocalDate.of(2026, 10, 10)
        val cells = CalendarMonthCalculator.build(start, end, emptyList()).filterNotNull()

        assertEquals(29, cells.size)
        assertEquals(start, cells.first().date)
        assertEquals(end, cells.last().date)
    }
}
