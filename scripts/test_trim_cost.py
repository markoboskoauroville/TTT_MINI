#!/usr/bin/env python3
"""
Test 1 for the silence trim: what it saves, and what it must never cut.

Billed seconds are the bill, so this is arithmetic and can be walked. The rule: a gap longer than
MAX is reduced to KEEP; anything shorter is left alone; **no speech is ever removed.**

    python3 scripts/test_trim_cost.py
"""

import itertools
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "app/src/main/kotlin/dev/patrickgold/florisboard"

failures: list[str] = []
checks = 0
MAX_MS, KEEP_MS = 700, 250


def check(name: str, ok: bool, detail: str = "") -> None:
    global checks
    checks += 1
    if not ok:
        failures.append(f"{name}: {detail}")


def trim(segments):
    """segments: list of ('speech'|'gap', ms). Returns the uploaded timeline."""
    out = []
    for kind, ms in segments:
        if kind == "speech":
            out.append((kind, ms))
        else:
            out.append((kind, KEEP_MS if ms > MAX_MS else ms))
    return out


def total(segments):
    return sum(ms for _, ms in segments)


def speech_of(segments):
    return sum(ms for k, ms in segments if k == "speech")


# ---------------------------------------------------------------- the rule
check("a long pause is cut to KEEP", trim([("gap", 3000)]) == [("gap", 250)])
check("a pause at the threshold is left", trim([("gap", 700)]) == [("gap", 700)],
      "cutting at exactly MAX would make the boundary arbitrary")
check("a short pause is untouched", trim([("gap", 400)]) == [("gap", 400)])
check("speech is never touched", trim([("speech", 50)]) == [("speech", 50)])

# ---------------------------------------------------------------- the invariant that protects words
#
# Walked over every arrangement of speech and gaps up to six segments: the amount of SPEECH in the
# upload must equal the amount in the recording, always. Everything else is negotiable; this is not.
walked = 0
for n in range(1, 7):
    for kinds in itertools.product(["speech", "gap"], repeat=n):
        for lengths in ((200,) * n, (900,) * n, (3000,) * n, tuple(300 * (i + 1) for i in range(n))):
            segs = list(zip(kinds, lengths))
            out = trim(segs)
            walked += 1
            check(f"{segs}: no speech lost", speech_of(out) == speech_of(segs), str(out))
            check(f"{segs}: never longer than the original", total(out) <= total(segs), str(out))
            check(f"{segs}: same number of segments", len(out) == len(segs), "a segment vanished")

# ---------------------------------------------------------------- what it is worth
#
# A realistic dictation: a sentence, a think, a sentence. The old thresholds are the comparison,
# because the change is only worth making if the difference is real.
def trim_with(segments, mx, keep):
    return [(k, (keep if ms > mx else ms)) if k == "gap" else (k, ms) for k, ms in segments]


dictation = [("speech", 2500), ("gap", 1200), ("speech", 3000), ("gap", 800),
             ("speech", 2000), ("gap", 1500), ("speech", 2500)]
raw = total(dictation)
old_way = total(trim_with(dictation, 2000, 400))
new_way = total(trim_with(dictation, MAX_MS, KEEP_MS))
check("the old thresholds saved nothing here", old_way == raw,
      f"old={old_way} raw={raw} — every pause was under two seconds")
check("the new ones save real time", new_way < raw * 0.9, f"new={new_way} raw={raw}")
check("and still lose no speech", speech_of(trim_with(dictation, MAX_MS, KEEP_MS)) == speech_of(dictation))

# ---------------------------------------------------------------- the wiring
def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


ctrl = code(SRC / "dictate/DictateController.kt")
check("the threshold is the tightened one", f"TRIM_MAX_SILENCE_MS = {MAX_MS}" in ctrl, "back to two seconds")
check("padding is kept", f"TRIM_KEEP_SILENCE_MS = {KEEP_MS}" in ctrl, "a hard cut mid-breath")
check("padding is never zero", "TRIM_KEEP_SILENCE_MS = 0" not in ctrl, "clipped first syllables")
check("trimming is on by default", "trim_silent_gaps" in code(SRC / "app/AppPrefs.kt"), "no setting at all")
check("the saving is logged", "trimmed $cut% of the audio before upload" in ctrl,
      "a saving nobody can see is a saving nobody can trust")
check("the log cannot divide by zero", "if (before > 0L)" in ctrl, "a crash in the send path")

print(f"trim cost, test 1: {checks} checks, {len(failures)} failed ({walked} timelines walked)")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
