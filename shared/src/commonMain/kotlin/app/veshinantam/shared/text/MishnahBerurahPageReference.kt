package app.veshinantam.shared.text

data class MishnahBerurahPageReference(
    val chelek: Int,
    val daf: Int,
    val side: Int,
) {
    val ordinal: Int get() = (daf - FIRST_DAFS[chelek - 1]) * 2 + side

    companion object {
        private val pattern = Regex(
            """Mishnah Berurah,?\s+chelek\s+([1-6])\s+page\s+(\d+)\s*([ab])""",
            RegexOption.IGNORE_CASE,
        )
        private val FIRST_DAFS = listOf(4, 2, 2, 196, 2, 2)

        fun parse(value: String): MishnahBerurahPageReference? {
            val match = pattern.find(value.trim()) ?: return null
            val chelek = match.groupValues[1].toInt()
            val daf = match.groupValues[2].toInt()
            val side = if (match.groupValues[3].equals("a", ignoreCase = true)) 0 else 1
            if (daf < FIRST_DAFS[chelek - 1]) return null
            return MishnahBerurahPageReference(chelek, daf, side)
        }
    }
}
