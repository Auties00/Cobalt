#!/usr/bin/env bash
#
# Regenerates Java FFM bindings for libopus (calls audio stack, package
# calls.media.audio.codec.opus.bindings).
#
# The bound surface is the PORTABLE extern-C shim cobalt_opus_shim.h, NOT the raw
# opus headers. The shim re-exposes only the encode/decode, control, repacketizer
# and packet-inspection surface the calls audio path uses, through fixed-width
# scalars and opaque void* handles, and turns the VARIADIC opus_encoder_ctl /
# opus_decoder_ctl(st, request, ...) controls into typed cobalt_opus_* setters and
# getters applied C-side. It also converts opus_packet_parse's array-of-pointers
# output (const unsigned char *frames[48]) into fixed-width offset and size
# arrays. jextract therefore emits a binding with no ABI-sensitive layout and no
# manual variadic-invoker shape (no OfLong vs OfInt, no per-argument ctl invoker,
# no struct classes), so the SAME committed binding is correct on LP64
# (Linux/macOS) and LLP64 (Windows) alike.
#
# Header class name: CobaltOpus (not Opus). The Java side now sees the Cobalt shim
# API, not the raw opus API, so the name reflects the shim and gives a clean break
# from the deleted raw binding. The shim is compiled into cobalt-native and its
# cobalt_opus_* symbols are forced into the export union by build-natives.sh
# build_combined (the per-function include flags below ARE that union's libopus
# contribution).
#
# The opus/*.h headers stay vendored under headers/opus/ only because build_opus
# consumes them as the compile target for cobalt_opus_shim.c; they are no longer
# bound here. Re-run whenever cobalt_opus_shim.h changes.
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
PKG="com.github.auties00.cobalt.calls.media.audio.codec.opus.bindings"

# Remove the prior shim binding and the leftover raw binding from before the
# portable-shim migration, so a regen leaves only the freshly emitted set.
PKG_DIR="$OUT/${PKG//.//}"
rm -f "$PKG_DIR/CobaltOpus.java" "$PKG_DIR/CobaltOpus\$shared.java" \
      "$PKG_DIR/Opus.java" "$PKG_DIR/Opus\$shared.java"

"$JEXTRACT" \
  -t "$PKG" \
  -I "$DIR" \
  --header-class-name CobaltOpus \
  --output "$OUT" \
  --include-function cobalt_opus_encoder_create \
  --include-function cobalt_opus_encoder_destroy \
  --include-function cobalt_opus_encode \
  --include-function cobalt_opus_encoder_set_bitrate \
  --include-function cobalt_opus_encoder_set_vbr \
  --include-function cobalt_opus_encoder_set_vbr_constraint \
  --include-function cobalt_opus_encoder_set_complexity \
  --include-function cobalt_opus_encoder_set_inband_fec \
  --include-function cobalt_opus_encoder_set_packet_loss_perc \
  --include-function cobalt_opus_encoder_set_dtx \
  --include-function cobalt_opus_encoder_set_force_channels \
  --include-function cobalt_opus_encoder_set_signal \
  --include-function cobalt_opus_encoder_set_lsb_depth \
  --include-function cobalt_opus_encoder_set_bandwidth \
  --include-function cobalt_opus_encoder_set_max_bandwidth \
  --include-function cobalt_opus_encoder_set_expert_frame_duration \
  --include-function cobalt_opus_encoder_set_prediction_disabled \
  --include-function cobalt_opus_encoder_get_lookahead \
  --include-function cobalt_opus_encoder_reset_state \
  --include-function cobalt_opus_decoder_create \
  --include-function cobalt_opus_decoder_destroy \
  --include-function cobalt_opus_decode \
  --include-function cobalt_opus_decoder_reset_state \
  --include-function cobalt_opus_repacketizer_create \
  --include-function cobalt_opus_repacketizer_destroy \
  --include-function cobalt_opus_repacketizer_init \
  --include-function cobalt_opus_repacketizer_cat \
  --include-function cobalt_opus_repacketizer_get_nb_frames \
  --include-function cobalt_opus_repacketizer_out_range \
  --include-function cobalt_opus_repacketizer_out \
  --include-function cobalt_opus_packet_parse \
  --include-function cobalt_opus_packet_get_nb_frames \
  --include-function cobalt_opus_packet_get_samples_per_frame \
  --include-function cobalt_opus_packet_get_bandwidth \
  --include-function cobalt_opus_packet_get_nb_channels \
  --include-function cobalt_opus_packet_has_lbrr \
  --include-function cobalt_opus_strerror \
  --include-function cobalt_opus_get_version_string \
  --include-constant COBALT_OPUS_OK \
  --include-constant COBALT_OPUS_BAD_ARG \
  --include-constant COBALT_OPUS_BUFFER_TOO_SMALL \
  --include-constant COBALT_OPUS_INTERNAL_ERROR \
  --include-constant COBALT_OPUS_INVALID_PACKET \
  --include-constant COBALT_OPUS_UNIMPLEMENTED \
  --include-constant COBALT_OPUS_INVALID_STATE \
  --include-constant COBALT_OPUS_ALLOC_FAIL \
  --include-constant COBALT_OPUS_APPLICATION_VOIP \
  --include-constant COBALT_OPUS_APPLICATION_AUDIO \
  --include-constant COBALT_OPUS_APPLICATION_RESTRICTED_LOWDELAY \
  --include-constant COBALT_OPUS_SIGNAL_VOICE \
  --include-constant COBALT_OPUS_SIGNAL_MUSIC \
  --include-constant COBALT_OPUS_BANDWIDTH_NARROWBAND \
  --include-constant COBALT_OPUS_BANDWIDTH_MEDIUMBAND \
  --include-constant COBALT_OPUS_BANDWIDTH_WIDEBAND \
  --include-constant COBALT_OPUS_BANDWIDTH_SUPERWIDEBAND \
  --include-constant COBALT_OPUS_BANDWIDTH_FULLBAND \
  --include-constant COBALT_OPUS_FRAMESIZE_ARG \
  --include-constant COBALT_OPUS_FRAMESIZE_2_5_MS \
  --include-constant COBALT_OPUS_FRAMESIZE_5_MS \
  --include-constant COBALT_OPUS_FRAMESIZE_10_MS \
  --include-constant COBALT_OPUS_FRAMESIZE_20_MS \
  --include-constant COBALT_OPUS_FRAMESIZE_40_MS \
  --include-constant COBALT_OPUS_FRAMESIZE_60_MS \
  --include-constant COBALT_OPUS_FRAMESIZE_80_MS \
  --include-constant COBALT_OPUS_FRAMESIZE_100_MS \
  --include-constant COBALT_OPUS_FRAMESIZE_120_MS \
  --include-constant COBALT_OPUS_AUTO \
  --include-constant COBALT_OPUS_BITRATE_MAX \
  "$DIR/cobalt_opus_shim.h"

echo "wrote $OUT/${PKG//.//}/CobaltOpus.java"
