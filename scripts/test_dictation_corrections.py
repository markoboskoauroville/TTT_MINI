#!/usr/bin/env python3
"""
Test 1 for learning from his corrections (MaDictationCorrections, DEVELOPMENT.md §222).

    python3 scripts/test_dictation_corrections.py

The decisions (finding a fix, counting it, applying it, the file format) are walked by the kotest
suite MaCorrectionRulesTest (23 cases, run with gradle when written). This suite checks the wiring:
that the dictation path learns before it writes and applies after, that a prompt's rewrite is not
learned, that the field is let go when it closes, and that nothing is sent anywhere.
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "app/src/main/kotlin/dev/patrickgold/florisboard"

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


ctrl = code(APP / "dictate/DictateController.kt")
rules = code(APP / "dictate/nlp/MaCorrectionRules.kt")
corr = code(APP / "dictate/nlp/MaDictationCorrections.kt")
ime = code(APP / "FlorisImeService.kt")
app = code(APP / "FlorisApplication.kt")
prefs = code(APP / "app/AppPrefs.kt")
ngram = code(APP / "dictate/nlp/MaNgram.kt")
model = code(APP / "dictate/nlp/MaNgramModel.kt")
screen = code(APP / "app/settings/dictate/MaCorrectionsScreen.kt")
routes = code(APP / "app/Routes.kt")
order = code(APP / "app/settings/MaSettingsOrder.kt")

# The dictation path: learn from the last one, apply to this one, watch this one.
co = ctrl[ctrl.find("private suspend fun commitOutput"):]
co = co[:co.find("private fun perCharDelayMs")]
check("commitOutput settles before writing", "settleCorrections(context)" in co)
check("commitOutput applies the rules", "MaDictationCorrections.apply(body)" in co)
i_settle, i_apply, i_commit = co.find("settleCorrections(context)"), co.find("MaDictationCorrections.apply(body)"), co.find("sink.commitText(body)")
check("settle, then apply, then commit", 0 <= i_settle < i_apply < i_commit)
check("the start is read before the commit", 0 <= co.find("watchStart") < i_commit)
check("the dictation is watched after the commit", co.find("MaDictationCorrections.noteDictation") > i_commit)
check("only the keyboard's own field is watched", "outputTarget == OutputTarget.IME" in co)

# A prompt rewrites the text: learn his fixes first, then let the rest go.
check("prompts settle and drop", re.search(r"settleCorrections\(appContext\)\s*MaDictationCorrections\.drop\(\)", ctrl) is not None)
check("the glossary reaches the speech model", "MaDictationCorrections.glossary()" in ctrl)

# The field closing settles; the keyboard merely hiding does not.
fi = ime[ime.find("override fun onFinishInput()"):]
check("onFinishInput settles", "MaDictationCorrections.settle" in fi[:fi.find("override fun", 10)])
fv = ime[ime.find("override fun onFinishInputView"):]
check("onFinishInputView does not settle", "MaDictationCorrections.settle" not in fv[:fv.find("override fun", 10)])
check("loaded at start", "MaDictationCorrections.initialize(" in app)

# Privacy: nothing sent, no words logged, secret and incognito fields never watched.
check("no network in the corrections", not re.search(r"okhttp|HttpURLConnection|Request\.Builder", corr + rules))
check("the log says how many, not which", re.search(r'MaLog\.add\("corrections", "learned \$\{swaps\.size\}', corr) is not None)
check("passwords are not watched", "TYPE_TEXT_VARIATION_PASSWORD" in corr and "isSecret(info)" in corr)
check("incognito is not watched", "incognito ||" in corr)

# The rules.
check("used from the second time", re.search(r"ACTIVE_AT\s*=\s*2\b", rules) is not None)
check("at most three words a side", re.search(r"MAX_WORDS\s*=\s*3\b", rules) is not None)
check("Cloud alone is not a seed", '"Cloud" to' not in rules)
for wrong, right in [("Amina", "Emina"), ("Fever", "Fiverr"), ("You Cloud", "Claude"), ("Bok Edibe", "Bok Edib"),
                     ("Davinci", "DaVinci Resolve"), ("Animatix", "animatic")]:
    check(f"seed {wrong} -> {right}", f'"{wrong}" to "{right}"' in rules)

# The wrong word leaves the personal model.
check("MaNgram.unlearn exists", "fun unlearn(word: String, language: String)" in ngram)
check("the model can forget one word", "fun forget(word: String): Boolean" in model)
check("settle unlearns", "MaNgram.unlearn(" in corr)

# Settings.
check("pref on", re.search(r'maCorrectionsEnabled = boolean\(\s*key = "dictate__ma_corrections_enabled",\s*default = true', prefs) is not None)
check("screen: switch first", 0 <= screen.find("maCorrectionsEnabled") < screen.find("Add a fix"))
check("screen: per-rule switch and delete", "MaDictationCorrections.setEnabled(rule" in screen and "MaDictationCorrections.delete(rule)" in screen)
check("screen: delete all asks first", "Delete every correction?" in screen)
check("screen: no 'Default' anywhere", "Default" not in screen and "default" not in screen.replace("FontWeight", ""))
check("route", "object MaDictationCorrections" in routes and "MaCorrectionsScreen()" in routes)
check("settings entry", 'CORRECTIONS("corrections", "My corrections"' in order and "MaSettingsEntry.CORRECTIONS," in order)

print(f"corrections, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print("  FAIL ", f)
sys.exit(1 if failures else 0)
