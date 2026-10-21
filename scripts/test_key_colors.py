#!/usr/bin/env python3
"""
Test 1 for key colours (G815 style) and the command line under each digit.

    python3 scripts/test_key_colors.py

The Kotlin (MaKeyColors, MaNumericSecondary.preview) was compiled with kotlinc and run against 16
cases when written; this suite checks the wiring, which is where a colour would silently not show.
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "app/src/main/kotlin/dev/patrickgold/florisboard"
SNYGG = ROOT / "lib/snygg/src/main/kotlin/org/florisboard/lib/snygg/ui"

failures: list[str] = []
checks = 0


def check(name: str, ok: bool, detail: str = "") -> None:
    global checks
    checks += 1
    if not ok:
        failures.append(f"{name}: {detail}")


def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


kc = code(APP / "dictate/MaKeyColors.kt")
ns = code(APP / "dictate/MaNumericSecondary.kt")
row = code(APP / "dictate/ui/MaExtraRow.kt")
tkl = code(APP / "ime/text/keyboard/TextKeyboardLayout.kt")
screen = code(APP / "app/settings/dictate/MaKeyColorsScreen.kt")
prefs = code(APP / "app/AppPrefs.kt")
order = code(APP / "app/settings/MaSettingsOrder.kt")
order_screen = code(APP / "app/settings/MaSettingsOrderScreen.kt")
routes = code(APP / "app/Routes.kt")
box = code(SNYGG / "SnyggBox.kt")
text = code(SNYGG / "SnyggText.kt")
icon = code(SNYGG / "SnyggIcon.kt")

# ---------------------------------------------------------------- the store
check("one preference", 'key = "dictate__ma_key_colors"' in prefs)
check("letters keep their colour through shift", "Character.toLowerCase(code)" in kc)
check("the number row is by position", 'fun idForNumberRow(index: Int): String = "n$index"' in kc,
      "a colour on a label would jump when the set changes")
check("a bad entry is skipped, never thrown", "?: continue" in kc)
check("sixteen swatches", kc.count("Swatch(\"") == 16)
check("the palette is bright enough to light a letter on a dark key",
      all(int(h[0:2], 16) >= 0x80 or int(h[2:4], 16) >= 0x80 or int(h[4:6], 16) >= 0x80
          for h in re.findall(r'Swatch\("[A-Za-z]+", 0xFF([0-9A-Fa-f]{6})', kc)),
      "a dim colour on a dark keycap is a letter he cannot read")

# ---------------------------------------------------------------- the keyboard draws it
check("SnyggBox takes a background override", "backgroundOverride: Color? = null" in box)
check("drawn inside the margin and the shape", "Modifier.background(backgroundOverride, style.shape())" in box)
check("text colour can follow", "colorOverride ?: style.foreground()" in text)
check("icon tint can follow", "tintOverride ?: style.foreground()" in icon)
check("main keys read the colours", "MaKeyColors.idForCode(key.computedData.code)" in tkl)
# G815: the LETTER is lit, the keycap keeps the theme (Marko, 21.10.2026: "color the letters itself").
check("main keys keep the theme's keycap", "backgroundOverride" not in tkl)
check("main keys light the legend", "val maForeground = maKeyColors[MaKeyColors.idForCode(key.computedData.code)]?.let { Color(it) }" in tkl)
check("labels follow", tkl.count("colorOverride = maForeground") >= 2)
check("icons follow", "tintOverride = maForeground" in tkl)
check("number row keeps the theme's keycap", "val background = style.background(" in row
      and "val foreground = paint?.let { Color(it) }" in row)
check("number row reads the colours", "colors[MaKeyColors.idForNumberRow(index)]" in row)

# ---------------------------------------------------------------- the feature rows
fr = code(APP / "dictate/ui/MaFeatureRow.kt")
legacy = code(APP / "dictate/ui/LegacyDictateLayout.kt")
check("ThemedKey reads the paint", "val paint = LocalMaKeyPaint.current" in legacy)
check("feature keys light the legend, not the key", "val fg = paint?.let { Color(it) }" in legacy
      and "val bg = style.background(" in legacy)
check("ThemedKey keeps its OptIn", "@OptIn(ExperimentalFoundationApi::class)\n@Composable\ninternal fun ThemedKey(" in legacy,
      "the paint declaration must not sit between the annotation and the function")
check("every feature key is wrapped", "LocalMaKeyPaint provides maKeyColors[MaKeyColors.idForFeature(maFeatureToken(button))]" in fr)
check("one token function, shared", "internal fun maFeatureToken(" in fr and "maFeatureToken(button)" in screen)
check("the grid shows feature rows", "Feature row ${rowIndex + 1}" in screen)
check("with the keyboard's own glyphs", "MaButtonGlyph(button, macroSlots, tintOverride = foreground)" in screen)

# ---------------------------------------------------------------- the third line
check("the command is printed under the digit", "MaNumericSecondary.preview(it)" in row)
check("two small lines at most", "maxLines = 2" in row)
check("the digit is lifted to make room", "BiasAlignment(0f, -0.8f)" in row)
check("preview skips what the corner already says", "it != hint(value)" in ns)
check("special keys shown as glyphs", "SPECIAL_KEYS.firstOrNull { it.token.equals(m.value, ignoreCase = true) }?.glyph" in ns)

# ---------------------------------------------------------------- the settings screen
check("the screen exists", "fun MaKeyColorsScreen()" in screen)
check("the grid is the real layout", "keyboardManager.lastCharactersEvaluator" in screen)
check("specials show their icon", "key.foregroundImageVector" in screen)
check("digits show their macro", "MaNumericSecondary.preview(it)" in screen)
check("routed", "Settings.MaKeyColors::class) { MaKeyColorsScreen() }" in routes)
check("listed in settings", "MaSettingsEntry.KEY_COLORS," in order)
check("has an icon", "MaSettingsEntry.KEY_COLORS -> Icons.Default.ColorLens" in order_screen)
check("has a route", "MaSettingsEntry.KEY_COLORS -> Routes.Settings.MaKeyColors" in order_screen)

print(f"key colours, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print("  FAIL ", f)
sys.exit(1 if failures else 0)
