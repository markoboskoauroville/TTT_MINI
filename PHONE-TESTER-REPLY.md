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

## REPORT, build 405 (21.10.2026, 18:05–18:10)

Installed over 1403 with `install -r`; the sha256 matched 0aad3bf5…a747; versionCode now 1405. Tapping the new icon opens settings, headed "TTT mini 405".

**Pixel 7, build 405: 6 pass, 1 fail, 2 blocked** (items 1–8 carried over from build 403: 405 changes only the icon, so I did not run them again)

9. PASS for what this phone can show. The new icon in the app drawer (grid and search) matches exactly: a dark round ground, red A, gold B and cyan C on dark keys, a violet mic key and a green space bar below them, "v405" in light seven-segment digits.
   - The launcher on this Pixel is **Nova Launcher**, not the Pixel launcher. Force-stopping nexuslauncher does not apply, and no cache was needed: the new icon appeared at once.
   - Home screen: TTT mini is not on his home screen. I did not add it, so his layout stays as it is.
   - Themed icon: NOT TESTABLE here. Nova draws the full-colour icon, and Android's Themed icons setting does not reach it (the grid_control provider refused adb).
   - Notification icon while recording: BLOCKED. Recording needs Marko at the mic; I'll ask him and add it here.

## BUILD 419 (from the builder, 25.10.2026)

The send_message to this session failed (session inactive), so the build is here.
Marko is on 4G: download it once, the newest only.

https://github.com/markoboskoauroville/TTT_MINI/releases/download/build-419/ttt-mini-build-419.apk
sha256 `30d9e20defca52593f95b33294179661ee764ce926984f24eaa00ae1ca83cb0d` (46,334,258 bytes)
Install: `adb -s 41241FDH2S10SJ install -r <apk>` (never uninstall).

Test plan, build 419:
1. The settings list has "My corrections" (next to Claude costs). In it: the "Learn from my corrections"
   switch at the top, then "Add a fix" (Heard → Write), then the rules. No word "Default" on the screen.
2. Add a fix: Heard "Kukljca", Write "Kukljica", Add. It shows under "In use", "added by you · every
   language". Switch it off and on; Delete removes it.
3. "Add my replace list (26)": 26 rules under In use; pressing it again adds nothing. "Delete all" asks
   "Delete every correction?" — press Cancel and leave the list (Marko may want it).
4. Typing side (from 22.10, no dictation needed): type "teh ", backspace into it, type "the "; then type
   "teh" again: "the" should be the first suggestion.
5. Dictation learning (needs Marko's voice, else BLOCKED): dictate a sentence with a name, fix one word by
   hand, dictate again; make the same fix a second time; it appears under "In use" and the next dictation
   is fixed by itself.
6. Fixes from your build 403 report: Smartbar, every switch above "Layout". Typing, Incognito after all the
   Suggestions and Corrections switches. Claude costs, one Haiku Ctrl+P shows about $0.000040 and
   "1 request". Prompts → "Sent to Claude" says "Each prompt is sent to the model ticked under it above."
   A model download at 0 %: the bar's track is faint, not almost full (only if it costs little data).

Rules: at most one Haiku request; no keys printed; nothing published or pushed to main.
Report under "## REPORT, build 419" here (or to session_0118g1MJ1Y3aD4nhAieRp6f8), as
"Pixel 7, build 419: X pass, Y fail, Z blocked".

## REPORT, build 420

Nothing Phone, build 420: 1 pass, 0 fail, 6 blocked (Mac session macbook-pro-local-elegant-koala, 27.10.2026 07:28)

Device: Nothing Phone A142 over wireless adb, 100.67.149.40:5555. The Pixel 7 was not touched after
the device change (before it, only read: versionCode 1413, active keyboard, screen awake).
APK: CI release build-420, sha256 7e31511a0798ba51ae45aed42a8c24133c2bd68cb2d8b50d7b9c6bd86a575cc0 (matches),
downloaded once. `install -r` → Success, versionCode 1419 → 1420. Nothing uninstalled.

- A. PASS. The settings list shows "Auto prediction" ("The suggestion row, the AI key's model, the words it learned")
  and "My corrections" ("Words you fix, fixed for you next time"); there is no "Learn my words" entry anywhere in the list.
- B–G. BLOCKED. While I opened Auto prediction, Marko was using the Nothing Phone himself: the Claude app came to the
  front with his draft "check the Nothing Phone report and fix what fails" and a 45 s TTT dictation being sent
  ("sending 0:45, 1105 kB wav fast, K1_5"). I stopped all input so I don't type into his live chat.
  Three of my scroll swipes (x 540, y 900→2000) may have landed in the Claude app during that moment; I saw no effect,
  but Marko should check his draft. Nothing in TTT's settings was toggled, so nothing needs putting back.
  No Haiku request was made.

To finish B–G I need the phone to myself: Marko says when it is free, or the tests move to the Pixel 7 emulator
(Pixel_7_API_35 on this Mac) for everything except E (no Anthropic key there).

### Restoring the chat interface (07:29)
Nothing restored yet, on purpose: at 07:28 Marko was using the phone himself, WhatsApp Business in front
(after the Claude chat, where he dictated). Bringing Claude to the front would take the phone from his hand.
State: the Claude chat (task #348) is intact in the background, where he left it; TTT's settings
(task #357, the Settings list only, nothing changed in it) is also in the background. I did not force-stop TTT
(it is his active keyboard). I could not see which "menu item" is missing: in my last screenshots of the Claude chat
(07:26–07:27) the title bar had its back arrow, the title, the progress circle and the ⋮ menu, and the
bottom row had + , the model "Opus 5.5 Medium", the mic and send. I need Marko to name the item, or the phone free, to look.
