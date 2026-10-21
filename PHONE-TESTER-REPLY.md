# Phone tester reply (Mac session "ttt mini find me", 21.10.2026)

I have no send_message tool, so I answer here.

## PREPARE
- `adb devices`: `41241FDH2S10SJ  device  model:Pixel_7 product:panther` (real Pixel 7 on USB)
- `dumpsys package com.mantraproductions.tttlight`: `versionCode=1385`, `versionName=1.0`

## BUILD
I wait for build 403 and do not download 402 (Marko is on a 4G SIM: newest APK only, never twice).
Put the 403 link and sha256 in this file under a heading `## BUILD 403`. I'll check this branch for it.

## REPORT, build 403 (21.10.2026, 17:40–17:59)

Installed over 1385 with `install -r`; the sha256 matched cf8b6f2b…4384b; versionCode now 1403; the home screen shows "TTT mini 403".

**Pixel 7, build 403: 5 pass, 1 fail, 2 blocked**

1. PASS. Prompts: each of the four choosers (Style, Croatian spell check, Ctrl+P, Ctrl+F) is a radio list: Claude Haiku 5.5 / Claude Sonnet 5.5 / Claude Opus 5.5 / Qwen3 4B Instruct 2507 / Qwen3 8B. There is no "Default" anywhere on Prompts. I set Ctrl+F to Sonnet, left, came back: it stayed Sonnet. I set it back to Haiku.
   - Note: the "Sent to Claude" block still said `Model: claude-haiku-latest` while Ctrl+F was set to Sonnet. It does not follow the chooser (or does not say which prompt it describes).
   - Note: "Default" appears outside Prompts: Recording → "Audio input source: Default microphone", and Formatting & vocabulary → "Punctuation & style: Default".
2. FAIL (partly). Switches at the top: Prompts, Recording, Magic finger, Floating button (one switch), Gestures, Clipboard, Keyboard and Learn my words are fine. **Smartbar**: "Layout" (no switch) sits between "Enable Smartbar" and "Flip toggle buttons". **Typing**: "Incognito mode" (no switch) is between the Suggestions switches, and the Corrections switches (Auto-capitalization, Auto-space, Remember caps lock, Double-space period) come after it. Formatting & vocabulary has no switches.
3. PASS. Key colours: a grid of the number row, feature row 1 and the whole keyboard. I painted g red: only the letter turned red and the keycap stayed dark, both in the grid and on the real keyboard (the hint symbol & turned red too). "Clear all" asked "Clear every key colour? All 1 painted keys go back…" before clearing. I cleared it, so he has no colours now, as before the test.
4. PASS. Number row (# key) on the real keyboard: no sign in the corner, big digits. At the bottom of each key there is only "_", because none of his digits has a snippet. I did not write a snippet to see real text there.
5. PASS. Qwen3 4B: "Downloading, 1 %" → ✕ → "Stopped at 1 %. Download continues from there." The Allow row shows "Shared folder: Download/models — Allowed…".
   - Note: at "Downloading, 0 %" the progress bar was drawn almost full (screenshot 09a).
   - Note: `adb shell ls /sdcard/Download/models/` is empty after the stop, so the partial file is not in the shared folder.
6. BLOCKED. Ctrl+F on a phone model needs the 2.4 GB model. Marko's internet is a 4G SIM, so I did not download it.
7. BLOCKED. Dictation needs his real voice.
8. PASS. Claude costs: one Ctrl+P on Haiku gave `21.10. 17:57 Proofread` / `claude-haiku-5-5 380 in / 5 out $0.0000`.
   - Note: the cost shows $0.0000 even for 380 tokens in; check the Haiku 5.5 price or the rounding.
   - Note: "1 requests logged" (plural).
   - The request itself: "helo" → "Hello", correct. My first attempt typed "helo wrold", but adb dropped the second word, which was my fault, not the app's.

Total spent: one Haiku request; about 24 MB of model download.
By mistake, one tap opened "Clear history?" on the History screen. I pressed Cancel and his history is untouched.
