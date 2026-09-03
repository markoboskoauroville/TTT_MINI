#!/usr/bin/env python3
"""
Test 1 for the bottom row, copied from SwiftKey.

MEASURED from Microsoft's own documentation, 26.8.2026: the `123` key sits in the LOWER LEFT,
directly below shift, and `abc` returns from the symbol and emoji layouts **in that same corner**.
The emoji key is lower right; symbols are two panels deep.

The corner is the whole point. A layout switcher that moves between panels is a switcher he has to
find twice, and finding it twice is what makes a keyboard feel foreign.

    python3 scripts/test_bottom_row.py
"""

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LAYOUTS = ROOT / "app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts"

failures: list[str] = []
checks = 0


def check(name: str, ok: bool, detail: str = "") -> None:
    global checks
    checks += 1
    if not ok:
        failures.append(f"{name}: {detail}")


def bottom(mod: str):
    rows = json.loads((LAYOUTS / mod / "default.json").read_text())
    return [k.get("label") for k in rows[-1]]


def labels_all(mod: str):
    rows = json.loads((LAYOUTS / mod / "default.json").read_text())
    return [k.get("label") for row in rows for k in row]


# ---------------------------------------------------------------- the corner
check("letters: 123 is bottom-left", bottom("charactersMod")[0] == "view_symbols",
      str(bottom("charactersMod")))
check("symbols: abc returns in the same corner", bottom("symbolsMod")[0] == "view_characters",
      str(bottom("symbolsMod")))
check("symbols2: abc returns in the same corner", bottom("symbols2Mod")[0] == "view_characters",
      str(bottom("symbols2Mod")))

# THE ONE INVARIANT: the switcher does not move between panels. If it does, he finds it twice.
firsts = {m: bottom(m)[0] for m in ("charactersMod", "symbolsMod", "symbols2Mod")}
check("the switcher never moves", all(f.startswith("view_") for f in firsts.values()), str(firsts))

# ---------------------------------------------------------------- nothing was lost
#
# A reorder that drops a key is a reorder that deletes a feature, and the keys here include ctrl,
# which he uses for Ctrl+P and Ctrl+F.
for mod, expected in (
    ("charactersMod", {"ctrl", "language_switch", "view_symbols2", "space", ".", "enter",
                       "view_symbols", "shift", "delete"}),
    ("symbolsMod", {"ctrl", "language_switch", "view_symbols2", "space", ".", "enter",
                    "view_characters"}),
    ("symbols2Mod", {"ctrl", "language_switch", "view_symbols", "space", ".", "enter",
                     "view_characters"}),
):
    got = set(l for l in labels_all(mod) if l)
    missing = expected - got
    check(f"{mod}: no key was dropped", not missing, f"missing {missing}")

# Ctrl survives, and it is the one whose loss he would feel first.
for mod in ("charactersMod", "symbolsMod", "symbols2Mod"):
    check(f"{mod}: ctrl is still there", "ctrl" in labels_all(mod), "Ctrl+P and Ctrl+F would be gone")

# The comma stays reachable from the period's popup, which is where it has always been.
chars = json.loads((LAYOUTS / "charactersMod" / "default.json").read_text())
period = next(k for row in chars for k in row if k.get("label") == ".")
popup = [p.get("label") for p in period.get("popup", {}).get("relevant", [])]
check("comma is still on the period's popup", "," in popup, str(popup))

# ---------------------------------------------------------------- the files still parse
for mod in ("charactersMod", "symbolsMod", "symbols2Mod"):
    rows = json.loads((LAYOUTS / mod / "default.json").read_text())
    check(f"{mod}: parses as rows of keys", isinstance(rows, list) and all(isinstance(r, list) for r in rows))
    for row in rows:
        for k in row:
            check(f"{mod}: every key has a code", "code" in k, str(k))

print(f"bottom row, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
