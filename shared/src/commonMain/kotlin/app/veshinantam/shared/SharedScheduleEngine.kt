package app.veshinantam.shared

data class SharedScheduleRules(
    val selectedWeekdays: Set<Int>,
    val excludedDates: Set<IsoDate> = emptySet(),
) {
    init {
        require(selectedWeekdays.isNotEmpty()) { "At least one weekday must be selected" }
        require(selectedWeekdays.all { it in 0..6 }) { "Weekdays must be in the range 0..6" }
    }

    fun isEligible(date: IsoDate): Boolean =
        GregorianCalendar.dayOfWeek(date.year, date.month, date.day) in selectedWeekdays && date !in excludedDates
}

data class SharedMaterialUnit(
    val id: String,
    val ordinal: Int,
    val labelEnglish: String,
    val labelHebrew: String,
    val quantity: Double = 1.0,
)

data class SharedPlannedTask(
    val stableKey: String,
    val material: SharedMaterialUnit,
    val type: LearningTaskType,
    val plannedDate: IsoDate,
    val originalLearningDate: IsoDate,
    val reviewIdentity: String? = null,
)

/** Platform-neutral scheduling rules used by both Android and Wasm. */
class SharedScheduleEngine {
    fun nextEligibleDate(candidate: IsoDate, rules: SharedScheduleRules): IsoDate {
        var date = candidate
        repeat(MAX_SEARCH_DAYS) {
            if (rules.isEligible(date)) return date
            date = date.plusDays(1)
        }
        error("No eligible date found within $MAX_SEARCH_DAYS days")
    }

    fun generateByDailyQuantity(
        units: List<SharedMaterialUnit>,
        startDate: IsoDate,
        unitsPerDay: Int,
        rules: SharedScheduleRules,
    ): List<SharedPlannedTask> {
        require(unitsPerDay > 0) { "Daily quantity must be positive" }
        if (units.isEmpty()) return emptyList()
        val tasks = mutableListOf<SharedPlannedTask>()
        var date = nextEligibleDate(startDate, rules)
        units.chunked(unitsPerDay).forEachIndexed { index, assignment ->
            assignment.forEach { unit -> tasks += learningTask(unit, date) }
            if (index < (units.size - 1) / unitsPerDay) date = nextEligibleDate(date.plusDays(1), rules)
        }
        return tasks
    }

    fun generateByCompletionDate(
        units: List<SharedMaterialUnit>,
        startDate: IsoDate,
        targetDate: IsoDate,
        rules: SharedScheduleRules,
    ): List<SharedPlannedTask> {
        require(targetDate >= startDate) { "Completion date cannot precede start date" }
        if (units.isEmpty()) return emptyList()
        val dates = eligibleDates(startDate, targetDate, rules)
        require(dates.isNotEmpty()) { "The date range contains no selected learning days" }
        if (units.size < dates.size) {
            val assignedDates = if (units.size == 1) listOf(dates.last()) else units.indices.map { index ->
                dates[index * dates.lastIndex / units.lastIndex]
            }
            return units.zip(assignedDates) { unit, date -> learningTask(unit, date) }
        }
        val base = units.size / dates.size
        val largerDayCount = units.size % dates.size
        var unitIndex = 0
        return buildList {
            dates.forEachIndexed { dayIndex, date ->
                repeat(base + if (dayIndex < largerDayCount) 1 else 0) {
                    add(learningTask(units[unitIndex++], date))
                }
            }
        }
    }

    fun generateChazarah(
        learningTasks: List<SharedPlannedTask>,
        dayOffsets: List<Int>,
        repeatsAnnually: Boolean,
        rules: SharedScheduleRules,
        annualReviewsThroughYear: Int,
    ): List<SharedPlannedTask> = buildList {
        if (learningTasks.isEmpty()) return@buildList
        require(learningTasks.all { it.type == LearningTaskType.LEARNING })
        require(dayOffsets.all { it > 0 } && dayOffsets.distinct().size == dayOffsets.size)

        val learningByDate = learningTasks.groupBy { it.originalLearningDate }.entries.sortedBy { it.key }
        val firstLearningDate = learningByDate.first().key
        val timeline = EligibleDateTimeline(firstLearningDate, rules, ::nextEligibleDate)
        val learningSlots: List<Pair<Int, List<SharedPlannedTask>>> = learningByDate.map { entry ->
            timeline.indexOf(entry.key) to entry.value
        }
        var previousSlotLag = 0
        dayOffsets.sorted().forEach { offset ->
            val slotLag = previousSlotLag + offset
            learningSlots.forEach { (learningSlot, learningSet) ->
                val reviewDate = timeline.dateAt(learningSlot + slotLag)
                learningSet.forEach { learning -> add(reviewTask(learning, reviewDate, "day:$offset")) }
            }
            previousSlotLag = slotLag
        }

        if (repeatsAnnually) {
            for (annualNumber in 1..(annualReviewsThroughYear - firstLearningDate.year).coerceAtLeast(0)) {
                val firstTargetDate = sameGregorianDate(firstLearningDate, firstLearningDate.year + annualNumber)
                val calculatedLag = timeline.indexOf(nextEligibleDate(firstTargetDate, rules))
                val slotLag = calculatedLag.coerceAtLeast(previousSlotLag + 1)
                learningSlots.forEach { (learningSlot, learningSet) ->
                    val reviewDate = timeline.dateAt(learningSlot + slotLag)
                    learningSet.forEach { learning ->
                        val annualYear = learning.originalLearningDate.year + annualNumber
                        if (annualYear <= annualReviewsThroughYear) add(reviewTask(learning, reviewDate, "annual:$annualYear"))
                    }
                }
                previousSlotLag = slotLag
            }
        }
    }

    private fun eligibleDates(start: IsoDate, target: IsoDate, rules: SharedScheduleRules): List<IsoDate> = buildList {
        var date = start
        while (date <= target) {
            if (rules.isEligible(date)) add(date)
            date = date.plusDays(1)
        }
    }

    private fun learningTask(unit: SharedMaterialUnit, date: IsoDate) = SharedPlannedTask(
        stableKey = "learning:${unit.id}:$date",
        material = unit,
        type = LearningTaskType.LEARNING,
        plannedDate = date,
        originalLearningDate = date,
    )

    fun generateOfficialOraysaChazarah(
        learningTasks: List<SharedPlannedTask>,
        learningRules: SharedScheduleRules,
    ): List<SharedPlannedTask> {
        if (learningTasks.isEmpty()) return emptyList()
        val daily = generateChazarah(
            learningTasks = learningTasks,
            dayOffsets = listOf(1),
            repeatsAnnually = false,
            rules = learningRules,
            annualReviewsThroughYear = learningTasks.first().plannedDate.year,
        ).map { it.copy(reviewIdentity = "oraysa:daily") }
        return daily + generateWeekendChazarah(learningTasks)
    }

    /** Splits Sunday–Monday learning onto Friday and Tuesday–Thursday learning onto Shabbos. */
    fun generateWeekendChazarah(learningTasks: List<SharedPlannedTask>): List<SharedPlannedTask> = buildList {
        if (learningTasks.isEmpty()) return@buildList
        require(learningTasks.all { it.type == LearningTaskType.LEARNING })
        learningTasks.groupBy { task ->
            val weekday = GregorianCalendar.dayOfWeek(task.plannedDate.year, task.plannedDate.month, task.plannedDate.day)
            task.plannedDate.minusDays(weekday)
        }.entries.sortedBy { it.key }.forEach { (sunday, week) ->
            val byDay = week.groupBy { it.plannedDate }
            val fridayMaterial = listOf(0, 1).flatMap { day -> byDay[sunday.plusDays(day)].orEmpty() }
            val shabbosMaterial = listOf(2, 3, 4).flatMap { day -> byDay[sunday.plusDays(day)].orEmpty() }
            if (fridayMaterial.isNotEmpty()) add(weekendTask(fridayMaterial, sunday.plusDays(5), "friday"))
            if (shabbosMaterial.isNotEmpty()) add(weekendTask(shabbosMaterial, sunday.plusDays(6), "shabbos"))
        }
    }

    private fun reviewTask(learning: SharedPlannedTask, date: IsoDate, identity: String) = SharedPlannedTask(
        stableKey = "review:${learning.stableKey}:$identity",
        material = learning.material,
        type = LearningTaskType.CHAZARAH,
        plannedDate = date,
        originalLearningDate = learning.originalLearningDate,
        reviewIdentity = identity,
    )

    private fun weekendTask(
        learning: List<SharedPlannedTask>,
        date: IsoDate,
        dayIdentity: String,
    ): SharedPlannedTask {
        val first = learning.first().material
        val last = learning.last().material
        fun range(start: String, end: String): String = if (start == end) start else "$start – $end"
        val sunday = date.minusDays(if (dayIdentity == "friday") 5 else 6)
        val material = SharedMaterialUnit(
            id = "weekend:$sunday:$dayIdentity",
            ordinal = first.ordinal,
            labelEnglish = range(first.labelEnglish, last.labelEnglish),
            labelHebrew = range(first.labelHebrew, last.labelHebrew),
            quantity = learning.sumOf { it.material.quantity },
        )
        return SharedPlannedTask(
            stableKey = "review:${material.id}",
            material = material,
            type = LearningTaskType.CHAZARAH,
            plannedDate = date,
            originalLearningDate = learning.first().originalLearningDate,
            reviewIdentity = "weekend:weekly:$dayIdentity",
        )
    }

    private fun sameGregorianDate(source: IsoDate, year: Int): IsoDate {
        val day = source.day.coerceAtMost(GregorianCalendar.daysInMonth(year, source.month))
        return IsoDate(year, source.month, day)
    }

    private class EligibleDateTimeline(
        firstDate: IsoDate,
        private val rules: SharedScheduleRules,
        private val nextEligibleDate: (IsoDate, SharedScheduleRules) -> IsoDate,
    ) {
        private val dates = mutableListOf(firstDate)
        private val indices = mutableMapOf(firstDate to 0)

        fun indexOf(date: IsoDate): Int {
            require(rules.isEligible(date))
            while (dates.last() < date) appendDate()
            return requireNotNull(indices[date])
        }

        fun dateAt(index: Int): IsoDate {
            require(index >= 0)
            while (dates.lastIndex < index) appendDate()
            return dates[index]
        }

        private fun appendDate() {
            val next = nextEligibleDate(dates.last().plusDays(1), rules)
            dates += next
            indices[next] = dates.lastIndex
        }
    }

    private companion object {
        const val MAX_SEARCH_DAYS = 366 * 10
    }
}
