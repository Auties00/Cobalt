#!/usr/bin/env bash
#
# Regenerates the Java FFM binding for the WebRTC Audio Processing Module shim
# (calls media.audio.processing stack, class
# com.github.auties00.cobalt.calls.media.audio.processing.bindings.CobaltWebRtcApm).
#
# The bound surface is the PORTABLE extern-C shim cobalt_webrtc_apm_shim.h, NOT
# the raw WebRTC AudioProcessing C++ API. The shim re-exposes the APM entry
# points (create / process_reverse / process / set_stream_delay_ms / destroy)
# through fixed-width scalars, float32 IO at the 10 ms WebRTC APM frame, and an
# opaque void* handle, so the SAME committed binding is correct on every host
# ABI. See re/calls2-spec/NATIVE-BINDINGS.md and flat/fn6819.c for the RE
# provenance of the stack the shim wraps (wa_mobile_audio_processing.cc).
#
# Header class name: CobaltWebRtcApm. The cobalt_webrtc_apm_* symbols are the
# shim entry points; build-natives.sh build_webrtc_apm compiles
# cobalt_webrtc_apm_shim.cpp into cobalt-native with the webrtc-audio-processing
# library linked statically, and forces these symbols into the export union (the
# per-function include flags below ARE that union's webrtc-apm contribution).
#
# Prerequisites: JEXTRACT_HOME pointing at a jextract 22+ install, or jextract on
# PATH. Download from https://jdk.java.net/jextract/ if absent.
#
# NOTE: This binding is currently committed by hand in the same shape jextract
# emits (see the other calls bindings, e.g. CobaltOpus) because the native
# webrtc-audio-processing library and the cobalt_webrtc_apm_shim.cpp
# implementation are not yet present in the tree; the WebRtcAudioProcessor seam
# gates the live-capture conditioner on symbol availability and stays bypassed
# until the native artifact lands. Re-run this script once the shim is buildable
# to regenerate the binding from the header verbatim.
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
PKG="com.github.auties00.cobalt.calls.media.audio.processing.bindings"

PKG_DIR="$OUT/${PKG//.//}"
rm -f "$PKG_DIR/CobaltWebRtcApm.java" "$PKG_DIR/CobaltWebRtcApm\$shared.java"

"$JEXTRACT" \
  -t "$PKG" \
  -I "$DIR/headers" \
  --header-class-name CobaltWebRtcApm \
  --output "$OUT" \
  --include-function cobalt_webrtc_apm_create \
  --include-function cobalt_webrtc_apm_process_reverse \
  --include-function cobalt_webrtc_apm_process \
  --include-function cobalt_webrtc_apm_set_stream_delay_ms \
  --include-function cobalt_webrtc_apm_destroy \
  --include-constant COBALT_APM_OK \
  --include-constant COBALT_APM_BAD_PARAM \
  --include-constant COBALT_APM_NOMEM \
  --include-constant COBALT_APM_ERROR \
  --include-constant COBALT_APM_SAMPLE_RATE_HZ \
  --include-constant COBALT_APM_FRAME_SAMPLES \
  --include-constant COBALT_APM_AEC_OFF \
  --include-constant COBALT_APM_AEC_MOBILE \
  --include-constant COBALT_APM_AEC_AEC3 \
  "$DIR/headers/cobalt_webrtc_apm_shim.h"

echo "wrote $OUT/${PKG//.//}/CobaltWebRtcApm.java"
