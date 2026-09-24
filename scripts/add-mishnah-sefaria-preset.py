"""Add a signed-catalog-ready Mishnah preset using Sefaria's tractate titles."""

import json
import re
from datetime import date
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PRESET_ID = "mishnah-yomis-sefaria"
ALIASES = {
    "Berachos": "Berakhot", "Sheviis": "Sheviit", "Terumos": "Terumot",
    "Maasros": "Maasrot", "Shabbos": "Shabbat", "Taanis": "Taanit",
    "Yevamos": "Yevamot", "Kesubos": "Ketubot", "Bava Basra": "Bava Batra",
    "Makkos": "Makkot", "Shevuos": "Shevuot", "Eduyos": "Eduyot",
    "Avos": "Avot", "Menachos": "Menachot", "Bechoros": "Bekhorot",
    "Arachin": "Arakhin", "Kerisus": "Keritot", "Middos": "Middot",
    "Ohalos": "Oholot", "Taharos": "Tahorot", "Mikvaos": "Mikvaot",
    "Machshirin": "Makhshirin", "Uktzin": "Oktzin",
}


def hebrew_number(number: int) -> str:
    letters = []
    for value, letter in ((400, "ת"), (300, "ש"), (200, "ר"), (100, "ק")):
        while number >= value:
            letters.append(letter)
            number -= value
    if number in (15, 16):
        return "".join(letters) + {15: "טו", 16: "טז"}[number]
    for value, letter in ((90, "צ"), (80, "פ"), (70, "ע"), (60, "ס"),
                          (50, "נ"), (40, "מ"), (30, "ל"), (20, "כ"), (10, "י")):
        if number >= value:
            letters.append(letter)
            number -= value
            break
    if number:
        letters.append("אבגדהוזחט"[number - 1])
    return "".join(letters)


def main() -> None:
    catalog_source = (ROOT / "shared/src/commonMain/kotlin/app/veshinantam/shared/preset/MaterialCatalog.kt").read_text(encoding="utf-8")
    structure_source = (ROOT / "shared/src/commonMain/kotlin/app/veshinantam/shared/preset/MaterialStructureData.kt").read_text(encoding="utf-8")
    catalog_match = re.search(r'val mishnah = parse\("""(.*?)"""\)', catalog_source, re.S)
    structure_match = re.search(r'val mishnayosPerPerek:.*?parseNested\("""(.*?)"""\)', structure_source, re.S)
    if not catalog_match or not structure_match:
        raise ValueError("Could not read bundled Mishnah structure")
    tractates = [entry.split("|") for entry in catalog_match.group(1).strip().split(";")]
    chapters = [[int(count) for count in entry.split(",")] for entry in structure_match.group(1).strip().split(";")]
    if len(tractates) != 63 or len(chapters) != len(tractates):
        raise ValueError("Unexpected Mishnah catalog size")

    units = []
    for (english, hebrew, chapter_count), mishnayos in zip(tractates, chapters):
        english, hebrew, chapter_count = english.strip(), hebrew.strip(), int(chapter_count)
        if len(mishnayos) != chapter_count:
            raise ValueError(f"Chapter mismatch for {english}")
        for chapter, count in enumerate(mishnayos, 1):
            for mishnah in range(1, count + 1):
                units.append({
                    "english": f"{ALIASES.get(english, english)} {chapter}:{mishnah}",
                    "hebrew": f"{hebrew} {hebrew_number(chapter)}:{hebrew_number(mishnah)}",
                })
    if len(units) != 4192:
        raise ValueError(f"Expected 4192 Mishnayos, found {len(units)}")

    source_position = "Kelim 30:2"
    canonical_position = source_position
    start_index = next(index for index, unit in enumerate(units) if unit["english"] == canonical_position)
    as_of = date(2026, 9, 24)
    current_index = (start_index + 2 * (as_of - date(2026, 9, 10)).days) % len(units)

    payload_path = ROOT / "catalog/preset-catalog.payload.json"
    payload = json.loads(payload_path.read_text(encoding="utf-8"))
    if payload["sequence"] != 11 or payload["programs"]:
        raise ValueError("Review the existing catalog before adding this preset")
    payload.update(catalogVersion="2026.09.24-12", sequence=12, positionAsOf=as_of.isoformat())
    payload["positions"][PRESET_ID] = units[current_index]["english"]
    payload["programs"].append({
        "id": PRESET_ID,
        "nameEnglish": "Mishnah Yomis (Sefaria)",
        "nameHebrew": "משנה יומית (ספריא)",
        "materialType": "MISHNAH",
        "dailyQuantity": 2,
        "selectedWeekdays": list(range(7)),
        "units": units,
    })
    payload_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Added {PRESET_ID}: {len(units)} units, position {units[current_index]['english']}")


if __name__ == "__main__":
    main()
