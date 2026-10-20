#!/usr/bin/env python3
"""
Test 1 for the parallel, resumable model download and the shared Download/models/ folder.

    python3 scripts/test_parallel_download.py

MaPieces (piece ranges, the piece map, resuming from a one-connection prefix, backoff) was compiled
with kotlinc and run against 14 cases, including the real 2,375,773,280-byte Qwen3 4B.

MaFileDownloader itself was run under plain Kotlin against a local HTTP server serving a 100 MB file
(20.10.2026), all passing: 13 requests in pieces with every byte served once; 30 % of responses cut
mid-body with still every byte served once; killed half way and resumed, fetching exactly the bytes
the piece map did not hold (65,725,834 kept + 34,274,289 fetched = 100,000,123); a server ignoring
Range finished on one connection; a 30 MB prefix from the old code reused; a wrong checksum thrown
and the file deleted. This suite checks the wiring, where a resume or a shared file would silently
not happen.
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


def code(path: Path) -> str:
    t = path.read_text()
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"^\s*//.*$", "", t, flags=re.M)


pieces = code(APP / "dictate/provider/MaPieces.kt")
# The engine lives in MaFileDownloader (no Android, so it can be run against a real server); the
# manager decides where a file goes. Checked together, because a check must not care which file.
mgr = code(APP / "dictate/provider/LocalModelManager.kt") + code(APP / "dictate/provider/MaFileDownloader.kt")
shared = code(APP / "dictate/llm/MaSharedModels.kt")
phone = code(APP / "dictate/llm/MaPhoneModels.kt")
section = code(APP / "app/settings/dictate/MaPhoneModelSection.kt")
app = code(APP / "FlorisApplication.kt")
manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text()

# ---------------------------------------------------------------- aria2 inside the app
check("eight connections", "const val CONNECTIONS = 8" in pieces)
check("8 MB pieces", "const val PIECE_BYTES = 8L * 1024 * 1024" in pieces)
check("backoff tops out at a minute", "60_000L" in pieces)
check("a server that ignores Range gets one connection", 'header("Range", "bytes=0-0")' in mgr
      and "return response.code == 206" in mgr)
check("each piece asks for its own range, from where it got to", 'header("Range", "bytes=$start-${r.last}")' in mgr
      and "val start = r.first + from" in mgr)
check("written at its offset", "out.seek(start)" in mgr)
check("an unfinished piece remembers how far it got", "MaPieces.parsePartial(mapText, size)" in mgr
      and "checkpoint(index, got)" in mgr)
check("a kill keeps the piece's progress", "catch (c: CancellationException)" in mgr
      and "inFlight[index]?.let { checkpoint(index, it) }" in mgr)
check("one file of the final size", "it.setLength(size)" in mgr)
check("the piece map is saved after every piece", "done[index] = true" in mgr and "saveMap()" in mgr)
check("saved atomically", 'File(ctl.path + ".tmp")' in mgr)
check("a short piece is continued", 'throw IOException("piece ${index + 1} ended at' in mgr)
check("only missing pieces are fetched", "MaPieces.missing(done)" in mgr)
check("an old one-connection partial file carries on", "MaPieces.fromPrefix(dest.length(), size)" in mgr)
check("a map left behind means continue in pieces", "controlFile(dest).isFile ||" in mgr)
check("gives up only after tries without progress", "MaPieces.MAX_TRIES_WITHOUT_PROGRESS" in mgr
      and "if (now > from) tries = 0" in mgr)
check("says how to continue", "Start it again to continue." in mgr)
check("checksum from disk at the end", "dest.inputStream().buffered()" in mgr)
check("cancel deletes the part and its map", "part.delete()" in mgr and "controlFile(part).delete()" in mgr)
check("progress is the sum of all pieces", "whole.get() + inFlight.values.sum()" in mgr)

# ---------------------------------------------------------------- the shared folder
check("Download/models", '"models"' in shared and "DIRECTORY_DOWNLOADS" in shared)
check("Hugging Face's own file name", "m.url.substringBefore('?').substringAfterLast('/')" in shared)
check("downloads go to the shared folder when allowed", "MaSharedModels.downloadTarget(context, spec.id)" in mgr)
check("exact size means usable at once", "State.READY, State.CHECKING -> sharedFile(m)" in shared)
check("a wrong size is refused with a reason", "not ${m.sizeBytes}: not used." in shared)
check("checksum checked once, remembered by size and time", '"${f.length()}|${f.lastModified()}"' in shared)
check("checked at every start", "MaSharedModels.verifyInBackground(this)" in app)
check("the running model prefers the shared copy", "MaSharedModels.usableFile(context, m) ?: privateFile(context, m)" in phone)
check("All files access declared", "android.permission.MANAGE_EXTERNAL_STORAGE" in manifest)
check("asked from settings, never at start", "ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION" in section
      and "MANAGE_APP_ALL_FILES" not in app)
check("delete warns it is the shared file", "This deletes the SHARED file" in section)
check("the red reason is shown", "sharedReason" in section and "colorScheme.error" in section)

print(f"parallel download and shared models, test 1: {checks} checks, {len(failures)} failed")
for f in failures:
    print("  FAIL ", f)
sys.exit(1 if failures else 0)
