#!/usr/bin/env python3
"""
Test 1 for the long-press pop-ups (MaPopups, Pop-up Configuration; Marko 22.10.2026).

    python3 scripts/test_popups.py

The storage format is modelled here in Python and walked against the cases that matter (the
Croatian default, several pop-ups, empty letters, tabs typed into a field); then the wiring is checked,
which is where a pop-up would silently not show: the preference, the keyboard's observer, the key's
rebuild, the settings entry, its route.
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "app/src/main/kotlin/dev/patrickgold/florisboard"
failures: list[str] = []
checks = 0


def check(name: str, ok: bool, detail: str = "") -> None:
    global checks
    checks += 1
    if not ok:
        failures.append(f"{name}: {detail}")


LETTERS = "abcdefghijklmnopqrstuvwxyz"


def clean(t: str) -> str:
    return t.replace("\t", " ").replace("\n", " ").replace("\r", " ").strip()


def parse(raw: str) -> dict:
    out = {}
    for line in (raw or "").split("\n"):
        parts = line.split("\t")
        key = parts[0].strip().lower()
        if len(key) != 1 or key not in LETTERS:
            continue
        pops = []
        for p in (x.strip() for x in parts[1:]):
            if p and p not in pops:
                pops.append(p)
        if pops:
            out[key] = pops
    return out


def serialize(m: dict) -> str:
    rows = []
    for k in sorted(m):
        if k not in LETTERS:
            continue
        v = []
        for p in (clean(x) for x in m[k]):
            if p and p not in v:
                v.append(p)
        if v:
            rows.append("\t".join([k] + v))
    return "\n".join(rows)


DEFAULT = "c\tč\tć\nd\tđ\ns\tš\nz\tž"
d = parse(DEFAULT)
check("default has the five Croatian letters", d == {"c": ["č", "ć"], "d": ["đ"], "s": ["š"], "z": ["ž"]}, str(d))
check("default round-trips", serialize(d) == DEFAULT, repr(serialize(d)))
check("an a has nothing", "a" not in d)
m = dict(d); m["l"] = ["lj", "ł", "lj"]
check("several pop-ups, duplicates dropped", parse(serialize(m))["l"] == ["lj", "ł"], serialize(m))
m["c"] = []
check("emptied letter is gone", "c" not in parse(serialize(m)))
m["n"] = ["n\tj"]
check("a tab cannot split a line", parse(serialize(m))["n"] == ["n j"], serialize(m))
check("garbage lines skipped", parse("??\tx\nab\ty\n\nc\tč") == {"c": ["č"]})

kotlin = (APP / "dictate/MaPopups.kt").read_text()
check("Kotlin default is the same five", all(s in kotlin for s in ['"c\\tč\\tć"', '"d\\tđ"', '"s\\tš"', '"z\\tž"']))
prefs = (APP / "app/AppPrefs.kt").read_text()
check("preference exists", "dictate__ma_popups" in prefs and "default = MaPopups.DEFAULT" in prefs)
km = (APP / "ime/keyboard/KeyboardManager.kt").read_text()
check("keyboard observes the preference", re.search(r"maPopups\.asFlow\(\).*?MaPopups\.current = MaPopups\.parse", km, re.S) is not None)
tk = (APP / "ime/text/keyboard/TextKey.kt").read_text()
check("a letter's popup is rebuilt from MaPopups", "MaPopups.forLabel(computed.label)" in tk and "computedPopups.clear()" in tk)
order = (APP / "app/settings/MaSettingsOrder.kt").read_text()
check("settings entry named as he said", '"Pop-up Configuration"' in order and '"Coloring the Buttons"' in order)
check("entry in DEFAULT", "MaSettingsEntry.POPUPS," in order)
routes = (APP / "app/Routes.kt").read_text()
check("route registered", "composableWithDeepLink(Settings.MaPopups::class) { MaPopupsScreen() }" in routes)

print(f"{checks} checks, {len(failures)} failed")
for f in failures:
    print("  FAIL", f)
sys.exit(1 if failures else 0)
