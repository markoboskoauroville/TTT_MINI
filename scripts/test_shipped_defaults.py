#!/usr/bin/env python3
"""
Test 1 for the shipped defaults.

A fresh install should already be the keyboard he uses. **But an exported settings file is not all
preferences** — it also holds his API keys, his clipboard, his dictated words and one-time migration
flags, and none of those may ship.

    python3 scripts/test_shipped_defaults.py
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PREFS = ROOT / "app/src/main/kotlin/dev/patrickgold/florisboard/app/AppPrefs.kt"

failures: list[str] = []
checks = 0


def check(name: str, ok: bool, detail: str = "") -> None:
    global checks
    checks += 1
    if not ok:
        failures.append(f"{name}: {detail}")


src = PREFS.read_text()

# ---------------------------------------------------------------- nothing private ships
#
# The export he sent carried five AssemblyAI keys in plain text. **Nothing key-shaped may ever appear
# in this file**, and this check is cheap enough to run for ever.
for pattern, what in (
    (r"[0-9a-f]{32}", "a 32-hex key"),
    (r"sk-ant-[A-Za-z0-9_-]{20}", "an Anthropic key"),
    (r"AIza[A-Za-z0-9_-]{30}", "a Google key"),
    (r"ghp_[A-Za-z0-9]{30}", "a GitHub token"),
):
    check(f"no {what} in the defaults", not re.search(pattern, src), "a secret would ship to everyone")

# His content and his usage are not defaults either. A default carries to every install, so shipping
# these would hand a stranger his clipboard and a counter he never earned.
for name, why in (
    ("dictate__last_dictation", "his last dictation"),
    ("dictate__ma_clip_captured", "his clipboard"),
    ("dictate__ma_ngram_pending", "words he dictated"),
    ("dictate__ma_key_search_memory", "what he searched for"),
    ("dictate__total_audio_seconds", "his usage counter"),
):
    m = re.search(rf'key = "{re.escape(name)}",\s*\n(?:\s*//[^\n]*\n)*\s*default = ([^\n]+?),?\n', src)
    if m:
        v = m.group(1).strip().rstrip(",")
        check(f"{name} ships empty — {why}", v in ('""', "0", "0L", "false"),
              f"ships {why}: {v[:40]}")

# ---------------------------------------------------------------- the keyboard he uses
#
# Spot-checked, not exhaustively: these are the ones that decide whether a fresh install is usable.
for name, want, why in (
    ("dictate__transcription_provider_id", '"assemblyai"', "a provider this build does not offer"),
    ("dictate__ma_reader_voice", '"beatrice_32"', "no voice chosen"),
    ("dictate__ma_reader_speed_tenths", "12", "his reading speed"),
    ("dictate__ma_reader_align", '"top"', "the alignment he reads at"),
    ("dictate__ma_reader_style", '"void"', "the effect he uses"),
    ("dictate__ma_dictation_api", "true", "the English path off by default"),
    ("dictate__ma_cloud_log", "true", "the Claude.ai reader off"),
):
    m = re.search(rf'key = "{re.escape(name)}",\s*\n(?:\s*//[^\n]*\n)*\s*default = ([^\n]+?),?\n', src)
    check(f"{name} ships as {want}", m is not None and m.group(1).strip().rstrip(",") == want,
          f"{why} — found {m.group(1).strip() if m else 'nothing'}")

# The row and settings-order strings are his, and they are the reason a fresh install looks right.
for name in ("dictate__ma_rows", "dictate__ma_settings_order", "dictate__ma_macro_slots",
             "dictate__ma_magic_targets"):
    m = re.search(rf'key = "{re.escape(name)}",\s*\n\s*default =\s*(?:\n\s*)?(?:(?://[^\n]*\n\s*)*)"',
                  src)
    check(f"{name} ships something", m is not None, "a fresh install would start empty")

# ---------------------------------------------------------------- types survived
#
# The defaults were rewritten mechanically from an export where everything is a string. An enum or a
# Color default replaced by a quoted string compiles nowhere — and that happened twice while writing
# this, caught by reading the diff rather than by any check.
check("the prompts layout is still an enum", "default = DictatePromptsLayout.ROW," in src,
      'replaced by "ROW", which is a different type')
check("the accent colour is still a Color", "default = Color(0xFFE8B15C)," in src,
      "replaced by its hex string, which is a different type")

print(f"shipped defaults, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
