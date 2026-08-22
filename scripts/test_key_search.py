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

print(f"key search, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
