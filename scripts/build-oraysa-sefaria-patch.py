"""Add Sefaria-compatible Oraysa amud references to the signed catalog payload."""

import json
import re
from datetime import date, timedelta
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ALIASES = {
    "Berachos": "Berakhot", "Shabbos": "Shabbat", "Taanis": "Taanit",
    "Yevamos": "Yevamot", "Kesubos": "Ketubot", "Bava Basra": "Bava Batra",
    "Makkos": "Makkot", "Shevuos": "Shevuot", "Horayos": "Horayot",
    "Menachos": "Menachot", "Bechoros": "Bekhorot", "Arachin": "Arakhin",
    "Kerisus": "Keritot", "Middos": "Middot",
}
OPENING = (
    "Berachos", "Shabbos", "Eruvin", "Pesachim", "Rosh Hashanah", "Yoma",
    "Sukkah", "Beitzah", "Megillah", "Taanis", "Moed Katan", "Chagigah",
)


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
    source = (ROOT / "shared/src/commonMain/kotlin/app/veshinantam/shared/preset/MaterialCatalog.kt").read_text(encoding="utf-8")
    catalog_match = re.search(r'val gemara = parse\("""(.*?)"""\)', source, re.S)
    final_match = re.search(r'val finalDafEndsOnAmudA = setOf\((.*?)\)', source, re.S)
    if not catalog_match or not final_match:
        raise ValueError("Could not read bundled Gemara structure")
    catalog = [entry.split("|") for entry in catalog_match.group(1).strip().split(";")]
    final_on_a = set(re.findall(r'"([^"]+)"', final_match.group(1)))
    ordered = [next(entry for entry in catalog if entry[0].strip() == name) for name in OPENING]
    ordered += [entry for entry in catalog if entry[0].strip() not in OPENING and entry[0].strip() != "Shekalim"]

    units = []
    original_to_canonical = {}
    for raw_name, raw_hebrew, raw_last in ordered:
        name, hebrew, last = raw_name.strip(), raw_hebrew.strip(), int(raw_last)
        if name == "Kinnim":
            locations = [(daf, side) for daf in range(22, 26) for side in "ab" if daf < 25 or side == "a"]
        elif name == "Tamid":
            locations = [(daf, side) for daf in range(25, 34) for side in "ab" if daf > 25 or side == "b"]
        elif name == "Middos":
            locations = [(daf, side) for daf in range(34, 38) for side in "ab"]
        else:
            locations = [(daf, side) for daf in range(2, last + 1) for side in "ab"
                         if daf < last or side == "a" or name not in final_on_a]
        for daf, side in locations:
            original = f"{name} {daf}{side}"
            english = f"{ALIASES.get(name, name)} {daf}{side}"
            original_to_canonical[original] = english
            units.append({
                "english": english,
                "hebrew": f"{hebrew} דף {hebrew_number(daf)}{'.' if side == 'a' else ':'}",
            })

    start_reference = original_to_canonical["Yevamos 105a"]
    start_index = next(index for index, unit in enumerate(units) if unit["english"] == start_reference)
    anchor_date, as_of = date(2026, 9, 10), date(2026, 9, 24)
    learning_days = sum((anchor_date + timedelta(days=day)).weekday() in (6, 0, 1, 2, 3)
                        for day in range(1, (as_of - anchor_date).days + 1))
    current_reference = units[start_index + learning_days]["english"]

    path = ROOT / "catalog/preset-catalog.payload.json"
    payload = json.loads(path.read_text(encoding="utf-8"))
    if payload["sequence"] != 13 or payload["positionAsOf"] != as_of.isoformat() or len(payload["programs"]) != 1:
        raise ValueError("Review the existing catalog before adding the Oraysa patch")
    payload["sequence"] = 14
    payload["catalogVersion"] = "2026.09.24-14"
    payload["positions"]["oraysa"] = current_reference
    payload["programs"].append({"id": "oraysa", "units": units})
    serialized = json.dumps(payload, ensure_ascii=False, separators=(",", ":")) + "\n"
    if len(serialized.encode("utf-8")) * 4 // 3 > 1024 * 1024:
        raise ValueError("Combined catalog may exceed signed envelope limit")
    path.write_text(serialized, encoding="utf-8")
    print(f"Oraysa: {len(units)} amudim, position {current_reference}, payload {len(serialized.encode('utf-8'))} bytes")


if __name__ == "__main__":
    main()
