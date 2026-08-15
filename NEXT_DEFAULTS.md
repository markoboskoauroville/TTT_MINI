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

**Do not rebuild this.** Marko's instruction is that anything playing keeps playing, the way it does
in a browser, and that no code should exist which reacts to what another app is doing with audio.
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
