# THE LIST

Marko picks by number. When a feature is tested and working, it is struck off and the list is
reissued. This is the working menu — everything below this section is the detail behind it.

**Ready to ship, no design left**

| # | Feature | Size |
|---|---|---|
| 1 | Paste timing defaults: 0, 500, 0 — and fix the on-screen advice that contradicts them | tiny |
| 2 | Settings order defaults, as he arranged them | tiny |
| 3 | Three more magic defaults: `Use Image URL`, `Add URL`, `Generate Images` | tiny |
| 4 | Check a key exists **before** recording, with a button straight to the API keys screen | small |
| 5 | Restore keys — the button missing beside Back up | small |
| 6 | Re-transcribe from the History screen, with a language beside it | small |
| 7 | Switchboard: rename to Magic Finger row, sand for tappable names, an icon per line | small |

**Needs a decision or a permission flow**

| # | Feature | Size |
|---|---|---|
| 8 | Grant all-files access moves into setup, beside the microphone | medium |
| 9 | Accessibility onboarding: the App info → ⋮ → Allow restricted settings walk | medium |
| 10 | Settings backup / restore / reset, same folder as the keys | medium |
| 11 | Strip Gemini and Anthropic; one AssemblyAI key for everything | medium |

**New rows and keys**

| # | Feature | Size |
|---|---|---|
| 12 | The pinned row: move the suggestion row and recording bar **below** the keys | medium |
| 13 | Zone keys independent of the keyboard, faces `1N` `2K` `3P` | medium |
| 14 | The new copy row: skip by word, locking Shift that selects, cut/copy/paste/all/delete | medium |
| 15 | Magic keys light up when their button is on screen, grey when it is not | medium |
| 16 | View switching: `?123` cycles, long press lists, Keyboard views screen, ticked quick-cycle | large |

**The big ones**

| # | Feature | Size |
|---|---|---|
| 17 | TRANSCRIPTION settings group — move everything about text into one place | medium |
| 18 | Shape: prompt library, 20 slots, model radio, through the LLM Gateway | large |
| 19 | Croatian suggestions that are actually good — dictionary work, not a switch | large |
| 20 | Wire the ONNX predictor to a real model (contract already built, build 84) | large |
| 21 | Configurable long-press symbols on `Z X C V B N M` — touches upstream layout code | large |
| 22 | The sequencer — see SEQUENCER_PARKED.md; needs the action-extraction refactor first | large |

**Notes on order**

- 17 before 18, or Shape lands in the wrong place and moves twice.
- 8 before 5 and 10 — those need the permission.
- 12 and 13 together; the number row's icon is the `1N` glyph.
- 21 and 22 alone, each in its own build.

---

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

---

# The big batch — everything from the 14 August session

Written from Marko's screenshots and dictation. No code. Ordered by what unblocks what, not by the
order he said them.

---

## 1. Re-transcribe from the History screen

**What is wrong.** The top entry in his history reads *"Transcription failed — re-transcribe to
retry"* and there is no button to do it. The instruction names an action the screen does not offer.

**What to add**, per row: a re-transcribe control, and a language beside it.

The language matters more than it looks and he has hit it twice now. If the app was set to Croatian
while he spoke English, the transcript is wrong and re-running it in the same language reproduces
the same wrong answer. The keyboard's history panel already grew a language badge for exactly this
in build 46; the settings History screen never did. Same fix, other screen.

**Cheap, because the machinery exists.** `retranscribeHistoryEntry` already resolves the language
through `MaLanguage.active()` at the moment it sends, so setting the language and pressing replay is
all it takes. This is wiring, not new capability.

**Failed rows first.** They are the ones with nothing to lose and the ones carrying the instruction.

---

## 2. Restore keys, and move Grant all-files access into setup

**Restore is missing.** The screen has BACK UP KEYS and no way back. He said it plainly several
sessions ago — *"why do I store keys for next installation when I cannot get it? We locked ourselves
out of our own house."* Backup without restore is bookkeeping.

The backup already lands at `Documents/TTTmini/keys.txt`, which survives uninstall. Restore is
importing from that known path, and the importer already exists — with the provider mis-assignment
bug fixed in build 81, so a restore now puts each key where it belongs.

**Grant all-files access does not belong on this screen.** It is a permission, and permissions belong
in setup beside the microphone. By the time somebody is on the API keys screen wanting their keys
back, being told to go and grant a permission first is being told the door was locked after walking
to it.

**Sequence it in setup:** microphone, then all-files, then offer to restore keys from the backup
immediately — because at that moment the permission is fresh and the file is exactly where the app
put it.

---

## 3. Backup, restore and reset for settings

At the very top of the settings, three controls:

- **Backup** — write every preference to the same folder as the keys, `Documents/TTTmini/`.
- **Restore** — read them back.
- **Reset** — everything to shipped defaults, as it arrived.

Same folder as the keys on purpose: one permission grant covers both, and one folder is one thing to
remember when moving to a new phone.

**Reset needs a confirmation and reset needs to be honest** about what it does not undo — it cannot
bring back API keys, and it should say so rather than appear to have destroyed them.

---

## 4. Check the key before recording, not after

**Today:** he records, it uploads, it fails, and he learns there was no key. The recording is spent
and he has to re-transcribe.

**Instead:** before the microphone opens, check a usable key exists for the active provider. If not,
refuse immediately with a message that carries a button **straight to the API keys screen** — not to
the settings, to the screen.

Cheap to check, and it saves the worst kind of failure: one where the user did everything right and
lost the work anyway.

Less likely once §2 lands, but "less likely" is not "cannot happen" — a key can expire, be deleted,
or be rejected between one sentence and the next.

---

## 5. The new copy row

Scrap the old one. The new row is a text editor for the field in front, laid out like the transport
controls on a media player, which is a shape everybody already reads.

| Control | What it does |
|---|---|
| ⏮ / ⏭ | jump one word back / forward |
| **Shift** | a **locking toggle** — press once and it stays down |
| cut / copy / paste | the obvious three |
| select all | |
| delete | as on the keyboard |

**The lock is the good idea in this.** With Shift locked, the skip buttons stop moving the cursor and
start *selecting*. So selecting three words is: lock, skip, skip, skip. No dragging on glass, no
handles to miss, which for someone working by voice on a phone is the difference between editing and
not bothering.

Worth stating: this makes the copy row the first thing in the app that is a **text editor** rather
than a set of shortcuts. The buckets paste, this one *edits*.

---

## 6. The symbol row becomes configurable

The `Z X C V B N M` row carries symbols on long press — `* " ' : ; ! ?`. He wants each of those
long-press slots replaceable with **anything from the feature row**, and the chosen thing's symbol
drawn on the key so he can see what is there.

*"It is like a keyboard button and feature row in one."*

**This is the most invasive item in the batch and should be last.** The others add screens and rows;
this one reaches into FlorisBoard's key layout and popup system, which is upstream code the app has
otherwise left alone. Worth doing — it is a genuinely new idea, and the long-press slots are wasted
space at the moment — but not worth doing in the same build as anything else.

---

## 7. TRANSCRIPTION — one settings entry for the whole subject

A new entry, named in capitals as he asked, gathering everything about turning speech into text and
then working on that text. **Moved rather than copied** — the point is one place, and leaving the old
entries behind would make two.

What belongs in it:

- Word predictions (moves from its own entry)
- The transcription model, cloud and local
- **Download the local model** — Parakeet TDT v3, 25 languages, ~670 MB
- Translation, when it exists
- **Shape** — see below

The organising idea: *anything that recognises or then modifies the transcribed text*. That is a
real category rather than a folder, which is why it will still make sense after three more features
land in it.

---

## 8. Shape — prompts applied to text already transcribed

The biggest of these, and the one the AssemblyAI research already cleared the way for.

**How it works.** Text is already in the box. He presses **Shape**, picks a prompt, and the text is
replaced by the model's version of it — grammar fixed, tone changed, translated, whatever the prompt
says.

**Where the button lives.** On the pinned line at the bottom — the free strip beside the pin from the
note above. It appears when there is finished text to work on and costs nothing when there is not.

**The prompt library:** twenty slots, four across and five down. Each slot has a **title** and a
**prompt box**. The title is what he presses; the prompt is what gets sent.

**Model choice:** a radio button per model, one selected. Through AssemblyAI's LLM Gateway, so
Claude and the rest are reachable on the AssemblyAI key alone — no second credential, no second bill.
This is why the Gemini and Anthropic providers can go: not because the models are unwanted, but
because they arrive by a better road.

**After the fact, by design.** The text is sent as text, not as audio, and only when he asks. That is
the whole point of the Gateway over the transcription-time summarisation, which AssemblyAI has
deprecated anyway.

---

## Suggested order

1. **Restore keys + setup permissions** (§2) — unblocks fresh installs, and everything else assumes
   keys are present.
2. **Key check before recording** (§4) — small, and stops the worst failure.
3. **History re-transcribe + language** (§1) — wiring only, high daily value.
4. **Settings backup/restore/reset** (§3) — same folder, same permission as §2.
5. **TRANSCRIPTION grouping** (§7) — do the move before adding Shape to it, or Shape lands in the
   wrong place and has to be moved twice.
6. **Shape** (§8) — the big one, and it wants §7 finished first.
7. **New copy row** (§5) — self-contained, can slot in anywhere.
8. **Configurable symbol row** (§6) — last, alone, because it touches upstream layout code.

---

# 9. The Switchboard, three fixes

## Rename: Magic Finger row

"Magic button row" becomes **Magic Finger row**. His name for it, and the more distinctive one — the
app already has a great many buttons and only one finger.

Rename everywhere it faces the user: the switchboard line, the settings entry, the screen title, the
row's own switch. Not in the code, where `MaMagic*` is fine and renaming files would cost a large
diff for nothing.

## Nothing shows what is tappable

The names open settings and the switches toggle, and **the screen gives no sign which is which**. He
built the row and still could not tell by looking. Two targets on one line is only safe when the
targets look different, and here they do not.

The fix is the rule the app already uses: **sand means this takes you somewhere**, established in
build 76 for the wand's gear. Apply it here — the names in sand, so a tappable name reads as a link
rather than as a label that happens to respond.

The summaries stay grey. They are description, not destination, and colouring them would say they go
somewhere too.

Worth noting this is the same class of mistake as the drag handle that was really an edit button: a
control that is present, correct and invisible because it looks like something else. Two of those in
one app is a pattern rather than an accident, and the rule to take from it is that **anything
tappable must look different from anything that is not** — not merely be tappable.

## An icon on every line

Every row gets a mark, matching the key it controls wherever one exists, so the switchboard reads
like the keyboard rather than like a list of names:

| Line | Icon |
|---|---|
| Feature row | the drag handle it already uses in the settings list |
| Magic Finger row | the finger — same mark as the key and the settings entry |
| Copy buckets | the paste mark from Paste timing |
| Suggestion row | the spellcheck mark from Word predictions |
| Number row | **the 1N keyboard glyph** from the note above, once that lands |

The number row is the interesting one: it should carry the same little-keyboard glyph the zone key
carries, so the switch and the key are visibly the same thing. That ties this note to the zone-key
note above — do them together and both are better, do them apart and the icon has to be chosen
twice.

---

# 10. Switching between keyboard views, properly

Reaching a view takes several presses at different places on the screen. Marko: *"For Baba, that's
too much."*

The `?123` key becomes the switcher, and it stays in one place so his thumb does not have to move.

- **Tap** — cycle to the next view. Tap tap tap, always the same spot.
- **Long press** — a list of every view, tapped directly. Faster than cycling once there are more
  than three.
- **At the top of that list, Edit** — opens a new settings screen, **Keyboard views**, where the
  views are dragged into the order the cycle follows. Same drag-to-reorder as everything else in this
  app, so there is nothing new to learn.

## The two-view swap, which is the part he actually needs

*"Sometimes I need to go between numerical keyboard and normal keyboard. Even direct access is too
slow."*

A separate key that flips between the **last two views used**. Not a cycle, not a list — one key,
two views, no thinking.

**And the better version he arrived at while talking:** rather than remembering the last two, put
**tick boxes in Keyboard views** marking which views take part. The key then cycles only the ticked
ones. Two ticked gives the swap; three gives a small cycle of exactly the views he uses.

That is better than "last two" because last-two is a guess about intent that goes wrong the moment
he visits a third view for one keystroke, and ticks are a statement of intent that cannot.

## Why this deserves real effort

Switching is not a feature next to the others — it is the tax paid on every one of them. Every view
added makes reaching all the rest slower, so the app gets worse at this as it gets better at
everything else. Worth over-building rather than under-building.
