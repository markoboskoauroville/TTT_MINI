#!/usr/bin/env python3
"""
Test 1 for watch mode: the reader waits for text that has not been written yet.

He reads a Claude answer while it is still being written. The screen does not CHANGE, it GROWS — so
comparing whole screens makes every poll look like a new passage, because the old text is still in
it. **Read the difference, not the screen.**

    python3 scripts/test_watch_mode.py
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


def norm(t):
    return " ".join("".join(c if c.isalpha() else " " for c in t.lower()).split())


def new_tail(before, now):
    """The port of MaReader.newTail."""
    a, b = norm(before), norm(now)
    if not b or a == b:
        return None
    if not b.startswith(a) or not a:
        return now.strip() or None
    added = b[len(a):].strip()
    if not added:
        return None
    n = len(added.split(" "))
    raw = now.strip().split()
    if n >= len(raw):
        return now.strip()
    return " ".join(raw[-n:]) or None


# ---------------------------------------------------------------- a growing answer
check("nothing added is silence", new_tail("the answer so far", "the answer so far") is None,
      "it would re-read the same screen every poll")
check("the tail is what was added",
      new_tail("the answer so far", "the answer so far and more") == "and more")
check("only the tail, not the whole screen",
      new_tail("a b c", "a b c d") == "d", new_tail("a b c", "a b c d"))
check("several words at once", new_tail("a b", "a b c d e") == "c d e")

# A stream arrives in pieces. Every piece must be spoken exactly once, in order, with nothing lost
# and nothing repeated — which is the whole property watch mode has to hold.
stream = ["hello", "hello there", "hello there this", "hello there this is",
          "hello there this is a", "hello there this is a test"]
spoken, seen = [], stream[0]
for frame in stream[1:]:
    t = new_tail(seen, frame)
    if t:
        spoken.append(t)
    seen = frame
check("every word spoken once", " ".join(spoken) == "there this is a test", str(spoken))
check("nothing repeated", len(spoken) == len(set(spoken)), str(spoken))

# ---------------------------------------------------------------- not a continuation
check("a different screen is read whole", new_tail("old thing", "something else") == "something else")
check("scrolling away is a new passage", new_tail("a b c", "x y z") == "x y z")
check("an empty screen says nothing", new_tail("a b c", "") is None,
      "a blank poll would be read as a passage")
check("the first poll of a blank start reads all", new_tail("", "first words") == "first words")

# Punctuation must survive: what is spoken comes from the RAW text, not the normalised one.
# Punctuation INSIDE the tail survives, because the tail is cut from the raw text. Punctuation
# attached to the last old word does not, because the cut is on a word boundary — "well," keeps its
# comma with "well", which was already spoken.
#
# My first expectation here was ", that is done.", which would require cutting mid-word. That is the
# bug the implementation deliberately avoids: slicing the raw string by a normalised offset cut words
# in half. **A test expectation is a claim, and this one was wrong** — the code was right.
check("punctuation inside the tail survives",
      new_tail("well", "well, that is done.") == "that is done.",
      new_tail("well", "well, that is done."))
check("the cut is on a word boundary",
      " " not in (new_tail("a b", "a b crocodile") or " "), "half a word would be spoken")

# A ticking clock must not make a still screen look like it grew.
check("a clock does not count as new text",
      new_tail("the answer 12:00", "the answer 12:01") is None,
      "the reader would speak the screen again every minute")


def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


reader = code(SRC / "dictate/MaReader.kt")
check("the tail helper exists", "fun newTail(" in reader, "it would compare whole screens")
check("watching is a flag on the reading", "var watching: Boolean" in reader, "a mode he must remember")
# The CONDITION, not just the call. Searching for `watchForMore(...)` anywhere passed a sabotage that
# wrapped it in `if (false)` — the call was still in the file, doing nothing. **A check that finds a
# line does not know whether the line runs.**
check("the end waits instead of stopping", "if (watching && !cleaned.isBlank()) {" in reader,
      "it still stops at the bottom of a growing answer")
check("and the wait is what it does there",
      "watchForMore(context, onMessage)" in reader.split("if (watching && !cleaned.isBlank()) {")[-1][:200],
      "the branch exists but leads somewhere else")
check("only stop ends it", "watching = false" in reader, "no way out")
check("nothing else ends it", reader.count("watching = false") == 1,
      "a timeout would end the watch during the pause he stepped away for")
check("the loop guard still applies", "passagesRead.contains(normalisedForCompare(tail))" in reader,
      "a screen that re-renders identically would be read twice")
check("it polls slowly enough to hear", "WATCH_POLL_MS = 1_800L" in reader,
      "three words at a time is worse than not reading")

print(f"watch mode, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
