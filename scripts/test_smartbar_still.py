#!/usr/bin/env python3
"""
Test 1 for the smartbar holding still while deleting.

He held backspace and the bar expanded and collapsed several times a second, taking the text above it
with it. His words: it makes him dizzy.

    python3 scripts/test_smartbar_still.py
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


def decide(candidates, selection, delete_held):
    """The port: does the bar change state on this keystroke?"""
    if delete_held:
        return None          # unchanged, whatever the candidates say
    return (not candidates) or selection


# ---------------------------------------------------------------- the bug, as a sequence
#
# Deleting "hello world" one character at a time: suggestions appear and vanish as words break.
# Without the guard the bar changes on almost every frame; with it, not once.
frames = [["hello"], [], ["worl"], [], ["wor"], [], ["wo"], []]
without = [decide(c, False, False) for c in frames]
changes_without = sum(1 for a, b in zip(without, without[1:]) if a != b)
check("without the guard the bar thrashes", changes_without >= 6, str(without))

with_guard = [decide(c, False, True) for c in frames]
check("with the guard it never changes", all(v is None for v in with_guard), str(with_guard))
check("and that is every frame of the hold", len(with_guard) == len(frames))

# ---------------------------------------------------------------- what must still work
check("released, an empty list expands", decide([], False, False) is True)
check("released, a suggestion collapses", decide(["a"], False, False) is False)
check("a selection expands even with candidates", decide(["a"], True, False) is True,
      "selecting text is when he wants the actions")
check("the guard does not outlive the press", decide([], False, False) is not None,
      "the bar would freeze for ever after one delete")


def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


nlp = code(SRC / "ime/nlp/NlpManager.kt")
fn = nlp[nlp.index("fun autoExpandCollapseSmartbarActions"):]
fn = fn[:fn.index("fun addToDebugOverlay")]
# find/index guarded, because the sabotage run must FAIL, not raise. Removing the guard made the
# split and the index throw, the count never printed and every other result was lost — §173 exactly,
# in a test written the same afternoon it is documented in. **A check that raises is not a check that
# fails**, and the sabotage is the only thing that ever proves the difference.
_del = fn.find("isPressed(KeyCode.DELETE)")
_fwd = fn.find("isPressed(KeyCode.FORWARD_DELETE)")
_exp = fn.find("val isExpanded")
check("the guard is in the function", _del >= 0, "the bar still thrashes")
check("forward delete too", _fwd >= 0, "one of the two keys is unguarded")
check("it returns rather than smoothing", _del >= 0 and "return" in fn[_del:_del + 200],
      "a delay still moves the bar, only later")
check("it is checked BEFORE the state is computed", 0 <= _del < _exp,
      "computing then discarding still writes the preference")

# The strip keeps its height when empty — the other half of not moving, fixed earlier.
bar = code(SRC / "ime/smartbar/Smartbar.kt")
check("an empty bucket strip keeps its slot", "maBucketStripHasContent()" in bar,
      "the strip would collapse and the text above would jump")

print(f"smartbar still, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
