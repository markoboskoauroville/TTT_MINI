#!/usr/bin/env python3
"""
Test 1 for the three-zone spacebar and the message strip.

    python3 scripts/test_space_zones.py
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


def zone(fraction):
    """The port of the spacebar's zone rule."""
    if fraction < 0.25:
        return "left"
    if fraction > 0.75:
        return "right"
    return "space"


# ---------------------------------------------------------------- the zones
check("far left is left", zone(0.0) == "left")
check("far right is right", zone(1.0) == "right")
check("dead centre is space", zone(0.5) == "space")

# The middle is HALF the bar and each arrow a quarter. Walked, because the boundaries are where a
# fat-fingered press lands and where an off-by-one would put an arrow under his thumb.
walked = 0
counts = {"left": 0, "space": 0, "right": 0}
for i in range(0, 1001):
    f = i / 1000
    counts[zone(f)] += 1
    walked += 1
check("the middle is about half the bar", 480 <= counts["space"] <= 520, str(counts))
check("the arrows are about a quarter each",
      230 <= counts["left"] <= 270 and 230 <= counts["right"] <= 270, str(counts))
check("every point belongs to exactly one zone", sum(counts.values()) == 1001)

# The boundaries themselves resolve to space, so the wider target wins a tie — a press that lands on
# the line moves the cursor never, and types a space instead. The reverse would move his cursor when
# he meant to type.
check("the left boundary is space", zone(0.25) == "space", "an ambiguous press would move the cursor")
check("the right boundary is space", zone(0.75) == "space")

# Monotonic: left, then space, then right, with no zone reappearing. A rule that came back would mean
# two disconnected strips doing the same thing.
seen = []
for i in range(0, 1001):
    z = zone(i / 1000)
    if not seen or seen[-1] != z:
        seen.append(z)
check("the zones are contiguous", seen == ["left", "space", "right"], str(seen))


def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


legacy = code(SRC / "dictate/ui/LegacyDictateLayout.kt")
check("the spacebar has zones", "onClickAt = { fraction ->" in legacy, "one action across the whole bar")
check("left sends the left arrow", "fraction < 0.25f -> keyboardManager.tapKey(KeyCode.ARROW_LEFT)" in legacy)
check("right sends the right arrow", "fraction > 0.75f -> keyboardManager.tapKey(KeyCode.ARROW_RIGHT)" in legacy)
check("the long press still opens the pad", "onLongClick = { MaCursorPad.open() }" in legacy,
      "the cursor pad is what the zones are a shortcut for")
check("a positional key does not also fire onClick", "if (onClickAt == null) {" in legacy,
      "a tap would send a space AND an arrow")

# ---------------------------------------------------------------- the messages
msg = code(SRC / "dictate/ui/MaMessage.kt")
row = code(SRC / "dictate/ui/MaFeatureRow.kt")
check("there is a message holder", "object MaMessage" in msg, "still using system toasts")
check("a new message replaces the old", "serial.value + 1" in msg,
      "three quick presses would queue six seconds of stale text")
check("no toasts left in the row", "Toast.makeText" not in row,
      "a toast lands over the keys, and setGravity has been ignored since Android 11")
check("the row uses the strip", row.count("MaMessage.show(") >= 10, "some messages still land elsewhere")
check("the strip is drawn with the chrome", "if (drawChrome) {" in row,
      "two feature rows would each draw their own copy")
check("it clears itself", "MaMessage.clear()" in row, "a message would stay until the next one")

print(f"space zones, test 1: {checks} checks, {len(failures)} failed ({walked} points walked)")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
