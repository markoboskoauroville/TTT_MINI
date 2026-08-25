#!/usr/bin/env python3
"""
Test 1 for the reader's end condition.

The bug: at the bottom of a chat it read the same screen a thousand times. The old check compared the
new passage against the LAST one and stopped when they were identical — and a live screen differs by
a character or two between reads, so they never were.

    python3 scripts/test_reader_end.py
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "app/src/main/kotlin/dev/patrickgold/florisboard"

failures: list[str] = []
checks = 0
MAX_SCREENS = 30


def check(name: str, ok: bool, detail: str = "") -> None:
    global checks
    checks += 1
    if not ok:
        failures.append(f"{name}: {detail}")


def norm(t):
    # Every non-letter becomes a space. Dropping them instead glues "two\nthree" into "twothree",
    # which is what the Kotlin did on its first pass and what the re-wrap check below caught.
    return " ".join("".join(c if c.isalpha() else " " for c in t.lower()).split())


def read(screens, seen=None):
    """Reads screens until an end condition fires. Returns what was actually read aloud."""
    seen = set() if seen is None else seen
    out = []
    before = None
    for text in screens:
        cleaned = text.strip()
        stuck = before is not None and norm(before) == norm(text)
        if not cleaned or stuck or norm(cleaned) in seen or len(seen) >= MAX_SCREENS:
            break
        seen.add(norm(cleaned))
        out.append(cleaned)
        before = text
    return out


# ---------------------------------------------------------------- the bug, exactly as he met it
#
# The bottom of a chat: the same words every time, with a clock that ticks. The OLD check compared
# only against the previous passage and 12:01 != 12:00, so it read forever.
bottom = [f"the end of the conversation 12:0{i % 10}" for i in range(50)]
out = read(bottom)
check("the same screen is read once", len(out) == 1, f"read {len(out)} times")
check("and it is the first one", out[0].startswith("the end"), str(out[:1]))

# A changing digit must not make it look like a new screen.
check("digits do not make a new screen", norm("hello 12:00") == norm("hello 12:01"))
check("but different words do", norm("hello there") != norm("hello world"))
# A real newline, not an escaped one. Written through a heredoc the backslash survived into the test
# and it was comparing the literal characters "\\n" — passing or failing for reasons unrelated to
# wrapping. The check is about newlines, so it has to contain one.
check("re-wrapped text is the same text", norm("one two" + chr(10) + "three") == norm("one  two three"))

# ---------------------------------------------------------------- what must still work
pages = ["first screen", "second screen", "third screen"]
check("a real passage is read through", read(pages) == pages, str(read(pages)))
check("it stops when the screen stops moving", read(["a", "b", "b", "c"]) == ["a", "b"],
      "kept going after the screen froze")
check("a revisited screen ends it", read(["a", "b", "a", "c"]) == ["a", "b"],
      "a chat that scrolls back to something already read would loop")
check("a blank screen ends it", read(["a", "  ", "b"]) == ["a"])

# The backstop. It is meant never to fire, which is exactly why it is tested.
# Words, not numbers: the normaliser strips digits, so "screen 1" and "screen 2" are the SAME screen
# and the old fixture was testing the revisit rule, not the ceiling. A fixture that cannot reach the
# condition it is written for passes for the wrong reason.
long_run = [f"screen {chr(97 + i // 26)}{chr(97 + i % 26)}" for i in range(100)]
check("the ceiling holds", len(read(long_run)) == MAX_SCREENS, f"read {len(read(long_run))}")
check("the ceiling is far past real use", MAX_SCREENS >= 20, "would cut off a long article")

# A reading that has stopped must be able to start again on the same screen.
seen = set()
read(["the same page"], seen)
check("the record is per reading", read(["the same page"], set()) == ["the same page"],
      "the fix would refuse to read a page twice, ever")


# ---------------------------------------------------------------- the wiring
def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


reader = code(SRC / "dictate/MaReader.kt")
check("it remembers every passage", "passagesRead.contains(" in reader, "only the last one, as before")
check("it checks the screen actually moved", "screenStuck" in reader,
      "scrollScreenDown reports the action, not the movement")
check("it compares normalised text", "normalisedForCompare(" in reader, "a ticking clock defeats it")
check("there is a ceiling", "MAX_SCREENS" in reader, "nothing bounds the loop")
check("the record is cleared on stop", "passagesRead.clear()" in reader,
      "the next reading of the same screen would refuse to start")
check("the first screen is recorded", "passagesRead.add(normalisedForCompare(text))" in reader,
      "screen one could be read twice")

print(f"reader end, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
