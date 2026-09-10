package app.veshinantam.ui.today

import android.content.Context

enum class TodaySortOrder {
    SCHEDULED_FIRST,
    NEWEST_DUE_FIRST,
    REFERENCE_ASCENDING,
    REFERENCE_DESCENDING,
}

class TodayDisplaySettings(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun readSortOrder(): TodaySortOrder = runCatching {
        TodaySortOrder.valueOf(preferences.getString(KEY_SORT_ORDER, null).orEmpty())
    }.getOrDefault(TodaySortOrder.SCHEDULED_FIRST)

    fun saveSortOrder(sortOrder: TodaySortOrder) {
        preferences.edit().putString(KEY_SORT_ORDER, sortOrder.name).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "today_display"
        const val KEY_SORT_ORDER = "sort_order"
    }
}
