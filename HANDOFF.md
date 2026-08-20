# TTT mini — where things stand

Last updated at build 122. Read this first, then `NEXT_DEFAULTS.md`.

## Where it is now

Build 122. Since 108: the switcher long-press list, magic finger spacers and rename, voice commands
("press send"), volume keys reshaped twice, audio focus removed entirely, Groq registered and Gemini
unregistered, roles hard-wired, Ctrl+P proofread and Ctrl+F flow, a shipped Croatian dictionary, TAB
and Shift+TAB by accessibility tree, a row Shift, and the Aa case cycle.

Open and designed but not built, all in `NEXT_DEFAULTS.md`: §28 Groq language detection (26), bucket
copy (§37), the pinned row, the three-state language button, configurable spacer width.

## What this app is

A dictation keyboard for Marko Boško: Android IME, forked from FlorisBoard, transcribing through
AssemblyAI. He is a filmmaker who works almost entirely by voice on a Nothing Phone 2a, has low
vision and dyslexia, and uses this all day. He dictates in Croatian and English.

**Repo:** `markoboskoauroville/TTT_MINI` (Apache-2.0)
**Package:** `com.mantraproductions.tttlight`
**CI:** GitHub Actions builds every push, publishes an APK, keeps the two newest releases.

## The two documents

- **`NEXT_DEFAULTS.md`** — the working file. `THE LIST` at the top numbers every pending feature;
  everything below it is the detail. **Marko picks by number.** When one is done and he confirms it
  works, strike it off and reissue the list.
- **`SEQUENCER_PARKED.md`** — one parked feature with its own design note and the refactor it needs.

## How he works

He sends screenshots and dictates. The dictation arrives lightly garbled — read through it rather
than around it, and ask when a word could be two things. He is precise about what he wants and
usually right about why; when he pushes back on a decision, he has almost always spotted something
real.

**He is paying for every build.** A red build costs him the same as a green one.

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
  proofreads, Groq detects language. The call path resolves through it at the moment of use, so a
  stale stored id cannot survive. Give a new provider a role there; never give the user a chip.
  Capability is not the same as job — Groq can transcribe and must not.
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
- **A key on the row carries a drawn shape, not a bare letter.** Every neighbour has a silhouette,
  and a loose letter among them looks unfinished. The zone keys are a letter inside a keyboard
  outline (`MaZoneGlyph`) for this reason. If the frame crowds the letter, take detail out of the
  frame — the spacebar line went for exactly this — rather than taking the frame away.
