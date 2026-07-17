#!/usr/bin/env bash
#
# Regenerates Java FFM bindings for dav1d (AV1 DECODE only; calls video stack,
# package calls.media.video.codec.av1.bindings). dav1d has no encoder; the AV1 encoder
# (libaom or SVT-AV1) is a SEPARATE FUTURE DEPENDENCY with its own dir, not an
# extension of this scaffolding.
#
# The bound surface is the PORTABLE extern-C shim cobalt_dav1d_shim.h, NOT the
# raw dav1d.h. The shim re-exposes only the dav1d decode functions the calls
# video stack uses, through fixed-width scalars and opaque void* handles, and
# builds every dav1d struct (Dav1dSettings / Dav1dData / Dav1dPicture and their
# dependency structs) C-side. jextract therefore emits a binding with no
# ABI-sensitive layout (no OfLong vs OfInt for ptrdiff_t stride fields, no
# host-dependent enum width, no struct classes at all), so the SAME committed
# binding is correct on LP64 (Linux/macOS) and LLP64 (Windows) alike.
#
# Binding the shim ALSO fixes a regen blocker in the old raw binding: dav1d.h's
# public structs reference dependency structs (Dav1dDataProps, Dav1dRef,
# Dav1dSequenceHeader, the picture metadata structs) that the old --include-struct
# list omitted, so jextract aborted with "Dav1dPicture depends on Dav1dDataProps
# which has been excluded". With the shim there are no struct bindings, so that
# whole dependency closure disappears.
#
# Header class name: CobaltDav1d (not Dav1d). The Java side now sees the Cobalt
# shim API, not the raw dav1d API, so the name reflects the shim and gives a
# clean break from the deleted raw-struct binding. The shim is compiled into
# cobalt-native and its cobalt_dav1d_* symbols are forced into the export union
# by build-natives.sh build_combined (the per-function include flags below ARE
# that union's dav1d contribution).
#
# dav1d.h (and its data.h/picture.h/common.h/headers.h/version.h siblings) stay
# vendored under headers/ only because build_av1 consumes them as the compile
# target for cobalt_dav1d_shim.c; they are no longer bound here.
# Re-run whenever cobalt_dav1d_shim.h changes.
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

# Remove the prior shim binding and any leftover raw-struct binding from before
# the portable-shim migration, so a regen leaves only the freshly emitted set.
PKG_DIR="$OUT/${PKG//.//}"
rm -f "$PKG_DIR/CobaltDav1d.java" "$PKG_DIR/CobaltDav1d\$shared.java" \
      "$PKG_DIR/Dav1d.java" "$PKG_DIR/Dav1d\$shared.java" \
      "$PKG_DIR/Dav1dSettings.java" "$PKG_DIR/Dav1dData.java" \
      "$PKG_DIR/Dav1dPicture.java" "$PKG_DIR/Dav1dPictureParameters.java" \
      "$PKG_DIR/Dav1dSequenceHeader.java" "$PKG_DIR/Dav1dDataProps.java" \
      "$PKG_DIR/Dav1dContext.java" "$PKG_DIR/Dav1dRef.java"

"$JEXTRACT" \
  -t "$PKG" \
  -I "$DIR" \
  --header-class-name CobaltDav1d \
  --output "$OUT" \
  --include-function cobalt_dav1d_version \
  --include-function cobalt_dav1d_open \
  --include-function cobalt_dav1d_send_data \
  --include-function cobalt_dav1d_get_picture \
  --include-function cobalt_dav1d_pic_plane \
  --include-function cobalt_dav1d_pic_stride \
  --include-function cobalt_dav1d_pic_width \
  --include-function cobalt_dav1d_pic_height \
  --include-function cobalt_dav1d_pic_bitdepth \
  --include-function cobalt_dav1d_pic_layout \
  --include-function cobalt_dav1d_picture_unref \
  --include-function cobalt_dav1d_close \
  --include-constant COBALT_DAV1D_LAYOUT_I400 \
  --include-constant COBALT_DAV1D_LAYOUT_I420 \
  --include-constant COBALT_DAV1D_LAYOUT_I422 \
  --include-constant COBALT_DAV1D_LAYOUT_I444 \
  --include-constant COBALT_DAV1D_OK \
  --include-constant COBALT_DAV1D_EAGAIN \
  --include-constant COBALT_DAV1D_EINVAL \
  --include-constant COBALT_DAV1D_ENOMEM \
  --include-constant COBALT_DAV1D_ERROR \
  "$DIR/cobalt_dav1d_shim.h"

echo "wrote $OUT/${PKG//.//}/CobaltDav1d.java"
