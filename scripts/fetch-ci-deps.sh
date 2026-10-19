#!/usr/bin/env bash
# Fetch Hexcasting + PAUCAL jars (NeoForge + Fabric) from BlameJared CI lastSuccessfulBuild,
# plus the Fabric/NeoForge Cloth Config builds used for AutoConfig.
#
# Mapping notes:
#   - NeoForge Hex/PAUCAL jars are named (Mojang) mappings -> plain `implementation`.
#   - Fabric Hex/PAUCAL/Cloth jars are intermediary-mapped -> `modImplementation` (Loom remaps).
#   - The *-common-* artifacts are intermediary-mapped duplicates and are intentionally NOT
#     fetched: the build never puts them on a classpath.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/common/libs"
mkdir -p "$OUT"
UA='Mozilla/5.0'
HEX_BASE='https://ci.blamejared.com/job/petrakat/job/Hexcasting/job/1.21/lastSuccessfulBuild/artifact'
PAU_BASE='https://ci.blamejared.com/job/petrakat/job/PAUCAL/job/1.21/lastSuccessfulBuild/artifact'
HEX_VER='0.12.0-devel-pre-64'
PAU_VER='0.7.1-pre-27+1.21.1'
CLOTH_VER='15.0.140'
CLOTH_FABRIC_URL="https://cdn.modrinth.com/data/9s6osm5g/versions/HpMb5wGb/cloth-config-${CLOTH_VER}-fabric.jar"
CLOTH_NEOFORGE_URL="https://cdn.modrinth.com/data/9s6osm5g/versions/izKINKFg/cloth-config-${CLOTH_VER}-neoforge.jar"

rm -f "$OUT"/hexcasting-*.jar "$OUT"/paucal-*.jar "$OUT"/cloth-config-*.jar

# --- NeoForge (named mappings) ---
curl -fsL -A "$UA" -o "$OUT/hexcasting-neoforge-1.21.1-${HEX_VER}.jar" \
  "$HEX_BASE/Neoforge/build/libs/hexcasting-neoforge-1.21.1-${HEX_VER}.jar"
curl -fsL -A "$UA" -o "$OUT/paucal-${PAU_VER}-neoforge.jar" \
  "$PAU_BASE/Neoforge/build/libs/paucal-${PAU_VER}-neoforge.jar"

# --- Fabric (intermediary mappings, remapped by Loom) ---
curl -fsL -A "$UA" -o "$OUT/hexcasting-fabric-1.21.1-${HEX_VER}.jar" \
  "$HEX_BASE/Fabric/build/libs/hexcasting-fabric-1.21.1-${HEX_VER}.jar"
curl -fsL -A "$UA" -o "$OUT/paucal-${PAU_VER}-fabric.jar" \
  "$PAU_BASE/Fabric/build/libs/paucal-${PAU_VER}-fabric.jar"

# --- Cloth Config (AutoConfig) ---
curl -fsL -A "$UA" -o "$OUT/cloth-config-${CLOTH_VER}-fabric.jar" "$CLOTH_FABRIC_URL"
curl -fsL -A "$UA" -o "$OUT/cloth-config-${CLOTH_VER}-neoforge.jar" "$CLOTH_NEOFORGE_URL"

echo "Installed into $OUT"
ls -lh "$OUT"
