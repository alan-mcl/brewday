#!/usr/bin/env python3
"""Audit master, allowlist (.prod), and shipped (data/db) reference data."""

from __future__ import annotations

import json
import sys
from collections import Counter
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
MASTER = REPO / "test_data" / "test_db"
PROD_LISTS = REPO / "src" / "dist"
SHIPPED = REPO / "data" / "db"

SILOS = {
    "fermentables": ("fermentables.json", "fermentables.prod"),
    "hops": ("hops.json", "hops.prod"),
    "yeasts": ("yeasts.json", "yeast.prod"),
    "miscs": ("miscs.json", "miscs.prod"),
    "styles": ("styles.json", "styles.prod"),
    "waters": ("waters.json", "water.prod"),
    "waterparameters": ("waterparameters.json", "waterparameters.prod"),
}


def load_json_map(path: Path) -> dict[str, dict]:
    with path.open(encoding="utf-8") as f:
        data = json.load(f)
    if isinstance(data, list):
        return {item["name"]: item for item in data}
    return data


def load_prod(path: Path) -> list[str]:
    if not path.is_file():
        return []
    lines = [line.strip() for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]
    return lines


def variant_families(names: list[str], min_size: int = 5) -> list[tuple[str, int]]:
    groups: dict[str, list[str]] = {}
    for name in names:
        if name.startswith("Apple Juice"):
            key = "Apple Juice"
        elif name == "Honey" or name.startswith("Honey - "):
            key = "Honey"
        elif name.startswith("Fruit - "):
            key = "Fruit"
        elif name.startswith("Wine Grape Juice"):
            key = "Wine Grape Juice"
        else:
            continue
        groups.setdefault(key, []).append(name)
    return sorted([(k, len(v)) for k, v in groups.items() if len(v) >= min_size], key=lambda x: -x[1])


def main() -> int:
    print("=== Reference DB audit ===\n")
    print(f"{'Silo':<16} {'Master':>8} {'Prod':>8} {'Shipped':>8}  Notes")
    print("-" * 72)

    for silo, (json_name, prod_name) in SILOS.items():
        master_path = MASTER / json_name
        prod_path = PROD_LISTS / prod_name
        ship_path = SHIPPED / json_name

        master = load_json_map(master_path) if master_path.is_file() else {}
        prod_lines = load_prod(prod_path)
        shipped = load_json_map(ship_path) if ship_path.is_file() else {}

        prod_set = set(prod_lines)
        dup_lines = [k for k, v in Counter(prod_lines).items() if v > 1]
        notes = []
        if dup_lines:
            notes.append(f"dup lines: {', '.join(dup_lines)}")
        only_ship = set(shipped) - prod_set
        only_prod = prod_set - set(master)
        if only_ship:
            notes.append(f"shipped not in prod ({len(only_ship)})")
        if only_prod:
            notes.append(f"prod not in master ({len(only_prod)})")

        print(
            f"{silo:<16} {len(master):>8} {len(prod_set):>8} {len(shipped):>8}  "
            + ("; ".join(notes) if notes else "")
        )

    styles = load_json_map(MASTER / "styles.json")
    by_guide = Counter(s.get("styleGuide") for s in styles.values())
    by_type = Counter(s.get("type") for s in styles.values())
    print("\n=== Styles (master) ===")
    print("styleGuide:", dict(sorted(by_guide.items())))
    print("type:", dict(sorted(by_type.items())))

    ferm = load_json_map(MASTER / "fermentables.json")
    print("\n=== Fermentable variant families (master) ===")
    for family, count in variant_families(list(ferm)):
        print(f"  {family}: {count}")

    policy_removals = []
    for name, s in styles.items():
        if s.get("styleGuide") == "BJCP 2015":
            policy_removals.append(("style", name))
    for name, f in ferm.items():
        if name.startswith("Apple Juice"):
            policy_removals.append(("fermentable", name))
        elif name == "Honey" or name.startswith("Honey - "):
            policy_removals.append(("fermentable", name))
    yeasts = load_json_map(MASTER / "yeasts.json")
    for name in ("Cider", "Cider Yeast", "English Cider Yeast"):
        if name in yeasts:
            policy_removals.append(("yeast", name))

    print(f"\n=== Policy removal candidates (master): {len(policy_removals)} ===")
    print(f"  styles (BJCP 2015): {sum(1 for t, _ in policy_removals if t == 'style')}")
    print(f"  fermentables: {sum(1 for t, _ in policy_removals if t == 'fermentable')}")
    print(f"  yeasts: {sum(1 for t, _ in policy_removals if t == 'yeast')}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
