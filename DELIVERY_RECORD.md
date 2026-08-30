# Delivery record — TTT mini

What was measured, what failed on the way, and what was not tested. Specified by
`MANTRA_MANIFEST/modules/delivery-gate.md`; rewritten per release rather than appended.

**A record showing only green is a record of the checks, not of the work.** The failures are here.

---

## Build 324 — 26.8.2026

    ARTEFACT   ttt-mini-build-324.apk
               https://github.com/markoboskoauroville/TTT_MINI/releases/download/build-324/ttt-mini-build-324.apk
    BUILT BY   GitHub Actions, from commit 88e9675
    PREVIOUS   build-322, still downloadable — G8's parachute exists

### The gates

| Gate | Result |
|---|---|
| G1 provenance | **PASS.** Tree clean, HEAD == origin/main == the built commit. All four actions pinned by commit SHA. Two releases kept. |
| G2 secrets | **PASS.** APK scanned as a binary in CI, blocking. Full history scanned: 0 strong-shape hits. |
| G3 analysis | **PARTIAL.** `verify.py` clean; 24 suites, ~60,000 checks, 0 failed. **Lint runs and does not block — 67 errors, 971 warnings.** detekt and R8 report have never run. |
| G4 dead code | **PASS on what is swept.** 41 feature keys: 0 undrawn, 0 without a picker glyph, 0 ungrouped. 6 reader effects: 0 offered-but-not-drawn. 67 `Ma*` preferences: 0 read nowhere. R8's kept-list never swept. |
| G5 dead loops | **PASS with a finding.** 28 `while (true)` examined; 27 bounded by a visible break or a delay with a cancellation point. One unbounded: `ImeWindowEditorHandles.kt:220`, a `pointerInput` block cancelled when the composable leaves. Read, understood, not a hang. |
| G6 stress | **NOT RUN.** No device. |
| G7 budgets | **PARTIAL.** APK 40,045,092 bytes, of which `libonnxruntime.so` is 25.8 MB and the app's own dex is 3.9 MB. Everything else needs a phone. |
| G8 upgrade | **NOT RUN.** No device. |
| G9 record | This document. |

### What failed on the way, and was fixed

- **The G2 gate failed its own first run because the artefact was CLEAN** — `set -o pipefail` plus a
  `grep` that exits 1 on no match. A check that goes red on success teaches you that red means
  nothing. Fixed before it ever guarded a real build.
- **`+0 bytes` between two builds was disbelieved and then explained.** Different sha256, identical
  file size; the APKs were opened and diffed entry by entry — `classes.dex` differed by **+1,920
  bytes**, and the zip total landing identically is alignment padding. **The file size was the wrong
  metric**; the sum of uncompressed entries is the one that moves.
- **The Cloudflare 403 classifier** was added after the audit against `quota-and-fallback.md` found
  that every 403 was treated as a bad key — one refusal would have buried the whole key ring.

### NOT TESTED — as specific as the passes

- **No build of this app has ever been installed or run on a phone by me.** Not one.
- **No recording has been made.** The microphone path, the WAV writer, the silence trim and every
  provider call are unexercised outside their unit walks.
- **No transcription has been requested.** The classifier is tested against bodies quoted from his
  measurements in another project, not from calls made here.
- **The accessibility service has never attached.** The magic finger, the screen reader, the copy
  fallback and the recording hairline have never drawn or acted on a real screen.
- **No reading has been spoken.** Speechify has never been called from this code; the chunk plan,
  the word timings and the effects are walked, not heard.
- **The keyboard has never been shown.** Every layout figure — the hairline's one device pixel, six
  tabs, a quarter of a spacebar — is arithmetic, not observation.
- **No upgrade has been performed.** Whether a phone carrying an older `maRows` string reads it back
  correctly is asserted in a test and has never happened on a device.
