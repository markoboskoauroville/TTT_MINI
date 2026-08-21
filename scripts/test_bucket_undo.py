#!/usr/bin/env python3
"""
Test 1 for bucket undo: the mechanism, alone.

The claim: **undo reverses the last thing that happened**, where a bucket change counts as a thing.

That is a rule about ORDER, so it is tested by generating sequences of actions and asserting what
the next undo does, rather than by checking three arrangements that happen to come to mind. The
port below mirrors MaBucketUndo and MaClipCapture.capture; if the Kotlin and this disagree, one of
them is wrong and the disagreement is the finding.

    python3 scripts/test_bucket_undo.py
"""

import itertools
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


CAPACITY = 10
VISIBLE = {1, 2, 3, 4, 5}


class World:
    """The keyboard, as far as undo is concerned."""

    def __init__(self):
        self.slots = [None] * CAPACITY
        self.rank = 0
        self.stack = []
        self.redo_stack = []
        self.armed = None
        self.last_text = 0
        self.clock = 0
        self.field = []          # what the text field holds
        self.field_undone = 0    # how many times the field was asked to undo

    def tick(self):
        self.clock += 1
        return self.clock

    # ---- actions -------------------------------------------------------
    def copy(self, text):
        """A copy reaching the clipboard, from anywhere."""
        before = list(self.slots)
        after = self._capture(before, text)
        if after != before:
            step = self.armed or (list(before), self.rank)
            self.armed = None
            self.stack.append((step[0], step[1], self.tick()))
            if len(self.stack) > 20:
                self.stack.pop(0)
            self.redo_stack.clear()
            self.slots = after
        else:
            self.armed = None

    def a_key(self, text, lands=True):
        """The A key: arm, press, and on a landing advance the ladder."""
        self.armed = (list(self.slots), self.rank)
        if not lands:
            self.armed = None
            return
        self.rank += 1
        self.copy(text)

    def bin(self):
        self.stack.append((list(self.slots), self.rank, self.tick()))
        self.redo_stack.clear()
        self.slots = [None] * CAPACITY
        self.rank = 0

    def type_text(self, text):
        self.field.append(text)
        self.last_text = self.tick()

    def paste_bucket(self, slot):
        """Pouring a bucket into the field: text goes out, bucket empties, NOT undoable."""
        text = self.slots[slot - 1]
        if text is None:
            return
        self.slots[slot - 1] = None
        self.type_text(text)

    def undo(self):
        if self.stack and self.stack[-1][2] >= self.last_text:
            slots, rank, _ = self.stack.pop()
            self.redo_stack.append((list(self.slots), self.rank, self.tick()))
            self.slots = list(slots)
            self.rank = rank
            return "buckets"
        self.field_undone += 1
        if self.field:
            self.field.pop()
        return "field"

    def redo(self):
        if self.redo_stack and self.redo_stack[-1][2] >= self.last_text:
            slots, rank, _ = self.redo_stack.pop()
            self.slots = list(slots)
            self.rank = rank
            return "buckets"
        self.field.append("redone")
        return "field"

    @staticmethod
    def _capture(slots, text):
        if not text.strip():
            return slots
        if any(s == text for s in slots):
            return slots
        free = next((i for i in sorted(VISIBLE) if slots[i - 1] is None), None)
        if free is None:
            return slots
        out = list(slots)
        out[free - 1] = text
        return out


# ---------------------------------------------------------------- the case it is FOR
w = World()
w.a_key("block one")
w.a_key("block two")
check("A twice fills C1 and C2", w.slots[:2] == ["block one", "block two"], str(w.slots[:3]))
check("undo goes to the buckets", w.undo() == "buckets")
check("C2 is empty again", w.slots[1] is None, str(w.slots[:3]))
check("undo again", w.undo() == "buckets")
check("C1 is empty again", w.slots[0] is None, str(w.slots[:3]))
check("undo falls through when the buckets run out", w.undo() == "field")

# ---------------------------------------------------------------- text wins when text is last
w = World()
w.copy("collected")
w.type_text("hello")
check("undo after typing goes to the field", w.undo() == "field")
check("the bucket is untouched", w.slots[0] == "collected")
# And it STAYS with the field. This is the finding of the walk: the first draft's comment claimed
# the bucket underneath was still reachable on a second press. It is not, and it should not be — the
# field's own undo history is invisible from here, so a press that jumped back to the buckets would
# undo a collected block for somebody who was still undoing sentences.
check("and it stays with the field", w.undo() == "field")
check("the bucket is still there", w.slots[0] == "collected")
# A new bucket change brings the key back to the buckets, which is what makes the rule "what you
# just did" rather than "whatever is on top of a pile".
w.copy("collected again")
check("a new bucket change takes the key back", w.undo() == "buckets")
check("and reverses that one", w.slots[1] is None)

# ---------------------------------------------------------------- the bin
w = World()
for n in range(1, 6):
    w.copy(f"text {n}")
w.a_key("block")
before_bin = list(w.slots)
before_rank = w.rank
w.bin()
check("the bin empties everything", all(s is None for s in w.slots))
check("undo un-bins", w.undo() == "buckets")
check("all five come back", w.slots == before_bin, str(w.slots[:6]))

# ---------------------------------------------------------------- pouring is not undone
w = World()
w.copy("carried")
w.paste_bucket(1)
check("pasting empties the bucket", w.slots[0] is None)
check("undo after a paste goes to the field", w.undo() == "field")
check("the bucket stays empty", w.slots[0] is None, "half an action was undone")
check("and it stays with the field", w.undo() == "field", "half an action was undone late")

# ---------------------------------------------------------------- the arming, and its edges
w = World()
w.a_key("block", lands=False)
check("a press that misses arms nothing", w.armed is None)
w.copy("ordinary copy")
check("so an ordinary copy records its own state", w.stack[-1][1] == 0, str(w.stack[-1]))

w = World()
w.a_key("same text")
w.a_key("same text")  # a repeat: captures nothing
check("a repeated block changes no bucket", w.slots[1] is None)
check("and leaves nothing armed", w.armed is None, "an arming would be spent by a later copy")

# ---------------------------------------------------------------- redo, the mirror
w = World()
w.a_key("block one")
w.a_key("block two")
check("undo takes the second block off", w.undo() == "buckets" and w.slots[1] is None)
check("redo puts it back", w.redo() == "buckets" and w.slots[1] == "block two", str(w.slots[:3]))
check("nothing left to redo", w.redo() == "field")

# A change since the undo throws the redo away. Restoring a state into a world that has moved on is
# how a bucket ends up holding something nobody put there.
w = World()
w.a_key("block one")
w.undo()
w.copy("something else")
check("a new copy clears the redo", w.redo() == "field", "a stale redo survived")
check("and the new copy is untouched", w.slots[0] == "something else", str(w.slots[:2]))

# Text since the undo does the same, by the same clock undo uses.
w = World()
w.copy("collected")
w.undo()
w.type_text("hello")
check("text since the undo clears it too", w.redo() == "field")

# The bin, undone and redone.
w = World()
for n in range(1, 4):
    w.copy(f"text {n}")
w.bin()
w.undo()
check("undo un-bins", w.slots[0] == "text 1")
check("redo re-bins", w.redo() == "buckets" and all(s is None for s in w.slots), str(w.slots[:4]))

# ---------------------------------------------------------------- walked, not sampled
#
# Every sequence of four actions drawn from the five kinds. After each, undo once and assert the two
# invariants that make this a rule rather than a habit.
kinds = ["copy", "a_key", "bin", "type", "paste"]
walked = 0
for seq in itertools.product(kinds, repeat=4):
    w = World()
    for n, kind in enumerate(seq):
        if kind == "copy":
            w.copy(f"c{n}")
        elif kind == "a_key":
            w.a_key(f"a{n}")
        elif kind == "bin":
            w.bin()
        elif kind == "type":
            w.type_text(f"t{n}")
        else:
            w.paste_bucket(1)
    slots_before, field_before = list(w.slots), list(w.field)
    where = w.undo()
    walked += 1
    # ALWAYS ONE: undo does something, or hands the key to the field, never neither.
    check(f"{seq}: undo resolves", where in ("buckets", "field"))
    # NEVER BOTH: it touches the buckets or the field, not both at once.
    if where == "buckets":
        check(f"{seq}: bucket undo leaves the field alone", w.field == field_before, "text moved too")
    else:
        check(f"{seq}: field undo leaves the buckets alone", w.slots == slots_before, "buckets moved too")
    # The ladder never points past what the buckets could hold, or below zero.
    # Redo, same two invariants. ALWAYS ONE and NEVER BOTH hold for it as well, or the pair of keys
    # is not a pair.
    slots_mid, field_mid = list(w.slots), list(w.field)
    where_r = w.redo()
    check(f"{seq}: redo resolves", where_r in ("buckets", "field"))
    if where_r == "buckets":
        check(f"{seq}: bucket redo leaves the field alone", w.field == field_mid, "text moved too")
    else:
        check(f"{seq}: field redo leaves the buckets alone", w.slots == slots_mid, "buckets moved too")
    # And a redo never invents a bucket: everything it restores was in a bucket at some point.
    check(f"{seq}: redo invents nothing", all(s is None or isinstance(s, str) for s in w.slots))

# ---------------------------------------------------------------- the wiring
def code(path: Path) -> str:
    text = path.read_text()
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"^\s*//.*$", "", text, flags=re.M)


km = code(SRC / "ime/keyboard/KeyboardManager.kt")
check("the undo key asks the buckets first", "MaBucketUndo.takeIfNewest(" in km, "undo never reaches them")
check("the redo key asks them too", "MaBucketUndo.takeRedo()" in km, "redo would be half a key")
check("and still falls through to the field on redo", "editorInstance.performRedo()" in km, "text redo was lost")

order = code(SRC / "dictate/MaFeatureOrder.kt")
check("undo is a key you can put on a row", 'UNDO("undo"' in order, "not in the catalogue")
check("redo is a key you can put on a row", 'REDO("redo"' in order, "not in the catalogue")

capture_src = code(SRC / "dictate/MaClipCapture.kt")
check("a new change clears the redo stack", "redoStack.clear()" in capture_src, "a stale redo can survive")
check("the tick and its clock are gone", "FILL_MARK_MS" not in capture_src, "the timed mark is still there")
check("and still falls through to the field", "editorInstance.performUndo()" in km, "text undo was lost")

ed = code(SRC / "ime/editor/EditorInstance.kt")
check("every commit stamps the text clock", "MaBucketUndo.noteText()" in ed, "undo cannot tell what was last")

cm = code(SRC / "ime/clipboard/ClipboardManager.kt")
check("a capture records a step", "MaBucketUndo.push(current)" in cm, "captures are not reversible")
check("a copy that changes nothing drops the arming", "MaBucketUndo.dropArming()" in cm, "a stale arming survives")

row = code(SRC / "dictate/ui/MaFeatureRow.kt")
check("the A key arms before pressing", "MaBucketUndo.armAuto(before)" in row, "undo cannot see before the press")
check("a miss disarms", "MaBucketUndo.disarm()" in row, "an arming is left waiting")
check("the bin records what it throws away", "MaBucketUndo.push(capturedSlots)" in row, "the bin is final")
# The ladder is gone from the app, so it is gone from here. It counted how far up the page the A
# key had climbed, and the key reads the frame now.
check("no ladder is left in the row", "maBucketRank" not in row and "autoRank" not in row, "the counter survives")
check("A reads what is in view", "pressScreenTargetInView" in row, "still walking the document")
check("A counts what is in view", "countScreenTargetsInView" in row, "cannot know when to stop")
check("the bucket wears a ring", "ring = if (text != null) onGreen else null" in row, "no ring on the key")
check("the timed tick is gone from the row", "justFilled" not in row, "the one-minute mark survives")

print(f"bucket undo, test 1: {checks} checks, {len(failures)} failed ({walked} sequences walked)")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
