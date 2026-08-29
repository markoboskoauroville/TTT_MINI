#!/usr/bin/env python3
"""
Test 1 for the bucket row: a special row like the copy row, with two ways to switch it.

    python3 scripts/test_bucket_row.py
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

# ---------------------------------------------------------------- it is a special row
check("it has its own arrangement", "fun defaultBucketRow()" in rows, "not a row of its own")
check("its own parser", "fun parseBucketRow(" in rows, "would share the copy row's storage")
check("its own preference", "maBucketRow = string(" in prefs, "arranged nowhere")
check("its own switch", "maBucketRowShown = boolean(" in prefs, "no way to show or hide it")
check("off by default", 'maBucketRowShown = boolean(\n            key = "dictate__ma_bucket_row_shown",\n            default = false,' in prefs,
      "a row that appears uninvited is a row he must switch off before he can type")

# The default is what he had on screen: bin, C1, C2, C3, A up, swap.
default_block = rows[rows.index("fun defaultBucketRow"):]
default_block = default_block[:default_block.index("fun parseBucketRow")]
for expected in ("CLIP_CLEAR", "AUTO_BUCKET", "APP_SWITCH", "(1..3).map { Entry(Button.Clip(it)) }"):
    check(f"the default has {expected}", expected in default_block, "not the row he photographed")

# ---------------------------------------------------------------- drawn, and only when asked
check("the row can be drawn alone", "bucketRowOnly" in row_ui, "no way to draw just this row")
check("the two special rows are separate", "copyRowOnly ->" in row_ui and "bucketRowOnly ->" in row_ui,
      "one flag doing two jobs")
check("the keyboard draws it behind its switch", "if (bucketRowShown)" in kbd, "always on, or never")
check("it draws no chrome", "bucketRowOnly = true,\n                    drawChrome = false," in kbd,
      "a second wand bar and a second dashboard on one keyboard")

# ---------------------------------------------------------------- two ways to switch it
check("a key switches it", "MaFeatureKey.BUCKET_ROW ->" in row_ui, "only reachable from settings")
check("the key wears the ring", "ring = if (bucketShown) onGreen else MaSwitcherRingOff" in row_ui,
      "no sign whether the row is there")
check("the switchboard switches it too", "Entry.BUCKET_ROW ->" in board, "not in the switchboard")
check("both read ONE preference",
      row_ui.count("maBucketRowShown") >= 1 and "prefs.dictate.maBucketRowShown" in board,
      "two switches for one row, which is the fault this app keeps meeting")
check("it is a key you can put on a row", 'BUCKET_ROW("bucketrow"' in order, "not in the catalogue")

# The switchboard id is APPENDED, not inserted: parse keeps his order and appends unknown ids, so a
# new entry must not shuffle the arrangement he built.
sb_order = code(SRC / "app/settings/dictate/MaSwitchboardOrder.kt")
ids = re.findall(r'([A-Z_]+)\("([a-z_]+)"\)', sb_order)
check("the new id exists", any(i[1] == "bucket_row" for i in ids), "no stable id")

print(f"bucket row, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
