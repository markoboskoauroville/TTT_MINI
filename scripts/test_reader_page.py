#!/usr/bin/env python3
"""
Test 1 for the page cut around the current word.

The bug: top alignment worked in the small box and did nothing in full screen. The box was aligned
correctly both times — but a full-screen page holds many lines and the current word could be
anywhere inside it, so aligning the PAGE says nothing about where the WORD is.

    python3 scripts/test_reader_page.py
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


def cut(words, index, per_page, align):
    """The port of the page cut."""
    if not (0 <= index < len(words)):
        return None
    used, frm, to = 0, index, index
    while True:
        take_after = align != "bottom" and to + 1 < len(words)
        take_before = align != "top" and frm - 1 >= 0
        if align == "middle" and take_after and (to - index) <= (index - frm):
            nxt = to + 1
        elif align == "middle" and take_before:
            nxt = frm - 1
        elif take_after:
            nxt = to + 1
        elif take_before:
            nxt = frm - 1
        else:
            nxt = -1
        if nxt < 0:
            break
        cost = len(words[nxt]) + 1
        if used + cost > per_page and to > frm:
            break
        used += cost
        if nxt > to:
            to = nxt
        else:
            frm = nxt
    return frm, to


W = [f"w{i}" for i in range(40)]

# ---------------------------------------------------------------- where the word lands
for i in (0, 5, 20, 39):
    frm, to = cut(W, i, 30, "top")
    check(f"top: the page starts at the word (i={i})", frm == i, f"{frm}..{to}")
    frm, to = cut(W, i, 30, "bottom")
    check(f"bottom: the page ends at the word (i={i})", to == i, f"{frm}..{to}")

# Middle grows one side at a time and stops when the page is full, so "near the centre" is within a
# word or two, not exact — and asserting exactness would be asserting the loop's step order rather
# than what he can see. The word must simply not be at either edge when there is room on both sides.
frm, to = cut(W, 20, 30, "middle")
check("middle: the word is not at an edge", frm < 20 < to, f"{frm}..{to}")
check("middle: there is text on both sides", (20 - frm) >= 1 and (to - 20) >= 1, f"{frm}..{to}")

# ---------------------------------------------------------------- the invariants that matter
walked = 0
for align in ("top", "middle", "bottom"):
    for i in range(len(W)):
        for per in (10, 30, 120):
            frm, to = cut(W, i, per, align)
            walked += 1
            # THE WORD IS ALWAYS ON THE PAGE. Everything else is presentation; this is correctness.
            check(f"{align} i={i} per={per}: the word is on the page", frm <= i <= to, f"{frm}..{to}")
            check(f"{align} i={i} per={per}: inside the passage", 0 <= frm and to < len(W))
            check(f"{align} i={i} per={per}: not empty", frm <= to)

# At the very start, top and middle cannot grow backwards and must not fail.
check("index 0, middle, does not run off the front", cut(W, 0, 30, "middle")[0] == 0)
check("last index, top, does not run off the end", cut(W, len(W) - 1, 30, "top")[1] == len(W) - 1)
check("a word longer than the page still shows", cut(["x" * 500], 0, 30, "top") == (0, 0),
      "a very long word would leave an empty page")
check("an index outside the passage gives nothing", cut(W, 99, 30, "top") is None)

# The page MOVES WITH THE READING rather than in jumps. Consecutive indices must give overlapping
# pages — that is what stops the highlight leaping when a page boundary is crossed.
for align in ("top", "middle", "bottom"):
    for i in range(len(W) - 1):
        a = cut(W, i, 30, align)
        b = cut(W, i + 1, 30, align)
        check(f"{align}: page {i} and {i+1} overlap", a[1] >= b[0] and b[1] >= a[0], f"{a} then {b}")


def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


cap = code(SRC / "dictate/ui/MaSubtitleRow.kt")
# paginate SURVIVES, and should: the scrolling full-screen view needs every line at once because it
# scrolls between them. What changed is that the small box no longer uses it.
check("the small box cuts around the word", "remember(words, index, perPage, alignNow)" in cap,
      "the page containing the word rather than the page around it")
check("the scrolling view obeys the alignment", "val alignLines = when (alignNow)" in cap,
      "full screen always scrolled the live line to the top, whatever the setting said")
check("and it scrolls without animating", "scrollToItem(" in cap and "animateScrollToItem" not in cap,
      "he cannot watch animated scrolling")
check("the cut reads the alignment", 'alignNow != "bottom"' in cap, "one shape for all three settings")


row = code(SRC / "dictate/ui/MaFeatureRow.kt")
check("the dashboard opens without a reading", "if (drawChrome && maDashboardOpen) {" in row,
      "a long press before pressing play does nothing")
check("the reading gate is gone", "maDashboardOpen && MaReader.currentIndex >= 0" not in row,
      "settings only reachable while something is already playing")

print(f"reader page, test 1: {checks} checks, {len(failures)} failed ({walked} cuts walked)")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
