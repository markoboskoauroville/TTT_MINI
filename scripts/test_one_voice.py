#!/usr/bin/env python3
"""
Test 1 for the one-voice rule.

He picks a voice and that voice reads, whatever the text is in. **No language is consulted**, which
is the last automatic decision in the reader and the one he asked to have removed.

    python3 scripts/test_one_voice.py
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
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


sp = code(SRC / "dictate/MaSpeechify.kt")
reader = code(SRC / "dictate/MaReader.kt")
screen = code(SRC / "app/settings/dictate/MaReaderScreen.kt")
dash = code(SRC / "dictate/ui/MaReaderDashboard.kt")
prefs = code(SRC / "app/AppPrefs.kt")

# ---------------------------------------------------------------- one choice
check("there is one voice preference", "maReaderVoice = string(" in prefs, "still one per language")
check("chosenVoice takes no language", "fun chosenVoice(): Voice" in sp,
      "the language would still decide")
check("and reads the single preference", "maReaderVoice.get()" in sp, "reads a per-language one")
check("it never asks the language", "MaLanguage" not in sp.split("fun chosenVoice()")[1][:400],
      "a language consulted is a decision he cannot see being made")

# ---------------------------------------------------------------- one list, English first
check("there is one list of all voices", "val ALL_VOICES: List<Voice> = ENGLISH_VOICES + CROATIAN_VOICES" in sp,
      "two lists means two selections")
check("English comes first", sp.index("ENGLISH_VOICES + CROATIAN") < sp.index("CROATIAN_VOICES +")
      if "CROATIAN_VOICES +" in sp else True, "Croatian first")

# The fallback is the head of that list, so it is an English voice — not "the first of the matching
# language", which no longer means anything.
check("the fallback is the head of the list", "?: ALL_VOICES.first()" in sp,
      "a fallback per language, in a world with no per-language choice")

# ---------------------------------------------------------------- one radio group
_eng = screen.find('heading = "English"')
_cro = screen.find('heading = "Croatian"')
check("English section is first on the screen", 0 <= _eng < _cro, f"{_eng} {_cro}")
check("both sections write the same preference", screen.count("pref = prefs.dictate.maReaderVoice,") == 2,
      "two groups, two selections, and something must choose between them")
check("neither writes a per-language one", "maReaderVoiceHr" not in screen and "maReaderVoiceEn" not in screen,
      "the old pair would still be written and read")

# ---------------------------------------------------------------- the callers
check("the reader asks for the one voice", "MaSpeechify.chosenVoice()" in reader, "still passing a language")
check("the dashboard shows every voice", "MaSpeechify.ALL_VOICES" in dash, "only the matching ones")
check("the dashboard names the voice, not the language", 'text = "Reading with " + chosenVoice.name' in dash,
      "it would name a setting that decides nothing")
check("no caller passes a language", "chosenVoice(MaLanguage" not in reader + dash,
      "one caller left deciding by language is the bug, unfixed")

# ---------------------------------------------------------------- the old prefs survive, unread
check("the old preferences are still declared", "maReaderVoiceEn = string(" in prefs,
      "a phone that stored them would trip a missing preference")

print(f"one voice, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
