#!/usr/bin/env python3
"""
Test 1 for the sentence memory and the waiting cues.

He scrolled up while watching and heard old text again. The passage guard could not catch it: half a
screen he has heard plus half he has not is a passage nobody has seen. **The unit of memory has to be
the unit of speech.**

    python3 scripts/test_read_memory.py
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "app/src/main/kotlin/dev/patrickgold/florisboard"

failures: list[str] = []
checks = 0
CUES = ["thought process", "still working on it", "identifying each image", "running command"]


def check(name: str, ok: bool, detail: str = "") -> None:
    global checks
    checks += 1
    if not ok:
        failures.append(f"{name}: {detail}")


def norm(t):
    return " ".join("".join(c if c.isalpha() else " " for c in t.lower()).split())


def is_cue(line):
    n = norm(line)
    return bool(n) and any(c in n for c in CUES)


class Memory:
    def __init__(self):
        self.spoken = set()

    def unheard(self, sentences):
        return [s for s in sentences if norm(s) and not is_cue(s) and norm(s) not in self.spoken]

    def remember(self, sentences):
        for s in sentences:
            if norm(s):
                self.spoken.add(norm(s))


# ---------------------------------------------------------------- the scroll-up he reported
m = Memory()
first = ["One.", "Two.", "Three."]
m.remember(m.unheard(first))
check("the first screen is all new", len(first) == 3)

# He scrolls up: an overlapping window, older text above and one line he has not heard.
scrolled = ["Zero.", "One.", "Two."]
new = m.unheard(scrolled)
check("only the unheard line is read", new == ["Zero."], str(new))
check("the heard ones are silent", "One." not in new and "Two." not in new, str(new))

m.remember(new)
check("scrolling over it again says nothing", m.unheard(scrolled) == [], str(m.unheard(scrolled)))

# The whole screen again, unchanged: silence, not a re-read.
check("the same screen twice is silence", m.unheard(first) == [], str(m.unheard(first)))

# ---------------------------------------------------------------- the waiting cues
for cue in ("Thought process", "Still working on it...", "Identifying each image in the sequence.",
            "Running command"):
    check(f"cue skipped: {cue!r}", is_cue(cue), "it would be read aloud")
for real in ("The thought behind the process was sound.", "I am still working out the geometry.",
             "Identify the door in shot four."):
    check(f"not a cue: {real!r}", not is_cue(real), "a sentence he wanted would be silenced")

m2 = Memory()
mixed = ["Thought process", "Here is the real answer.", "Still working on it..."]
check("cues are dropped, the answer is kept", m2.unheard(mixed) == ["Here is the real answer."],
      str(m2.unheard(mixed)))
check("a screen of only cues reads nothing", m2.unheard(["Thought process", "Running command"]) == [])

# A cue must never be REMEMBERED as spoken either — otherwise a sentence that legitimately contains
# those words later would be silenced for the wrong reason.
m3 = Memory()
m3.remember(m3.unheard(["Thought process"]))
check("cues are not remembered", len(m3.spoken) == 0, str(m3.spoken))

# ---------------------------------------------------------------- normalisation
check("wrapping does not make a sentence new", norm("one two\nthree") == norm("one  two three"))
check("a ticking clock does not either", norm("done 12:00") == norm("done 12:01"))
check("different words still differ", norm("the cat") != norm("the dog"))


def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


mem = code(SRC / "dictate/MaReadMemory.kt")
reader = code(SRC / "dictate/MaReader.kt")
check("the memory exists", "object MaReadMemory" in mem, "still passage-level only")
check("the reader filters before speaking", "val fresh = MaReadMemory.unheard(" in reader,
      "it would synthesise everything on screen")
check("it synthesises the FILTERED sentences", "val sentences = fresh" in reader,
      "the filter would decide and the synthesis would ignore it")
check("nothing new goes back to waiting", 'endOfText(context, onMessage, "nothing new to read")' in reader,
      "a screen of cues would end the watch")
check("the memory is cleared on stop", "MaReadMemory.clear()" in reader,
      "pressing read again on the same screen would be met with silence")
check("the cue list is short", mem.count('        "') <= 6,
      "every entry is a phrase this app refuses to read")

print(f"read memory, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
