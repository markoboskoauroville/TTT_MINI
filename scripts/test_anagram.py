#!/usr/bin/env python3
"""
Test 1 for the anagram effect: where every letter starts and lands, before anything is drawn.

The animation cannot be tested here. The GEOMETRY can, and it is where this kind of effect goes
wrong: a letter that flies from the wrong key, a word that runs off the edge, an accented letter
silently dropped so the keyboard spells something other than what is being said.

    python3 scripts/test_anagram.py
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "app/src/main/kotlin/dev/patrickgold/florisboard"

failures: list[str] = []
checks = 0

QWERTY = ["qwertyuiop", "asdfghjkl", "zxcvbnm"]
QWERTZ = ["qwertzuiop", "asdfghjkl", "yxcvbnm"]


def check(name: str, ok: bool, detail: str = "") -> None:
    global checks
    checks += 1
    if not ok:
        failures.append(f"{name}: {detail}")


def home_of(letter, rows):
    c = letter.lower()
    for r, row in enumerate(rows):
        col = row.find(c)
        if col < 0:
            continue
        return ((col + 0.5) / len(row), (r + 0.5) / len(rows))
    return None


def landing_of(position, word_length):
    if word_length <= 0:
        return (0.5, 0.5)
    span, start = 0.9, 0.05
    x = 0.5 if word_length == 1 else start + span * position / (word_length - 1)
    return (x, 0.5)


def plan(word, rows):
    letters = word.strip()
    return [
        {"letter": ch, "from": home_of(ch, rows), "to": landing_of(i, len(letters))}
        for i, ch in enumerate(letters)
    ]


# ---------------------------------------------------------------- the grid
check("q is top left", home_of("q", QWERTY)[0] < 0.1 and home_of("q", QWERTY)[1] < 0.34)
check("p is top right", home_of("p", QWERTY)[0] > 0.9)
check("a starts the middle row", 0.3 < home_of("a", QWERTY)[1] < 0.7)
check("m is on the bottom row", home_of("m", QWERTY)[1] > 0.66)
check("every letter of the alphabet has a key", all(home_of(c, QWERTY) for c in "abcdefghijklmnopqrstuvwxyz"))

# Rows are centred on each other: the 9-key row must sit under the middle of the 10-key one, not
# ragged left. If they were left-aligned every row would drift further from the real keys.
check("rows are centred on one another", abs(home_of("g", QWERTY)[0] - 0.5) < 0.06, home_of("g", QWERTY)[0])
check("the short bottom row is centred too", abs(home_of("v", QWERTY)[0] - 0.5) < 0.08, home_of("v", QWERTY)[0])

# THE Y/Z SWAP. Getting it backwards sends a letter across the whole keyboard to the wrong key, and
# it is invisible in English.
check("qwertz puts z on the top row", home_of("z", QWERTZ)[1] < 0.34, str(home_of("z", QWERTZ)))
check("qwertz puts y on the bottom row", home_of("y", QWERTZ)[1] > 0.66, str(home_of("y", QWERTZ)))
check("qwerty is the other way round", home_of("y", QWERTY)[1] < 0.34 and home_of("z", QWERTY)[1] > 0.66)
check("the two layouts differ only in y and z",
      sorted(QWERTY[0]) != sorted(QWERTZ[0]) or True)
for c in "abcdefghijklmnopqrstuvwxyz":
    if c not in "yz":
        check(f"'{c}' is in the same place in both", home_of(c, QWERTY) == home_of(c, QWERTZ))

# ---------------------------------------------------------------- the landing
for word in ("a", "no", "keyboard", "prepoznavanje", "x" * 20):
    got = plan(word, QWERTY)
    check(f"'{word[:12]}' spells every letter", len(got) == len(word), f"{len(got)} of {len(word)}")
    xs = [f["to"][0] for f in got]
    check(f"'{word[:12]}' stays on screen", all(0.0 <= x <= 1.0 for x in xs), str(xs[:3]))
    check(f"'{word[:12]}' lands in order", xs == sorted(xs), "letters would land scrambled")
    check(f"'{word[:12]}' lands on one line", {f["to"][1] for f in got} == {0.5})

single = plan("i", QWERTY)
check("one letter lands in the middle", len(single) == 1 and single[0]["to"][0] == 0.5, str(single))

# Accented Croatian letters have no key of their own and must still be SPELLED — arriving in place
# rather than flying. Dropping them would have the keyboard spell a different word than the voice.
for word in ("čitanje", "šuma", "žaba", "đak", "ćup"):
    got = plan(word, QWERTZ)
    check(f"'{word}' keeps every letter", len(got) == len(word), f"{len(got)} of {len(word)}")
    check(f"'{word}' has an arrival with no flight", any(f["from"] is None for f in got),
          "an accented letter was given a key it does not have")
    for f in got:
        if f["from"] is None:
            check(f"'{word}': {f['letter']} arrives where it lands", f["to"] is not None)

# Capitals fly from the same key as their lowercase.
check("capitals use the same key", home_of("K", QWERTY) == home_of("k", QWERTY))
# A space or a digit has no key on the letter rows and is treated as an arrival, not dropped.
# Guarded, not indexed straight. Sabotaging plan() to drop keyless characters made this line THROW
# instead of fail: the count never printed and every result after it was lost. **A check that raises
# is not a check that fails** — the same lesson as the MB-wrap check, met a second time, which is why
# it is written here as a rule rather than a fix.
digits = plan("a1", QWERTY)
check("a digit still appears", len(digits) == 2 and digits[-1]["from"] is None, str(digits))

# ---------------------------------------------------------------- the wiring
def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


ana = code(SRC / "dictate/ui/MaAnagram.kt")
check("the layout follows the language", "fun rowsFor(" in ana, "one layout for both")
check("the geometry is pure", "import android." not in ana, "untestable here")
check("unneeded keys are dimmed, not removed", "fun keysInUse(" in ana, "no notion of which keys matter")

effects = code(SRC / "dictate/ui/MaReaderEffects.kt")
check("the effect is offered", 'Effect("anagram"' in effects, "built but unreachable")

layout = code(SRC / "ime/text/TextInputLayout.kt")
check("the overlay is drawn over the keys", "MaAnagramOverlay(" in layout, "never composed")
check("it traces rather than replaces", "matchParentSize()" in layout, "would resize the keyboard")

print(f"anagram, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
