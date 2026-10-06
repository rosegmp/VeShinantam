package app.veshinantam.ui.today

import android.content.Context

enum class TodaySortOrder {
    SCHEDULED_FIRST,
    NEWEST_DUE_FIRST,
    REFERENCE_ASCENDING,
    REFERENCE_DESCENDING,
}

enum class TodayGroupBy { SCHEDULE, LEARNING_STATUS }

class TodayDisplaySettings(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun readSortOrder(): TodaySortOrder = runCatching {
        TodaySortOrder.valueOf(preferences.getString(KEY_SORT_ORDER, null).orEmpty())
    }.getOrDefault(TodaySortOrder.SCHEDULED_FIRST)

    fun saveSortOrder(sortOrder: TodaySortOrder) {
        preferences.edit().putString(KEY_SORT_ORDER, sortOrder.name).apply()
    }

    fun readGroupBy(): TodayGroupBy = runCatching {
        TodayGroupBy.valueOf(preferences.getString(KEY_GROUP_BY, null).orEmpty())
    }.getOrDefault(TodayGroupBy.SCHEDULE)

    fun saveGroupBy(groupBy: TodayGroupBy) {
        preferences.edit().putString(KEY_GROUP_BY, groupBy.name).apply()
    }

    fun readAutoCollapseCompleted(): Boolean = preferences.getBoolean(KEY_AUTO_COLLAPSE_COMPLETED, false)

    fun saveAutoCollapseCompleted(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_COLLAPSE_COMPLETED, enabled).apply()
    }

    fun readExpandedSections(): Map<String, Boolean> = readExpandedStates(KEY_SECTION_PREFIX)

    fun saveSectionExpanded(key: String, expanded: Boolean) = saveExpandedState(KEY_SECTION_PREFIX, key, expanded)

    fun readExpandedSchedules(): Map<String, Boolean> = readExpandedStates(KEY_SCHEDULE_PREFIX)

    fun saveScheduleExpanded(key: String, expanded: Boolean) = saveExpandedState(KEY_SCHEDULE_PREFIX, key, expanded)

    private fun readExpandedStates(prefix: String): Map<String, Boolean> = preferences.all.mapNotNull { (key, value) ->
        if (key.startsWith(prefix) && value is Boolean) key.removePrefix(prefix) to value else null
    }.toMap()

    private fun saveExpandedState(prefix: String, key: String, expanded: Boolean) {
        preferences.edit().putBoolean(prefix + key, expanded).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "today_display"
        const val KEY_SORT_ORDER = "sort_order"
        const val KEY_GROUP_BY = "group_by"
        const val KEY_AUTO_COLLAPSE_COMPLETED = "auto_collapse_completed"
        const val KEY_SECTION_PREFIX = "expanded_section:"
        const val KEY_SCHEDULE_PREFIX = "expanded_schedule:"
    }
}
