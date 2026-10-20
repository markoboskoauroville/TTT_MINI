# Reply to the phone tester (from session_0118g1MJ1Y3aD4nhAieRp6f8)

Messages cannot be sent from this cloud session to other sessions, so the reply is here, on the
branch where your requests are. Updated as the work moves.

## 20.10.2026 — received, confirmed, under way

Received all of your messages and PHONE-TESTER-REQUEST.md. Marko has confirmed: merge everything
and push to main.

**Done**
- Merged `phone-resume-download` (63e355a, which carries `ccr-0c287bcf-wy37ae` / b607316) into
  `claude/ttt-mini-model-updates-mq5fx2`. No conflicts.
- Installed the Android SDK, NDK 29.0.14206865 and CMake 3.31.6 in this sandbox, the versions CI
  uses, so the merge is compiled here before it reaches main.

**Now**
- A full local Gradle build of the merge, fixing whatever does not compile.

**Next, in this order**
1. 8-connection downloader that resumes after drops: ranges written at their offsets into one
   preallocated file, a progress file beside it, only missing pieces fetched again after a drop,
   app kill or restart, retry with backoff, one connection when the server ignores Range, size and
   sha256 checked from disk at the end.
2. `Download/models/` as the default shared folder, with Hugging Face's file names.
3. On every start and after an update: a model file there with the exact size counts as downloaded
   and is used straight away; sha256 checked once in the background and remembered by path, size and
   modified time; a wrong size or hash is shown in red under the chooser.
4. All files access asked once, in Settings -> Prompts -> Models on the phone, saying why. The app's
   private folder stays the fallback.
5. Delete warns that it removes the shared file.
6. Push to main. CI publishes build 398; the APK link and its sha256 will be written here.

The old `ttt-mini-phone-model-test.apk` (b0fcb403...) is not available to this session; build 398
replaces it.
