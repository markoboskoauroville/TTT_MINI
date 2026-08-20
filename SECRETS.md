# Secrets manifest

**Paste this into any chat that will be handed a file of keys.** It describes how tokens and API
keys are handled in this project, from the moment a file is attached to the moment Marko says shred
it.

---

## 1. What arrives, and where it lives

Keys arrive as an attached text file: a GitHub token, Speechify keys, Groq keys, Gemini, Hume,
AssemblyAI, Anthropic. The file lands in `/mnt/user-data/uploads/` and **stays there for as long as
the conversation lives**. It is not deleted between messages.

The working copy goes in a vault inside the sandbox:

```bash
mkdir -p /home/claude/.secret && chmod 700 /home/claude/.secret
```

One file per provider — `gh_token`, `speechify_keys`, `groq_keys`, `gemini_key` — each `chmod 600`.

**Read this next part carefully, because it is the thing most likely to be got wrong.** The sandbox
filesystem is wiped between sessions. The vault does **not** survive into the next chat. What
survives is the uploaded file. So every new session rebuilds the vault from `/mnt/user-data/uploads/`
as its first act, and if that folder is empty the honest answer is "the keys are gone, please attach
them again" rather than a search for a vault that cannot exist.

---

## 2. Extract by shape, never by eye

Do not print the file to look at it. Pull the keys out with a pattern and write them straight to the
vault:

```bash
grep -oE 'gsk_[A-Za-z0-9]{20,}'   /mnt/user-data/uploads/groq.txt      > /home/claude/.secret/groq_keys
grep -oE 'sk_[A-Za-z0-9_-]{30,}'  /mnt/user-data/uploads/speechify.txt > /home/claude/.secret/speechify_keys
grep -oE 'AIza[A-Za-z0-9_-]{30,}' /mnt/user-data/uploads/gemini.txt    > /home/claude/.secret/gemini_key
chmod 600 /home/claude/.secret/*
wc -l /home/claude/.secret/*        # count only — never the contents
```

**Why by shape.** These files are notes, not key lists. They contain account names, URLs with
tracking parameters, and blank lines. Splitting on whitespace has genuinely produced attempts to
authenticate with the word *cafeteria* and with a Google `srsltid` token. A shape filter takes the
keys and leaves the prose.

If the file must be quoted back — to show its structure, to ask which account is which — redact
first, always:

```bash
sed -E 's/(sk_|gsk_|AIza|ghp_)[A-Za-z0-9_-]+/<REDACTED>/g' /mnt/user-data/uploads/keys.txt | head -20
```

---

## 3. The rules that do not bend

- **A key is never printed.** Not in chat, not in a log line, not in an error message, not in a
  commit message, not in a comment, not in a filename.
- **A key is never committed.** Not in source, not in a config file, not in a test fixture, not in
  documentation. Add `.secret/` to `.gitignore` and check `git status` before every `git add -A`.
- **A key is used by reference, never by value.** `-H "Authorization: Bearer $(cat
  /home/claude/.secret/groq_keys | head -1)"`. The value goes from the file into the process and
  nowhere else — it never appears in a variable that gets echoed, in a heredoc that gets shown, or
  in a URL that gets logged.
- **Before any push, scan the diff:**

  ```bash
  git diff --cached | grep -nE '(sk_|gsk_|AIza|ghp_|github_pat_)[A-Za-z0-9_-]{20,}' && echo "STOP"
  ```

  A key in a commit is public the moment it is pushed, and rewriting history does not un-publish it.

---

## 4. What "not sent anywhere" honestly means

It means: **not sent anywhere it does not have to go.** Being precise about this matters more than
being reassuring.

- The key **is** sent to the provider it belongs to, over TLS, when a test calls that provider's API.
  That is the entire point of having it.
- The key is **not** sent to any other provider, any analytics, any third party, or any repository.
- The key is **not** typed into the conversation, so it does not sit in the transcript. The only way
  it enters the transcript is if something prints it — which is why nothing prints it.
- The vault is **local to the sandbox** for this conversation and is destroyed when the sandbox is.
- The uploaded file itself was attached to the conversation by Marko, so its contents are part of
  that conversation's data by his own action. Extracting it into the vault does not add a copy
  anywhere new; it only avoids putting the key on screen again.

---

## 5. Keys are for Test 2, and Test 2 only

The Four Tests framework:

1. **The logic alone** — no network, no key. Pure functions, run in Python or Kotlin against the
   cases that matter, including the ones the code declines.
2. **The real thing, once** — one real call with a real key, to prove the endpoint, the auth header,
   the response shape and the status codes are what the documentation claims. **This is the only
   test that touches a key.**
3. **The ugly cases** — empty input, a dead key, a 429, no network, a truncated response.
4. **The upgrade** — what happens to somebody who already had the previous version.

A key exists to answer Test 2 and to answer it cheaply: one small request, the smallest model, the
shortest input. Do not loop over a catalogue to be thorough. Do not synthesise a paragraph when a
sentence proves the same thing. It is his money.

---

## 6. Key rings and what a failure means

Most providers here have several keys, in a ring, for fallback. The mapping is measured, not assumed:

- **200** — good.
- **401 / 403** — the key is dead. Condemn it, move to the next one, retry the same request so the
  failure is never seen.
- **429** — the key is **valid and throttled**. Rest it. Never condemn it. This distinction is the
  one that gets lost, and losing it burns a working key.
- **404** — usually the wrong endpoint, not a bad key. Check the path before blaming the ring.
- Anything else — stop and report. Retrying a working key against a real error spends money twice.

**Never test a key speculatively.** Use the first key not known dead, and let a real request find out.
A dead key should cost one wasted call in its entire life.

If a dead list is stored, store **SHA-256 fingerprints, never keys**. A file of fingerprints is safe
to read, safe to log, and safe to commit.

---

## 7. Shredding

The keys stay until Marko says shred. He says it in plain words — "shred the keys", "shred Groq" —
and then:

```bash
shred -u /home/claude/.secret/groq_keys 2>/dev/null || rm -f /home/claude/.secret/groq_keys
ls -la /home/claude/.secret/          # show what remains, by name
```

Report what was removed and what is left, by **name only**. Never by value.

Two things to say plainly at that moment, because they are easy to assume and both are wrong:

- Shredding the vault does **not** remove the uploaded file from the conversation. If he wants that
  gone, he deletes the attachment or the chat.
- Shredding does **not** revoke anything. A key that has been used is still live at the provider. If
  the concern is exposure rather than tidiness, the answer is revoking it in the provider's console
  and issuing a new one — and it is worth saying so rather than letting a shred feel like more than
  it is.

---

## 8. If a key is ever exposed

Say so immediately and plainly, in the same message it is noticed. Do not bury it, do not soften it,
do not wait to see whether it matters. Then, in this order: name which key, say where it went, and
say that it must be revoked at the provider now. A revoked key costs him two minutes. An exposed key
that nobody mentioned costs whatever is on the account.

---

## 9. The short version

Extract by shape into `/home/claude/.secret/`, `chmod 600`. Never print, never commit, never echo.
Use by reference. Scan the diff before every push. One real call per feature, and only for Test 2.
401 kills a key, 429 rests it. Rebuild the vault from uploads at the start of every session, because
the sandbox does not survive. Shred on the word, report by name, and say plainly that shredding is
not revoking.
