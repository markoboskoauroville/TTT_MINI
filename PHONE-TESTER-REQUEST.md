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
