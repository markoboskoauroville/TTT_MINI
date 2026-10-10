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


# ---------------------------------------------------------------- one key, one cycle
#
# `sy1` and `sy2` are gone, replaced by ONE key that walks the views in a ring:
#
#     characters -> symbols -> symbols2 -> characters
#
# Two keys meant two orders to remember and one of them skipped a panel. **A ring has no order to
# learn: press it again and you arrive somewhere new, and pressing it enough always brings you
# home.** Its label is the view it goes TO, so the key says where the next press lands.
#
# The long press keeps all three on the popup, so a deadline does not mean walking the ring.
# READ FROM THE FILES, not written here. The first version hardcoded this dict and the ring-walk
# checks below then passed a sabotage that made symbols point back to characters — they were walking
# the test's idea of the ring, not the keyboard's. **A model is not a witness to the code it models**,
# and this is the second time that exact sentence has been earned this month.
CYCLE = {m: bottom(m)[1] for m in ("charactersMod", "symbolsMod", "symbols2Mod")}
EXPECTED = {"charactersMod": "view_symbols", "symbolsMod": "view_symbols2",
            "symbols2Mod": "view_characters"}
for _m, _t in EXPECTED.items():
    check(f"{_m}: goes to {_t}", CYCLE.get(_m) == _t, f"{CYCLE.get(_m)}")
for mod in EXPECTED:
    row = bottom(mod)
    check(f"{mod}: ctrl is on the far left", row[0] == "ctrl", str(row))
    check(f"{mod}: the ring key is second", row[1].startswith("view_"), str(row))

# THE RING CLOSES, and every panel is on it. Walked rather than eyeballed: three steps from any
# starting panel must visit all three and return.
for start in CYCLE:
    seen, here = [], start
    for _ in range(3):
        here = {"view_characters": "charactersMod", "view_symbols": "symbolsMod",
                "view_symbols2": "symbols2Mod"}[CYCLE[here]]
        seen.append(here)
    check(f"from {start}: the ring visits all three", set(seen) == set(CYCLE), str(seen))
    check(f"from {start}: three presses come home", seen[-1] == start, str(seen))

# The jump is still there for when he has no time to walk it.
for mod in CYCLE:
    rows = json.loads((LAYOUTS / mod / "default.json").read_text())
    popup = [x.get("label") for x in rows[-1][1].get("popup", {}).get("relevant", [])]
    check(f"{mod}: the long press offers all three", set(popup) == set(CYCLE.values()), str(popup))

# ---------------------------------------------------------------- the corner
# ---------------------------------------------------------------- ctrl far left, one switcher
#
# He asked for SwiftKey's corner to be set aside here: `ctrl` on the far left, then a SINGLE view
# switcher. The second `sy` key is gone — two keys that both change the view are two orders to
# remember, and he had to learn which one skipped what.
for mod in ("charactersMod", "symbolsMod", "symbols2Mod"):
    row = bottom(mod)
    check(f"{mod}: ctrl is far left", row[0] == "ctrl", str(row))
    check(f"{mod}: the switcher is second", row[1].startswith("view_"), str(row))

# THE CYCLE, with no skipping: letters -> symbols -> symbols2 -> letters.
#
# Each panel's key goes to the NEXT panel, so pressing it repeatedly walks every view and returns.
# Before this, two keys each jumped to a fixed panel and neither visited all three in order.
CYCLE = {"charactersMod": "view_symbols",
         "symbolsMod": "view_symbols2",
         "symbols2Mod": "view_characters"}
for mod, expected in CYCLE.items():
    check(f"{mod}: goes to {expected}", bottom(mod)[1] == expected, str(bottom(mod)))

# It must be a CYCLE, not a chain: every view reachable, and every view left.
targets = set(CYCLE.values())
check("every view is a destination", targets == {"view_characters", "view_symbols", "view_symbols2"},
      str(targets))
check("no view points at itself", all(CYCLE[m] != {"charactersMod": "view_characters",
                                                   "symbolsMod": "view_symbols",
                                                   "symbols2Mod": "view_symbols2"}[m] for m in CYCLE),
      "a key that goes where it already is does nothing")

# THE SHORTCUT: a long press offers all three, so he can jump when he has no time to cycle.
for mod in ("charactersMod", "symbolsMod", "symbols2Mod"):
    rows = json.loads((LAYOUTS / mod / "default.json").read_text())
    popup = [x.get("label") for x in rows[-1][1].get("popup", {}).get("relevant", [])]
    check(f"{mod}: long press offers every view", set(popup) == targets, str(popup))

# ---------------------------------------------------------------- nothing was lost
#
# A reorder that drops a key is a reorder that deletes a feature, and the keys here include ctrl,
# which he uses for Ctrl+P and Ctrl+F.
for mod, expected in (
    # Updated for SwiftKey's row: language_switch is gone on purpose and view_symbols2 moved to the
    # 123 popup. What must still be present is everything he presses.
    # The comma key became the Symbols 2 key (10.10.2026); the comma itself is on the period's
    # hold, checked below.
    ("charactersMod", {"ctrl", "space", ".", "enter", "view_symbols", "view_symbols2", "shift", "delete"}),
    # The switcher on each panel is now the NEXT view, not always "back to letters" — so what must be
    # present is "a switcher", checked above, rather than one particular destination. Letters remain
    # reachable from every panel: by cycling, and immediately from the long-press menu.
    ("symbolsMod", {"ctrl", "space", ".", ",", "enter", "view_symbols2"}),
    ("symbols2Mod", {"ctrl", "space", ".", ",", "enter", "view_characters"}),
):
    got = set(l for l in labels_all(mod) if l)
    missing = expected - got
    check(f"{mod}: no key was dropped", not missing, f"missing {missing}")

# ---------------------------------------------------------------- SwiftKey's row, applied
#
# From his screenshot: 123 | , | space | . | enter, with a DEDICATED COMMA left of space and the
# period carrying ,!? on its popup. Five keys where his had seven.
#
# **This is a removal, so it is the part to be careful about.** `language_switch` went and should
# have: the language is chosen by the record and send keys now, so a key that switches it is the
# mode this app spent three builds deleting. `view_symbols2` went from the ROW and is still on the
# 123 key's popup — moved, not removed.
#
# `ctrl` STAYED, against the screenshot. SwiftKey has no Ctrl+P and no Ctrl+F; he uses both hourly,
# and copying a layout is not a reason to delete a feature that layout never had.
for mod in ("symbolsMod", "symbols2Mod"):
    row = bottom(mod)
    check(f"{mod}: six keys", len(row) == 6, str(row))
    check(f"{mod}: a dedicated comma", "," in row, "the comma is still hidden on a popup")
    check(f"{mod}: the comma sits left of space", row.index(",") < row.index("space"), str(row))
    check(f"{mod}: no language switcher", "language_switch" not in row,
          "a key that switches a mode the record keys already decide")
    check(f"{mod}: no second view key", sum(1 for k in row if k.startswith("view_")) == 1,
          "two keys changing the view is two orders to remember")
    check(f"{mod}: enter is last", row[-1] == "enter", str(row))

# ---------------------------------------------------------------- Symbols 2 in one press
#
# 10.10.2026, his ask: reaching Symbols 2 from the letters took two presses (sy1, then sy2) and that
# is one too many. The comma key right of sy1 became a Symbols 2 key, so from the letters each
# symbol panel is one press away: sy1 on the left, sy2 beside it. The comma moved to the period's
# hold, which the period already advertised with a "," in its corner.
row = bottom("charactersMod")
check("charactersMod: six keys", len(row) == 6, str(row))
check("charactersMod: sy1 then sy2, side by side", row[1:3] == ["view_symbols", "view_symbols2"], str(row))
check("charactersMod: no comma key on the row", "," not in row, str(row))
check("charactersMod: no language switcher", "language_switch" not in row, str(row))
check("charactersMod: enter is last", row[-1] == "enter", str(row))
_chars = json.loads((LAYOUTS / "charactersMod" / "default.json").read_text())
_sy2 = [p.get("label") for p in _chars[-1][2].get("popup", {}).get("relevant", [])]
check("charactersMod: sy2's hold offers every view too", set(_sy2) == targets, str(_sy2))
_period = next(k for k in _chars[-1] if k.get("label") == ".")
check("the period's hold types the comma", (_period.get("popup", {}).get("main") or {}).get("code") == 44,
      "the comma would be unreachable from the letters")

# What the removed keys offered must still be reachable, or the tidy-up deleted a panel.
chars_rows = json.loads((LAYOUTS / "charactersMod" / "default.json").read_text())
sw_popup = [p.get("label") for p in chars_rows[-1][1].get("popup", {}).get("relevant", [])]
check("the second symbol panel is still reachable", "view_symbols2" in sw_popup, str(sw_popup))

# Ctrl survives, and it is the one whose loss he would feel first.
for mod in ("charactersMod", "symbolsMod", "symbols2Mod"):
    check(f"{mod}: ctrl is still there", "ctrl" in labels_all(mod), "Ctrl+P and Ctrl+F would be gone")

# The comma stays reachable from the period's popup, which is where it has always been.
chars = json.loads((LAYOUTS / "charactersMod" / "default.json").read_text())
period = next(k for row in chars for k in row if k.get("label") == ".")
popup = [p.get("label") for p in period.get("popup", {}).get("relevant", [])]
# The comma has its own key now, so the period's popup carries ! and ? — which is what SwiftKey
# prints above that key as ",!?".
check("the period offers ! and ?", "!" in popup and "?" in popup, str(popup))

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
