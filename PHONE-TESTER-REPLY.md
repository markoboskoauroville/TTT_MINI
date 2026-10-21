# Reply to the phone tester (from session_0118g1MJ1Y3aD4nhAieRp6f8)

Reach me with send_message to session_0118g1MJ1Y3aD4nhAieRp6f8, or write in PHONE-TESTER-REQUEST.md.

## 20.10.2026 — build 398 is out: test this

APK:    https://github.com/markoboskoauroville/TTT_MINI/releases/download/build-398/ttt-mini-build-398.apk
sha256: 11c71869038b4ddbf0881f2d6adce362ecdd234f88fa210ea0e38296ed212b3c
size:   46,323,199 bytes
Built by CI from main d6fd298 (green). Signed with the usual key, so it installs over the old one.
It replaces the old ttt-mini-phone-model-test.apk, which this session never had.

Contains: the phone model (ccr-0c287bcf-wy37ae), the resumable download (phone-resume-download),
the aria2-style parallel download, and the shared Download/models folder.

## What to test first

1. Install, open Settings -> Prompts -> Models on the phone, tap Allow (All files access).
2. Back in the app, Qwen3 4B should read "In Download/models, in use; checking its checksum in the
   background", then "Downloaded, in Download/models". No download should start.
3. Choose Qwen3 4B as the model of a prompt and run it.
4. Parallel download: delete nothing; instead try the 8B download only if you want to watch it
   (5 GB, the app warns it is too big for this phone's memory). Kill the app mid-way, start again:
   it should carry on, not restart. A <name>.part and <name>.part.pieces sit in Download/models
   while it runs.
5. Delete on a model in the shared folder should say it deletes the SHARED file.

Report anything red, with the exact text, by send_message.

## Full test plan for build 398 (sent by send_message on 20.10.2026)

Rules: Haiku only, short texts, one try per case; dictations under ~15 s; never print keys; no
build-apk.yml. Record PASS / FAIL / BLOCKED with the exact on-screen text, a screenshot for every
FAIL, logcat for every crash.

A. Models on the phone: A1 Allow All files access; A2 Qwen3 4B "checking" then "Downloaded, in
Download/models", no download (time it); A3 force-stop and reopen, still downloaded; A4 Ctrl+F on
Qwen3 4B, also in flight mode, time it; A5 8B shows the memory warning, do not download; A6 rename
the 4B to .bak, Download, force-stop at ~200 MB, Download again continues not from 0, Cancel removes
.part and .pieces, rename back; A7 Delete dialog says SHARED file, press Cancel.
B. Claude prompts on Haiku: B1 model rows; B2 "Show exactly what is sent" with Croatian / English /
Unclear; B3 Ctrl+P on "dobro jutro kako si danas" stays Croatian, then "Last message sent" and
"Last answered by"; B4 edit the language line, then Built-in.
C. Spell check: C1 switch on; C2 short Croatian dictation shows "Croatian spell check"; C3 English
dictation does not.
D. Claude costs: D1 the requests are listed with tokens and dollars; D2 balance 10, Set.
E. Keyboard: E1 Key colours grid; E2 digit 1 red, A blue (stays with Shift), then Clear all; E3 the
command printed under a number-row digit is readable.
F. Crashes and dumpsys meminfo after A4.

Report: send_message to session_0118g1MJ1Y3aD4nhAieRp6f8, first line "Build 398: X pass, Y fail,
Z blocked", then one line per FAIL/BLOCKED. Full report under "Report build 398" in
PHONE-TESTER-REQUEST.md on this branch.

## 20.10.2026 — build 399: short retest

APK:    https://github.com/markoboskoauroville/TTT_MINI/releases/download/build-399/ttt-mini-build-399.apk
sha256: d99e8b10eab992c4da86b4498d5209f70e44a35ff76ca55525f304cae994a330 (46,323,203 bytes, main cda6161)

R1 Prompts: radio buttons with full model names, no "Default" anywhere (screenshot). R2 A4 again on
Qwen3 4B: stops at the end, no inventions, no cut mid-word; then Ctrl+F back to Claude Haiku 5.5.
R3 space before a dictation after "C2:". R4 stopped download shows "Stopped at N %". R5 Clear all
asks. R6 Ctrl+P output tokens close to the answer (thinking off). Report: "Build 399: X pass, Y fail".

## 21.10.2026 — build 400 replaces 399 for the retest

APK:    https://github.com/markoboskoauroville/TTT_MINI/releases/download/build-400/ttt-mini-build-400.apk
sha256: 6557cb981632ea5a2d0759c961d14299d154e8762ddcb967107f5e063b6ac29a (46,323,199 bytes, main 95d9783)

R0 (new): Settings → Prompts starts with the two switches (Style after every dictation, Croatian
spell check); screenshot. R1–R6 as for 399. Report: "Build 400: X pass, Y fail".
