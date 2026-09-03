#!/usr/bin/env python3
"""
Test 1 for the key search: the three layers, in Python, before any build.

The claim: **literal first, then what he has meant before, and a model only when both are empty.**
The third layer costs money and can be wrong, so the condition under which it fires is the thing
most worth testing.

    python3 scripts/test_key_search.py
"""

import re
import sys
import unicodedata
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


# ---------------------------------------------------------------- the port
def fold(text):
    flat = "".join(c for c in unicodedata.normalize("NFD", text) if not unicodedata.combining(c)).lower()
    return " ".join("".join(c if c.isalnum() else " " for c in flat).split())


class Entry:
    def __init__(self, id, label, description, letters=""):
        self.id, self.label, self.description, self.letters = id, label, description, letters
        self.haystack = fold(f"{label} {description} {letters} {id}")

    def __repr__(self):
        return self.id


def literal(query, entries):
    words = fold(query).split()
    if not words:
        return list(entries)
    hits = [e for e in entries if all(w in e.haystack for w in words)]

    def rank(e):
        label = fold(e.label)
        if all(label.startswith(w) for w in words):
            return 0
        if all(w in label for w in words):
            return 1
        return 2

    return sorted(hits, key=rank)


def learn(store, query, id, cap=200):
    q = fold(query)
    if not q or not id:
        return store
    out = list(store)
    for i, (sq, sid, n) in enumerate(out):
        if sq == q and sid == id:
            out[i] = (sq, sid, n + 1)
            break
    else:
        out.append((q, id, 1))
    return sorted(out, key=lambda r: -r[2])[:cap]


def remembered(query, store, entries):
    q = fold(query)
    if not q:
        return []
    by_id = {e.id: e for e in entries}
    rows = [r for r in store if r[0] == q or (len(q) >= 2 and r[0].startswith(q))]
    rows.sort(key=lambda r: (r[0] != q, -r[2]))
    out = []
    for _, sid, _ in rows:
        e = by_id.get(sid)
        if e is not None and e not in out:
            out.append(e)
    return out


def resolve(query, entries, store):
    """Returns (entries, from_memory, ai_wanted)."""
    if not fold(query):
        return list(entries), False, False
    hits = literal(query, entries)
    if hits:
        return hits, False, False
    learned = remembered(query, store, entries)
    if learned:
        return learned, True, False
    return [], False, True


KEYS = [
    Entry("volume_keys", "Volume keys", "Dictation", "Volume keys"),
    Entry("record", "Record", "Dictation", "Record"),
    Entry("send", "Send", "Dictation", "Send"),
    Entry("clip:3", "Copy bucket 3", "Copy buckets, C1 to C10", "C3"),
    Entry("auto_bucket", "A-bucket, code", "Copy buckets, C1 to C10", "A"),
    Entry("all_paste", "AP (all paste)", "Clipboard", "AP"),
    Entry("undo", "Undo", "Editing the text", "Undo"),
    Entry("macro:1", "M1, empty", "Your macros, M1 to M10", "M1"),
]

# ---------------------------------------------------------------- layer one
check("a plain word finds its key", literal("record", KEYS)[0].id == "record", str(literal("record", KEYS)))
check("the section name is searchable", {e.id for e in literal("clipboard", KEYS)} == {"all_paste"})
check("the face is searchable", literal("c3", KEYS)[0].id == "clip:3")
check("two words narrow, never widen", len(literal("copy bucket", KEYS)) <= len(literal("copy", KEYS)))
check("a label hit outranks a description hit", literal("copy", KEYS)[0].id == "clip:3", str(literal("copy", KEYS)))
check("an empty query is everything", len(literal("  ", KEYS)) == len(KEYS))
check("nonsense finds nothing", literal("qwertz", KEYS) == [])

# Accents. He types Croatian, and the same word typed two ways must be one word here or the memory
# learns two names for one key and neither fires reliably.
check("accents fold away", fold("Čišćenje") == "ciscenje", fold("Čišćenje"))
check("punctuation folds away", fold("AP (all paste)") == "ap all paste", fold("AP (all paste)"))
check("case folds away", fold("VOLUME") == fold("volume"))

# ---------------------------------------------------------------- layer two
store = []
check("nothing is remembered at the start", resolve("sound off", KEYS, store)[2] is True)
store = learn(store, "sound off", "volume_keys")
found, from_memory, ai = resolve("sound off", KEYS, store)
check("his own words work the second time", [e.id for e in found] == ["volume_keys"], str(found))
check("and it is marked as memory", from_memory is True)
check("and no model is wanted", ai is False, "would have paid for a question it can answer")

# A prefix of a remembered query finds it while it is still being typed.
check("a prefix reaches it", [e.id for e in remembered("sound", store, KEYS)] == ["volume_keys"])
# One letter does not: too many things start with one letter to be a name.
check("one letter is not a name", remembered("s", store, KEYS) == [])

# Used more often wins.
store = learn(store, "x", "record")
store = learn(store, "x", "send")
store = learn(store, "x", "send")
check("the habit beats the accident", remembered("x", store, KEYS)[0].id == "send",
      str(remembered("x", store, KEYS)))

# Learning never replaces a literal match — the app's own word keeps working whatever he taught it.
store2 = learn([], "record", "undo")
found, _, _ = resolve("record", KEYS, store2)
check("a literal match still wins", found[0].id == "record", str(found))

# The cap drops the least used, not the oldest.
big = []
for i in range(205):
    big = learn(big, f"q{i}", "record")
big = learn(big, "q7", "record")
big = learn(big, "q7", "record")
check("the cap holds", len(big) == 200, str(len(big)))
check("the most used survives the cap", any(r[0] == "q7" for r in big), "the habit was dropped")

# ---------------------------------------------------------------- layer three
_, _, ai = resolve("make the phone stop beeping", KEYS, store)
check("the model is wanted only when both are empty", ai is True)
for q in ("record", "sound off", "  "):
    check(f"'{q}' does not pay for a model", resolve(q, KEYS, store)[2] is False)

# The answer must name something that exists, or it is nothing.
def read_answer(reply, entries):
    cleaned = reply.strip().strip("\"'.` ").lower()
    for e in entries:
        if e.id.lower() == cleaned:
            return e
    for e in entries:
        if e.id.lower() in cleaned:
            return e
    return None


check("a real id is accepted", read_answer("volume_keys", KEYS).id == "volume_keys")
check("quotes and stops are tolerated", read_answer('"record".', KEYS).id == "record")
check("an invented id is refused", read_answer("the volume button", KEYS) is None, "a model invented a key")
check("NONE is refused", read_answer("NONE", KEYS) is None)

# ---------------------------------------------------------------- the wiring
def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


screen = code(SRC / "app/settings/dictate/MaRowsScreen.kt")
check("the picker resolves through the one rule", "MaKeySearch.resolve(" in screen, "its own matching")
check("the model is asked only when wanted", "if (!result.aiWanted" in screen, "asked on every query")
check("and only after he stops typing", "delay(700L)" in screen, "a call per keystroke")
check("and only for a real query", "query.trim().length < 3" in screen, "asked for one letter")
check("the guess is marked", '" (AI)"' in screen, "a guess that looks like a match")
check("picking teaches the memory", "MaKeySearch.learn(" in screen, "it can never stop needing the model")

ctrl = code(SRC / "dictate/DictateController.kt")
check("the ask reuses the ring", "requestRewordRaw(prompt, cheapest = true)" in ctrl, "a second HTTP path")
check("the cheap model is pinned", "preset.defaultChatModel ?: account.chatModel" in ctrl, "his expensive model")
check("a failure is silent", "}.getOrNull()" in ctrl, "an error box on a picker")

search = code(SRC / "dictate/MaKeySearch.kt")
check("the search is pure", "import android" not in search, "Android in the logic, untestable here")

# ---------------------------------------------------------------- findable by the words he uses
#
# He searched "feature row" and found nothing: the six keys were labelled "show row 3", and the
# phrase he uses for the thing was not the phrase they were named with. **A key nobody can find by
# its own name is a key that does not exist.**
order_src = (SRC / "dictate/MaFeatureOrder.kt").read_text()


def fold(t):
    return " ".join("".join(c if c.isalnum() else " " for c in t.lower()).split())


for n in range(1, 7):
    label = f"F{n}, show feature row {n}"
    check(f"F{n} is labelled with 'feature row'", f'"{label}"' in order_src, "still 'show row N'")
    hay = fold(label)
    check(f"F{n} matches 'feature row'", all(w in hay for w in fold("feature row").split()),
          f"searching what he calls it finds nothing: {hay}")
    check(f"F{n} still matches 'F{n}'", fold(f"F{n}") in hay, "the key face is what he sees first")
    check(f"F{n} matches 'row {n}'", all(w in hay for w in fold(f"row {n}").split()),
          "the old phrasing must keep working")

# ---------------------------------------------------------------- labels must fit the card
#
# "History, what you dictated" truncated to **"History, what you"** on the picker's two-line card —
# a question that answers nothing, sitting beside "Clipboard history". Two faults in one label: too
# long for the place it is drawn, and describing the transcript when the screen keeps the AUDIO.
#
# **A label is written for the narrowest place it appears**, not for the source file. So this is a
# check on every label, not a fix for one — seven others were longer than the one that broke.
import re as _re
_labels = _re.findall(r'^\s{4}[A-Z_0-9]+\("[a-z_0-9]+",\s*"([^"]+)"\)',
                      (SRC / "dictate/MaFeatureOrder.kt").read_text(), _re.M)
check("there are labels to check", len(_labels) > 40, str(len(_labels)))
LIMIT = 24
for _l in _labels:
    check(f"fits the card: {_l!r}", len(_l) <= LIMIT, f"{len(_l)} characters, truncates like the one he found")

check("the audio history says audio", '"Audio history"' in (SRC / "dictate/MaFeatureOrder.kt").read_text(),
      "names the transcript and hides the recordings")
# Against the LABELS, not the file: the comment recording the fix quotes the old label, and a check
# reading raw text cannot tell code from prose. It failed on the sentence explaining why it exists.
check("it is not the old sentence", "History, what you dictated" not in _labels,
      "truncates to a question")

print(f"key search, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
