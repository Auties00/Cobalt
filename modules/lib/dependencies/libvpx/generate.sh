#!/usr/bin/env bash
#
# Regenerates Java FFM bindings for libvpx (calls video stack, package
# calls.media.video.codec.vpx.bindings).
#
# The bound surface is the PORTABLE extern-C shim cobalt_vpx_shim.h, NOT the raw
# libvpx headers. The shim re-exposes only the VP8/VP9 encode and decode surface
# the calls codec uses, through fixed-width scalars and opaque void* handles, and
# builds every libvpx struct (vpx_codec_ctx_t / vpx_codec_enc_cfg_t /
# vpx_codec_dec_cfg_t / vpx_image_t / vpx_codec_cx_pkt_t) C-side. jextract
# therefore emits a binding with no ABI-sensitive layout (no OfLong vs OfInt for
# the `unsigned long` duration/deadline of vpx_codec_encode, no host-dependent
# enum width, no struct classes at all), so the SAME committed binding is correct
# on LP64 (Linux/macOS) and LLP64 (Windows) alike.
#
# Header class name: CobaltVpx (not LibVpx). The Java side now sees the Cobalt
# shim API, not the raw libvpx API, so the name reflects the shim and gives a
# clean break from the deleted raw-struct binding. The codec selector argument
# picks VP8 or VP9 C-side, so VP9 is reached without a separate Java entry point.
# The shim is compiled into cobalt-native and its cobalt_vpx_* symbols are forced
# into the export union by build-natives.sh build_combined (the per-function
# include flags below ARE that union's libvpx contribution).
#
# The vpx/*.h headers stay vendored under headers/vpx/ only because build_libvpx
# consumes them as the compile target for cobalt_vpx_shim.c; they are no longer
# bound here. Re-run whenever cobalt_vpx_shim.h changes.
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
PKG="com.github.auties00.cobalt.calls.media.video.codec.vpx.bindings"

# Remove the prior shim binding and any leftover raw-struct binding from before
# the portable-shim migration, so a regen leaves only the freshly emitted set.
PKG_DIR="$OUT/${PKG//.//}"
rm -f "$PKG_DIR/CobaltVpx.java" "$PKG_DIR/CobaltVpx\$shared.java" \
      "$PKG_DIR/LibVpx.java" "$PKG_DIR/LibVpx\$shared.java" \
      "$PKG_DIR/vpx_codec_ctx.java" "$PKG_DIR/vpx_codec_enc_cfg.java" \
      "$PKG_DIR/vpx_codec_dec_cfg.java" "$PKG_DIR/vpx_image.java" \
      "$PKG_DIR/vpx_codec_cx_pkt.java" "$PKG_DIR/vpx_fixed_buf.java" \
      "$PKG_DIR/vpx_rational.java"

"$JEXTRACT" \
  -t "$PKG" \
  -I "$DIR" \
  --header-class-name CobaltVpx \
  --output "$OUT" \
  --include-function cobalt_vpx_encoder_create \
  --include-function cobalt_vpx_encoder_reconfigure \
  --include-function cobalt_vpx_encoder_encode \
  --include-function cobalt_vpx_encoder_get_packet \
  --include-function cobalt_vpx_encoder_destroy \
  --include-function cobalt_vpx_decoder_create \
  --include-function cobalt_vpx_decoder_decode \
  --include-function cobalt_vpx_decoder_get_frame \
  --include-function cobalt_vpx_img_plane \
  --include-function cobalt_vpx_img_stride \
  --include-function cobalt_vpx_img_width \
  --include-function cobalt_vpx_img_height \
  --include-function cobalt_vpx_decoder_destroy \
  --include-function cobalt_vpx_strerror \
  --include-constant COBALT_VPX_CODEC_VP8 \
  --include-constant COBALT_VPX_CODEC_VP9 \
  --include-constant COBALT_VPX_PLANE_Y \
  --include-constant COBALT_VPX_PLANE_U \
  --include-constant COBALT_VPX_PLANE_V \
  --include-constant COBALT_VPX_OK \
  --include-constant COBALT_VPX_BAD_PARAM \
  --include-constant COBALT_VPX_NOMEM \
  --include-constant COBALT_VPX_IMG_WRAP_FAILED \
  "$DIR/cobalt_vpx_shim.h"

echo "wrote $OUT/${PKG//.//}/CobaltVpx.java"
