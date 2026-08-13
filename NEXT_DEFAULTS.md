# Defaults to ship — note for the next build

Not code. A record of what Marko has actually arrived at on his own phone, so the next fresh
install starts there instead of at whatever was guessed. Everything below is read off his
screenshots of build 85, not proposed.

## Paste timing

He has tuned these by hand against real fields and settled on:

| Step | Value |
|---|---|
| 1. Before select all | **0** |
| 2. Before delete | **500** |
| 3. Before paste | **0** |

Currently all three default to 200. **Ship 0, 500, 0.** Confirmed by Marko: it is what works, tuned
against real fields on his phone over weeks.

It contradicts the advice printed on that screen, which tells the user to raise step 3 first. The
advice is the thing that is wrong, not the numbers — measurement beats reasoning, and the numbers
were measured. The wait that actually matters is **after asking the field to select everything**:
that is the step where the field has real work to do and the app is waiting on another process. The
other two are waits on this app's own calls, which have already returned.

So when shipping these, fix the on-screen text as well, or the app will ship advice that argues with
its own defaults.

## Settings order

The order he has dragged into place:

1. Edit settings order
2. Paste timing
3. Switchboard
4. Magic button
5. Feature row
6. API keys
7. Recording
8. Custom mappings
9. Output
10. History
11. Recovered recordings
12. Learn my words
13. Word predictions

Then the inherited FlorisBoard block: Keyboard, Smartbar, Gestures, Typing, Clipboard.

Note that **Switchboard sits third, not first**, although it was shipped first. He moved it below
Paste timing. Ship his order, not the alphabetical or the logical one.

## Magic button terms worth shipping

Confirmed working against real screens, with the app he uses them in:

**Claude (com.anthropic.claude)** — `Send`, `Stop responding`, `Copy message`.
Also available: `New chat`, `Share`, `Narrate`, `Retry message`, `Add to chat`, `Scroll to bottom`.

**imgtoimg.ai in Firefox** — `Use Image URL`, `Upload Image`, `Generate Images`, `Re-edit`,
`Regenerate`, `Download`.

The first three already ship. They belong to no app on purpose, and the same reasoning applies
below.

### Ship these four as defaults

| Term | Label | Why |
|---|---|---|
| `Copy message` | copy | already shipping |
| `Stop responding` | stop | already shipping |
| `Send` | send | already shipping |
| `Use Image URL` | url | **add this one** |
| `Add URL` | +url | **add this one** — confirms the dialog the one above opens |
| `Generate Images` | gen | **add this one** — starts the render |

`Use Image URL` and `Add URL` are one workflow rather than two keys: the first opens the dialog, he
pastes, the second confirms. Shipping one without the other leaves him reaching for the screen
halfway through, which is the reaching this feature exists to remove.

`Generate Images` completes that workflow: use URL, paste, add, generate. It is stored **without the
number**, although the button reads "Generate Images 2" — the number is the coin cost and changes
with the model, so a term carrying it would break the day he switches away from Nano Banana Pro.
Phrase matching finds the characters inside the longer label, which is exactly what that behaviour
is for.

All three are phrases and all three are safe. "Use Image URL" cannot fire on "Upload Image" beside it, and
"Add URL" cannot fire on the dialog's own title "Add Image URL" — phrase matching looks for the
characters in sequence, and "add image url" does not contain "add url".

Unscoped, like the other three. It belongs to a **web page** rather than an app, so scoping it to
`org.mozilla.firefox` would break the moment he opens the same site in another browser — and the
label is specific enough that it will not collide with anything else he uses.

The remaining imgtoimg terms — `Upload Image`, `Generate Images`, `Re-edit`, `Regenerate`,
`Download` — are his second daily workflow and the obvious candidates after that, but they are his
to choose rather than mine to assume. Four keys is a row he can read; nine is a row he has to
search.

## One thing not to forget

`Open menu, new feature available` was in an earlier Claude dump and is a trap: the label changes
when Anthropic stops showing the badge. Never ship a term containing words like "new feature
available" — match on the stable part or not at all.

---

# The pinned row — Marko's idea, worth building

He noticed the strip beside the pin at the bottom of the keyboard is empty, and it always has been.
It exists to hold the pin and nothing else, so it is free height the app already pays for.

**"It's government land. Let's use it."**

## What it is

A row at the very bottom, below the feature rows, permanently present because the pin lives there
anyway. Anything that currently appears and disappears above the keys can move into it instead.

## What should move there

**The suggestion row.** This is the fix for the flicker properly, rather than the switch that
merely keeps its height. Suggestions currently sit above the keys, so they push everything down when
they arrive; from the pinned row they change nothing above them because there is nothing above them
to move.

**The recording bar.** It appears on top and shoves the whole keyboard down mid-sentence. Marko:
*"put recording down, not to jump up."* He is right, and it is the same argument — a bar that
appears where there is already space costs nothing, and one that appears above the keys costs every
key its position.

## Why this is worth doing rather than nice to have

Every complaint about things jumping while he types has the same cause: transient rows are drawn
**above** the keys, so appearing and disappearing moves the keys. Anything drawn **below** them
cannot. This is not a workaround for the flicker — it is the actual fix, and the switch shipped in
build 85 is the workaround.

## Terms found in the Android photo picker

For picking a reference image without touching the screen:

| Term | Scope |
|---|---|
| `Done` | **must be scoped** to `com.google.android.photopicker` |
| `Preview` | same |
| `Deselect all` | same |

`Done` is the first term where scoping is not optional. Every app has one, so unscoped it would fire
in mail, in settings, anywhere. The six shipped defaults are specific enough to be safe unscoped;
this one is not, and the difference is worth stating rather than discovering.

---

# Magic keys that know whether their button is there

Marko's idea, and the better half of it is straightforward.

## The light-up

A magic key is grey when the button it presses is not on screen, and lit when it is. Same language
the buckets already use: dim means nothing there, lit means ready. He learns it once and reads it
everywhere.

This is worth more than it looks. A magic key currently gives no sign whether it will work until it
is pressed, so a press that finds nothing is indistinguishable from a broken key — which is exactly
the confusion that cost several builds earlier. A key that says *I can see it* before it is touched
removes that whole class of doubt.

**Feasible as described.** The accessibility service already walks the tree for the dump. The work
is to run that walk on window-content changes, collect which of his terms are present, and expose it
as state the row observes — the same shape as `MaScreenTargets.Learn.state`.

**The cost is the catch, and it needs designing rather than assuming.** A tree walk on every content
change is a walk many times a second in a scrolling app, and this runs in the keyboard's process on
a phone in someone's hand. It needs: throttling to no more than a few times a second, skipping
entirely while no magic key is drawn, and stopping when the screen has not changed. Get this wrong
and the feature is a battery complaint rather than a nicety.

## The auto-raise

*"When it recognizes the button, it just jumps up."*

**Partly possible, and the limit is Android's rather than ours.** An input method may not simply show
itself; it is shown when a text field takes focus. In the photo picker there is no text field, which
is precisely the case he wants it for.

What does exist is the pin, already built: when pinned, `onEvaluateInputViewShown` returns true and
the keyboard stays up with no editor at all. So the achievable version is **stay up**, not **jump
up** — pinned, the keyboard is already there when the picker opens, and the magic keys light as
their targets appear.

Whether an unpinned keyboard can be raised on demand needs testing on his phone rather than
reasoning about, since OEMs differ. Try `requestShowSelf` and believe the device, not the
documentation.

## Order to build

1. The light-up, throttled. It is self-contained, it makes every magic key honest, and it is the
   part he will feel immediately.
2. Then test whether auto-raise works at all on a Nothing Phone 2a, and only then decide whether it
   is a feature or a note saying "pin the keyboard first".

---

# The zone keys grow up: independent of the keyboard

Marko circled the `1` key and said it plainly: *"it's not child of the keyboard, it's now grown-up
kid who is independent."*

## What is wrong today

The zone keys switch parts of the typing keyboard on and off. They assume the keyboard is there.
Pressing `1` when the keyboard is collapsed asks for a number row on a keyboard that is not drawn,
so it does nothing visible and looks broken.

## What he wants

Each zone key brings up **its own row, on its own**, whether or not the letters are showing. Press
`1` and a number row appears — no keyboard needed. The zone is a thing in itself, not a switch on
something else.

This pairs with the pinned row note above: transient rows drawn below the keys cost nothing, and a
number row summoned without the alphabet is exactly that. It also makes the keys useful in the case
Marko keeps hitting — a screen with no text field, where the keyboard is collapsed and only the
magic keys matter.

## The labels: a letter for what the zone is

He asked for mnemonics on the key faces, because three bare numerals say nothing about what each one
opens:

| Key | Face | What it opens |
|---|---|---|
| Zone 1 | **1N** | **N**umbers — the digit row |
| Zone 2 | **2K** | **K**eys — the letter keyboard |
| Zone 3 | **3P** | **P**aste — the copy row |

The number keeps the position, the letter says the contents. Both fit inside the little keyboard
glyph drawn in build 72, so this is a label change rather than a new design — the glyph already has
room, since the numeral was sized for one character and there is space beside it.

Worth keeping the numeral first, in that order. He has learned the positions by number, and a face
reading `N1` would be a different key to relearn rather than the same key better labelled.
