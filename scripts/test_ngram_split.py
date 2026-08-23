#!/usr/bin/env python3
"""
Test 1 for the split n-gram: two isolated models, one per language.

The claim: **what is written in one language never reaches the other's suggestions.** Modelled and
walked, because the failure it replaces was silent — the old model answered confidently in the wrong
language and looked like a bad guess rather than a bug.

    python3 scripts/test_ngram_split.py
"""

import itertools
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "app/src/main/kotlin/dev/patrickgold/florisboard"

failures: list[str] = []
checks = 0
LANGUAGES = ["en", "hr"]


def check(name: str, ok: bool, detail: str = "") -> None:
    global checks
    checks += 1
    if not ok:
        failures.append(f"{name}: {detail}")


class Ngram:
    """The two models, as MaNgram now keeps them."""

    def __init__(self):
        self.models = {lang: {} for lang in LANGUAGES}
        self.badge = "en"

    def switch(self, lang):
        self.badge = lang

    def learn(self, text):
        # The language at the moment of writing, captured before any save.
        for w in text.split():
            self.models[self.badge][w] = self.models[self.badge].get(w, 0) + 1

    def predict(self, prefix):
        m = self.models[self.badge]
        return sorted([w for w in m if w.startswith(prefix)], key=lambda w: (-m[w], w))

    def forget_everything(self):
        for lang in LANGUAGES:
            self.models[lang].clear()


# ---------------------------------------------------------------- isolation
n = Ngram()
n.learn("kolegica kolegica kolodvor")          # English badge, Croatian words: his mistake, kept
n.switch("hr")
n.learn("kolegica kolodvor kolo")
n.switch("en")
n.learn("kolonel")

check("the English model has only what was typed on the English badge",
      set(n.models["en"]) == {"kolegica", "kolodvor", "kolonel"}, str(n.models["en"]))
check("the Croatian model has only its own",
      set(n.models["hr"]) == {"kolegica", "kolodvor", "kolo"}, str(n.models["hr"]))

# THE INVARIANT: a word learned only in one language is never predicted in the other.
n.switch("hr")
check("Croatian never offers an English-only word", "kolonel" not in n.predict("kol"), str(n.predict("kol")))
n.switch("en")
check("English never offers a Croatian-only word", "kolo" not in n.predict("kol"), str(n.predict("kol")))

# Counts do not leak either. "kolegica" was written twice on the English badge and once on Croatian,
# and each side must rank it by its OWN evidence.
check("counts are not shared", n.models["en"]["kolegica"] == 2 and n.models["hr"]["kolegica"] == 1,
      f"en={n.models['en']['kolegica']} hr={n.models['hr']['kolegica']}")

# ---------------------------------------------------------------- his actual way of working
#
# A sentence in one language, the badge, a sentence in the other. Every order of six such turns,
# asserting after each that neither model has ever seen the other's words.
EN_ONLY = {"today", "meeting", "tomorrow"}
HR_ONLY = {"danas", "sastanak", "sutra"}
walked = 0
for seq in itertools.product(LANGUAGES, repeat=6):
    n = Ngram()
    for lang in seq:
        n.switch(lang)
        n.learn(" ".join(EN_ONLY if lang == "en" else HR_ONLY))
        walked += 1
        check(f"{seq}: no Croatian in the English model", not (set(n.models["en"]) & HR_ONLY), str(n.models["en"]))
        check(f"{seq}: no English in the Croatian model", not (set(n.models["hr"]) & EN_ONLY), str(n.models["hr"]))

# Switching the badge alone never changes what either model holds.
n = Ngram()
n.learn("one two")
before = {k: dict(v) for k, v in n.models.items()}
for lang in ("hr", "en", "hr", "en"):
    n.switch(lang)
check("switching alone learns nothing", n.models == before, str(n.models))

# Forgetting is everything, or the count goes to zero and his words come back with the badge.
n = Ngram()
n.learn("one")
n.switch("hr")
n.learn("jedan")
n.forget_everything()
check("forget clears both", not n.models["en"] and not n.models["hr"], str(n.models))

# ---------------------------------------------------------------- the wiring
def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


ngram = code(SRC / "dictate/nlp/MaNgram.kt")
check("there are two models", "private val models = mutableMapOf<String, MaNgramModel>()" in ngram, "still one")
check("the badge chooses", "MaLanguage.active()" in ngram, "deaf to the language key")
check("predict reads only the active model", "val active = modelFor(active())" in ngram, "reads both")
# The variable was renamed `badge` when the language gate landed; the claim is unchanged and still
# the one that matters — read OUTSIDE the coroutine, so a badge press between the commit and the save
# cannot file the sentence under the wrong language.
check("the language is captured before the coroutine", "val badge = active()" in ngram,
      "a badge press between commit and save would file the sentence under the wrong language")
check("the mixed file is removed once", "LEGACY_FILE_NAME" in ngram, "the old mixed counts survive")
check("the backfill routes by entry language", "entry.language" in ngram, "rebuilt mixed again")
check("forget clears every model", "for (language in LANGUAGES) {" in ngram, "one language left standing")

ai = code(SRC / "dictate/nlp/MaAiPredict.kt")
check("the AI prompt names the language more than once", ai.count("append(language)") >= 2,
      "one mention loses to the evidence in the text so far")
check("and forbids the other one", "Never answer in another language" in ai, "will follow the text instead")

row = code(SRC / "ime/smartbar/CandidatesRow.kt")
check("the row asks by the badge", 'MaLanguage.active() == MaLanguage.HR' in row, "asks by the subtype instead")

print(f"ngram split, test 1: {checks} checks, {len(failures)} failed ({walked} turns walked)")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
