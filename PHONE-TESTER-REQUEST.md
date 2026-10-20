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
