# Sequencer — parked, not abandoned

Marko asked for this and then asked to park it so the scroll key could ship. This is the note so the
idea is not lost and the next session does not have to re-derive the design or the obstacle.

## What it is

A `SEQ` key that sits in a row and runs the other keys of that row, left to right, one per press.
Press once, the first key fires. Press again, the second. It never fires itself.

Marko's own description of why: *"It's like a manual macro runner. Baba is doing that manual, but one
after the other, so I don't need to look up."* The value is not automation — it is that his eyes and
his thumb stop having to find a different key each step. One position, pressed repeatedly.

## Behaviour to build

- The index advances on each press and wraps at the end of the row.
- It must be visible which step is next, or the key is unusable after any interruption. The simplest
  honest answer is the key's own label: `S1`, `S2`, `S3`.
- It resets when the keyboard closes, or after a pause. A half-finished sequence resumed an hour
  later fires the wrong step into the wrong app.
- If a row holds two SEQ keys, both must not drive the same index.

## Why it was not built in this round

The obstacle is real and worth stating plainly. Every key's action is written inline inside
`MaFeatureRow`'s `when` — 17 branches inside a 558-line composable — and each branch closes over
composition state: `prefs`, `scope`, `context`, `keyboardManager`, `capturedSlots`, `magicTargets`,
`macroSlots`. There is no function anywhere that says "run this button". SEQ needs exactly that.

Building it therefore means extracting every branch's action into something callable, used by both
the key's own `onClick` and by SEQ. That is the right refactor — it is the same duplication that
already caused the clipboard paste sequence to exist twice and drift — but it touches every key on
the row at once, which is not a change to make in the same build as a new feature.

**Do the extraction as its own commit, with no new behaviour in it.** Ship it, confirm every key
still works, and only then add SEQ on top. A refactor and a feature in one build means a bug in
either one looks like a bug in the other.

## Where to start

`app/src/main/kotlin/dev/patrickgold/florisboard/dictate/ui/MaFeatureRow.kt`, the `when (button)`
inside `rows.forEach`. The shape wanted is roughly:

```kotlin
// Built once per composition from the state the actions need, then both the keys and SEQ call it.
class MaButtonActions(/* prefs, scope, context, keyboardManager, capturedSlots, ... */) {
    fun run(button: MaRows.Button)
}
```

Keys whose action is not a single call — backspace repeats on hold, the zone keys toggle
preferences, the mic key starts and stops a recording — need deciding case by case. A recording key
inside a sequence is probably wrong and should be skipped rather than fired.
