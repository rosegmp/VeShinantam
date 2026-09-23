package app.veshinantam.localization

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import app.veshinantam.shared.text.HebrewReferenceFormatter
import java.util.Locale

enum class AppLanguage(val languageTag: String) {
    ENGLISH("en"),
    HEBREW("he");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            if (tag?.lowercase(Locale.ROOT)?.startsWith("he") == true) HEBREW else ENGLISH
    }
}

enum class SefarimLanguage { ENGLISH, HEBREW, BOTH }
enum class PrimaryCalendar { GREGORIAN, HEBREW }

class LanguageSettings(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): AppLanguage {
        val stored = preferences.getString(KEY_LANGUAGE, null)
        val fallback = context.resources.configuration.locales[0]?.language
        return AppLanguage.fromTag(stored ?: fallback)
    }

    fun save(language: AppLanguage) {
        preferences.edit().putString(KEY_LANGUAGE, language.languageTag).apply()
    }

    fun readSefarimLanguage(): SefarimLanguage = runCatching {
        SefarimLanguage.valueOf(preferences.getString(KEY_SEFARIM_LANGUAGE, null) ?: SefarimLanguage.BOTH.name)
    }.getOrDefault(SefarimLanguage.BOTH)

    fun saveSefarimLanguage(language: SefarimLanguage) {
        preferences.edit().putString(KEY_SEFARIM_LANGUAGE, language.name).apply()
    }

    fun readPrimaryCalendar(): PrimaryCalendar = runCatching {
        PrimaryCalendar.valueOf(preferences.getString(KEY_PRIMARY_CALENDAR, null) ?: PrimaryCalendar.GREGORIAN.name)
    }.getOrDefault(PrimaryCalendar.GREGORIAN)

    fun savePrimaryCalendar(calendar: PrimaryCalendar) {
        preferences.edit().putString(KEY_PRIMARY_CALENDAR, calendar.name).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "display_settings"
        const val KEY_LANGUAGE = "language"
        const val KEY_SEFARIM_LANGUAGE = "sefarim_language"
        const val KEY_PRIMARY_CALENDAR = "primary_calendar"
    }
}

object SefarimDisplay {
    fun label(
        english: String,
        hebrew: String,
        preference: SefarimLanguage,
        uiLanguage: AppLanguage,
    ): String {
        val normalizedHebrew = HebrewReferenceFormatter.normalize(english, hebrew)
        val displayedEnglish = BidiText.isolate(english, AppLanguage.ENGLISH, uiLanguage)
        val displayedHebrew = BidiText.isolate(normalizedHebrew, AppLanguage.HEBREW, uiLanguage)
        return when {
            english.isBlank() -> displayedHebrew
            normalizedHebrew.isBlank() -> displayedEnglish
            preference == SefarimLanguage.ENGLISH -> displayedEnglish
            preference == SefarimLanguage.HEBREW -> displayedHebrew
            uiLanguage == AppLanguage.HEBREW -> "$displayedHebrew / $displayedEnglish"
            else -> "$displayedEnglish / $displayedHebrew"
        }
    }
}

object BidiText {
    private const val LEFT_TO_RIGHT_ISOLATE = '\u2066'
    private const val RIGHT_TO_LEFT_ISOLATE = '\u2067'
    private const val POP_DIRECTIONAL_ISOLATE = '\u2069'

    fun isolate(value: String, contentLanguage: AppLanguage, uiLanguage: AppLanguage): String {
        if (value.isBlank() || contentLanguage == uiLanguage) return value
        val isolate = if (contentLanguage == AppLanguage.HEBREW) RIGHT_TO_LEFT_ISOLATE else LEFT_TO_RIGHT_ISOLATE
        return "$isolate$value$POP_DIRECTIONAL_ISOLATE"
    }

    fun isolateForUi(value: String, uiLanguage: AppLanguage): String {
        val contentLanguage = value.firstNotNullOfOrNull { character ->
            when (Character.getDirectionality(character)) {
                Character.DIRECTIONALITY_LEFT_TO_RIGHT -> AppLanguage.ENGLISH
                Character.DIRECTIONALITY_RIGHT_TO_LEFT,
                Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC,
                -> AppLanguage.HEBREW
                else -> null
            }
        } ?: return value
        return isolate(value, contentLanguage, uiLanguage)
    }
}

object AppLocale {
    fun wrap(context: Context): Context = wrap(context, LanguageSettings(context).read())

    fun wrap(context: Context, language: AppLanguage): Context {
        val locale = Locale.forLanguageTag(language.languageTag)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(configuration)
    }

    fun syncPlatform(context: Context) {
        if (Build.VERSION.SDK_INT < 33) return
        val selected = LanguageSettings(context).read().languageTag
        val manager = context.getSystemService(LocaleManager::class.java)
        if (manager.applicationLocales.toLanguageTags() != selected) {
            manager.applicationLocales = LocaleList.forLanguageTags(selected)
        }
    }

    fun apply(context: Context, language: AppLanguage) {
        if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(language.languageTag)
        } else {
            context.findActivity()?.recreate()
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
