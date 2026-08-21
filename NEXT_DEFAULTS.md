# THE LIST

Marko picks by number. When a feature is tested and working, it is struck off and the list is
reissued. This is the working menu — everything below this section is the detail behind it.

**Ready to ship, no design left**

| # | Feature | Size |
|---|---|---|
| ~~1~~ | ~~Paste timing defaults: 0, 500, 0~~ — **done, and it was already done**: the prefs shipped 0/500/0 and the screen already advises raising step 2. The list was stale. | — |
| ~~2~~ | ~~Settings order defaults~~ — **done, and it was already done**: `MaSettingsOrder.DEFAULT` already matched his thirteen exactly. | — |
| ~~3~~ | ~~Three more magic defaults~~ — **shipped**: `Use Image URL` (url), `Add URL` (+url), `Generate Images` (gen), unscoped. **See the caveat below.** | — |
| ~~4~~ | ~~Check a key exists before recording~~ — **already done**: `startRecording` refuses on no key AND no local model, with OPEN_SETTINGS. The list was stale. | — |
| ~~5~~ | ~~Restore keys button~~ — **shipped, build 112.** Restore existed but fired only on a completely empty keyring, which covers a fresh install and nothing else. Now a button beside Back up, merging through the same importer. | — |
| ~~6~~ | ~~Re-transcribe with a language beside it~~ — **already done**: the in-keyboard history panel carries the HR/ENG badge next to replay, and `retranscribeHistoryEntry` reads it through `MaLanguage.active()`. | — |
| 7 | Switchboard: rename to Magic Finger row, sand for tappable names, an icon per line | small |

**Caveat on 3, and a small job it creates.** `defaults()` is only ever reached through
`parse(raw).ifEmpty { defaults() }`, so a new default reaches a **fresh install only**. Marko already
has a stored target list, so the three new terms will not appear on his phone; the only route today
is the Reset button on the Magic button screen, which discards anything he has taught. **Worth
building: a one-time merge that appends missing built-in defaults to an existing list, guarded by a
"defaults merged up to version N" flag.** Small, and it makes every future default actually arrive.


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
| ~~19~~ | ~~Croatian suggestions~~ — **shipped, build 116.** 50,000 Croatian words bundled, searched under the personal model. See §33. | — |
| 20 | Wire the ONNX predictor to a real model — **measured and argued against, see §33.** Do not start this without reading it. | large |
| 21 | Configurable long-press symbols on `Z X C V B N M` — touches upstream layout code | large |
| 22 | The sequencer — see SEQUENCER_PARKED.md; needs the action-extraction refactor first | large |
| 23 | The hardware trigger for voice commands — **not** Power, see §27 for what is possible | medium |
| ~~24~~ | ~~Voice commands settings entry~~ — **shipped**, build 111. Documents the commands; the trigger choice joins it with 23. | — |
| 26 | **Groq language detection on the first 5 seconds** — see §28. Groq is now a registered provider (build 114), so this is back to medium: chunk, call, clamp. | medium |
| ~~25~~ | ~~Merge new built-in defaults into an existing list~~ — **shipped, build 112.** `MaMagicTargets.mergeNewDefaults` plus a version high-water mark; runs when the Magic finger screen opens. Raise `DEFAULTS_VERSION` whenever a default is added. | — |

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

---

# 10b. Shiva and Shakti share a key — the layout update

Marko's observation, and it is a real one about the layout rather than a flourish: **comma and dot
each occupy a whole key, while every other key on the board carries two characters.** `Z` carries
`*`, `X` carries `"`, `C` carries `'`. Only these two live alone.

## The merge

**Dot keeps the key. Comma moves onto its long press.**

Dot leads because it does more work — every sentence ends with one, and dictated text ends with
rather more than that. Comma is the second character on that key, reached by holding, exactly as `*`
is reached by holding `Z`.

This is the same rule the rest of the board already follows, so there is nothing new for anybody to
learn — only one key that stops being an exception.

## What the freed key becomes

The `?123` key becomes the **quick layout switcher**, and the freed comma space gives it room.

- **Tap** — jump straight between the layouts he has chosen. Not a cycle through all of them:
  through the two or three he ticked.
- **Long press** — *"beam me up, Scotty"* — straight into the settings screen where those layouts
  are ticked.

This is the same design as §10 above and should be built with it rather than beside it. §10 is the
slow switcher, walking every view in order; this is the fast one, jumping only between the ticked
few. Two keys, two speeds, one settings screen listing the layouts with tick boxes and a drag
handle.

## Status: half done in build 99

**Done:** the dot key now carries comma on its long press, so Shiva and Shakti share the seat.

**Also done in build 100:** the comma key is gone. Its slot now jumps straight to the second symbol
layout, so the two switchers sit side by side — one tap to either, where reaching the second used to
take two.

The faces changed with it. `?123` and `=\<` read as two unrelated keys and said nothing about
layouts; they are now ☰1 and ☰2, three stacked bars for the rows of a keyboard and a numeral for
which one. One family, and neither of them claims to be "the digits key" now that the layouts can be
reordered.

Still open from §16: the **ticked** cycle, where he chooses which layouts take part rather than
having two fixed keys.

## Note on effort

The merge itself is a **layout change** — FlorisBoard keeps its key layouts as data, so moving comma
onto the dot key's popup is editing that data rather than writing logic. Contained, and lower risk
than most of the list.

The switcher half is §16 and is large. **Do the comma merge first and on its own**: it is small, it
is immediately visible, and it frees the space the switcher will want.

---

# 23. Automatic language, clamped to two

Marko's objection to open detection is correct and settles the design: with 99 possible answers and
only two possible truths, a detector can return Spanish for accented English or Chinese for a noisy
room. **Whatever detects, its answer must be clamped to {hr, en} before anything acts on it.**

AssemblyAI's own `language_detection` cannot be constrained to a candidate set. That alone rules it
out as the router — not its accuracy, its unboundedness.

## Two detectors, at two moments, for two different jobs

### A. Before transcribing — audio, via Groq Whisper

Needed because the path must be chosen **before** the audio is sent: English can take Sync and be
fast; Croatian cannot, since Sync's eighteen languages do not include it.

- Send only the **first few seconds**, not the whole recording. Language is obvious in one sentence
  and this keeps it nearly free.
- Smallest Whisper on Groq, chosen for latency rather than accuracy — the question is much easier
  than transcription.
- **Clamp the answer:** `hr` stays `hr`; **everything else becomes `en`.** Not "unknown", not the
  raw guess. Two languages means two outcomes, and English is the safe default because it is the one
  Sync supports.

### B. After transcribing — text, and this one should be local and free

Marko's second idea, and better than it sounds: check the **transcript** rather than the audio.

**This needs no model and no network at all.** Telling Croatian from English in a sentence is easy
enough to do on the phone in microseconds:

- Croatian carries č ć ž š đ, which English never does — one of those is nearly conclusive.
- Failing that, the commonest words separate cleanly: *je, i, na, se, da, u, ne, su* against
  *the, and, of, to, is, it*.
- A few dozen words and five letters, counted. No download, no latency, no cost, works offline.

So the "send the text to another model to ask what language it is" step should never be built as an
API call. It is a hundred lines of Kotlin and a word list.

**What it is for:** disagreement. If the transcript looks Croatian but was transcribed as English,
the History row says so and offers the re-transcribe with one tap — the failure Marko keeps hitting,
caught automatically instead of noticed later.

It also removes any need for a second Groq call, and eventually the first: once §20's on-device model
exists, audio detection can be local too and Groq leaves entirely.

## The switch stays, and starts moving on its own

He was explicit: *"I want to see how it's switching automatically."*

The HR/ENG key remains exactly where it is and keeps its tap-to-override. What changes is that the
detector writes to it, so he watches it flip and can disagree. A setting that changes itself in front
of you is trustworthy in a way a hidden one never is — and it means the override is always one tap,
never a settings trip.

**Manual and automatic in one control**, which is what he asked for.

## Order

1. **B first.** It is local, free, and needs no key — and it makes every wrong transcription visible
   even before A exists.
2. **A second**, once B is proving how often the language is actually wrong.
3. **Drop Groq** when §20 lands and detection can be done on-device.

---

# 24. The corpus he already has

Marko asked for his transcriptions to be collected and analysed so the app learns from them, with a
cache he can inspect, delete, and read statistics from.

**Most of it exists.** Saying so is more useful than building a second copy.

- **The store is the History.** Every transcription is already kept with its text, language, model,
  duration and size, and the storage limits screen already shows what is retained.
- **The learning already happens twice.** `MaNgram` learns from what he commits and feeds the
  suggestion row. `Learn my words` mines the history for vocabulary the dictionary lacks.

So the answer is **not a transcription cache** — that would be a second copy of the History that can
disagree with it, and the wrong kind of work. What is actually missing:

## What to build

**A statistics screen over the history that already exists.**

- Total words dictated, total recordings, hours of audio.
- The words he uses most, with counts — his own vocabulary ranked.
- Croatian against English, by proportion.
- How often a transcription was re-run, and in which language. That number is the app grading
  itself.

**Make the corpus openable.** Export the history as one text file to `Documents/TTTmini/`, beside
the keys and the settings backup. Then he can read it, keep it, or feed it somewhere else — and
deleting it in the app does not destroy it.

**Say what it costs.** File size and word count on that screen, so deleting is an informed choice
rather than a guess.

## Why this fits the app rather than decorating it

His words: *"we are building applications which are not only useful, which are teachers. Student and
teacher at the same time."*

Statistics on his own speech are the app teaching him something about himself he cannot otherwise
see — which words he leans on, how much he actually dictates, how often the language guess is wrong.
And every one of those numbers is also the app being taught: the re-transcribe count says whether
§23 is working, the word ranks are what §19 needs for Croatian suggestions.

The same data serves both directions, which is what makes it worth building rather than merely nice.

---

# 25. The two switchers become two slots

Today each switcher key leads somewhere fixed. Marko wants each to be a **slot holding a layout he
chose**, so the pair becomes "my two layouts" rather than "the two layouts the app picked".

- **Tap** — go to whatever that slot holds. Unchanged from his thumb's point of view.
- **Swipe up / down** — change what the slot holds, cycling the available layouts.
- **Long press** — open the full list and choose directly.

Two keys, the same behaviour, **separate memories**. That is the whole design: `text`/`sym1` today
is one possible pair among many, and which pair he wants changes with what he is doing.

## What exists and what does not

**Long press now opens the list — built, not yet confirmed on the phone.** Every view-switcher key
in `charactersMod`, `symbolsMod` and `symbols2Mod` carries a popup listing the three text views, and
`VIEW_CHARACTERS`, `VIEW_SYMBOLS`, `VIEW_SYMBOLS2` were added to `ExceptionsForKeyCodes` in
`PopupUiController.kt`. That list is the gate: extended popups are refused to any key whose code
sits below `SPACE`, which is every system key, and that refusal was why holding a switcher did
nothing. Basic popups stay refused, so no preview bubble appears over the faces.

**This is navigation, not slots.** Holding a switcher and choosing takes you to that view; the key
does not remember. Slot memory and the face reading from a preference are still open, and are the
real §25.

**Numeric and phone are deliberately absent from the list.** `VIEW_NUMERIC`'s face is the two-line
string `1 2\n3 4`, which is a key face rather than a list entry. Adding those views wants a
single-line face first, and that face cannot simply be changed in `strings_dont_translate.xml`
because the same string draws the real numeric key.

**Swipe up and down on a specific key does not exist.** FlorisBoard has a gesture system, but it is
built around swipe-on-the-whole-keyboard actions and long-press popups, not per-key vertical swipes.
This is the part that needs real work and the reason this is not a small feature.

**Suggested fallback if per-key swipe proves awkward:** long press opens the picker, and that alone
delivers most of the value. Swipe is the refinement, not the feature.

## Storage

Two preferences holding a layout id each, defaulting to what ships now — `symbols` and `symbols2`
from the letters view. The key's face comes from the slot rather than being fixed, so `sym1` becomes
whatever is in it. That means the label logic in ComputingEvaluator stops being a constant and starts
reading a preference; small, but it touches the file that draws every key face, so it wants care.

---

# 26. Voice commands — "press send" (shipped, build 110)

A finished transcription reading **exactly two words**, a press verb and one more word, presses that
button instead of being typed. `MaVoiceCommand.targetIn` is the rule and `commitOutput` in
`DictateController` is the seam — the one place every finished transcription passes through on its
way to a field, whichever sink it is bound for.

Verbs: `press`, `pritisni`, `stisni`, `klikni`. Trailing punctuation is stripped first, because
AssemblyAI punctuates and "Press send." is what actually arrives.

**Why exactly two words.** Dictated text is what this app is for, and a rule that ate part of a
sentence would lose work that cannot be recovered — the words were spoken, not typed, so there is
nothing to undo back to. Two words alone is not something anybody dictates mid-sentence: it is said
to a machine, after a pause, on purpose. Tested against `I will press send now` and
`press the send button`; both stay as text.

**Nothing is lost when it misses.** `pressScreenTarget` returns the term it found, or null. On null
the code falls through and types the words as it always would have. That is what makes it safe to
fire without confirming.

**The spoken word is matched against taught terms first**, by face and by term, so "press stop"
finds the target labelled `stop` and searches for `Stop responding` — he never has to say the long
name. The raw spoken word goes last as a fallback so an untaught button still answers to its own
name.

Switch: `dictate__ma_voice_commands`, on by default, on the Magic finger screen.

---

# 27. The hardware trigger — and why it cannot be Power

**Volume Up + Power is not buildable. This is a platform limit, not a difficulty.** The power key is
consumed by the system's window manager and is never dispatched to applications, and it is not
delivered to accessibility services either, even ones holding `flagRequestFilterKeyEvents`. There is
no permission that changes this. Any design resting on Power has to be redrawn rather than
attempted.

## What is actually available

**Volume keys, globally, through the accessibility service.** An `AccessibilityService` declaring
`android:canRequestFilterKeyEvents` and `flagRequestFilterKeyEvents` receives `onKeyEvent` for the
volume keys **everywhere** — no keyboard, no focus, any app. This app already runs such a service
for the finger, the floating button and TAB, so the trigger has a home already built.

That gives these as real options:

- **Volume Up + Volume Down together.** The closest thing to what he described: two buttons, one
  gesture, works everywhere. Does not collide with the screenshot, which is Power + Volume Down.
  The one caution is Android's own "hold both volume keys" accessibility shortcut — a short chord is
  distinguishable from a three second hold, but it must be checked on his phone.
- **Long press Volume Up.** One button, no chord. Cheapest of the three.
- **Double press Volume Up.**

## What already exists and overlaps

`maHandleVolumeKey` in `FlorisImeService` already gives Volume Up start/stop dictation and Volume
Down cancel-or-switch-language — but **only while the input view is shown**, deliberately, because
an IME receives key events only then. The new trigger is the same idea moved into the service that
can hear those keys with no keyboard on screen, and the two must be made to agree rather than both
firing when the keyboard is up.

## Settings

A **Voice commands** entry of its own in the settings list, holding the on/off switch that now lives
on the Magic finger screen plus the trigger choice. Deferred with the trigger, since a screen for one
switch is a screen he has to find for no reason.

---

# 28. Groq decides the language, from the first five seconds

**Groq, the inference service — never xAI's Grok.** Marko was explicit and it is worth writing down
because dictation turns one into the other every time: when he says Grok he means the fast open
service already in `ProviderRegistry` as `"groq"`, with a key slot and a legacy migration path
(`DictateLegacySettings.transcriptionApiKeyGroq`).

## The shape, as he specified it

1. Cap the **first five seconds** of the recording into a second file, separate from the full audio.
2. Send that chunk to Groq's Whisper.
3. Take the language from the reply, **clamp it to {hr, en}** — `hr` stays `hr`, everything else
   becomes `en`, per §23.
4. **One millisecond before** the real request goes to AssemblyAI, apply that language.

## One call, not two

He asked for the chat model to be dropped and he is right: Groq's transcription response already
carries the detected language, so a second call to a chat model would be asking a slower question
that has already been answered. One call, and the answer arrives while he is still speaking.

## What it must not break

**Croatian must never reach the Sync path.** If detection returns `hr`, the request goes async, no
matter that the language was decided by a machine rather than by him. `SYNC_SAFE_LANGUAGES` stays an
allow-list, and the detection result is applied *before* `maUseSyncPath` reads it — that read
already happens late, after the resample, which is exactly where this fits.

## Where it plugs in

The language and the speed are both read **when the request is built**, not when recording starts —
see the note in `maHandleVolumeKey` about why the second volume press was reverted. That late read
is what makes this feasible without touching the recorder: the detection has the whole length of the
dictation to come back, and only has to win the race against the user pressing stop.

**If it is not back in time, or it fails, or there is no Groq key: use the switch as it stands.**
Detection is an improvement on the manual setting, never a precondition for dictating.

## The switch still moves, and it is now safe for it to

§23 said the detector should write to the HR/ENG badge so he can watch it flip and disagree. That
was written when volume down toggled the language on a short press, which made an automatic write
and an accidental write indistinguishable. Since build 111 the language only moves on a long press,
so a badge that changes by itself now means exactly one thing.

---

# 29. Audio focus: removed, and not to be rebuilt

Recording used to take `AUDIOFOCUS_GAIN_TRANSIENT` with `USAGE_VOICE_COMMUNICATION`, behind a
`dictate__audio_focus` preference that defaulted **on**. Two things followed from that, and both were
wrong:

- Every other player on the phone was asked to duck or pause whenever he started dictating.
- The focus listener **paused his recording** whenever another app took focus back.

With MA Reader playing in the background and something else starting up, those two rules met: each
program politely stopping for the other, which is why the behaviour read as backwards rather than
merely unwanted.

**Removed entirely**, not defaulted off: the request, the listener, the preference, the settings
toggle, the settings-search entry, the legacy migration and the legacy key. Six files. Nothing
replaces it.

**SUPERSEDED at build 159 — see §71.** He asked for exclusive recording back. The part of this
section that still stands is the second half: **nothing may react to what another app does with
audio.** Asking others to pause is fine; listening for focus changes is what was broken.
This app records through the microphone; it has no business holding the phone's audio session. A
future "pause music while recording" request should be read as a request to change *his* mind, not
as a gap to fill.

**Not covered by this:** the short press on volume down, which hands a volume change back to the
system through `adjustSuggestedStreamVolume`. That is the user pressing a volume key and getting a
volume change — the opposite of the app deciding something about somebody else's playback — and he
asked for it by name in build 111.

---

# 30. Ctrl+P, proofreading — and the correction to §28

## Ctrl+P

Bound in `maHandleCtrlCombo` alongside the clipboard and undo shortcuts, and taken deliberately: it
used to fall through to a real ctrl+P event, which means print on a desktop and nothing on a phone.

It goes through `DictateController.applyPrompt` with a throwaway `PromptModel` from `MaProofread` —
the same path the prompt library and the wand already use, so the network call, the key ring walk,
the stop button and the no-key error that opens settings were all already built and tested.
`requiresSelection = true` gives the right behaviour for free: correct the selection, or select the
whole field and correct that.

**The instruction is mostly prohibitions.** A model asked to correct will rewrite — shortening,
formalising, dropping deliberate repetition — and a paragraph that comes back reading better but no
longer sounding like him is worse than a missing comma, because it is harder to notice. It also
names no language, since naming English would translate his Croatian.

**Not yet done: pinning it to a model.** Ctrl+P uses whatever the rewording provider is set to. He
asked for Claude Sonnet specifically, and the honest state is that this respects his setting rather
than forcing one. Forcing a provider per prompt means an override threaded through `requestRewordRaw`,
which reads the account, the ring id and the model together. **Worth building: an optional
provider/model override on `applyPrompt`,** which would also let library prompts pick their own
model — §18 wants exactly that, so the two should be built together.

## §28 was wrong about Groq, and this is the correction

**There is no `groq` provider in `ProviderRegistry`.** The ids are openai, openrouter, gemini,
anthropic, together, deepinfra, mistral, soniox, elevenlabs, deepgram, assemblyai, assemblyai-sync,
xai, deepseek, ollama, local, custom.

What exists are references to one that is not there: `ProviderIcons` maps `"groq"` to a drawable,
`DictateLegacyMigrator` writes accounts with `providerId = "groq"`, and `SetupScreen` declares
`RECOMMENDED_PROVIDER_ID = "groq"` — **which is itself dead, never read anywhere**.

So §28's step 2 is not "send the chunk to Groq". It is: add the Groq preset to the registry, get a
key into the ring, wire and test the Whisper endpoint, and only then detect. Sizing it as medium was
wrong.

**Worth considering instead:** OpenAI runs Whisper on the same OpenAI-compatible shape and is already
a registered provider with a key slot. Same one call, same returned language field, none of the new
provider work. The clamp and the two-signal rule from §28 are unchanged either way — they read a
language string and a transcript, and do not care who produced them.

---

# 31. The keyring: Groq in, Gemini out

## Why a Groq key could not be stored

`ProviderRegistry.presets` was `ASSEMBLYAI, GEMINI, ANTHROPIC, LOCAL`. **A preset defined in that
file but absent from that list does not exist**: `byId` returns null, and `MaKeyImport.importAll`
does `byId(id) ?: continue`. Groq had no preset at all, and `MaKeyImport.PROVIDERS` never listed it.

Meanwhile `MaKeys` has known the Groq shape all along — `GROQ = gsk_[0-9A-Za-z_-]{20,}`, with an
`extract` branch and entries in both `belongsToAnotherProvider` and `mismatchWarning`. So a Groq key
in a keys file was recognised, matched, and then dropped on the floor for want of a provider to file
it under.

Fixed by adding the preset (`api.groq.com/openai/v1/`, chat and transcription, dynamic models) and
registering it. Also removed a dead branch in `extract`: `"groq" -> GROQ` was followed by
`"openai", "groq" -> OPENAI`, whose groq half could never be reached.

## Gemini, removed

Unregistered rather than deleted. Its preset stays defined, because removing it would take the
model-picker branch and the icon mapping with it for the sake of a few unread lines, and because
unregistering is the reversible half.

**A stored Gemini key is not erased.** It stops being importable and stops being shown — the keys
screen ends in `ids.mapNotNull { ProviderRegistry.byId(it) }`, so an id the registry does not know
drops out even when an account still holds it. If he wants the key actually gone from the ring, that
is a separate deliberate wipe and should be asked for rather than assumed.

## Model names to check

`llama-3.3-70b-versatile` and `whisper-large-v3-turbo` are the defaults for an account that never
chose one, and they are only as current as this was written. `supportsDynamicModels` is true, so the
picker can fetch the live list; a stale default costs a trip to the model picker, not a broken
provider.

---

# 32. The roles are fixed, and the chips are gone

## What went wrong

The API keys screen asked which provider held which role: a `transcription` and a `rewording` chip
per provider, writing `transcriptionProviderId` and `rewordingProviderId`.

Groq arrived in build 114, advertised transcription because it genuinely can transcribe, and **took
the transcription role**. Every dictated word went to Groq's Whisper instead of AssemblyAI, silently,
with the screen showing what looked like a correct configuration. The Croatian-never-Sync rule lives
inside the AssemblyAI path, so while that was true the rule protected nothing — AssemblyAI was not
being called at all.

A setting that can be wrong in a way nothing announces is a trap, not a setting.

## What replaces it

`MaRoles` holds the mapping: `assemblyai` transcribes, `anthropic` rewords and proofreads, `groq`
detects language. `transcriptionAccount()` and `rewordingAccount()` **resolve** through it at the
moment the job is done rather than reading a stored id, so a value that has drifted cannot survive,
because nothing consults it.

The stored preference remains as a fallback for a setup with no key for the fixed provider, which
keeps custom endpoints working — but it can never resolve to the language provider, in either role.

The chips are removed. Each provider now shows its role as a plain label, read from `MaRoles`, so the
screen cannot describe an arrangement different from the one in use. `MaRoleChip` and the two
`ProviderSection` parameters that fed it are deleted.

**The on-device model is unaffected**: that path reads `localTranscriptionAccount()` against
`ProviderRegistry.LOCAL` directly and never went through the role id.

## The rule to keep

When a new provider is added, give it a role in `MaRoles` — do not give the user a chip. Capability
is not the same as job: Groq can transcribe and must not, and the difference is a fact about this
app rather than about Groq.

---

# 33. Croatian suggestions, and why not the neural model

## What shipped

50,000 Croatian words in `assets/dictate/hr_words.txt`, 545 KB, searched by `MaBaseDictionary` and
appended by `MaNgram.predict` **after** the personal tiers.

Three conditions on it, each deliberate:

- **Only with a prefix.** A word list with no prefix returns the commonest word in the language,
  which is not a prediction.
- **Only when Croatian is active.** English already has a dictionary from upstream, and offering both
  would put Croatian under an English sentence.
- **Always last.** A word he has written outranks a word the language merely contains. `tier` is not
  read downstream, so position in the list *is* the ranking — which is why appending is the whole
  mechanism and `TIER_BASE = 0` is documentation.

It sits **outside** the `MIN_WORDS_BEFORE_PREDICTING` gate on purpose. An empty personal model is
exactly the state this exists for: a fresh install should suggest Croatian on the first word, not
after three hundred.

**Memory.** Held as one string plus one `IntArray` of line offsets, not 50,000 `String` objects,
which would cost megabytes in object headers alone inside a process Android kills for using what an
app may use freely. Loaded on first Croatian use; an English-only session never pays for it.

**Verified before building**: the binary search plus forward scan was run against brute force over
the real file for fourteen prefixes including `ž`, `šta`, `đ`, a one-letter prefix and a miss, plus
the exclude path. All identical.

**Licence.** CC BY-SA 4.0, from hermitdave/FrequencyWords over OpenSubtitles. Attribution and the
list of modifications are in `assets/dictate/hr_words_LICENSE.txt`. **That licence covers the data
file only**, not the app. Share-alike applies if the list is redistributed.

## Sizing, if it is ever revisited

Coverage of running text by list size, measured on the source corpus: 10k → 81%, 20k → 85%, 30k →
87.5%, **50k → 89.7%**, 80k → 91.3%, 120k → 92.3%. The next 50,000 words buy about three points and
double the file. 50k is where the curve flattens.

## §20, the neural predictor — measured, and argued against

`Xenova/distilgpt2` int8 was downloaded and its graph inspected rather than guessed:

- **81 MB**, 15 inputs (`input_ids`, `attention_mask`, twelve `past_key_values.N.key/value`,
  `use_cache_branch`), output `logits [batch, seq, 50257]`.
- **50257 is the GPT-2 BPE vocabulary: English only.** It cannot help the language that was actually
  missing.
- 81 MB inside an input method risks the keyboard being killed mid-sentence.
- `MaNeuralPredictor.BUDGET_MS` is 60 ms and discards slower passes, disabling the model after five.
  distilgpt2 int8 on this class of phone lands near that line, so it could install and then silently
  switch itself off.
- Uncounted work: a byte-level BPE tokenizer in Kotlin plus ~1.5 MB of vocab and merges, and a
  downloader, since 81 MB cannot go in the repo.
- `MaNeuralPredictor` is also called from nowhere; wiring it into `NlpManager` is a further step.

**If it is ever revisited, find a small multilingual model first.** An English-only model is the
impressive answer to a question this app does not have.

---

# 34. TAB for fields, through the node tree

**A real Tab key does not do this, and that was tested rather than assumed.** `MaMacroSyntax`
already maps `{Tab}` to `KEYCODE_TAB` and sends it through `sendKeyEvent`. In Suno it moves the
caret inside the field it is already in and nothing else.

That is not the app misbehaving. Android moves focus on Tab only between views marked focusable in
touch mode, and almost nothing is, because until recently no phone had a Tab key. So no amount of
sending a better Tab would have worked.

**`MaFeatureKey.NEXT_FIELD` reads the accessibility node tree instead.**
`DictateAccessibilityService.focusNextEditableField()` collects every visible editable node
depth-first, finds the one holding focus, and calls `ACTION_FOCUS` on the next, wrapping to the
first. It tries each following field in turn rather than stopping at the first refusal, because a
field can decline focus and stopping there makes the key look broken when the next would have
worked.

Depth-first is declaration order, which in practice is down the screen. Wrapping means that on a
two-field screen — lyrics and style — one key is a toggle between them, which is the actual problem.

**Why this works where Tab does not:** the tree exists because screen readers need it, so every app
provides one whether or not it was built with a keyboard in mind. Native, web view and browser all
behave the same.

Caps: `MA_FIELD_MAX_DEPTH` and `MA_FIELD_MAX_COUNT`, both 40. Deeper than the dictation-target search
because this must find every field rather than the nearest one, and modern layouts nest hard.

**Two keys share the word TAB and must not be merged.** `APP_SWITCH` moves between apps;
`NEXT_FIELD` moves inside one. Both are in the default row.

The icon is `Icons.Default.KeyboardTab`, **verified to exist** by extracting
`material-icons-extended-android` and finding `androidx/compose/material/icons/filled/KeyboardTabKt.class`
— this file's own history says an icon that only exists in the documentation is a red build.

---

# 35. Shift+Tab, a row Shift, and Aa

## The visibility caveat, fixed

`collectEditable` filtered on `isVisibleToUser`, which was wrong and was mine. A form taller than the
screen reports its later fields as not visible, so the key refused to reach **exactly the fields that
are hardest to reach by hand**. Off screen is a reason to scroll to a field, not a reason to pretend
it is not there.

The filter is gone and `ACTION_SHOW_ON_SCREEN` runs before `ACTION_FOCUS`, so the container scrolls
the field into the frame and then it takes focus.

## Shift on the feature row

`MaFeatureKey.SHIFT` writes the same `inputShiftState` the letter shift writes — the real modifier,
not a private flag sharing a name. It exists because the letter shift is unreachable in the state
this row is for: the row alone with the keys folded away.

Three states in a ring: off, once, locked. Locked earns its place here more than on the letters,
because walking backwards through a long form is several presses of TAB and re-arming shift before
each one would be the worse key. Lit in sand while it is holding.

## Shift+TAB

`NEXT_FIELD` reads `inputShiftState` and walks backwards when it is set, from either shift. A
`SHIFTED_MANUAL` is **spent** by the press, the way a letter spends it; `CAPS_LOCK` is not, because
somebody who locked it means to keep going.

## Aa, the case cycle

`MaCaseCycle`: lower → UPPER → Sentence → Title → lower. Selection if there is one, otherwise the
whole field — the same rule Ctrl+P uses.

**The next case is read from the text, never from a counter.** A remembered position would go wrong
the moment anything else changed the text and would do it invisibly, so the key would seem to skip a
step for no reason. Unrecognised text starts the ring rather than being refused.

It steps twice when a transform would change nothing, because two cases can produce identical text —
a single capitalised word is both Sentence and Title — and stopping there looks like a dead key.

The four transforms are `MaCaseTransform`'s, not new ones: locale-aware, with `don't` not becoming
`Don'T` and sentence case lowering the shout. **Verified against the ring** before wiring: four
presses return to the start, text with no letters returns null, and Croatian diacritics uppercase
correctly.

---

# 36. Volume keys, and the finger's reach

## Volume keys, settled

**Volume down is released entirely.** It has held three jobs — language, cancel-a-recording,
language again on a hold — and every one made the commonest button on the phone mean something other
than quieter. Marko changes language rarely and turns the volume down constantly. `maHandleVolumeKey`
returns false for it, so nothing is consumed and Android does what it always did. **Do not give this
key a job again.**

**Volume up decides on release.** Short raises the volume through `adjustSuggestedStreamVolume`; a
hold of `MA_VOL_RECORD_HOLD_MS` (500 ms) calls `onMicClick`, which is both ends of a dictation, so
the same hold starts and stops. The frequent action is the cheap gesture and the rare one costs a
deliberate hold — which is the right way round for somebody playing bhajan through the same phone.

Cancelling a recording lost its key and keeps the bar's own control and the mic key, both on screen
while recording.

## The finger reaches off screen now

`matchOf` refused any node that was not `isVisibleToUser`, which made the finger refuse the presses
worth most: a button five screens down is exactly the one that saves the most scrolling, and it was
the one press that could not be made.

The check is gone, and `ACTION_SHOW_ON_SCREEN` runs before the click so the app scrolls to what it
acted on — a press whose effect happens unseen is indistinguishable from one that did nothing.

**The bottom-most match wins, and that is now the whole rule rather than a tiebreak.** A node below
the fold has a larger `bottom` than anything on screen, so in a chat carrying a copy button under
every answer ever given, the finger takes the newest rather than the newest *visible* one.

### The limit that remains, and it is Android's

**A list only puts materialised rows in the tree.** `RecyclerView` and its equivalents recycle rows
that are far off screen, so those nodes do not exist to be found — not filtered out, absent. Removing
the visibility check reaches everything the app has built, which is typically the viewport plus a
screen or so either side. It cannot reach a button ten screens up, because there is nothing there.

**Worth building, for the "hundreds of copy buttons" problem:** a target that names an occurrence —
`copy 1`, `copy 2`, counted from the bottom — and a finger that scrolls and re-searches when a target
is not found. The second is what actually removes the limit above, since scrolling is what makes the
rows exist.

---

# 37. The spacebar mark, and volume down carries a term

## The spacebar was bare, and the comment said otherwise

`computeLabel` returned **null** for `KeyCode.SPACE`, with a comment saying the face was decided
where the key is drawn. It is — but that code reads `key.label?.let { ... }`, so a null label meant
the block never ran and the bar was drawn empty. **The mark existed in a comment and nowhere on the
keyboard.** A worked example of why a comment is not evidence.

Now returns `U+23B5`. The drawing code still honours `SpaceBarMode.NOTHING` and still substitutes its
own mark, so what is returned here only has to be non-null.

## Volume down: a hold presses a magic finger term

Both hardware keys now have the same shape — nothing decided on the way down, a short press is
volume, a hold is the other meaning. Neither key can take the volume away on a quick tap, which is
the requirement when bhajan is playing through the same phone.

The hold presses a term through `pressScreenTarget`, the same path the finger uses, so a term that
works on the row works on the key. `dictate__ma_volume_down_term`, default `Send`. Empty leaves it a
plain volume key, and with the accessibility service off it stays a volume key rather than swallowing
the press.

**Still to build: the settings UI for it** — a way to pick which taught term the key carries, rather
than the preference only being reachable in code.

## Bucket copy — designed, not built

Confirmed from a real node dump: **code boxes are distinguishable.** They carry
`desc="Copy code"`; message footers carry `desc="Copy message"`. A term of `copy code` finds only
code blocks.

`ClipboardManager.captureIntoClipSlots` already files every clipboard change into the next free
bucket, reads which buckets are visible on each copy, and stops when they are full. **So bucket copy
does not write buckets at all** — it presses copy buttons one after another and the existing system
fills them in order. Resetting the buckets already sends the next copy back to the first.

To build:
- A key that collects every `copy code` match, orders them **bottom-up** (newest first), and presses
  each with a settle pause between — the clipboard must land before the next press or a bucket is
  skipped. Start at 250 ms and tune on the phone.
- A second plain copy key that takes the last match whatever it is, code or not.
- **A face that says which bucket is next** — a `B` with a pouring arrow — because without it he
  cannot tell where the sequence has got to.
- **Long press to reset to bucket one.** This is the part that makes it usable rather than a trap:
  without it a mistake means copying from last month.

**The limit that remains is Android's**: recycled rows are absent from the tree, not hidden in it, so
one press reaches the code boxes near the viewport rather than the whole conversation. A finger that
scrolls and re-searches is what actually removes it.

---

# 38. Ctrl+F, better flow

**Ctrl+P is unchanged and stays unchanged.** It fixes what is wrong and may not touch a word he
chose. `MaFlow` is the other half: it is *allowed* to rewrite, and that difference is why there are
two keys rather than one compromise. When he wants the commas fixed he wants only the commas fixed;
when he wants the paragraph to read well he is asking for the words to move.

**What it targets is what speech leaves behind.** Dictation says the same thought three times in
three shapes, because the mouth finds the sentence while saying it and the false starts stay in the
transcript. A proofreader will not touch any of that, so the result reads as careless writing, which
is the opposite of true. The instruction is aimed at redundancy and order: say each thing once, put
them in the sequence that follows.

**Register named explicitly** — warm and direct, half friendly and half professional, and *not*
corporate. A model told to "improve" text reaches for the flattened business voice, which strips out
exactly what makes a message recognisably his.

**The line it may not cross: it may cut, join and reorder; it may not add.** Given permission to
rewrite, a model will invent a politeness he did not offer or a commitment he did not make, and a
message that goes out carrying an invented promise is worse than one that rambles.

The spinning dust already shows the running prompt's name, so the two keys are told apart mid-flight
by naming this one `Better flow` against the proofreader's `Proofread` — no new UI needed.

## Volume down's term now has a picker

In Gestures, under Volume keys. Offered as a **list of the terms already taught to the finger**
rather than a text field, because a term typed by hand that matches nothing on the Magic finger
screen is a key that silently does nothing, with nothing on that screen to say why. Includes an
explicit "nothing" option that leaves it a plain volume key.

The old summary there described behaviour that no longer existed — volume up starting on a short
press, volume down swapping views — and has been rewritten to match the code.

---

# 39. Dictation with no text field, from anywhere — SHIPPED, build 123

**Asked: can a long press on volume up start dictation when the keyboard is not on screen — in any
app, with no input box — and keep the result in History?**

**Yes, and the parts nearly all exist. It is not free.**

## Why it does not work today

The volume keys are handled in `FlorisImeService.onKeyDown`, which is guarded by `isInputViewShown`
and, more fundamentally, only receives key events at all while the IME has an input connection. No
text field means no keyboard means no key events. Nothing about the current wiring can reach outside
that.

## What makes it possible

`DictateAccessibilityService` already runs for the magic finger, TAB and the floating button. An
accessibility service that declares `android:canRequestFilterKeyEvents="true"` and adds
`flagRequestFilterKeyEvents` to its config receives `onKeyEvent` **globally** — every app, no
keyboard, including the volume keys.

`accessibility_service_config.xml` currently declares
`flagDefault|flagRetrieveInteractiveWindows|flagReportViewIds|flagInputMethodEditor`. Neither the
flag nor the attribute is there, and `onKeyEvent` is not overridden. That is the whole gap for the
capture half.

## What to build

1. The flag, the attribute, and `onKeyEvent` in the service, with the same press-length rule the IME
   uses so a short press stays volume.
2. A destination when there is no field. `commitOutput` writes through a sink; with nothing to write
   to it must not simply fail. It should land in History and on the clipboard, and say so.
3. **Check History is written even when the commit fails** — `DictateHistoryEntry` has a `failed`
   flag, so the shape is there, but confirm the row is saved before assuming the note is recoverable.
   A dictation that is lost because there was nowhere to put it is the one outcome that must not
   happen.

## Warn him before building it

Granting "observe your keystrokes" is a serious-sounding Android prompt, and the service will need
re-enabling after the config change. Both are worth saying out loud rather than discovering.

Note this is also the honest home for §27's hardware trigger for voice commands: same mechanism, same
`onKeyEvent`, and the two should be built together rather than each growing its own key handling.

---

# 40. How §39 was actually built

`DictateAccessibilityService.onKeyEvent` now sees the volume keys everywhere, behind
`android:canRequestFilterKeyEvents` and `flagRequestFilterKeyEvents`.

**The destination question answered itself.** Dictation runs with `OutputTarget.OVERLAY`, the target
the floating button already uses, so everything needed was built: a focused field still receives the
text, a screen with no field fails the commit and takes the `!committed` branch — which calls
`rememberLastDictation`, `recordHistory` and surfaces Reinsert. **`recordHistory` runs on both the
success and the failure path**, so the note is kept either way. Nothing new had to be written for it.

**Three rules the handler obeys, each load-bearing:**

- **The keyboard wins when it is up.** `FlorisImeService.ownsVolumeKeys()` was added for this. Both
  handlers can hear one press, and without it a hold would start a recording and stop it at once.
- **A short press is always the volume.** The release decides and hands the change back by hand,
  since the press was swallowed. Consuming a volume key and giving nothing back is what would make
  the phone feel broken.
- **Every other key is handed straight back.** The service can see all of them and looks at two.

**It ships off** (`dictate__ma_global_volume_keys`, default false), with the switch in Gestures. This
is the one setting whose failure mode is "the volume buttons stop working", which must not arrive by
update. He turns it on having read what it does.

**This is also §27's hardware trigger.** Voice commands should reuse this `onKeyEvent` rather than
growing a second key handler.

---

# 41. The dot key's comma, and per-spacer width

## Why the dot key showed no comma

Hints are paired **by position** between the letter rows and the symbol rows, in
`LayoutManager.addRowHints`. The bottom row — ctrl, the switchers, space, dot, enter — has no symbol
row lined up with it, so it was never given a hint at all. The comma was reachable only by holding,
while every letter above advertised its second character in the corner.

A key on the board keeping its second meaning secret is worse than none of them showing it.

Fixed by a narrow rule after the pairing loop: a key with no symbol hint, whose popup declares a
CHARACTER `main`, takes that as its hint. The comma moved from `relevant` to `main` in
`charactersMod/default.json` to match.

**Keyed on `main` and not on the first `relevant` entry, deliberately.** The switcher keys carry a
popup listing every view (§25); a hint reading "text" in the corner of `sym1` would be noise. They
declare only `relevant`, so the rule passes them over. **Anything given a `main` popup from now on
will grow a hint — that is the trade.**

## Per-spacer width

Stored in the spacer's own `label`, which is unused on a spacer — there is nothing to write on a gap
— so it costs no change to how targets are serialised or parsed. Blank falls back to
`maSpacerTenths`, which is what every spacer made before this will have.

Minus, the number, plus, on the spacer's own row: no dialog and no text field, because the only
question is wider or narrower and the answer is judged by looking at the row rather than by choosing
a number. Ten tenths is one wand key, the same unit the Feature row screen uses, so the number means
the same on both screens. Steps of two tenths, clamped 1..200.

## Three-state language — still open, and why it was not built here

The button needs `English / Croatian / Auto`, and **Auto has no engine**: §28's Groq detection is
designed and not built, `MaLanguageGuess` is dead code that reads finished text rather than audio,
and a third state that silently resolves to English would give him English words for Croatian speech
— the exact failure that started this thread.

Two honest ways to build it:

1. **After 26.** Auto means what he asked for: the first seconds go to Groq, the answer is clamped by
   the not-English rule plus the free text check, and applied before the request is built.
2. **Before 26, as a stopgap:** Auto means "follow the language the last dictation actually turned
   out to be", using `MaLanguageGuess` on each finished transcription and remembering the verdict.
   Cheap, uses code already written, and honest so long as the button says so.

Do not ship a third state that has neither.

---

# 42. Dictating into a field nobody tapped

When nothing on screen has input focus, `activeWindowEditable` used to return
`findEditableDescendant(root, 0)` — **the first editable node depth-first, bounded to depth 6**. Two
things wrong with that for the case he described: first is whatever the layout declares earliest,
which is a search box at the top far more often than the composer at the bottom; and depth 6 does not
reach a composer inside a modern nested layout at all.

Now it collects every editable node and takes the **lowest on screen**, brings it into view, and
focuses it before writing. The box worth writing into is the one nearest the thumb, and on every
messaging screen ever built that is the last one down the page.

So: open the app, hold volume up, speak, release — the words land in the obvious box without tapping
it first to raise a keyboard he was never going to type on.

## Still open from this batch

- **Setup flow**: all-files access as its own step, restore-from-backup on the keys step, and a
  guided accessibility step that opens App info → ⋮ → Allow restricted settings and then the
  accessibility screen. §8 and §9 hold the earlier notes; this is now the priority of the three.
- **Three-state language + §28.** His design confirmed and correct: **English and Croatian skip Groq
  entirely** — the label is set, the language goes with the request, no detection call at all. Only
  **Auto** sends the first seconds to Groq, and the answer is applied when the request is built. That
  is exactly the seam §28 describes, and it means the cost of detection is paid only by the state
  that asked for it.

---

# 43. The setup flow, rebuilt

Seven steps now: enable IME, select IME, microphone, **all-files access**, keys, **accessibility**,
finish.

## All-files access sits before the keys, and that is the whole point

The previous version leaves a key backup in `Documents/`, and Android will not let the app open a
file it does not own without this grant. Asking for the keys first sent him to a picker to find a
backup the app was not allowed to read — which is exactly how it went.

## Restore, not just import

The keys step now offers **Restore keys from backup** above the file picker, reading
`MaVault.DISPLAY_PATH` directly: no picker, no remembering where the file was put, because the
location is known. It appears **only** when all-files access has been granted and a backup actually
exists, so it is never a button that cannot work.

## The accessibility step is two buttons in an order that looks wrong

A sideloaded app has its accessibility toggle **greyed out** until restricted settings are allowed,
and that lives behind the three-dot menu of App info. So the step sends him to **App info first**,
then to the accessibility list. Going straight to the accessibility list would show him a switch he
cannot move and no reason why.

## Polling, because these grants have no result

There is no launcher contract for all-files access or for the accessibility toggle — nothing comes
back when the user returns. So the wizard re-checks `MaVault.hasFullAccess()` and
`DictateAccessibilityService.isRunning` every 400 ms while it is open, and both are in the
`LaunchedEffect` key list; without that it would sit on a step already completed.

Both new steps can be skipped, and a skip is remembered for the session, so neither can trap someone
whose phone does not offer the setting at all.

---

# 44. The bottom row gives its width to the spacebar

`ctrl`, both switchers and `enter` were each `1.56f` — half again a letter's width. Four wide keys on
one row, and the spacebar took only what was left.

All four are now `1.00f`. **Nothing else had to change**: space carries `flayGrow = 1.0f`, so every
tenth taken from them is handed straight to it. Shift and delete keep `1.56f`, because they are the
two keys hit most often without looking and they have the edge of the row to aim at.

The switcher faces are `sy`, with the digit as a **corner hint**. It has its own branch in
`computeLabelsAndDrawables` rather than coming from a popup, because that digit is not a second
character the key can type — holding `sy` lists the layouts (§25), it does not insert a 1.

---

# 45. Global volume keys: what to check when they do nothing

Reported twice as not working. The code path is: config flag → service re-granted → preference on →
keyboard not in front. **All four are required, and three of them are invisible.**

1. `flagRequestFilterKeyEvents` and `canRequestFilterKeyEvents` — in `accessibility_service_config.xml`
   since build 124.
2. **The service must be re-enabled after that config change.** Android binds a service with the
   capabilities it had when it was switched on; a service left running across the update keeps the
   old ones and never receives `onKeyEvent`. Off and on again in Accessibility settings.
3. `dictate__ma_global_volume_keys` — **ships off**, switch in Gestures → Volume keys.
4. `FlorisImeService.ownsVolumeKeys()` must be false, i.e. the keyboard not on screen. That one is
   correct behaviour and is the intended case anyway.

**Worth building: make the state visible.** The Gestures switch should say whether the service is
actually receiving keys — a line reading "the service is running and receiving keys" versus "the
service needs re-enabling after the last update". Three of the four requirements currently fail
silently and identically, and no amount of explaining in a chat message substitutes for the screen
saying which one is unmet.

---

# 46. Build 124 broke the accessibility service. Reverted at 131.

**`android:canRequestFilterKeyEvents="true"` made the whole service unusable on a Nothing Phone 2a
running Android 16.** Not degraded — unusable: the accessibility toggle could not be turned on at
all, so the magic finger, TAB, the floating button and overlay dictation all died with it.

**Why.** Key filtering is the capability a keylogger needs. A sideloaded app that declares it is put
behind Android's "restricted settings" gate, and on that phone the App info overflow menu offering
"Allow restricted settings" **did not appear at all** — so there was no way through. §39 assumed the
cost of asking was a scarier warning. The real cost was the entire service, including every part
that had nothing to do with keys.

**Reverted.** The config is byte-identical to build 123. `onKeyEvent` and the global-volume-key code
remain but can never fire, which is harmless and keeps the work for when it can be tested.

**Do not re-declare that attribute without a way to verify on his phone first.** The next attempt
should be a separate debug APK, not the one he uses all day.

**This is also why §39 must be reconsidered rather than retried.** Dictating with no keyboard needs
another route entirely — the floating button and the OVERLAY sink already work without key filtering,
and that is the direction.

## Two shortcuts on the settings header

The build-number line now opens **App info** rather than the About page: it is the one line naming
the install, so it is where the hand goes when the install itself needs changing. Beside it,
**Accessibility** opens that settings screen directly. Everything built on the service dies together
when it is off, and Android switches it off often enough to deserve a door rather than directions.

## Still wanted: the log screen

He asked for a log with timestamps he can paste back. Nothing in the app records why the service is
not running, so every diagnosis so far has been guesswork across a chat window. **This is the next
thing worth building** — it is what would have found §46 in one step instead of three builds.

---

# 47. Rolled back: the volume keys act on press again, and the global handler is gone

**Volume up starts and stops the recording the moment it goes down**, as before build 121. The hold
was introduced so a short press could still be the volume while bhajan played. It solved that and
cost more than it saved: every dictation, dozens a day, began by waiting half a second for a key
that used to answer at once. **The volume is the rarer need while the keyboard is up, and it has an
easy answer — put the keyboard away and the keys are ordinary volume keys again.**

Volume down presses the magic finger term on the way down too, same reasoning.

Repeats are swallowed: a held key repeats, and a repeat would start a recording and stop it while
the finger is still down.

**The global handler is removed entirely** — `onKeyEvent`, `finishGlobalVolume`, both timestamps, the
hold constant, `dictate__ma_global_volume_keys` and its switch. It could not fire since §46 reverted
the capability, and a feature that cannot work is worse kept than deleted. §39 and §45 are history
now, not plans.

**Together, §46 and §47 undo builds 121 through 130's volume work.** What is left is what worked
before it: the keys are the app's while the keyboard is up, and the phone's the rest of the time.

## Not built, and asked for twice

**The log screen.** ~~Asked for repeatedly and missing~~ — **shipped, build 136. See §49.** Nothing in the
app records why the accessibility service is not running, which is why §46 took three builds and a
broken app to find. It should hold a timestamped list he can copy in one tap. **Build this before
anything else.**

---

# 48. The dot key has two functions again

Build 126 gave the dot its comma by setting a **symbol hint**, which was the wrong mechanism. A
symbol hint does not merely draw a glyph in the corner: `addComputedHints` also merges the hinted
key's own popup set into this key's. The dot ended up carrying a third thing. He asked for two
functions and got three.

Now:

- The layout gives the dot `popup.relevant = [comma]` and nothing else. **Tap is a dot, hold is a
  comma, and there is no third meaning to find.**
- The corner glyph is set directly in `computeLabelsAndDrawables`, beside the switchers' branch,
  because it is a label and nothing more.
- The §41 rule in `LayoutManager` that derived hints from `popup.main` is **removed**. It was the
  cause, and it existed to serve this one key.

**The lesson: a symbol hint is a behaviour, not a decoration.** To draw something in a corner, write
the label. To give a key a second character, give it a popup. Do not use one to get the other.

## Key widths, settled

Shift, delete, **ctrl and enter** are all `1.56f`, so the four corners of the two bottom rows match
and the block reads as one shape. Only the **switchers** stay at `1.00f` — they were merely wide,
not hit without looking, and their width is what the spacebar gained.

## The spacebar mark is drawn at 1.8×

`SnyggText` gained `fontSizeScale`, which **multiplies** the themed size rather than replacing it, so
the font scale, the theme and the user's size multiplier all still apply underneath. U+23B5 is a low
flat bracket about a third the height of a capital, so at letter size it read as a speck on the
widest key of the board.

---

# 48. The dot key, pinned to two meanings

It had three. A tap gave `.`, a second gave `…`, and the comma it was supposed to carry was buried in
a list — the wrong one on top and the wanted one hardest to reach.

The layout declares only a comma. The extra entries arrive from the **popup mappings merged in
`TextKey.compute`**, which run after the layout's own popup and add to it. Chasing which mapping
supplies them was the wrong fix: any future mapping could add more.

So the popup is now **cleared and rebuilt** for code 46, after every merge: main is the comma, and
nothing else. The corner label is set in the same file by the same code rather than derived from the
popup through §41's rule, which did not survive the merges on this key. **One place decides what the
dot key means and what it shows, and they cannot drift apart.**

## Bottom row, settled

Ctrl and enter return to `1.56f`, matching shift and delete, so the four corners of the two bottom
rows are one shape and the columns line up. **Only the switchers stay at `1.00f`** — they were the
keys genuinely too wide for what they say, and their width is what the spacebar gained.

The space mark goes from `1.8f` to `2.6f`. It is squat rather than small: a scale that would be huge
on a letter is modest on a glyph with no ascender, and 2.6 is about the height of a capital.

## The accessibility service is still not enabling, and I am out of ways to guess

The capability was reverted at 131 and the config is byte-identical to 123, which was working. If the
toggle is still refused, **the cause is no longer in this repo's diff** — it is the phone's state:
Android may have remembered the app as restricted from when 124–130 were installed.

Worth trying, in order: uninstall and reinstall (this clears the restricted flag), or Settings →
Apps → TTT mini → Force stop, then enable.

**And this is exactly why the log screen must come first.** Every diagnosis in this thread has been
inference from a chat message. Nothing in the app records whether the service ever started.

---

# 49. The log — shipped, build 136

Asked for four times before it was built, and he was right every time. Every diagnosis in this
project has been inference from a chat message; §46 took three builds and a broken app precisely
because nothing recorded whether the accessibility service had ever started.

**`log` sits in the settings header**, left of `access`. In the header rather than in the settings
list because that list is ordered by a stored preference — a new default entry would appear at the
*bottom* for anyone who has rearranged it, which is him, on every install.

## Shape

- **Writes to a file** in `filesDir`, not just memory. The interesting failures are the ones where
  something does not start, and the process dies and restarts constantly; an in-memory list would be
  empty at exactly the moment it was wanted.
- **400 lines**, then trimmed in batches of 100 so trimming does not cost a rewrite per line. Small
  enough to paste into a chat in one go — a log nobody can paste is a log nobody reads.
- **Never throws.** Every write is inside `runCatching`. A log that could break the accessibility
  service would produce exactly the silence it exists to end.
- **Newest first on screen**, oldest first in the file. Appending is what makes writing cheap;
  reading reversed is what makes the last failure the first thing seen.

## What it records

`app` start with build number, Android level and device model — so a paste identifies the phone
without asking. `a11y` service connected, **with flags and capabilities in hex**: a service can
connect with fewer capabilities than it asked for, which is silent and looks exactly like an app
bug. Also unbind and destroy, so "never started" reads differently from "started and died".
`dictate` commits, by **length not content** — his transcriptions are his writing, and the log is
meant to be pasted. `keys` the volume path, including whether the service was up and what the finger
found.

**Add a line whenever a failure would otherwise be silent.** That is the rule this screen is for.

---

# 50. The magic finger defaults are his row now

Was: `Copy message`, `Stop responding`, `Send`, `Use Image URL`, `Add URL`, `Generate Images`. Six
terms guessed at what a button-pressing keyboard ought to offer.

**He kept one.** The other five were keys he deleted on every reinstall, and he reinstalls
constantly, so the list cost him work each time and bought nothing.

Now: a **spacer 5.4 keys wide**, then **send**. The spacer is not decoration — it pushes send to the
right-hand end of the row, under the thumb that holds the phone, which is the entire reason the
spacer feature exists. Shipping the row without it would ship the feature without its point.

**`DEFAULTS_VERSION` deliberately stays at 1.** Bumping it would run `mergeNewDefaults` against
lists that already exist and append a second spacer to his. Defaults are for fresh installs here;
that flag is for genuinely new terms.

## The feature row defaults are NOT changed, and here is what is needed

He asked for these too, and only Row 1 was visible in the screenshot: **History, Settings, Dictation
view**. Rows 2 and 3 were not shown.

The feature row's default is a single flat list in `MaFeatureOrder.DEFAULT`, split into rows by
`MaRows` — so setting Row 1 alone means guessing the other two, and a wrong guess is worse than the
current default because it looks deliberate. **Ask for screenshots of Row 2 and Row 3, then set all
three together.**

The spacer width on that screen already defaults to 10, which matches what he has.

---

# 51. Ctrl+P and Ctrl+F stop typing the model's chatter into his messages

Two failures, both from his screenshots, both now guarded.

**Empty input reached the model.** It replied "I don't see any text to edit in your message", and
that sentence was written into the field he was typing in. The whole-field branch already refused a
blank field; the selection branch did not, and a selection can be reported present and come back
empty when the field is one the accessibility path cannot read. Now both routes end the same way:
say so, spend nothing, leave the field alone.

**The model answered instead of obeying.** "I've reviewed the text you provided. It appears to
contain only the phrase…" — pasted into a message to another person. The instruction forbids
preamble; the model ignored it. **No amount of rewording the prompt makes that impossible**, so the
guard is on the output rather than the input.

**The tell is length.** Proofreading and reflowing return roughly what they were given — shorter, or
a little different, never several times longer. Commentary about a short phrase is always far longer
than the phrase. Over `3 × input + 80` characters, the reply is refused, shown as an error with the
first 300 characters as detail, and **his text is left exactly as it was**. Deliberately generous: a
real correction never comes close, and guessing wrong this way costs one visible message rather than
a ruined one already sent.

## The status row at the bottom is still not done, and it is now the oldest debt

Asked for repeatedly since §12. It is a `Column` reorder in `TextInputLayout`: `Smartbar()` is
composed first, above the edit row, the number row, the keys and the feature row. Moving it after
the feature-row `Box` puts it where the pin already is.

It was not done here because the Ctrl+P failure was writing AI commentary into messages he sends to
people, and that had to stop first. **Do this next, before item 26.** The branch to be careful about
is the `isActionsOverflowVisible` / `GifSearchPanel` split at the top of the same `Column`, which
also composes a fixed-height `Box` and must keep doing so.

---

# 52. The status line moved to the bottom, and the automatic bucket

## The status line is under the keys now

`Smartbar()` was the first thing in `TextInputLayout`'s `Column`, directly above the keys — which
put the recorder, the errors and the word suggestions in the band of screen immediately below what
he was writing, covering the line he was reading. A keyboard takes the bottom of the screen and
gives back the top; a status line at the top of the keyboard took a second bite.

It is now composed **last**, below the feature rows and beside the pin, at the edge the eye already
goes to for the navigation buttons. Same component, so suggestions, the recording bar and the error
line all followed it down without any of them knowing. The `GifSearchPanel` branch still composes its
fixed-height box, since both sides of that `if` are measured the same way.

## The automatic bucket

`MaFeatureKey.AUTO_BUCKET`. Press once: the **last code block on screen** is copied. Press again:
the one above it. Again: the one above that.

**It only presses `copy code`.** Code blocks announce that name; the copy button under a whole
answer announces `copy message`. Confirmed from a real node dump (§37). So a chat full of both gives
up only its code.

**It does not touch the buckets.** `ClipboardManager.captureIntoClipSlots` already files every copy
into the next free slot and stops when they are full. This key only presses the right button in the
right order — which is why the feature is small at all.

**`MaScreenTargets.findIn` now takes a rank.** It sorts matches lowest-first instead of taking a
maximum, so "the next one up" becomes a question it can answer. Rank 0 is the bottom-most, which is
what every other caller wants and what `pressFirstMatch` still asks for.

**The face carries the count** — `A1`, `A2`, `A3` — because the count is the only thing about this
key that cannot be seen by looking at the screen. **Long press resets to A1**; without it one
mistaken press leaves the counter pointing at last month with no way back down the page.

The rank only advances on a press that landed, so running off the top does not keep climbing while
nothing happens.

`maBucketRank` is file-level Compose state, not `remember` and not a preference: collecting blocks
means leaving the keyboard to look at what was copied, so it must survive a close — but a rank
restored from last week would point at nothing.

---

# 53. Rolled back: the status line returns to the top, and the pin becomes a key

## The status line is above the keys again

Build 139 moved it to the bottom so nothing it showed could cover the text being written. **That
reasoning was sound and the result was still wrong.** At the bottom it sits below the feature rows,
past where his eye goes, and he reads it by hunting for it rather than by noticing it. Habit beat
the argument — which is the right way round for something read a hundred times a day.

**The lesson worth keeping: a correct argument about layout can still lose to where somebody's eye
already goes.** Ship the reasoning, but believe the person using it.

## The pin's strip is gone; the pin is a feature key

It held **half a key row, permanently**, for a switch set once and then forgotten. `MaFeatureKey.PIN`
now does the same job as an ordinary key that can be placed, moved or removed like any other.

The old strip existed on the argument that a pin removable from the row cannot be reached to put
back. True, and survivable: the same preference is in Settings, so unticking the key is inconvenient
rather than a locked door. **He was told the trade and chose it** — the same shape of decision as
`MIC` leaving `ALWAYS_ON`.

Filled when pinned, outlined when not — the pair the clipboard panel uses — and lit in sand while it
holds, like the row's other sticky key.

**`MaKeyboardPin.kt` is now referenced from nowhere.** Left in place rather than deleted at the end
of a build; delete it in a pass of its own, after confirming nothing else wants it.

---

# 54. Why it suggested "things" while he was typing "other"

**They were not wrong answers. The wrong question was being asked.**

`currentWordText` comes from the editor's composing region, and a great many apps never set one — a
web view, a dialog, anything drawing its own input. In those the prefix arrived **empty**, and an
empty prefix means something completely different downstream: `MaNgramModel.predict` stops filtering
by prefix and the bigram tier answers "what word follows `other`". Hence `things`, `parts`.

Now `MaNgram.predict` reads the word off the end of the text when the editor will not supply it —
the trailing run of letters before the cursor, apostrophes and hyphens kept so `don't` and
`well-known` survive. **And the context loses that word too**, or `contextOf` would count the
half-typed word as the previous one and predict what comes after `other` while he is still writing
it.

Which gives exactly the two behaviours he described, from one change: mid-word it completes the word;
after a space the trailing run is empty, the prefix is empty, and the bigram tier answers what comes
next. Tested against eight cases including the screenshot, `don't`, `well-known` and Croatian.

## On putting a model behind this

He asked again for AI. **§33 still stands and should be read before starting**: the only verifiable
candidate was 81 MB, English-only — the language that already had a dictionary — and close enough to
`MaNeuralPredictor`'s own 60 ms budget to disable itself. It would not have fixed this bug either;
the prefix was being dropped before any predictor saw it.

**What would actually raise the ceiling now, in order of value per effort:**

1. **Learn from a longer history.** `MaNgram` learns only from what is committed through this
   keyboard. Every transcription in `DictateHistory` is his own text and is already stored — feeding
   it in at first run would give the personal model months of his vocabulary immediately.
2. **An English base dictionary**, the counterpart of §33's Croatian one. Upstream's English list
   only serves the suggestion strip via its own provider, not `MaNgram`.
3. **A small multilingual model**, and only after 1 and 2 have been tried and measured. An
   English-only model remains the impressive answer to a question this app does not have.

---

# 55. The model learns from his history, the suggestion row can appear, and the spacebar stops doubling

## The word model reads the dictation history, once

`MaNgram` only ever learned from words committed **through this keyboard**, so a fresh install knew
nothing and had to be taught his vocabulary again by hand, over weeks — while every dictation he had
ever made sat in the history database. Thousands of his words, his phrasing, his subjects.

**That is a better corpus for predicting his writing than any general model**, because it is not a
sample of how people write, it is a record of how *he* writes.

`MaNgram.backfillFromHistory` runs after `initialize` (so it adds to the loaded model rather than
racing the load), takes the newest `BACKFILL_MAX = 2000` entries, skips anything over
`MAX_LEARN_LENGTH`, and marks `dictate__ma_ngram_backfilled`. **A flag, not an is-the-model-empty
check**: a model that has learned a little is still worth backfilling, and a second pass would double
every count. It logs how many it learned and the resulting word total.

## The suggestion row was structurally unable to appear

`MaBucketStrip` and `CandidatesRow` share one slot, and the bucket legend won **whenever any bucket
held text**. A bucket keeps its text until it is replaced — so for anybody who actually uses the
buckets, the legend held the strip permanently and the word suggestions could never be seen.

Reversed: candidates take the slot whenever there are any, the legend has it otherwise. Candidates
exist only while a word is being typed, which is exactly when they are wanted and exactly when nobody
is reading the legend. Patched in **both** smartbar layouts — the block appears twice.

## Two space marks on the numeric keyboard

`computeIcon` drew `Icons.Default.SpaceBar` on the numeric and phone layouts, from when
`computeLabel` returned null for space and the bar would have been blank. Since build 121 **the label
is the mark, on every mode** — so on those four layouts both fired, at two different sizes. The icon
branch is gone; the label is the one that stays, since it is the same glyph everywhere and scales
with the key face.

---

# 56. Why send worked in Claude and not in Gemini

**Not case, and not a missing label.** Both apps carry `contentDescription="Send"` on the button,
capital S. His two dumps made the real cause visible.

`matchOf` builds its label from description **plus text plus view id**. In Gemini the send button is
wrapped in a `ComposeView` whose id reads `assistant_robin_input_send_button_compose` — no name of
its own, but the id contains the token `send`, so **both nodes matched**. The wrapper's box is 26px
taller than the button's, and bottom-most wins, so the wrapper was chosen. Its nearest clickable
ancestor is `assistant_robin_chat_input_half_sheet` — the whole input sheet. The finger tapped the
text field, focus moved, and nothing was sent.

Claude has no such wrapper, so its only match was the button. That is the entire difference, and it
is why "try both commands" would not have helped: the term was right both times.

**Fixed by ranking, not by matching.** `isAnnounced` asks whether the match came from a name the app
*announces* — a description or a text — rather than from a view id. Announced names sort first;
position only decides between equals. A description is what a person means by a button's name; an id
is a programmer's spelling that leaked into the tree.

Verified against both dumps before building: Gemini now picks the button rather than the wrapper,
Claude is unchanged.

## The bucket red, removed (build 144)

Asking was right: he wanted **no behaviour change at all**. Buckets fill 1, 2, 3 up to the last
visible one and then stop accepting, exactly as before. The bin key empties them and they fill again
from the first.

**The red was the whole complaint.** Full is not a fault — it is the ordinary end of filling them —
and red on a keyboard reads as something being wrong, so every time he used the feature as intended
it sent him looking for a problem that did not exist.

Three places drew it, all now gone: the number in `MaBucketStrip`, the C-key labels, and the bin key
that turned red "so the row shows both the problem and its answer". `bucketsFull` and `MaRecordRed`
were left with no readers and were removed with them. `MaClipCapture.isFull` still governs whether a
copy is accepted — the state is unchanged, only its colour.

**The rule worth keeping: red is for a fault, and a full container is not one.**

---

# 57. Send has one definition and two triggers

The send key on the magic finger row and a long press on volume down are **the same press made two
ways**. They were nearly able to disagree: volume down held its own copy of the word in
`dictate__ma_volume_down_term` and pressed that directly, so renaming the term on the Magic finger
screen — or teaching send a longer name because some app announces it differently — would have fixed
the key on the row and left the hardware button pressing a word that no longer existed. Silently,
and only in the app where it mattered.

`MaMagicTargets.resolveTerm(targets, name)` is now the single source: the stored name is looked up
among the taught terms, by face or by term, and the target's own term comes back. Falls back to the
name as given, so a hand-typed term still works and a list that has not loaded cannot make the
button dead. The log records both — `volume down: 'Send' -> 'Send'` — so a mismatch is visible
rather than inferred.

**Written into HANDOFF as a standing rule**, because he asked for it to survive the session: any
future way of firing send joins the same way. One list, one term, no copies.

---

# 58. The automatic bucket and the bin are one mechanism

**A-bucket shipped in build 139** (§52) and he asked for it again, which usually means it was not
findable. Two things fixed rather than rebuilt:

**The bin now resets the ladder.** They are halves of one thing: A-bucket presses copy buttons and
the clipboard capture files each copy into the next free slot. Clearing the slots without clearing
`maBucketRank` left the two disagreeing — buckets ready for the newest block, the ladder still
pointing eight blocks up the page — so the next press collected something from far above and dropped
it into bucket one. The bin is the reset for both, which is what he expected, and it saves a second
control nobody would remember. Long press on A-bucket still resets it alone.

**It sits beside the bin in the default order**, not off among the unrelated keys. He asked for it in
the same part of the row as the buckets; the bin is where the eye already goes for them.

Behaviour is otherwise as built: press for the last code block, press again for the one above, `A1`
`A2` `A3` on the face showing which is next, `copy code` only so message-copy buttons are never
touched.

**The limit remains Android's** (§37): recycled list rows are absent from the tree, not hidden in it,
so one run collects the blocks near the viewport rather than a whole conversation. A finger that
scrolls and re-searches is what would lift that, and it is still unbuilt.

---

# 59. A-bucket scrolls the ladder along

After a copy lands, A-bucket now **reveals the next block up** —
`MaScreenTargets.revealMatch`, which is `findIn` with `clickIt = false`: it finds and scrolls into
view, and does not press.

**Two things at once, and they are the same action.**

The list scrolls to show the block about to be collected, which pushes the one just collected down
toward the bottom of the screen. So the last thing copied is the last thing visible, and he can read
down the page to check what went into the buckets instead of counting presses. That was his idea and
it is a better confirmation than any indicator on the key.

And it is what makes a long conversation reachable at all. His ten-box dump proved the limit exactly:
seven boxes present (two of them `hidden` but complete), one caught mid-recycle with its container
but no button, and **three absent from the tree entirely**. Rows far from the viewport do not exist
until something scrolls near them. Revealing the next block is what causes the ones above it to be
built — so the ladder keeps climbing instead of stopping at whatever was in memory when he started.

**Also confirmed from that dump:** each Claude code box carries exactly one copy button,
`desc="Copy code"` at the top right, with `Expand code` beside it. The fenced-backtick copy he wants
to avoid is `Copy message`, under the whole answer, and A-bucket has never targeted it.

---

# 60. The top strip always has something to say

The strip is meant to follow what he is doing, and one common case said nothing at all: a plain copy.
Now it shows `copied` and the clipboard text when no bucket holds anything.

**The order it resolves in, highest first:**

1. **Recording** — the recorder bar, in `DictateSmartbarUi`.
2. **Typing** — word suggestions. They exist only while a word is in progress (§55), which is exactly
   when they are wanted.
3. **Buckets** — the legend, number and contents per slot. Something he put there deliberately.
4. **Clipboard** — the last thing copied. Merely the last thing that happened, so it sits below a
   bucket, and it answers the question he would otherwise open the clipboard panel for.
5. Nothing.

**`maBucketStripHasContent` had to learn about the clipboard too.** The strip computes what to draw,
but the caller decides whether it gets the slot at all — so a clipboard line computed inside a strip
that is never composed would never have appeared. Two places, one rule; they are asked in the same
order.

`primaryClipFlow` is a `StateFlow`, so it needs Compose's `collectAsState` while the preferences
around it use JetPref's. Imported under an alias, the way `DictateHistoryScreen` already does it.

---

# 61. The dictation view can be pinned as the opening view

The gear is gone from that screen and a **pin** takes the corner.

**Settings did not deserve the corner.** It was there on the reasoning that it is opened rarely and
deliberately — which is the argument for it *not* having a key on the screen he uses most. It is
already on the feature row and in the app; a third route was worth less than the corner it occupied.

**`"dictation"` is restored to `maOpeningView`.** It was removed earlier on two arguments. One was
wrong: the view shows a mic key, it does not press it, and recording on open is a separate setting.
The other — that this screen is always secondary — was mine to assume and his to decide. **He
dictates far more than he types**, so for him the keyboard is the secondary screen, and until now
every open began on the wrong one.

The pin is the **only** thing that sets this preference, so it can always be unset from the same
corner it was set in. That reversibility is what the earlier removal was really protecting against,
and it is cheaper than removing the option.

Filled when pinned, outlined when not, as everywhere else in this app.

---

# 62. The cursor trackpad, and the road to flicking

Hold the spacebar and the keyboard becomes a pad: drag anywhere to move the caret. Lift to leave.

**It replaces the language picker on that long press.** Choosing a language is rare and deliberate
and has two other routes — the badge and a long press on volume down. Moving a caret through
dictated text is constant, and doing it by tapping at the text is the least accurate gesture on a
phone, because **the finger covers exactly the character being aimed at**.

`gestures__space_bar_long_press` is now ignored rather than read. Everything its old values could
choose is reachable elsewhere, so there was nothing to lose by not asking.

**Shape.** The pad fills the keyboard and is drawn last in the `Box`, so it takes the touches and
nothing underneath can be pressed — that is what makes it a mode rather than a hint. Only the
keyboard, so the text stays visible above and he can watch the caret move.

Horizontal drag steps by character (28px), vertical by line (56px). **Distance accumulates** and is
spent in whole steps: a threshold that reset each event would ignore a slow, careful finger, which is
exactly the finger this is for. Arrows go through `inputEventDispatcher`, the same pipe every key
uses, so the editor handles them by its own rules.

Lifting closes it. No confirm, no cancel — the caret moved with the drag, so what he sees on lifting
is what he gets, and a mode needing dismissal is a mode left open by accident.

## Next: flicking

He described the direction this opens. Instead of holding a key and waiting for a popup, **swipe
towards the second symbol printed on it** and get that symbol. Same information already on every key
face (§41 put the comma there, §48 pinned it), no wait, no popup.

The parts are in place: `TextKeyboardLayoutController` already tracks pointer movement per key for
the glide typing and delete-swipe paths, and every key already knows its hint. What it needs is a
direction test on release and a decision about how far a flick must travel before it stops being a
tap. **Build it after living with the pad**, since both change what a finger on a key means.

---

# 63. Configurable transcription view — designed, NOT built

**What he asked for:** every button on the transcription view except the pin can be swapped for any
feature-row key. Settings shows two rows — the original and the replacement — with an edit picker and
a reset.

**Why it was not built in one pass, measured rather than guessed:** the feature-row key rendering is
a single `when (button)` block of **556 lines** (`MaFeatureRow.kt` 427-983) closing over **18
locals** — scope, context, prefs, editorInstance, keyboardManager, bucket slots, prompt state,
language state and the rest. Nothing outside that composable can draw a feature key today.

## Three builds, in this order

**1. Extract the renderer.** One composable that draws any `MaFeatureKey`, given what it needs.
Behaviour-neutral: the feature row must look and act exactly as before, which is also how it is
verified. **This is the whole risk of the feature** — a rushed extraction does not fail red, it
quietly drops a behaviour in one of thirty branches and neither of us notices for days. Do it alone,
with nothing else in the build.

**2. The override.** A preference mapping slot id → `MaFeatureKey` id, empty meaning default. Slot
ids for the eleven swappable positions; **the pin has no slot and cannot be overridden**, since it is
the only way back out of a pinned opening view (§61).

**3. The settings screen.** Both rows visible at once, original above replacement, so a swap is read
as a substitution rather than as a list of unrelated keys. Reset clears all overrides.

## Shipped defaults, unchanged by this

The view he screenshotted is the intended default: `ENG ab AB Ab AbAb` / `db mic backspace` /
`pin keyboard space enter`. Overrides sit on top; reset returns to exactly this.

---

# 64. The cursor pad trapped him. Three ways out now, and it works in both views.

**Build 150 shipped a mode with no exit.** The pad closed only on the *end of a drag*. A finger that
pressed and lifted without moving did nothing, so the pad stayed over the keyboard and the app was
unusable until it was force stopped. He tapped it repeatedly — the one thing anybody tries — and
nothing happened.

**The lesson, and it is the important part: a mode with one way in needs a way out that works when
the first thing tried is doing nothing.** A gesture handler that only reports movement cannot be the
only exit, because the natural response to being stuck is to stop moving and tap.

Three exits now:

1. **Tap anywhere** — `detectTapGestures`, declared before the drag handler so a tap is heard even
   when the drag detector sees nothing worth reporting. The label says so: *tap to close*.
2. **Lift after a drag** — as before.
3. **Any new field** — `MaCursorPad.close()` in `onStartInputView`, unconditional. Even if every
   gesture failed, switching app or tapping another box returns a working keyboard.

**Vertical**: `STEP_Y` was 56px, half a key, so a natural upward drag ended before the first step was
spent. Lowered to 34. The arrow codes were fine — `KeyboardManager` sends `KEYCODE_DPAD_UP/DOWN` and
they are repeatable — so this was distance, not dispatch.

**It works in the transcription view now.** That view has no letter keys, so moving through a long
transcription meant tapping at the text, which is exactly what the pad replaces, on the screen where
the text is longest. Same hold, same pad, both keyboards.

**Rule: draw the pad wherever it can be opened.** A long press that raises a pad the screen never
renders is this same trap in a place with fewer ways out.

## Asked for and not built: SwiftKey-style prediction

Two distinct things, both real, neither small:

**Middle-weighted suggestions.** The likeliest word in the centre, larger, with weaker candidates
smaller on each side. This is presentation over the existing ranking and is the cheaper half —
`CandidatesRow` already receives an ordered list; it would draw index 0 centre-large rather than
left-first.

**Learning whole strings — emails, names, addresses.** Type `m` and get the whole address. This is
not the n-gram: it is a store of complete tokens seen often, keyed by prefix, ranked by frequency and
recency. §55's history backfill is the corpus for it, and every email he has ever dictated is already
in `DictateHistory`.

**Do not install SwiftKey to copy it.** The behaviour is describable without it, which is what he did.

---

# 65. The trackpad: stays open, and can select and delete

**It stays open now.** It used to close whenever the finger lifted, which meant one journey per hold
— to move, then select, then delete, he had to raise it three times. **A trackpad that shuts every
time the hand leaves it is not a trackpad, it is a long gesture.**

That is safe in a way build 150 was not, because there is now a **visible** way out. The trap was
never that it stayed open; it was that nothing on screen said how to leave.

**Three corners, and the fourth deliberately empty:**

- **Top right — keyboard icon**, closes. His choice over an X, and he was right: the icon already
  means "back to the keys" everywhere else in this app.
- **Bottom left — shift**, toggles selection. Lit in sand while it holds, because it changes what
  the next drag does and nothing else would say so.
- **Bottom right — backspace**, deletes.
- **Top left — nothing.** It is where a thumb crosses the pad on its way anywhere, and a key there
  would be pressed by accident more often than on purpose.

Keys rather than gestures: the pad is already one gesture, and a second and third layered on the
same finger would make every drag a guess about which was meant.

**Selection reuses the editor's own manual-selection mode.** `KeyboardManager` already extends the
selection on an arrow whenever `activeState.isManualSelectionMode` is set — so turning that on makes
every arrow the pad sends select instead of move, with no special cases and identical behaviour to
holding shift.

**Both exits clear it.** Closing the pad and opening any new field both unset the flag. Left set, the
next arrow from any key would extend a selection long after the pad was gone, with nothing on screen
to explain it.

---

# 66. The pad was drawn on top but did not receive touches

He pressed the close key and nothing happened, while his presses landed on the **keys hidden
underneath** — typing blind through a window he could not shut.

**Cause: `pointerInteropFilter` on the parent.** `TextKeyboardLayout` puts that filter on the
`BoxWithConstraints` that contains both the keys and the pad. It forwarded every touch to the key
controller and returned `true`, consuming it. A parent interop filter runs **before** its children,
so the pad's corner buttons never saw a click at all.

**Being drawn last is not the same as receiving touches.** Build 153 assumed z-order was enough. It
governs painting; the interop filter governs delivery, and they are decided in opposite directions.

Two changes:

- The filter **returns false while `MaCursorPad.active`**, handing events to the children instead.
- The pad **absorbs stray taps** with a `detectTapGestures` that does nothing. A press without
  movement is not a drag, so without this it fell through to the keys. Deliberately no action: the
  middle of the pad is for dragging, and a pad that vanished on a stray tap would be build 150's
  problem inverted.

`LegacyDictateLayout` has no parent interop filter — its gestures are per-key modifiers — so the pad
already worked there.

---

# 67. Every spacebar already opens the pad — and now says so in the log

He reported the hold working only on the letters keyboard. Traced rather than rebuilt, and **all four
routes were already wired**:

- **Typing and numeric/phone keyboards** — one `onLongPress` branch in `TextKeyboardLayout` covers
  every mode it draws, and all four numeric/phone layouts do carry a space key (verified in the
  layout JSON, `western_arabic.json` and `telpad.json`).
- **Transcription view** — `LegacyEditAction.SPACE` got `onLongClick` in build 152, and the overlay
  is drawn in that view's outer `Box`.
- **Feature row** — its space key *is* `LegacyEditAction.SPACE`, rendered by `LegacyActionKey`, so it
  inherited the same long press.

**What was almost certainly wrong was build 154's fault, not the wiring.** Until then the parent
interop filter ate every touch, so the pad opened and could not be used or dismissed — which reads
from the outside as "nothing happened". He was testing 153 or earlier.

**`MaCursorPad.open()` now writes `pad opened` to the log.** Every spacebar in the app routes through
that one function, so a hold that writes nothing never called it, and a hold that writes a line but
shows nothing is a drawing fault instead. Two very different problems that look identical from the
outside — and this is exactly the class of question the log was built for (§49).

---

# 68. A suggestion replaces the whole word, and the best guess sits in the middle

## The fragment bug

Tap into the middle of a word, tap a suggestion, and you got three fragments: half the old word, the
whole new one, the rest of the old word. With no composing region there is nothing marking which word
was meant, so `commitCompletion` fell back to inserting at the cursor — and the cursor is wherever
the finger last landed.

`EditorInstance.commitCompletion` now **selects the whole word around a collapsed cursor** before
committing, so the commit replaces a selection instead of inserting at a point. Apostrophes and
hyphens count as part of the word. **A selection he made by hand is left alone** — that is a
deliberate statement about what to replace, and widening it would overrule him.

## Middle-weighted candidates

The likeliest word is now **centre**, not left. Reading order puts the best guess where the eye
starts, which sounds right and is not: the thumb rests under the centre of the row, so the word most
often wanted was the furthest from the thumb and every acceptance cost a reach.

Only when there are exactly three. With two there is no middle; with four or more the centre stops
being one obvious place.

**This introduced a wrong-word bug that had to be fixed in the same pass.** The click handler read
`candidates[n]` — the *original* list at the *displayed* position. Harmless while those were the same
list, wrong the moment the reorder made position 0 hold candidate 1: tapping the left word would have
committed the middle one. The loop variable is now captured by value. **A reorder of a displayed list
means auditing every index that reaches back into the original.**

**Still to do: the middle one drawn larger.** Sizing lives in the Snygg stylesheet for
`SmartbarCandidateWord`, so it needs a style variant rather than a font size hardcoded here — which
would ignore his theme.

---

# 69. Step one of the modular rebuild: keys become modules

**The philosophy, in his words: Lego, not a moulded shell.** The same blocks make a castle or an
aeroplane because a block does not know what it is part of. Every key in this app was written inside
the row it belonged to, so a key on a different surface meant building it twice.

**`MaKeyModules.kt` holds the first three:** `MaPinKey`, `MaCaseKey`, `MaNextFieldKey`. Each takes
what it needs as arguments and nothing else, and can be drawn by the feature row, the transcription
view, a future row, or a test. `MaFeatureRow` now names the key and says nothing about what the key
is — 1242 lines to 1180.

## The contract for every key still to move

A module may take a `Context`, an `EditorInstance`, a `KeyboardManager` or a preference — things that
exist everywhere. **It may not take the feature row's own state.** Where a key seems to need that,
the state belongs somewhere shared, and moving it there is part of the job rather than a reason to
leave the key behind.

`MaNextFieldKey` is the worked example: reading the keyboard's shift state is the *row's* business,
because the row sits under a keyboard. The key only needs to know which way to walk, so it is told —
and the same module works on a surface that has no shift at all.

## What had to open up, and it was small

`ThemedTextKey` was private and is now `internal`; `maOpenAccessibilitySettings` likewise.
`ThemedIconKey` was already internal, which is why the icon keys were the easy half. **A key that can
only be drawn inside one file is a key that can only live in one row** — that visibility was the
whole cage.

## Order for the rest

Move the keys that depend on least, one or two per build, each behaviour-neutral. The hard ones —
the buckets, the magic finger row, the mic — depend on row state and should come last, after the
easy moves have shown where shared state actually needs to live. **Do not rewrite the row.** He uses
it every day, and the row keeps working throughout by construction.

Once enough keys are modules, §63 becomes small: the transcription view is just another surface that
names keys.

---

# 70. The likeliest word is now drawn larger

Finishes what §68 left half done. The centre word — the best guess — is drawn at `fontSizeScale =
1.2f`, its neighbours at the theme's own size.

**A scale, not a size.** `SnyggText.fontSizeScale` multiplies whatever the stylesheet decided, so a
larger or smaller keyboard font stays larger or smaller and the emphasis stays a fifth bigger than
its neighbours either way. A hardcoded size would have looked right on his theme and wrong on every
other — which is why §68 declined to fake it and left a note instead.

**Modest on purpose.** It has to read as "this is the one" at a glance without making the row jump
about as the ranking changes between keystrokes.

Applied to the primary word only, not the secondary line. And it follows the same rule the ordering
does: with three candidates the middle is emphasised, with any other count the first is, because
with two there is no middle and with more the centre is not one obvious place.

---

# 71. Exclusive recording: ask, but never listen

He asked for it back, and §29 anticipated exactly this: *"a future request should be read as a
request to change his mind, not as a gap to fill."* It is that request.

**What is different from the version §29 removed.** The old code also *listened* for focus loss and
paused **his recording** whenever another app took focus. With a reader playing in the background the
two rules met — each program politely stopping for the other — which is why it read as backwards.

**The listening was wrong, not the asking.** The new one asks and does not listen. The required
change listener is deliberately empty. **Nothing another app does can interrupt a recording; only he
can stop it.** That is the rule to keep.

**`AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE`**, not plain transient: other players *pause* rather than
duck, and do not resume until focus is abandoned. Ducking would leave his bhajan playing quietly into
the microphone, and a quieter recording of the wrong sound is no improvement on a loud one.

Requested before the microphone opens; released in `cleanupAudioRouting`, which **all six** exit
paths already call — sent, cancelled, failed or torn down — so the music always comes back. Both ends
are logged, so "the music never resumed" can be checked rather than guessed.

---

# 72. Ctrl+P and Ctrl+F carry his own wording

**The answer to "is there a place to write my own prompt" was no.** Both keys used a throwaway
`PromptModel` built from a constant, deliberately kept out of the prompt library so a renamed or
deleted library row could not leave a shortcut doing nothing (§30). That protected the shortcut and
locked the wording with it.

Now: **Keyboard shortcuts → Your own wording**, one box per key.

- **Empty means use the shipped instruction.** The default is `""`, not a copy of the text — so the
  built-in can be improved later without overwriting what he wrote, and "reset" is clearing a field
  rather than remembering what the original said.
- The shipped text shows as a **placeholder**, not as content. Filling the box would make every
  default look like something he had written.
- **Saved as he types, read fresh on every press.** No Save button, because there is nothing to get
  wrong: a half-finished sentence affects only the next press and is fixed by finishing it.

## Ctrl+F now carries the prompt he wrote

Shipped **word for word**, down to the sentence order, and the class comment says not to improve it.
The output goes out as his own words under his own name, so a prompt smoothed into something more
standard-sounding would produce messages that read like anybody.

**It does not say "return only the text", and that is left alone on purpose.** §51's length guard
catches the failure that line would have prevented — a reply far longer than its input is refused and
shown rather than typed. **A guard on the output beats a line in the prompt, because a model can
ignore the line and cannot ignore the guard.**

`NAME` is still "Better flow", so the spinning dust reads the same. Worth renaming when he says what
he wants it called.

---

# 73. Settings back up beside the keys

**`backup` at the top of settings**, next to `log` and `access`. One press writes every preference to
`Documents/TTTmini/settings.jetpref` — the same folder as the key backup.

**Why not the existing backup screen.** That one makes a zip through a file picker: choose a place,
name it, find it again later. Right for an occasional archive, wrong for somebody who reinstalls
several times a day. **A backup whose location has to be remembered is not there when the phone is
wiped.** This writes to the one place the app already knows how to find, under a fixed name, so a
fresh install can look for it without being told where.

**No picker, no confirmation.** Pressing twice is harmless and there is nothing to name.

**The same folder as the keys, deliberately** — one grant of all-files access (§43) covers both, and
a restore that means finding two files in two places is a restore that gets half done.

**Separate files, also deliberately.** Keys are secrets that outlive any version of this app;
settings are shaped by it and may not survive a large enough change. Losing one must never cost the
other.

**Setup offers it**, above the key restore, because redoing settings by hand costs him more. Offered
rather than applied: it overwrites what is currently set, which is right on a fresh install and a
real loss on a configured one, so the choice stays his.

`ImportStrategy.Merge`, not `Erase` — a setting the backup predates keeps whatever this version
decided for it, instead of being blanked because an older file had never heard of it.

Uses `FlorisPreferenceStore.export`/`import`, the same calls the backup screen makes, so the
datastore's own format and version handling are not reimplemented here to drift.

---

# 74. One prompt, one place

**Two places held the same two prompts on one screen** — the shortcut rows described them, and a
section below let you edit them. One key, two locations, and the question of which was real.

Now the row **is** the editor. The name is blue and underlined, the way a link is on the settings
home, so it reads as something that opens. Tapping it opens a dialog with the prompt in it.

**What the row shows is the prompt actually in use**, not a summary of it: his wording if he has
written one, the shipped one otherwise, three lines and then an ellipsis. A summary is a second
thing to keep true and was already drifting from what the key really did. `your wording` appears
under it when it is his.

**Saving the shipped text unchanged stores nothing.** Only a real edit becomes his, so a prompt left
alone keeps following the default if that is improved later. `Use default` appears in the dialog only
when there is something to clear.

**A dialog rather than an inline box**, because these are paragraphs: a text box inside a list is
either too small to write in or too tall to scroll past.

## Ctrl+F's default is his current wording, exactly

Shipped word for word. The earlier version ended on a line reading "Text to rewrite:" — **that line
was mine, not his**, and it is gone with the rest of my edits to his words. The rewording path
appends the text after a blank line, so nothing has to announce it.

**If a model ever starts treating the trailing sentence as text to rewrite**, the fix is a separator
in the *path*, not a line added to his prompt.

---

# 75. The number row stands on its own

Key 1 did nothing whenever the main keyboard was folded away. `MaExtraRow` was gated on
`maZoneKeyboard` **as well as** its own switch, on the reasoning that folding the keys should take
the number row with them.

**That reasoning treated it as part of the keyboard, and it is not.** It is a row he switches on and
off with key 1, exactly as key 3 does for the copy row — so tying it to the keys made a switch that
silently does nothing in the state where it is wanted most. One borrowed condition removed; its own
switch still decides.

## Tested by the four (§ his Four Tests, 18.8.2026)

**Test 1 — the mechanism alone.** The gating rule as a truth table, six cases: everything on, keys
folded with the row on (the bug), row off with keys on, both off, and the overflow panel open over
each. 6/6. **Row and keys resolve differently in 2 of the 6**, which is what proves they are now
independent rather than one condition wearing two names.

**Test the test.** Restored the old rule and re-ran: 1 failure, and the independence count dropped
from 2 to 1. The test watches the thing it claims to.

**Test 4 — the upgrade.** `dictate__ma_extra_row` keeps its key, its default and its meaning; no
migration touches it. His stored choice survives. **One visible consequence, stated rather than
discovered:** anybody upgrading with the row switched ON and the keys folded will now see the number
row appear where it did not before. That is the fix working, not a regression.

**Tests 2 and 3 were NOT run.** Both need the app on the phone: whether the row lays out correctly
with no keyboard beneath it, and what the keyboard height becomes when the row is the only thing in
zone two. Neither can be reached from here. **The height arithmetic is the one to watch** — the note
in `TextInputLayout` says a previous attempt at row-by-row hiding left a band of nothing where the
keys used to be.

---

# 76. Spoken formatting commands

Say **"the word parenthesis"** and get "the (word)". **"hello dot"** gives "hello." Also
`question mark`, `exclamation mark`, `uppercase`, and Croatian `zagrada`, `točka`, `upitnik`,
`uskličnik`, `velika slova`.

**Only as the last word, and it is removed** — the same rule as §26's voice commands, for the same
reason: a rule that fired anywhere would eat the word "dot" out of a sentence about a dot, and
dictated words that get eaten cannot be undone back to.

**Stacking falls out of the rule**: "hello world uppercase dot" applies the stop, then uppercase,
because after removing one the next is now last.

`uppercase` takes the whole **preceding sentence**, not the last word — he said "everything, what I
just said", which is more than one word. **Say if that is wrong**; the other reading is one line.

Both languages recognised at once, since he switches mid-thought.

## Tested by the four

**Test 1 — 19 cases, 0 failures.** The cases it is for, the cases it must **refuse** ("the dot on the
map", "put it in parenthesis for me", "uppercase letters are useful" — all untouched), both
boundaries (empty, one word, command alone), transcriber punctuation, and the two-word commands.

**It caught a real bug.** "hello world uppercase dot" lost its full stop: the stop was added, then
consumed with the word it had attached to. Fixed by a rule rather than a patch — **transforming
commands preserve trailing punctuation, ending commands define it** — because re-appending in both
cases would give "Hello world..".

**Test 3 — every ugly case survives.** Empty, whitespace only, one word, commands only, 5000 words
(1.65 ms), malformed "....!!!", hostile "(((( parenthesis", stacking past the bound, mixed language,
Croatian diacritics. Nothing throws. **Idempotent**: applying twice equals applying once, on all four
sampled forms.

**Known defect, found by test 3 and NOT fixed: newlines are flattened.** "first line\nsecond line
uppercase" returns one line. It only bites when a command is used on multi-line text, and the fix is
to operate on the last line rather than the whole string. Written here rather than rushed in.

**Tests 2 and 4 not run.** Test 2 needs the phone. Test 4 has nothing to check: no stored data, no
preference, no format — a fresh install and an upgrade behave identically.

## Still queued from the same request, NOT built

**The CC settings screen** — keys visible and testable, prompt groups for Ctrl+P and Ctrl+F, several
named prompts each with a radio button, `new` to add one, **each shown as ONE collapsed line** that
opens a full editor. His words: it must read like meditation, one line, no scrolling.

**Better Ctrl+P and Ctrl+F prompts** — "professional proofreader", and reflow that does **not change
meaning**, which is his repeated complaint.

**Offline resilience** — retry, then a **resend** button in the status line when it gives up.

**History resend and status** — every recording marked transcribed or not, colour coded (his
suggestion: yellow done, blue not), with resend, so no dictation is ever lost.

---

# 77. Voice formatting: the full set, with pronunciations he can hear

**31 spoken marks**, grouped by what they do to the text: ending a sentence, wrapping the last word,
following the last word, and the whole dictation. Settings → **Voice formatting**.

**Every row can be switched off.** A command is a word taken out of his sentence, and the commoner
the word the more often that is wrong — "at", "plus" and "minus" appear in ordinary speech
constantly. Ticking is how he decides which trade is worth it, mark by mark.

**Stored as the ones switched OFF**, not the ones on, so a mark added in a later version is live by
default instead of invisible to anybody whose saved list predates it. Empty means everything works,
which is also the right first run.

## Underscore is the odd one, deliberately

Every other command reaches back one word or one sentence. `underscore` takes **the whole
dictation** and replaces every space, because it is for naming a file and a file name is the whole
line or nothing.

## The recordings

His English carries an accent the transcriber sometimes mishears, and a misheard command is worse
than a missing one — it types a stray word into the middle of a sentence. Reading "say: ampersand"
does not say which sounds the recogniser wants. Each row has a speaker that plays the word in
**Received Pronunciation**.

Made with Hume TTS, converted to **Opus 24 kbit mono: 31 files, 76 KB total, ~2.5 KB each** — less
than one photograph. Raw WAV was 121 KB *per file*; the conversion is what makes carrying all of
them free.

**The id is the filename and the command word**, one string tying row, audio and matcher, so they
cannot drift apart. Verified: 31 catalogue entries, 31 files, none missing, none orphaned.

## Tested

**Test 1 — 18 cases, 0 failures** on the expanded set: every group, both languages, the refusals
("the hash of the file", "meet me at the door", "underscore is a character" — all untouched), and
stacking.

**It caught two real bugs.** "my file name underscore dot" lost its full stop, and "the word
parenthesis comma" lost its comma — the suffix rule only preserved `.!?` and underscore preserved
nothing. **A comma placed by "comma" is as much his as a stop placed by "dot"**, so the rule now
covers `.!?,;:` and underscore keeps it too.

**Test 2 on the API — real key, real call, HTTP 200**, and the failure path was real as well: 15 of
31 came back **429**, which turned out to be a per-minute limit rather than a quota, proved by a
single call succeeding after a pause. Pacing at 12 s produced 31 of 31.

**Not tested: playback on the phone.** Whether Opus in assets plays through `MediaPlayer` on his
Android 16, and whether the volume is right against his ringer. Both need the device.

## The key

Used from the sandbox only, never echoed, never committed, never logged. It stays available for the
rest of this project and is shredded when he says the work is done.

---

# 78. AUTO language, at last (item 26)

The badge has three states now: **ENG, HR, and AUTO**. On AUTO the recording goes to Groq's Whisper
first, and the answer decides what AssemblyAI is told.

**The question asked is "is this English", not "which language is this"** — his rule, and the right
one. Measured with real speech through the real API: English audio returns `"English"`, Croatian
returns `"Croatian"`. But Croatian is routinely heard as Slovenian, Serbian, Czech or Polish, while
English is almost never mistaken for anything. Everything not recognisably English becomes Croatian,
**which also fails in the safe direction**, since Croatian must never reach the sync path and a wrong
answer here only makes it slower.

**Null is a real answer.** No Groq key, no network, a timeout, a refused key: the language stays
exactly as it was set by hand. **AUTO can only improve on the manual setting, never break it** — which
is what makes it safe to leave on.

**The badge shows what AUTO settled on** — `A·HR`, not just `AUTO` — because a badge that hid the
resolved language would remove the one thing a badge is for, and a wrong detection would arrive later
as a bad transcription instead of being visible at a glance.

## Three traps, all real

**The scope is `Dispatchers.Main`.** `transcribe` is not a suspend function, so the probe is pushed
to IO with `runBlocking(Dispatchers.IO)`. A network call left on Main does not merely freeze the
keyboard on modern Android — it throws `NetworkOnMainThreadException`.

**The User-Agent is load-bearing.** `api.groq.com` sits behind Cloudflare, which refuses a request
with no browser-like User-Agent with 403 and an HTML body, indistinguishable from every key being
dead. Do not remove it.

**One badge function, no arguments.** Four places draw the badge; a per-caller version is four places
to disagree. Each one is keyed on the mode as well as the language, or a tap to AUTO leaves the badge
still reading HR.

## Tested

**Test 1 — 20 cases, 0 failures** on the clamp: English in five spellings, nine neighbour languages
that must all land on Croatian, and the empty, null, whitespace and unknown cases.

**Test 2 — real API, real keys, real speech.** Croatian audio generated with Hume and sent to Groq
came back `'Croatian'`; a tone came back `'English'`. HTTP 200 both times, with the User-Agent header.

**The 30-second cap is done — see §79.**

**Not tested: the probe on the phone.** The multipart upload, the key ring walking a real ring, and
whether 12 s is the right timeout on mobile data.

---

# 79. The probe sends 30 seconds, not the whole recording

`AudioConcat.trimSeconds` writes the first N seconds of a PCM WAV to a new file.

**A byte copy, not a re-encode.** PCM has no frames to align to and no codec state to carry, so the
first 30 seconds are simply the first 30 seconds' worth of bytes with a corrected header. No ffmpeg,
no decoder, nothing lost. It reuses `parseWav` and `wavHeader`, already in that file and already
proven by the segment merge.

**Why it matters:** the probe must finish *before* the real request starts, so its upload is time he
waits with nothing happening. Whisper settles the language from the opening sentence; a five minute
dictation was spending the whole upload answering a question decided at the start.

**A failed trim is not fatal** — the untrimmed file is sent instead, which is slower and still
correct. The temp file is deleted in a `finally`, so a copy of his voice never outlives the question
it was made to answer, whether the probe answered, failed or threw.

## Tested

**Test 1 — 7 arithmetic cases, 0 failures**: longer than the limit, exactly the limit, shorter (the
whole file, not a refusal), one byte, odd length, zero data, zero seconds. Plus frame alignment
checked across 1ch/2ch, 16/32-bit, 16/44.1/48 kHz — all aligned.

**Test 2 — real file, real API, and an outside party agreeing.** A 62.3 s Croatian WAV trimmed to
960,044 bytes; `ffprobe` reports **30.000000 s**, and Groq's own response reports
`duration: 30.000001024`. **Both the full and the trimmed file returned `'Croatian'`** — half the
upload, same answer. That external duration figure is the number worth having, because it is measured
by something I do not control.

## Trap repeated, and it is now twice

`AudioConcat` was already imported and I added it again — the identical mistake that made build 166
red with `MaKeys`, two builds earlier. **Grep for the import LINE, never for the symbol.** The check
now runs after every import edit and reports duplicate lines directly.

---

# 79. Croatian text, English suggestions — a chain I built myself

His screenshot: badge on HR, suggestions offering `credits`, `credited`, `creditors`. Two of my own
changes made it, one build apart.

**Build 111** cut `MaLanguage.set` from the keyboard subtype, at his request: typing and speaking are
two acts, and a control for the microphone should not relay the keys. **Build 150** then took the
language picker off the spacebar long press and gave that gesture to the cursor pad.

Suggestions come from the **subtype**. So after 150 there was no route to the suggestion language at
all — not the badge, not the spacebar, not settings. He typed Croatian and the keyboard answered in
English, correctly, from the only language it had been left with.

**The subtype follows the badge again.** What 111 was really protecting against was the keys being
relaid unasked; that is a layout question, and the two subtypes here are qwertz and qwerty, which is
the layout he wants for that language anyway. **If a control writes a language, it must write the one
the suggestions read, or the two will drift and only a screenshot will find it.**

## The bold was on the wrong word

It followed `isEligibleForAutoCommit`, a property of the FIRST candidate — so when §68 moved the best
guess to the centre, the bold stayed left and pointed at the wrong word. Emphasis is now one decision
made by position, so the large word and the bold word cannot separate again.

## The dizziness

Long-pressing the spacebar clears the candidates, the strip fell back to something shorter or to
nothing, and the text above jumped down and back. **Hidden now means empty but still there**: the
strip keeps its height and draws nothing, so nothing above it moves.

## Hiding the row

Long press the language badge. He asked for an X at the end of the row and then talked himself out of
it — an X costs a slot in a strip that is already narrow and only ever does one thing, while the
badge is already there and already about that row. Short press still cycles ENG, HR, AUTO.

Its own preference, not the existing suggestion setting: that one governs whether suggestions are
COMPUTED. This is a curtain, and drawing it should not throw away the word model's work.

## Asked for and NOT built: the split recording

He wants the recorder writing **two files at once** — the first capped at 30 s for the Groq probe,
the rest continuing in the second — so AUTO gets exactly the audio it needs without waiting for a
long dictation to finish.

That is the right design and it is a real change to `MaAudioRecorder`: a second `MediaRecorder` or a
tee on the encoder, a cap that closes one file cleanly while the other keeps writing, and both paths
tidied on cancel, failure and process death. **It deserves its own build with the four tests, not the
end of this one.** Today the probe sends the whole file, which is correct but slower on a long
dictation.

---

# 79. History says what is finished, and can send again

**A stripe down the left of every row.** Sand for transcribed, blue for not sent yet. The question he
asks while scrolling is "is anything here unfinished", and a colour answers that without being read.

**Deliberately not red.** A recording that has not been sent is **work outstanding, not a fault** —
the same reasoning that took red off the buckets (§44). Red on a list he opens to find something
makes every visit feel like an incident. The red text on failed rows is gone with it; the stripe
carries the state now.

**Resend, on the rows that need it.** A dictation that never transcribed is the one loss in this app
that trying again later cannot undo: the words were spoken once. The audio is kept for exactly this,
and until now the only way back to it was the keyboard's own history panel — which meant finding it
with the keyboard up, rather than while reading the list.

**Shown only on a failed entry.** On a successful one it would redo work already done and overwrite
text he may have edited since.

It calls `DictateController.retranscribeHistoryEntry`, the same function the keyboard panel uses, so
a recording resent from the list behaves identically and writes its result back into the same row.

## Still queued

- **Bucket copy by occurrence** — `copy 1`, `copy 2` counted from the bottom, and a finger that
  scrolls and re-searches when a target is not found (§37, §59).
- **The CC settings screen** — key list, prompt groups per shortcut, several named prompts with radio
  buttons, each one collapsed to a single line that opens an editor (§ his request, 18.8).
- **Offline retry with a manual resend in the status line** — the recording path gives up silently
  today; he wants a button when it does.
- **English base dictionary**, the counterpart of §33's Croatian one.

---

# 80. Two bugs in AUTO, both mine, both about WHEN not WHAT

He spoke English and got a Croatian transcription, and the language switch in history was frozen.
**The detector was never the problem.**

## The detection was right and was thrown away

`MaLanguage.set` launches its write on the **Main** dispatcher, and `transcribe` also runs on Main —
so `set` followed by a read of `active()` in the same function returned the **old** value, every
time. The probe said English, the write was queued, the request was built from the stale value, and
Croatian went out.

`setNow` writes with `runBlocking` and returns only when it has landed. **Anything that decides a
language and acts on it in the same breath must use it.** `cycleMode` writes the same way now, for
the same reason: he taps the badge and speaks immediately, so the tap must land before the send
reads it.

## The frozen switch

Re-transcribing goes through the same `transcribe`, so **the probe ran again and overwrote the
language he had just chosen** — every attempt. The switch was not broken; the app was arguing with
him and winning.

`if (!isReplay && ...)`. **Re-transcribing exists BECAUSE the language was wrong**, which makes it
the one moment when his choice is better information than any detector.

## Tested by the four

**Test 1 — 10 cases, 0 failures.** Both his reported cases, both replay directions, probe failure,
and manual modes ignoring the probe.

**Test the test — restoring the async write turns exactly 2 cases red**, and they are precisely the
two he reported. The test watches the real fault rather than agreeing with the code.

**Test 3 — 5 ugly cases, 0 failures:** empty probe answer, unknown language, junk stored language,
junk mode, junk mode on replay. Everything lands on a usable language.

**Test 4** — `dictate__ma_language_mode` defaults to `hr`, so an upgrade never switches AUTO on by
itself, and no stored value changes meaning.

**Not tested: the phone.** Whether `runBlocking` on Main is fast enough not to be felt on a
preference write — it is a local datastore write, but it is a block on the UI thread and worth
watching.

---

# 81. The screen reader, with Speechify voices

A speaker key in the feature row. **Short press reads what is on screen; press again to pause, again
to continue. Long press opens the reader settings.** The face shows what the NEXT press will do —
speaker, pause bars, play triangle — because a key that describes its own state leaves him working
out what pressing it would achieve.

## Ukrainian stands where Croatian would

**Speechify has no Croatian voice.** Verified across the whole catalogue: 985 voices, every page
walked, no `hr-HR` on any model. The only Slavic locales that exist are Russian (50 voices), Polish
(2) and Ukrainian (2).

He compared five renderings of the same Croatian sentence and chose **Lesya (Ukrainian)** first,
**Beatrice (British, multilingual)** second. Slavic phonetics turn `č ć ž š đ` and the `-lj- -nj-`
clusters into sounds rather than spellings.

**The model follows the voice, never a global default.** `lesya` exists only on `simba-multilingual`
and `simba-3.0`; `simba-english` would fail for her, and `simba-3.2` answers HTTP 400 for anything
outside the eight curated `_32` ids.

**The language pill picks the voice.** The same badge that sets the dictation language chooses who
reads, so there is nothing extra to keep in step.

## The hard part was refusing, not reading

A screen is mostly chrome. Two rules do nearly all the work:

- **Anything clickable is never read.** Nobody writes a paragraph inside a button, so this separates
  "Copy message" and "More options" from prose without a list to maintain — and those two were the
  exact cases the length rule could not catch.
- **Under 12 characters is a label.** "Send", "New chat", "Opus 5", every timestamp.

Then: nothing without a letter in it, a short chrome list for the long boilerplate, and duplicates
dropped — a string usually appears twice, on the container and on the text node inside it, and
hearing every sentence twice is the fastest way to ruin a reader.

**Reading order comes from the screen, not the tree.** Sorted by top edge then left, because the tree
is a layout hierarchy and gets it wrong exactly when there are columns.

## Tested

**Test 1 — 22 cases, 0 failures**, using real strings from his own dumps. Both refusal cases that
failed the first attempt drove the clickability rule. Reading order and de-duplication verified.

**Not tested: a real synthesis from the app.** The key ring, the mp3 playback, and whether the
reading is pleasant. All need the phone and his Speechify keys imported.

**He must import the keys first** — Speechify is registered now (it was recognised by `MaKeys` all
along but missing from `ProviderRegistry`, exactly the gap Groq had in §31), so the keys screen will
accept them.

---

# 82. The Speechify key test asked the wrong question

The keys screen showed **"no connection"** with *The requested resource could not be found* against
keys that work perfectly. The API was answering; the test was asking for something that does not
exist.

`OpenAiCompatibleClient.validateKey()` requests **`/models`**. Every OpenAI-shaped provider has it and
**Speechify does not**, so it answered 404, which fell to the catch-all branch and was reported as a
network failure — **sending him to look at his wifi over a healthy key**.

`MaSpeechify.validateKey` asks `GET /v1/voices?limit=1` instead: the cheapest thing Speechify will
answer, one voice, no synthesis, nothing billed. Measured against the real API — a good key returns
200, a nonsense key returns 401 with `{"error":{"code":"unauthorized"}}`.

**A test must ask the question the service can answer, not the question the other services happen to
share.** That is the general lesson; the specific one is that a 404 is not a connection failure and
must never be reported as one.

Statuses follow the engine handoff: 200 works, 401/403 condemns the key, **429 is throttling and does
not condemn**, anything else says what it was rather than guessing. 7 mapping cases, 0 failures.

`MaKeyRingStore` is told on both success and rejection, so the verdict shown on this screen is the
same one the reader will act on — there is only one place holding it.

---

# 82. Karaoke: the subtitle row, the spacebar word, and reading speed

**Speechify returns word timings free with every synthesis.** Measured: each word carries
`start_time` and `end_time` in milliseconds, and the windows have no gaps or overlaps. So following
along costs one comparison every 60 ms and no second request.

## The subtitle row is the default, and the spacebar version is the option

The spacebar borrows a key that **only exists while the letter keyboard is shown** — so it vanishes
exactly when he reads a screen with the keys folded away, which is most of the time. The subtitle row
belongs to the reader instead, and is there whenever the reader is.

**One sentence, not the passage.** A screen of text on one line is unreadable and chasing a highlight
through it is worse than nothing. Sand and bold for the word being said, dim for the rest — two
states only, because a fade of "recently said" makes the eye chase the gradient.

**Nothing at all when nothing is being read**, not an empty bar, so it can sit in the layout
unconditionally without the keyboard changing height mid-reading.

**Sentences are derived, not given.** Measured: three sentences come back as ONE mark object with a
flat word list, whatever the `type` field claims. A sentence is therefore the run between one word
ending in `.` `!` `?` and the next. Verified against real timings — every boundary lands correctly.

## The S key

`MaFeatureKey.SUBTITLE`, face `S`, toggles the row. A key rather than only a setting because it is
changed **while reading**: wanted for a passage being followed closely, in the way for one half
heard. Lit when on, because between sentences the row is blank and an unlit press would look like it
had done nothing.

## Speed

`0.5×` to `2.5×` in tenths, stepped in reader settings. **Applied to playback, not to the
synthesis** — so it costs nothing, takes effect on the next press rather than the next request, and
no audio is re-bought to hear it faster.

**The karaoke divides the playhead by the speed before looking up the word.** The timings describe
the audio at normal rate while `currentPosition` advances in real time, so without that the
highlight would drift further behind the longer he listened — worse than not having it.

## Tested

**Test 1 on the word lookup — 13 cases, 0 failures**, using the real timings from a live call: every
boundary, both sides, past the end, and negative. Confirmed no gaps and no overlaps.

**Test 1 on sentences — every boundary correct** across three sentences of real marks.

**Not tested: the phone.** Whether 60 ms feels smooth, whether the highlight sits right against the
voice, and whether `setSpeed` is honoured on his Android 16.

---

# 83. Volume button passthrough: quick tap acts, hold is the volume

His design, and it dissolves a problem that had no good answer. The keys are the app's while the
keyboard is up, which left no way to change the volume without folding the keyboard away.

**The decision moves to RELEASE.** A quick tap is a tap; a finger still down after 500 ms was never
a tap at all. Nothing happens on the way down any more — at the moment of pressing there is no way to
know which of the two he meant.

- **Volume up, tap** — start or stop recording.
- **Volume down, tap** — press the magic finger term, still resolved through the row's own list.
- **Either, held past 500 ms** — real volume, repeating every **111 ms** (his number) with the
  system's own volume UI showing, until the finger lifts.

**The cost, stated:** recording used to start on the way down and now starts on the way up. For a tap
that is the length of the tap — not felt. It is the price of one key doing two jobs, and it is the
right trade.

**Its own coroutine scope, on Main.** The service's lifecycle scope would carry a repeat across a
keyboard that has been hidden, and a volume key repeating for a keyboard nobody can see is how an app
gets uninstalled.

**Held-key repeats from the system are swallowed**, because the repeating is done here on our own
clock — otherwise both would run and the volume would race.

## Tested

**Test 1 — 7 cases, 0 failures**: instant tap, normal tap, the last millisecond that is still a tap,
the first that is not, and three hold lengths with their exact step counts.

**Both invariants proved across 429 hold lengths:** *never both* — no press ever produces an app
action and a volume change — and *always one* — no press ever does nothing at all. Those two are the
whole feature, and a case-by-case table would not have shown them.

**Not tested: the phone.** Whether 500 ms reads as the right threshold under a real thumb, and
whether `FLAG_SHOW_UI` puts the system volume bar somewhere that covers the keyboard. Both are one
constant away if they feel wrong.

---

# 84. The subtitle box, and reading past the fold

## Why it vibrated

The row scrolled horizontally to keep the current word in view, so **every word shifted the whole
line** — and on a long sentence the highlight ran off the edge anyway.

It is one wrapped block now, three lines, in a rounded box **the height of the composer he reads
beside** (96dp), so the two read as one interface. Nothing moves except the colour.

**Built as one `AnnotatedString`, not a `Row` of `Text`s** — a Row cannot wrap, so it would push the
sentence sideways again: the same bug in different clothes.

**Fixed height rather than wrapping to fit**, or the keyboard would change height between a short
sentence and a long one, with his thumb already moving towards a key.

## Reading past the fold

When a screenful finishes, it scrolls a page and reads the next, until the bottom.

**Two independent stop conditions, because either alone fails:**

- **Nothing scrolled** — catches a screen that cannot move at all.
- **The text is identical to last time** — catches the far commoner case of a list that reports a
  *successful* scroll and does not actually move. **That is what would otherwise scroll forever at
  the bottom.**

Comparing text rather than counting attempts is deliberate: a fixed number of tries would stop early
on a long page and spin on a short one.

`lastPassage` is cleared in `stop()` and **not** in `stopPlayer()`, which runs between screenfuls —
clearing it there would erase the very comparison that detects the end, and pressing play again on
the same screen would stop instantly, reading as a reader that had died.

**450 ms settle** after a scroll, because a list needs a moment to build the rows it just moved into
place; reading instantly finds the old ones or none.

## Tested

**Test 1 — 5 scenarios, no runaway in any:** three normal pages, a scroll refused immediately, a
**list that lies** (reports success, text repeats), a blank page after two, and a single-page screen.
The liar case stops after one passage rather than spinning, which was the whole point.

**Not tested: the phone.** Whether 450 ms is enough for Claude's message list, and whether the box
lines up with the composer as intended.

---

# 85. Jump between pages, do not glide

He cannot watch animated scrolling. **The animation is the app's, not ours** — `ACTION_SCROLL_FORWARD`
is a *request*, and a list answers it with its own smooth scroll. There is no flag from here that
turns that off.

But there is another action. **`ACTION_SCROLL_TO_POSITION` is answered by a RecyclerView with
`scrollToPosition`, which moves in one frame with nothing to watch.** So `scrollBy` now tries the
jump first and only falls back to the animated scroll when the app will not take it:

- Read the list's `collectionInfo` for the total row count.
- Work out the visible span from the `collectionItemInfo` of the attached children.
- Ask for the row one screenful further on.
- If any of that is missing — no collection, no item rows, target out of range — return false and
  let the old action run.

**Where an app publishes no collection, the animation stays and is not ours to remove.** Said
plainly rather than promised away.

## The waits, which WERE ours

`SETTLE_MS` cut from **450 ms to 120 ms**. The long wait was sized for a smooth scroll to finish
travelling; a jump has nothing to travel, and the only thing left is the rows being attached — one
or two frames.

**Not zero.** At zero the read happens in the same frame as the jump and finds the OLD rows, which
would not look like a fast reader, it would look like one that skips a screen.

`SCROLL_SETTLE_MS` (220 ms) is untouched and never fires for the reader: it only runs *between*
pages of a multi-page scroll, and the reader asks for one page at a time. Verified rather than
assumed — **total wait per page is 120 ms and nothing else.**

---

# 86. Two real highlight bugs, found by looking rather than tuning

He said the highlight was "a little bit off" and asked for the algorithm to be checked rather than
adjusted. He was right to: **neither fault was timing, and no amount of tuning would have reached
either.**

## Bug one: the highlight matched by TEXT

`words.indexOfFirst { it.text == word }` — the subtitle looked up the current word **by its letters**.
A passage says "the" many times, so it always landed on the **first** one.

Measured on a 36-word passage: `"the"` appears at positions 0, 6, 11, 15, 18, 23, 26, 30 and 34, and
the old code highlighted position **0 every single time**. That is the jumping he saw, and it had
nothing to do with milliseconds.

`MaReader` now publishes `currentIndex`, and the subtitle compares by **position**. The text is not
an identity; the index is.

## Bug two: the speed was counted twice

`MediaPlayer.currentPosition` reports a position in the **media timeline**, which already advances at
the playback rate. Multiplying it by the speed again made the highlight run ahead by exactly that
factor.

Measured against real timings: at 1.5×, the old arithmetic was wrong in **3 of 5 samples**; the new
one in **0**. At 1.0× the two agree — which is why it read as vague drift rather than a plain error,
and why it would have survived any amount of adjusting the tick rate.

The `speed` parameter is **gone from `startTicker`** so it cannot creep back.

## Pages that fit, instead of sentences

Sentences were the wrong unit: a long one overflows a three-line box and the text jumped as the
highlight moved through the part that could not be shown; a short one left the box nearly empty.
Neither has anything to do with how much room there is.

A page is now a run of words that **fits** — by characters, since what fills three lines is a number
of letters rather than of words. Whole words only, computed once per passage.

**The page changes only when the spoken word leaves it.** Verified: over the whole passage the page
changed exactly as many times as there are pages, so it never flickers back.

Every page fits, and every word belongs to exactly one page — both checked across the passage.

---

# 87. Detect the language from the TRANSCRIPT, not from the sound

Auto-detection was unreliable and he was right that it should not be. **The mistake was asking the
wrong question.**

Whisper's `language` field is an acoustic guess made from the opening seconds, before it has heard
much, and Croatian is a small language sitting beside four larger ones it is routinely confused
with. Meanwhile the same response carries **the transcript** — and text is decisive in a way sound is
not. No English sentence contains `č`. No Croatian sentence is built out of "the" and "of".

**Asking for a language and discarding what was actually heard was the whole error.**

## Three independent signals

- **Diacritics, counted triple.** One `ž` is nearly proof by itself.
- **Function words.** The commonest words in either language, so present in almost any sentence.
- **Endings** — and this is the one that matters most. Croatian inflects and English does not, so
  `-ati -iti -ost -ući -ima -oga` need **no vocabulary at all**. A sentence about camera rigs or tax
  forms, full of words no list will ever hold, still scores correctly.

The acoustic label is kept only as a fallback for a transcript too short to score — "Okay", "Hvala"
— where nothing could do better.

## Tested

**16 cases, 0 failures**: both languages, Croatian without diacritics, badly transcribed Croatian,
one-word utterances, mixed sentences, and empty input.

**The endings rule was added because a real case failed without it** — *"Trebam napraviti novi
projekt i poslati ga danas"* has no diacritics and no listed function words, and scored nothing.

**Checked against the real Groq transcript** of real Croatian speech, mistakes and all — `"Dobar dan,
ja som Baba i ovo je snimka Rahavatskom jeziku"` — and it lands on Croatian despite Whisper having
mangled three words.

## The badge says AUTO

It read `A·HR`, meaning auto mode had resolved to Croatian. **One fact too many on a key the size of
a thumbnail**, and he read it as a state he did not recognise. On AUTO the language is no longer his
decision, so showing it invites him to check something he cannot change.

## Right language, right route

`SYNC_SAFE_LANGUAGES` still gates the fast path and still contains only English, and it reads
`MaLanguage.active()` — which the probe now writes **synchronously** before the request is built
(§80). So detection genuinely drives the routing: English takes sync, Croatian takes the slow
accurate model, and neither is a coincidence of timing.

---

# 88. Suggestions were eating the trackpad's selection

He reported selecting a few words on the trackpad and watching the selection vanish, over and over.
It read as chaos and it was one line of cause.

**Computing suggestions marks a composing region around the word at the cursor, and marking one
collapses whatever is selected.** So every arrow the pad sent triggered a refresh, and the refresh
threw the selection away.

Three parts to the fix, and all three are needed:

- **`resetSuggestions` returns early while the pad is open**, and *clears* rather than merely
  skipping, so no stale row is left hanging over a pad it no longer describes.
- **The Smartbar treats the candidate list as empty while the pad is open.** Suppressing new
  suggestions does not remove the last set from the flow — without this the row would still show
  words describing a cursor that has since moved.
- **Closing the pad asks for suggestions again**, through `MaCursorPad.onClosed`. Otherwise the row
  stays empty until the next keystroke, which looks like the pad broke suggestions on its way out.
  Registered once in the service, not at the two places the pad is drawn, so the two cannot restore
  different things. A callback rather than the pad reaching for the keyboard manager — it is drawn
  by two layouts and knows about neither.

## Tested

**4 cases, 0 failures**, and the invariant that matters: **a selection made on the trackpad is never
destroyed**, across 200 runs.

**The first version of the test failed, and the test was wrong, not the code.** It asserted that a
selection survives with the pad closed during ordinary typing — but there is no selection then, and
a composing region collapsing nothing is correct. Fixing the model rather than the code is the
whole point of testing the test.

## Still open: making them work together

He asked for suppression as a first step and coexistence as the better answer. Coexistence needs
suggestions computed **without** marking a composing region — the region exists so that a picked
suggestion knows what to replace, and §68 already replaces the whole word around the cursor without
needing one. **That is the thread to pull** if the row is ever wanted during selection.

---

# 89. Voice formatting stops flattening newlines

The defect found by §76's own test and written down rather than fixed. **Splitting the whole text on
whitespace and rejoining with spaces destroyed every line break**, so a spoken command used on
multi-line text silently collapsed it — worse than the command not working at all.

`applyOnce` now works on the **last line only** and puts the earlier lines back untouched.

**Not a compromise, the correct scope.** A command counts only as the final word, so the last line is
the only line a command can be on. The whole-text version was never right; it merely looked right
until the text had two lines.

The inner word list is renamed `body`, because `head` now means the untouched earlier lines and two
different `head`s in one function is a bug waiting to be written.

**8 cases, 0 failures**, including three-line and four-line input with every command family, and the
check that matters: **the newline count is identical before and after, in every case.**

---

# 90. Two more keys become modules

`MaSubtitleToggleKey` and `MaReaderKey` join the pin, the case cycle and the next-field key in
`MaKeyModules.kt`. **Five of about twenty.** The row is 1237 lines down to 1199, and four imports it
no longer needs went with them.

Both obey the contract from §69. The subtitle toggle takes only a colour and reads its own
preference — the simplest kind of module, droppable anywhere without the surface knowing what a
subtitle is. The reader key takes a context, a way to open its settings and a way to say something;
`MaReader` holds the state, so **the same key drawn on two surfaces shows the same thing without
either surface tracking it.**

## The mistake worth recording

The first attempt sliced from `SUBTITLE` to `PIN` and replaced **nothing**, because `SUBTITLE` sits
*after* `PIN` in that file — the slice was empty and the write was a no-op.

**The balance check passed and the line count was identical, and both were true.** A structural check
cannot tell "nothing changed" from "changed correctly"; it took reading the file for the branches to
see it. Same lesson as §87's type mismatch, from the other direction: **after a scripted edit, verify
the thing the edit was supposed to produce, not the shape of the file.**

---

# 91. The copy row becomes an ordinary feature row

His copy row lived in the **Smartbar quick actions** — a separate arrangement with its own editor and
its own key set. That is why it could hold paste **or** AP and never both: `ALL_PASTE` and
`ALL_CLEAR` are not Smartbar actions and never could be dragged there.

**So the four clipboard keys moved into the feature row vocabulary:** `PASTE`, `CUT`, `COPY`,
`CLIP_HISTORY`. Each goes through `tapKey`, the very path the Smartbar action used, so a paste from
this row is the same paste — **the key moved, its behaviour did not.**

That makes his request possible in one place: the copy row is now just a feature row, arranged in the
editor that already exists, where any key can replace any key and reset restores the default. No new
editor was needed; the row simply had to stop being special.

**Shipped as default row two:** select all, paste, cut, clipboard history, dictation history, AP, AC.

**COPY is deliberately absent.** He listed seven keys and copy was not among them. One tick away in
the catalogue if that was an oversight rather than a decision — and it is worth asking, because a
copy row without copy is a surprising thing to ship silently.

## Two traps caught before the build

**`ROW_COUNT` is 3.** Adding the copy row as a fourth would have been **silently dropped** — `parse`
pads or truncates to exactly three, so the list would have looked right in the source and never
reached the keyboard. The copy row takes slot two and one empty row remains.

**Three failed anchors before one landed.** `SELECT_ALL("selectall"` was wrong — the real id is
`select_all` — and the script asserted, wrote nothing, and reported success for the parts that ran
before it. **A scripted edit that aborts halfway leaves the file in neither state**, and only reading
the result afterwards showed it.

## Note for the next session

He must press **Reset all three rows** in Feature row settings, or install fresh. A stored
arrangement always wins over a changed default — that is the whole point of storing it, and it means
a default change never reaches an existing install by itself.

---

# 92. Groq removed, restore added, and the copy row gets its own screen

## Groq and auto-detection are gone

**Detection was never reliable enough to be trusted with a whole dictation**, and a wrong language
does not fail visibly — it returns fluent nonsense in the other language, which is worse than an
error. Manual is one tap and right every time.

Removed: `MaLanguageProbe` (file deleted), `probeLanguage`, `probeOnce`, the three probe constants,
the probe call in the send path, and `MODE_AUTO` from the badge cycle. The badge is ENG/HR again.

**Two things kept on purpose.** The `GROQ` preset stays in `ProviderRegistry`, just out of the
offered list, so an existing stored Groq account still parses instead of becoming an unknown
provider. And `MODE_AUTO` survives as a constant so a stored `"auto"` from an older install is
recognised rather than treated as junk.

## Restore, beside backup

It shipped with only the writing half — **the half that feels finished and never gets used**. Restore
asks first, because it replaces every current setting: right on a fresh install, a real loss on a
configured one, and a mis-tap away from backup.

## The copy row is its own thing

Same `MaRows.Row`, same catalogue, same entries — **a key added anywhere in the app appears in this
editor with nothing to remember**. What differs is where it lives: `dictate__ma_copy_row`, and it is
drawn **only in the transcription view**, which has no letter keys and where the clipboard is most of
the job.

Its own preference rather than a fourth slot in `maRows`, because `parse` pads and truncates to
exactly `ROW_COUNT` — a fourth row there is silently dropped and the source looks right while the
keyboard never shows it.

`MaFeatureRow` gained `copyRowOnly`, so the transcription view draws that row and not the three that
belong to the typing keyboard. **Same composable either way**, so a key behaves identically in both
places; the caller chooses which rows, not which implementation.

---

# 92. Groq removed, restore added with history, copy row made its own thing

## Auto language detection is gone

The probe, its constants, `MaLanguageProbe`, the AUTO badge state and Groq from the offered
providers. **Detection was never reliable enough to be trusted with a whole dictation, and a wrong
language does not fail visibly** — it returns fluent nonsense in the other language. Manual is one
tap slower and right every time.

The Groq *preset* stays in the registry so an existing stored account still parses instead of
becoming an unknown provider. `MODE_AUTO` stays as a constant for the same reason: an older install
with `"auto"` saved is read as Croatian rather than as a value nobody recognises.

## Restore, with dates

Backup shipped without its other half. It now writes **`settings-YYYY-MM-DD-HH-mm.jetpref`** beside
the plain file and keeps **twenty**.

**The stamp is big-endian in the filename and day-first on screen.** Sorting is by name, and only
year-month-day sorts correctly; what he reads is `19.08. 18:30`. Two orders on purpose, and one
function knows both.

**History is read from the folder**, never from a list this class maintains — a list drifts the first
time a file is moved by hand, and the folder is the truth.

**The list is the confirmation.** A yes/no dialog followed by "restore the latest" gives him no way
to reach the backup from *before* the change he is undoing, which is the only reason anybody opens
this. Choosing a date is both the safeguard and the feature. The newest is labelled, because it is
what he wants nine times in ten and should not need a date read.

The plain `settings.jetpref` is untouched, so setup's restore still finds "the backup" without
knowing history exists.

## The copy row is its own row

Not one of the three. Stored in `maCopyRow`, edited on its own screen, and **drawn only in the
transcription view** — the screen with no letters, where the clipboard is most of the work.

**Its own preference rather than a fourth slot**, because `MaRows.parse` pads and truncates to
exactly `ROW_COUNT` — a fourth row there is silently dropped and the source looks right while the
keyboard never shows it.

Same `Row`, same `Entry`, same catalogue, same keys. **A key added to the app appears in this editor
with nobody remembering to add it.** Only the storage and the surface differ.

---

# 93. "null" instead of every dictation — the worst bug I have shipped

Build 184 replaced **every dictation that did not end in a formatting command** with the word `null`.
That is essentially all of them.

`return head + when (command) { ... else -> null }`. **In Kotlin `String + null` does not produce
null — it produces the four letters "null".** So `applyOnce` returned a non-null string, the caller
read that as a successful transform, and the dictation was thrown away and replaced.

The transform is computed into a value now, and the head is prepended **only when it is not null**.

## Why nothing caught it

The §89 test exercised the whole `apply` loop and every case in it **ended in a command**. Not one
case asked what happens to an ordinary sentence, because the change was about newlines and ordinary
sentences were not what I was thinking about.

**The rule this earns: a transform that can decline must be tested on input it declines.** The
"nothing happens" path is the commonest path in production and it was the only one with no test.

Four cases now, including two that decline, and the invariant stated: **a dictation with no command
is returned unchanged, never replaced.**

---

# 94. n, k, c — and the copy row editor is the feature row editor

## The zone keys say what they toggle

`1 2 3` became **`n` `k` `c`**: number row, keyboard, copy row. A digit only said which position a
key held in a list nobody can see. **He reads a picture faster than a word and a word faster than a
code, and 1, 2, 3 was a code.**

Lowercase, because the letter sits inside a keyboard outline only two thirds of the glyph tall and a
capital reaches its walls. The settings labels changed with the glyphs, so the list and the key agree.

**Six call sites**, and three of them were in the settings preview — a signature change that compiled
in one file and broke another is exactly the shape of the last three red builds.

## Standing rule: a button in settings carries its icon

**Written into HANDOFF.** Whenever a setting lists keys or buttons, each row shows its own glyph
beside the name. He recognises the image before he reads the word, and a list of names alone makes
him translate twice.

## The copy row editor reuses the feature row's

`MaRowKeyItem`, `MaKeyPicker` and `MaReorderableColumn` are shared rather than reimplemented, so the
copy row has **drag handles, icons, position numbers and ticks** — identical to the feature row,
because it is literally the same code. `MaRowKeyItem` and `MaKeyPicker` went from private to
internal; that visibility was the only thing standing in the way.

**Two independent surface ticks** replace the single "show" switch: *on the typing keyboard* and *in
the transcription view*. It is one row drawn in two places, not two rows to keep in step, so both,
one or neither are all sensible answers.

---

# 95. The copy row gets its own colour, and the icon rule becomes standing

**A dark amber behind the copy row's keys**, so a glance says which list is being edited. Not the
sand used on the keyboard: **that colour already means "this key is holding something"**, and reusing
it would give one colour two jobs — the first time a lit key and a copy row shared a screen they
would look identical and mean different things.

**Lifted still wins over the accent.** The key under the finger must read as lifted whatever list it
is in; drag feedback matters more than the identity of the row.

The accent is a parameter on the shared `MaRowKeyItem` with a null default, so the feature row is
untouched and there is still exactly one implementation.

## The icon rule is now in HANDOFF

> **A button in a settings list always carries its icon.**

His words: *he recognises an image faster than text*. A list of names alone makes him translate twice
— word to picture, picture to key. It also catches a class of mistake for free: **a row whose glyph
is wrong is visible the moment the screen opens.**

---

# 95. The zone keys: a big letter, and the border says it is on

The keyboard outline inside the glyph is gone, and the little spacebar line with it. **At the size a
key actually is, that outline left room for a letter too small to read at a glance** — which defeats
the whole reason for using `n k c` instead of `1 2 3`.

**The border moved to the key.** A lit outline around the whole key reads from further away than a
lit glyph inside one, and it leaves the entire face for the letter, which is now 30dp against the
old 24dp with most of that spent on the outline.

`MaZoneKeyShape` matches the shape the keys are already clipped to, so the border sits exactly on the
key's own edge rather than near it.

**Written into HANDOFF as a standing rule**, because it generalises: *emphasise a key with a border
around the whole key, not with a mark inside it.*

## Also into HANDOFF: a button in settings always carries its icon

Wherever a settings screen lists keys, buttons or rows, each entry shows the glyph it has on the
keyboard, beside the name. **He recognises an image faster than a word**, and a list of names alone
makes him translate twice — name to picture, picture to key.

## The copy row is amber

`MaRowKeyItem` takes an optional `accent`, and the copy row editor passes a dark amber. Lifted still
wins, so the key under the finger reads as lifted in either list — drag feedback matters more than
which row it belongs to.

**Deliberately not the sand** used on the keyboard. That colour means "this key is holding
something" throughout the app; reusing it for "this is the copy row" would give one colour two jobs,
and the first screen showing both would say the same thing twice and mean different things.

---

# 95. The letter is the glyph, and the ring is the state

The zone keys drew a small keyboard outline with a spacebar line and a digit tucked inside. **Three
keys that toggle three different things all drew the same picture**, and the only mark that
distinguished them — the letter — got the space left over.

Inverted. **The letter is now the whole glyph**, at 0.86 of the key face, and whether the key is ON
is said by a **green ring around the key itself**. One thing per surface, and the thing that varies
gets the room.

The keyboard outline, the spacebar line and seven drawing imports are gone with it.

**Written into HANDOFF as a rule**, because it generalises: an important key says it is on with an
outline around the key, not by tinting something inside it. A ring reads from across the room.

## Also into HANDOFF: icons in settings lists

Whenever a screen lists keys or buttons, every row carries the same glyph the keyboard shows. He
recognises the image before he reads the word. `MaRowKeyItem` is the shape to copy — and reusing it
beats matching it, which is why the copy row editor shares it outright.

## The copy row gets its own colour

A dark amber behind its rows, so a glance says which list is being edited. **Deliberately not the
sand** used for a lit key: that colour already means "this key is holding something" everywhere in
the app, and giving one colour two jobs fails the first time a lit key and a copy row share a screen.

**Lifted still wins over the accent** while dragging, because the feedback under the finger matters
more than the identity of the list.

---

# 96. Tap the subtitle to skip, and hear a voice before choosing it

## The highlight stopped moving the text

**Colour only, never weight.** Bold changes the *width* of a word, so every word the highlight
touched nudged the whole line sideways — the text appeared to breathe as it was read. A colour
change moves nothing.

## Tap the subtitle to skip the sentence

The commonest thing he wants while listening is *not this bit*. The speaker key pauses; the subtitle
now skips.

**A seek, not a re-synthesis.** The passage is one file with word timings, so skipping is free and
instant. Asking Speechify for audio from a later point would be a second charge for words already
bought.

The end of a sentence is found in `MaReader` from the same flat word list the subtitle uses, so the
two cannot disagree about where a sentence stops. At the last sentence it does nothing, which is what
"skip the rest" means when the rest is all of it.

## Reader settings

**Speed moved to the top**, above the display choice — it is the thing he changes most and it was
below two lists.

**Tapping a voice speaks it: "Hi, I am Lesya."** The voice says its own name, which gives accent,
pace and warmth in four words and ties the sound to the row he is looking at. On IO, because a
synthesis on Main would freeze the settings screen.

**A tick to switch previews off.** On by default, since hearing a voice is the whole basis for
choosing one — but a sample is a synthesis and costs characters, and once chosen he should not pay to
reorder a list.

## Gemini: instrumented rather than guessed

The reader works in Claude and not in Gemini, and his dump does not show why — the message text is
present, not clickable, and long enough to pass every filter.

**"The reader does nothing here" has three causes that look identical:** no window reachable, a window
with no text worth reading, or text found and the voice failing after. `readableText` now logs
**windows, lines and characters**, so one paste of the log separates them instead of another round of
guessing.

---

# 96. Profiles — the first piece of the Avid model

He asked for Avid's settings architecture: collapsible folders, duplication, checkmarks, a
master-detail pane. **That is a redesign of every settings screen, not one build.** This is the piece
worth having first and on its own.

## What shipped

**Profiles**: every setting in the app, saved under a name, swapped in one tap. Settings → Profiles.

Three of Avid's behaviours, and they are the ones that carry the idea:

- **The checkmark is a radio.** Exactly one profile is in force and choosing another visibly takes it
  from the last, so the list reads as a state rather than as a folder of files.
- **Duplicate.** Nobody edits a working configuration directly — they copy it, rename the copy, and
  edit that. It is the difference between a settings screen and version control.
- **Saving under an existing name updates it**, rather than quietly creating "studio 2".

**A profile is the same bytes the timestamped backup writes.** The only difference is that the name
is his rather than a clock's, which is what turns a thing to recover into a thing to choose.

`maActiveProfile` stores a **name, not a path** — the folder is the truth, a path breaks on rename or
a new phone, and a name that no longer exists simply shows nothing active, which is correct.

## What is deliberately not built

**The collapsible tree and the master-detail pane.** They reorganise every screen in the app, and
profiles are worth having before anything is rearranged around them.

**When they are built**, the shape to follow: `MaSettingsEntry` is already an ordered list of
categories, and `MaSettingsOrderScreen` already routes each to a screen. A tree is that list with a
`children` field and a twist-down; the detail pane is the existing screen rendered beside it rather
than pushed onto it.

**And the deeper idea worth stealing before the UI:** Avid's real move is that a *setting* is an
instance you can have several of — three export presets, four keyboard maps. Here that would mean
several named feature-row layouts or prompt sets, not just one of each. **Profiles do it for the whole
app at once; per-category instances would do it properly.**

---

# 96. The Avid settings architecture: what exists, what is missing, and the order to build it

He specified the model precisely: **profiles → categories → instances**, with radio-button
activation, twist-down folders, duplication, master-detail routing and a search filter. It is the
right model for someone with twenty-seven years of Avid muscle memory, and it should be built.

**It must not be built in one pass.** It touches all nineteen settings screens and he uses this
keyboard every hour of every day.

## Layer 1 — the profile. ALREADY BUILT.

`MaSettingsVault` + `MaProfilesScreen` have named profiles, `RadioButton` activation, **duplicate**,
delete, and a folder that is the truth rather than a cached list. `switchProfile` and
`exportProfile` are covered by activation and the shared Documents folder.

**Do not rebuild this.** Read it first.

## Layer 2 — instances within a category. NOT BUILT, and it is the whole idea.

Today each screen holds **one** configuration. Avid's power is that a category holds *several named
instances* and one carries the checkmark: three keyboard maps, four export presets, and you switch
by clicking.

**Where this is worth having, in order of value:**

1. **Prompts** — Ctrl+P and Ctrl+F already hold one custom wording each. Several named prompts with
   a checkmark is exactly his earlier request (§ the CC screen), and this is the same feature.
2. **Rows** — the three feature rows plus the copy row are already presets; naming them and adding
   duplicate makes them instances.
3. **Voices** — already a radio list. It is an instance list without the vocabulary.

**Everything else holds one thing and should stay holding one thing.** A category with one possible
instance is a folder with one file in it, and Avid users know that is noise.

## Layer 3 — the tree, twist-downs, master-detail, search. LAST.

The settings home is a flat draggable list he arranged himself. Turning it into a tree is the most
visible change and the least valuable one: **nineteen categories that mostly hold a single instance
would make every setting two taps further away than it is now.**

Build layer 2 first. If enough categories end up holding several instances, the tree earns itself. If
they do not, the tree was never the feature — the instances were.

## The generic mechanism to write once

`name`, `isActive`, `isDefault`, `configurationData`, and `duplicate()`. One storage shape reused by
prompts, rows and voices rather than three implementations — the same argument that made the copy row
share the feature row's editor rather than copy it.

---

# 97. The zone keys get their frame back, and the ring goes

Two builds ago the `n k c` keys were a letter inside a small keyboard outline with a spacebar line
under it. Then the outline moved to the key as a lit green ring and the letter grew to fill the bare
face. He looked at it on the row and called it ugly, amateurish, a downgrade, and out of standard
with the rest of the application — and every word of that is correct.

**Why the ring was wrong.** Nothing else in that row wears a border. AP, AC, the clipboard keys, the
mic, the gear, the speakers — all of them are a shape on a plain key face, and state is said by
colour. Three keys with a green outline around them are not emphasised, they are *different*, and
different is what the eye lands on first. The ring did read from further away. It also read as
something added later by somebody who had not looked at the row.

**Why the bare letter was wrong.** Every neighbour has a silhouette. A loose lowercase letter sitting
among drawn glyphs looks like a placeholder waiting for its icon.

**What it is now.** The keyboard outline is back inside the glyph, drawn at the full width of the
box. The spacebar line is gone: at this size the rounded outline reads as a keyboard without it, and
the bar was costing the bottom quarter of the body to prove something already obvious. That quarter
went to the letter, which is now `0.70` of the glyph box against the `0.40` it had — nearly twice the
height, inside a frame, which was the whole point of the exercise the ring was invented to solve.

**The rule this leaves.** Colour is the state channel and it is the only one. Shape is the identity
channel. If a frame crowds its content, take detail out of the frame; do not take the frame away.
Both HANDOFF entries that said the opposite have been rewritten rather than deleted, so the next
person can see it was tried.

---

# 98. Prompts become a category of named instances — Avid layer 2, first use

Section 96 said layer 2 is the actual idea and prompts are where it pays first. This builds it there
and nowhere else.

**Ctrl+P and Ctrl+F each hold a set now**, not a wording. Any number of named instances, exactly one
with the checkmark, and that is the one the key sends. Radio and not a tick-box: there is one wording
in force and the control should say so rather than leave two looking equally chosen.

**Default is a real instance and it is locked.** Its text is empty, which means *use the instruction
compiled into the app* — empty rather than a copy of the shipped words, so improving the built-in
text later reaches everybody instead of only the people who never touched it. It cannot be renamed,
edited or deleted, and its editor offers one verb: duplicate. That is the Avid behaviour rather than
a restriction. Nobody edits a factory setting; they copy it and edit the copy.

**Duplicate does not steal the checkmark.** A copy is the start of an experiment, and a key that
changed what it does the moment you pressed copy would be a nasty surprise mid-sentence.

## Migration is a shape check, not a flag

The old preference held plain text. A stored value that does not start with `{` is therefore a
wording from the previous version, and is read as two instances: the locked Default and one called
**Mine** holding what he wrote, active. No flag, no version number, nothing to run once — a value
written by the old build is simply legible to the new one, forever.

## The bug this nearly shipped with

The shortcuts screen used to write a paragraph straight into that same preference. Left alone it
would have looked like a legacy value on the next read and **silently discarded every other instance
in the category**. A row that destroys data by doing exactly what it always did is the worst kind,
because nothing about it looks new. It shows and routes now; the editing lives on one screen.

## Tested

Ten cases against the parse/serialize rules before any of it was wired up: first run, legacy
promotion, a round trip carrying quotes, newlines, tabs and a backslash, an active name that no
longer exists, a file with no Default, duplicate names, corrupt JSON, and the copy-naming ladder.

**Also caught before the build:** an unused-import sweep removed `getValue`, which `by` delegation
needs without ever naming it. HANDOFF already warns that import checking must be exact-line; it
should also say that `getValue` and `setValue` are never textually present and must never be swept.

---

# 99. The highlight becomes a setting, and the page turns white

Three separate controls in Reader settings rather than a list of presets, because they combine:
white-and-underlined is a real choice and so is yellow-and-bold, and a preset list would need eight
rows to say what three switches say.

- **Colour: yellow or white.** Two, not a picker. It has to carry against a near-black box and it
  has to differ from the words around it; everything outside those two is either one of them or
  worse than both.
- **Bold, off by default.** The reason it was removed in the first place still stands: bold changes
  the WIDTH of a word, so the line re-flows as the highlight passes and the text appears to breathe.
  It is a choice now rather than a ban, because he asked for it and on a short page the movement is
  small.
- **Underline, off by default.** The one mark that costs nothing: it sits in the descender space the
  line already reserves, so it can be on with a white highlight on white text and still be the only
  thing that moves.

**The rest of the page is now plain white**, where it was a dim grey. He asked for it, and it is also
what makes the white highlight usable at all — with a grey page, "white" would just have been a
second colour rather than the same colour marked a different way.

Defaults are yellow, not bold, not underlined, which is exactly what it did before, so nobody's
reading changes until they change it.

---

# 100. One gesture: up to record, down to send

> *"I record up, I press down, it sends. No waiting, not many steps."*

Volume down now does two jobs, decided by whether a recording is running.

**While recording** it is the whole thing in one press: stop, transcribe, and press Send once the
words have landed. **Otherwise** it is exactly what it always was — press the send target on screen —
so nothing anybody relied on changed.

## Three things that had to be right

**The send is armed, not performed.** The key handler cannot send: the text is not in the field yet,
and a send that fires first sends an empty message that nobody notices until they read the other end.
So volume down sets `sendAfterCommit` and the transcription path fires it when the words are down.

**It fires below BOTH delivery branches.** The realtime path commits through `commitDictationFinal`
and never touches `committed`, so a send placed inside the other branch would simply never fire for
anyone using realtime — and it would fail *silently*, which is the worst way for this to be wrong.

**It is disarmed on every abandoned path** — `cancelRecording`, `cancelTranscription`, and an empty
transcript. Otherwise the arming survives and the *next* ordinary dictation sends itself. **A stray
send is worse than a missed one: it puts words in front of somebody.**

## One definition, three triggers

`MaMagicTargets.pressSend()` now owns resolving and pressing the send term, and the volume tap, the
volume-while-recording path and the row's key all go through it. The moment a second copy exists the
three can disagree about what "send" means, and the one that disagrees is the one used at four in the
morning.

## Tested

Seven cases, 0 failures: the full gesture, the old idle behaviour, up-then-up not sending, a cancel
followed by an ordinary dictation that must not self-send, an empty transcript, and two gestures in a
row producing exactly two sends.

**Not tested: the phone.** Whether the send lands reliably after the commit in a given app, and
whether any app needs a moment between the text arriving and the button being pressed.

---

# 101. Five caption styles, and the box goes full screen

He asked for the styles people put on captions, and for the small box to be able to fill the screen
like the reader in `ma-reader-thermux` does.

## The five

| Style | What it does |
|---|---|
| **Highlight** | the page stays readable, one word marked — the old behaviour, still the default |
| **Typewriter** | the words arrive as they are spoken; nothing ahead is visible |
| **Karaoke** | the line fills up behind the voice: said in amber, saying in white, coming in grey |
| **Spotlight** | everything turned down but the word being said |
| **One word** | that word alone, large, centred — the style Instagram and TikTok made ordinary |

**All five leave the layout alone, and that is why these five and not five others.** Colour, alpha
and decoration change nothing about where a word sits.

**Typewriter draws what has not been said yet as fully transparent rather than absent**, so it still
occupies its space and the words already on screen never move to make room. That is the whole trick;
without it, typewriter is the worst offender of them all.

**Karaoke's current word is white and NOT bold.** The first version bolded it, and Test 1 caught that
it was the only style able to re-flow the line — in a style whose entire effect is a steady front
edge moving along a *still* line, that ruins it. Three colours say it without weight.

**One word is the only one that hides its neighbours**, which is the point: there is nothing to read
ahead to.

## Full screen

Long-press the box and it grows to 330dp, covering the keys; 26sp text, nine lines, and pagination
follows — 460 characters a page instead of 105, because a fullscreen box paged for three lines shows
a paragraph floating in an empty screen and turns a page every few seconds for nothing.

**The rule taken from MA Reader:** full screen there hides *every* child of the view except the text,
so anything added later is hidden by default and has to argue its way back on. The version before it
named the things to hide — a list — and lists go stale, which is exactly how the player bar survived
into full screen. Here the same idea costs one number: the box covers the keys and nothing else is
asked to move.

**Two gestures, no controls drawn.** Tap skips the sentence, long press toggles full screen. A
caption with buttons on it is not a caption, and both gestures are on the element already under the
thumb.

## Tested

The style rules alone across four styles at three positions, and the layout check that found the
karaoke bold. **Not tested: the phone** — whether 330dp covers the keys on his Nothing Phone 2a, and
whether 460 characters is right for nine lines at 26sp.

---

# 102. Full screen means full screen, and the line comes to the eye

Three corrections and one new thing, all from one message.

## Full screen now hides everything

It was a taller box with the keyboard still under it. While reading with the box expanded, the
subtitle row is now the **only thing composed** — no rows, no keys, nothing beneath.

**Gated on something actually being read**, so a preference can never leave the keyboard unusable:
stop the reader and every key is back.

Its height is `FlorisImeSizing.keyboardUiHeight()`, the height the keys would have taken, rather than
a number of its own that would be wrong on the next phone. `weight(1f)` does not work here — the
enclosing Column wraps its content, so a weight resolves to nothing.

## The font stopped growing

Full screen enlarged the type as well as the box, so one gesture changed two things and he could not
have the whole page without large type he had not asked for. **Full screen shows MORE, not BIGGER.**

Size is now `maReaderFontSize`, one number for both views, stepped in settings, 10–40. One word is the
exception and scales from the same number — there is only ever one on screen, so it takes the room
the others share.

## The reader, as MA Reader does it

Full screen no longer pages. **The whole passage is there, in short lines, and the line holding the
spoken word is scrolled to the TOP.** His eye rests in one place and the sentences come up to meet
it; everything below is what is coming, and he can glance at it without losing the place, because the
place does not move. Lines already read are dimmed.

**A "page" full screen is ONE line**, not three: it is the unit that jumps, and a three-line unit
would jump three lines at a time and lose the steadiness that makes it readable. Scaled by the font,
since larger type fits fewer letters across the same width.

**`scrollToItem`, never `animateScrollToItem`.** He cannot watch animated scrolling, and a line
sliding into position is precisely the thing that turns his eyes inside out.

Verified: every word maps to the line that contains it, the scroll fires exactly once per line, and
it is monotonic — it never scrolls back up.

## Postponed, at his suggestion

The Avid-style settings — a triangle beside each entry that opens it in place instead of pushing a
new screen. He said postpone if it is too much for this pass, and it is: it touches all nineteen
screens. Layer 2 in `MANTRA_MANIFEST/modules/design-language.md` §10 already describes the model.

---

# 103. The switchboard becomes the whole remote

Five switches became thirteen, in three groups, and every row carries the glyph the keyboard itself
shows — the standing rule from `MANTRA_MANIFEST/modules/design-language.md` §4.

**On the keyboard:** the keys, the number row, the feature row, the suggestion row.
**The copy row:** on the keyboard, in dictation, the edit row, the buckets.
**Pressing and speaking:** the magic finger row, the volume keys, the subtitle row, full screen.

Grouped because thirteen switches in one list is a list, and three groups of four is a remote. He
described it as a remote controller and that is the right word for what it should feel like.

## The subtitle row is stored as a word, and still gets a switch

`maReaderDisplay` holds `subtitle`, `spacebar` or `off` — three values, of which two are "shown". A
switch is still the honest control: **he is asking for it or not asking for it, and *where* it
appears is a choice for the Reader screen** where there is room to explain the difference. Switching
it back on returns `subtitle` rather than whatever he last had, because what he last had was `off`.

## Two entry points, not one clever function

The first version took either a boolean or a string preference and branched. That would call
`collectAsState` a **different number of times depending on which argument was given**, which is the
kind of thing that works until it does not. There are two small composables over one shared row
instead, so every pass reads its preference the same way.

## Checked before building

All eleven icons confirmed present in the artifact, and every preference named by a row confirmed to
exist in `AppPrefs` — both by script rather than by eye. Those two are the cause of most red builds
in this project.

---

# 104. The housekeeping: every red build becomes a check

Six of the last thirty builds were red and **not one was a design mistake.** They were all the same
shape — a scripted edit leaving a duplicate declaration, an orphaned annotation, an import added
twice or removed while still used, or a symbol used above the line declaring it.

Each cost five minutes of CI and a message to him saying it had gone red. `scripts/verify.py` turns
every one of them into a check that runs in under a second.

**What it checks**, each learned from a specific failure: duplicate imports · an import above
`package` · a name declared twice in one scope · two `@Composable` in a row · an icon used without
its import · `by` delegation without `getValue` · a preference that no longer exists in `AppPrefs` ·
a `when` with no branch for a new enum entry · brace and paren balance against `HEAD` · anything
key-shaped in the diff.

**It reads only the files this commit touches.** A full-tree scan finds old debts and buries today's
mistake among them.

## The part that took the longest was not crying wolf

The duplicate-declaration check used indentation as a proxy for scope. First version: **454 false
alarms** on code that compiles perfectly. Tightened: **106**. Both useless, because Kotlin is happy
for one name to live in two scopes — and **a checker that cries wolf is worse than no checker**,
since it trains you to ignore it.

It tracks scope by brace depth now. Down to 17 across 407 files, all in files this project never
edits, and it still catches the real one.

## Tested

**All five reproduced red builds caught, 5 of 5.** Then end to end: two real faults injected into a
real file → exit 1 naming both; restored → exit 0.

---

# 105. The controller starts coming apart, at the only seam that is free

`DictateController` was 3,821 lines holding recording, transcription, history, prompts, audio routing
and provider resolution. It is 3,657 now, with **`MaProviders.kt`** holding the last of those.

## Why this block and not a bigger one

The obvious candidate was history — eleven functions, contiguous, 230 lines. It reads `_state`,
`transcribe` and the output sink, so moving it would mean **making the controller's private state
public purely to relocate code.** That trades one problem for a worse one, and the worse one is
permanent.

This block was measured before it was moved: **it reads nothing but preferences.** No state, no sink,
no scope. It answers questions — which account, which preset, which model, may this take the fast
path — from settings alone, which is exactly the shape that belongs outside a controller.

**The rule for the next extraction:** measure what a block touches before deciding to move it. A
seam that requires widening visibility is not a seam.

## What the file must never become

A place where anything is decided by side effect. Everything in it is a pure question about
configuration; the moment something there writes a preference or touches the UI, it has stopped
being resolution and belongs back in the controller.

## The red build this avoided

`maUseSyncPath` uses `SYNC_SAFE_LANGUAGES`, a **private** const still sitting in the controller.
Moved with it, since its only user went with it — and the doc comment went too, because the reason
it is an allow-list is the whole value of the line.

`scripts/verify.py` did **not** catch this and CI did: four more symbols were left behind — `File`,
`OpenAiCompatibleClient`, `MaResample` and two sync constants. **Moving code is the one edit that
breaks references without touching the line that uses them**, so both files keep their shape and
neither looks wrong.

`check_symbols_resolve` closes it, and its scope is the interesting part. Over the whole tree it
raises **256 complaints on code that has compiled for years** — annotations, nested types, things a
regex cannot see the shape of. Over a file **HEAD has never seen** it is exact, and a file that did
not exist before is precisely when code has been moved. So it runs on new files only.

Verified both ways on a genuinely new path: both missing imports named, and silent on the corrected
version.

---

# 106. The documents get their own housekeeping

The code came out of this round clean: no dead preferences, no settings entry without a screen, no
`TODO` left in the dictate package, every enum branch covered. The debt was in the documents, and it
was mine.

**`HANDOFF.md` said "last updated at build 122."** We are at 214. Its "open and not built" list named
Groq language detection — a feature that has since been **built and then removed entirely**. A
briefing that is ninety builds stale is worse than none, because it is believed.

Rewritten to what is actually true, including the honest list of what is open: the Avid settings, the
rest of the controller split, the key modules, offline retry, the English dictionary.

**`NEXT_DEFAULTS.md` is 4,000 lines and 101 sections**, and `HANDOFF` was telling every new chat to
read it second. My own rule in `MANTRA_MANIFEST/modules/handoff.md` says a document that grows
forever becomes a log and a log is read by nobody — and this is exactly that, written by the person
who wrote the rule.

It is not deleted, because the reasoning in it is the most valuable thing in the repository. It is
**named as an archive**: search it for one decision, do not read it end to end, and do not trust an
old section as current, since later ones override earlier and several describe features that are
gone.

`NEXT_DEFAULTS_INDEX.md` lists all 94 titled sections so a decision can be found without reading the
log. Regenerate it whenever sections are added.

**The rule this leaves:** when a briefing and a log both exist, the briefing must be rewritten every
time it stops being true, and the log must never be the thing a new chat is told to read.

---

# 107. The Black Void, and full screen that actually is

He is dyslexic. The reader is not a nice extra in this app; it is the part that does the most work,
and this round is about making it serve that properly.

## Full screen was never full screen

The branch sat two thirds of the way down the layout, so the early return skipped only what came
**after** it — the status line, the edit strip and the number row had already been composed. "Full
screen" arrived with three bands above it and read as a large box.

It is now the **first thing in the Column**, above the Smartbar and every row, so the return is
total: while reading expanded, the reader is the entire keyboard.

## The Black Void

A fifth style, and the one built for how he actually reads. **Full screen, true black, one word at a
time, as large as it will go.** No page, no neighbours, no box edge, no background lighter than the
void.

With nothing else on screen there is nothing to track back to, nothing to skip ahead to, and no line
to lose. The word arrives, is read, and is replaced.

**True black rather than the box's near-black**, because anything lighter draws an edge and an edge
is a second thing on screen. Long press collapses it back to the small box — the same gesture that
opened it.

## The zone keys get real icons

`n k c` were a hand-drawn keyboard outline with a letter inside, and he was right about them twice.
The neighbours in that row are drawn glyphs from one set; **a shape drawn by hand beside them reads
as an amateur patch however carefully it is proportioned.**

Numbers, Keyboard and ContentPaste now, at 24dp like every other icon, green when the zone is open.
`MaZoneGlyph.kt` is deleted rather than left unused.

**The rule this settles**, after two attempts at drawing it by hand: *use the icon set. If the set
has nothing for it, that is a reason to reconsider the key, not a reason to draw one.*

## Not built this round

**The colour picker** — a full-screen wheel, DaVinci-grade, with a draggable loupe, used everywhere
a colour is chosen. It is a real build of its own and deserves one.

---

# 108. A void that is actually the screen, and the colour wheel

## The half void

The reader asked for `keyboardUiHeight()`, so it filled the keyboard and the chat stayed above it. He
called it a half void and he was right.

**An input method draws in its own window, and that window is as tall as the view asks to be.** It
asks for `LocalConfiguration.screenHeightDp` now, so the window covers the display and the void is
the screen. The app behind pans up as it does for any tall keyboard.

## The colour wheel

Full screen, HSV, with a loupe. Hue round the rim, saturation from white at the centre to full colour
at the edge, brightness on a bar underneath — **the arrangement every professional tool uses**, and
using a different one would cost him muscle memory he already has.

**The loupe sits above the finger**, not under it. That offset is the entire reason a loupe exists;
without it a picker is a guess made with a thumb in the way.

**Held as HSV, not as a Color.** Converting back and forth loses the hue of a fully desaturated
colour — drag to the centre and out again and the hue would have been forgotten on the way.

**Two gradients, not a bitmap:** a sweep for hue, a radial white for saturation. Scales to any screen
and costs nothing.

**Live and cancellable.** The colour applies as it is dragged, so it is judged against the thing it
is for; Cancel puts back what it was, which is what makes dragging freely safe.

Wired to the reader highlight first. `maReaderHighlightHex` overrides the yellow-or-white pair rather
than replacing it, so no existing setting changes the day the wheel arrives, and a corrupted value
falls back rather than becoming black on black.

**Next place it belongs:** the subtitle page colour, the void's word colour, and the sand used for a
lit key — one wheel, everywhere a colour is chosen.

---

# 109. The dashboard, live speed, and swatches

## The speed was not slow, it was not connected

He changed the speed in settings while the voice was reading and it "started to respond after a few
sentences". That is exactly what it was doing: **the speed was read once, when a passage began
playing**, so a change reached the audio only when the next passage started.

`MaReader.setSpeedNow` sets `playbackParams` on the running player, so it applies at once. **A dial
is judged against what it is doing** — a control that takes effect later is one he cannot judge,
because by the time it works he has forgotten what he changed it from.

The karaoke needed no adjustment: it reads `currentPosition`, which is a position in the media
timeline and stays true whatever rate the media plays at. Same reason the speed came out of the
ticker in §86.

One trap in it: setting params on a *paused* player starts it on some Android versions, so the paused
state is restored immediately rather than trusted to survive.

## The dashboard

Long press on the reader key no longer leaves the keyboard for the settings app — **it opens three
dials over the reading: voice, speed, highlight.** Same long press closes it.

**Half the screen, not all of it.** The void takes the display because one word needs no context;
this is the opposite — he is adjusting *while listening*, so the reading has to stay visible. Covering
it would mean adjusting blind, which is the problem this solves.

**Only three.** Everything else about the reader is chosen once and left; putting it here would bury
the one thing wanted mid-sentence. **It closes itself when the reading stops**, so he can never be
left with a panel to dismiss.

## Swatches beside the wheel

Adobe puts swatches in every program and keeps the picker behind them, because the two answer
different questions: **a wheel answers "which colour", a grid answers "which of the ones I keep
using"** — and nine times in ten it is the second.

**Black and white first**, and that ordering is the design. Pure white is a single point at the
centre of the brightness axis and pure black is another, so on a wheel they are the two colours a
finger is least likely to hit exactly. A grid makes them one tap. Then greys, the app's own amber,
then hues in spectrum order, because that is where the eye looks without reading.

Swatches is the default view. Four of them also sit on the dashboard, where there is room for four.

## The verifier learned one more thing

It called `jetpref` and `kotlinx` imports unresolved, because it searched only `app/` and `lib/`.
Reporting that a dependency lives outside the repository is true and useless. It now checks only
imports this repository could satisfy.

---

# 110. Switchers get a ring, the switchboard gets his order, and the volume keys say why

## The volume keys

**Nothing was lost in the split.** `onKeyDown`, `onKeyUp`, `maHandleVolumeKey` and
`maFinishVolumeKey` are all intact and correct — up records, down while recording stops and sends,
down otherwise presses the target. The code that took weeks is there.

What is missing is an explanation when it declines, and there are exactly two reasons: **the Volume
keys switch is off**, or **the keyboard is not on screen**. Those look identical from outside, and
one of them is now reachable by a stray tap on the switchboard that did not exist a week ago.

Both now write a line to the log naming the cause. The next time these seem dead, `log` answers it in
one line rather than costing a session searching for code that was never lost.

## Switchers wear a ring

The row mixes two kinds of key that look identical and behave nothing alike: **a clipboard icon that
pastes, and a clipboard icon that shows the copy row.** Same picture, opposite jobs. Nothing on the
face can separate them, so the distinction goes around the key.

A ring in the ordinary ink at 55% — **not a colour.** Colour is the state channel: green means a
switcher is ON, and a second colour meaning "this is a switcher" would give one channel two jobs. The
ring says *kind*; the ink says *state*. Monochrome, as he asked.

Worn by the three zone keys and the language badge. Any future key that switches rather than types
passes `switcher = true`.

Drawn **after the background and on the key's own shape** — the first attempt put it on the modifier
before `ThemedKey`'s own padding, which would have placed it outside the key entirely.

## The switchboard is one list, in his order

Three headings and a paragraph of prose are gone. He said he already knows what "on the keyboard"
means, and he is right: **a heading naming a category he can see is a line to scroll past.** Titles
and icons only.

And it drags. Every list he reads often is arrangeable — the settings home, the rows, the copy row —
and the switchboard was the last one still in an order somebody else chose. **A shipped order is a
guess, and a guess repeated every day is worse than a control.**

**Stored by id, never by position.** A list of numbers would mean something different the moment a
switch is added. Unknown ids are dropped, missing ones **appended rather than hidden**, so a switch
added next year is reachable by somebody whose arrangement predates it.

Tested: 5 cases, and the invariant that every switch is present exactly once whatever is stored —
empty, unknown, duplicated, partial.

---

# 111. The reader that went dead, and brightness instead of colour

## Why the speaker key died

Not fragmentation, and no module was lost. **`state` and `player` are two facts that can disagree**,
and when they do the key goes dead in a way that looks like nothing at all.

With `state = SPEAKING` and no player: a press pauses nothing and sets PAUSED, the next press starts
nothing and sets SPEAKING, and it flips between them **forever**. No sound, no way back to IDLE, and
the reader is dead until the keyboard is rebuilt. That is exactly what he described.

They come apart easily. The completion handler releases the player and hands off to the
scroll-and-continue coroutine; if that coroutine is cancelled in between — keyboard hidden, process
trimmed — the state is left claiming to read with nothing behind it.

**The fix is not to audit every path that could cancel.** There is always one more, and the audit has
to be redone whenever the code changes. `toggle` now checks the claim against the thing before acting
on it: **the player is the truth, `state` is a claim about it.** No player means idle, whatever the
machine believes.

Verified against the exact sequence: the old machine flips paused/resumed forever; the new one starts
reading on the first press. Normal use is unchanged.

## Brightness, not colour

Four coloured chips became one **grey-to-white ramp**. A highlight over white text does not need a
hue — it needs to be *more* than the page, and that is one axis.

**Bounded at half grey.** Below that the mark stops out-reading the page and does the opposite of its
job; a control should not offer settings that defeat it. **Neutral at every position** — r = g = b —
or it is a colour picker pretending to be a brightness control.

Tested both directions: position → hex → position round-trips exactly, never darker than `#808080`,
always neutral, and an old coloured value falls back sanely rather than throwing.

## Written into the manifest

Sections 10 to 13 of `MANTRA_MANIFEST/modules/design-language.md`: a dial is judged against what it
is doing; a switcher is not an action and the face cannot say so; brightness is not colour; state and
reality are two facts that will disagree.

---

# 112. Three integrity passes

Asked for a check across everything, three times, each reading the last.

## Pass 1 — structure, whole tree

407 files swept for duplicate imports, doubled annotations, icons used without importing them,
delegation without `getValue`, and Compose helpers without theirs.

**Three hits, two false.** `SpaceBar` was inside a comment explaining why that icon is *not* used;
the twelve "delegation without `getValue`" were custom non-Compose delegates in inherited
FlorisBoard files.

**One real:** `MaRowsScreen` had `@Composable` above a doc comment above the function. It compiles —
the annotation still binds — but it is the exact shape that caused red build 209, where a second
annotation hid behind a doc comment. Moved below its doc.

## Pass 2 — do the parts agree

Every enum entry has a branch in every screen that switches on it; every settings entry has both a
route and an icon; every route object is registered in the nav graph; every `ma*` preference read
anywhere exists in `AppPrefs`.

**All clean — but the first run reported `MaSwitchboardOrder: 0 entries`**, which is not a pass, it is
a check that never ran. Its enum is indented differently from the others, so the pattern missed it.
Re-run with the right pattern: 12 entries, all covered, all preferences real.

**A check that finds nothing and a check that runs nothing look identical from the outside.** Print
the count, always.

## Pass 3 — can a state machine reach a dead end

Reader, volume keys and send arming, each walked exhaustively rather than sampled.

- **Volume:** 1,000 hold lengths — 0 do both, 0 do nothing.
- **Send:** no sequence produces a send that was not asked for; none leaves the arming set.
- **Reader:** without the heal, **`SPEAKING` and `PAUSED` with no player are silent forever**. With
  it, none.

**And the reader test was wrong twice before it was right.** First it called pausing a live player a
dead end. Then it reported AUDIO from a null player — modelling the very bug away. Only after
modelling `start()` on nothing as *silence* did it reproduce the failure he actually had.

**The rule: when a test says the bug does not exist, suspect the test.** It had already been fixed in
the code, so the test agreeing was worthless — what proved it was the test failing on the old
behaviour and passing on the new.

---

# 113. Swatches as asked, effects not voices, and the volume keys prove themselves

## The swatches I should have built the first time

He asked for swatches. I built a slider and wrote a paragraph justifying it. **That was not a better
answer to his question, it was a different question answered instead.**

**Seven greys, dark to white.** Seven because a row of seven is read at a glance and chosen without
aiming; a slider needs a precise finger and hands back a number nobody asked for.

**The dark end is not black.** `#000000` on a near-black page is a square that appears to do nothing,
and a highlight set to it disappears into the text — a setting that defeats the control offering it.
The row starts at `#6E6E73`, the darkest that still reads as a mark.

## Effects, not voices

The voice is chosen once and left; it is taste, settled in an afternoon. **The effect is what changes
per passage** — void for something hard, highlight for skimming, karaoke to see the sentence coming.
That is what belongs on a panel opened mid-reading.

The six effects now live in `MaReaderEffects`, shared by the dashboard and the Reader screen, so a
new one cannot be added to one and forgotten in the other.

## The volume keys, made to prove what happened

The handlers are correct — read twice now, line by line. But *"the volume keys do not work"* has
**three** causes that are indistinguishable from outside: **the events never reach the keyboard**, the
switch is off, or the keyboard is not on screen. Only the last two were logged, which is why two
rounds of checking the handler found nothing.

Every volume press is now logged **before any decision is taken**. If nothing appears in the log when
he presses volume, the events are not arriving and no amount of reading the handler will help,
because it is never called.

And a disabled switch now says so **out loud, on screen** rather than only in a log. That switch did
not exist a week ago, it sits in a list, and one stray tap turns it off. An invisible failure cost him
days of believing a feature had been deleted; a toast turns it into a fact he cannot miss.

---

# 114. The seventh setup page is gone

"You're all set" said nothing that was not already true, and everything it pointed at was in the
settings list its own button opened. **A page whose only content is a summary of the pages already
read is a page to press past.**

The step, its five strings and its entry in the enum are deleted. The flag it set and the navigation
it did now live on a **Finish** button at the end of the accessibility step, so the last thing he does
in setup is a real step rather than an acknowledgement of one. Six steps, not seven.

The router's fallback pointed at `FinishUp` when nothing was left to grant; it lands on the last real
step now. Translated copies of the dead strings are left in the locale files — unused strings cost
nothing, and rewriting forty of them to delete text nobody reads is not a good trade.

---

# 115. A key that copies the screen

Everything this keyboard does to another application depends on the accessibility tree, and when the
finger cannot find a button or the reader cannot find the words, **that tree is the only place the
answer lives.**

Reading it used to mean summoning the wand, pressing the thing, and copying from the bar it raised —
a diagnostic that needed a rehearsal, at the moment something is already going wrong.

**One key.** Press it and the screen underneath is on the clipboard, ready to paste into a chat.

It calls `DictateAccessibilityService.dumpScreen`, the same path the wand's copy button uses, so the
two cannot produce different dumps. The toast says how many characters were copied — a dump that came
back tiny is itself the answer, and a silent success looks identical to a silent failure.

**A stack of layers, not a bug icon.** What it reads is the layer under the picture, which is exactly
what the tree is. Nothing here is broken, and an instrument should not look like an alarm.

Add it from Feature row settings; it is called **Dump the screen for diagnosis**.

---

# 116. The dead end at the end of setup, and why the wizard kept coming back

## The dead end, which was mine

Removing the seventh page put **Finish** inside the `else` of the accessibility step — the branch that
only draws when accessibility is **off**. His has been on for weeks, so he reached the last page of
setup with no button on it and no way forward. **A dead end at the end of a wizard**, which is the
worst place to put one.

Outside the branch now. Setup ends whatever the state of that switch, because the switch is optional
and finishing is not.

## Why it appeared on every update

Inherited from upstream, and it had nothing to do with updating:

```kotlin
if (API33+ && notificationPermissionState == NOT_SET) {
    prefs.internal.isImeSetUp.set(false)   // "show the setup screen again"
}
```

**If the notification permission has never been answered, the whole setup reappears.** On a phone
where that permission is simply never granted, the condition is true **forever** — so every launch
after every install walked back through six pages he had already finished, several times a day.

**Setup reappearing is not a way to ask for a permission.** Once somebody has finished it, *finished
is a fact and not a state to be revoked* by an unrelated toggle. The reset is gone; anything that
still needs asking asks where it is needed.

## Skip setup

A link in the header, on **every** page. Skipping one slide was never the problem — the problem is
being asked at all, six times, by a wizard whose entire content he wrote.

**In the header rather than the footer**, so it is reachable on a page with no other button and
cannot be scrolled past on the pages that have several. Nothing is lost by taking it: every grant it
offers is also in Settings, and the ones already granted stay granted.

**One `finishSetup`**, shared by Skip and Finish, because the flag must be written or the wizard
returns on the next launch, and `popUpTo` must be inclusive or Back walks straight into it again.

## Also fixed: reading anything long

His log gave the answer to the other bug in one pass. Every passage of **1,906 characters or fewer
spoke; every one of 2,110 or more came back HTTP 400** — twenty-three readings, no exceptions either
side of that gap. `MAX_CHARS` was **4,000**, chosen by me and never tested against the service, so a
long screen simply refused to read and filled the log with 400s that looked like a key fault.

Now 1,800, measured. **A cap invented rather than measured is wrong in one direction and silent
about it.**

---

# 116. The dead end I built, and the character cap I invented

## The wizard could not be finished

Removing the seventh page moved Finish onto the accessibility step — **inside its `else` branch**. So
anyone whose accessibility was already running, which is everyone after the first install, reached
the last page and found a line of text and nothing to press. **A dead end at the end of the wizard.**

And because finishing is what writes `isImeSetUp`, the flag was never written, so setup returned on
every launch. One misplaced brace produced both complaints.

**The rule: the way out of a screen must never be inside a branch.** A step can have nothing left to
do; it can never have no way forward.

`finishSetup` is one function now, passed into the steps rather than rebuilt inside them, because the
Skip link at the top does the same thing and two definitions of "setup is over" would eventually
disagree about whether the flag was written.

## The other reason the wizard kept returning

Inherited from upstream: if the notification permission had never been answered, the app set
`isImeSetUp = false` on every launch. On a phone where that permission is simply never granted — his
— that condition is true forever.

**Setup reappearing is not a way to ask for a permission.** Once somebody has finished it, *finished
is a fact, not a state to be revoked* by an unrelated toggle. Left as it was for a genuine first run,
where the flag is already false and the line changes nothing.

## Speechify refuses over ~2,000 characters

From his log, 23 readings: **every passage of 1,906 characters or fewer spoke; every one of 2,110 or
more came back `speak refused, http 400`.** No exceptions on either side.

`MAX_CHARS` was 4,000 — a number I chose and never tested against the service — so long screens
simply refused to read and the log filled with 400s that looked like a key problem.

Now 1,800, below the largest measured success with room for the service to tighten. **A cap invented
rather than measured is wrong in one direction and silent about it.**

---

# 117. The volume keys, rewritten, with no way to switch them off

Rewritten as `MaVolumeKeys`, specified in **`VOLUME_KEYS.md`**, and **the setting is gone** — no
preference, no switchboard row, no gestures entry.

That setting is the whole story. It defaulted on, it sat in a list of thirteen switches that had just
become draggable, and one touch turned it off in silence. He lost days believing the feature had been
deleted; I spent three rounds reading a handler that was correct the entire time.

**A control that can silently disable the thing somebody uses most is not a feature, it is a
trapdoor.** The specification says not to add it back and why.

Behaviour unchanged and now written down: up taps to record and to stop; down while recording stops,
transcribes and presses Send when the words land; down otherwise presses Send; either held is the
real volume. Decision on release, send armed rather than performed, disarmed on every abandoned path.

## One word is gone

It was the void with a smaller word and a box around it — not a second effect, the same one done less
well. A stored `oneword` falls through to `highlight`, which is the safe direction: a page he can read
rather than one word he did not choose.

## A way out of full screen that can be seen

Long press still closes it, but a gesture is invisible and a full-screen view with no visible exit is
a trap the first time it is forgotten. A dark grey **✕** in the corner, with a touch target larger
than the glyph. Deliberately near-invisible: an escape hatch, not a control.

## Two tools that were wrong, and cost more than the bugs

**The balance checker in `verify.py` reported the largest file in this project as two braces out when
it was balanced.** It stripped block comments with a regex over the whole file. Three rounds went into
hunting a fault that did not exist. It scans character by character with real state now — **a checker
that is wrong is worse than none, because it is believed.**

**And my own cut of the One word block took the Void's comment with it**, because I chose the
boundary by eye rather than by structure. Restored from HEAD and cut again by locating both blocks
first. When removing code, find the end of what you are removing before removing the start of it.

---

# 118. The reader becomes the thing the keys serve

## The twenty seconds

**The key ring restarted at key one on every single read.** Every dead and every throttled key ahead
of the good one was paid for again — a full network round trip each — before a word could be spoken.
His log shows it plainly: `key 1 throttled, rolling forward`.

It resumes from the key that answered last. Measured against his situation — 21 keys, five tired ones
at the front — that is **six requests per read down to one.** The skipped keys are not forgotten: the
walk wraps, so a key that recovers is found again.

## Nothing interrupts a reading

`onStartInputView` stopped the reader, and it fires for a notification, a dialog, switching views,
pinning the keyboard — every one killed a reading mid-sentence. **A reading is a task he started
deliberately and can end with one press.** It now survives all of that, and the keyboard being
collapsed by the system.

Three things stop it: the reader key, the ✕, and the end of the passage.

## The volume keys drive the reading

Nobody dictates into a screen they are listening to. While a reading is in progress, **up taps skip
forward and down taps go back**; holding either is still the volume.

Not a setting — the keys follow what is on screen. A control that means the obvious thing for the
situation in front of him needs no mode and no memory.

**Back is two-step**, as every media player is: part-way through a sentence it restarts that
sentence, pressing again goes to the previous one. 1.5 seconds is the line between "I missed that"
and "I want the one before".

## The swatches collapse

Seven open by default is seven things to look past every time the panel is raised, and the panel is
raised for speed far more often than for colour. One swatch showing the current choice; tap to unfold,
and it folds itself away once chosen — the row exists to answer one question and it has been answered.

---

# 119. Down is forward, and back is one step

Both corrections from him, both about the same instinct.

**Down goes to the next sentence, up to the previous.** The text moves down the screen as it is read,
so down is where the next sentence is, and every scroll on the phone has already trained the hand.
Mapping up to "next" because up is bigger imposes a rule from arithmetic on a rule from movement.

**Back is one step. No replay window.** The first version restarted the current sentence if he was
already part-way into it — the media-player convention, and wrong here. A skip key skips. To hear a
sentence again he presses up then down: two deliberate presses, and less thought than a key whose
meaning changes with how long he has been listening.

**A control that does two different things depending on timing is one that has to be predicted.** At
speed, predictable beats clever. `REPLAY_WINDOW_MS` is deleted rather than raised, because any value
for it is the wrong idea.

Checked: up-then-down returns to the same sentence from anywhere except the first, where there is no
previous one and up restarts it instead. Each press moves exactly one sentence, and the last sentence
holds rather than running off the end.

---

# 120. Which key is speaking, in the corner

A dim `3/21` in the bottom-right of every reading view: the Speechify key that spoke, and how many
are on the ring.

**A ring that walks silently is a ring nobody can reason about.** When a reading is slow to start,
this says at once whether it is on key 3 because two are tired, or on key 1 and simply waiting for the
network — a distinction that used to cost a log export and a round trip to me.

**Updated on every attempt — it IS the progress bar.** The first version updated only on success, on
the reasoning that a flickering number is a progress bar rather than an answer. He wanted the progress
bar, and he was right: while a reading is slow to start, watching the number climb *is* the diagnosis.
A still `1` means waiting for the network; `1 2 3 4` means the ring is walking past tired keys.

**One number, no total.** He can count his keys in settings, and a second number on a screen built for
one word at a time is one more thing competing for the eye.

Held as Compose state rather than a plain field, so it actually refreshes — a `@Volatile Int` would
have been correct and invisible, which is the worst combination for an instrument.

As dim as the close mark, and for the same reason: it is an instrument, not a control, and anything
brighter would compete with the words. Drawn only when there is something to say.

**In every reading view**, not only full screen, because the question it answers is asked when
something is wrong, and being wrong does not pick a view first.

---

# 121. A line that says it is listening

The volume keys record whether or not the keyboard is on screen, and he found that by accident and
kept it — dictation without opening anything is the feature, not a side effect.

But **a recording nobody can see is a recording he does not know he started**, and the first he would
learn of it is a transcript arriving from a conversation he had with somebody else in the room.

**Anything that captures a microphone must be visible while it does.** Not as a courtesy — as the
minimum honesty of a device that listens.

## A line, not a bubble

There is already a floating button and it is a *control*: draggable, pressable, occupying a corner.
This carries one bit — *this is recording* — and the smallest shape that carries one bit is a line.

Three device-pixels at the very bottom, full width, not touchable and not focusable, sitting in the
gesture bar's own margin so it covers nothing.

**Red, and this is the one place in this app where red is right.** Buckets stopped turning red when
full and untranscribed recordings are blue, because neither is a fault — but a live microphone
genuinely is something to be aware of.

## Only when the keyboard is hidden

With the keyboard up he can already see the timer, the waveform and the red dot. A second indicator
would be a second thing saying what the first one says.

Driven by combining the recorder's state with `_imeVisible` rather than by an event, so **it cannot
be left showing by a path nobody thought of** — every change to either fact re-answers the question.
`show()` is idempotent because that flow emits on every change and adding a window twice throws.

---

# 122. The strip says what it is, and the history badge answers again

## The history badge

It called `MaLanguage.badge()`, which reads the preference directly. **Compose cannot see that
dependency**, so the label had no reason to redraw when the preference changed — the tap worked, the
language changed, and the badge went on saying what it said before. From outside: a button that does
nothing.

The label now comes from the state already being collected, so the redraw is a fact rather than a
hope. **A value read outside Compose's knowledge is a value that will eventually stop updating**, and
it fails as "the control is broken" rather than as anything that looks like a stale read.

Its touch target grew too: it was text with padding, and text is exactly as wide as its letters — two
of them here. On a list where every other control is a full row, a target the size of "HR" is one a
thumb misses.

## The recording strip

He asked for **the recording bar**, and I built a strip with the word `recording` on it — my own idea,
answering a question he had not asked. He had already described it and sent a screenshot.

It is the bar now: **ENG, the bin, the red dot and the clock, and send.** The same controls doing the
same things, so the interface does not change identity depending on whether the keyboard is up — it
moves back to its usual place when the keyboard arrives, and that is all.

**Tapping the numbers brings the keyboard up.** His instruction, and the right target: the clock is
the biggest thing on the bar and the one the eye is already on.

Plain Android views rather than Compose, because a Compose overlay needs lifecycle and saved-state
owners this service does not have. The clock reads the recorder's own state once a second instead of
counting for itself, so a pause is honoured and the two views can never disagree about how long he
has been speaking.

`FLAG_NOT_TOUCHABLE` is gone since every glyph is a control; `NOT_FOCUSABLE` stays, because it must
never take the cursor from the field being dictated into.

---

# 123. The language badge moves onto the recording it describes

**The header badge is gone.** It said what the *next* send would use — a fact about the future,
attached to a list of the past — and it was clipping to "HR" besides.

**Each recording now carries its own badge: the language it was actually sent in.** That answers the
question being asked in that screen: *this came back wrong, what did I send it as?*

**Tapping it switches the language and re-transcribes in one press.** Correcting the mistake is one
gesture, because that is what the mistake costs him.

**"Transcribe" is now "Retranscribe"** — the whole word, and the right one: this recording has
already been transcribed once. It had been clipping to "Transcri", so the row is tighter too: four
labels share the line now and the words carry the meaning, so the space between them was doing
nothing but pushing them apart.

## Still owed: the overlay bar, pixel for pixel

He has asked three times and I have approximated it twice, which is worse than not starting.

The obstacle is real and worth writing down: the bar is a **Compose** composable using `SnyggIconButton`
and the IME's theme scope, and the overlay is a plain window owned by the accessibility service —
which has no lifecycle owner, no saved-state registry and no Snygg provider. Rebuilding it in Views is
what produced two near-misses.

**The answer is to host the real composable**, by giving the overlay window the owners Compose needs
and the theme provider the bar reads. That is its own build, and it is the next one.

---

# 124. The badge appears, the words are whole, and the header balances

Three faults, one cause: **I sized words by fractions of the line.**

A weight is a promise about width that a word cannot keep. Given a quarter each, "Insert" clipped to
"Ins", "Delete" to "Del", "Re-Transcribe" to "Retranscri", and the badge — the narrowest — was
squeezed to nothing and **never appeared at all.** He reported it as missing; it had been drawn with
no room to exist.

Sized to content, spread with `SpaceBetween`. Each label says its whole word and every gap is equal.

**The badge is `[ENG]` / `[HR]`**, in square brackets so it reads as a label on the recording rather
than a fourth action. Tap it: the language swaps and the audio goes back up in one press.
**Re-Transcribe** keeps its dash and its final letters.

## The header

Removing the old header badge left the cog stranded beside the back arrow, with the whole row leaning
into the left corner. The title is back between them and the cog is at the far edge.

**A row with everything at one end is not a row, it is a pile.** Written into
`MANTRA_MANIFEST/modules/design-language.md` §10 along with the rest: equal distances everywhere, and
**after removing anything from a row, look at the row** — balance breaks silently, and it breaks in
whatever was left behind.

---

# 125. The VU meter, from the module rather than from scratch

Third time asked, and he was right to keep asking.

**It reads `DictateController.audioLevel` — the same StateFlow the keyboard's meter collects.** The dB
conversion, the −54 floor, the 0.6 dB peak decay and the three colours are lifted from
`MaRecordMeter` unchanged. **One source of numbers drawn twice, rather than two meters that happen to
look similar.**

Drawn with a Canvas rather than composed, because the overlay window has no lifecycle owner for
Compose to attach to. **That is a difference in the brush, not in the picture** — and it is the whole
of what "the module" could not be reused literally, which I should have said two builds ago instead
of quietly approximating.

**Peak hold, because a bar alone is a flicker at speech rates.** The mark falls at 0.6 dB per frame:
slow enough to read, fast enough to follow a sentence. That is what makes it a meter rather than a
light.

**Fast attack, slow release**, the asymmetry every hardware meter has — an attack that lags feels
dead, a release that snaps feels nervous.

It sits directly under the clock, as one column, which is how the keyboard's bar arranges them: the
numbers with the level beneath, not two things sharing a row.

---

# 126. The overlay hosts the real bar

Asked four times, approximated three. **It is the same function now: `DictateSmartbarUi`, called
directly.** Same controls, same icons, same VU meter, same theme — anything that changes on the
keyboard changes on the overlay, because it is the same code.

## What was actually in the way

A window added by an accessibility service has **no Activity behind it**, and `ComposeView` refuses
to compose without three things an Activity supplies invisibly: a **lifecycle owner**, a
**saved-state registry** and a **view-model store**. The failure is a blank view, not an error, which
is why "reuse the composable" kept collapsing into "reimplement the composable".

`MaOverlayHost` supplies all three in eighty lines.

**Eighty lines against three failed rebuilds.** That trade should have been obvious two builds
earlier, and the rule is worth keeping: **when reuse is blocked by plumbing, build the plumbing.** A
copy of a living thing has to be maintained forever and drifts the first time the original changes —
which is exactly what happened here, three times.

The lifecycle goes RESUMED on attach and DESTROYED on removal. A registry left at CREATED composes
but never animates, so the meter would sit still — a failure that looks like a broken meter rather
than a missing lifecycle.

---

# 127. The offline note

He dictates with no text field open — the volume keys record whether or not a keyboard is up — and
until now the transcript went into the history looking exactly like every other entry: a first line, a
timestamp, and nothing saying **it was never delivered anywhere.**

**A dictation with nowhere to go is a note, and a different kind of thing.**

- **Its own source.** `DictateHistorySource.OFFLINE`, set from the commit's own result rather than
  guessed: `commitOutput` already returns whether the words landed, and that is the fact.
- **Its own title**, written by the model. He comes back to these hours later, and a wall of first
  lines is not something anybody can search by eye.
- **Its own colour** — teal, deliberately not amber and not red. Amber means "the live one"
  everywhere else here and red means a fault; a note he dictated on purpose is neither.

## Three decisions worth keeping

**Only notes get titled.** Every other entry was watched arriving in a field. Titling all of them
would spend his money labelling things he already knows.

**The title never fails the save.** Wrapped, and a failure returns empty — which the list renders as
the first line, exactly as before. A note without a title is a note; a note lost because a titling
call timed out is not.

**Under forty characters gets no title.** A six-word title on an eight-word note is not a summary, it
is the note again in worse handwriting.

## The migration

The title needed a column, and this database falls back to **destructive** migration — which would
have deleted every recording he has. `MIGRATION_4_5` is one line of SQL. **His history is not
replaceable and a column is not expensive**, so the fallback must never be the plan.

---

# 128. The crash I shipped in 239

Build 239 hosted the real bar and **would have crashed the moment it was shown.**

`FlorisImeTheme` reads `LocalWindowController`, and that local's default is not a fallback — it is
`error("only available within an IME view")`, a hard throw. I wrapped the composable in the theme and
never provided the local, so the first background recording would have taken down the composition.

**That is exactly what "it does not survive without the original keyboard" was**, and he described it
before I had built it.

Fixed by providing one. `ImeWindowController` takes preferences and a scope and needs no IME; all the
theme wants from it is a font scale.

## And a guard, because the blast radius was wrong

The accessibility service owns the magic finger and the reader as well as this window. An exception
while adding the bar would have killed all three — **losing the finger and the reader because a strip
of UI could not be drawn is a wildly disproportionate way to fail.**

It is wrapped now, and a failure logs and detaches the host. The recording is unaffected either way:
the microphone does not run through this window, so the worst case is recording without seeing the
bar, which is where the feature started.

## The lesson

**A composition local whose default is `error()` is a dependency, not a convenience.** Grepping for
what a composable *calls* found nothing; the dependency was one level down, in what the theme reads.
When lifting a composable out of its home, check what its wrapper reads too.

---

# 129. Why the bar recorded but never appeared

He asked the right question: **the hand-built version showed and the real one did not — what is
different?** Comparing them found it in one line.

| | views version (worked) | compose version (invisible) |
|---|---|---|
| window height | `WRAP_CONTENT` | `WRAP_CONTENT` |
| **who decides the height** | **the LinearLayout, from its children** | **`DictateSmartbarUi`, via `fillMaxSize()`** |
| children with a fixed size | yes — dot 10dp, meter 4dp, text | no — everything fills the parent |
| **resulting height** | sum of the children | **zero** |
| did it throw | no | no |
| did recording work | yes | yes |

`DictateSmartbarUi` sizes itself with `fillMaxSize()`, which is exactly right **inside the keyboard**:
the smartbar slot has a fixed height and the bar fills it. My window is `WRAP_CONTENT`, so the
parent's height is whatever the child asks for — and **a child asking to fill its parent, in a parent
sized by its child, resolves to nothing.**

The window was added. The composition ran. The recording worked. It was nought pixels tall.

Fixed by wrapping it in a `Box` of `FlorisImeSizing.smartbarHeight` — the same number the keyboard
gives it, so the bar is the size it is at home rather than a size invented here.

## The pattern, twice in three builds

`LocalWindowController` threw; this one was silent. Both are the same mistake: **a composable carries
assumptions about its parent that are invisible in its own source.** The theme it is wrapped in, the
locals it reads, the height it expects to be given.

**When lifting a composable out of its home, the question is not what it calls — it is what it was
being given.**

---

# 130. Reverted to the version that works

Back to the hand-built bar from build 238: `LinearLayout` with ENG, the bin, the red dot, the clock,
the VU meter and send. It draws, and that is the whole of what matters.

The Compose route is abandoned. Two obstacles were found and fixed — the missing
`LocalWindowController`, then `fillMaxSize()` resolving to zero — and it still did not appear, which
means there is at least a third I have not found. **Three builds of a feature that already worked,
spent on making it "properly" reuse code, is a bad trade whichever way it ends.**

`MaOverlayHost` is deleted rather than left in the tree. An unused class that solves a problem nobody
has is a thing the next person has to read and decide about.

## What this cost, and the rule

He named it correctly: **go back to the one that was working.** I had a working bar at 238 and spent
239 to 242 replacing it with something that did not, because "the same function drawn twice" is a
better *argument* than "two implementations of one bar".

It is not a better outcome. **A duplicated thing that works beats a shared thing that does not, and
the time to find that out is one attempt, not three.**

The duplication is real and it will drift — if the keyboard's bar changes, this one will not follow.
That is now a known, written-down cost rather than a hidden one, and it is the cheaper of the two.

---

# 131. The badge becomes a switch, and the note carries its own colour

## Two steps, not one

The badge swapped the language **and** sent the audio back up in the same tap. That sounded
efficient and it is wrong: the tag could not be looked at or corrected without spending a
transcription, and a mis-tap cost a call to the provider.

**The badge now only changes what the tag says. Re-Transcribe sends, in whatever the badge shows.**

**A switch that also acts is two controls wearing one coat.**

The choice lives beside the row rather than in the entry, because it is a decision about the *next*
send and not a fact about the recording: until Re-Transcribe is pressed, the recording is still
exactly what it always was.

## Delete had fallen off the row

It was in a stale second copy of `HistoryPanelRow` that an earlier edit of mine left behind — and
removing that revealed a second duplicate, `MaDeleteChooser`, which then failed the build with
"conflicting overloads".

**Kotlin only complains when something calls the ambiguous name**, so a duplicate that nothing calls
yet sits there silently until an edit wakes it up. `verify.py` checked duplicate properties but not
duplicate functions; it does both now, **keyed by name and parameter list** — comparing names alone
raised seventy complaints about legitimate overloads, which is how a checker gets ignored.

## The note carries the colour, not its title

The colour was on the title, so a note whose title failed to generate looked exactly like every other
entry — **the very case where recognising it matters most.** The transcript itself is teal now, so an
offline note is identifiable whether or not the model ever named it.

---

# 132. Bullets, and Delete all at the top

## Why Delete kept disappearing

`Arrangement.SpaceBetween` **spaces children; it does not shrink them.** When the four words were
wider than the row, the last one — Delete — simply left the screen. It happened twice, and neither
time did anything report it: **an off-screen child is not an error.**

The actions are one centred line joined by `·` now. Words plus two characters each, no arrangement to
overflow, nothing to clip. And it reads as what it is: a short list of things he can do to this
recording.

`Insert · [ENG] · Re-Transcribe · Delete`

## Delete all belongs on the list

It was reachable only per entry, so clearing a long history meant pressing Delete once per recording.
**An action whose scope is the list should live on the list**, not be assembled out of repeated
single actions.

At the top, in the header, red, and it asks first — everything else here can be undone or
re-transcribed; this cannot, and it sits two taps from what he came to do.

---

# 133. Two real causes, after four cosmetic attempts

He asked why this is going slowly. The answer is in this entry: **I changed the appearance four times
without once checking what the components actually were.**

## Delete kept leaving the screen because these were BUTTONS

`MaHistoryAction` wrapped every label in `SnyggButton` — a Material button, with a **minimum width
and its own internal padding.** Measured: the four labels as buttons need about **419dp** in a row
about **360dp** wide. They never fitted, in any arrangement.

I tried weights, then `SpaceBetween`, then bullets, then centring. **All four were arrangements, and
no arrangement shrinks a child.** As clickable text the same four labels come to 300dp and fit with
room over.

## Delete all crashed because a dialog needs an Activity

A Compose `AlertDialog` opens a real platform Dialog, and a dialog needs a window token from an
Activity. **An input method has no Activity** — its window belongs to the system — so the first tap
took the keyboard down.

`MaDeleteChooser`, ten lines away, was already inline for exactly this reason. **I put a dialog next
to a working example of why not to.**

## The rule, since he asked

**When a screen already solves a problem, copy that solution; do not reach for the standard
component.** And when a fix does not work, the next attempt should change *kind*, not degree — four
attempts at arranging a row that could not be arranged is three more than the evidence justified.

---

# 133. EN in the circle, and the deletes tested properly

## EN, not ENG

Three letters do not fit inside a round key and the last one was being cut at the edge. Two letters
fit, and HR is already two — so the pair is even, and **a badge whose width changed with the language
would shift every key beside it each time he switched.**

The history panel keeps ENG, where the row is wide and the extra letter costs nothing.

## The deletes, put through the four tests

**Test 1, the logic alone.** Eleven cases against a model of the three deletions, holding one
invariant: *no row may point at a file that is gone.* Delete, audio-only, an entry with no audio, a
file delete that fails, clear-all with an undeletable directory, deleting twice. **0 failures** — the
order in the store is already right, file first and row second.

**Test 3, the ugly cases** — and this is where the real faults were, after the dialog crash was
already fixed:

- **`clearAll` ran on the main thread.** It walks the audio directory and deletes every file in it.
  With a few hundred recordings that blocks the keyboard long enough for Android to kill the input
  method for not responding. The other two were already on IO; this one was not.
- **None of the three was wrapped.** A delete can throw — a file held open by a media scanner, a
  revoked permission — and **an exception escaping a launched coroutine takes the keyboard down.** He
  asked for delete that does not crash; the crash is not always in the dialog.

All three are on IO and guarded now, and a failure writes a line naming what threw.

## Not fixed, and worth knowing

**A row whose audio file was removed from outside the app** — storage cleared, files pruned — still
shows Re-Transcribe, and pressing it will fail. The row is not corrupt and nothing crashes; the
button is simply offering something that cannot happen. Fixing it means checking the file exists when
the row is drawn, which is disk work in a list, so it needs doing carefully rather than quickly.

---

# 134. A row that lost its audio now says so

A row records where a recording was put; it cannot know when something else removes it. Clearing the
app's storage, a restore that carried the database but not the audio, an OS pruning files — each
leaves a row insisting it has audio that is gone, and the list then offers **Re-Transcribe on a file
that cannot be read.**

## Repair, not hide

The quick fix was to check the file while drawing each row and quietly not offer the button. That is
**disk work inside a scrolling list**, it runs again on every recomposition, and it leaves the
database still lying — the same wrong answer handed to every other reader.

`repairMissingAudio` writes the truth once instead: any row whose file is missing has its `audioPath`
cleared. **The transcript is untouched — losing a file is not a reason to lose the words.**

## Three things that make it safe

**It cannot loop.** A row is only written when the path is set *and* the file is missing; afterwards
the path is null, so the next pass skips it. The write triggers one more emission of the list, and
that emission finds nothing to do.

**A failed check leaves the row alone.** `exists()` throwing — a revoked permission, an unmounted
volume — returns *true* by default, so an unreadable check can never destroy a good pointer. **Only a
definite answer is acted on.**

**Once per opening, not per row.** `LaunchedEffect(Unit)` on IO, in both the keyboard panel and the
settings screen, because they read the same rows and a fix in one that leaves the other lying is not
a fix.

It says nothing when nothing was wrong, which is every ordinary open.

## Tested

Seven cases: no repairs when every file is present, exactly one when one is missing, **the second
pass doing nothing** (the convergence property), all-missing then stable, rows without audio skipped,
a throwing check leaving the row intact, and the transcript surviving in every case. 0 failures.

---

# 135. The bar moves to the top, and the finger learns to tap

## The bar was trapping him

At the bottom it sat over the navigation bar, so while recording he could not go back or switch
apps — and **switching apps mid-recording is exactly what he does.** An indicator that traps you is
worse than no indicator.

It is at the top now. That strip is free: the bar only appears when the keyboard is down, so nothing
of his is up there.

## Gemini's send button, and why ACTION_CLICK was not enough

His dump shows the tree is fine — a clickable `View` wrapping a `Send` label, one hop up from the
match. The finder found it. **The press was refused.**

Compose builds its accessibility nodes by hand, and a node that reports `isClickable` does not always
accept `ACTION_CLICK`. The action returns false and nothing happens, which from outside is a magic
finger that does not work in that app.

**A dispatched tap at the centre of the bounds is the fallback** — fifty milliseconds down and up,
which is what a finger does and works on anything genuinely on screen.

**Second, not first.** `ACTION_CLICK` is precise, survives an element moving between the look and the
press, and does not care what is drawn on top. **The gesture is the fallback because it is the
blunter instrument, not because it is worse.**

It logs when it falls back, so the next time an app needs it there is a record rather than a guess.

---

# 136. The helper bar's looks, and nothing else

**Look only. No engine.** Every action on the bar is the same call it was — cancel, mic, cycle
language, show keyboard — and the tests from earlier still describe it. He asked for that explicitly
and he was right to: the last time I changed how it worked it broke in a thousand pieces.

## What changed

**A frame, not a weighted row**, so the clock is at the true centre of the *screen*. Weighted spacers
centre the middle group only when the things either side weigh the same — ENG plus a bin against one
arrow, which they do not — so the numbers sat slightly off. **Off-centre is the kind of wrong the eye
reports without being able to name.**

**The meter moved above the clock.** This bar lives at the top of the screen; putting the level under
the numbers pushes it toward the middle of his view, and something glanced at belongs at the edge
with everything else that is only glanced at.

**No cog.** Settings are not reached for mid-recording, and its room went to the send arrow, now the
biggest control on the bar.

**Send is amber** — the same amber as the meter's headroom. On this bar send means *into the archive*
rather than into a text field, and the colour is the only difference between the two recorders he
needs to see.

**Fully opaque black.** It sits over the status bar, and a translucent strip there is two layers of
information competing in one place.

**The dot is centred on the digits**, in both bars. In the keyboard's meter the row is bottom-aligned
so the megabyte figure tucks under the clock — right for a small number beside a large one — but a
lamp is not a number, and hanging it from the same line made it read as having slipped. **Only the
dot was re-aligned; everything else kept its arrangement.**

---

# 137. The bar becomes the status bar, and the note finally shows

## It replaces the status bar rather than sitting on it

Exactly `status_bar_height` tall, opaque, at the top. **It occupies a strip that was never his**:
nothing of the app moves, nothing of his is hidden, and the row he loses is one he was not reading.
The old two-line bar covered the clock *and* took a band of the app underneath.

**One line, so the meter stands up.** A horizontal meter needs a run of width and a line of its own;
upright it needs the height it already has. Same numbers, same colours, same peak — and upright it
fills from the **bottom**, because level rising is level going up, which is the one direction a meter
is allowed to grow.

Left to right: meter · dot · clock · — · bin · send · **ENG last**, because the language is the one
thing here he changes rather than watches.

Send still stops and transcribes. **The destination is decided by what is on screen, not by this
being a different button** — with no field open it lands in the archive, and when he opens the
keyboard mid-recording the keyboard's own bar picks it up, because both read one state.

## Why the offline note never showed, three times

`deliveredToField` was set in **one** of the two delivery branches. The realtime path left it at
whatever the previous dictation happened to be, so a note dictated with no field open inherited a
`true` from earlier and was filed as ordinary.

**A flag written on one path and read on both is not a flag, it is a memory of somewhere else.**

## The colour, harmonious

Teal was a contrast colour and he asked for harmony. The list is sand and amber on near-black, and a
cyan line in it reads as an error or a link — **a different SHADE of the same hue says "another kind
of the same thing"; a different hue says "something else has gone wrong".**

`#C9A227`, a deeper gold. Measured at **7.8:1** against the panel, comfortably past the 4.5:1 needed
for body text, and darker than the accent so the two never compete: amber marks what can be pressed,
this marks what was never sent.

## The sweep, all three passes

**Structure:** one hit, false — `SpaceBar` inside a comment explaining why that icon is not used.
**Agreement:** 30 feature keys, 20 settings entries, 11 switchboard entries, 310 preferences, 15
routes — **0 disagreements, and every count printed**, because a check reporting zero is a check that
did not run.
**Dead ends:** reader never silent from any state; volume 4,000 hold lengths with 0 doing both and 0
doing nothing; send never fires unasked and never stays armed; offline classification correct on all
four combinations; repair converges on the second pass.

---

# 138. The notch

He is right that an overlay cannot draw over Android's own status bar — that strip belongs to the
system, and what my window took was the row *beneath* it. Moving the bar to the top therefore moved
the problem: the app's own top-corner buttons were now the ones covered.

**The window is only as wide as its contents and sits in the middle.**

A window is either touchable or it is not — there is no per-region setting, and making the whole
strip pass touches through would have taken the bin, the send and the language with it.

So: **the corners are not click-through because they let touches pass. They are click-through because
there is nothing there.** Like the notch on a phone — the middle is spoken for, the sides are his,
and app buttons live in the corners.

Which is also why the controls are now packed with no gaps between them: **every dp of width this
takes is a dp of his screen that stops working.**

---

# 139. One copy row, and a beat before Send

## The clash

The transcription view had **two clipboard rows**: the old edit strip at the top and the arrangeable
copy row below it — different orders, different settings, neither aware of the other. He could not
tell which one his arrangement applied to, because the answer was "one of them".

**Two controls for one job is not a choice, it is a bug with an options screen.**

The copy row is the master — it is the one the editor arranges — so it **takes the strip's position at
the top** and the strip is removed from that view. The strip stays on the typing keyboard, where it is
not duplicated and `maEditRow` still switches it.

**In the transcription view the copy row is always on**, not a switch: that view has no letters and
the clipboard is its whole job, so a switch offering to remove the only row on the screen is offering
to break it. On the typing keyboard it stays switchable, where it is one row among several.

## 333 ms before pressing Send

The gesture works every time in Claude and sometimes in Gemini. **The delay is not for a race in this
app — it is politeness toward one in theirs.** Gemini's send button enables itself once the field
reports content, and a press landing in the same frame as the text arrives at a button that is still
disabled. Nothing throws; the message just sits there.

Nothing here can observe the other side becoming ready, so a wait is the only instrument available. A
third of a second is under the threshold where he would call it slow and well over the frame or two
the other app needs.

---

# 140. Permissions, numbered, and warnings that open the fix

## A screen of its own, first in the settings

Permissions were spread across a wizard he sees once and a scatter of errors he sees when something
has already failed. Reinstalling — several times a day — meant hunting them one at a time from
different starting points.

**Numbered, because the order is not arbitrary.** Restricted settings must be allowed before
accessibility can be switched on; the microphone is useless without the keyboard enabled. A list in
execution order is one he can work down without thinking, which is the point of gathering it.

**Each row says whether it is granted and opens the exact page that grants it.** A permission screen
that only names permissions is a worse version of the system settings — the value is entirely in
those two things.

**It rechecks on resume.** A grant happens in another app, so this screen is always holding a stale
answer after a trip out. Without the recheck a granted permission keeps reading as missing until the
screen is closed and reopened, which looks like the grant not having worked.

**"Allow restricted settings" reports as never granted**, because Android exposes no flag for it. An
unticked step he has already done costs a glance; a ticked one he has not done costs an afternoon.

## The warning is the door

`grant the microphone permission` said what was wrong and left him to find where — from a keyboard,
which cannot show a permission dialog, so the answer was always several screens away.

**An error that names a fix and does not offer it is only half an error message.** Any failure
mentioning a permission, AudioRecord or accessibility now opens the Permissions screen.

Matched on the message text rather than an error kind, because these arrive from the recorder, the
service and the system, and none of them share an enum. **Wrong in the harmless direction**: a false
positive opens a useful screen, a false negative leaves the banner as it was.

## Caught before building

`Icons.Default.Lock` is **absent** from the icon artifact this project builds against — a missing
icon is a red build, not a blank space. Shield instead. And `FlorisImeService` was used in the
smartbar without being imported.

---

# 141. The permissions screen, fixed

Two bugs, both mine, both shipped in 257.

## The crash

I used `androidx.compose.ui.platform.LocalLifecycleOwner`. **Every other screen in this project uses
`androidx.lifecycle.compose.LocalLifecycleOwner`** — the Compose one is deprecated and throws where
the lifecycle one resolves. It compiled, so nothing said a word until the screen was opened.

**The project already had the right pattern in `DictateFloatingButtonScreen` and I wrote a new one
instead.** When a screen needs to do something another screen already does, copy that screen.

And the observer was added inside `LaunchedEffect` and never removed — it outlived the screen and
fired against a composition that was gone. `DisposableEffect` with `onDispose` now, as the working
screen does.

## Every query is wrapped

Seven `runCatching`s, each defaulting to **not granted**. These are queries into the system and any
of them can throw on a ROM that answers differently. **One screen that cannot open is worse than one
row that says the wrong thing**, and the safe default leaves the step visible with the button that
fixes it.

## Permissions is pinned to the top

Appending new entries is the right rule — it is what stops a feature added next year being invisible
to somebody whose arrangement predates it. But it put this one at the bottom of a long list, and
**this is the screen he reaches for when nothing else works**. A repair tool at the end of the list is
one he has to scroll to while the thing is broken.

Pinned rather than reordered once, so it survives any drag and any restored backup. Tested against
five stored orders — empty, an old order without it, one where he dragged it to the middle, one with
an unknown id, one with it stored last: first every time, every entry present exactly once.

---

# 142. Permissions and API keys, one entry

**A permission and a key are the same kind of thing from where he stands:** something that must be
granted before the app can do its job, and something he has to set up again after a reinstall. They
fail together and they were fixed in two different places.

First entry in the settings, **"Permissions and API keys"** — the numbered grants, then a Keys
section that opens the key manager.

**The keys screen stays its own screen.** It is long: the ring, the tester, the importer. Nesting its
body inside another screen would mean unpicking four hundred lines to gain nothing he can see.
**Merging the entry is what he asked for; merging the implementation would have been me hearing a
different request.**

**Its row is never ticked**, and that is deliberate. A key is not a yes-or-no — it can be present and
dead, or present for one provider and missing for another. A tick claiming "done" is the kind of
false reassurance that costs an afternoon.

## The old entry is kept, and hidden

`KEYS` still exists in the enum so a stored order containing `keys` parses exactly as written rather
than being silently repaired. It is filtered out of the list wherever it appears: **an entry that
opens the same screen from two places is two doors to one room.**

Tested against five stored orders — fresh, an old one that lists `keys`, one with it dragged to the
middle, one listing it twice, one with an unknown id: permissions first every time, `keys` never
shown, every other entry present exactly once.

---

# 143. His settings become the defaults

He sent his exported `.jetpref`. **It contained his live AssemblyAI ring — 23 key-shaped values — so
none of it was copied verbatim.** The preferences were read, the accounts blob was skipped, and no
key was printed at any point. See `SECRETS.md` §2a.

The non-secret ones are now what ships:

| | was | now |
|---|---|---|
| number row on the feature row | off | **on** |
| edit strip | on | **off** |
| copy buckets | on | **off** |
| scroll pages | 1 | **4** |
| keyboard number row | on | **off** |
| settings order | alphabetical default | **his** |
| feature row arrangement | default | **his** — zone keys, mic, reader, switchboard, settings |

**A fresh install should look like the keyboard he uses**, not like a starting point he rebuilds every
time — and he reinstalls several times a day.

## Permissions cannot be moved

`parse` already pinned it first, so a drag appeared to work and then silently undid itself on reopen.
**A control that accepts a gesture and then discards it is worse than one that refuses**, because it
teaches him the setting does not stick rather than that it is fixed.

`move` now refuses to move it and refuses to drop anything above it. Four cases tested.

## The copy row's position is fixed

Above the record button, below the language badge, in the transcription view, **and not a setting**.

It was under the record row and switchable. Both are wrong: that view has no letter keys, so
select-all, paste, cut and the histories are most of what the screen is *for* — **a switch offering to
remove them is offering to empty the screen** — and its place is where his thumb already is between
speaking and pasting.

---

# 144. A settings export is a secret file

He sent a `.jetpref` to set the shipped defaults from. It held **twenty-three key-shaped values** —
his live AssemblyAI ring — inside a `provider_accounts` blob, among sixty ordinary preferences.

**The secret was not the point of the file, which is exactly what makes it dangerous.** A key file
announces itself. A settings export looks like configuration.

What was actually done, and what should always be done: read in place, values taken by name, the
accounts blob skipped, nothing copied into the working tree, nothing printed. Verified afterwards —
no 32-hex value anywhere in the repo except one inherited FlorisBoard file from June, no `.jetpref`
in any commit.

**Take the values, never the file.** A shipped default is a number written into `AppPrefs`; the file
it came from is not something this repository should ever have held.

Written into `MANTRA_MANIFEST/modules/secrets.md` §2c and into the handoff, with the general rule
underneath it: **anything he sends to configure something is a file about his setup, and a file about
his setup contains whatever his setup contains.** Ask what is in it before deciding how to treat it,
and assume the answer is "secrets".

---

# 145. The pin comes off

I pinned PERMISSIONS to the top — forced there on every read, and refused by `move`. He rejected it,
and the distinction he drew is the right one:

**A default is where something starts. A pin is a decision taken away from him.**

This list is his. **The one entry he was not allowed to move would have been the one proving it is
not his** — and the reason I gave (it is the screen he needs when things are broken) is an argument
for putting it first, not for holding it there.

It is first in `DEFAULT`, so a fresh install opens with it at the top. After that it is an entry like
any other: drag it anywhere, and it stays where it is put.

`parse` no longer reorders, `move` no longer refuses. Nine cases: first on a fresh install; moved to
the middle and it stays; that order survives a reload; moved to the end and it stays; another entry
can be dropped above it; and across four stored orders nothing is lost or duplicated, with `keys`
still filtered out.

**The lesson worth keeping:** when a rule protects him from himself, check that he asked to be
protected. He did not, and the version that trusted him was both simpler and correct.

---

# 146. One copy row, for real this time

## What §139 left behind

§139 unified the transcription view and said the strip could stay on the typing keyboard, "where it
is not duplicated". That sentence was true of the screen and false of the app. It left **two rows
that were both called the copy row**:

| | transcription view | typing keyboard |
|---|---|---|
| composable | `MaFeatureRow(copyRowOnly = true)` | `LegacyEditRow` |
| preference | `maCopyRow` | `legacyActionRow` |
| key set | `MaFeatureKey` | `LegacyEditAction` |
| editor | Copy row screen | `LegacyActionRowSetting` |

Marko sent two screenshots and said: both come from one source of truth, yet the last button differs,
so delete whatever makes it differ. The last key on the keyboard was `LegacyEditAction.KEYBOARD`,
which draws a **microphone** in the typing view because it means "swap views" — a key that does not
exist in the other row's vocabulary at all. The fourth and sixth keys differed for the same reason.

**He was right about the symptom and the cause was worse than the one he named.** They were never one
source. And the comment above the call site said, in as many words, that both views drew the identical
row from identical code. **A comment is not evidence** — the same lesson the spacebar mark taught,
paid for a second time in the same file.

## What it is now

One row, one preference, one composable, one editor.

- The typing keyboard draws `MaFeatureRow(copyRowOnly = true)` in the slot the strip had.
- `maEditRow` still switches it there — **the copy-row key on the feature row, key 3, the key he
  already presses.** Not a new switch and not a settings-screen tick three rooms away.
- The transcription view is unchanged: always on, no switch, by §139.
- `LegacyEditRow` is deleted. `LegacyActionKey` and the `LegacyEditAction` enum stay, because the
  feature row draws AP, AC, select-all and backspace through them — the same keys from the same code.
- `LegacyActionRowSetting` is deleted. It arranged a row that now appears nowhere, and **an editor for
  a row that does not exist is a control that does nothing**, which this project has repeatedly found
  to be worse than a missing one.

## The two switches that were not switches

`maCopyRowOnKeyboard` switched an **appended second copy** of the real copy row onto the keyboard,
while `maEditRow` switched the strip. So the keyboard had two clipboard rows available under two
switches, and the tick labelled "on the typing keyboard" did not control the row that was on the
typing keyboard.

`maCopyRowOnDictate` controlled **nothing at all**. Nothing read it after §139 made that view fixed.
It sat in the switchboard and on the copy row screen looking like a working control.

Both preferences are gone, both ticks are gone, and `COPY_KEYBOARD` / `COPY_DICTATION` are gone from
the switchboard. `MaSwitchboardOrder.parse` drops ids it does not recognise, so an arrangement written
before this loses those two and keeps everything else — no migration needed, by that design. The
remaining entry keeps the id `edit_row` (an id is a name in a file, not a label) and is now called
**Copy row**, with `ContentPaste`, the glyph key 3 draws.

## `drawChrome`

The keyboard now hosts **two** `MaFeatureRow` instances: the feature rows at the bottom and the copy
row at the top. `maDashboardOpen` is file-level state and `maMagicRowShown` is a preference, so both
instances would have drawn the wand bar, the magic row and the reader dashboard — two of each,
stacked. The second instance passes `drawChrome = false`.

The scroll stepper is deliberately **not** gated: `scrollMenu` is local to each instance, so it opens
over the row whose key was actually held.

## Rejected

**Putting the copy row at the bottom of the keyboard** by leaving the append path in and pointing key
3 at it. It would have been a smaller diff and it moves a row he did not ask to move.

**Keeping the two preferences as deprecated.** A preference nothing reads is indistinguishable from
one that is broken, and this whole entry exists because of controls that looked like they worked.

## Tested

`scripts/test_copy_row.py`, 838 checks, Test 1, no build required: one reader of `maCopyRow`, both
views calling the same composable with `copyRowOnly`, no surviving `LegacyEditRow` caller, no appended
second copy, no reader of either dead preference, the keyboard switched by `maEditRow` and the
transcription view switched by nothing, chrome gated. Made to fail on purpose twice — the appended row
grown back, and `drawChrome = true` — and it went red for each.

**Not tested:** anything visual. Heights, balance and the row's position on the real keyboard are code
inspection only until it is on his phone.

## The check that cried wolf

`verify.py` reported `MaSwitchRow` "declared twice in the same scope" in a file that has compiled for
months. It was a **false positive**, and confirmed against the source before anything was changed:
`MaSwitchRow(pref: PreferenceData<Boolean>, …)` and `MaSwitchRow(stringPref: PreferenceData<String>,
onValue, offValue, …)` are legal overloads.

The check already keyed on the parameter list for exactly this reason — but it read only the declaring
line, so every **wrapped** signature, which is every Compose signature in this project, hashed as the
empty string and collided with its own overload. It only fired now because the file had never been in
a diff since the check was written.

Fixed by gathering the parameter list until its parens balance. Verified by putting a genuine
duplicate in — identical signature, same scope — and confirming it still goes red.

**A checker that cries wolf is a checker that gets ignored**, and it had one chance to be believed.

---

# §147 — The buckets lose their switch, and three detections stop guessing

Build 265. Four things he photographed on 21.8.2026, all of them the same shape underneath: a
feature that was working exactly as written and reading, from the phone, as broken.

## The buckets were off, and that is why A was off too

`captureIntoClipSlots` opened with `if (!prefs.dictate.maBucketsEnabled.get()) return`, and that
preference defaults to **false**. So on his phone the C keys were drawn on the row, dim, catching
nothing — and the A key, which presses "copy code" on a code block and lets the ordinary clipboard
capture put it away, had nowhere to put anything. He reported the A key as broken. It was not. The
bucket behind it was closed.

**The switch is gone, and presence on the row replaces it.** C keys on a row means the buckets are
live; no C keys means there is nowhere for a copy to go, which the existing `visible` set already
enforces without any preference at all — `capture` returns the buckets unchanged when the visible set
has no free slot, and an empty set has no free slot.

Why this is better than fixing the default: **a switch that can be left in the position where the
feature looks broken is worth less than no switch.** The gesture that makes buckets wanted is putting
a C key on the row, and that gesture can now do the whole job. Nothing to find in a settings list
three screens away, and no state in which the keys are on the keyboard and inert.

Removed with it: `maBucketsEnabled`, the switchboard entry `BUCKETS`, and the switch at the top of
the Paste timing screen. `MaSwitchboardOrder.parse` drops the stored id, so his arrangement survives
minus that row.

**A1 fills C1, A2 fills C2, A3 fills C3** — and always did, by construction rather than by a rule
written anywhere: A presses the *n*th code block's copy button, the copy reaches the clipboard, and
capture pours it into the lowest empty visible bucket. Modelled in `scripts/test_buckets_and_
permissions.py` and walked, along with the cases it must refuse.

## The tick that vanishes belongs to somebody else

He asked for the "copied" checkmark to stay a minute. That checkmark is drawn by the chat app on its
own copy button, and nothing in this keyboard can hold it there — worth saying plainly rather than
appearing to fix it.

So the same question is answered on our own key. `MaClipCapture.lastFilled` records which bucket took
the last copy and when; the C key wears a small tick for `FILL_MARK_MS`, one minute, and then stops.
Compose state rather than a plain field, because the capture happens in the clipboard manager with
the keyboard already drawn — a plain field would be written and nothing would redraw, which is the
wand bar's old bug. A `LaunchedEffect` ticks once a second only while a mark is young, so the tick
clears itself and costs nothing the rest of the day.

The tick is a **shape**, not a colour: colour on that key already says whether the bucket is holding
something, and a second meaning on the same channel would make neither readable. It is drawn over the
key in a Box so the number does not move to make room for a mark that comes and goes.

Which bucket is found by **diffing the two lists** rather than by having `capture` report it. capture
stays a pure function over a list, and the difference between what was stored and what is stored now
cannot disagree with what was actually written.

## One source is not a detection

"Enable the keyboard" said **not done** on a phone where the keyboard was enabled and in use. It
asked one question — does `Settings.Secure.ENABLED_INPUT_METHODS` contain our package name — and that
string is not reliably readable by an ordinary app on a modern Android. It comes back null, and a
null contains nothing, so the answer was no.

It asks three now and takes any yes: `InputMethodManager.enabledInputMethodList` first, since that is
the public API for exactly this question; the Secure string second; `DEFAULT_INPUT_METHOD` third,
because nothing can be the current keyboard without being enabled. **Each is wrapped on its own.**
One `runCatching` around all three would have let the first failure discard the other two answers,
which is the same fault in a different place.

**Allow restricted settings** was hardcoded `false` and could never tick. Android exposes no flag for
it, so it is now answered by its consequence: restricted settings is the gate that stops a sideloaded
app's accessibility service being switched on at all, so an accessibility service that is *running*
is proof the gate was opened. Not an assumption — a thing that could not otherwise have happened.
Still false when accessibility is off, which is honest: the next step is the same either way.

A row that is permanently outstanding on a screen whose whole job is to say what is left teaches him
to ignore the numbers, and then the ones that mean something are ignored too.

The API keys row below the steps is still deliberately never ticked. A key can be present and dead.
The first version of the test could not tell that apart from the bug and would have had to be argued
with; it now looks only inside `maPermissionSteps`.

## The megabytes broke a line

`"%.1f"` in a column sized by `Modifier.weight(1f)`. The clock takes what it takes and the size gets
the remainder, so at 15,0 it was narrower than the text and wrapped onto a second line inside a strip
one line tall. **A weight is a promise about width that a number cannot keep** — the same rule that
clipped "Insert" to "Ins", in digits.

The lamp, the clock and the size now start at the left edge, where the meter below them starts, and
the single weight is at the *end* taking up what is left. The size says `MB`, `maxLines = 1`,
`softWrap = false`, sized to its own content.

The decimal separator stays the phone's own. `"%.1f"` gives 5,6 on his Croatian phone and that is
correct there; forcing a full stop would make this keyboard the only thing on his screen writing
numbers the other way.

## Tested

Test 1: 438 checks, 0 failed, including the capture rule modelled and walked in Python. Broken on
purpose four ways and confirmed red, then green again. One of those sabotage runs made a check
*throw* rather than fail — the count never printed and the exit code was right by accident — and that
check is now guarded. A check that raises is not a check that fails.

Not tested: nothing ran on a phone. The A key's press path through the accessibility service was not
exercised, the three permission sources were not observed on his ROM, and the tick has not been seen.
