package app.veshinantam.shared.preset

enum class SeferChoice { GEMARA, YERUSHALMI, MISHNAH, MISHNAH_BERURAH, RAMBAM, CHOFETZ_CHAIM, TEHILLIM, KITZUR, OTHER }
enum class GemaraUnit { DAF, AMUD }
enum class MishnahUnit { MISHNAH, PEREK }
enum class MishnahBerurahUnit { PAGE, SEIF, SIMAN }

data class Masechta(
    val english: String,
    val hebrew: String,
    val lastLocation: Int,
    val mishnayosPerPerek: List<Int> = emptyList(),
    val lastDafHasAmudB: Boolean = true,
)
data class UnitReference(val english: String, val hebrew: String)

object MaterialCatalog {
    val sectionedChoices = setOf(SeferChoice.GEMARA, SeferChoice.YERUSHALMI, SeferChoice.MISHNAH, SeferChoice.RAMBAM)

    private val finalDafEndsOnAmudA = setOf(
        "Berachos", "Eruvin", "Yoma", "Rosh Hashanah", "Taanis", "Megillah", "Moed Katan",
        "Chagigah", "Bava Metzia", "Horayos", "Menachos", "Chullin", "Bechoros", "Arachin",
        "Temurah", "Meilah", "Niddah",
    )

    val gemara = parse("""
        Berachos|ברכות|64;Shabbos|שבת|157;Eruvin|עירובין|105;Pesachim|פסחים|121;Shekalim|שקלים|22;Yoma|יומא|88;Sukkah|סוכה|56;Beitzah|ביצה|40;Rosh Hashanah|ראש השנה|35;Taanis|תענית|31;Megillah|מגילה|32;Moed Katan|מועד קטן|29;Chagigah|חגיגה|27;Yevamos|יבמות|122;Kesubos|כתובות|112;Nedarim|נדרים|91;Nazir|נזיר|66;Sotah|סוטה|49;Gittin|גיטין|90;Kiddushin|קידושין|82;Bava Kamma|בבא קמא|119;Bava Metzia|בבא מציעא|119;Bava Basra|בבא בתרא|176;Sanhedrin|סנהדרין|113;Makkos|מכות|24;Shevuos|שבועות|49;Avodah Zarah|עבודה זרה|76;Horayos|הוריות|14;Zevachim|זבחים|120;Menachos|מנחות|110;Chullin|חולין|142;Bechoros|בכורות|61;Arachin|ערכין|34;Temurah|תמורה|34;Kerisus|כריתות|28;Meilah|מעילה|22;Kinnim|קינים|4;Tamid|תמיד|33;Middos|מידות|4;Niddah|נדה|73
    """).map { it.copy(lastDafHasAmudB = it.english !in finalDafEndsOnAmudA) }

    val mishnah = parse("""
        Berachos|ברכות|9;Peah|פאה|8;Demai|דמאי|7;Kilayim|כלאים|9;Sheviis|שביעית|10;Terumos|תרומות|11;Maasros|מעשרות|5;Maaser Sheni|מעשר שני|5;Challah|חלה|4;Orlah|ערלה|3;Bikkurim|ביכורים|4;Shabbos|שבת|24;Eruvin|עירובין|10;Pesachim|פסחים|10;Shekalim|שקלים|8;Yoma|יומא|8;Sukkah|סוכה|5;Beitzah|ביצה|5;Rosh Hashanah|ראש השנה|4;Taanis|תענית|4;Megillah|מגילה|4;Moed Katan|מועד קטן|3;Chagigah|חגיגה|3;Yevamos|יבמות|16;Kesubos|כתובות|13;Nedarim|נדרים|11;Nazir|נזיר|9;Sotah|סוטה|9;Gittin|גיטין|9;Kiddushin|קידושין|4;Bava Kamma|בבא קמא|10;Bava Metzia|בבא מציעא|10;Bava Basra|בבא בתרא|10;Sanhedrin|סנהדרין|11;Makkos|מכות|3;Shevuos|שבועות|8;Eduyos|עדויות|8;Avodah Zarah|עבודה זרה|5;Avos|אבות|6;Horayos|הוריות|3;Zevachim|זבחים|14;Menachos|מנחות|13;Chullin|חולין|12;Bechoros|בכורות|9;Arachin|ערכין|9;Temurah|תמורה|7;Kerisus|כריתות|6;Meilah|מעילה|6;Tamid|תמיד|7;Middos|מידות|5;Kinnim|קינים|3;Kelim|כלים|30;Ohalos|אהלות|18;Negaim|נגעים|14;Parah|פרה|12;Taharos|טהרות|10;Mikvaos|מקואות|10;Niddah|נדה|10;Machshirin|מכשירין|6;Zavim|זבים|5;Tevul Yom|טבול יום|4;Yadayim|ידיים|4;Uktzin|עוקצין|3
    """).mapIndexed { index, masechta ->
        masechta.copy(mishnayosPerPerek = MaterialStructureData.mishnayosPerPerek[index])
    }

    val yerushalmi = parse("""
        Berachos|ברכות|68;Peah|פאה|37;Demai|דמאי|34;Kilayim|כלאים|44;Sheviis|שביעית|31;Terumos|תרומות|59;Maasros|מעשרות|26;Maaser Sheni|מעשר שני|33;Challah|חלה|28;Orlah|ערלה|20;Bikkurim|ביכורים|13;Shabbos|שבת|92;Eruvin|עירובין|65;Pesachim|פסחים|71;Beitzah|ביצה|22;Rosh Hashanah|ראש השנה|22;Yoma|יומא|42;Sukkah|סוכה|26;Taanis|תענית|26;Shekalim|שקלים|33;Megillah|מגילה|34;Chagigah|חגיגה|22;Moed Katan|מועד קטן|19;Yevamos|יבמות|85;Kesubos|כתובות|72;Sotah|סוטה|47;Nedarim|נדרים|40;Nazir|נזיר|47;Gittin|גיטין|54;Kiddushin|קידושין|48;Bava Kamma|בבא קמא|44;Bava Metzia|בבא מציעא|37;Bava Basra|בבא בתרא|34;Shevuos|שבועות|44;Makkos|מכות|9;Sanhedrin|סנהדרין|57;Avodah Zarah|עבודה זרה|37;Horayos|הוריות|19;Niddah|נדה|13
    """)

    val yerushalmiSchottenstein = parse("""
        Berachos|ברכות|94;Peah|פאה|73;Demai|דמאי|77;Kilayim|כלאים|84;Sheviis|שביעית|87;Terumos|תרומות|107;Maasros|מעשרות|46;Maaser Sheni|מעשר שני|59;Challah|חלה|49;Orlah|ערלה|42;Bikkurim|ביכורים|26;Shabbos|שבת|113;Eruvin|עירובין|71;Pesachim|פסחים|86;Shekalim|שקלים|61;Yoma|יומא|57;Sukkah|סוכה|33;Beitzah|ביצה|49;Rosh Hashanah|ראש השנה|27;Taanis|תענית|31;Megillah|מגילה|41;Chagigah|חגיגה|28;Moed Katan|מועד קטן|23;Yevamos|יבמות|88;Kesubos|כתובות|77;Nedarim|נדרים|42;Nazir|נזיר|53;Sotah|סוטה|52;Gittin|גיטין|53;Kiddushin|קידושין|53;Bava Kamma|בבא קמא|40;Bava Metzia|בבא מציעא|35;Bava Basra|בבא בתרא|39;Sanhedrin|סנהדרין|75;Shevuos|שבועות|49;Avodah Zarah|עבודה זרה|34;Makkos|מכות|11;Horayos|הוריות|18;Niddah|נדה|11
    """)

    val rambam = parse("""
        Transmission of the Oral Law|מסירת תורה שבעל פה|3;Positive Mitzvot|מצוות עשה|3;Negative Mitzvot|מצוות לא תעשה|3;Overview of Mishneh Torah Contents|תוכן החיבור|3;Foundations of the Torah|הלכות יסודי התורה|10;Human Dispositions|הלכות דעות|7;Torah Study|הלכות תלמוד תורה|7;Foreign Worship and Customs of the Nations|הלכות עבודה זרה וחוקות הגויים|12;Repentance|הלכות תשובה|10;Reading the Shema|הלכות קריאת שמע|4;Prayer and the Priestly Blessing|הלכות תפילה וברכת כהנים|15;Tefillin, Mezuzah and the Torah Scroll|הלכות תפילין ומזוזה וספר תורה|10;Fringes|הלכות ציצית|3;Blessings|הלכות ברכות|11;Circumcision|הלכות מילה|3;The Order of Prayer|סדר התפילה|4;Sabbath|הלכות שבת|30;Eruvin|עירובין|8;Rest on the Tenth of Tishrei|הלכות שביתת עשור|3;Rest on a Holiday|הלכות שביתת יום טוב|8;Leavened and Unleavened Bread|הלכות חמץ ומצה|9;Shofar, Sukkah and Lulav|הלכות שופר וסוכה ולולב|8;Sheqel Dues|הלכות שקלים|4;Sanctification of the New Month|הלכות קידוש החודש|19;Fasts|הלכות תעניות|5;Scroll of Esther and Hanukkah|הלכות מגילה וחנוכה|4;Marriage|הלכות אישות|25;Divorce|הלכות גירושין|13;Levirate Marriage and Release|הלכות יבום וחליצה|8;Virgin Maiden|הלכות נערה בתולה|3;Woman Suspected of Infidelity|הלכות סוטה|4;Forbidden Intercourse|הלכות איסורי ביאה|22;Forbidden Foods|הלכות מאכלות אסורות|17;Ritual Slaughter|הלכות שחיטה|14;Oaths|הלכות שבועות|12;Vows|הלכות נדרים|13;Nazariteship|הלכות נזירות|10;Appraisals and Devoted Property|הלכות ערכים וחרמין|8;Diverse Species|הלכות כלאים|10;Gifts to the Poor|הלכות מתנות עניים|10;Heave Offerings|הלכות תרומות|15;Tithes|הלכות מעשרות|14;Second Tithes and Fourth Year's Fruit|הלכות מעשר שני ונטע רבעי|11;First Fruits and other Gifts to Priests Outside the Sanctuary|הלכות ביכורים ושאר מתנות כהונה שבגבולין|12;Sabbatical Year and the Jubilee|הלכות שמיטה ויובל|13;The Chosen Temple|הלכות בית הבחירה|8;Vessels of the Sanctuary and Those who Serve Therein|הלכות כלי המקדש והעובדין בו|10;Admission into the Sanctuary|הלכות ביאת מקדש|9;Things Forbidden on the Altar|הלכות איסורי המזבח|7;Sacrificial Procedure|הלכות מעשה הקרבנות|19;Daily Offerings and Additional Offerings|הלכות תמידים ומוספין|10;Sacrifices Rendered Unfit|הלכות פסולי המוקדשין|19;Service on the Day of Atonement|הלכות עבודת יום הכפורים|5;Trespass|הלכות מעילה|8;Paschal Offering|הלכות קרבן פסח|10;Festival Offering|הלכות חגיגה|3;Firstlings|הלכות בכורות|8;Offerings for Unintentional Transgressions|הלכות שגגות|15;Offerings for Those with Incomplete Atonement|הלכות מחוסרי כפרה|5;Substitution|הלכות תמורה|4;Defilement by a Corpse|הלכות טומאת מת|25;Red Heifer|הלכות פרה אדומה|15;Defilement by Leprosy|הלכות טומאת צרעת|16;Those Who Defile Bed or Seat|הלכות מטמאי משכב ומושב|13;Other Sources of Defilement|הלכות שאר אבות הטומאות|20;Defilement of Foods|הלכות טומאת אוכלים|16;Vessels|הלכות כלים|28;Immersion Pools|הלכות מקואות|11;Damages to Property|הלכות נזקי ממון|14;Theft|הלכות גניבה|9;Robbery and Lost Property|הלכות גזילה ואבידה|18;One Who Injures a Person or Property|הלכות חובל ומזיק|8;Murderer and the Preservation of Life|הלכות רוצח ושמירת נפש|13;Sales|הלכות מכירה|30;Ownerless Property and Gifts|הלכות זכייה ומתנה|12;Neighbors|הלכות שכנים|14;Agents and Partners|הלכות שלוחין ושותפין|10;Slaves|הלכות עבדים|9;Hiring|הלכות שכירות|13;Borrowing and Deposit|הלכות שאלה ופיקדון|8;Creditor and Debtor|הלכות מלווה ולווה|27;Plaintiff and Defendant|הלכות טוען ונטען|16;Inheritances|הלכות נחלות|11;The Sanhedrin and the Penalties within their Jurisdiction|הלכות סנהדרין והעונשין המסורין להם|26;Testimony|הלכות עדות|22;Rebels|הלכות ממרים|7;Mourning|הלכות אבל|14;Kings and Wars|הלכות מלכים ומלחמות|12
    """)

    val mishnahBerurahChelakim = listOf(1..127, 128..241, 242..344, 345..428, 429..529, 530..697)

    fun yerushalmiUnits(masechta: Masechta): List<UnitReference> = (1..masechta.lastLocation).map { daf ->
        UnitReference("Yerushalmi ${masechta.english} $daf", "ירושלמי ${masechta.hebrew} דף ${PresetHebrewNumerals.format(daf)}.")
    }

    fun rambamUnits(section: Masechta): List<UnitReference> = (1..section.lastLocation).map { chapter ->
        UnitReference("Rambam, ${section.english} $chapter", "רמב״ם, ${section.hebrew} פרק ${PresetHebrewNumerals.format(chapter)}")
    }

    val tehillimUnits: List<UnitReference> = (1..150).map { chapter ->
        UnitReference("Tehillim $chapter", "תהילים פרק ${PresetHebrewNumerals.format(chapter)}")
    }

    val monthlyTehillimUnits: List<UnitReference> = listOf(
        "1-9", "10-17", "18-22", "23-28", "29-34", "35-38", "39-43", "44-48", "49-54", "55-59",
        "60-65", "66-68", "69-71", "72-76", "77-78", "79-82", "83-87", "88-89", "90-96", "97-103",
        "104-105", "106-107", "108-112", "113-118", "119:1-96", "119:97-176", "120-134", "135-139", "140-150",
    ).map { range -> UnitReference("Tehillim $range", "תהילים ${hebrewLocation(range)}") }

    val kitzurYomiUnits: List<UnitReference> get() = KitzurYomiData.units

    val chofetzChaimUnits: List<UnitReference> by lazy {
        chofetzChaimPortions.split(';').map { raw ->
            val (key, from, to) = raw.split('|')
            val (english, hebrew) = chofetzSectionNames.getValue(key)
            val rangeEnglish = when { from.isBlank() -> ""; from == to -> " $from"; else -> " $from-$to" }
            val rangeHebrew = when {
                from.isBlank() -> ""
                from == to -> " ${hebrewLocation(from)}"
                else -> " ${hebrewLocation(from)}–${hebrewLocation(to)}"
            }
            UnitReference("Chofetz Chaim, $english$rangeEnglish", "חפץ חיים, $hebrew$rangeHebrew")
        }
    }

    private val chofetzSectionNames = mapOf(
        "Hakdamah" to ("Preface" to "הקדמה"),
        "Psichah" to ("Introduction, Opening Comments" to "פתיחה"),
        "Lavin" to ("Introduction, Negative Commandments" to "לאוין"),
        "Asin" to ("Introduction, Positive Commandments" to "עשין"),
        "Arurin" to ("Introduction, Curses" to "ארורין"),
        "HilchosLH" to ("Hilchos Lashon Hara" to "הלכות לשון הרע"),
        "HilchosRechilus" to ("Hilchos Rechilus" to "הלכות רכילות"),
        "Tziyurim" to ("Tziyurim" to "ציורים"),
    )

    private const val chofetzChaimPortions = "Hakdamah|1|4;Hakdamah|5|10;Hakdamah|11|16;Hakdamah|17|22;Hakdamah|23|27;Hakdamah|28|33-34;Hakdamah|29|32;Psichah|1|4;Psichah|5|11;Lavin|1|2;Lavin|3|4;Lavin|5|6;Lavin|7|9;Lavin|10|11;Lavin|12|13;Lavin|14|15;Lavin|16|17;Asin|1|2;Asin|3|4;Asin|5|6;Asin|7|8;Asin|9|10;Asin|11|12;Asin|13|14;Arurin||;HilchosLH|1.1|1.2;HilchosLH|1.3|1.4;HilchosLH|1.5|1.6;HilchosLH|1.7|1.9;HilchosLH|2.1|2.2;HilchosLH|2.3|2.4;HilchosLH|2.5|2.6;HilchosLH|2.7|2.8;HilchosLH|2.9|2.10;HilchosLH|2.11|2.11;HilchosLH|2.12|2.13;HilchosLH|3.1|3.2;HilchosLH|3.3|3.4;HilchosLH|3.5|3.6;HilchosLH|3.7|3.8;HilchosLH|4.1|4.2;HilchosLH|4.3|4.4;HilchosLH|4.5|4.6;HilchosLH|4.7|4.8;HilchosLH|4.9|4.10;HilchosLH|4.11|4.11;HilchosLH|4.12|5.1;HilchosLH|5.2|5.4;HilchosLH|5.5|5.6;HilchosLH|5.7|5.8;HilchosLH|6.1|6.2;HilchosLH|6.3|6.4;HilchosLH|6.5|6.6;HilchosLH|6.7|6.8;HilchosLH|6.9|6.10;HilchosLH|6.11|6.12;HilchosLH|7.1|7.2;HilchosLH|7.3|7.4;HilchosLH|7.5|7.6;HilchosLH|7.7|7.8;HilchosLH|7.9|7.9;HilchosLH|7.10|7.12;HilchosLH|7.13|7.14;HilchosLH|8.1|8.2;HilchosLH|8.3|8.4;HilchosLH|8.5|8.7;HilchosLH|8.8|8.9;HilchosLH|8.10|8.11;HilchosLH|8.12|8.12;HilchosLH|8.13|8.14;HilchosLH|9.1|9.2;HilchosLH|9.3|9.4;HilchosLH|9.5|9.6;HilchosLH|10.1|10.2;HilchosLH|10.3|10.4;HilchosLH|10.5|10.6;HilchosLH|10.7|10.8;HilchosLH|10.9|10.10;HilchosLH|10.11|10.12;HilchosLH|10.13|10.14;HilchosLH|10.15|10.16;HilchosLH|10.17|10.17;HilchosRechilus|1.1|1.3;HilchosRechilus|1.4|1.5;HilchosRechilus|1.6|1.7;HilchosRechilus|1.8|1.9;HilchosRechilus|1.10|1.11;HilchosRechilus|2.1|2.2;HilchosRechilus|2.3|2.4;HilchosRechilus|3.1|3.1;HilchosRechilus|3.2|3.4;HilchosRechilus|4.1|4.3;HilchosRechilus|5.1|5.2;HilchosRechilus|5.3|5.4;HilchosRechilus|5.5|5.5;HilchosRechilus|5.6|5.7;HilchosRechilus|6.1|6.2;HilchosRechilus|6.3|6.4;HilchosRechilus|6.5|6.7;HilchosRechilus|6.8|6.10;HilchosRechilus|7.1|7.1;HilchosRechilus|7.2|7.2;HilchosRechilus|7.3|7.4;HilchosRechilus|7.5|7.5;HilchosRechilus|8.1|8.3;HilchosRechilus|8.4|8.5;HilchosRechilus|9.1|9.2;HilchosRechilus|9.3|9.4;HilchosRechilus|9.5|9.6;HilchosRechilus|9.7|9.9;HilchosRechilus|9.10|9.10;HilchosRechilus|9.11|9.12;HilchosRechilus|9.13|9.13;HilchosRechilus|9.14|9.15;Tziyurim|1|3;Tziyurim|4|5;Tziyurim|6|7;Tziyurim|8|9;Tziyurim|10|11"

    private fun hebrewLocation(value: String): String = Regex("\\d+").replace(value) { match ->
        PresetHebrewNumerals.format(match.value.toInt())
    }.replace('.', ':').replace("-", "–")

    fun gemaraUnits(masechtos: List<Masechta>, startDaf: Int, endDaf: Int, unit: GemaraUnit): List<UnitReference> =
        ranged(masechtos, startDaf, endDaf, minimum = 2).flatMap { (masechta, number) ->
            if (unit == GemaraUnit.DAF) listOf(UnitReference("${masechta.english} $number", "${masechta.hebrew} דף ${PresetHebrewNumerals.format(number)}."))
            else listOf("a" to ".", "b" to ":")
                .take(if (number == masechta.lastLocation && !masechta.lastDafHasAmudB) 1 else 2)
                .map { (side, punctuation) ->
                UnitReference("${masechta.english} $number$side", "${masechta.hebrew} דף ${PresetHebrewNumerals.format(number)}$punctuation")
            }
        }

    fun mishnahPerakim(masechtos: List<Masechta>, startPerek: Int, endPerek: Int): List<UnitReference> =
        ranged(masechtos, startPerek, endPerek, minimum = 1).map { (masechta, number) ->
                UnitReference("${masechta.english} perek $number", "${masechta.hebrew} פרק ${PresetHebrewNumerals.format(number)}")
        }

    fun mishnahUnits(masechta: Masechta, unit: MishnahUnit): List<UnitReference> = when (unit) {
        MishnahUnit.PEREK -> mishnahPerakim(listOf(masechta), 1, masechta.lastLocation)
        MishnahUnit.MISHNAH -> {
            require(masechta.mishnayosPerPerek.size == masechta.lastLocation)
            masechta.mishnayosPerPerek.flatMapIndexed { perekIndex, count ->
                (1..count).map { mishnahNumber ->
                    val location = "${perekIndex + 1}:$mishnahNumber"
                    val hebrewLocation = "${PresetHebrewNumerals.format(perekIndex + 1)}:${PresetHebrewNumerals.format(mishnahNumber)}"
                    UnitReference("${masechta.english} $location", "${masechta.hebrew} $hebrewLocation")
                }
            }
        }
    }

    fun simanim(titleEnglish: String, titleHebrew: String, start: Int, end: Int): List<UnitReference> {
        require(start > 0 && end >= start)
        return (start..end).map { UnitReference("$titleEnglish siman $it", "$titleHebrew סימן ${PresetHebrewNumerals.format(it)}") }
    }

    fun mishnahBerurahUnitOptions(chelek: Int, unit: MishnahBerurahUnit): List<UnitReference> {
        require(chelek in 1..mishnahBerurahChelakim.size)
        return when (unit) {
            MishnahBerurahUnit.PAGE -> MaterialStructureData.mishnahBerurahPages[chelek - 1].flatMap { page ->
                listOf("a" to ".", "b" to ":").map { (side, punctuation) ->
                    UnitReference(
                        "Mishnah Berurah, chelek $chelek page $page$side",
                        "משנה ברורה חלק ${PresetHebrewNumerals.format(chelek)} עמוד ${PresetHebrewNumerals.format(page)}$punctuation",
                    )
                }
            }
            MishnahBerurahUnit.SIMAN -> mishnahBerurahChelakim[chelek - 1].map { siman ->
                UnitReference("Mishnah Berurah, chelek $chelek siman $siman", "משנה ברורה חלק ${PresetHebrewNumerals.format(chelek)} סימן ${PresetHebrewNumerals.format(siman)}")
            }
            MishnahBerurahUnit.SEIF -> mishnahBerurahChelakim[chelek - 1].flatMap { siman ->
                val seifCount = MaterialStructureData.mishnahBerurahSeifimPerSiman[siman - 1]
                (1..seifCount).map { seif ->
                    UnitReference(
                        "Mishnah Berurah, chelek $chelek siman $siman seif $seif",
                        "משנה ברורה חלק ${PresetHebrewNumerals.format(chelek)} סימן ${PresetHebrewNumerals.format(siman)} סעיף ${PresetHebrewNumerals.format(seif)}",
                    )
                }
            }
        }
    }

    fun sections(choice: SeferChoice): List<Masechta> = when (choice) {
        SeferChoice.GEMARA -> gemara
        SeferChoice.YERUSHALMI -> yerushalmi
        SeferChoice.MISHNAH -> mishnah
        SeferChoice.RAMBAM -> rambam
        else -> emptyList()
    }

    fun unitOptions(
        choice: SeferChoice,
        section: Masechta? = null,
        gemaraUnit: GemaraUnit = GemaraUnit.DAF,
        mishnahUnit: MishnahUnit = MishnahUnit.MISHNAH,
        chelek: Int = 1,
        mishnahBerurahUnit: MishnahBerurahUnit = MishnahBerurahUnit.SIMAN,
    ): List<UnitReference> = when (choice) {
        SeferChoice.GEMARA -> requireNotNull(section).let { gemaraUnits(listOf(it), 2, it.lastLocation, gemaraUnit) }
        SeferChoice.YERUSHALMI -> yerushalmiUnits(requireNotNull(section))
        SeferChoice.MISHNAH -> mishnahUnits(requireNotNull(section), mishnahUnit)
        SeferChoice.MISHNAH_BERURAH -> mishnahBerurahUnitOptions(chelek, mishnahBerurahUnit)
        SeferChoice.RAMBAM -> rambamUnits(requireNotNull(section))
        SeferChoice.CHOFETZ_CHAIM -> chofetzChaimUnits
        SeferChoice.TEHILLIM -> tehillimUnits
        SeferChoice.KITZUR -> simanim("Kitzur Shulchan Aruch", "קיצור שולחן ערוך", 1, 221)
        SeferChoice.OTHER -> emptyList()
    }

    fun selectedUnits(
        choice: SeferChoice,
        fromSectionIndex: Int,
        toSectionIndex: Int,
        start: UnitReference,
        end: UnitReference,
        gemaraUnit: GemaraUnit = GemaraUnit.DAF,
        mishnahUnit: MishnahUnit = MishnahUnit.MISHNAH,
        chelek: Int = 1,
        mishnahBerurahUnit: MishnahBerurahUnit = MishnahBerurahUnit.SIMAN,
    ): List<UnitReference> {
        val fullRange = if (choice in sectionedChoices) {
            val catalog = sections(choice)
            require(fromSectionIndex in catalog.indices && toSectionIndex in fromSectionIndex..catalog.lastIndex)
            catalog.subList(fromSectionIndex, toSectionIndex + 1).flatMap { section ->
                unitOptions(choice, section, gemaraUnit, mishnahUnit, chelek, mishnahBerurahUnit)
            }
        } else {
            unitOptions(choice, null, gemaraUnit, mishnahUnit, chelek, mishnahBerurahUnit)
        }
        val startIndex = fullRange.indexOf(start)
        val endIndex = fullRange.indexOf(end)
        require(startIndex >= 0 && endIndex >= startIndex)
        return fullRange.subList(startIndex, endIndex + 1)
    }

    private fun ranged(selected: List<Masechta>, start: Int, end: Int, minimum: Int): List<Pair<Masechta, Int>> {
        require(selected.isNotEmpty())
        return selected.flatMapIndexed { index, masechta ->
            val from = if (index == 0) start else minimum
            val to = if (index == selected.lastIndex) end else masechta.lastLocation
            require(from in minimum..masechta.lastLocation && to in from..masechta.lastLocation)
            (from..to).map { masechta to it }
        }
    }

    private fun parse(value: String): List<Masechta> = value.trim().split(';').map { entry ->
        val parts = entry.trim().split('|')
        Masechta(parts[0], parts[1], parts[2].toInt())
    }
}
