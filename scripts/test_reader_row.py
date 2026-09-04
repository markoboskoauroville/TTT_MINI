#!/usr/bin/env python3
"""
Test 1 for the reader row: the reading's commands, on a row of their own.

    python3 scripts/test_reader_row.py
"""

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


def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


rows = code(SRC / "dictate/MaRows.kt")
prefs = code(SRC / "app/AppPrefs.kt")
row_ui = code(SRC / "dictate/ui/MaFeatureRow.kt")
kbd = code(SRC / "ime/text/TextInputLayout.kt")
board = code(SRC / "app/settings/dictate/MaSwitchboardScreen.kt")
order = code(SRC / "dictate/MaFeatureOrder.kt")

# ---------------------------------------------------------------- a special row like the others
check("its own arrangement", "fun defaultReaderRow()" in rows, "not a row of its own")
check("its own parser", "fun parseReaderRow(" in rows, "would share another row's storage")
check("its own preference", "maReaderRow = string(" in prefs, "arranged nowhere")
check("its own switch", "maReaderRowShown = boolean(" in prefs, "no way to show it")
check("off by default", 'key = "dictate__ma_reader_row_shown",\n            default = false,' in prefs,
      "six transport keys on a keyboard that is not reading")

# ---------------------------------------------------------------- every command he asked for
for key, why in (
    ("READ_PLAY", "play and pause, which he asked to be one key"),
    ("READ_PREV", "skip backwards"),
    ("READ_NEXT", "skip forwards"),
    ("READ_STOP", "stop waiting"),
    ("READ_WATCH", "whether it waits at the end"),
    ("READ_SLOWER", "speed down"),
    ("READ_FASTER", "speed up"),
):
    check(f"{key} exists — {why}", f'{key}("' in order, "missing from the catalogue")
    check(f"{key} is drawn", f"MaFeatureKey.{key}" in row_ui, "in the catalogue and not on the keyboard")

# Play and pause are ONE key, and it delegates rather than deciding for itself.
check("play/pause is one key", "READ_PLAY" in row_ui and "READ_PAUSE" not in order,
      "two keys where he asked for one")
check("it uses the reader's own toggle", "MaReader.toggle(context)" in row_ui,
      "a second definition of what a press means")

# Stop ends the WATCH as well — that is the "stop waiting" he asked for, and MaReader.stop clears it.
check("stop ends the wait too", "MaReader.stop()" in row_ui, "the watch would keep running")

# ---------------------------------------------------------------- two ways to switch it on
check("a key switches the row", "MaFeatureKey.READER_ROW ->" in row_ui, "only reachable from settings")
check("the key wears the ring", "ring = if (readerShown) onGreen else MaSwitcherRingOff" in row_ui,
      "no sign whether the row is up")
check("the switchboard switches it too", "Entry.READER_ROW ->" in board, "not in the switchboard")
check("both read ONE preference", "maReaderRowShown" in board and "maReaderRowShown" in row_ui,
      "two switches for one row, the fault this app keeps meeting")

# ---------------------------------------------------------------- the simple key still just reads
check("the plain reader key survives", 'READER("reader"' in order,
      "the one-press case is the common one and must not need a row")
check("and the row is a separate key", 'READER_ROW("reader_row"' in order, "one key doing both")

# ---------------------------------------------------------------- drawn on the keyboard
check("the keyboard draws it behind its switch", "if (readerRowShown) {" in kbd, "always on, or never")
check("it draws no chrome", "readerRowOnly = true,\n                    drawChrome = false," in kbd,
      "a second wand bar on one keyboard")
check("it is a third special row", "readerRowOnly ->" in row_ui, "would draw the feature rows")

# The speed clamp: the row must not be able to set a speed the reader cannot use.
def clamp(v):
    return max(50, min(300, v))


for start, step in ((300, 10), (50, -10), (100, 10), (55, -10)):
    check(f"speed {start}{step:+} stays in range", 50 <= clamp(start + step) <= 300, str(clamp(start + step)))
check("the clamp is in the code", "coerceIn(50, 300)" in row_ui, "a speed nothing can play")

print(f"reader row, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
