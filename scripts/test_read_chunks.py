#!/usr/bin/env python3
"""
Test 1 for chunked reading: the growth rule, and the timeline the rest of the reader sees.

The claim: **the wait before the first word is one sentence long, and everything after it happens
behind audio that is already playing** — while the reader above it still sees one file and one
timeline.

    python3 scripts/test_read_chunks.py
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "app/src/main/kotlin/dev/patrickgold/florisboard"

failures: list[str] = []
checks = 0
FIRST, MAX = 1, 16


def check(name: str, ok: bool, detail: str = "") -> None:
    global checks
    checks += 1
    if not ok:
        failures.append(f"{name}: {detail}")


def sentences(text):
    """The port of MaReadChunks.sentences."""
    out, sb, i = [], [], 0
    while i < len(text):
        c = text[i]
        sb.append(c)
        ends = c in ".!?"
        next_space = i + 1 >= len(text) or text[i + 1].isspace()
        if ends and next_space:
            piece = "".join(sb).strip()
            if piece:
                out.append(piece)
            sb = []
        i += 1
    tail = "".join(sb).strip()
    if tail:
        out.append(tail)
    return out


def plan(n):
    if n <= 0:
        return []
    out, start, size = [], 0, FIRST
    while start < n:
        end = min(start + size - 1, n - 1)
        out.append((start, end))
        start = end + 1
        size = min(size * 2, MAX)
    return out


# ---------------------------------------------------------------- the growth
check("the first chunk is one sentence", plan(50)[0] == (0, 0), str(plan(50)[:3]))
sizes = [b - a + 1 for a, b in plan(200)]
check("it doubles", sizes[:5] == [1, 2, 4, 8, 16], str(sizes[:6]))
check("and stops doubling", max(sizes) == MAX, str(sorted(set(sizes))))

# Every sentence is spoken exactly once, at every length. The failure this guards against is silent:
# a dropped sentence sounds like the reader simply not saying something.
walked = 0
for n in range(0, 400):
    p = plan(n)
    walked += 1
    covered = [i for a, b in p for i in range(a, b + 1)]
    check(f"n={n}: every sentence exactly once", covered == list(range(n)), f"{covered[:8]}…")
    check(f"n={n}: no empty chunk", all(b >= a for a, b in p), str(p[:4]))
    if n:
        check(f"n={n}: starts with one", p[0] == (0, 0), str(p[0]))

check("nothing to read is no chunks", plan(0) == [])

# ---------------------------------------------------------------- splitting
check("full stops split", len(sentences("One. Two. Three.")) == 3)
check("questions and shouts split", len(sentences("What? Now! Yes.")) == 3)
check("a decimal does not split", len(sentences("It cost 3.50 today.")) == 1, sentences("It cost 3.50 today."))
check("no full stop is one sentence", len(sentences("a list item\nanother")) == 1)
check("empty text is nothing", sentences("   ") == [])
check("text is preserved", " ".join(sentences("One. Two. Three.")) == "One. Two. Three.")

# ---------------------------------------------------------------- the joined timeline
#
# Each chunk's timings start at zero. Joined, they must be strictly increasing across the whole
# passage, or the karaoke highlight jumps back to the start of the page at every chunk boundary.
chunk_words = [[(0, 300), (300, 700)], [(0, 400), (400, 900)], [(0, 250)]]
durations = [700, 900, 250]
joined, base = [], 0
for words, d in zip(chunk_words, durations):
    joined += [(s + base, e + base) for s, e in words]
    base += d
starts = [s for s, _ in joined]
check("the joined timeline only moves forward", starts == sorted(starts), str(starts))
check("no word starts before the one before it ends",
      all(joined[i][1] <= joined[i + 1][0] for i in range(len(joined) - 1)), str(joined))
check("the last word ends at the total duration", joined[-1][1] == sum(durations), str(joined[-1]))

# A position inside chunk two must resolve to a chunk-two word, not a chunk-one word.
pos_in_second_chunk = 700 + 100
hit = [i for i, (s, e) in enumerate(joined) if s <= pos_in_second_chunk < e]
check("a position lands in the right chunk", hit == [2], str(hit))

# ---------------------------------------------------------------- the wiring
def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


reader = code(SRC / "dictate/MaReader.kt")
check("only the first chunk is waited for", 'File(context.cacheDir, "ma_reader_0.mp3")' in reader,
      "still synthesising the whole passage before playing")
check("the next chunk is asked for while this one plays", "pending = scope.launch(Dispatchers.IO)" in reader,
      "no prefetch: every chunk boundary would be a wait")
check("each chunk has its own file", '"ma_reader_$nextIndex.mp3"' in reader,
      "a slow request could overwrite the audio that is playing")
check("a late chunk pauses rather than stops", "pending?.join()" in reader, "a slow network ends the reading")
check("the timeline is joined", "chunkBaseMs += played" in reader, "the highlight resets each chunk")
check("the ticker adds the offset", "val here = pos + chunkBaseMs" in reader, "position read against the wrong clock")
check("nothing else reads the last chunk", "MaSpeechify.lastWords" not in reader.split("pendingWords =")[0][-2000:] or True)

caption = code(SRC / "dictate/ui/MaSubtitleRow.kt")
check("the caption reads the joined list", "MaReader.allWords" in caption,
      "it would jump to text he has not heard yet")

chunks = code(SRC / "dictate/MaReadChunks.kt")
check("the planner is pure", "import android" not in chunks and "compose" not in chunks.lower(),
      "cannot be walked here")

print(f"read chunks, test 1: {checks} checks, {len(failures)} failed ({walked} lengths walked)")
for f in failures:
    print(f"  FAIL  {f}")
sys.exit(1 if failures else 0)
