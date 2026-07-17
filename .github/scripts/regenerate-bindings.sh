#!/usr/bin/env bash
#
# Runs every per-dep generate.sh under modules/lib/dependencies/*/,
# regenerating the Java FFM bindings against the currently-vendored
# headers. Stops on the first failure so the cause is obvious.

set -Eeuo pipefail
trap 'echo "[regenerate-bindings] FAILED at ${BASH_SOURCE[0]}:${LINENO}: ${BASH_COMMAND}" >&2' ERR

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$DIR/../.." && pwd)"

OS="$(uname -s)"

GENERATORS=()
while IFS= read -r gen; do
    GENERATORS+=("$gen")
done < <(find "$ROOT/modules/lib/dependencies" -maxdepth 2 -name generate.sh -type f | sort)
[ "${#GENERATORS[@]}" -gt 0 ] || { echo "[regenerate-bindings] no generate.sh files found under modules/lib/dependencies/*/" >&2; exit 1; }

for gen in "${GENERATORS[@]}"; do
    dep="$(basename "$(dirname "$gen")")"
    echo "[regenerate-bindings] $dep"
    bash "$gen"
done

echo "[regenerate-bindings] done (${#GENERATORS[@]} deps)"
