package app.veshinantam.shared.preset

import app.veshinantam.shared.text.HebrewNumerals

internal object PresetHebrewNumerals {
    fun format(value: Int): String = HebrewNumerals.format(value)
}
