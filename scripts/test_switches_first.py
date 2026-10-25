#!/usr/bin/env python3
"""
Test 1 for "switches first" on every settings screen (MANTRA_MANIFEST mantra-design-language.md
§1.9, Marko 21.10.2026: "What is global should be up").

    python3 scripts/test_switches_first.py

For each screen reordered on 21.10.2026: every on/off switch named below must come before the first
detail setting named below it (a list, slider, colour picker or a dialog entry). A new switch added
under the details, or a detail added above the switches, fails here.
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
S = ROOT / "app/src/main/kotlin/dev/patrickgold/florisboard/app/settings"

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


# screen file, its function, switches that must be first, the first detail they must precede
SCREENS = [
    ("dictate/DictateScreen.kt", "fun DictateRecordingScreen",
     ["maCorrectedDictation", "maDictationApi", "pushToTalk", "realtimeTranscription", "useBluetoothMic",
      "keepScreenAwake", "skipSilentRecordings", "trimSilentGaps", "instantRecording,"],
     "dictate__longform_title"),
    ("dictate/DictateScreen.kt", "fun DictateOutputScreen",
     ["autoEnter", "instantOutput", "resendButton", "hapticFeedback", "rememberLastDictation"],
     "prefs.dictate.outputSpeed"),
    ("dictate/MaMagicScreen.kt", "fun MaMagicScreen",
     ["maMagicRowShown", "maVoiceCommands"], "How it works:"),
    ("dictate/DictateFloatingButtonScreen.kt", "fun DictateFloatingButtonScreen",
     ["floatingButtonShowWithDictateKeyboard", "floatingButtonSnapToEdge", "floatingButtonAutoDim",
      "floatingButtonRememberPosition", "floatingButtonHaptic", "floatingButtonUndoEnabled",
      "floatingButtonCopyToClipboard"], "prefs.dictate.floatingButtonDesign"),
    ("gestures/GesturesScreen.kt", "fun GesturesScreen", ["maSwipeToSymbol"], "Volume keys"),
    ("clipboard/ClipboardScreen.kt", "fun ClipboardScreen",
     ["useInternalClipboard", "prefs.clipboard.historyEnabled,"], "prefs.clipboard.syncToFloris"),
    ("clipboard/ClipboardScreen.kt", "fun ClipboardScreen",
     ["historyAutoCleanOldEnabled", "historyAutoCleanSensitiveEnabled", "historySizeLimitEnabled",
      "historyHideOnPaste", "historyHideOnNextTextField", "clearPrimaryClipAffectsHistoryIfUnpinned"],
     "historyNumGridColumnsPortrait"),
    ("keyboard/KeyboardScreen.kt", "fun KeyboardScreen",
     ["utilityKeyEnabled", "popupEnabled", "mergeHintPopupsEnabled", "spaceBarSwitchesToCharacters"],
     "prefs.keyboard.utilityKeyAction"),
    ("typing/TypingScreen.kt", "fun TypingScreen",
     ["autoCapitalization", "autoSpacePunctuation", "rememberCapsLockState", "doubleSpacePeriod"],
     "prefs.dictate.maTextCase"),
    # build 403: Incognito (a list) sat between the Suggestions and the Corrections switches.
    ("typing/TypingScreen.kt", "fun TypingScreen",
     ["api30InlineSuggestionsEnabled", "doubleSpacePeriod"], "prefs.suggestion.incognitoMode"),
    ("smartbar/SmartbarScreen.kt", "fun SmartbarScreen", ["flipToggles"], "prefs.suggestion.displayMode"),
    # build 403: Layout (a list) sat between "Enable Smartbar" and "Flip toggle buttons".
    ("smartbar/SmartbarScreen.kt", "fun SmartbarScreen", ["enabled,", "flipToggles"], "listPref = prefs.smartbar.layout"),
    ("dictate/MaVocabularyScreen.kt", "fun MaVocabularyScreen", ["maNgramEnabled"], "LaunchedEffect(Unit)"),
    ("dictate/MaCorrectionsScreen.kt", "fun MaCorrectionsScreen", ["maCorrectionsEnabled"], "Add a fix"),
    ("dictate/MaPromptsScreen.kt", "fun MaPromptsScreen", ["maPhoneStyleEnabled", "maSpellcheckEnabled"],
     "MaPhoneModelSection()"),
]

for rel, fn, switches, detail in SCREENS:
    t = code(S / rel)
    start = t.find(fn)
    check(f"{rel}: {fn} exists", start >= 0)
    if start < 0:
        continue
    d = t.find(detail, start)
    check(f"{rel}: detail '{detail}' found", d >= 0)
    for sw in switches:
        i = t.find(sw, start)
        check(f"{rel}: {sw} before {detail}", 0 <= i < d, "a switch below the settings that adjust it")

print(f"switches first, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print("  FAIL ", f)
sys.exit(1 if failures else 0)
