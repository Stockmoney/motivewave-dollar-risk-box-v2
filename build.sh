#!/usr/bin/env bash
# Builds build/DollarRiskBoxV2.jar from the sources.
#
#   bash build.sh            build only
#   bash build.sh install    build, then copy into your "MotiveWave Extensions" folder
#
# Needs a JDK (17+; the jar is compiled for Java 21 bytecode) and the MotiveWave SDK jar,
# which ships inside your MotiveWave installation (it is NOT part of this repository).
# Override locations with environment variables if yours differ:
#   MW_SDK   path to mwave_sdk.jar   (default: macOS app bundle)
#   MW_EXT   extensions folder       (default: ~/MotiveWave Extensions)
#   JAVA_HOME
set -euo pipefail
cd "$(dirname "$0")"

SDK="${MW_SDK:-/Applications/MotiveWave.app/Contents/Java/mwave_sdk.jar}"
EXT="${MW_EXT:-$HOME/MotiveWave Extensions}"

# macOS ships a /usr/bin/javac stub that fails without a JDK, so every candidate is test-run.
JAVAC=""; JAR=""; JAVA=""
candidates=()
[[ -n "${JAVA_HOME:-}" ]] && candidates+=("$JAVA_HOME/bin")
if [[ -x /usr/libexec/java_home ]]; then
  jh="$(/usr/libexec/java_home 2>/dev/null || true)"; [[ -n "$jh" ]] && candidates+=("$jh/bin")
fi
candidates+=(/opt/homebrew/opt/openjdk/bin /usr/local/opt/openjdk/bin)
if command -v javac >/dev/null 2>&1; then candidates+=("$(dirname "$(command -v javac)")"); fi
for d in "${candidates[@]}"; do
  if [[ -x "$d/javac" && -x "$d/jar" && -x "$d/java" ]] && "$d/javac" -version >/dev/null 2>&1; then
    JAVAC="$d/javac"; JAR="$d/jar"; JAVA="$d/java"; break
  fi
done
[[ -n "$JAVAC" ]] || { echo "No working JDK found. Install one (e.g. 'brew install openjdk') or set JAVA_HOME." >&2; exit 1; }
[[ -f "$SDK" ]] || { echo "MotiveWave SDK jar not found: $SDK (set MW_SDK)" >&2; exit 1; }

rm -rf build && mkdir -p build/classes/dollar_risk_box_v2/nls
# JavaFX ships beside the SDK jar (Contents/javafx); the study uses it to catch the Escape key.
FX="$(dirname "$SDK")/../javafx"
CP="$SDK"; for j in "$FX"/javafx.*.jar; do [[ -f "$j" ]] && CP="$CP:$j"; done
"$JAVAC" --release 21 -encoding UTF-8 -Xlint:all,-auxiliaryclass -cp "$CP" -d build/classes $(find dollar_risk_box_v2 -name "*.java")
cp dollar_risk_box_v2/nls/strings.properties build/classes/dollar_risk_box_v2/nls/
# tests of the pure parts (no MotiveWave needed to run them)
mkdir -p build/test-classes
"$JAVAC" --release 21 -encoding UTF-8 -Xlint:all -cp build/classes -d build/test-classes $(find test -name "*.java")
"$JAVA" -cp build/classes:build/test-classes dollar_risk_box_v2.Tests
"$JAR" cf build/DollarRiskBoxV2.jar -C build/classes .
echo "built build/DollarRiskBoxV2.jar"

if [[ "${1:-}" == "install" ]]; then
  mkdir -p "$EXT"
  # A new file name each time: a running MotiveWave caches an open jar by path, so a replaced
  # jar with the same name would load its classes but not its text resources until a restart.
  rm -f "$EXT"/DollarRiskBoxV2-*.jar   # only V2's own jars: V1 (DollarRiskBox-<ts>.jar) stays
  cp build/DollarRiskBoxV2.jar "$EXT/DollarRiskBoxV2-$(date +%s).jar"
  touch "$EXT/.last_updated"   # MotiveWave rescans the folder when this file changes
  echo "installed into: $EXT"
fi
