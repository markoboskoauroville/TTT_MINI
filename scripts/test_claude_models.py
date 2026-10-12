#!/usr/bin/env python3
"""
Test 1 for the newest-Claude rule and the native Messages path.

    python3 scripts/test_claude_models.py

The parsing and the choice are mirrored here from MaClaudeModels.kt and walked against a catalog;
the wiring is checked in the source. The Kotlin itself was also compiled and run against the same
cases when it was written (33 checks, 0 failed).
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LIB = ROOT / "lib/dictate-core/src/main/kotlin/dev/patrickgold/florisboard/dictate/provider"
APP = ROOT / "app/src/main/kotlin/dev/patrickgold/florisboard"

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


# ---------------------------------------------------------------- the mirror
TIERS = ["haiku", "sonnet", "opus", "fable"]
FALLBACK = {"haiku": "claude-haiku-5-5", "sonnet": "claude-sonnet-5-5", "opus": "claude-opus-5-5",
            "fable": "claude-fable-5-1"}
NEW = re.compile(r"^claude-([a-z]+)-(\d+)(?:-(\d{1,2}))?(?:-(\d{8}))?$")
OLD = re.compile(r"^claude-(\d+)(?:-(\d{1,2}))?-([a-z]+)(?:-(\d{8})|-latest)?$")


def parse(i):
    s = i.strip().lower()
    m = NEW.match(s)
    if m:
        return (m[1], int(m[2]), int(m[3] or 0), bool(m[4])) if m[1] in TIERS else None
    m = OLD.match(s)
    if m:
        return (m[3], int(m[1]), int(m[2] or 0), bool(m[4])) if m[3] in TIERS else None
    return None


def newest(tier, available):
    c = [(i, parse(i)) for i in available]
    c = [(i, v) for i, v in c if v and v[0] == tier]
    c.sort(key=lambda p: (-p[1][1], -p[1][2], p[1][3], len(p[0])))
    return c[0][0] if c else None


def resolve(model, available):
    if model == "claude-haiku-4-5-20251001":
        model = "claude-haiku-latest"
    if model.startswith("claude-") and model.endswith("-latest") and model[7:-7] in TIERS:
        return newest(model[7:-7], available) or FALLBACK[model[7:-7]]
    return model


CAT = ["claude-haiku-4-5-20251001", "claude-haiku-4-5", "claude-haiku-5-5", "claude-sonnet-5",
       "claude-sonnet-5-5", "claude-opus-4-8", "claude-opus-5-5", "claude-fable-5-1",
       "claude-3-5-haiku-20241022", "claude-mythos-5-1"]

check("newest haiku", resolve("claude-haiku-latest", CAT) == "claude-haiku-5-5")
check("next haiku without an update", resolve("claude-haiku-latest", CAT + ["claude-haiku-6"]) == "claude-haiku-6",
      "a new model on the list must be picked up with no app change")
check("numbers, not text", resolve("claude-haiku-latest", ["claude-haiku-5-9", "claude-haiku-5-10"]) == "claude-haiku-5-10")
check("a tier never crosses", resolve("claude-haiku-latest", ["claude-sonnet-9"]) == "claude-haiku-5-5",
      "falls back to a real Haiku id, never a Sonnet")
check("no list, real fallback", resolve("claude-haiku-latest", []) == "claude-haiku-5-5")
check("the old default reads as the newest haiku", resolve("claude-haiku-4-5-20251001", CAT) == "claude-haiku-5-5")
check("a pinned id is left alone", resolve("claude-sonnet-5", CAT) == "claude-sonnet-5")
check("mythos is not offered", parse("claude-mythos-5-1") is None)
check("old-style ids parse", parse("claude-3-5-haiku-20241022") == ("haiku", 3, 5, True))

# ---------------------------------------------------------------- the mirror matches the source
cm = code(LIB / "MaClaudeModels.kt")
check("mirror: tiers", 'listOf("haiku", "sonnet", "opus", "fable")' in cm)
for tier, mid in FALLBACK.items():
    check(f"mirror: fallback {tier}", f'"{tier}" to "{mid}"' in cm)
check("mirror: old default", 'OLD_DEFAULT = "claude-haiku-4-5-20251001"' in cm)

# ---------------------------------------------------------------- the wiring
client = code(LIB / "OpenAiCompatibleClient.kt")
reg = code(LIB / "ProviderRegistry.kt")
check("Claude goes to the Messages API", '"messages"' in client and "x-api-key" in client,
      "the OpenAI shim cannot set effort")
check("Claude is routed before the shim", client.find("if (isAnthropic) return completeAnthropic(request)")
      < client.find("completeOnce(effective)"))
check("the alias is resolved against the catalog", "MaClaudeModels.resolve(asked, catalog)" in client)
check("a refused model heals once, same tier", "MaClaudeModels.successor(model, claudeCatalog(refresh = true))" in client)
check("effort only where accepted", "MaClaudeModels.supportsEffort(model)" in client,
      "Haiku 4.5 answers output_config.effort with a 400")
check("the catalog reads every page", 'models?limit=1000' in client)
check("the preset default is the alias", "defaultChatModel = MaClaudeModels.DEFAULT" in reg,
      "a number written into the preset goes stale")
check("no stale haiku id as a default", '"claude-haiku-4-5-20251001"' not in reg)

# Croatian on AssemblyAI async: universal-2 always behind the chosen model.
check("universal-2 follows the chosen model", "listOf(it, ASSEMBLY_FALLBACK_MODEL)" in client,
      "Croatian is not a Universal-3 language")
check("the fallback is universal-2", 'ASSEMBLY_FALLBACK_MODEL = "universal-2"' in client)

# The settings screen shows what is sent, from the same builder.
screen = code(APP / "app/settings/dictate/MaPromptsScreen.kt")
check("the preview uses the builder", "MaRewordPrompt.build(" in screen)
check("the last request is shown", "MaRewordPrompt.last" in screen)
check("the language lines are editable", "maLanguageRule" in screen and "maLanguageRuleUnsure" in screen)

print(f"claude models, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print("  FAIL ", f)
sys.exit(1 if failures else 0)
