package app.veshinantam.ui.calendar

import android.icu.util.HebrewCalendar
import android.icu.util.TimeZone
import app.veshinantam.shared.text.HebrewNumerals
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

data class HebrewMonthPeriod(
    val start: LocalDate,
    val endInclusive: LocalDate,
    val month: Int,
    val year: Int,
)

object HebrewDateFormatter {
    fun format(date: LocalDate, locale: Locale): String {
        val calendar = calendarFor(date)
        val day = calendar.get(HebrewCalendar.DAY_OF_MONTH)
        val month = calendar.get(HebrewCalendar.MONTH)
        val year = calendar.get(HebrewCalendar.YEAR)
        val hebrewUi = locale.language == "he"
        val monthName = if (hebrewUi) hebrewMonths[month] else ashkenaziMonths[month]
        return if (hebrewUi) {
            "${hebrewNumber(day)} $monthName ${hebrewNumber(year % 1000)}"
        } else {
            "$day $monthName $year"
        }
    }

    fun monthContaining(date: LocalDate): HebrewMonthPeriod {
        val source = calendarFor(date)
        val year = source.get(HebrewCalendar.YEAR)
        val month = source.get(HebrewCalendar.MONTH)
        val first = HebrewCalendar(utc).apply {
            clear()
            set(HebrewCalendar.YEAR, year)
            set(HebrewCalendar.MONTH, month)
            set(HebrewCalendar.DAY_OF_MONTH, 1)
        }
        val start = localDate(first.timeInMillis)
        return HebrewMonthPeriod(start, start.plusDays(first.getActualMaximum(HebrewCalendar.DAY_OF_MONTH).toLong() - 1), month, year)
    }

    fun monthTitle(period: HebrewMonthPeriod, locale: Locale): String {
        val hebrewUi = locale.language == "he"
        val monthName = if (hebrewUi) hebrewMonths[period.month] else ashkenaziMonths[period.month]
        return if (hebrewUi) "$monthName ${hebrewNumber(period.year % 1000)}" else "$monthName ${period.year}"
    }

    fun dayLabel(date: LocalDate, locale: Locale): String {
        val day = calendarFor(date).get(HebrewCalendar.DAY_OF_MONTH)
        return if (locale.language == "he") HebrewNumerals.format(day) else day.toString()
    }

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    private fun calendarFor(date: LocalDate): HebrewCalendar = HebrewCalendar(utc).apply {
        time = Date.from(date.atStartOfDay(ZoneOffset.UTC).toInstant())
    }

    private fun localDate(timeInMillis: Long): LocalDate =
        Instant.ofEpochMilli(timeInMillis).atZone(ZoneOffset.UTC).toLocalDate()

    private val ashkenaziMonths = listOf(
        "Tishrei", "Cheshvan", "Kislev", "Teves", "Shevat", "Adar I", "Adar", "Nisan", "Iyar", "Sivan", "Tammuz", "Av", "Elul",
    )
    private val hebrewMonths = listOf(
        "תשרי", "חשוון", "כסלו", "טבת", "שבט", "אדר א׳", "אדר", "ניסן", "אייר", "סיוון", "תמוז", "אב", "אלול",
    )

    private fun hebrewNumber(value: Int): String {
        require(value in 1..999)
        var remaining = value
        val result = StringBuilder()
        val hundreds = listOf(400 to 'ת', 300 to 'ש', 200 to 'ר', 100 to 'ק')
        hundreds.forEach { (number, letter) ->
            while (remaining >= number) {
                result.append(letter)
                remaining -= number
            }
        }
        when {
            remaining == 15 -> { result.append("טו"); remaining = 0 }
            remaining == 16 -> { result.append("טז"); remaining = 0 }
        }
        val tens = listOf(90 to 'צ', 80 to 'פ', 70 to 'ע', 60 to 'ס', 50 to 'נ', 40 to 'מ', 30 to 'ל', 20 to 'כ', 10 to 'י')
        tens.firstOrNull { remaining >= it.first }?.let { (number, letter) -> result.append(letter); remaining -= number }
        val ones = listOf(' ', 'א', 'ב', 'ג', 'ד', 'ה', 'ו', 'ז', 'ח', 'ט')
        if (remaining > 0) result.append(ones[remaining])
        return when (result.length) {
            1 -> "$result׳"
            else -> result.insert(result.length - 1, '״').toString()
        }
    }
}
