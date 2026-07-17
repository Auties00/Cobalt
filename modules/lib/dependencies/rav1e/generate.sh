#!/usr/bin/env bash
#
# Regenerates Java FFM bindings for rav1e (AV1 ENCODE only; calls video stack,
# package calls.media.video.codec.av1.bindings -- the SAME package as the dav1d decode
# binding CobaltDav1d, since both are the AV1 stack: dav1d decodes, rav1e encodes).
#
# The bound surface is the PORTABLE extern-C shim cobalt_rav1e_shim.h, NOT the
# raw cargo-c generated rav1e.h. The shim re-exposes only the rav1e encode
# functions the calls video stack uses, through fixed-width scalars and opaque
# void* handles, and builds every rav1e type (RaConfig / RaFrame / RaRational and
# the RaChromaSampling / RaChromaSamplePosition / RaPixelRange / RaEncoderStatus /
# RaFrameType enums and the RaPacket struct) C-side. jextract therefore emits a
# binding with no ABI-sensitive layout (no OfLong vs OfInt for the ptrdiff_t
# stride / size_t length, no host-dependent enum width, no RaPacket struct class),
# so the SAME committed binding is correct on LP64 (Linux/macOS) and LLP64
# (Windows) alike.
#
# Header class name: CobaltRav1e. The Java side sees the Cobalt shim API, not the
# raw rav1e API, so the name reflects the shim. The shim is compiled into
# cobalt-native and its cobalt_rav1e_* symbols are forced into the export union by
# build-natives.sh build_combined (the per-function include flags below ARE that
# union's rav1e contribution).
#
# rav1e.h (the cargo-c generated header) is vendored under headers/ only because
# build_rav1e consumes it as the compile target for cobalt_rav1e_shim.c; it is
# not bound here. Re-run whenever cobalt_rav1e_shim.h changes.
#
# Prerequisites: JEXTRACT_HOME pointing at a jextract 22+ install, or jextract on
# PATH. Download from https://jdk.java.net/jextract/ if absent.
#

set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$DIR/../../../.." && pwd)"
if [ -n "${JEXTRACT_HOME:-}" ]; then
  JEXTRACT="$JEXTRACT_HOME/bin/jextract"
else
  JEXTRACT="$(command -v jextract || true)"
fi
[ -n "$JEXTRACT" ] && [ -f "$JEXTRACT" ] || { echo "jextract not found; set JEXTRACT_HOME or add to PATH" >&2; exit 1; }

OUT="$ROOT/modules/lib/src/main/java"
PKG="com.github.auties00.cobalt.calls.media.video.codec.av1.bindings"

# Remove the prior shim binding so a regen leaves only the freshly emitted set.
PKG_DIR="$OUT/${PKG//.//}"
rm -f "$PKG_DIR/CobaltRav1e.java" "$PKG_DIR/CobaltRav1e\$shared.java"

"$JEXTRACT" \
  -t "$PKG" \
  -I "$DIR" \
  --header-class-name CobaltRav1e \
  --output "$OUT" \
  --include-function cobalt_rav1e_encoder_create \
  --include-function cobalt_rav1e_encoder_send \
  --include-function cobalt_rav1e_encoder_receive \
  --include-function cobalt_rav1e_packet_unref \
  --include-function cobalt_rav1e_encoder_destroy \
  --include-function cobalt_rav1e_strerror \
  --include-constant COBALT_RAV1E_OK \
  --include-constant COBALT_RAV1E_BAD_PARAM \
  --include-constant COBALT_RAV1E_NOMEM \
  --include-constant COBALT_RAV1E_FAILURE \
  "$DIR/cobalt_rav1e_shim.h"

echo "wrote $OUT/${PKG//.//}/CobaltRav1e.java"
