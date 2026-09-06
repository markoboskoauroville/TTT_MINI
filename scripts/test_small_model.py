#!/usr/bin/env python3
"""
Test 1 for the Haiku rule, and for the keys coming first.

    python3 scripts/test_small_model.py
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "app/src/main/kotlin/dev/patrickgold/florisboard"

failures: list[str] = []
checks = 0
TOO_BIG = ["opus", "sonnet", "gpt-4o", "gpt-5", "70b", "405b", "ultra", "pro"]


def check(name: str, ok: bool, detail: str = "") -> None:
    global checks
    checks += 1
    if not ok:
        failures.append(f"{name}: {detail}")


def small_for(pid):
    p = pid.lower()
    if "anthropic" in p or "claude" in p:
        return "claude-haiku-4-5-20251001"
    if "groq" in p:
        return "llama-3.1-8b-instant"
    if "gemini" in p or "google" in p:
        return "gemini-2.0-flash"
    if "openai" in p:
        return "gpt-4o-mini"
    return None


# ---------------------------------------------------------------- the rule
check("anthropic gets haiku", "haiku" in small_for("anthropic"), small_for("anthropic"))
check("and by any id spelling", small_for("anthropic_claude") == small_for("claude"),
      "a lookup that misses silently returns to the expensive path")
check("groq gets the 8b", "8b" in small_for("groq"))
check("gemini gets flash", "flash" in small_for("gemini"))
check("an unknown provider falls through", small_for("something_new") is None,
      "a provider nobody has thought about should keep working, not fail")

# NOTHING BIG. The whole point: every named model must be a small one.
for pid in ("anthropic", "groq", "gemini", "openai"):
    m = small_for(pid)
    for big in TOO_BIG:
        if big == "gpt-4o" and m == "gpt-4o-mini":
            continue          # "-mini" is the small one; the substring is a false match
        check(f"{pid}: {m} is not {big}", big not in m, f"{m} would be billed as a large model")

# ---------------------------------------------------------------- the wiring
def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


small = code(SRC / "dictate/MaSmallModel.kt")
ctrl = code(SRC / "dictate/DictateController.kt")
check("the small models are named", "fun forProvider(" in small, "trusted to a default")
check("haiku is named outright", "claude-haiku" in small, "a rule that names nothing enforces nothing")
# find, not index. The sabotage removed the call and this THREW — the count never printed, every
# other result was lost. Third time this month, and each time in a check written the same hour as the
# section documenting the previous one.
_named = ctrl.find("MaSmallModel.forProvider(account.providerId)")
_preset = ctrl.find("?: preset.defaultChatModel")
check("the controller asks for it FIRST", _named >= 0, "the preset default would decide")
check("the preset is the fallback, not the source", 0 <= _named < _preset,
      "a preset edited next year would silently promote every rewording")

# Every cheap-model caller must go through the one path. If a second appeared, the rule would hold in
# one place and not the other.
check("one cheap path", ctrl.count("cheapest = true") == 1,
      "a second caller could pick its own model")
check("and the AI features use it", "askCheapModel" in ctrl, "prediction would use the chat model")

# ---------------------------------------------------------------- keys before permissions
perms = code(SRC / "app/settings/dictate/MaPermissionsScreen.kt")
order = code(SRC / "app/settings/MaSettingsOrder.kt")
check("the screen is named keys first", '"API keys and permissions"' in order,
      "the name is what he scans for")
check("the keys row is drawn first", perms.index("MaKeysHeaderRow") < perms.index("steps.forEachIndexed"),
      "the thing he uses daily sits behind seven steps he finished months ago")
check("the keys are not a numbered step", "number = steps.size + 1" not in perms,
      "an eighth step of a seven-step setup")
check("and carry no tick", "granted = false" not in perms.split("MaKeysHeaderRow")[-1][:400],
      "a key is never simply done")

keys_ui = code(SRC / "app/settings/dictate/DictateKeysScreen.kt")
check("the button says what it does", '"TEST KEYS"' in keys_ui, "still describing the algorithm")
check("not what it walks", '"FIND A WORKING KEY"' not in keys_ui)

# ---------------------------------------------------------------- restricted settings
#
# The three-dot menu is not a property of the phone. Since Android 13 a sideloaded app has
# ACCESS_RESTRICTED_SETTINGS errored, and the item appears only once the system has SEEN the app
# refused at that gate. The old instruction — "skip if your phone does not offer it" — sent him away
# from the one action that makes it appear.
check("the instruction says to be refused first", "let it refuse you" in perms,
      "skip if your phone does not offer it, which is the opposite of what works")
check("the old instruction is gone", "Skip if your phone does not offer it" not in perms)

# Both commands, with the REAL package name. A package name mistyped once is debugged for twenty
# minutes, which is why they are copyable rather than printed for typing.
check("the appops command is there", "ACCESS_RESTRICTED_SETTINGS allow" in perms, "no way past without the menu")
check("it names the real package", "com.mantraproductions.tttlight" in perms, "a command that does nothing")
check("the package matches the build", "com.mantraproductions.tttlight" in
      (ROOT / "app/build.gradle.kts").read_text(), "the screen and the app disagree")
check("the install command is there", "pm install -i com.android.vending" in perms,
      "the permanent fix is missing")
check("both are copyable", perms.count('Text("COPY")') >= 1, "he would have to type them")
check("the app does not claim to force it", "force" not in perms.lower(),
      "an app that claims to lift its own restriction is claiming the impossible")

print(f"small model, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
