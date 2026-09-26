#!/usr/bin/env python3
"""
Test 1 for the number row's second characters.

The editor existed for weeks inside a screen with no settings entry, so the only way to reach it was
a deep link nobody types. **A feature that compiles, passes its tests and cannot be opened is not a
feature** — this is the third time this month.

    python3 scripts/test_number_row.py
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "app/src/main/kotlin/dev/patrickgold/florisboard"

failures: list[str] = []
checks = 0
SEP = "\u001f"


def check(name: str, ok: bool, detail: str = "") -> None:
    global checks
    checks += 1
    if not ok:
        failures.append(f"{name}: {detail}")


def sanitize(v):
    """The port: no newlines, no separator, no letters, no digits, capped at 16."""
    v = v.replace("\n", "").replace("\r", "").replace(SEP, "")
    return "".join(c for c in v if not c.isalnum())[:16]


# ---------------------------------------------------------------- what he can put there
for good in ("/", "(", ")", "{", "}", "[", "]", "'", '"', "`", "~", "!", "?", ";", ":",
             "<", ">", "|", "\\", "@", "#", "$", "%", "^", "&", "*", "+", "=", "-", "_"):
    check(f"{good!r} is allowed", sanitize(good) == good, "a character he asked for is dropped")

# ---------------------------------------------------------------- what it refuses, and his reason
for bad in ("7", "0", "k", "Z", "é", "\u0161"):
    check(f"{bad!r} is dropped", sanitize(bad) == "",
          "the digit and letter rows already have it, so this spends the only spare gesture")

# Filtered, not refused: the useful part of a paste survives.
check("a mixed paste keeps the punctuation", sanitize("a/b") == "/",
      "an error and an empty field instead of the slash he wanted")
check("a pasted paragraph cannot become a key", len(sanitize("!" * 50)) == 16)

# The separator must never survive: one in a value would split a slot in two and shift every key
# after it.
check("the separator is stripped", sanitize(f"/{SEP}(") == "/(", "one field would become two")
check("newlines are stripped", sanitize("/\n(") == "/(")
check("empty stays empty", sanitize("") == "", "a key with nothing behind it is a choice")

# Ten slots, always ten, whatever is stored.
def parse(raw):
    parts = raw.split(SEP)
    return [sanitize(parts[i]) if i < len(parts) else "" for i in range(10)]


check("a short string still gives ten", len(parse("/")) == 10, "a key would have no slot at all")
check("a long string still gives ten", len(parse(SEP.join("!" * 20))) == 10)
check("a round trip survives", parse(SEP.join(["/", "(", ")"] + [""] * 7))[:3] == ["/", "(", ")"])


def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


core = code(SRC / "dictate/MaNumericSecondary.kt")
order = code(SRC / "app/settings/MaSettingsOrder.kt")
screen = code(SRC / "app/settings/MaSettingsOrderScreen.kt")
routes = code(SRC / "app/Routes.kt")
editor = code(SRC / "app/settings/dictate/MaNumericSecondarySetting.kt")

check("the rule is in the sanitiser", "filter { !it.isLetterOrDigit() }" in core,
      "a letter typed into a field would become a key")
check("it filters rather than refusing", "take(16)" in core, "a paste would be lost whole")

# ---------------------------------------------------------------- it can actually be opened
check("there is a settings entry", 'NUMBER_ROW("number_row"' in order, "unreachable, as before")
check("it is in DEFAULT", "MaSettingsEntry.NUMBER_ROW," in order,
      "it would never appear for anyone who has saved an order")
check("it has an icon", "MaSettingsEntry.NUMBER_ROW ->" in screen)
check("it has a route", "MaSettingsEntry.NUMBER_ROW -> Routes.Settings.MaNumberRow" in screen)
_m = re.search(r"((?:\s*@\w+(?:\([^)]*\))?\s*)*)\bobject MaNumberRow\b", routes)
check("the route carries @Deeplink", _m is not None and "@Deeplink" in _m.group(1),
      "the app would not start — this exact shape crashed build 369")
check("the route is registered", "composableWithDeepLink(Settings.MaNumberRow::class)" in routes)
check("the screen hosts the editor", "MaNumericSecondarySetting()" in
      code(SRC / "app/settings/dictate/MaNumberRowScreen.kt"), "an empty screen")
check("the rule is stated where he types", "letters and numbers are dropped" in editor,
      "a field that silently drops what he types is a field he retries")

print(f"number row, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
