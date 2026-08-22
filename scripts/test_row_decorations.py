#!/usr/bin/env python3
"""
Test 1 for build 279: the level meter's curve, the clock's format, and the wiring of three changes.

    python3 scripts/test_row_decorations.py
"""

import math
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


# ---------------------------------------------------------------- the meter
def bar(level):
    """The port of maLevelToBar."""
    v = max(abs(level), 1e-6)
    db = 20.0 * math.log10(v)
    return min(max((db + 48.0) / 48.0, 0.06), 1.0)


check("silence still shows a sliver", bar(0.0) == 0.06, bar(0.0))
check("full scale fills it", bar(1.0) == 1.0, bar(1.0))
check("it never leaves the box", all(0.0 <= bar(x) <= 1.0 for x in [0, 1e-9, 0.5, 1.0, 2.0, -1.0]))
check("negative levels read the same as positive", bar(-0.4) == bar(0.4))

# Monotonic across the whole range, or the bar moves the wrong way somewhere.
xs = [i / 500 for i in range(501)]
check("louder is never shorter", all(bar(a) <= bar(b) for a, b in zip(xs, xs[1:])), "not monotonic")

# The curve is the point: ordinary speech has to MOVE the bar. A linear bar on the raw level would
# sit at a tenth of the height for a normal voice and only wake up for a shout.
quiet, speech, loud = bar(0.02), bar(0.15), bar(0.6)
check("normal speech is well up the meter", speech > 0.55, f"{speech:.2f}")
check("quiet is visibly lower than speech", quiet < speech - 0.15, f"{quiet:.2f} vs {speech:.2f}")
check("loud is near the top but not pinned", 0.85 < loud < 1.0, f"{loud:.2f}")
check("a linear bar would have been useless", 0.15 < speech, "the whole reason for the dB curve")

# ---------------------------------------------------------------- the clock
def clock(ms):
    return "%d:%02d" % (ms // 60000, (ms // 1000) % 60)


check("zero", clock(0) == "0:00")
check("seconds pad", clock(7000) == "0:07")
check("the minute rolls", clock(60000) == "1:00")
check("under a second is still 0:00", clock(999) == "0:00")
check("long takes remain readable", clock(3_723_000) == "62:03", clock(3_723_000))
check("it never wraps at an hour", ":" in clock(3_600_000) and clock(3_600_000).startswith("60"))

# ---------------------------------------------------------------- the wiring
def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


row = code(SRC / "dictate/ui/MaFeatureRow.kt")
check("the record key reads the same level as the bar", "DictateController.audioLevel" in row, "its own source")
check("and the same clock", "r.accumulatedMs" in row, "a second way of counting the same seconds")
check("the decorations are only drawn while recording", row.count("if (recording) {") >= 2,
      "a meter reading nothing on an idle key")

check("the send key asks whether there is anything to press", "MaMagicTargets.sendVisible()" in row, "always lit")
check("and dims rather than disappears", "tint = if (sendHere) null else" in row,
      "the keys beside it would move")

targets = code(SRC / "dictate/MaMagicTargets.kt")
check("looking does not press", "fun sendVisible()" in targets, "no way to ask without acting")
check("it uses the same term as the press", "resolveTerm(targets, stored)" in targets,
      "lit for one button and pressing another")

picker = code(SRC / "app/settings/dictate/MaRowsScreen.kt")
check("the picker sits above the keyboard", "imePadding()" in picker, "Add is behind the keyboard again")
check("Add is beside Cancel", picker.count("onAdd(chosen.toList())") == 2, "only reachable at the bottom")
check("and is dead when nothing is ticked", picker.count("enabled = chosen.isNotEmpty()") == 2, "adds nothing")

print(f"row decorations, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
