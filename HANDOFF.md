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

**Build 261.** The last stretch, newest first:

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
