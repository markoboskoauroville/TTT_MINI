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
# Split on the DEFINITION, not the first mention. `captureNow` calls `captureToLog` and now appears
# above it, so splitting on the bare name read the wrong function — the check was looking inside the
# caller for a guard that lives in the callee. **A name is not a location when more than one thing
# uses it.**
_body = reader.split("private fun captureToLog(")[1][:900]
check("a failed write never stops the reading", "runCatching {" in _body,
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

# AND IT MUST BE IN DEFAULT, or it can never be shown.
#
# parse() returns the stored order plus DEFAULT and nothing else. The entry existed, the icon
# existed, the route existed and was registered, it all compiled — and the menu item was simply
# absent, with nothing anywhere to say why. **Membership of a list is not something an
# exhaustiveness check looks at.**
order_src = (SRC / "app/settings/MaSettingsOrder.kt").read_text()
check("the entry exists", 'CLOUD_READER("cloud_reader"' in order_src, "no menu entry at all")
check("and is in DEFAULT", "MaSettingsEntry.CLOUD_READER," in order_src,
      "parse can never return it, so the menu item never appears")

# ---------------------------------------------------------------- renaming a chat
#
# He renames chats. A key taken from the first text then points at a file under the old name, and the
# conversation splits in two. The rename is his; the split would be mine.
def continuation_of(screen, logs, tail_chars=2000):
    """logs is a list of (name, text)."""
    now = norm(screen)
    if len(now) < 40:
        return None
    probe = " ".join(now.split()[:12])
    if len(probe) < 40:
        return None
    for name, text in logs:
        if probe in norm(text[-tail_chars:]) or probe in norm(text):
            return name
    return None


LONG = ("we were discussing the door shot and whether the close up belongs before "
        "the wide or after it in the sequence")
logs = [("2026-09-04--door-shot.txt", "earlier text " + LONG + " and more after that")]
check("a renamed chat continues its log", continuation_of(LONG, logs) == "2026-09-04--door-shot.txt",
      "the conversation would split in two")
check("a different chat starts a new one",
      continuation_of("a completely different conversation about the colour grade and nothing else at all", logs) is None,
      "one chat would be appended to another")
check("a short screen never matches", continuation_of("hello", logs) is None,
      "two chats that both start with a greeting would merge")
check("no logs means a new log", continuation_of(LONG, []) is None)
check("an unreadable log is not a match", continuation_of(LONG, [("x.txt", "")]) is None,
      "an empty file would swallow every chat")

# The direction of the risk, asserted: a miss costs a second file, a wrong match costs a merge. The
# short-screen guard is what keeps misses the common failure.
for short in ("ok", "yes", "thanks", "one two three"):
    check(f"{short!r} is too short to match", continuation_of(short, logs) is None)

cloud = code(SRC / "dictate/MaCloudLog.kt")
check("the matcher exists", "fun continuationOf(" in cloud, "a rename would split the log")
check("it matches on the tail", "takeLast(tailChars)" in cloud,
      "anywhere-in-file would match a quotation of one chat inside another")
check("the reader asks it before making a file", "MaCloudLog.continuationOf(" in reader,
      "it would open a second file for a renamed chat")
check("a continued log seeds what it last saw", "logSeen = runCatching { file.readText()" in reader,
      "opening a logged chat would append its visible screen a second time")

# ---------------------------------------------------------------- the screen opens at all
screen_src = (SRC / "app/settings/dictate/MaCloudLogScreen.kt").read_text()
check("the screen is not doubly scrollable", "scrollable = false" in screen_src,
      "a LazyColumn inside FlorisScreen's scroller throws the moment the screen opens")
check("it is named for what it reads", '"Claude.ai reader"' in screen_src, "still called the cloud")

# ---------------------------------------------------------------- the Log key
#
# Capture used to happen only while the reader was WATCHING, so a chat he read with his eyes was
# never kept. This is the press that keeps it.
order_src2 = (SRC / "dictate/MaFeatureOrder.kt").read_text()
check("the key exists", 'CLOUD_LOG("cloud_log"' in order_src2, "no way to log by hand")
_row = code(SRC / "dictate/ui/MaFeatureRow.kt")
check("it is drawn", "MaFeatureKey.CLOUD_LOG ->" in _row, "in the catalogue and not on the keyboard")
check("it goes through the one capture path", "MaReader.captureNow(context)" in _row,
      "a second capture would disagree with the reader about what is already logged")
check("captureNow reuses captureToLog", "captureToLog(context, screen)" in
      reader.split("fun captureNow")[1][:900],
      "two ideas of what already-logged means, in one file")
check("it says something", "MaMessage.show(MaReader.captureNow" in _row,
      "a press that writes a file and says nothing looks like a press that did nothing")
check("it names the log it wrote to", "MaCloudLog.titleOf(file.name)" in reader,
      "the only useful thing a message can say is WHICH log — that is what shows a rename was followed")
check("it refuses politely when switched off", '"Turn the Claude.ai reader on in settings"' in reader,
      "a silent no-op")
check("and when the service is off", "Turn on the accessibility service" in
      reader.split("fun captureNow")[1][:900], "it would log a blank screen")

print(f"cloud log, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
