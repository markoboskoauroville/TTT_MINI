# Volume keys — the specification

**This is the contract. If the behaviour ever differs from this document, the code is wrong, not the
document.**

Implemented in `MaVolumeKeys.kt`. Wired in `FlorisImeService.onKeyDown` / `onKeyUp`, and nowhere
else.

---

## There is no setting

**The volume keys cannot be switched off.** There is no preference, no switch in the switchboard, no
entry in the gestures screen. They are live whenever the keyboard is on screen.

There was a setting, and it is why this was dead for five builds. It defaulted on, it sat in a list of
thirteen switches that had just become draggable, and one stray touch turned it off silently. He lost
days believing the feature had been deleted; three rounds were spent reading a handler that was
correct the whole time.

**A control that can silently disable the thing somebody uses most is not a feature, it is a
trapdoor.** Do not add it back. If some future person cannot bear the keys being taken, the answer is
to make the *keyboard* not take them when it is not on screen — which is already the behaviour.

---

## The behaviour

| Press | State | What happens |
|---|---|---|
| **Volume UP**, quick tap | idle | start recording |
| **Volume UP**, quick tap | recording | stop, and send to transcription |
| **Volume DOWN**, quick tap | recording | stop → transcribe → **when the words land, press Send on screen** |
| **Volume DOWN**, quick tap | not recording | press Send on screen |
| **Either**, held | any | the real system volume, repeating, with the system's own bar |

### While something is being read, the taps drive the reading

| Press | What happens |
|---|---|
| **Volume DOWN**, quick tap | **next** sentence |
| **Volume UP**, quick tap | **previous** sentence |
| **Either**, held | still the real volume |

**Down goes forward.** The text moves down the screen as it is read, so down is where the next
sentence is, and every scroll on the phone already trains the hand that way. Mapping up to "next"
because up is bigger would impose a rule from arithmetic on a rule from movement.

**Nobody dictates into a screen they are listening to.** So while a reading is in progress a tap
means skip, not record. The keys are not remapped by a setting; they follow what is on screen, and a
control that means the obvious thing for the situation in front of him needs no mode and no memory.

**One step, always. No replay window, no two-step.** A skip key skips. An earlier version restarted
the current sentence when he was already part-way into it — the media-player convention — and he was
clear that it is wrong here: to hear a sentence again he presses up then down, which is two
deliberate presses and takes less thought than a key whose meaning changes with how long he has been
listening.

**A control that does two different things depending on timing is one that has to be predicted.** At
speed, predictable beats clever.

The only asymmetry is the first sentence, where there is no previous one: up restarts it.

"Quick tap" means released in under **500 ms**. Held means longer. Once held, the volume repeats every
**111 ms** until the finger lifts.

---

## The rules that make it work

**The decision happens on RELEASE, never on press.** At the moment of pressing there is no way to
know which of the two he meant. Nothing happens on the way down except starting the clock.

**A press that became the volume does nothing else on release.** Otherwise a hold would change the
volume *and* start a recording.

**The send is ARMED, not performed.** When volume down stops a recording, the send cannot happen in
the key handler — the text is not in the field yet, and a send fired there sends an empty message
that nobody notices until they read the other end. The handler sets `DictateController.sendAfterCommit`
and the transcription path fires it once the words are down.

**The send is disarmed on every abandoned path** — cancelled recording, cancelled transcription, empty
transcript. Otherwise the arming survives and the *next* ordinary dictation sends itself. A stray send
is worse than a missed one: it puts words in front of somebody.

**Held-key repeats from the system are swallowed.** The repeating is done here on our own clock, and
both running would race.

**Its own coroutine scope, on Main.** The service's lifecycle scope would carry a repeat across a
keyboard that has been hidden.

---

## The two invariants

- **Never both.** No press produces an app action *and* a volume change.
- **Always one.** No press does nothing at all.

Test them across the whole range of hold lengths, not at three sample points. They are the feature.

---

## The reading outlives the keyboard

Nothing stops a reading except **the reader key, the ✕, or the passage ending.** Not a new text
field, not a notification, not a dialog, not switching views, not the keyboard being collapsed by the
system.

`onStartInputView` used to stop it, and that was wrong: it fires for all of those, and every one of
them killed a reading mid-sentence. **A reading is a task he started deliberately and can end with one
press. Interrupting it because a notification arrived is not caution, it is losing his place for
him.**

## Diagnosing

Every press logs before any decision is taken. Open **log** in settings:

```
vol  up down
vol  up tap — record / stop
```

- **No `vol` line at all** → the events are not reaching the keyboard. The handler is irrelevant;
  look at how the IME receives hardware keys.
- **`down` but no tap line** → the release never arrived, or it was treated as a hold.
- **`held — passing to the system volume`** → it decided this was a hold. If that is wrong, `HOLD_MS`
  is too short for his thumb.
