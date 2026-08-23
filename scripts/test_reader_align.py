#!/usr/bin/env python3
"""
Test 1 for the slideshow hold, the alignment and the abbreviations.

    python3 scripts/test_reader_align.py
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


# ---------------------------------------------------------------- the hold, as a state machine
#
# Walked, because the failure is invisible in code review: it only shows as a flicker on a phone.
def run(sequence):
    """Feeds a sequence of live pages (None = nothing current) and returns what is SHOWN each step."""
    held = None
    shown = []
    for live in sequence:
        if live is not None:
            held = live
        shown.append(live if live is not None else held)
    return shown


check("a gap holds the previous page", run(["a", None, "b"]) == ["a", "a", "b"], str(run(["a", None, "b"])))
check("a long gap holds it all the way", run(["a", None, None, None, "b"]) == ["a", "a", "a", "a", "b"])
check("nothing before the first page", run([None, None, "a"]) == [None, None, "a"],
      "showed something that had never arrived")
check("the change is the only event", run(["a", "a", None, "b"]) == ["a", "a", "a", "b"])

# NEVER BLANK MID-PASSAGE: once anything has been shown, nothing after it is blank.
walked = 0
import itertools
for n in range(1, 9):
    for pattern in itertools.product(["a", None], repeat=n):
        shown = run(list(pattern))
        walked += 1
        seen = False
        for cell in shown:
            if cell is not None:
                seen = True
            elif seen:
                check(f"{pattern}: never blank after the first page", False, str(shown))
                break
        else:
            check(f"{pattern}: never blank after the first page", True)

# ---------------------------------------------------------------- the wiring
def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


caption = code(SRC / "dictate/ui/MaSubtitleRow.kt")
check("the last page is held", "val page = live ?: held" in caption, "the text still empties between chunks")
check("the hold resets per passage", "remember(words) { mutableStateOf<Page?>(null) }" in caption,
      "the previous reading's last sentence would survive into the next")
check("alignment is applied", "contentAlignment = when (alignPref)" in caption, "the setting does nothing")
for value in ("middle", "bottom"):
    check(f"{value} is handled", f'"{value}" ->' in caption, "falls through to top")
check("no animation between places", "animate" not in caption.lower(),
      "movement on screen while he is listening")

effects = code(SRC / "dictate/ui/MaReaderEffects.kt")
check("every effect has a short form", effects.count(", \"") >= 6, "a chip with no abbreviation")
for long, short in (("Highlight", "Hi"), ("Typewriter", "Ty"), ("Karaoke", "Kar"),
                    ("Spotlight", "Sp"), ("Void", "Vo"), ("Matrix", "Mx")):
    check(f"{long} shortens to {short}", f'"{long}", "{short}"' in effects, "wrong or missing abbreviation")
    # An abbreviation, not a different name.
    check(f"{short} is a prefix of {long}", long.lower().startswith(short.lower().rstrip()) or short == "Mx",
          "the chip and the settings would name it differently")
check("the top-line effect is gone", '"top", "Top line"' not in effects,
      "two ways to put the reading at the top")

dash = code(SRC / "dictate/ui/MaReaderDashboard.kt")
check("the chips show the short form", "text = effect.short" in dash, "still the long labels")
check("the alignment control is on the panel", "maReaderAlign.set(option.first)" in dash, "no way to change it")

print(f"reader align, test 1: {checks} checks, {len(failures)} failed ({walked} sequences walked)")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
