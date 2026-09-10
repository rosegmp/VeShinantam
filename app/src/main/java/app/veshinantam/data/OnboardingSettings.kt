package app.veshinantam.data

import android.content.Context

class OnboardingSettings(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isComplete(): Boolean = preferences.getBoolean(KEY_COMPLETE, false)

    fun markComplete() {
        preferences.edit().putBoolean(KEY_COMPLETE, true).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "onboarding"
        const val KEY_COMPLETE = "complete"
    }
}
