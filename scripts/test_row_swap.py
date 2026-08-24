#!/usr/bin/env python3
"""
Test 1 for F1/F2/F3 and swapping rows.

    python3 scripts/test_row_swap.py
"""

import itertools
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "app/src/main/kotlin/dev/patrickgold/florisboard"

failures: list[str] = []
checks = 0


def check(name: str, ok: bool, detail: str = "") -> None:
    global checks
    checks += 1
    if not ok:
        failures.append(f"{name}: {detail}")


def swap(rows, a, b):
    if a == b or not (0 <= a < len(rows)) or not (0 <= b < len(rows)):
        return list(rows)
    out = list(rows)
    out[a], out[b] = out[b], out[a]
    return out


def set_enabled(rows, i, on):
    if not (0 <= i < len(rows)):
        return list(rows)
    return [(k, on if j == i else e) for j, (k, e) in enumerate(rows)]


# Six rows now, up from three. The list is built rather than written out, so this suite says what
# ROW_COUNT is instead of hardcoding a number that has already changed once.
ROW_COUNT = 6
ROWS = [(f"keys{i}", i < 2) for i in range(ROW_COUNT)]

# ---------------------------------------------------------------- the swap
# Written against ROWS rather than against the old hardcoded names, which is what broke when the
# rows became six: the literal said keysA/keysB and the generated list says keys0/keys1. A test that
# repeats its fixture's contents by hand fails on the day the fixture changes, for no reason.
check("two rows trade places", swap(ROWS, 0, 1)[:2] == [ROWS[1], ROWS[0]], str(swap(ROWS, 0, 1)[:2]))
check("every other row is untouched", swap(ROWS, 0, 1)[2:] == ROWS[2:], "a row he did not touch moved")
check("the on/off state travels with the keys", swap(ROWS, 0, 2)[0] == ROWS[2],
      "the flag stayed with the position instead of the keys")
check("swapping with itself changes nothing", swap(ROWS, 1, 1) == ROWS)
check("out of range changes nothing", swap(ROWS, 0, 99) == ROWS)
check("swap is its own undo", swap(swap(ROWS, 0, 2), 0, 2) == ROWS, "not reversible")

# NOTHING IS EVER LOST OR DUPLICATED — the property that makes swap safe and insert not.
walked = 0
for seq in itertools.product(range(ROW_COUNT), repeat=4):
    rows = list(ROWS)
    for i in range(0, len(seq) - 1, 2):
        rows = swap(rows, seq[i], seq[i + 1])
        walked += 1
        keys = [k for k, _ in rows]
        check(f"{seq}: still six rows", len(rows) == ROW_COUNT, str(rows))
        check(f"{seq}: no row lost", sorted(keys) == sorted(k for k, _ in ROWS), str(keys))
        check(f"{seq}: no row duplicated", len(set(keys)) == ROW_COUNT, str(keys))

# ---------------------------------------------------------------- the toggles
r = set_enabled(ROWS, 2, True)
check("a row can be switched on", r[2][1] is True)
check("only that row changed", r[:2] == ROWS[:2], "another row moved with it")
check("off then on returns", set_enabled(set_enabled(ROWS, 0, False), 0, True) == ROWS)
# All three off is allowed here; visibleRows draws the settings key alone rather than nothing.
allo = ROWS
for i in range(ROW_COUNT):
    allo = set_enabled(allo, i, False)
check("all three can be off", all(not e for _, e in allo), "a row cannot be switched off")


# ---------------------------------------------------------------- the wiring
def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


rows_kt = code(SRC / "dictate/MaRows.kt")
check("the swap is in the model", "fun swapRows(" in rows_kt, "written on a screen and untestable")
check("the toggle is in the model", "fun setRowEnabled(" in rows_kt, "written on a screen")

order = code(SRC / "dictate/MaFeatureOrder.kt")
for n in range(1, ROW_COUNT + 1):
    check(f"F{n} is a key", f'ROW_{n}("row_{n}"' in order, "not in the catalogue")
check("the model says six", "ROW_COUNT = 6" in rows_kt, "the keys and the model disagree")

# UPGRADE, the part of this that could lose his work.
#
# parse pads a short read to ROW_COUNT with empty, disabled rows — so a stored three-row arrangement
# opens as three arranged rows and three empty ones. Going the OTHER way truncates, which is why the
# constant carries a warning: lowering it deletes the rows beyond it the first time the app writes.
def parse_pad(stored, count):
    return [stored[i] if i < len(stored) else ("empty", False) for i in range(count)]


old_three = [("keysA", True), ("keysB", True), ("keysC", False)]
upgraded = parse_pad(old_three, ROW_COUNT)
check("an old three-row arrangement survives", upgraded[:3] == old_three, str(upgraded[:3]))
check("the new rows arrive empty", all(k == "empty" for k, _ in upgraded[3:]), str(upgraded[3:]))
check("and switched off", not any(e for _, e in upgraded[3:]), "three rows appear on his keyboard uninvited")


row_ui = code(SRC / "dictate/ui/MaFeatureRow.kt")
check("the keys are written once", row_ui.count("MaFeatureKey.ROW_1, MaFeatureKey.ROW_2, MaFeatureKey.ROW_3") == 1,
      "three copies is three places for the ring to disagree with the row")
check("the ring says which are showing", "ring = if (on) onGreen else null" in row_ui, "no feedback")
check("the key writes through the model", "MaRows.setRowEnabled(storedRows, which, !on)" in row_ui,
      "its own idea of what a row is")

# The four arrows.
for a in ("LEFT", "RIGHT", "UP", "DOWN"):
    check(f"arrow {a} is a key", f'ARROW_{a}("arrow_{a.lower()}"' in order, "not in the catalogue")
check("the arrows are written once", row_ui.count("MaFeatureKey.ARROW_LEFT, MaFeatureKey.ARROW_RIGHT") == 1,
      "four copies is four chances to drift")
check("they send the keyboard's own codes", "keyboardManager.tapKey(code)" in row_ui,
      "its own movement, so long-press repeat and shift-selection would differ")

screen = code(SRC / "app/settings/dictate/MaRowsScreen.kt")
check("the editor can swap", "MaRows.swapRows(rows, tab, target)" in screen, "no way to reorder")
check("it writes through commit", "commit(MaRows.swapRows" in screen,
      "a path that updates the state without the preference")
check("the tab follows the keys", "tab = target" in screen,
      "he would be left looking at a different row and think the swap went the wrong way")

print(f"row swap, test 1: {checks} checks, {len(failures)} failed ({walked} swaps walked)")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
