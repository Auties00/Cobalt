#!/usr/bin/env bash
#
# Regenerates Java FFM bindings for openh264 (package
# calls.media.video.codec.h264.bindings).
#
# The bound surface is the PORTABLE extern-C shim cobalt_h264_shim.h, NOT the raw
# codec_api.h. openh264's runtime API is C++ (the ISVCEncoderVtbl / ISVCDecoderVtbl
# function-pointer tables dispatched through C++ vtables) and its public structs
# (SEncParamExt / SSourcePicture / SFrameBSInfo / SDecodingParam / SBufferInfo)
# carry C enums and "long long" timestamps whose width and the C enum width are
# host-dependent. Driving the vtables from FFM is fragile, and a binding generated
# for one ABI ClassCastExceptions or mis-reads fields on another. The shim calls
# the C++ vtable methods directly C-side, builds every openh264 struct C-side from
# portable scalars, and re-exposes a flat extern-C surface through fixed-width
# scalars and opaque void* handles, so jextract emits a binding with no
# ABI-sensitive layout (no struct classes, no vtable classes), and the SAME
# committed binding is correct on LP64 (Linux/macOS) and LLP64 (Windows) alike.
# The shim is compiled into cobalt-native and its cobalt_h264_* symbols are forced
# into the export union by build-natives.sh build_combined (the per-function
# include flags below ARE that union's openh264 contribution).
#
# Header class name: CobaltOpenH264 (not OpenH264). The Java side now sees the
# Cobalt shim API, not the raw openh264 API, so the name reflects the shim and
# gives a clean break from the deleted raw-struct/vtable binding.
#
# codec_api.h / codec_app_def.h / codec_def.h stay vendored under headers/ only
# because build_openh264 consumes them as the compile target for
# cobalt_h264_shim.cpp; they are no longer bound here.
# Re-run whenever cobalt_h264_shim.h changes.
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
PKG="com.github.auties00.cobalt.calls.media.video.codec.h264.bindings"

# Remove the prior shim binding and the entire raw-struct/vtable binding emitted
# before the portable-shim migration, so a regen leaves only the freshly emitted
# set. This lists the previous CobaltOpenH264 header binding, the old OpenH264
# header binding, and every struct/typedef/version class the old generate.sh
# emitted (the vtables, the version struct, and all the S*/Tag*/Source_Picture_s
# classes).
PKG_DIR="$OUT/${PKG//.//}"
rm -f "$PKG_DIR/CobaltOpenH264.java" "$PKG_DIR/CobaltOpenH264\$shared.java" \
      "$PKG_DIR/OpenH264.java" "$PKG_DIR/OpenH264\$shared.java" \
      "$PKG_DIR/ISVCEncoderVtbl.java" "$PKG_DIR/ISVCDecoderVtbl.java" \
      "$PKG_DIR/_tagVersion.java" "$PKG_DIR/OpenH264Version.java" \
      "$PKG_DIR/TagEncParamBase.java" "$PKG_DIR/SEncParamBase.java" \
      "$PKG_DIR/TagEncParamExt.java" "$PKG_DIR/SEncParamExt.java" \
      "$PKG_DIR/TagBitrateInfo.java" "$PKG_DIR/SBitrateInfo.java" \
      "$PKG_DIR/TagSVCDecodingParam.java" "$PKG_DIR/SDecodingParam.java" \
      "$PKG_DIR/Source_Picture_s.java" "$PKG_DIR/SSourcePicture.java" \
      "$PKG_DIR/TagBufferInfo.java" "$PKG_DIR/SBufferInfo.java" \
      "$PKG_DIR/TagSysMemBuffer.java" \
      "$PKG_DIR/TagDecoderCapability.java" "$PKG_DIR/SDecoderCapability.java" \
      "$PKG_DIR/TagParserBsInfo.java" "$PKG_DIR/SParserBsInfo.java" \
      "$PKG_DIR/SLayerBSInfo.java" "$PKG_DIR/SFrameBSInfo.java" \
      "$PKG_DIR/SVideoProperty.java" "$PKG_DIR/SSpatialLayerConfig.java" \
      "$PKG_DIR/SSliceArgument.java" "$PKG_DIR/SLTRConfig.java" \
      "$PKG_DIR/SLTRMarkingFeedback.java" "$PKG_DIR/SLTRRecoverRequest.java"

"$JEXTRACT" \
  -t "$PKG" \
  -I "$DIR" \
  --header-class-name CobaltOpenH264 \
  --output "$OUT" \
  --include-function cobalt_h264_version \
  --include-function cobalt_h264_encoder_create \
  --include-function cobalt_h264_encoder_set_rates \
  --include-function cobalt_h264_encoder_force_idr \
  --include-function cobalt_h264_encoder_encode \
  --include-function cobalt_h264_encoder_get_packet \
  --include-function cobalt_h264_encoder_destroy \
  --include-function cobalt_h264_decoder_create \
  --include-function cobalt_h264_decoder_decode \
  --include-function cobalt_h264_decoder_get_frame \
  --include-function cobalt_h264_img_plane \
  --include-function cobalt_h264_img_stride \
  --include-function cobalt_h264_img_width \
  --include-function cobalt_h264_img_height \
  --include-function cobalt_h264_decoder_destroy \
  --include-function cobalt_h264_last_native_status \
  --include-function cobalt_h264_strerror \
  --include-constant COBALT_H264_USAGE_CAMERA_REALTIME \
  --include-constant COBALT_H264_USAGE_SCREEN_REALTIME \
  --include-constant COBALT_H264_RC_QUALITY \
  --include-constant COBALT_H264_RC_BITRATE \
  --include-constant COBALT_H264_RC_BUFFERBASED \
  --include-constant COBALT_H264_RC_TIMESTAMP \
  --include-constant COBALT_H264_RC_OFF \
  --include-constant COBALT_H264_COMPLEXITY_LOW \
  --include-constant COBALT_H264_COMPLEXITY_MEDIUM \
  --include-constant COBALT_H264_COMPLEXITY_HIGH \
  --include-constant COBALT_H264_OK \
  --include-constant COBALT_H264_BAD_PARAM \
  --include-constant COBALT_H264_NOMEM \
  --include-constant COBALT_H264_CREATE_FAIL \
  --include-constant COBALT_H264_NATIVE_ERROR \
  "$DIR/cobalt_h264_shim.h"

echo "wrote $OUT/${PKG//.//}/CobaltOpenH264.java"
