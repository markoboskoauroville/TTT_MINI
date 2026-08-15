# TTT mini — where things stand

Written at build 107. Read this first, then `NEXT_DEFAULTS.md`.

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

## What this session learned the hard way

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
- **One AssemblyAI key is the direction.** LLM Gateway reaches Claude and the rest on that key, which
  is why Gemini and Anthropic are slated for removal (§11).

## Working agreement

Ship small, verified builds rather than large ones. Tell him plainly what was not done and why. When
a request is bigger than the room left, write it to `NEXT_DEFAULTS.md` rather than starting it badly.
