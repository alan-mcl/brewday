#!/usr/bin/env python3
"""Apply reference DB cleanup per maintainer plan (master JSON + .prod allowlists)."""

from __future__ import annotations

import json
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
MASTER = REPO / "test_data" / "test_db"
PROD_LISTS = REPO / "src" / "dist"
SHIPPED = REPO / "data" / "db"

CIDER_YEASTS = frozenset({"Cider", "Cider Yeast", "English Cider Yeast"})
LIGHT_LAGER_PALE_MALTY = "Light Lager - Pale - Malty"


def load_json_array(path: Path) -> list[dict]:
    with path.open(encoding="utf-8") as f:
        data = json.load(f)
    if not isinstance(data, list):
        raise SystemExit(f"expected JSON array: {path}")
    return data


def save_json_array(path: Path, items: list[dict]) -> None:
    with path.open("w", encoding="utf-8", newline="\n") as f:
        json.dump(items, f, indent=2)
        f.write("\n")


def should_remove_fermentable(name: str) -> bool:
    if name.startswith("Apple Juice"):
        return True
    if name == "Honey" or name.startswith("Honey - "):
        return True
    return False


def prune_styles(items: list[dict]) -> list[dict]:
    return [
        s
        for s in items
        if s.get("name") != "_test_style" and s.get("styleGuide") != "BJCP 2015"
    ]


def prune_fermentables(items: list[dict]) -> list[dict]:
    return [f for f in items if not should_remove_fermentable(f["name"])]


def prune_yeasts(items: list[dict]) -> list[dict]:
    return [y for y in items if y["name"] not in CIDER_YEASTS]


def add_light_lager_water_param(items: list[dict]) -> list[dict]:
    names = {x["name"] for x in items}
    if LIGHT_LAGER_PALE_MALTY in names:
        return items
    ship_path = SHIPPED / "waterparameters.json"
    shipped = load_json_array(ship_path)
    record = next((x for x in shipped if x["name"] == LIGHT_LAGER_PALE_MALTY), None)
    if record is None:
        raise SystemExit(f"missing {LIGHT_LAGER_PALE_MALTY!r} in {ship_path}")
    out = list(items)
    out.append(record)
    out.sort(key=lambda x: x["name"])
    return out


def rewrite_prod_styles() -> None:
    path = PROD_LISTS / "styles.prod"
    lines = [l.strip() for l in path.read_text(encoding="utf-8").splitlines() if l.strip()]
    kept = [l for l in lines if not l.endswith("/BJCP 2015")]
    path.write_text("\n".join(kept) + "\n", encoding="utf-8")


def rewrite_prod_fermentables() -> None:
    path = PROD_LISTS / "fermentables.prod"
    lines = [l.strip() for l in path.read_text(encoding="utf-8").splitlines() if l.strip()]
    seen: set[str] = set()
    kept: list[str] = []
    for line in lines:
        if should_remove_fermentable(line):
            continue
        if line in seen:
            continue
        seen.add(line)
        kept.append(line)
    path.write_text("\n".join(kept) + "\n", encoding="utf-8")


def rewrite_prod_yeasts() -> None:
    path = PROD_LISTS / "yeast.prod"
    lines = [l.strip() for l in path.read_text(encoding="utf-8").splitlines() if l.strip()]
    kept = [l for l in lines if l not in CIDER_YEASTS]
    path.write_text("\n".join(kept) + "\n", encoding="utf-8")


def rewrite_prod_waterparameters() -> None:
    path = PROD_LISTS / "waterparameters.prod"
    lines = [l.strip() for l in path.read_text(encoding="utf-8").splitlines() if l.strip()]
    if LIGHT_LAGER_PALE_MALTY not in lines:
        lines.append(LIGHT_LAGER_PALE_MALTY)
    lines.sort()
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    styles_path = MASTER / "styles.json"
    styles = prune_styles(load_json_array(styles_path))
    save_json_array(styles_path, styles)
    print(f"styles.json: {len(styles)} entries")

    ferm_path = MASTER / "fermentables.json"
    ferm = prune_fermentables(load_json_array(ferm_path))
    save_json_array(ferm_path, ferm)
    print(f"fermentables.json: {len(ferm)} entries")

    yeast_path = MASTER / "yeasts.json"
    yeasts = prune_yeasts(load_json_array(yeast_path))
    save_json_array(yeast_path, yeasts)
    print(f"yeasts.json: {len(yeasts)} entries")

    wp_path = MASTER / "waterparameters.json"
    wp = add_light_lager_water_param(load_json_array(wp_path))
    save_json_array(wp_path, wp)
    print(f"waterparameters.json: {len(wp)} entries")

    rewrite_prod_styles()
    rewrite_prod_fermentables()
    rewrite_prod_yeasts()
    rewrite_prod_waterparameters()
    print("Updated src/dist/*.prod allowlists")
    return 0


if __name__ == "__main__":
    sys.exit(main())
