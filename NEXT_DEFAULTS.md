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
