package app.veshinantam.domain.material

import app.veshinantam.domain.model.BilingualLabel
import app.veshinantam.domain.model.MaterialUnit
import app.veshinantam.domain.model.PlannedTask
import app.veshinantam.domain.model.TaskType
import app.veshinantam.shared.preset.MaterialCatalog
import app.veshinantam.shared.preset.UnitReference
import app.veshinantam.ui.calendar.HebrewDateFormatter
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Calendar-based, repeating Tehillim schedule. */
object MonthlyTehillim {
    const val PRESET_ID = "tehillim-monthly"
    const val REVISION = 2
    const val SOURCE_MARKER = ":HEBREW_MONTH_V2"
    const val HORIZON_DAYS = 120L

    fun referenceOn(date: LocalDate): UnitReference {
        val month = HebrewDateFormatter.monthContaining(date)
        val day = ChronoUnit.DAYS.between(month.start, date).toInt() + 1
        val length = ChronoUnit.DAYS.between(month.start, month.endInclusive).toInt() + 1
        return MaterialCatalog.monthlyTehillimForDay(day, length)
    }

    fun indexOn(date: LocalDate): Int {
        val month = HebrewDateFormatter.monthContaining(date)
        return ChronoUnit.DAYS.between(month.start, date).toInt()
    }

    fun learningBetween(start: LocalDate, endInclusive: LocalDate): List<PlannedTask> = buildList {
        var date = start
        while (!date.isAfter(endInclusive)) {
            val reference = referenceOn(date)
            val material = MaterialUnit(
                id = "monthly-tehillim:$date",
                ordinal = indexOn(date),
                label = BilingualLabel(reference.english, reference.hebrew),
            )
            add(PlannedTask(
                stableKey = "learning:monthly-tehillim:$date",
                material = material,
                type = TaskType.LEARNING,
                plannedDate = date,
                originalLearningDate = date,
            ))
            date = date.plusDays(1)
        }
    }
}
