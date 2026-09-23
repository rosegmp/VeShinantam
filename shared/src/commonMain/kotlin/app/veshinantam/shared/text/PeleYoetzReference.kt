package app.veshinantam.shared.text

data class PeleYoetzReference(val volume: Int, val day: Int) {
    companion object {
        private val pattern = Regex(
            """^Pele Yoetz, Volume ([12]), Day (\d+), Ot """,
            RegexOption.IGNORE_CASE,
        )

        fun parse(value: String): PeleYoetzReference? {
            val match = pattern.find(value.trim()) ?: return null
            val volume = match.groupValues[1].toInt()
            val parsedDay = match.groupValues[2].toInt()
            val day = if (volume == 2 && parsedDay in 1..245) parsedDay + 254 else parsedDay
            return PeleYoetzReference(
                volume = volume,
                day = day,
            ).takeIf { (it.volume == 1 && it.day in 1..254) || (it.volume == 2 && it.day in 255..499) }
        }
    }
}
