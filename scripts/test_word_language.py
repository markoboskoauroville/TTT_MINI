#!/usr/bin/env python3
"""
Test 1 for the language gate: which words cost money, and which do not.

The claim he made and I am holding to: **a word already in a model is already in the right place.**
Everything else follows from it, and the thing most worth testing is the set of cases that reach the
network, because that set is the bill.

    python3 scripts/test_word_language.py
"""

import itertools
import re
import sys
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "app/src/main/kotlin/dev/patrickgold/florisboard"

failures: list[str] = []
checks = 0
CROATIAN = "čćžšđ"


def check(name: str, ok: bool, detail: str = "") -> None:
    global checks
    checks += 1
    if not ok:
        failures.append(f"{name}: {detail}")


def normalise(w):
    return "".join(c for c in w.lower() if c.isalpha() or c == "'").strip("'")


def decide(word, badge, known_en, known_hr):
    """The port of MaWordLanguage.decide. Returns (kind, payload)."""
    w = normalise(word)
    if not w:
        return ("known", set())
    known = set()
    if known_en:
        known.add("en")
    if known_hr:
        known.add("hr")
    if known:
        return ("known", known)
    if any(c in CROATIAN for c in w):
        return ("certain", "hr")
    return ("ask", badge)


# ---------------------------------------------------------------- what never costs anything
#
# Walked, not sampled: every combination of known-in-English, known-in-Croatian, badge, and a word
# with or without Croatian letters. Sixteen cases, and only two of them may reach the network.
asked = []
for ken, khr, badge, marked in itertools.product([True, False], [True, False], ["en", "hr"], [True, False]):
    word = "čaša" if marked else "table"
    kind, payload = decide(word, badge, ken, khr)
    case = f"ken={ken} khr={khr} badge={badge} marked={marked}"
    if ken or khr:
        check(f"{case}: known words are free", kind == "known", kind)
    elif marked:
        check(f"{case}: Croatian letters are free", kind == "certain" and payload == "hr", f"{kind} {payload}")
    else:
        check(f"{case}: only new unmarked words are asked about", kind == "ask", kind)
        asked.append(case)
check("exactly two of sixteen cases cost money", len(asked) == 2, str(asked))

# Known beats the diacritic rule: a filed word is not re-examined for any reason.
kind, payload = decide("čaša", "en", known_en=True, known_hr=False)
check("known beats the letters", kind == "known" and payload == {"en"}, f"{kind} {payload}")

# Known in both stays in both. Forcing a choice would delete an earned suggestion.
kind, payload = decide("radio", "hr", True, True)
check("a word in both models stays in both", payload == {"en", "hr"}, str(payload))

# There is NO "plain letters means English" rule, and there must not be: most Croatian words have no
# diacritics, so that rule would be right often and wrong constantly.
kind, _ = decide("kolodvor", "hr", False, False)
check("a plain Croatian word is not assumed English", kind == "ask", kind)

# ---------------------------------------------------------------- folding
check("case folds", normalise("Table") == "table")
check("punctuation goes", normalise("word,") == "word")
check("an apostrophe survives inside", normalise("don't") == "don't")
check("an empty word is not asked about", decide("...", "en", False, False)[0] == "known")


# ---------------------------------------------------------------- reading the answers
def read_answer(reply, asked_set):
    out = {}
    for line in reply.splitlines():
        parts = line.strip().lower().split("=")
        if len(parts) != 2:
            continue
        w = normalise(parts[0])
        if w not in asked_set:
            continue
        v = parts[1].strip().strip('."\'')
        if v == "en":
            out[w] = {"en"}
        elif v == "hr":
            out[w] = {"hr"}
        elif v == "both":
            out[w] = {"en", "hr"}
    return out


A = {"table", "kolodvor", "radio"}
check("a clean answer parses", read_answer("table=en\nkolodvor=hr\nradio=both", A) ==
      {"table": {"en"}, "kolodvor": {"hr"}, "radio": {"en", "hr"}})
check("prose is ignored", read_answer("Here you go:\ntable=en\nHope that helps", A) == {"table": {"en"}})
check("a word nobody asked about is refused", read_answer("banana=en", A) == {},
      "a model can add words to his dictionary")
check("an unknown language is dropped", read_answer("table=fr", A) == {}, "filed under a language that does not exist")
check("an empty reply is nothing", read_answer("", A) == {})


# ---------------------------------------------------------------- the wiring
def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


ngram = code(SRC / "dictate/nlp/MaNgram.kt")
check("the gate is on the learning path", "MaWordLanguage.decide(" in ngram, "words bypass it")
check("known words ask nothing", "is MaWordLanguage.Answer.Known -> Unit" in ngram, "pays for what it knows")
check("the sentence is still learned whole", "modelFor(badge).learn(text)" in ngram,
      "sequences lost, which is what an n-gram is for")
check("asking is batched", "BATCH = 60" in ngram, "a request per word")
check("asking is never automatic", "suspend fun classifyPending" in ngram, "fires while typing")
check("the queue is capped", "PENDING_CAP" in ngram, "a leak rather than a queue")
check("the model knows what it knows", "fun knows(" in code(SRC / "dictate/nlp/MaNgramModel.kt"),
      "cannot tell new from old, so everything is asked")

nlp = code(SRC / "ime/nlp/NlpManager.kt")
check("the shipped dictionary is dropped when it disagrees", "if (providerAgrees) {" in nlp,
      "English words under a Croatian badge — the bug in the screenshot")
check("and the personal model is not", "addAll(personal)" in nlp, "his own words dropped too")

setting = code(SRC / "app/settings/dictate/MaNgramSetting.kt")
check("the screen shows both languages", "MaWordLanguage.HR" in setting, "one number hides the empty model")
check("each can be wiped alone", "MaNgram.forget(language)" in setting, "wiping one costs the other")
check("the queue is visible", "MaNgram.pendingCount()" in setting, "no way to see what is waiting")

# ---------------------------------------------------------------- reflow must not translate
#
# He reported reflow returning English for Croatian text, and only noticing after pasting it. The
# instruction never named a language and every style rule in it is English, so the model followed the
# instruction's language rather than the text's.
CRO = "čćžšđ"


def detect(text, badge):
    """The port of MaWordLanguage.detect. Never returns unknown."""
    if any(c in CRO for c in text):
        return "hr"
    return badge


def name_of(code):
    return "Croatian" if code == "hr" else "English"


check("Croatian letters settle it", detect("Idemo na kolodvor u pet, čekaj me", "en") == "hr",
      "the badge would have said English and the reflow would have translated")
check("one accented letter is enough", detect("ovo je čudno", "en") == "hr")
check("plain English follows the badge", detect("meet me at five", "en") == "en")
check("plain Croatian follows the badge", detect("idemo na kolodvor", "hr") == "hr",
      "no diacritics, so the badge is the only evidence there is")
check("it never answers unknown", detect("", "en") in ("en", "hr"),
      "unknown resolves to English every time, which is the bug")
check("the name is a real language name", name_of("hr") == "Croatian" and name_of("en") == "English",
      "the original language is a rule the model has to resolve, and it resolves it to English")

wl = code(SRC / "dictate/nlp/MaWordLanguage.kt")
check("the detector exists", "fun detect(" in wl, "left to the model to infer")
check("it names the language", "fun nameOf(" in wl, "nothing to put in the instruction")

ctrl = code(SRC / "dictate/DictateController.kt")
check("every rewording names the language", "Your entire answer must be in" in ctrl, "silent translation")
check("it forbids translating outright", "Do not translate it into English" in ctrl)
# THE ORDERING, which is the whole fix. The language rule must sit AFTER the instruction and the
# system prompt, and immediately before his text.
lang_at = ctrl.find("The text below is written in")
sys_at = ctrl.find("if (sys.isNotBlank())")
check("the language rule comes after the style rules", 0 < sys_at < lang_at,
      "English written after it drags the output back into English")
check("and immediately before the text", ctrl.find('append("\\n\\n").append(input)', lang_at) > lang_at,
      "something else sits between the rule and the text it governs")

print(f"word language, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
