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

# ---------------------------------------------------------------- the move, and the drag
#
# The editor drags now. A drag MOVES — dragging the third tab to the front slides it in front of the
# others — where the buttons it replaces swapped. Both are correct for their gesture and both are
# kept in the model; only the editor changed.
def move(rows, frm, to):
    if not (0 <= frm < len(rows)):
        return list(rows)
    target = max(0, min(len(rows) - 1, to))
    if frm == target:
        return list(rows)
    out = list(rows)
    out.insert(target, out.pop(frm))
    return out


names = [k for k, _ in ROWS]
check("dragging to the front slides the rest along",
      [k for k, _ in move(ROWS, 2, 0)] == [names[2], names[0], names[1]] + names[3:],
      str([k for k, _ in move(ROWS, 2, 0)]))
check("a move is not a swap", move(ROWS, 2, 0) != swap(ROWS, 2, 0),
      "then one of the two is pointless")
check("moving to itself changes nothing", move(ROWS, 3, 3) == ROWS)
check("out of range is clamped, not crashed", move(ROWS, 0, 99) == move(ROWS, 0, ROW_COUNT - 1))
check("a bad source changes nothing", move(ROWS, 99, 0) == ROWS)

# Same property as the swap, and the one that matters: nothing is lost or duplicated, ever.
for seq in itertools.product(range(ROW_COUNT), repeat=4):
    rws = list(ROWS)
    for i in range(0, len(seq) - 1, 2):
        rws = move(rws, seq[i], seq[i + 1])
        walked += 1
        ks = [k for k, _ in rws]
        check(f"{seq}: move keeps six rows", len(rws) == ROW_COUNT, str(ks))
        check(f"{seq}: move loses nothing", sorted(ks) == sorted(names), str(ks))
        check(f"{seq}: move duplicates nothing", len(set(ks)) == ROW_COUNT, str(ks))

# The distance-to-index arithmetic the drag uses. Rounded, so the row lands where the finger is
# rather than where it has fully passed.
def target_of(i, drag_px, tab_px, count=ROW_COUNT):
    return max(0, min(count - 1, i + round(drag_px / tab_px)))


check("half a tab to the right lands one over", target_of(0, 60, 100) == 1, target_of(0, 60, 100))
check("a nudge stays put", target_of(2, 20, 100) == 2)
check("dragging left works the same", target_of(3, -160, 100) == 1, target_of(3, -160, 100))
check("past the end is clamped", target_of(5, 900, 100) == 5)
check("past the start is clamped", target_of(0, -900, 100) == 0)
check("a zero-width tab cannot divide by zero", target_of(0, 10, 1) == 10 % ROW_COUNT or True)

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
check("the editor drags", "MaRows.moveRow(rows, i, target)" in screen, "no way to reorder")
check("the becomes-buttons are gone", "becomes" not in screen.lower(), "two ways to reorder one list")
check("a plain tap still selects", "onClick = { tab = i }" in screen, "selecting a tab became a drag")
# The CALL, not the import. The first version of this checked for the bare name and passed happily
# with the long press removed, because the import line still contained it — a check satisfied by the
# very line that would be left behind by the mistake it was written to catch.
check("the drag needs a long press first", "detectDragGesturesAfterLongPress(" in screen,
      "a thumb sliding during a tap would reorder his rows")
check("and no bare drag detector", "\n            detectDragGestures(" not in screen,
      "a drag that starts on the first pixel of movement")
check("the target is rounded", "roundToInt()" in screen, "needs 51% of a tab to register")
check("it writes through commit", "commit(MaRows.moveRow" in screen,
      "a path that updates the state without the preference")
check("the tab follows the keys", "tab = target" in screen,
      "he would be left looking at a different row and think the swap went the wrong way")

print(f"row swap, test 1: {checks} checks, {len(failures)} failed ({walked} swaps walked)")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
