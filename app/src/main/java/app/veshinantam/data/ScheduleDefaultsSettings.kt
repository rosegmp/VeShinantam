package app.veshinantam.data

import android.content.Context
import app.veshinantam.domain.model.ChazarahDefaults

class ScheduleDefaultsSettings(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun readChazarahOffsets(): List<Int> = preferences.getString(KEY_CHAZARAH_OFFSETS, null)
        ?.let(ChazarahDefaults::parse)
        ?: ChazarahDefaults.BUILT_IN_OFFSETS

    fun saveChazarahOffsets(offsets: List<Int>) {
        require(offsets.isNotEmpty() && offsets.all { it > 0 } && offsets.distinct().size == offsets.size)
        preferences.edit()
            .putString(KEY_CHAZARAH_OFFSETS, ChazarahDefaults.format(offsets))
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "schedule_defaults"
        const val KEY_CHAZARAH_OFFSETS = "chazarah_offsets"
    }
}
