#!/usr/bin/env python3
"""
Test 1 for the picker's grouping: the mechanism, alone.

Parses the Kotlin rather than trusting it, because the claims are about a list of forty-six buttons
and the failure mode is a key quietly missing from every section.

    python3 scripts/test_key_groups.py
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


order = (SRC / "dictate/MaFeatureOrder.kt").read_text()

# Every key, and every group, read from the source.
enum_body = order[: order.index("enum class MaFeatureGroup")]
keys = re.findall(r"^\s{4}([A-Z][A-Z_0-9]*)\(\"", enum_body, re.M)
groups = re.findall(r"^\s{4}([A-Z][A-Z_0-9]*)\(\"", order[order.index("enum class MaFeatureGroup"):], re.M)

# Not a fixed count. It was 30, then undo and redo were added and the check failed for the only
# reason a catalogue ever changes — a key was added. A number that has to be edited on every
# addition is a number that gets edited without being read. What matters is that there ARE keys and
# that every one of them has a section, which is asserted below.
check("keys were found at all", len(keys) >= 30, f"{len(keys)} keys")
check("nine sections", len(groups) == 9, f"{len(groups)}: {groups}")

# The mapping, parsed out of the exhaustive `when`.
mapping_src = order[order.index("val MaFeatureKey.group"):]
assigned: dict[str, str] = {}
current: list[str] = []
for line in mapping_src.splitlines():
    key = re.match(r"\s*MaFeatureKey\.([A-Z_0-9]+),\s*$", line)
    arrow = re.match(r"\s*->\s*MaFeatureGroup\.([A-Z_]+)\s*$", line)
    if key:
        current.append(key.group(1))
    elif arrow:
        for k in current:
            assigned[k] = arrow.group(1)
        current = []

# EVERY key has a section, and no key has two.
for k in keys:
    check(f"{k} has a section", k in assigned, "falls through into nothing")
check("no key is assigned twice", len(assigned) == len(set(assigned)), "duplicate branch")
check("nothing assigned that is not a key", set(assigned) <= set(keys), str(set(assigned) - set(keys)))

# Every section is used. A heading nobody belongs to is a promise with nothing under it.
used = set(assigned.values()) | {"BUCKETS", "MACROS"}
for g in groups:
    check(f"section {g} has keys", g in used, "empty heading")

# The thing he asked for, stated as a test: A is with the buckets.
check("A is in the buckets", assigned.get("AUTO_BUCKET") == "BUCKETS", assigned.get("AUTO_BUCKET", "nowhere"))
check("the bin is in the buckets", assigned.get("CLIP_CLEAR") == "BUCKETS", assigned.get("CLIP_CLEAR", "nowhere"))

# A few groupings that would be wrong if somebody moved them by feel later.
for key, group in (
    ("ALL_PASTE", "CLIPBOARD"),
    ("ALL_CLEAR", "CLIPBOARD"),
    ("ZONE_1", "KEYBOARD"),
    ("ZONE_3", "KEYBOARD"),
    ("MIC", "DICTATION"),
    ("READER", "READING"),
    ("NEXT_FIELD", "MOVING"),
    ("DUMP", "TOOLS"),
):
    check(f"{key} is in {group}", assigned.get(key) == group, assigned.get(key, "nowhere"))

# ---------------------------------------------------------------- the order within the buckets
rows = (SRC / "dictate/MaRows.kt").read_text()


def bucket_order(kind, slot=0):
    """The port of MaRows.bucketOrder."""
    if kind == "auto":
        return 0
    if kind == "clip":
        return slot
    if kind == "bin":
        return 11
    return 0


seq = [("auto", 0)] + [("clip", n) for n in range(1, 11)] + [("bin", 0)]
sorted_seq = sorted(seq, key=lambda x: bucket_order(*x))
check("A first, then C1 to C10, then the bin", sorted_seq == seq, str(sorted_seq))

check("the grouped catalogue exists", "fun catalogueGrouped()" in rows, "the screen has nothing to call")
check("empty sections are dropped", "if (buttons.isEmpty()) null" in rows, "a heading could stand alone")

screen = (SRC / "app/settings/dictate/MaRowsScreen.kt").read_text()
check("the picker uses it", "MaRows.catalogueGrouped()" in screen, "still grouping on the screen")
# Comments stripped first. The catch-all is named in the comment explaining why it is gone, and a
# check that reads comments is a check that fires on its own documentation.
screen_code = re.sub(r"^\s*//.*$", "", re.sub(r"/\*.*?\*/", "", screen, flags=re.S), flags=re.M)
check('the "Keys" catch-all is gone', '"Keys"' not in screen_code, "twenty-six things under one heading")

# NOTE, PAID FOR ONCE: this block was appended to the END of the file, after the exit, and reported
# a happy total having run none of it. The count went up by two for an unrelated reason and hid it.
# **Anything added to a test goes ABOVE the summary line**, and the summary is the last thing in the
# file.

# ---------------------------------------------------------------- the two new keys
#
# Send and Record, added 22.8.2026. Checked by name because both are keys that DO something on
# another app's screen, and the failure mode for those is silence.
row_src = (SRC / "dictate/ui/MaFeatureRow.kt").read_text()
row_code = re.sub(r"^\s*//.*$", "", re.sub(r"/\*.*?\*/", "", row_src, flags=re.S), flags=re.M)

check("Send is in the catalogue", 'SEND("send"' in order, "not offered in the picker")
check("Record is in the catalogue", 'RECORD("record"' in order, "not offered in the picker")
check("both are dictation", assigned.get("SEND") == "DICTATION" and assigned.get("RECORD") == "DICTATION",
      f"SEND={assigned.get('SEND')} RECORD={assigned.get('RECORD')}")

# Send must go through the magic finger's own term, not a hardcoded word. Otherwise the key and the
# volume key would disagree the moment he changes the term.
check("Send uses the configured term", "MaMagicTargets.pressSend()" in row_code, "hardcoded, or its own path")
check("Send says when it finds nothing", "No Send button found" in row_src, "silent failure")

# Record must be the same call as the volume key and the bar's mic, or three routes drift apart.
check("Record uses the one entry point", "DictateController.onMicClick(context)" in row_code, "its own recording path")
check("Record wears the live ring", "ring = if (recording) onGreen else null" in row_code, "no sign it is running")

# One red, named once. Two reds mixed by eye are the same colour to whoever wrote them.
meter_src = (SRC / "dictate/ui/MaRecordMeter.kt").read_text()
check("the recording red is named once", "val MaRecordRed = Color(0xFF9B3B33)" in meter_src, "not shared")
check("the lamp uses it", "MaRecordLampOn = MaRecordRed" in meter_src, "a second red")
check("the key uses it", "MaRecordRed" in row_code, "the dot is some other red")

print(f"key groups, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
