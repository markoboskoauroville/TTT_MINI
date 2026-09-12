#!/usr/bin/env python3
"""
Test 1 for the cloud reader: keys, appending, sentences.

    python3 scripts/test_cloud_log.py
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


def norm(t):
    return " ".join("".join(c if c.isalpha() else " " for c in t.lower()).split())


def key_for(first):
    w = norm(first).split()[:8]
    return "-".join(w) if w else "untitled"


def tail(last, now):
    a, b = norm(last), norm(now)
    if not b or a == b:
        return None
    if a and b.startswith(a):
        n = len(b[len(a):].split())
        if n == 0:
            return None
        raw = now.strip().split()
        return now.strip() if n >= len(raw) else " ".join(raw[-n:])
    return now.strip() or None


def sentences(log):
    out = []
    for para in log.split("\n\n"):
        t = para.strip()
        if not t:
            continue
        start = 0
        for i, c in enumerate(t):
            if c in ".!?":
                nxt = t[i + 1] if i + 1 < len(t) else None
                if nxt is None or nxt in " \n":
                    s = t[start:i + 1].strip()
                    if s:
                        out.append(s)
                    start = i + 1
        rest = t[start:].strip()
        if rest:
            out.append(rest)
    return out


# ---------------------------------------------------------------- the key
check("a key is the first words", key_for("Let us talk about the door shot") == "let-us-talk-about-the-door-shot")
check("it survives a re-render", key_for("Hello there 12:00") == key_for("Hello there 12:01"),
      "a ticking clock would start a second log for one chat")
check("it survives re-wrapping", key_for("one two\nthree") == key_for("one  two three"))
check("two different chats differ", key_for("about the door") != key_for("about the window"))
check("empty text still gets a name", key_for("") == "untitled", "a file with no name")
check("it is capped at eight words", len(key_for(" ".join(str(i) + "x" for i in range(30))).split("-")) <= 8)

# ---------------------------------------------------------------- appending
check("nothing new appends nothing", tail("the answer", "the answer") is None,
      "the file would double on every poll")
check("a growing answer appends the tail", tail("the answer", "the answer goes on") == "goes on")
check("a different screen appends all of it", tail("old", "something else") == "something else")
check("a clock does not count as growth", tail("done 12:00", "done 12:01") is None,
      "a minute of silence would append the screen again")

# A streamed answer must end up in the file exactly once, in order.
stream = ["a", "a b", "a b c", "a b c d"]
log, seen = [], stream[0]
for f in stream[1:]:
    t = tail(seen, f)
    if t:
        log.append(t)
    seen = f
check("a stream lands once and in order", " ".join(log) == "b c d", str(log))

# ---------------------------------------------------------------- sentences to tap
s = sentences("One. Two! Three?\n\nFour.")
check("split on . ! and ?", s == ["One.", "Two!", "Three?", "Four."], str(s))
check("a decimal does not split", sentences("It cost 3.50 today.") == ["It cost 3.50 today."],
      str(sentences("It cost 3.50 today.")))
check("a trailing fragment survives", sentences("Done. And then") == ["Done.", "And then"])
check("blank input gives nothing", sentences("   ") == [])
check("paragraphs are kept apart", len(sentences("A.\n\nB.")) == 2)


def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


log_kt = code(SRC / "dictate/MaCloudLog.kt")
reader = code(SRC / "dictate/MaReader.kt")
screen = code(SRC / "app/settings/dictate/MaCloudLogScreen.kt")
prefs = code(SRC / "app/AppPrefs.kt")

check("the module is pure", "import android" not in log_kt, "cannot be walked without a phone")
check("capture runs from the reader's poll", "captureToLog(context, now)" in reader, "a second poll")
# Compared on the CALL, both of them, and guarded. The first version compared the first appearance of
# each string — and `captureToLog` appears in its own function definition further down, so the
# comparison was against a definition rather than a call and the sabotage passed.
_cap = reader.find("captureToLog(context, now)")
_tail = reader.find("val tail = newTail(seen, now)")
check("before the cue filter", 0 <= _cap < _tail,
      "the log would miss what the reader chose not to speak")
check("it is off by default", 'maCloudLogEnabled = boolean(\n            key = "dictate__ma_cloud_log",\n            default = false,' in prefs,
      "it writes files without being asked")
check("a failed write never stops the reading", "runCatching {" in reader.split("captureToLog")[1][:600],
      "a full disk would end the reading")
check("nothing in the capture path deletes", "delete()" not in log_kt, "a wrong key would cost text")

check("tapping reads from that sentence on", "sentences.drop(i)" in screen, "it would read one line")
check("it uses the one reader", "MaReader.speakText(" in screen, "a second reading engine would drift")
check("speakText exists", "fun speakText(" in reader, "the screen calls something that is not there")
check("reading a log is not watching", "watching = false" in reader.split("fun speakText")[1][:400],
      "a file does not grow, and it would wait for ever")
check("and clears the sentence memory", "MaReadMemory.clear()" in reader.split("fun speakText")[1][:400],
      "a passage heard live this morning would be skipped this afternoon")

# ---------------------------------------------------------------- the route, which is what crashed
#
# Build 369 shipped this feature with a route that had @Serializable and no @Deeplink.
# `composableWithDeepLink` does `requireNotNull` on that annotation, so the nav graph threw while the
# app was LAUNCHING. It compiled, every suite passed, CI was green, and the keyboard would not open.
#
# `check_route_deeplink` in verify.py is the real gate — it refuses the build. This is the second
# lock, here rather than there because somebody reading THIS feature's test should see what it cost.
# Comments stripped: one sitting between the annotations breaks the run the pattern walks.
routes = re.sub(r"^\s*//.*$", "", (SRC / "app/Routes.kt").read_text(), flags=re.M)
_m = re.search(r"((?:\s*@\w+(?:\([^)]*\))?\s*)*)\bobject MaCloudLog\b", routes)
check("the route exists", _m is not None, "the screen is unreachable")
check("and carries @Deeplink", _m is not None and "@Deeplink" in _m.group(1),
      "the app will not start — this is the exact shape of the 369 crash")
check("and @Serializable", _m is not None and "@Serializable" in _m.group(1),
      "navigation cannot address it")
check("it is registered", "composableWithDeepLink(Settings.MaCloudLog::class)" in routes,
      "a route nothing routes to")
check("the settings entry points at it", "MaSettingsEntry.CLOUD_READER -> Routes.Settings.MaCloudLog"
      in (SRC / "app/settings/MaSettingsOrderScreen.kt").read_text(),
      "a menu entry that goes nowhere")

print(f"cloud log, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
