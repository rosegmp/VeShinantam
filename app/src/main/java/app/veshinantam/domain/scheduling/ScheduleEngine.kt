package app.veshinantam.domain.scheduling

import app.veshinantam.domain.model.ChazarahPattern
import app.veshinantam.domain.model.MaterialUnit
import app.veshinantam.domain.model.PlannedTask
import app.veshinantam.domain.model.ScheduleRules
import app.veshinantam.domain.model.TaskType
import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalDate

/** Pure, deterministic scheduling rules. Persistence and UI deliberately live elsewhere. */
class ScheduleEngine {
    fun nextEligibleDate(candidate: LocalDate, rules: ScheduleRules): LocalDate {
        var date = candidate
        repeat(MAX_SEARCH_DAYS) {
            if (rules.isEligible(date)) return date
            date = date.plusDays(1)
        }
        error("No eligible date found within $MAX_SEARCH_DAYS days")
    }

    fun generateByDailyQuantity(
        units: List<MaterialUnit>,
        startDate: LocalDate,
        unitsPerDay: Int,
        rules: ScheduleRules,
    ): List<PlannedTask> {
        require(unitsPerDay > 0) { "Daily quantity must be positive" }
        if (units.isEmpty()) return emptyList()

        val tasks = mutableListOf<PlannedTask>()
        var date = nextEligibleDate(startDate, rules)
        units.chunked(unitsPerDay).forEachIndexed { index, assignment ->
            assignment.forEach { unit ->
                tasks += learningTask(unit, date)
            }
            if (index < (units.size - 1) / unitsPerDay) {
                date = nextEligibleDate(date.plusDays(1), rules)
            }
        }
        return tasks
    }

    fun generateByCompletionDate(
        units: List<MaterialUnit>,
        startDate: LocalDate,
        targetDate: LocalDate,
        rules: ScheduleRules,
    ): List<PlannedTask> {
        require(!targetDate.isBefore(startDate)) { "Completion date cannot precede start date" }
        if (units.isEmpty()) return emptyList()

        val dates = eligibleDates(startDate, targetDate, rules)
        require(dates.isNotEmpty()) { "The date range contains no selected learning days" }
        if (units.size < dates.size) {
            val assignedDates = if (units.size == 1) {
                listOf(dates.last())
            } else {
                units.indices.map { index ->
                    dates[index * (dates.lastIndex) / units.lastIndex]
                }
            }
            return units.zip(assignedDates) { unit, date -> learningTask(unit, date) }
        }

        val base = units.size / dates.size
        val largerDayCount = units.size % dates.size
        var unitIndex = 0
        return buildList {
            dates.forEachIndexed { dayIndex, date ->
                val count = base + if (dayIndex < largerDayCount) 1 else 0
                repeat(count) {
                    add(learningTask(units[unitIndex++], date))
                }
            }
        }
    }

    fun generateChazarah(
        learningTasks: List<PlannedTask>,
        pattern: ChazarahPattern,
        rules: ScheduleRules,
        annualReviewsThroughYear: Int,
    ): List<PlannedTask> = buildList {
        if (learningTasks.isEmpty()) return@buildList
        learningTasks.forEach { require(it.type == TaskType.LEARNING) }

        val learningByDate = learningTasks.groupBy { it.originalLearningDate }.toSortedMap()
        val firstLearningDate = learningByDate.firstKey()
        val timeline = EligibleDateTimeline(firstLearningDate, rules, ::nextEligibleDate)
        val learningSlots = learningByDate.map { (date, learning) -> timeline.indexOf(date) to learning }
        var previousSlotLag = 0
        pattern.dayOffsets.sorted().forEach { offset ->
            // Offsets are successive gaps in eligible learning slots. Once a rest day moves
            // a chazarah, the whole series keeps its spacing instead of collapsing assignments.
            val slotLag = Math.addExact(previousSlotLag, offset)
            learningSlots.forEach { (learningSlot, learningSet) ->
                val reviewDate = timeline.dateAt(learningSlot + slotLag)
                learningSet.forEach { learning ->
                    add(reviewTask(learning, reviewDate, "day:$offset"))
                }
            }
            previousSlotLag = slotLag
        }

        if (pattern.repeatsAnnually) {
            for (annualNumber in 1..annualReviewsThroughYear - firstLearningDate.year) {
                val firstTargetDate = sameGregorianDate(firstLearningDate, firstLearningDate.year + annualNumber)
                val calculatedLag = timeline.indexOf(nextEligibleDate(firstTargetDate, rules))
                val slotLag = calculatedLag.coerceAtLeast(previousSlotLag + 1)
                learningSlots.forEach { (learningSlot, learningSet) ->
                    val reviewDate = timeline.dateAt(learningSlot + slotLag)
                    learningSet.forEach { learning ->
                        val annualYear = learning.originalLearningDate.year + annualNumber
                        if (annualYear <= annualReviewsThroughYear) {
                            add(reviewTask(learning, reviewDate, "annual:$annualYear"))
                        }
                    }
                }
                previousSlotLag = slotLag
            }
        }
    }

    fun generateOfficialOraysaChazarah(
        learningTasks: List<PlannedTask>,
        learningRules: ScheduleRules,
    ): List<PlannedTask> = buildList {
        if (learningTasks.isEmpty()) return@buildList
        learningTasks.forEach { require(it.type == TaskType.LEARNING) }

        addAll(
            generateChazarah(
                learningTasks = learningTasks,
                pattern = ChazarahPattern(dayOffsets = listOf(1), repeatsAnnually = false),
                rules = learningRules,
                annualReviewsThroughYear = learningTasks.first().plannedDate.year,
            ).map { it.copy(reviewIdentity = "oraysa:daily") },
        )

        addAll(generateWeekendChazarah(learningTasks))
    }

    /** Splits Sunday-Monday learning onto Friday and Tuesday-Thursday learning onto Shabbos. */
    fun generateWeekendChazarah(learningTasks: List<PlannedTask>): List<PlannedTask> = buildList {
        if (learningTasks.isEmpty()) return@buildList
        learningTasks.forEach { require(it.type == TaskType.LEARNING) }

        learningTasks.groupBy { task ->
            task.plannedDate.minusDays((task.plannedDate.dayOfWeek.value % 7).toLong())
        }.toSortedMap().forEach { (sunday, week) ->
            val byDay = week.groupBy(PlannedTask::plannedDate)
            val fridayMaterial = listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY)
                .flatMap { day -> byDay[sunday.plusDays((day.value % 7).toLong())].orEmpty() }
            val shabbosMaterial = listOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY)
                .flatMap { day -> byDay[sunday.plusDays((day.value % 7).toLong())].orEmpty() }
            if (fridayMaterial.isNotEmpty()) add(weekendTask(fridayMaterial, sunday.plusDays(5), "friday"))
            if (shabbosMaterial.isNotEmpty()) add(weekendTask(shabbosMaterial, sunday.plusDays(6), "shabbos"))
        }
    }

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
