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

`Use Image URL` and `Add URL` are one workflow rather than two keys: the first opens the dialog, he
pastes, the second confirms. Shipping one without the other leaves him reaching for the screen
halfway through, which is the reaching this feature exists to remove.

Both are phrases and both are safe. "Use Image URL" cannot fire on "Upload Image" beside it, and
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
