#!/usr/bin/env python3
"""
Test 1 for the language keys and the status-line label.

Two bugs and one design change:
  · the sending line showed the SETTING, not the language the request was sent in
  · a single record key plus a language setting means every recording begins with a question

    python3 scripts/test_language_keys.py
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


class World:
    """Setting, request, and what the status line says."""

    def __init__(self):
        self.setting = "en"
        self.in_flight = None

    def record_in(self, lang):
        self.setting = lang          # set FIRST
        self.in_flight = lang        # then the audio exists and the request captures it

    def change_setting(self, lang):
        self.setting = lang

    def label(self):
        return self.in_flight if self.in_flight is not None else self.setting

    def finish(self):
        self.in_flight = None


# ---------------------------------------------------------------- the bug he reported
w = World()
w.record_in("en")
check("sending English says English", w.label() == "en", w.label())
w.change_setting("hr")
check("changing the setting does NOT relabel the request in flight", w.label() == "en",
      "this is exactly 'sending English, status says Croatian'")
w.finish()
check("with nothing in flight the setting is the answer", w.label() == "hr", w.label())

# Walked: any sequence of records and setting changes, the label must equal the request's language
# whenever one is flying, and the setting otherwise. Never anything else.
import itertools
walked = 0
for seq in itertools.product(["rec_en", "rec_hr", "set_en", "set_hr", "finish"], repeat=4):
    w = World()
    for act in seq:
        if act.startswith("rec_"):
            w.record_in(act[4:])
        elif act.startswith("set_"):
            w.change_setting(act[4:])
        else:
            w.finish()
        walked += 1
        expected = w.in_flight if w.in_flight is not None else w.setting
        check(f"{seq}: the label is the request's language", w.label() == expected, w.label())

# The order inside a press: language BEFORE recording, or the request captures the old one.
w = World()
w.record_in("hr")
check("the language is set before the request exists", w.in_flight == "hr" and w.setting == "hr")


def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


ctrl = code(SRC / "dictate/DictateController.kt")
ui = code(SRC / "dictate/ui/DictateSmartbarUi.kt")
row = code(SRC / "dictate/ui/MaFeatureRow.kt")
order = code(SRC / "dictate/MaFeatureOrder.kt")

check("the request carries its language", "val language: String," in ctrl, "nothing to label it with")
check("it is captured when the request leaves", "language = MaLanguage.active()," in ctrl)
check("the line reads the request", "DictateController.inFlightLanguage" in ui,
      "it would show the setting, which is the bug")
check("and not the setting directly", 'if (languageMode == MaLanguage.EN) "ENG"' not in ui,
      "the setting relabels a request already on the wire")

for k in ("RECORD_HR", "RECORD_EN", "SEND_HR", "SEND_EN"):
    check(f"{k} is a key", f'{k}("' in order, "not in the catalogue")
check("the record pair is written once", "MaFeatureKey.RECORD_HR, MaFeatureKey.RECORD_EN ->" in row,
      "two places for the set-then-record order to drift")
check("the send pair is written once", "MaFeatureKey.SEND_HR, MaFeatureKey.SEND_EN ->" in row)

rec = row[row.index("MaFeatureKey.RECORD_HR, MaFeatureKey.RECORD_EN ->"):][:1600]
check("the language is set before recording starts",
      rec.index("MaLanguage.set(context, language)") < rec.index("DictateController.onMicClick"),
      "the audio would exist before the language did, and transcribe would capture the old one")
check("it confirms in words", '"Sending "' in rec and '"Recording "' in rec,
      "no confirmation of which language it went in")

print(f"language keys, test 1: {checks} checks, {len(failures)} failed ({walked} steps walked)")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
