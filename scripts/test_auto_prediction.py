#!/usr/bin/env python3
"""
Test 1 for Settings → Auto prediction (DEVELOPMENT.md §223).

    python3 scripts/test_auto_prediction.py

Marko, 27.10.2026: everything about word prediction under one menu entry, the AI model included.
This pins that every prediction setting is on MaPredictionsScreen and on no other settings screen,
that the AI key asks the model chosen there, and that the choice is a radio list of real names.
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "app/src/main/kotlin/dev/patrickgold/florisboard"
SETTINGS = SRC / "app/settings"

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


screen = code(SETTINGS / "dictate/MaPredictionsScreen.kt")

# Every prediction setting is on the one screen ...
PREFS = [
    "prefs.suggestion.enabled", "prefs.suggestion.nextWordPrediction", "prefs.suggestion.autoCorrect",
    "prefs.suggestion.multilingualTyping", "prefs.suggestion.api30InlineSuggestionsEnabled",
    "prefs.suggestion.displayMode", "prefs.dictate.maNgramEnabled", "prefs.dictate.maAiPredictKey",
    "prefs.dictate.maPredictModel", "prefs.dictate.maCorrectionsEnabled",
]
for p in PREFS:
    check(f"{p} on Auto prediction", p in screen)
check("what was learned is shown there", "MaNgramSetting(showSwitch = false)" in screen)
check("teaching from dictations is reached from there", "Routes.Settings.MaVocabulary" in screen)

# ... and on no other settings screen. The Switchboard's one-tap row for the suggestion row is a
# shortcut that opens this screen, so it is allowed.
ALLOWED = {"dictate/MaPredictionsScreen.kt", "dictate/MaSwitchboardScreen.kt", "dictate/MaNgramSetting.kt"}
for path in SETTINGS.rglob("*.kt"):
    rel = str(path.relative_to(SETTINGS))
    if rel in ALLOWED or "/search/" in str(path):
        continue
    t = code(path)
    for p in PREFS:
        check(f"{p} not on {rel}", p not in t, "a prediction setting outside Auto prediction")
    check(f"MaNgramSetting not on {rel}", "MaNgramSetting(" not in t)

sw = code(SETTINGS / "dictate/MaSwitchboardScreen.kt")
check("the switchboard row opens Auto prediction", "Routes.Settings.MaPredictions" in sw)

# The entry: named as he names it, the only one about prediction.
order = code(SETTINGS / "MaSettingsOrder.kt")
check("the entry is called Auto prediction", 'PREDICTIONS("predictions", "Auto prediction"' in order)
check("Learn my words has no entry of its own", "it == MaSettingsEntry.VOCABULARY" in order)
check("My corrections has no entry of its own", "it == MaSettingsEntry.CORRECTIONS" in order)
check("My corrections is reached from it", "Routes.Settings.MaDictationCorrections" in screen)
check("the big A icon", "MaSettingsEntry.PREDICTIONS -> Icons.Default.FontDownload" in code(SETTINGS / "MaSettingsOrderScreen.kt"))
check("beside Prompts and its costs",
      re.search(r"MaSettingsEntry\.CLAUDE_COSTS,\s*MaSettingsEntry\.PREDICTIONS,", order[order.find("val DEFAULT"):]) is not None)
strings = (ROOT / "app/src/main/res/values/strings.xml").read_text()
check("the title string", '<string name="settings__ma_predictions__title">Auto prediction</string>' in strings)
check("the screen uses it", "R.string.settings__ma_predictions__title" in screen)

# The model: a radio list of real names, Claude only, paid with the loaded Anthropic key.
check("a radio list", "RadioButton(" in screen)
check("each row shows the real name", "MaClaudeModels.displayName(MaClaudeModels.resolve(alias, cached))" in screen)
check("the three Claude tiers", 'listOf("haiku", "sonnet", "opus")' in screen)
check("no 'Default' on the screen", "Default" not in screen)
check("the key's state is said, never the key", "hasKey" in screen and "apiKey}" not in screen)

ctrl = code(SRC / "dictate/DictateController.kt")
row = code(SRC / "ime/smartbar/CandidatesRow.kt")
ngram = code(SRC / "dictate/nlp/MaNgram.kt")
check("askPredictionModel uses the chosen model", "modelOverride = prefs.dictate.maPredictModel.get()" in ctrl)
check("the override reaches the cheap path", "if (modelOverride != null && account.providerId == MaRoles.REWORDING)" in ctrl)
check("the AI key asks it", "DictateController.askPredictionModel(" in row)
check("sorting new words into a language asks it", "DictateController.askPredictionModel(" in ngram)

# Search finds the moved settings on their new screen.
index = (SETTINGS / "search/SettingsSearchIndex.kt").read_text()
for key in ["enabled", "auto_correct", "multilingual_typing", "next_word_prediction", "display_mode"]:
    check(f"search: {key} -> Auto prediction",
          re.search(r"pref__suggestion__" + key + r"__label, R\.string\.settings__ma_predictions__title, Routes\.Settings\.MaPredictions", index) is not None)

# The one-time move up beside Prompts (he could not find it near the bottom of his stored order).
check("the move exists", "fun withPredictionsBesidePrompts(raw: String): String" in order)
mig = code(SRC / "dictate/data/prefs/DictateLegacyMigrator.kt")
check("it runs once", "maPredictionsMovedV1.get()) return" in mig and "maPredictionsMovedV1.set(true)" in mig)
check("it runs at start", "DictateLegacyMigrator.movePredictionsBesidePromptsIfNeeded()" in code(SRC / "FlorisApplication.kt"))


def move(raw: str) -> str:  # the same steps as withPredictionsBesidePrompts
    ids = [i.strip() for i in raw.split(",") if i.strip() and i.strip() not in ("predictions", "corrections")]
    anchor = next((a for a in ("claude_costs", "prompts") if a in ids), None)
    if anchor is None:
        return raw
    ids.insert(ids.index(anchor) + 1, "predictions")
    return ",".join(ids)


check("after Claude costs", move("a,prompts,claude_costs,b,vocabulary,predictions") == "a,prompts,claude_costs,predictions,b,vocabulary")
check("after Prompts without costs", move("prompts,x,predictions") == "prompts,predictions,x")
check("corrections folds in", move("prompts,claude_costs,corrections,x,predictions") == "prompts,claude_costs,predictions,x")
check("nothing to anchor: unchanged", move("x,y,predictions") == "x,y,predictions")

print(f"auto prediction, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print("  FAIL ", f)
sys.exit(1 if failures else 0)
