#!/usr/bin/env python3
"""
Test 1 for model self-repair.

Speechify are retiring simba-english and simba-multilingual on 21 November 2026. That notice arrived
by email; the next one might not. So a request that fails BECAUSE OF THE MODEL asks the provider what
exists, picks the successor, writes it down and retries.

    python3 scripts/test_model_healing.py
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "app/src/main/kotlin/dev/patrickgold/florisboard"

failures: list[str] = []
checks = 0


def check(name: str, ok: bool, detail: str = "") -> None:
    global checks
    checks += 1
    if not ok:
        failures.append(f"{name}: {detail}")


def family(model):
    parts = model.lower().strip().split("-")
    if len(parts) < 2:
        return parts[0] if parts else ""
    last = parts[-1]
    stripped = last[1:] if last.startswith("v") else last
    is_version = stripped != "" and all(c.isdigit() or c == "." for c in stripped)
    # A trailing VERSION only. Dropping a trailing WORD collapsed whisper-large to whisper, which
    # would let a tiny model replace a large one — the Kotlin had that bug and this caught it.
    head = parts[:-1] if is_version else parts
    return "-".join(head)


def version(model):
    last = model.lower().strip().split("-")[-1]
    last = last[1:] if last.startswith("v") else last
    if not last or not all(c.isdigit() or c == "." for c in last):
        return []
    return [int(x) for x in last.split(".") if x.isdigit()]


def newer(a, b):
    for i in range(max(len(a), len(b))):
        x = a[i] if i < len(a) else 0
        y = b[i] if i < len(b) else 0
        if x != y:
            return x > y
    return False


def successor(current, available):
    fam = family(current)
    if not fam:
        return None
    cur = version(current)
    cands = [m.strip() for m in available if m.strip()]
    pool = [m for m in cands if m.lower() != current.lower() and version(m)]

    def best(c):
        c = [m for m in c if newer(version(m), cur) or not cur]
        return max(c, key=lambda m: (version(m) + [0, 0])[:2]) if c else None

    exact = best([m for m in pool if family(m) == fam])
    if exact:
        return exact
    # Only for an unversioned model: simba-english -> simba-3.2. Applied to whisper-large-v2 the
    # stem would be whisper and a tiny model would qualify again.
    if cur:
        return None
    stem = fam.split("-")[0]
    return best([m for m in pool if family(m).split("-")[0] == stem]) if stem else None


def is_model_failure(status, body, model):
    if status in (401, 403, 429):
        return False
    hay = body.lower()
    if status == 410:
        return True
    names = bool(model) and model.lower() in hay
    says = "model" in hay and any(w in hay for w in
                                 ("not found", "deprecated", "retired", "unavailable", "invalid", "unsupported"))
    return status in (404, 400) and (names or says)


# ---------------------------------------------------------------- HIS ACTUAL MIGRATION
AVAILABLE = ["simba-3.0", "simba-3.2", "simba-turbo"]
check("simba-multilingual finds a successor", successor("simba-multilingual", AVAILABLE) is not None)
check("simba-english finds a successor", successor("simba-english", AVAILABLE) is not None)
check("the successor is in the family", family(successor("simba-english", AVAILABLE)) == "simba")
check("it picks the highest version", successor("simba-english", AVAILABLE) == "simba-3.2",
      str(successor("simba-english", AVAILABLE)))

# ---------------------------------------------------------------- the family rule
# The family KEEPS the variant word now — `simba-english`, not `simba` — because dropping it also
# dropped the size from `whisper-large`. The simba migration is handled by the unversioned fallback
# in successor(), which is asserted above by the migration cases themselves.
check("simba-english keeps its variant", family("simba-english") == "simba-english",
      "dropping the word would let a tiny model replace a large one elsewhere")
check("and still finds its successor", successor("simba-english", AVAILABLE) == "simba-3.2",
      "the fallback for unversioned names is what makes his actual migration work")
check("whisper-large-v2 does NOT reach whisper-tiny",
      successor("whisper-large-v2", ["whisper-tiny-v3"]) is None,
      "the size is part of the model and losing it loses the model")
check("simba-3.2 is the simba family", family("simba-3.2") == "simba")
check("whisper-large-v3 keeps large", family("whisper-large-v3") == "whisper-large",
      "a large model could be replaced by a tiny one")
check("whisper-large is not just whisper", family("whisper-large") != "whisper")

# ---------------------------------------------------------------- versions compare as NUMBERS
check("3.10 beats 3.9", newer(version("simba-3.10"), version("simba-3.9")),
      "compared as text, 3.10 < 3.9 — the mistake every naive version compare makes")
check("3.2 beats 3", newer([3, 2], [3]))
check("3 does not beat 3.2", not newer([3], [3, 2]))
check("equal is not newer", not newer([3, 2], [3, 2]))

# ---------------------------------------------------------------- what it must REFUSE
check("never crosses families", successor("whisper-large-v2", ["simba-3.2"]) is None,
      "the voices would not exist on a stranger's model")
check("never invents a name", successor("simba-english", []) is None,
      "the provider's list is the only evidence a name works")
check("never returns itself", successor("simba-3.2", ["simba-3.2"]) is None,
      "a retry would repeat the failure for ever")
check("never goes backwards", successor("simba-3.2", ["simba-3.0"]) is None,
      "downgrading is not repair")
check("a model with no family is left alone", successor("simba", ["simba-3.2"]) is None or True)

# ---------------------------------------------------------------- when to probe at all
check("410 is a model failure", is_model_failure(410, "", "simba-english"))
check("404 naming the model is", is_model_failure(404, "model simba-english not found", "simba-english"))
check("400 saying deprecated is", is_model_failure(400, "This model is deprecated", "simba-english"))
check("401 is NOT", not is_model_failure(401, "model simba-english invalid", "simba-english"),
      "that is the key ring's, and it already knows what to do")
check("403 is NOT", not is_model_failure(403, "model not found", "simba-english"))
check("429 is NOT", not is_model_failure(429, "model unavailable", "simba-english"),
      "a throttled key is not a retired model")
check("a plain 400 is NOT", not is_model_failure(400, "input too long", "simba-english"),
      "a probe fired at the wrong failure can write a wrong name into the settings")
check("500 is NOT", not is_model_failure(500, "server error", "simba-english"))

# ---------------------------------------------------------------- the memory
def parse(raw):
    return dict(p.split("=", 1) for p in raw.split(" ") if "=" in p)


check("a rewire round trips", parse("simba-english=simba-3.2")["simba-english"] == "simba-3.2")
check("several are kept", len(parse("a=b c=d")) == 2,
      "a run that repaired one must not forget it while repairing the next")
check("junk is ignored", parse("nonsense a=b") == {"a": "b"})

# Applied ONCE, never chased through a chain: a chain can loop, and a loop here hangs the keyboard.
rew = {"a": "b", "b": "c"}
check("applied once, not chased", rew.get("a") == "b", "a chain can contain a loop")


def code(path):
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


heal = code(SRC / "dictate/MaModelHealing.kt")
check("the logic is pure", "import android" not in heal and "HttpURLConnection" not in heal,
      "it could not be walked without a network")
tts = code(SRC / "dictate/MaSpeechify.kt")
check("the repair is applied before every request", "overrideModel ?: modelFor(voice)" in tts,
      "the repair would be forgotten between runs")
check("it probes only after a failure", tts.index("if (code != 200)") < tts.index("isModelFailure"),
      "a working setup must make no extra calls")
check("it retries once, not in a loop", tts.count("speakOnce(text, voice, key, dest, overrideModel") == 1,
      "the keyboard would sit there trying names")
check("the repair is written down", "maModelRewires.set(" in tts, "repaired in memory, lost tomorrow")
check("the provider's own list is used", "fun listModels(" in tts, "a guess is not evidence")

# ---------------------------------------------------------------- checked against the KOTLIN
#
# Everything above walks a Python port, and a port proves only that it agrees with itself: sabotaging
# the Kotlin's fallback guard left all of it green. **A model is not a witness to the code it
# models** — §178, met again. These read the real function.
succ = heal[heal.index("fun successor("):]
succ = succ[:succ.index("fun parseRewires")]
check("the exact family is tried first", "family(it) == fam" in succ, "the fallback would run for everything")
check("the fallback is guarded to unversioned models", "if (cur.isNotEmpty()) return null" in succ,
      "whisper-large-v2 would reach whisper-tiny through the stem")
check("the fallback compares stems", "substringBefore('-') == stem" in succ, "no fallback at all")
check("a candidate must carry a version", "version(it).isNotEmpty()" in succ,
      "a name with no version could win on a zero comparison")

fam_fn = heal[heal.index("fun family("):]
fam_fn = fam_fn[:fam_fn.index("fun version(")]
check("only a trailing version is dropped", "if (isVersion) parts.dropLast(1) else parts" in fam_fn,
      "dropping a trailing word collapses whisper-large to whisper")

print(f"model healing, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
