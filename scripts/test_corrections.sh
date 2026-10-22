#!/bin/sh
# Runs the correction-learning and forgetting-curve tests on the Mac, with the Kotlin compiler that
# Gradle already downloaded (no kotlinc install). MaCorrections.kt, MaNgramModel.kt, MaPopups.kt are pure.
set -e
C=~/.gradle/caches/modules-2/files-2.1
jar() { find "$C" -path "*$1*" -name "*.jar" ! -name "*sources*" | head -1; }
CP="$(jar kotlin-compiler-embeddable/2.0.21):$(jar kotlin-stdlib/2.0.21):$(jar kotlin-script-runtime/2.0.21):$(jar kotlin-reflect/1.6.10):$(jar kotlin-daemon-embeddable/2.0.21):$(jar trove4j):$(jar kotlinx-coroutines-core-jvm):$(jar annotations/13.0)"
STD="$(jar kotlin-stdlib/2.0.21)"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; N="$ROOT/app/src/main/kotlin/dev/patrickgold/florisboard/dictate"
OUT="$(mktemp -d)"
java -cp "$CP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -cp "$STD" -d "$OUT" \
  "$N/nlp/MaCorrections.kt" "$N/nlp/MaNgramModel.kt" "$ROOT"/scripts/kotlin_tests/*.kt 2>&1 | grep -E "error" || true
java -cp "$OUT:$STD" MainKt
