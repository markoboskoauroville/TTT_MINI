#!/usr/bin/env python3
"""
Test 1 for the AI key on the prediction row.

Two claims worth testing before a build, both of them about what reaches the editor:

  1. **Nothing that does not continue what he typed may ever be shown.** Tapping a suggestion
     replaces the composing word, so a suggestion not starting with his letters silently rewrites
     what he had already written.
  2. **What is taught to the local model is the word IN ITS CONTEXT**, not the word alone.

    python3 scripts/test_ai_predict.py
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "app/src/main/kotlin/dev/patrickgold/florisboard"

failures: list[str] = []
checks = 0
WANTED = 5


def check(name: str, ok: bool, detail: str = "") -> None:
    global checks
    checks += 1
    if not ok:
        failures.append(f"{name}: {detail}")


def read_words(reply, current):
    """The port of MaAiPredict.readWords."""
    prefix = current.strip().lower()
    out = []
    for line in reply.splitlines():
        w = line.strip()
        for bullet in ("-", "*", "\u2022"):
            if w.startswith(bullet):
                w = w[len(bullet):]
        w = w.strip()
        w = re.sub(r"^\d+[.)]\s*", "", w).strip("\"'` .,")
        if not w or " " in w or len(w) > 40:
            continue
        if prefix and not w.lower().startswith(prefix):
            continue
        if w not in out:
            out.append(w)
    return out[:WANTED]


def lesson(context, chosen):
    tail = " ".join(context.strip().split()[-6:])
    return chosen if not tail else f"{tail} {chosen}"


# ---------------------------------------------------------------- reading the reply
check("plain lines are words", read_words("one\ntwo\nthree", "") == ["one", "two", "three"])
check("numbering is stripped", read_words("1. one\n2) two", "") == ["one", "two"])
check("bullets are stripped", read_words("- one\n* two\n\u2022 three", "") == ["one", "two", "three"])
check("quotes are stripped", read_words('"one"\n\'two\'\n`three`', "") == ["one", "two", "three"])
check("prose is dropped", read_words("Here are some words:\nhello\nHappy to help!", "") == ["hello"],
      str(read_words("Here are some words:\nhello\nHappy to help!", "")))
check("duplicates are dropped", read_words("one\none\ntwo", "") == ["one", "two"])
check("the count is capped", len(read_words("\n".join(f"w{i}" for i in range(20)), "")) == WANTED)
check("an empty reply is nothing", read_words("", "") == [])
check("a runaway word is dropped", read_words("x" * 60 + "\nok", "") == ["ok"])

# THE ONE THAT MATTERS: every suggestion must continue what he typed.
reply = "predict\npredicate\nhello\nPREDICTION\nprefix"
got = read_words(reply, "pred")
check("only continuations survive", got == ["predict", "predicate", "PREDICTION"], str(got))
check("case does not matter to the filter", "PREDICTION" in got, "his lowercase prefix hid a capital")
for w in got:
    check(f"'{w}' continues 'pred'", w.lower().startswith("pred"))
check("nothing survives when nothing fits", read_words("hello\nworld", "zzz") == [])
# With no word being typed, everything is a candidate — this is next-word prediction, not completion.
check("an empty prefix accepts anything", read_words("hello\nworld", "") == ["hello", "world"])

# ---------------------------------------------------------------- the lesson
check(
    "the lesson carries the context",
    lesson("I am going to the", "shop") == "I am going to the shop",
)
check(
    "a long context is trimmed to six words",
    lesson("a b c d e f g h i", "j") == "d e f g h i j",
    lesson("a b c d e f g h i", "j"),
)
check("no context is just the word", lesson("   ", "shop") == "shop")
check("the chosen word is always last", lesson("one two", "three").split()[-1] == "three")

# ---------------------------------------------------------------- the wiring
def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


row = code(SRC / "ime/smartbar/CandidatesRow.kt")
check("the key is asked for, not automatic", "MaAiPredict.busy.value = true" in row, "fires by itself")
check("it reuses the cheap ask", "DictateController.askCheapModel(" in row, "a second provider path")
check("a pick teaches the local model", "MaNgram.learn(" in row, "the database never learns")
check("only an AI pick teaches it", "if (aiWords.isNotEmpty())" in row, "teaching the model its own output back")
check("the overlay clears on a pick", "MaAiPredict.clear()" in row, "stale words survive the commit")
check("nothing is auto-committed", "isEligibleForAutoCommit = false" in row, "an unread guess could insert itself")

# Taught BEFORE the commit, or the lesson contains its own answer.
learn_at = row.find("MaNgram.learn(")
commit_at = row.find("keyboardManager.commitCandidate(tapped)")
check("taught before the commit", 0 < learn_at < commit_at, f"learn={learn_at} commit={commit_at}")

predict = code(SRC / "dictate/nlp/MaAiPredict.kt")
check("the prompt separates the fragment from the sentence", "The word being typed starts with:" in predict,
      "the model completes the sentence instead of the word")
# `import android.` with the dot. Without it this matched `androidx.compose.runtime.mutableStateOf`
# and called a Compose state holder an Android dependency — a check failing on correct code, which
# is the fastest way to teach somebody to stop reading it.
check("no Android framework in the logic", "import android." not in predict, "untestable here")
check("and no network either", "http" not in predict.lower(), "the ask belongs in the controller")

print(f"ai predict, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
