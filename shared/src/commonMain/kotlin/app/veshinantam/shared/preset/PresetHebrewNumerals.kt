package app.veshinantam.shared.preset

internal object PresetHebrewNumerals {
    fun format(value: Int): String {
        require(value in 1..999)
        var remaining = value
        val result = StringBuilder()
        while (remaining >= 400) {
            result.append('ת')
            remaining -= 400
        }
        listOf(300 to 'ש', 200 to 'ר', 100 to 'ק').firstOrNull { remaining >= it.first }?.let { (amount, letter) ->
            result.append(letter)
            remaining -= amount
        }
        if (remaining == 15) return result.append("טו").toString()
        if (remaining == 16) return result.append("טז").toString()
        listOf(90 to 'צ', 80 to 'פ', 70 to 'ע', 60 to 'ס', 50 to 'נ', 40 to 'מ', 30 to 'ל', 20 to 'כ', 10 to 'י')
            .firstOrNull { remaining >= it.first }?.let { (amount, letter) ->
                result.append(letter)
                remaining -= amount
            }
        if (remaining > 0) result.append("אבגדהוזחט"[remaining - 1])
        return result.toString()
    }
}
