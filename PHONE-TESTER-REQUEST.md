# From the phone tester (Claude on Marko's Nothing Phone 2a), 20.10.2026


1. THE TEST APK (blocking). ttt-mini-phone-model-test.apk (sha256 b0fcb403f7fc612bb6e4dcce70ac1ec78021ff7154dc0be8da28e6ced97c4102) is not on the phone and not on GitHub. Please commit it to branch ccr-0c287bcf-wy37ae as releases/ttt-mini-phone-model-test.apk (or attach it to a draft release) and give the path and sha256. Do NOT use build-apk.yml for this: it publishes a Latest release and deletes older ones.

2. THE MODEL IS ALREADY ON THE PHONE. /sdcard/Download/models/Qwen3-4B-Instruct-2507-Q4_0.gguf, 2,375,773,280 bytes, sha256 e0ba675d86ab277c61701c6793659b2ae801d95e3be791464c321e6fbf613be2 verified (the one in MaPhoneModels.kt). Please let the app take a model from a file (e.g. "Use a file already on the phone" next to Download, or pick up /sdcard/Download/models/<name>.gguf when the size and sha256 match), so it isn't downloaded twice.

3. NEW FEATURE, MARKO: "build aria2 inside my application, so it downloads in parallel and can resume a broken download."
   - Parallel: split the file into ranges (e.g. 8 connections, 8 MB pieces), fetch them at once with HTTP Range, write each at its offset into one preallocated file (RandomAccessFile), and keep a small progress file (which pieces are done) next to it, like aria2's .aria2 control file.
   - Resume: after a dropped connection, an app kill or a phone restart, only the missing pieces are fetched again. Retry each piece with backoff (2 s up to 60 s), give up only after many tries in a row bring no new bytes, and keep the partial file for the next start. Cancel deletes it.
   - At the end: exact size plus sha256 from disk, then move into place. The progress bar and notification show the sum of all pieces.
   - Fall back to one connection when the server ignores Range (answers 200).
   - My branch phone-resume-download (commit 63e355a, LocalModelManager.kt only) already does the one-connection resume and retry, so it's a starting point. It is not compiled; please build it.
   - Why: on this phone Hugging Face reset the connection twice in the first 70 MB. With aria2 on the command line (-x 8 -s 8 -c) the same file finished at 2.6 MB/s on average and passed sha256.

4. When the new APK is ready, put it on the branch with its sha256. The phone session will run the whole test plan once and send the report back.

5. PUSH THE LATEST WORK TO MAIN (Marko, 20.10.2026). Please merge your newest updates into main: the phone model (branch ccr-0c287bcf-wy37ae, commit b607316: llama.cpp, Models on the phone, Style after every dictation, a model chooser on every prompt), the resumable download from branch phone-resume-download (63e355a), and the parallel aria2-style download once it's written. Build it first, and fix what doesn't compile. Pushing to main runs build-apk.yml, which publishes a new release (TTT mini build N), and that release gives the phone an APK to install and test.

6. ONE MODEL FOLDER, SHARED, AND FOUND ON ITS OWN (Marko, 20.10.2026).
   - Confirmed on the phone at 17:39: /storage/emulated/0/Download/models/Qwen3-4B-Instruct-2507-Q4_0.gguf (the same file as /sdcard/Download/models/...), 2,375,773,280 bytes, sha256 e0ba675d86ab277c61701c6793659b2ae801d95e3be791464c321e6fbf613be2. Downloaded by aria2 on the command line, checked by aria2 against that hash.
   - Make Download/models/ the app's default download location for the phone models, under the same file names as on Hugging Face (Qwen3-4B-Instruct-2507-Q4_0.gguf, Qwen3-8B-Q4_K_M.gguf), so the app, aria2 and any other tool all use one copy.
   - On every start, and right after an update is installed, the app looks in Download/models/. When a model's file is there with the exact size from MaPhoneModels, the app shows it as downloaded and starts using it, with no download and no question. Check the sha256 once, in the background, and remember the result (path + size + mtime) so it isn't hashed again on every start. A file with the wrong size or hash counts as not downloaded, and the reason shows in red under the chooser.
   - Android permission: Download/ is shared storage. To read a file another tool put there (and to mmap it for llama.cpp), the app needs "All files access" (MANAGE_EXTERNAL_STORAGE, the way Donnie Horoscope does it) or a folder the user picks once through the system picker (SAF), with the permission kept. Without either, it can't see the file. Ask once in Settings → Prompts → Models on the phone, and explain why there.
   - The app's private models folder can stay as the fallback when the permission is refused.
   - "Delete" on that screen must say whether it deletes the shared file (other tools use it too).

## Report build 398 (phone tester, 20.10.2026 20:35)

**Build 398: 17 pass, 1 fail, 2 blocked.** Nothing Phone 2a, 7.6 GB RAM, build 1398 installed over 1397.

| Case | Result | What the screen said |
|---|---|---|
| A1 All files access | PASS | "Allowed. Models are downloaded there, and a model already there at the right size is used without downloading it again." |
| A2 4B found, no download | PASS | "Downloaded, in Download/models" |
| A3 force-stop, reopen | PASS | still downloaded |
| A4 Ctrl+F on Qwen3 4B | **FAIL** | Works online (76 s) and in flight mode (48 s), but makes up sentences and is cut off mid-word. Example: "…I'll send the link by 3pm. You can reach me at jones@company.com. It's not perfect, but it's". Fixed in a3a5d7d, not yet in an APK. |
| A5 8B memory warning | PASS | "Too big for this phone's memory: it may not start" |
| A6 resume download | PASS | Killed at 9 % (24 of 284 pieces of 8 MB); Download again continued at 9 %, then 11 %. Cancel removed .part and .part.pieces. ~3.7 MB/s. |
| A7 Delete dialog | PASS | says it deletes the SHARED file; Cancel kept it. Dialog "2,4 GB" vs row "2.4 GB" (fixed in a3a5d7d). |
| B1 model rows | PASS | Haiku / Sonnet / Opus / Phone 4B / Phone 8B on every prompt, with "In use: …" |
| B2 Show exactly what is sent | PASS | Croatian / English / Unclear each swap the language line. Wording: the English line says "…must be in English. Do not translate it into English or any other language." |
| B3 Ctrl+P "dobro jutro kako si danas" | PASS | "Dobro jutro, kako si danas?" in 6 s; "Last message sent 20:23:28 — asked for claude-haiku-latest"; "Last answered by: claude-haiku-5-5" |
| B4 edit language line, Built-in | PASS | "(yours)" after Save, "(built in)" after Built-in |
| C1 spell check switch | PASS | on |
| C2 Croatian dictation | BLOCKED | needs a real voice: Android mutes TTS played from a background app ("AudioHardening background playback would be muted for com.google.android.tts") |
| C3 English dictation | BLOCKED | same |
| D1 costs list | PASS | "Today $0.0030 · This month $0.01 · 72 requests logged", each with tokens and dollars; Haiku 5.5 at $0.10/$0.50 is right |
| D2 balance 10, Set | PASS | "about $10.00 … entered on 20.10. 20:32, minus $0.0000 spent" |
| E1 Key colours grid | PASS | |
| E2 1 red, A blue + Shift, Clear all | PASS | |
| E3 command under a digit | PASS | "/exit", "/rc" readable; "/rc" grey on dark is the weaker of the two |
| F crashes, meminfo | PASS | no crash or ANR. Keyboard PSS 172–230 MB. During A4, lowmemorykiller killed ~12 background apps (com.mantra.cockpit, com.mantra.route, com.mantra.sampleplayer, Settings, TTS…); MemAvailable 1.2 GB after. |

**Also seen**
1. Dictation of near-silence (Croatian, H key) typed an invented English sentence: "Do you feel only that quality I wanted? Yes or no?". The usual Whisper silence hallucination; worth a silence/energy check before transcribing, and the language was ignored.
2. "Style after every dictation" is set to Phone: Qwen3 4B on this phone, so every dictation waits ~45–75 s and squeezes memory. Check it is meant to be the default.
3. Spell check sometimes answers with far more than it got: 480 in / 1051 out, 447 in / 857 out, 443 in / 859 out (Haiku). Same padding family as A4.
4. After a killed download the row only says "Download"; nothing shows that 9 % is already on disk and will be kept.
5. Clear all in Key colours wipes everything with no confirmation and no undo.
6. Dictated text is glued to what is before the cursor: "C2:" + dictation gave "C2:Do you…", no space.
