package app.veshinantam.domain.scheduling

import app.veshinantam.domain.model.ChazarahPattern
import app.veshinantam.domain.model.MaterialUnit
import app.veshinantam.domain.model.PlannedTask
import app.veshinantam.domain.model.ScheduleRules
import app.veshinantam.domain.model.TaskType
import app.veshinantam.shared.IsoDate
import app.veshinantam.shared.LearningTaskType
import app.veshinantam.shared.SharedMaterialUnit
import app.veshinantam.shared.SharedPlannedTask
import app.veshinantam.shared.SharedScheduleEngine
import app.veshinantam.shared.SharedScheduleRules
import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalDate

/** Pure, deterministic scheduling rules. Persistence and UI deliberately live elsewhere. */
class ScheduleEngine {
    private val shared = SharedScheduleEngine()

    fun nextEligibleDate(candidate: LocalDate, rules: ScheduleRules): LocalDate =
        shared.nextEligibleDate(candidate.toSharedDate(), rules.toSharedRules()).toAndroidDate()

    fun generateByDailyQuantity(
        units: List<MaterialUnit>,
        startDate: LocalDate,
        unitsPerDay: Int,
        rules: ScheduleRules,
    ): List<PlannedTask> = shared.generateByDailyQuantity(
        units.map { it.toSharedUnit() }, startDate.toSharedDate(), unitsPerDay, rules.toSharedRules(),
    ).map { it.toAndroidTask() }

    fun generateByCompletionDate(
        units: List<MaterialUnit>,
        startDate: LocalDate,
        targetDate: LocalDate,
        rules: ScheduleRules,
    ): List<PlannedTask> = shared.generateByCompletionDate(
        units.map { it.toSharedUnit() }, startDate.toSharedDate(), targetDate.toSharedDate(), rules.toSharedRules(),
    ).map { it.toAndroidTask() }

    fun generateChazarah(
        learningTasks: List<PlannedTask>,
        pattern: ChazarahPattern,
        rules: ScheduleRules,
        annualReviewsThroughYear: Int,
    ): List<PlannedTask> = shared.generateChazarah(
        learningTasks.map { it.toSharedTask() }, pattern.dayOffsets, pattern.repeatsAnnually,
        rules.toSharedRules(), annualReviewsThroughYear,
    ).map { it.toAndroidTask() }

    fun generateOfficialOraysaChazarah(
        learningTasks: List<PlannedTask>,
        learningRules: ScheduleRules,
    ): List<PlannedTask> = shared.generateOfficialOraysaChazarah(
        learningTasks.map { it.toSharedTask() }, learningRules.toSharedRules(),
    ).map { it.toAndroidTask() }

    /** Splits Sunday-Monday learning onto Friday and Tuesday-Thursday learning onto Shabbos. */
    fun generateWeekendChazarah(learningTasks: List<PlannedTask>): List<PlannedTask> =
        shared.generateWeekendChazarah(learningTasks.map { it.toSharedTask() }).map { it.toAndroidTask() }

    private fun LocalDate.toSharedDate() = IsoDate(year, monthValue, dayOfMonth)
    private fun IsoDate.toAndroidDate() = LocalDate.of(year, month, day)
    private fun ScheduleRules.toSharedRules() = SharedScheduleRules(
        selectedWeekdays.map { it.value % 7 }.toSet(),
        excludedDates.map { it.toSharedDate() }.toSet(),
    )
    private fun MaterialUnit.toSharedUnit() = SharedMaterialUnit(
        id, ordinal, label.english, label.hebrew, quantity,
    )
    private fun PlannedTask.toSharedTask() = SharedPlannedTask(
        stableKey, material.toSharedUnit(), LearningTaskType.valueOf(type.name),
        plannedDate.toSharedDate(), originalLearningDate.toSharedDate(), reviewIdentity,
    )
    private fun SharedPlannedTask.toAndroidTask() = PlannedTask(
        stableKey = stableKey,
        material = MaterialUnit(
            id = material.id,
            ordinal = material.ordinal,
            label = app.veshinantam.domain.model.BilingualLabel(material.labelEnglish, material.labelHebrew),
            quantity = material.quantity,
        ),
        type = TaskType.valueOf(type.name),
        plannedDate = plannedDate.toAndroidDate(),
        originalLearningDate = originalLearningDate.toAndroidDate(),
        reviewIdentity = reviewIdentity,
    )

    private fun eligibleDates(
        startDate: LocalDate,
        targetDate: LocalDate,
        rules: ScheduleRules,
    ): List<LocalDate> = buildList {
        var date = startDate
        while (!date.isAfter(targetDate)) {
            if (rules.isEligible(date)) add(date)
            date = date.plusDays(1)
        }
    }

    private fun learningTask(unit: MaterialUnit, date: LocalDate) = PlannedTask(
        stableKey = "learning:${unit.id}:$date",
        material = unit,
        type = TaskType.LEARNING,
        plannedDate = date,
        originalLearningDate = date,
    )

    private fun reviewTask(learning: PlannedTask, date: LocalDate, identity: String) = PlannedTask(
        stableKey = "review:${learning.stableKey}:$identity",
        material = learning.material,
        type = TaskType.CHAZARAH,
        plannedDate = date,
        originalLearningDate = learning.originalLearningDate,
        reviewIdentity = identity,
    )

    private fun weekendTask(
        learning: List<PlannedTask>,
        date: LocalDate,
        dayIdentity: String,
    ): PlannedTask {
        val first = learning.first().material
        val last = learning.last().material
        fun range(start: String, end: String): String = if (start == end) start else "$start – $end"
        val material = MaterialUnit(
            id = "weekend:${date.minusDays(if (dayIdentity == "friday") 5 else 6)}:$dayIdentity",
            ordinal = first.ordinal,
            label = app.veshinantam.domain.model.BilingualLabel(
                range(first.label.english, last.label.english),
                range(first.label.hebrew, last.label.hebrew),
            ),
            quantity = learning.sumOf { it.material.quantity },
        )
        return PlannedTask(
            stableKey = "review:${material.id}",
            material = material,
            type = TaskType.CHAZARAH,
            plannedDate = date,
            originalLearningDate = learning.first().originalLearningDate,
            reviewIdentity = "weekend:weekly:$dayIdentity",
        )
    }

    private fun sameGregorianDate(source: LocalDate, year: Int): LocalDate = try {
        source.withYear(year)
    } catch (_: DateTimeException) {
        // A February 29 anniversary is observed February 28 in a non-leap year.
        LocalDate.of(year, 2, 28)
    }

    private companion object {
        const val MAX_SEARCH_DAYS = 366 * 10
    }

    private class EligibleDateTimeline(
        firstDate: LocalDate,
        private val rules: ScheduleRules,
        private val nextEligibleDate: (LocalDate, ScheduleRules) -> LocalDate,
    ) {
        private val dates = mutableListOf(firstDate)
        private val indices = mutableMapOf(firstDate to 0)

        fun indexOf(date: LocalDate): Int {
            require(rules.isEligible(date))
            while (dates.last().isBefore(date)) appendDate()
            return requireNotNull(indices[date])
        }

        fun dateAt(index: Int): LocalDate {
            require(index >= 0)
            while (dates.lastIndex < index) appendDate()
            return dates[index]
        }

        private fun appendDate() {
            val nextDate = nextEligibleDate(dates.last().plusDays(1), rules)
            dates += nextDate
            indices[nextDate] = dates.lastIndex
        }
    }
}
