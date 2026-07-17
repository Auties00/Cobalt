#!/usr/bin/env bash
#
# Regenerates modules/lib/src/main/resources/META-INF/native-checksums.json
# from the single combined library under
# modules/lib/natives/bin/<classifier>/. The version field is read from the
# root pom.xml; the commitSha pins the GitHub download URL to the immutable
# HEAD of the run that wrote the manifest.

set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$DIR/../.." && pwd)"
NATIVES_BIN="$ROOT/modules/lib/natives/bin"
MANIFEST="$ROOT/modules/lib/src/main/resources/META-INF/native-checksums.json"

VERSION=$(grep -m1 '<version>' "$ROOT/pom.xml" | sed 's/[<\/]*version[> ]*//g' | tr -d ' \r')
COMMIT_SHA=$(git -C "$ROOT" rev-parse HEAD)

CLASSIFIERS=(linux-x86_64 linux-aarch64 windows-x86_64 windows-aarch64 darwin-x86_64 darwin-aarch64)
KEY=cobalt-native

ENTRIES=()
for classifier in "${CLASSIFIERS[@]}"; do
    bin_dir="$NATIVES_BIN/$classifier"
    [ -d "$bin_dir" ] || continue
    case "$classifier" in
        linux-*)   glob='*.so*'   ;;
        darwin-*)  glob='*.dylib' ;;
        windows-*) glob='*.dll'   ;;
    esac
    binary=""
    biggest_sz=0
    while IFS= read -r candidate; do
        [ -L "$candidate" ] && continue
        sz=$(wc -c < "$candidate" | tr -d ' ')
        if [ "$sz" -gt "$biggest_sz" ]; then
            binary="$candidate"
            biggest_sz="$sz"
        fi
    done < <(find "$bin_dir" -maxdepth 1 -type f -name "$glob" 2>/dev/null)
    [ -n "$binary" ] || continue
    sha=$(sha256sum "$binary" | awk '{print $1}')
    size=$(wc -c < "$binary" | tr -d ' ')
    rel="modules/lib/natives/bin/$classifier/$(basename "$binary")"
    ENTRIES+=("    \"${KEY}/${classifier}\": { \"sha256\": \"${sha}\", \"size\": ${size}, \"path\": \"${rel}\" }")
done

mkdir -p "$(dirname "$MANIFEST")"
{
    echo '{'
    echo "  \"version\": \"${VERSION}\","
    echo "  \"commitSha\": \"${COMMIT_SHA}\","
    echo '  "binaries": {'
    last_idx=$(( ${#ENTRIES[@]} - 1 ))
    for i in "${!ENTRIES[@]}"; do
        if (( i == last_idx )); then
            echo "${ENTRIES[$i]}"
        else
            echo "${ENTRIES[$i]},"
        fi
    done
    echo '  }'
    echo '}'
} > "$MANIFEST"

echo "wrote $MANIFEST (${#ENTRIES[@]} entries) at commit ${COMMIT_SHA}"
