#!/usr/bin/env python3
"""
Test 1 for the pin circle on clipboard entries.

Multi-select, not radio: any number filled at once, and a filled one sits at the top.

    python3 scripts/test_clip_pin.py
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


def toggle(items, i):
    """One tap on the circle of entry i."""
    return [(t, (not p) if j == i else p) for j, (t, p) in enumerate(items)]


def sections(items):
    """What ClipboardHistory does: pinned first, then the rest, each in its own order."""
    return [x for x in items if x[1]] + [x for x in items if not x[1]]


ITEMS = [(f"clip{i}", False) for i in range(5)]

# ---------------------------------------------------------------- it is NOT a radio button
one = toggle(ITEMS, 1)
two = toggle(one, 3)
check("two can be filled at once", [p for _, p in two] == [False, True, False, True, False], str(two))
check("filling one does not empty another", two[1][1] is True, "it behaved like a radio button")
check("a second tap empties it", toggle(two, 1)[1][1] is False, "no way to unpin")
check("tapping is its own undo", toggle(toggle(ITEMS, 2), 2) == ITEMS)

# ---------------------------------------------------------------- filled goes to the top
check("a filled entry rises", sections(two)[0] == ("clip1", True), str(sections(two)[0]))
check("all the filled ones are above all the rest",
      [p for _, p in sections(two)] == [True, True, False, False, False], str(sections(two)))
check("nothing is lost in the sort", sorted(sections(two)) == sorted(two))
check("nothing is duplicated", len(set(t for t, _ in sections(two))) == len(two))

# Order within each section is kept: pinning c3 must not reorder c1 against it, nor the unpinned
# among themselves. A history that reshuffles is a history he cannot find anything in.
check("pinned keep their relative order", [t for t, _ in sections(two) if _ ] == ["clip1", "clip3"],
      "pinning reordered the pins")
check("the rest keep theirs",
      [t for t, p in sections(two) if not p] == ["clip0", "clip2", "clip4"], "the history reshuffled")

# ---------------------------------------------------------------- walked
walked = 0
for taps in itertools.product(range(5), repeat=4):
    items = list(ITEMS)
    for t in taps:
        items = toggle(items, t)
        walked += 1
        s = sections(items)
        check(f"{taps}: five entries, always", len(s) == 5, str(s))
        check(f"{taps}: none lost", sorted(s) == sorted(items), str(s))
        # The invariant that makes it a section rather than a sort: no unpinned entry above a pinned.
        seen_unpinned = False
        ok = True
        for _, pinned in s:
            if not pinned:
                seen_unpinned = True
            elif seen_unpinned:
                ok = False
        check(f"{taps}: nothing pinned below something unpinned", ok, str(s))


# ---------------------------------------------------------------- the wiring
def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


ui = code(SRC / "ime/clipboard/ClipboardInputLayout.kt")
check("the circle toggles the pin", "clipboardManager.unpinClip(item) else clipboardManager.pinClip(item)" in ui,
      "its own idea of selected, which the history would not honour")
check("it is a circle", "CircleShape" in ui, "a tick box reads as a setting, not as one of a set")
check("empty and filled are one shape", "if (item.isPinned) MaClipRing else Color.Transparent" in ui,
      "two different marks to learn")
check("it reads the item's own state", "item.isPinned" in ui, "a second copy of pinned that can drift")

hist = code(SRC / "ime/clipboard/ClipboardHistory.kt")
check("the history already sections by pinned", "all.filter { it.isPinned }" in hist,
      "the circle would fill and nothing would move")

print(f"clip pin, test 1: {checks} checks, {len(failures)} failed ({walked} taps walked)")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
