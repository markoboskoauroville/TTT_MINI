#!/usr/bin/env python3
"""
Test 1 for the Matrix effect and the reading gestures.

An animation cannot be tested by watching it, so the rule underneath it is pure and is walked here:
given a word, a seed and how far the voice has got, what does each cell show.

    python3 scripts/test_matrix.py
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "app/src/main/kotlin/dev/patrickgold/florisboard"

failures: list[str] = []
checks = 0
ROWS, WORD_ROW = 7, 3


def check(name: str, ok: bool, detail: str = "") -> None:
    global checks
    checks += 1
    if not ok:
        failures.append(f"{name}: {detail}")


def settled_count(word, progress):
    """The port of MaMatrix.settledCount."""
    if not word:
        return 0
    p = min(max(progress, 0.0), 1.0)
    return min(max(int(len(word) * p + 1.0), 0), len(word))


# ---------------------------------------------------------------- the resolve
w = "keyboard"
check("nothing spoken still shows the first letter", settled_count(w, 0.0) == 1, settled_count(w, 0.0))
check("finished means all of it", settled_count(w, 1.0) == len(w), settled_count(w, 1.0))
check("half way is about half", 4 <= settled_count(w, 0.5) <= 5, settled_count(w, 0.5))
check("an empty word settles nothing", settled_count("", 0.5) == 0)

# It must run AHEAD of the voice, never behind: a letter arriving as it is spoken is too late to read.
walked = 0
for n in range(1, 40):
    word = "x" * n
    for step in range(0, 101):
        p = step / 100
        s = settled_count(word, p)
        walked += 1
        spoken = int(n * p)
        check(f"len={n} p={p:.2f}: never behind the voice", s >= spoken, f"{s} < {spoken}")
        check(f"len={n} p={p:.2f}: never past the end", s <= n, f"{s} > {n}")
# And monotonic: a letter that has settled never goes back to noise.
for n in (1, 5, 12, 30):
    word = "x" * n
    seq = [settled_count(word, i / 100) for i in range(101)]
    check(f"len={n}: settling never reverses", seq == sorted(seq), str(seq[:12]))

# ---------------------------------------------------------------- the wiring
def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


matrix = code(SRC / "dictate/ui/MaMatrix.kt")
check("the rule is pure", "import android" not in matrix and "compose" not in matrix,
      "cannot be walked without a phone")
check("the word sits in the middle row", "WORD_ROW = ROWS / 2" in matrix, "the eye has to hunt for it")
check("noise and voice have separate clocks", "frame: Int" in matrix and "progress: Float" in matrix,
      "the background would speed up with the speaking rate")

caption = code(SRC / "dictate/ui/MaSubtitleRow.kt")
effects = code(SRC / "dictate/ui/MaReaderEffects.kt")
check("matrix is offered", 'Effect("matrix"' in effects, "not in the list")
check("anagram is gone from the list", "anagram" not in effects.lower(), "still offered")
for f in (SRC / "dictate/ui").glob("*.kt"):
    check(f"no anagram left in {f.name}", "MaAnagram" not in f.read_text(), "the deleted effect survives")
check("its file is gone", not (SRC / "dictate/ui/MaAnagram.kt").exists(), "still on disk")

# And the effect is actually DRAWN, not merely offered.
#
# It was in the list for one commit with nothing rendering it: picking it would have shown the plain
# highlight and looked like a setting that does nothing. **A style in the menu and no branch drawing
# it is worse than an absent feature** — the absent one is honest.
# ---------------------------------------------------------------- the window does not blink
#
# Chunked fetching made this visible: between one chunk ending and the next starting there is a
# moment with no current word, and the whole box used to vanish and come back. Several times a
# passage. **The distracting thing was never the empty bar, it was the appearing and disappearing.**
check("only idle hides the window", "MaReader.state == MaReader.State.IDLE) return" in caption,
      "still leaves on a missing index, which is a blink")
check("no bare index guard survives", "if (index < 0) return" not in caption, "the old blink is back")
check("a missing page draws an empty box", "SubtitleBox(modifier = modifier, full = full) { }" in caption,
      "returns instead, and the box disappears mid-passage")

# ---------------------------------------------------------------- top line, promoted out
#
# The "top" EFFECT is gone, and these checks go with it rather than being deleted quietly. Where the
# reading sits turned out to be a question worth asking of every effect, so it became `maReaderAlign`
# — see test_reader_align.py, which asserts the replacement. Two ways to put the reading at the top
# would have been the thing this file usually complains about.
effects_src = code(SRC / "dictate/ui/MaReaderEffects.kt")
check("the top-line effect is gone", 'Effect("top"' not in effects_src, "two controls for one job")
check("and nothing still draws it", 'style == "top"' not in caption, "a branch for a style nobody can pick")

check("matrix has a branch that draws it", 'style == "matrix"' in caption, "offered but never drawn")
check("it uses the shared window", "SubtitleBox(modifier = modifier, full = full)" in caption,
      "a second kind of window means a second copy of every gesture")

# ---------------------------------------------------------------- the gestures
check("both sides kill", caption.count("onKill()") == 1 and "abs(x) > SWIPE_MIN -> onKill()" in caption,
      "one side does something else, and a mis-aimed kill would not kill")
check("down is next", "y > 0f -> onNext()" in caption, "down does not go forward")
check("pinch zooms", "detectTransformGestures" in caption, "no way in or out of full screen by pinch")
check("a resting thumb is not a swipe", "SWIPE_MIN = 24f" in caption, "too twitchy to trust")

vol = code(SRC / "dictate/MaVolumeKeys.kt")
check("the volume keys let go of the reader", "MaReader.skipSentence()" not in vol, "still steering it")
check("and pass through while reading", "if (MaReader.currentIndex >= 0) return false" in vol,
      "cannot change the volume of the voice")

print(f"matrix and gestures, test 1: {checks} checks, {len(failures)} failed ({walked} points walked)")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
