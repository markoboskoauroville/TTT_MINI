#!/usr/bin/env python3
"""
Test 1 for build 265: the mechanism, alone, before any build.

Three claims, each checked where it can be checked without a phone.

    python3 scripts/test_buckets_and_permissions.py
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
    text = path.read_text()
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"^\s*//.*$", "", text, flags=re.M)


# ---------------------------------------------------------------- the capture rule
#
# The buckets, as a pure function of what is on the row. This is the actual logic Marko asked for
# — "available when it's on screen, disabled when it's not" — so it is modelled and walked here,
# not merely grepped for.

CAPACITY = 10


def capture(slots, text, visible):
    """A faithful port of MaClipCapture.capture."""
    if not text.strip():
        return slots
    if any(s == text for s in slots):
        return slots
    free = next((i for i in sorted(visible) if slots[i - 1] is None), None)
    if free is None:
        return slots
    out = list(slots)
    out[free - 1] = text
    return out


empty = [None] * CAPACITY

# The case it is FOR: buckets on the row, copies land in order, A1 -> C1, A2 -> C2, A3 -> C3.
slots = empty
for n, block in enumerate(["code one", "code two", "code three"], start=1):
    slots = capture(slots, block, visible={1, 2, 3, 4, 5})
    check(f"A{n} fills bucket {n}", slots[n - 1] == block, f"landed in {slots}")

# The case it must REFUSE: no C keys on the row means no capture, and no crash.
check("no buckets on the row captures nothing", capture(empty, "x", visible=set()) == empty)

# Boundaries: full stops capturing, blank burns nothing, a repeat burns nothing.
full = ["a", "b", None] + [None] * 7
check("full visible set stops capturing", capture(["a", "b"] + [None] * 8, "c", {1, 2}) == ["a", "b"] + [None] * 8)
check("blank is refused", capture(empty, "   ", {1, 2}) == empty)
check("a repeat is refused", capture(["a"] + [None] * 9, "a", {1, 2}) == ["a"] + [None] * 9)

# A gap in the visible set: buckets 1 and 5 on the row, 5 fills only after 1 is taken.
check("lowest visible bucket first", capture(empty, "x", {5, 1})[0] == "x")
check("skips a bucket that is not on the row", capture(["a"] + [None] * 9, "x", {5, 1})[4] == "x")

# Which bucket the tick goes on: the diff between the two lists, the same way the app finds it.
before = ["a", None, None] + [None] * 7
after = capture(before, "x", {1, 2, 3})
filled = next((i for i in range(CAPACITY) if after[i] is not None and before[i] is None), None)
check("the tick lands on the bucket that changed", filled == 1, f"got {filled}")

# ---------------------------------------------------------------- the wiring
clipmgr = code(SRC / "ime/clipboard/ClipboardManager.kt")
check("capture is not gated on a preference", "maBucketsEnabled" not in clipmgr, "the switch is still in the path")
check("the fill is recorded", "MaClipCapture.noteFilled" in clipmgr, "nothing sets the tick")

row = code(SRC / "dictate/ui/MaFeatureRow.kt")
check("the C key reads no switch", "bucketsOn" not in row, "the key still consults a preference")
check("the C key wears the tick", "FILL_MARK_MS" in row, "the mark is never drawn")

capture_kt = code(SRC / "dictate/MaClipCapture.kt")
check("the mark lasts a minute", "FILL_MARK_MS = 60_000L" in capture_kt, "wrong duration")

prefs = code(SRC / "app/AppPrefs.kt")
check("maBucketsEnabled is gone", "maBucketsEnabled" not in prefs, "still declared")
for path in SRC.rglob("*.kt"):
    check(f"no reader of maBucketsEnabled in {path.name}", "maBucketsEnabled" not in code(path), "still read")

# ---------------------------------------------------------------- the permissions screen
perms = code(SRC / "app/settings/dictate/MaPermissionsScreen.kt")
check("keyboard detection asks the input method manager", "enabledInputMethodList" in perms, "single-source again")
check("keyboard detection has the default-IME fallback", "DEFAULT_INPUT_METHOD" in perms, "only two sources")
check(
    "each source is wrapped on its own",
    perms.count("runCatching {") >= 6,
    "one failure could still discard the others",
)
# Only inside the steps list. The API keys row below it is deliberately never ticked — a key can be
# present and dead — and a check that could not tell the two apart would have to be argued with every
# time it fired, which is how a check stops being read.
steps_block = perms[perms.index("private fun maPermissionSteps"):]
check(
    "restricted settings is inferred, not hardcoded false",
    "granted = false," not in steps_block,
    "a permission row that can never tick",
)
check(
    "restricted settings reads the accessibility service",
    "DictateAccessibilityService.isRunning" in steps_block,
    "nothing is inferred from the consequence",
)

# ---------------------------------------------------------------- the recording readout
meter = code(SRC / "dictate/ui/MaRecordMeter.kt")
check("the size says MB", '"%.1f MB"' in meter, "still three bare characters")
check("the size cannot wrap", "softWrap = false" in meter, "it can still break onto two lines")
# Guarded rather than indexed straight. The first run of this check THREW when the MB text was
# removed on purpose, and a check that raises is not a check that fails: the count never prints, the
# other results are lost, and the exit code is right by accident.
size_at = meter.find('"%.1f MB"')
weight_at = meter.rfind("Spacer(modifier = Modifier.weight(1f))")
check(
    "the readings start at the left",
    size_at >= 0 and weight_at > size_at,
    "the weight is not after the readings, or the MB text is missing",
)

print(f"buckets and permissions, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
