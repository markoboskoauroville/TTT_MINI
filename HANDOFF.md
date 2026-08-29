# TTT mini — where things stand

Last updated at **build 261**. Read this first. Read `NEXT_DEFAULTS.md` only when you need the
reasoning behind one particular decision — it is a 4,000-line log, not a briefing.

## What to read, and in what order

1. **This file.** Everything still true about this project.
2. **`MANTRA_MANIFEST`** (`markoboskoauroville/MANTRA_MANIFEST`) — the principles that hold across all
   his apps: the four tests, secrets, the design language, the key ring. Start at its `START_HERE.md`,
   which says which of its files this project needs and which to skip.
3. **`VOLUME_KEYS.md`** before touching the volume keys. It is a contract, not a description: if the
   code differs from it, the code is wrong.
4. **`SECRETS.md`** before touching a key file. Note §2a — a redaction built from known key shapes
   fails *open* on a format it has not seen, and that leaked a key.
5. **`NEXT_DEFAULTS.md`** only to look one decision up. It is an archive of 130+ entries; several
   early ones describe features that have since been removed, and later entries override earlier.
   `NEXT_DEFAULTS_INDEX.md` lists them all so it can be searched without being read.

**Run `python3 scripts/verify.py` before every push.** Details below.

## Where it is now

**Build 318.** The last stretch, newest first:

- **Two automatic buckets.** `A↑` takes the lowest code block in the frame and works upward, `A↓`
  takes the highest and works downward. One body, one range reversed — everything else, the skip
  rule and the three messages, is shared. The old `A1` face is gone; it was a leftover from the
  ladder.

- **The lowercase style option is gone — it should never have existed.** `prose-voice.md` §3 says
  Yshai's lowercase is *the one thing we deliberately do not copy*, and build 316 turned that note
  into a setting offering to copy it. There is one voice, his, with sentence capitalisation. The
  section states it rather than offering a choice that does not exist.

- **Ctrl+P now corrects a three-word phrase.** The instruction said "if the text is already correct,
  return it unchanged" and a model reading a bare fragment decided a fragment was intended. The new
  rule sits ABOVE that one and names what to do: capital at the front, terminal punctuation.
- **Grammar correction (Mantra)**, under the Ctrl+F prompt: Marko or Yshai. Not two voices — one
  voice with `prose-voice.md`'s single deviation on or off. Your own wording is never modified.
- **Row renaming already shipped in build 313** — the field is above the tabs, labelled *Name this
  row*.

- **Reflow no longer translates.** Every rewording prompt now DETECTS the language of the text,
  NAMES it, and puts that rule LAST — after the style rules and immediately before his text. It was
  the ordering as much as the omission: English style rules after a language rule drag the output
  back into English.
- **The reflow instruction follows `prose-voice.md`** — short paragraphs, the honest counterweight,
  witness rather than assert, one image, the principle before the request, ending by taking pressure
  off. Shape only, never meaning.

- **Rows can be named.** "bucket row", "keyboard row" — typed on the row's own tab, shown on the
  tab, falling back to "Row 3" when blank. The name rides inside the existing META field as
  `1~name`, so an arrangement stored before today reads back unchanged. Separators are **stripped,
  not escaped**.

- **A Cloudflare 403 can no longer bury the key ring.** Every 403 was classified as a bad key, and
  Cloudflare refuses the *client* — so one request would have killed every key at once, recoverable
  only by hand. The branch is first, and a test asserts the ordering.
- **A User-Agent on every request**, from all three clients. The app was sending none, which is what
  provokes that 403 in the first place.
- Audited against `MANTRA_MANIFEST/modules/quota-and-fallback.md`; the audit is §11 of that module.
  **Still open: `Retry-After` is parsed nowhere.**

- **The loop guard moved to `speak`**, where every reading must pass. It was in `continueBelow` and
  the loop survived, which says the loop is on another path — so rather than hunting the caller, the
  check is at the door. A repeat inside one reading now ends with *"Already read this — stopping"*.
- **A filled square marks the head of every passage**, his idea and his fallback: if it appears
  twice he is hearing the same words and can stop it himself. It clears after three words, is drawn
  by every effect, and is never sent to Speechify.

- **The clipboard-history key wears the history arrow**, composed by `MaHistoryGlyph`: the clipboard
  for the subject, the counter-clockwise arrow for the past. It was `ContentPasteGo`, whose arrow
  points right and means *go*. Written up as **design-language §14a** in the manifest — one idea,
  one mark, applied to whatever it is about.

- **The reader stops at the end instead of looping.** It compared each new passage only against the
  one before, and a live screen differs by a character between reads — a clock, a timestamp — so it
  read the same screen forever. Three answers now: the screen must actually have moved, the passage
  must not have been read already *at any point in this reading*, and a ceiling of 30 screens as a
  backstop. Comparison is on letters only, so a ticking clock cannot make one screen look like two.

- **A circle on every clipboard entry pins it.** Looks like a radio button, behaves like nothing of
  the sort: any number filled at once, and filled entries sit in the pinned section at the top. It
  toggles `pinClip`/`unpinClip`, so it is the same pin the long-press popup already had — with the
  lid off.

- **Copy and cut work when the input connection has gone.** Selecting text collapses the keyboard and
  takes the connection with it; the selection is still on screen, so the accessibility service asks
  the view that owns it to `ACTION_COPY`. Second route, tried only after the connection comes back
  empty. The old message about input connections is gone.
- **Copy row: select all · paste · copy · cut · clipboard history · AP · AC · pin.** The pin is on
  this row because selecting is what collapses the keyboard — the key he needs first is now beside
  the keys it enables.

- **The three zone keys follow the ring convention now**: green ring on, cream ring off, glyph never
  changes. They had it backwards — monochrome ring, green glyph — because they were written before
  the convention existed.
- **The copy row is select all · paste · cut · clipboard history · AP · AC.** Transcription history
  is off it, still in the catalogue under Dictation. A stored arrangement is not rewritten, so if
  you have ever edited the copy row, remove the clock key yourself.

- **The feature row's Shift is the letter keyboard's Shift.** It sends `KeyCode.SHIFT` down and up
  through `inputEventDispatcher` instead of writing `inputShiftState` itself, so double-tap lock,
  auto-shift, and the recapitalise-a-selection behaviour all arrive for free. Locked wears the
  capslock glyph; armed is lit; off is plain.

- **Tabs are dragged to reorder the rows.** Long press, then slide; a plain tap still selects. The
  "Row 2 becomes Row 1" buttons are gone — at six rows that was five buttons per tab. The drag
  **moves** where the buttons swapped, because a drag depicts sliding in front of the others.
  `MaRows.moveRow`; `swapRows` stays, tested, no longer called from the editor.

- **Six feature rows**, up from three, and **F4/F5/F6** to go with F1–F3. The upgrade is free
  because `parse` has always padded a short read: his three rows come back as rows one to three and
  the new ones arrive empty and switched off. **Lowering `ROW_COUNT` would delete the rows beyond
  it** — noted at the constant.
- **Four arrow keys**, left/right/up/down, sending the letter keyboard's own key codes through
  `keyboardManager.tapKey`, so long-press repeat and shift-selection behave exactly as they do there.

- **The delivery gate is in CI.** Every action pinned by commit SHA (the one blocking finding of the
  nine gates); `verify.py` and all 18 Test 1 suites blocking; the built APK scanned for key shapes
  and blocking; the loop count recorded. **Android Lint runs but does not block yet** — it has never
  run on a release of this app (`-x lintVitalRelease`), and it is being measured before it is made
  fatal, on purpose, per delivery-gate §5.2.

- **The Trim silent gaps switch reports what it saved**: "Last dictation: 34% less audio uploaded",
  under the switch, in Settings → Dictation. The switch itself has been there all along, default on —
  and saying nothing is how it sat at a threshold that saved nothing for months.

- **Silence trimming tightened for cost: 2000/400 → 700/250.** Only pauses over two seconds were
  being cut, which is almost none of a real dictation. Every trim now logs what percentage it saved.
  **Open and not done: async still defaults to `universal-3-pro`** and offers `universal-2`, neither
  of which is on his price list; and `longform` skips trimming entirely, which exempts the longest
  and most expensive recordings.

- **F1, F2, F3 keys** show and hide feature rows 1, 2 and 3 from the keyboard, each wearing the green
  ring while its row is showing. Written once for the three, and they write through
  `MaRows.setRowEnabled`.
- **A row can become another row.** "Row 2 becomes → Row 1" on the editor's tab, a **swap** and never
  an insert: three rows exist always, and a move that shuffled the others would renumber a row he
  never touched. Keys, arrangement and on/off state travel together; the tab follows the keys.

- **`[ENG] [×] [⠹ status…]`** — brackets around each part and 12dp between them, because two of the
  three are irreversible in opposite directions and a thumb aimed at hold was landing on X.
- **The badge was read once, so it never changed.** `MaLanguage.badge()` is a plain call; Compose
  had no way to know the preference behind it moved. It is derived from an observed
  `maLanguageMode` now.
- **Releasing a hold re-ran the speech gate on already-trimmed audio**, which judged it silent and
  raised an error — a tap that should have sent looked like a cancel. Release passes `gate = false`,
  as the history replay always has, and the hold keeps its own copy of the audio because the trimmer
  rewrites the original under it.

- **The sending line is laid out like the recording bar**: `ENG · X · ⠹ status…`. The badge is where
  it is in the recorder, because he reaches for it without looking. The bin's position becomes X —
  same place, same job, and a letter rather than a glyph because this line is machine output.
- **Tapping the middle holds the send**, and tapping again sends. A hold is a cancel that keeps the
  tape: `Transcribing(held = true)`, request thrown away, audio kept. The badge sets the language
  while held and does *not* send, so he can stop, choose, then send.

- **The language badge is on the sending line, and tapping it resends.** Same badge as the history
  rows. A tap cycles the language, cancels the request in flight and sends the same audio again —
  `DictateController.retranscribeInLanguage`, with the audio and its metadata held in `inFlight`
  for exactly as long as the request is.

- **The text holds between chunks.** The last page stays up until the next is ready — a slideshow,
  where the change is the only event. Build 286 stopped the window disappearing; the text still
  emptied, which is the same fault one layer in.
- **Top, middle or bottom**, beside the highlight swatch, with no animation. Where the reading sits
  is a question for every effect, so the **"Top line" effect is gone** — promoted to
  `maReaderAlign`, not deleted.
- **The effect chips are abbreviations** — Hi, Ty, Kar, Sp, Vo, Mx — sized to a key on the feature
  row, so all six fit one row with nothing behind a scroll.

- **The recording overlay is one hairline.** Full width, one device pixel, at the top, and
  `FLAG_NOT_TOUCHABLE` — the meter and nothing else. The notch carried a meter, a red dot, a clock, a
  bin, a send and the language badge; every control on it had a better home already, and with none
  left the window can refuse touches entirely, which the notch never could.

- **The English words under a Croatian badge came from the shipped dictionary, not the n-gram.** A
  suggestion provider whose locale disagrees with the badge is now dropped entirely, not ranked
  lower. An empty row is more honest than a wrong-language one.
- **A language gate on every word learned.** Known to a model → already right, costs nothing. Carries
  č ć ž š đ → Croatian, costs nothing. New and unmarked → learned under the badge and queued, and a
  batch of up to 60 is sorted by Haiku when he presses **Sort new words by language**. Never on the
  typing path.
- **The n-gram setting shows both models** — words known and words read, per language — with a wipe
  each and the number of words waiting to be sorted.

- **The reading window no longer blinks.** It stays for as long as the reader is anything but idle
  and draws empty when there is nothing to show. Chunked fetching exposed the old rule — a moment
  with no current word between chunks made the whole box vanish and return, several times a passage.
- **Top line**, a seventh effect: the sentence being read is pinned to the top edge with what is
  coming underneath, so the eye never travels back up for the next one.

- **Reading starts after one sentence, not after the whole screen.** `MaReadChunks` cuts the passage
  into chunks that grow — 1, 2, 4, 8, 16 — and each is synthesised while the previous one plays.
  `MaReader.allWords` joins every chunk's timings with the audio before it and `chunkBaseMs` is that
  offset, so the ticker, skip, the caption and the effects still see one file and one timeline.
  The other half of the twenty seconds was the key ring restarting at key one on every read, which
  is already fixed.

- **Matrix replaces Anagram.** The word resolves out of falling noise, one letter ahead of the voice,
  in the void's own window. `MaMatrix` is pure and walked; anagram and its test are deleted.
- **Four gestures on the reading window.** Swipe **either way** kills — voice, window, and a
  transcription in flight; **down** next sentence, **up** previous; **pinch** full screen. Both sides
  kill because the gesture is used without looking and a kill you have to aim is one you can miss.
- **The volume keys let go of the reader.** While reading they pass through, so they change the
  volume of the voice. `VOLUME_KEYS.md` amended.
- **STOP in the long-press dashboard**, and four deliberate stops in total, all doing the same two
  things.
- **The volume-keys key is a drawn rocker**, not a speaker — it wore the reader key's glyph.

- **Anagram, the sixth reading effect.** The keyboard becomes the display: the keys dim and the
  word's letters fly from their own keys to the middle and spell it out with the voice. `MaAnagram`
  holds the geometry, pure and walked in `scripts/test_anagram.py`; `MaAnagramOverlay` traces the
  key grid rather than moving real keys, because a key cannot be re-parented mid-frame. QWERTZ or
  QWERTY by the language badge — the Y/Z swap is invisible in English and sends a letter across the
  whole keyboard when it is wrong. Croatian accented letters have no key and **arrive in place
  rather than being dropped**, or the keyboard would spell a different word than the voice says.

- **The Add keys screen sits above the keyboard** (`imePadding`) and has **Add beside Cancel** in
  the top bar with a count. Searching used to leave a ticked box with no reachable way to accept it.
- **The record key wears a vertical level meter on the left and the elapsed time on the right**,
  only while recording, from the same `audioLevel` and the same clock the recording bar reads.
- **The send key is dim when there is no Send button on the screen.** `MaMagicTargets.sendVisible()`
  looks without pressing, polled every two seconds only while the key is on a row. Dim rather than
  hidden, or the keys beside it would move.

- **The personal n-gram is two models, one per language, and they never meet.** `MaNgram` keys on
  `MaLanguage.active()` — the EN/HR badge — for learning, predicting, saving and forgetting. The old
  mixed `ma_ngram.tsv` is deleted once and both models are rebuilt from the dictation history, which
  records a language per entry. The AI key follows the badge too and its prompt names the language
  three times and forbids the other, because the text before the cursor is often in the other
  language and one mention loses to that evidence.

- **An AI key at the end of the prediction row.** Pressed, it asks the cheap model for five words
  and they replace the row; pressed again, the ordinary guesses come back. **Whichever word he picks
  is taught to the local n-gram with the six words before it**, so the key is a way of correcting
  the local model quickly rather than a prediction engine. Only an AI pick teaches — teaching the
  model its own output back is how an engine becomes certain of one word. `MaAiPredict`, and
  `CandidatesRow`.

- **The Add keys screen has a search, in three layers.** `MaKeySearch`: literal match on label,
  section, face and id; then what he has meant before, learned every time he picks a key with a
  query showing (`maKeySearchMemory`); then a model, only when both are empty, after 700ms of quiet,
  for queries of three characters or more, pinned to the provider's cheapest model and reusing the
  key ring through `DictateController.askCheapModel`. The guess is marked **(AI)**, and picking it
  teaches the memory like any other pick, **so the third layer works itself out of a job.** The whole
  rule is pure Kotlin and walked in `scripts/test_key_search.py`.

- **Send and Record are keys on the feature row**, both under Dictation. Send goes through
  `MaMagicTargets.pressSend()`, so it uses his configured term and cannot drift from what volume-down
  does; it says so when no Send button is found. Record calls `DictateController.onMicClick`, the
  same entry point as volume-up and the bar's mic, and wears the green live ring while running. The
  red dot is `MaRecordRed`, named once and shared with the recording lamp.

- **The volume keys can be switched off, by one key on the row and nothing else.** `VOLUME_KEYS`,
  wearing the same green ring as a full bucket. `VOLUME_KEYS.md` is amended rather than contradicted:
  the old "no setting" rule was protecting against an *invisible* switch in a draggable list, and
  `scripts/test_volume_switch.py` enforces that only `AppPrefs`, `MaVolumeKeys` and `MaFeatureRow`
  may name `maVolumeKeysLive`. Both `onDown` and `onUp` are gated, or a press given to the system on
  the way down would act on release.
- **Power + Volume Up is not possible for any app.** Android never delivers the power key to apps or
  to accessibility services; it is handled in the system policy layer, which is also why the
  screenshot chord belongs to the system and not to whatever is on screen.

- **The recording readings are centred as a group**, by `Arrangement.Center` and not by weights.
  Weights are what centred them originally and what made the size wrap; the arrangement measures
  each child at its own width and splits the leftover around them. Left-aligning in 265 was the
  wrong half of the fix.

- **A takes the code block you are looking at.** The ladder is gone — `autoRank`, the A1/A2/A3 face,
  the long-press reset, `Step.rank`. The key sees only what is in the frame, tries the **lowest
  first** and works upward, skips a block already in a bucket, and says which bucket took it. When
  every block on screen is already held it says that instead. Collecting is not linear: he scrolls
  up, takes one, scrolls down, takes another, and a counter was answering a question he had stopped
  asking. Long press reports what is on screen and how many buckets are free.
- **`verify.py`'s `strip_code` could not see 900 of MaScreenTargets.kt's 963 lines.** A Kotlin char
  literal holding a double quote — `append('"')` — opened a string that never closed, so every check
  reading stripped code had been passing on nothing there. Char literals and raw strings are handled
  now. **Print the count; a check that runs nothing looks exactly like a check that finds nothing.**

- **A bucket holding something wears a green ring**, and it stays until the bucket is poured out.
  It replaces the one-minute tick: a tick is an event and has to be caught, a ring is a state.
  `ThemedKey` gained a `ring` colour for it — the switcher ring stays monochrome and means *kind*,
  the coloured one means *state*. The confirmation inside the chat app cannot be held; this answers
  the same question on our own row.
- **Undo and Redo are keys** in the catalogue, under Editing, drawn through `LegacyActionKey` so
  they are the same press as any other way of firing undo. **Redo covers the buckets too**, and its
  rule is stricter than undo's: any change at all since the undo throws the redo away.

- **The key picker is grouped by meaning**, nine sections from `MaFeatureGroup`, and the grouping is
  a property of the key rather than a `when` on the screen — so a new key cannot be added without
  saying what it is for. **A is in the buckets section**, with C1 to C10 and the bin, in lifecycle
  order: A fills, the buckets hold, the bin empties. The old "Keys" catch-all held twenty-six
  unrelated things in the order they happened to be written.

- **Undo covers the buckets.** `MaBucketUndo` holds twenty steps. The undo key reverses the newest
  bucket change when it is newer than the last text this keyboard wrote, and otherwise goes to the
  field exactly as before. A capture and the bin are undoable; **pouring a bucket into a field is
  not**, because that press also filled the field and undoing half of it is a state nobody asked
  for. Once text is the newest thing the key stays with the field — the field's own history is
  invisible from here. The A key arms the step *before* it presses, so undo rewinds the ladder too.
  `maBucketRank` moved to `MaClipCapture.autoRank` to make that reachable.

- **The buckets have no switch.** They are live whenever C keys are on a row and dead when they are
  not. `maBucketsEnabled` is gone: it defaulted OFF, so a fresh install drew grey C keys that caught
  nothing, and the A key looked broken with them because a copied code block had nowhere to go.
- **The bucket that catches a copy wears a tick for one minute.** `MaClipCapture.lastFilled`, set by
  the capture, drawn by the key. The tick he was watching before belongs to the other app's copy
  button and lasts under a second; nothing here can change that one.
- **Permissions detection asks three sources and takes any yes** — `enabledInputMethodList`, the
  Secure string, then `DEFAULT_INPUT_METHOD` — each wrapped separately. The Secure string alone
  answered "not enabled" for a keyboard that was on. **Allow restricted settings** is inferred from
  the accessibility service running, since it could not have been switched on through a closed gate.
- **The recording readings start at the left and the size says MB**, one line, sized to content. A
  weight gave it whatever the clock did not use, and at 15,0 MB that wrapped onto a second line.

- **Defaults are his.** A fresh install ships his exported settings order, his feature row, number
  row on, edit strip off, buckets off, scroll pages 4. He reinstalls several times a day; a fresh
  install should be the keyboard he uses, not a starting point.
- **Permissions and API keys** is the first settings entry **by default, and movable like any other**
  — first in `DEFAULT`, never pinned. It is what he opens when the keyboard, the microphone or
  accessibility is not working. Numbered in the order they
  must be granted, each row opening the page that grants it, rechecked on resume. Any error mentioning
  a permission, AudioRecord or accessibility opens it.
- **One copy row in the whole app**, drawn by `MaFeatureRow(copyRowOnly = true)` and by nothing else.
  Both views ask the same composable for the same preference, so arranging it once arranges it in both
  places. In the transcription view it is fixed above the record button and is not a setting; on the
  typing keyboard `maEditRow` switches it — the copy-row key on the feature row, key 3.
  The keyboard used to draw `LegacyEditRow` here instead, from a different preference and a different
  key set, while two comments claimed both views drew one row. That is why the two rows ended in
  different keys (§146). `LegacyEditRow`, `LegacyActionRowSetting`, `maCopyRowOnKeyboard` and
  `maCopyRowOnDictate` are all gone with it.
- **The recording notch.** While recording with the keyboard down, a strip the height of the status
  bar sits at the top of the screen: upright VU meter, red dot, clock, bin, send, ENG. It is only as
  wide as its contents, so the corners of the screen are still his — a window is touchable or it is
  not, and the notch shape is what buys click-through without losing the controls.
- **The offline note.** A dictation that lands with no text field open is stored as its own kind of
  entry: its own source, a title written by the model, and deeper gold text in the history.
- **History**: `Insert · [ENG] · Re-Transcribe · Delete`, joined by bullets. The badge is a **switch**
  and Re-Transcribe is the **action** — two steps. Delete all in the header. All deletes on IO and
  wrapped. Rows whose audio file has vanished repair themselves on opening.
- **Volume keys rewritten** as `MaVolumeKeys`, specified in `VOLUME_KEYS.md`, **with no setting** —
  see the rule below. While reading, the taps drive the reader instead: down is next, up is previous.
- **The reader**: Black Void, five caption styles, full screen that survives notifications, a
  dashboard on long press, live speed, and the key ring resuming from the last good key.
- **Magic finger** falls back to a dispatched tap when `ACTION_CLICK` is refused (Compose apps).
- **`scripts/verify.py`** — every red build this project has had, as a check that runs in a second.

**Open, designed, not built:**

- **Avid-style settings** — a triangle beside each entry opening it in place rather than pushing a
  screen. Postponed by Marko himself; it touches all nineteen screens.
- **A transcription-view settings screen**, owning what that view shows. Note that the copy row's
  position and presence are now fixed by design and must NOT become settings there — he was explicit.
  `LegacyActionRowSetting` no longer needs rehousing: it is deleted, along with the row it arranged.
- **The rest of the controller split.** History is the obvious next block and is NOT free: it reads
  `_state`, `transcribe` and the output sink. A seam that requires widening visibility is not a seam.
- **Key modules** — five of about twenty keys extracted into `MaKeyModules.kt`.
- **Offline retry** with a manual resend in the status line.
- **An English base dictionary**, twin of the Croatian one.

## Rules learned the hard way in this stretch

**Two rows with one name are two rows, whatever the comment says.** The copy row existed twice for
several builds — `maCopyRow` drawn by `MaFeatureRow` in one view, `legacyActionRow` drawn by
`LegacyEditRow` in the other — while the comment at each call site said both views drew the identical
row from identical code. He spotted it from two screenshots and named the wrong cause; the real one
was worse. **Before believing that two surfaces share a component, check which preference each one
reads**, and if a comment is the only evidence, it is not evidence.

**Never add a setting that can silently disable something he relies on.** The volume keys had one. It
defaulted on, sat in a list of thirteen draggable switches, and one stray touch turned it off in
silence. He lost days believing the feature had been deleted and I spent three rounds reading a
handler that was correct. **A control that can quietly switch off the thing somebody uses most is a
trapdoor, not a feature.**

**A composable carries assumptions about its parent that are invisible in its own source.** Hosting
the keyboard's recording bar in an overlay failed twice: `LocalWindowController`'s default is a hard
`error()`, and `fillMaxSize()` inside a `WRAP_CONTENT` window resolves to **zero height** — the second
one silently. **When lifting a composable out of its home, ask what it was being GIVEN, not what it
calls.** After three attempts the overlay went back to plain Views, and that was the right trade: a
duplicated thing that works beats a shared thing that does not.

**A flag written on one path and read on both is not a flag.** `deliveredToField` was set in one of
two delivery branches, so offline notes inherited the previous dictation's answer. This is why the
note colour "did not work" three times.

**Weights are promises about width that words cannot keep.** Four labels at a quarter each clipped to
"Ins" and "Del" and squeezed a badge out of existence. Size to content; join with separators.

**After removing anything from a row, look at the row.** Balance breaks silently and it breaks in
whatever was left behind.

## What this app is

A dictation keyboard for Marko Boško: Android IME, forked from FlorisBoard, transcribing through
AssemblyAI. He is a filmmaker who works almost entirely by voice on a Nothing Phone 2a, has low
vision and dyslexia, and uses this all day. He dictates in Croatian and English.

**Repo:** `markoboskoauroville/TTT_MINI` (Apache-2.0)
**Package:** `com.mantraproductions.tttlight`
**CI:** GitHub Actions builds every push, publishes an APK, keeps the two newest releases.

## The two documents

- **`NEXT_DEFAULTS.md`** — the **archive**, not a briefing. 101 numbered sections and 4,000 lines,
  one per build, each recording what was decided and why. Search it for a specific decision; do not
  read it end to end and do not treat its old sections as current — several describe features that
  have since been removed, Groq's language detection among them.
  **Everything still true lives in this file.**
- **`NEXT_DEFAULTS_INDEX.md`** — every section of the archive in one list, so a decision can be found
  without reading the archive. Regenerate it whenever sections are added.
- **`SEQUENCER_PARKED.md`** — one parked feature with its own design note and the refactor it needs.

## How he works

He sends screenshots and dictates. The dictation arrives lightly garbled — read through it rather
than around it, and ask when a word could be two things. He is precise about what he wants and
usually right about why; when he pushes back on a decision, he has almost always spotted something
real.

**He is paying for every build.** A red build costs him the same as a green one.

**A settings export is a secret file.** A `.jetpref` he sends to set defaults from carries his API
ring among ordinary preferences and does not announce it. Read it in place, take the values, never
copy the file, never print it, say by name what was skipped, and shred it after. `SECRETS.md` §2c.

**Keys and tokens follow `SECRETS.md`, without exception.** Extract by shape into
`/home/claude/.secret`, never print, never commit, use by reference, and scan the staged diff for
key shapes before every push. The vault does not survive a session — rebuild it from
`/mnt/user-data/uploads` at the start of each one, and if that folder is empty say so instead of
hunting. Shred only on his word, and say plainly that shredding is not revoking.

**`getValue` and `setValue` are invisible and must never be swept.** Property delegation — `val x by
pref.collectAsState()`, `var y by remember { mutableStateOf(...) }` — needs those imports and never
names them in the code. Any tool that removes imports by searching for the symbol will delete both
and the file stops compiling with an error that points at the delegation rather than at the import.

**A button in settings always carries its icon.** Wherever a settings screen lists keys, buttons or
rows, each entry shows the glyph it actually has on the keyboard, beside the name. He recognises an
image faster than a word, and a list of names alone makes him translate twice — once from the name
to the picture, once from the picture to the key. This is not decoration; it is how he finds things.

**A key says its state with colour, and its identity with a drawn shape.** Reversed on 19.8.2026,
after a build that emphasised the zone keys with a lit ring around the whole key and stripped the
glyph down to a bare letter to make room for it. He called it a downgrade, amateurish, and out of
standard with the rest of the app — and it was, because no other key in the row wears a border, so
the three that did read as bolted on. Green means on. The face carries a shape, and the shape is
what tells the keys apart. Do not put a border on one key that the keys beside it do not have.

**He runs several projects at once, and sometimes a message lands in the wrong chat.** He asked for
this check by name. If a message does not belong to TTT mini — another repo, another app, a website,
a film, a design note about something this app does not contain — say so and stop:

> Please notice you are in the wrong chat.

Nothing more. Do not answer the question anyway, do not guess which project it was, and do not
lecture him about it. One line, and wait.

**Check before saying it.** The tell is content with no counterpart in this app — the giveaway is
usually a noun that does not exist here at all. Look first: a grep or a `find` costs one tool call
and is the difference between a useful warning and an irritating one. A screenshot of an unfamiliar
screen is not enough on its own, since this app has many screens; a portrait photo in a keyboard
with no images anywhere is.

**If it is ambiguous, ask rather than assert.** He switches context faster than the chat does, and
being told he is in the wrong place when he is not is worse than a question.

## What the last session learned the hard way (builds 108-122)

**A comment is not evidence.** The spacebar carried a comment saying it "always wears the spacebar
mark now", and `computeLabel` returned null for it, and the drawing code reads `key.label?.let { }`.
So the mark existed in a comment and nowhere on the keyboard, and he asked for it repeatedly across
sessions while the code claimed it was done. When he says something is missing, believe the phone,
not the comment.

**Verify constants and icons by extracting the artifact, not by recognising the name.**
`Icons.Default.KeyboardTab` was checked by downloading `material-icons-extended-android` and finding
`KeyboardTabKt.class` — that one was fine. `AccessibilityNodeInfo.ACTION_SHOW_ON_SCREEN` was not
checked and does not exist as a plain int; it is an `AccessibilityAction` only. Red build 118, paid
for, and the correct form was eleven lines further down the same file.

**Adding an enum entry breaks exhaustive `when` blocks elsewhere.** Every `MaFeatureKey` or
`MaSettingsEntry` addition needs its branch in `MaRowsScreen`/`MaSettingsOrderScreen` too. Find them
by extracting the enum names with a regex and diffing against what each `when` covers; do not go by
memory of how many there are.

**Run `python3 scripts/verify.py` before every push.** It is the balance check plus every other
red build this project has had: duplicate imports, imports above `package`, a name declared twice in
one scope, two `@Composable` in a row, an icon used without its import, `by` delegation without
`getValue`, a preference that no longer exists, a `when` missing a branch for a new enum entry, and
anything key-shaped in the diff. Each check is there because it cost a build. It reads only the files
this commit touches, and exit 1 means CI would have said the same thing five minutes later.

**Balance-check every edited file against `HEAD`, not against zero.** Counting braces and parens in
the new text only tells you it is self-consistent. Comparing the counts to the committed version
catches a splice that ate one line too many, which happened once and was caught this way.

**A filter that seems like caution can be the bug.** `isVisibleToUser` was added to the field walk as
a safety check and made TAB refuse exactly the fields hardest to reach by hand; the same filter in
`MaScreenTargets` had been quietly crippling the magic finger for far longer. Off screen is a reason
to scroll to something, not a reason to decide it does not exist.

**Keep both documents current as you go.** This session updated `NEXT_DEFAULTS.md` after every build
and did not touch this file once until asked, which would have handed the next session a briefing
twelve builds stale. Update both, in the same commit as the work.

## What an earlier session learned the hard way

**Cutting code by eye fails.** Four times a change spanning several files removed more than intended
— twice pushed. The pattern was always the same: replacing a range by naming its two ends, where the
second end was whatever came next in my head rather than what came next in the file. Cut from a
label to the label that genuinely follows it, found by reading, and count what is left.

**Verify before pushing, not in the same command.** A check whose result arrives after the commit is
a log entry, not a check.

**Search by symbol, not by file name.** `MaCtrlState` looked orphaned by its filename and was used in
two files. Ask what a file exports, then whether anything uses those.

**Look before adding.** Several features were already half-built by a parallel session. Check the
tree before writing.

## Things that are true about the app

- **Croatian must never use the Sync path.** Sync covers 18 languages and Croatian is not one; it
  defaults to English and returns fluent Croatian that is the wrong words. `SYNC_SAFE_LANGUAGES` is
  an allow-list and must stay one.
- **The feature row is the main feature.** Three rows, drag-arranged, row 1 nearest the keys.
- **Colour means state.** `#9B3B33` recording, sand means "this takes you somewhere", dim means
  present but inactive.
- **The accessibility service powers the magic button, TAB and the floating button.** It must be
  declared in the manifest — it once was not, and everything depending on it failed silently.
- **Send is one thing with two triggers, and they must never drift apart.** The send key on the
  magic finger row and a long press on volume down are the same press. Volume down resolves its
  term through `MaMagicTargets.resolveTerm` against the taught list — it must never read a term
  straight out of a preference and press that. Rename it once on the Magic finger screen and both
  follow. **Any future way of firing send joins them the same way**: one list, one term, no copies.
- **A button in a settings list always carries its icon.** Wherever a screen lists keys, buttons or
  rows, each line shows the same glyph the keyboard draws, beside the name. He asked for this by
  name: *he recognises an image faster than text*, and a list of names alone makes him translate
  twice — once from the word to the picture, again from the picture to the key. It also catches a
  whole class of mistake, because a row whose glyph is wrong is visible the moment the screen opens.
- **Roles are wired, not chosen.** `MaRoles` decides: AssemblyAI transcribes, Anthropic rewords and
  proofreads, Speechify speaks. The call path resolves through it at the moment of use, so a stale
  stored id cannot survive. Give a new provider a role there; never give the user a chip. Capability
  is not the same as job.
  **Groq is gone**, and with it automatic language detection: the language is chosen by hand, on the
  badge. A detector that is confidently wrong returns fluent text in the other language, which is
  worse than an error.
- **Anthropic is no longer slated for removal.** Ctrl+P and Ctrl+F depend on it. Gemini is
  unregistered (§31); a stored Gemini key is hidden, not erased.
- **A preset absent from `ProviderRegistry.presets` does not exist.** `byId` returns null and key
  import skips it. That is why a Groq key could not be stored for months.
- **Both hardware keys decide on release.** Short press is volume, a 500 ms hold is the other
  meaning: volume up records and stops, volume down presses a magic finger term. Neither may take
  the volume away on a quick tap — he adjusts bhajan on the same phone.
- **Croatian has a shipped dictionary** (`assets/dictate/hr_words.txt`, CC BY-SA, see §33). It is
  searched only with a prefix, only when Croatian is active, and always after the personal model.
- **`MaNeuralPredictor` and `MaLanguageGuess` are both dead code** — written, never called. Check for
  callers before assuming a file is live.

## Working agreement

Ship small, verified builds rather than large ones. Tell him plainly what was not done and why. When
a request is bigger than the room left, write it to `NEXT_DEFAULTS.md` rather than starting it badly.

He pays for every build, red or green. Verify before pushing and read the result before committing —
not in the same command. Where the logic can be checked without a phone, check it: the case ring, the
voice-command parser and the dictionary search were each run against brute force or a table of cases
before being wired in, and none of them needed a build to find their bugs.

He dictates, so messages arrive garbled. Read through it, and ask when a word could mean two things —
Groq and Grok cost a whole exchange before that was written down.

## Settings rules

- **A button in a settings list carries its icon.** Whenever a screen lists keys or buttons, every
  row shows the same glyph the keyboard shows, beside the name. He recognises the image before he
  reads the word, and a list of bare names makes him translate twice. `MaRowKeyItem` is the shape to
  copy, and reusing it is better than matching it.
- **An important key says it is on by COLOUR, not by a border around the key.** Tried the other way
  round on 19.8.2026 and reversed the same day: a ring on three keys in a row where nothing else has
  one is the thing the eye lands on, and it reads as a patch rather than as emphasis. Colour is the
  app's state channel everywhere else; keep it the only one.
- **A key on the row carries a drawn shape from the icon set, not a bare letter and not a hand-drawn
  one.** The zone keys were letters, then a hand-drawn keyboard outline with a letter inside, and he
  called both amateurish — correctly: every neighbour is a glyph from one set, and a shape drawn by
  hand beside them reads as a patch however carefully proportioned. They are `Numbers`, `Keyboard`
  and `ContentPaste` now. **If the set has nothing for a key, that is a reason to reconsider the key,
  not to draw one.**
