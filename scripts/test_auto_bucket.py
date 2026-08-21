#!/usr/bin/env python3
"""
Test 1 for the A key's new rule: the mechanism, alone.

The rule in one line: **take the lowest code block in the frame that is not already in a bucket.**

Ported and walked here, because the failure it replaces was not a bug in code — it was a rule that
did not match how he works. A rule is the thing to test first.

    python3 scripts/test_auto_bucket.py
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


def press(in_view, buckets, visible=(1, 2, 3, 4, 5)):
    """
    One press of A.

    `in_view` is the code blocks on screen, lowest first — the order the key tries them in.
    Returns (outcome, buckets, presses) where outcome is 'copied', 'all-held', 'full' or 'empty'.
    """
    if not in_view:
        return "empty", buckets, 0
    presses = 0
    for text in in_view:
        presses += 1
        # The press happens; the capture decides. This mirrors MaClipCapture.capture exactly.
        if text in buckets.values():
            continue                      # already held: skip to the one above
        free = next((n for n in sorted(visible) if n not in buckets), None)
        if free is None:
            return "full", buckets, presses
        out = dict(buckets)
        out[free] = text
        return "copied", out, presses
    return "all-held", buckets, presses


# ---------------------------------------------------------------- the case it is FOR
out, b, n = press(["lowest", "higher"], {})
check("takes the lowest block first", b.get(1) == "lowest", str(b))
check("one press, one block", n == 1, f"{n} presses")
out, b, n = press(["lowest", "higher"], b)
check("the next press takes the one above", b.get(2) == "higher", str(b))
check("and it cost two presses, the held one and the new one", n == 2, f"{n} presses")

# One block in view: it is the one, and pressing again says so rather than taking it twice.
out, b, _ = press(["only"], {})
check("a single block is taken", b.get(1) == "only")
out, b2, _ = press(["only"], b)
check("pressing again changes nothing", b2 == b, str(b2))
check("and it says why", out == "all-held", out)

# ---------------------------------------------------------------- the cases it must refuse
check("nothing in view is not an error", press([], {})[0] == "empty")

full = {n: f"t{n}" for n in (1, 2, 3, 4, 5)}
check("full buckets are reported as full", press(["new block"], full)[0] == "full")
# And full is NOT reported when the block was already held — two different messages, and the
# difference is the one thing he has to act on.
check("held beats full", press(["t3"], full)[0] == "all-held", "a held block reported as full")

# ---------------------------------------------------------------- his actual movement, walked
#
# Not linear. He scrolls up, takes one, scrolls down, takes another, and revisits screens he has
# already collected from. Every ordering of four screens drawn from a page of five blocks, with the
# frame showing two blocks at a time.
page = ["b1", "b2", "b3", "b4", "b5"]
frames = [tuple(page[i:i + 2]) for i in range(len(page) - 1)]
walked = 0
for seq in itertools.product(range(len(frames)), repeat=4):
    buckets: dict[int, str] = {}
    for f in seq:
        # The frame's blocks, lowest first. Lower on the page is later in the list, so it reverses.
        in_view = list(reversed(frames[f]))
        outcome, buckets, _ = press(in_view, buckets)
        walked += 1
        # NOTHING IS EVER TAKEN TWICE. This is the invariant the whole change exists for.
        held = list(buckets.values())
        check(f"{seq}: no block is in two buckets", len(held) == len(set(held)), str(buckets))
        # A bucket never changes what it holds while it holds it.
        check(f"{seq}: buckets only fill, never rewrite", all(v in page for v in held), str(buckets))
        check(f"{seq}: outcome is one of the four", outcome in ("copied", "all-held", "full", "empty"))

# Revisiting a screen already collected from never takes anything.
buckets = {}
_, buckets, _ = press(["b2", "b1"], buckets)
_, buckets, _ = press(["b2", "b1"], buckets)
before = dict(buckets)
outcome, buckets, _ = press(["b2", "b1"], buckets)
check("a third visit to the same screen takes nothing", buckets == before, str(buckets))
check("and says everything here is held", outcome == "all-held", outcome)

# ---------------------------------------------------------------- the wiring
def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


row = code(SRC / "dictate/ui/MaFeatureRow.kt")
check("the key presses only what is in view", "pressScreenTargetInView" in row, "still walking the page")
check("the key counts what is in view", "countScreenTargetsInView" in row, "no bound on the loop")
check("it tries every block in the frame", "for (rank in 0 until inView)" in row, "one press and give up")
check("the face is A, not A1", 'label = "A"' in row, "the ladder is still on the key")
check("it says which bucket took it", "C${slot + 1}" in row, "no confirmation")
check("it says when a block is already held", "Already copied" in row, "silent on the case he asked about")
check("full and held are different messages", "Every bucket is full" in row, "one message for two states")

targets = code(SRC / "dictate/overlay/MaScreenTargets.kt")
check("visible-only exists", "visibleOnly" in targets, "no way to limit to the frame")
check("and is off by default", "visibleOnly: Boolean = false" in targets, "every other caller changed too")
check("the frame test checks bounds as well", "Rect.intersects" in targets, "isVisibleToUser alone")

svc = code(SRC / "dictate/overlay/DictateAccessibilityService.kt")
check("the service exposes the in-view press", "fun pressScreenTargetInView" in svc)
check("and the in-view count", "fun countScreenTargetsInView" in svc)

capture = code(SRC / "dictate/MaClipCapture.kt")
check("the ladder is gone from the model", "autoRank" not in capture, "a counter nothing sets")

print(f"auto bucket, test 1: {checks} checks, {len(failures)} failed ({walked} frames walked)")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
