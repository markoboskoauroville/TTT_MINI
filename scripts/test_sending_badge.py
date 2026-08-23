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
        if self.in_flight is None or self.state != "transcribing":
            return False
        self.language = new_language
        if cancel_first:
            self.jobs = []         # cancelled, not left to land
        self.state = "idle"
        self.state = "transcribing"
        self.jobs = self.jobs + [(self.in_flight, new_language)]
        return True

    def finish(self):
        self.state = "idle"
        self.jobs = []


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
check("it refuses unless sending", "_state.value !is UiState.Transcribing) return" in body,
      "would fire from idle")
check("it cancels the old job", "transcribeJob?.cancel()" in body, "two answers racing")
check("it checks the file is still there", "audioFile.length() == 0L) return" in body,
      "would send an empty file")

ui = code(SRC / "dictate/ui/DictateSmartbarUi.kt")
check("the badge is on the sending line", "text = MaLanguage.badge()" in ui, "no badge while sending")
check("and it is tappable", "retranscribeInLanguage(context, MaLanguage.active())" in ui,
      "readable but not changeable, which is the frustrating half")
check("it cycles the same way the key does", "MaLanguage.cycleMode(context)" in ui,
      "two ideas of what next means")

print(f"sending badge, test 1: {checks} checks, {len(failures)} failed ({walked} taps walked)")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
