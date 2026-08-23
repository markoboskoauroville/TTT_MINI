#!/usr/bin/env python3
"""
Test 1 for the language badge on the sending line.

The claim: **tapping it abandons the request and sends the same audio again in the new language** —
never two requests in flight, never a stale answer landing over the new one, and never a re-record.

    python3 scripts/test_sending_badge.py
"""

import itertools
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
    """The controller, as far as this feature is concerned."""
    # held: the request was thrown away and the audio kept.

    def __init__(self):
        self.state = "idle"
        self.language = "en"
        self.in_flight = None      # the audio being sent
        self.jobs = []             # live requests
        self.audio_recorded = 0

    def record_and_send(self, audio):
        self.audio_recorded += 1
        self.state = "transcribing"
        self.in_flight = audio
        self.jobs = [(audio, self.language)]

    def tap_badge(self, new_language, cancel_first=True):
        """
        The port of retranscribeInLanguage.

        `cancel_first` exists so the sabotage run can express the mistake this guards against —
        leaving the old request to land. The first version of this model reassigned `jobs`
        wholesale, so removing the cancellation changed nothing and the sabotage came back green.
        **A model that cannot express the bug cannot test for it.**
        """
        if self.in_flight is None or self.state not in ("transcribing", "held"):
            return False
        self.language = new_language
        if self.state == "held":
            # Set the language and stay held: he stopped it in order to choose, and sending as he
            # chooses would take the decision away at the moment he was making it.
            return True
        if cancel_first:
            self.jobs = []         # cancelled, not left to land
        self.state = "idle"
        self.state = "transcribing"
        self.jobs = self.jobs + [(self.in_flight, new_language)]
        return True

    def finish(self):
        self.state = "idle"
        self.jobs = []

    def tap_middle(self):
        """The port of toggleTranscriptionHold."""
        if self.in_flight is None or self.state not in ("transcribing", "held"):
            return False
        if self.state == "held":
            self.state = "transcribing"
            self.jobs = [(self.in_flight, self.language)]
        else:
            self.jobs = []          # thrown away
            self.state = "held"     # but the audio is kept
        return True

    def tap_x(self):
        self.state = "idle"
        self.jobs = []
        self.in_flight = None


# ---------------------------------------------------------------- the case it is for
w = World()
w.record_and_send("a.wav")
check("sending in the badge's language", w.jobs == [("a.wav", "en")], str(w.jobs))
check("the tap is accepted while sending", w.tap_badge("hr") is True)
check("one request, in the new language", w.jobs == [("a.wav", "hr")], str(w.jobs))
check("the same audio", w.in_flight == "a.wav", "the audio changed")
check("nothing was re-recorded", w.audio_recorded == 1, f"recorded {w.audio_recorded} times")

# ---------------------------------------------------------------- what it must refuse
w = World()
check("no tap when idle", w.tap_badge("hr") is False, "would send an old file out of nowhere")
w.record_and_send("a.wav")
w.finish()
check("no tap after it finished", w.tap_badge("hr") is False, "would resend something already answered")

# ---------------------------------------------------------------- walked
#
# Any sequence of taps, at any point, must leave exactly one request in flight and never more.
walked = 0
for taps in itertools.product(["en", "hr"], repeat=5):
    w = World()
    w.record_and_send("a.wav")
    for lang in taps:
        w.tap_badge(lang)
        walked += 1
        # NEVER TWO. A cancelled request left to land would overwrite the answer he asked for, and
        # which of the two won would depend on the network.
        check(f"{taps}: never two in flight", len(w.jobs) == 1, str(w.jobs))
        check(f"{taps}: always the current language", w.jobs[0][1] == w.language, str(w.jobs))
        check(f"{taps}: always the same audio", w.jobs[0][0] == "a.wav", str(w.jobs))
    check(f"{taps}: recorded once", w.audio_recorded == 1)


# ---------------------------------------------------------------- hold, choose, send
w = World()
w.record_and_send("a.wav")
check("the middle holds it", w.tap_middle() and w.state == "held", w.state)
check("nothing is in flight while held", w.jobs == [], str(w.jobs))
check("the audio is kept", w.in_flight == "a.wav", "the recording was thrown away with the request")
check("the badge sets the language while held", w.tap_badge("hr") and w.language == "hr")
check("and does NOT send", w.jobs == [], "sent before he had finished choosing")
check("the middle sends it", w.tap_middle() and w.state == "transcribing")
check("in the language he chose", w.jobs == [("a.wav", "hr")], str(w.jobs))
check("still one recording", w.audio_recorded == 1)

# X kills everything, from either state.
for reach_held in (True, False):
    w = World()
    w.record_and_send("a.wav")
    if reach_held:
        w.tap_middle()
    w.tap_x()
    check(f"held={reach_held}: X ends it", w.state == "idle" and w.jobs == [], str(w.state))
    check(f"held={reach_held}: X lets the audio go", w.in_flight is None, "still holding a recording")
    check(f"held={reach_held}: nothing can resume it", w.tap_middle() is False, "resumed after a kill")

# Walked: any sequence of middle taps and badge taps leaves at most one request in flight, and never
# one while held.
import itertools as _it
for seq in _it.product(["mid", "en", "hr"], repeat=5):
    w = World()
    w.record_and_send("a.wav")
    for act in seq:
        if act == "mid":
            w.tap_middle()
        else:
            w.tap_badge(act)
        walked += 1
        check(f"{seq}: never more than one in flight", len(w.jobs) <= 1, str(w.jobs))
        check(f"{seq}: held means nothing in flight", not (w.state == "held" and w.jobs), str(w.jobs))
        check(f"{seq}: the audio survives every tap", w.in_flight == "a.wav", "lost the recording")
    check(f"{seq}: recorded once", w.audio_recorded == 1)

# ---------------------------------------------------------------- the wiring
def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


ctrl = code(SRC / "dictate/DictateController.kt")
check("the in-flight audio is kept", "inFlight = InFlight(audioFile" in ctrl, "nothing to resend")
check("the resend exists", "fun retranscribeInLanguage" in ctrl, "the badge would do nothing")

# INSIDE the function, not anywhere in the file.
#
# The first version of these three searched the whole of DictateController, and every one of them
# passed for the wrong reason: `cancelTranscription` a few lines below contains the same state guard
# and the same `transcribeJob?.cancel()`. Deleting the guard from the resend left the test green.
# **A check that can be satisfied by a different function is not checking the function.**
start = ctrl.index("fun retranscribeInLanguage")
depth, i, body_start = 0, ctrl.index("{", start), None
for j in range(i, len(ctrl)):
    depth += (ctrl[j] == "{") - (ctrl[j] == "}")
    if depth == 0:
        body = ctrl[i:j]
        break
# The guard is now a cast rather than an `!is` check, because the function needs the state's `held`
# flag as well as its type. Same refusal — a non-Transcribing state gives null and returns.
check("it refuses unless sending", "as? UiState.Transcribing ?: return" in body,
      "would fire from idle")
check("it cancels the old job", "transcribeJob?.cancel()" in body, "two answers racing")

# The hold, read from its own function body for the same reason as above.
hstart = ctrl.index("fun toggleTranscriptionHold")
hdepth, hi = 0, ctrl.index("{", hstart)
for j in range(hi, len(ctrl)):
    hdepth += (ctrl[j] == "{") - (ctrl[j] == "}")
    if hdepth == 0:
        hold_body = ctrl[hi:j]
        break
check("the hold keeps the audio", "inFlight = null" not in hold_body.split("held")[0],
      "throws the recording away with the request")
check("the hold cancels the request", "transcribeJob?.cancel()" in hold_body, "two things running")
check("releasing sends the same file", "current.audioFile," in hold_body, "sends something else")
check("the badge does not send while held", "if (state.held) {" in ctrl, "sends before he has chosen")
check("it checks the file is still there", "audioFile.length() == 0L) return" in body,
      "would send an empty file")

ui = code(SRC / "dictate/ui/DictateSmartbarUi.kt")
# The badge is derived from the observed preference now, not from a one-shot MaLanguage.badge().
# That IS the fix for the dead badge, so the check has to look for the new spelling — the old string
# would have kept passing on exactly the code he reported as broken.
check("the badge is on the sending line", '"[$badge]"' in ui, "no badge while sending")
check("and it is tappable", "retranscribeInLanguage(context, MaLanguage.active())" in ui,
      "readable but not changeable, which is the frustrating half")
check("it cycles the same way the key does", "MaLanguage.cycleMode(context)" in ui,
      "two ideas of what next means")

# ---------------------------------------------------------------- the three bugs he found
ui = code(SRC / "dictate/ui/DictateSmartbarUi.kt")

# 1. The badge was read once, so it never changed on screen. Observed now.
check("the badge is observed", "maLanguageMode.collectAsState()" in ui,
      "read once — the tap works and the letters never change")
check("the badge is derived from what the tap writes", 'if (languageMode == MaLanguage.EN)' in ui,
      "two sources for one letter")

# 2. Brackets, and room between three controls with asymmetric costs.
for part in ('"[$badge]"', '"[\\u00D7]"', '"["'):
    check(f"bracketed: {part}", part in ui, "not in brackets")
check("room between the controls", ui.count("horizontal = 12.dp") >= 3,
      "packed tight, and a thumb aimed at hold lands on X")

# 3. Release must not re-run the gate, and must send the copy.
check("release skips the gate", "gate = false," in hold_body,
      "the trimmer judges already-trimmed audio silent, which reads as a cancel")
check("the hold keeps its own copy", 'File(context.cacheDir, "dictate_held_send.wav")' in hold_body,
      "the trimmer can rewrite the file under it")

print(f"sending badge, test 1: {checks} checks, {len(failures)} failed ({walked} taps walked)")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
