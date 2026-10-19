#!/usr/bin/env bash
# Fetch Hexcasting + PAUCAL NeoForge jars from BlameJared CI lastSuccessfulBuild
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/common/libs"
mkdir -p "$OUT"
UA='Mozilla/5.0'
HEX_BASE='https://ci.blamejared.com/job/petrakat/job/Hexcasting/job/1.21/lastSuccessfulBuild/artifact'
PAU_BASE='https://ci.blamejared.com/job/petrakat/job/PAUCAL/job/1.21/lastSuccessfulBuild/artifact'
HEX_VER='0.12.0-devel-pre-64'
PAU_VER='0.7.1-pre-27+1.21.1'
rm -f "$OUT"/hexcasting-*.jar "$OUT"/paucal-*.jar
curl -fsL -A "$UA" -o "$OUT/hexcasting-neoforge-1.21.1-${HEX_VER}.jar" \
  "$HEX_BASE/Neoforge/build/libs/hexcasting-neoforge-1.21.1-${HEX_VER}.jar"
curl -fsL -A "$UA" -o "$OUT/hexcasting-common-1.21.1-${HEX_VER}.jar" \
  "$HEX_BASE/Common/build/libs/hexcasting-common-1.21.1-${HEX_VER}.jar"
curl -fsL -A "$UA" -o "$OUT/paucal-${PAU_VER}-neoforge.jar" \
  "$PAU_BASE/Neoforge/build/libs/paucal-${PAU_VER}-neoforge.jar"
curl -fsL -A "$UA" -o "$OUT/paucal-${PAU_VER}-common.jar" \
  "$PAU_BASE/Common/build/libs/paucal-${PAU_VER}-common.jar"
echo "Installed into $OUT"
ls -lh "$OUT"
