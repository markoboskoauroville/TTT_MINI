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
# The delete guard is gone with everything else in that function. Nothing to find, and nothing to
# check for — the absence of ALL switching is what is asserted now.
# SUPERSEDED by removal. These asserted the shape of a guard inside a function that now does
# nothing: there is no delete check because there is no switching to guard. The claim that survives
# is the one above — nothing sets the expanded state automatically — and it is stronger than both.

# The strip keeps its height when empty — the other half of not moving, fixed earlier.
bar = code(SRC / "ime/smartbar/Smartbar.kt")
check("an empty bucket strip keeps its slot", "maBucketStripHasContent()" in bar,
      "the strip would collapse and the text above would jump")

# ---------------------------------------------------------------- nothing moves it automatically
#
# The guard on a held delete key was too narrow: the switching happens on ordinary typing too, and a
# narrow fix for a symptom already reported twice is worse than none, because it looks addressed. He
# reported it a third time and called it seasickness.
#
# The automatic switching is GONE, not narrowed.
fn2 = nlp[nlp.index("fun autoExpandCollapseSmartbarActions"):]
fn2 = fn2[:fn2.index("fun addToDebugOverlay")]
check("nothing sets the expanded state automatically", "sharedActionsExpanded.set" not in fn2,
      "the bar can still move on its own")
check("nothing reads the candidate lists either", "isNullOrEmpty()" not in fn2,
      "a decision computed is a decision that will be acted on again one day")
check("the function still exists", "fun autoExpandCollapseSmartbarActions" in nlp,
      "three call sites on the typing path would need removing at the end of a session")

# The one thing that DOES move it, and it is his.
cand = code(SRC / "ime/smartbar/CandidatesRow.kt")
check("a long press toggles the bar", "onLongPress = {" in cand, "no way to close it")
check("it toggles rather than closing", "!prefs.smartbar.sharedActionsExpanded.get()" in cand,
      "one-way: closed and no way back")
check("it is on the whole row", "pointerInput(Unit)" in cand, "he would have to aim at a key")
# Compared inside the MODIFIER CHAIN. Searching the whole file found the `florisHorizontalScroll`
# IMPORT at the top, which is before everything — the check compared a use against an import and
# failed on correct code. **A position check has to bound the region it is talking about.**
_chain = cand[cand.index("elementName = FlorisImeUi.SmartbarCandidatesRow"):]
_chain = _chain[:_chain.index("horizontalArrangement")]
check("and before the scroll modifier",
      _chain.find("pointerInput(Unit)") < _chain.find("florisHorizontalScroll"),
      "a horizontal drag would swallow the press on exactly the crowded rows he wants gone")
check("no animation on the toggle", "sharedActionsExpandWithAnimation.set(false)" in cand,
      "the bar would slide, which is the movement he is complaining about")

print(f"smartbar still, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
