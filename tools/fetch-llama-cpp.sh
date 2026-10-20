#!/usr/bin/env bash
#
# Fetches llama.cpp, the engine that runs the style model on the phone (MaPhoneStyle, §191).
#
# Source, not binaries: app/src/main/cpp/CMakeLists.txt compiles it into libmallm.so together with
# the small JNI bridge (ma_llm.cpp). Pinned to one release tag AND its commit, so a moved tag fails
# the build instead of quietly changing the engine. Not committed (see .gitignore).
#
# Usage:  tools/fetch-llama-cpp.sh [--force]
#
set -euo pipefail

TAG="b11400"
COMMIT="0bb496dbd3af0add77ff82c406a915b41e839d56"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DST="$REPO_ROOT/app/src/main/cpp/llama.cpp"

if [[ "${1:-}" != "--force" && -f "$DST/.ma-commit" && "$(cat "$DST/.ma-commit")" == "$COMMIT" ]]; then
  echo "✓ llama.cpp $TAG already present (use --force to re-fetch)."
  exit 0
fi

rm -rf "$DST"
git clone --quiet --depth 1 --branch "$TAG" https://github.com/ggml-org/llama.cpp "$DST"
GOT="$(git -C "$DST" rev-parse HEAD)"
if [[ "$GOT" != "$COMMIT" ]]; then
  echo "llama.cpp tag $TAG is $GOT, expected $COMMIT. Refusing a moved tag." >&2
  rm -rf "$DST"
  exit 1
fi
rm -rf "$DST/.git"
echo "$COMMIT" > "$DST/.ma-commit"
echo "✓ llama.cpp $TAG ($COMMIT) in app/src/main/cpp/llama.cpp"
