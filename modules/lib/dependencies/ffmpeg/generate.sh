#!/usr/bin/env bash
#
# Regenerates Java FFM bindings for the FFmpeg subset the toolkit uses (package
# util.ffmpeg). One umbrella header (ffmpeg_umbrella.h) drives a single jextract
# pass so cross-library type dependencies resolve in one compilation unit;
# per-library invocations fail when a struct field's type is not in the include
# set. Re-run whenever the FFmpeg headers under headers/ change.
#
# Prerequisites: JEXTRACT_HOME pointing at a jextract 22+ install, or jextract on
# PATH. Download from https://jdk.java.net/jextract/ if absent.

set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$DIR/../../../.." && pwd)"
if [ -n "${JEXTRACT_HOME:-}" ]; then
  JEXTRACT="$JEXTRACT_HOME/bin/jextract"
else
  JEXTRACT="$(command -v jextract || true)"
fi
[ -n "$JEXTRACT" ] && [ -f "$JEXTRACT" ] || { echo "jextract not found; set JEXTRACT_HOME or add to PATH" >&2; exit 1; }

# Windows ships jextract as a .bat next to the bash-friendly launcher;
# pick whichever is executable.
if [[ "$JEXTRACT" != *.bat ]] && [ ! -x "$JEXTRACT" ] && [ -x "${JEXTRACT}.bat" ]; then
    JEXTRACT="${JEXTRACT}.bat"
fi

OUT="$ROOT/modules/lib/src/main/java"
PKG="com.github.auties00.cobalt.util.ffmpeg"
PKG_DIR="$OUT/${PKG//.//}"
mkdir -p "$PKG_DIR"

# Wipe any prior output so stale symbols don't survive a rename.
rm -f "$PKG_DIR/Ffmpeg.java" "$PKG_DIR/Ffmpeg\$"*.java

"$JEXTRACT" \
  -t "$PKG" \
  -I "$DIR/headers" \
  --header-class-name Ffmpeg \
  --output "$OUT" \
  --include-function avformat_alloc_output_context2 \
  --include-function avformat_alloc_context \
  --include-function avformat_free_context \
  --include-function avformat_open_input \
  --include-function avformat_find_stream_info \
  --include-function avformat_close_input \
  --include-function avformat_write_header \
  --include-function avformat_new_stream \
  --include-function av_read_frame \
  --include-function av_write_frame \
  --include-function av_interleaved_write_frame \
  --include-function av_write_trailer \
  --include-function av_dump_format \
  --include-function avio_open2 \
  --include-function avio_closep \
  --include-function avio_alloc_context \
  --include-function avio_context_free \
  --include-function avcodec_find_decoder \
  --include-function avcodec_find_encoder \
  --include-function avcodec_find_decoder_by_name \
  --include-function avcodec_find_encoder_by_name \
  --include-function avcodec_alloc_context3 \
  --include-function avcodec_free_context \
  --include-function avcodec_parameters_alloc \
  --include-function avcodec_parameters_free \
  --include-function avcodec_parameters_copy \
  --include-function avcodec_parameters_to_context \
  --include-function avcodec_parameters_from_context \
  --include-function avcodec_open2 \
  --include-function avcodec_send_packet \
  --include-function avcodec_receive_frame \
  --include-function avcodec_send_frame \
  --include-function avcodec_receive_packet \
  --include-function avcodec_flush_buffers \
  --include-function av_packet_alloc \
  --include-function av_packet_free \
  --include-function av_packet_ref \
  --include-function av_packet_unref \
  --include-function av_packet_clone \
  --include-function av_packet_rescale_ts \
  --include-function av_init_packet \
  --include-function avdevice_register_all \
  --include-function av_input_video_device_next \
  --include-function av_input_audio_device_next \
  --include-function av_output_video_device_next \
  --include-function av_output_audio_device_next \
  --include-function avdevice_list_input_sources \
  --include-function avdevice_free_list_devices \
  --include-function av_frame_alloc \
  --include-function av_frame_free \
  --include-function av_frame_ref \
  --include-function av_frame_unref \
  --include-function av_frame_get_buffer \
  --include-function av_frame_make_writable \
  --include-function av_strerror \
  --include-function av_get_pix_fmt_name \
  --include-function av_get_sample_fmt_name \
  --include-function av_image_get_buffer_size \
  --include-function av_image_fill_arrays \
  --include-function av_image_alloc \
  --include-function av_freep \
  --include-function av_malloc \
  --include-function av_free \
  --include-function av_dict_set \
  --include-function av_dict_free \
  --include-function av_opt_set \
  --include-function av_opt_set_int \
  --include-function av_opt_set_double \
  --include-function av_rescale_q \
  --include-function av_channel_layout_default \
  --include-function av_channel_layout_uninit \
  --include-function av_channel_layout_copy \
  --include-function sws_getContext \
  --include-function sws_getCachedContext \
  --include-function sws_scale \
  --include-function sws_freeContext \
  --include-function swr_alloc \
  --include-function swr_alloc_set_opts2 \
  --include-function swr_init \
  --include-function swr_is_initialized \
  --include-function swr_convert \
  --include-function swr_free \
  --include-function swr_get_delay \
  --include-function av_image_copy_to_buffer \
  --include-function av_frame_copy \
  --include-function av_dict_get \
  --include-function avfilter_graph_alloc \
  --include-function avfilter_graph_parse_ptr \
  --include-function avfilter_graph_config \
  --include-function avfilter_graph_free \
  --include-function avfilter_graph_create_filter \
  --include-function avfilter_get_by_name \
  --include-function avfilter_inout_alloc \
  --include-function avfilter_inout_free \
  --include-function av_buffersrc_add_frame_flags \
  --include-function av_buffersink_get_frame \
  --include-function av_strdup \
  --include-struct AVFormatContext \
  --include-struct AVStream \
  --include-struct AVOutputFormat \
  --include-struct AVInputFormat \
  --include-struct AVIOInterruptCB \
  --include-struct AVIOContext \
  --include-struct AVCodecContext \
  --include-struct AVCodec \
  --include-struct AVCodecParameters \
  --include-struct AVPacket \
  --include-struct AVPacketSideData \
  --include-struct AVFrame \
  --include-struct AVFrameSideData \
  --include-struct AVRational \
  --include-struct AVChannelLayout \
  --include-struct AVChannelCustom \
  --include-struct AVDictionaryEntry \
  --include-struct AVDeviceInfo \
  --include-struct AVDeviceInfoList \
  --include-struct AVProgram \
  --include-struct AVChapter \
  --include-struct AVStreamGroup \
  --include-struct AVStreamGroupTileGrid \
  --include-struct AVStreamGroupLCEVC \
  --include-struct AVProducerReferenceTime \
  --include-struct AVProfile \
  --include-struct AVCodecHWConfig \
  --include-struct AVHWAccel \
  --include-struct AVPanScan \
  --include-struct AVCPBProperties \
  --include-struct AVBufferRef \
  --include-struct AVClass \
  --include-struct AVOption \
  --include-struct AVOptionRange \
  --include-struct AVOptionRanges \
  --include-struct AVFilterGraph \
  --include-struct AVFilterContext \
  --include-struct AVFilter \
  --include-struct AVFilterInOut \
  --include-struct AVFilterLink \
  --include-struct AVFilterFormatsConfig \
  --include-typedef AVPixelFormat \
  --include-typedef AVSampleFormat \
  --include-typedef AVMediaType \
  --include-typedef AVCodecID \
  --include-typedef AVPictureType \
  --include-typedef AVColorRange \
  --include-typedef AVColorPrimaries \
  --include-typedef AVColorTransferCharacteristic \
  --include-typedef AVColorSpace \
  --include-typedef AVChromaLocation \
  --include-typedef AVFieldOrder \
  --include-typedef AVChannelOrder \
  --include-typedef AVDiscard \
  --include-typedef AVDurationEstimationMethod \
  --include-typedef AVStreamParseType \
  --include-typedef AVAudioServiceType \
  --include-typedef AVPacketSideDataType \
  --include-typedef AVFrameSideDataType \
  --include-typedef AVHWDeviceType \
  --include-typedef AVClassCategory \
  --include-typedef AVOptionType \
  --include-typedef AVRounding \
  --include-typedef SwrContext \
  --include-typedef SwsContext \
  --include-constant AV_PIX_FMT_YUV420P \
  --include-constant AV_PIX_FMT_YUVJ420P \
  --include-constant AV_PIX_FMT_YUV422P \
  --include-constant AV_PIX_FMT_YUV444P \
  --include-constant AV_PIX_FMT_RGBA \
  --include-constant AV_PIX_FMT_BGRA \
  --include-constant AV_PIX_FMT_RGB24 \
  --include-constant AV_PIX_FMT_BGR24 \
  --include-constant AV_PIX_FMT_NV12 \
  --include-constant AV_PIX_FMT_GRAY8 \
  --include-constant AV_SAMPLE_FMT_S16 \
  --include-constant AV_SAMPLE_FMT_S16P \
  --include-constant AV_SAMPLE_FMT_FLT \
  --include-constant AV_SAMPLE_FMT_FLTP \
  --include-constant AVMEDIA_TYPE_AUDIO \
  --include-constant AVMEDIA_TYPE_VIDEO \
  --include-constant AVMEDIA_TYPE_DATA \
  --include-constant AVMEDIA_TYPE_SUBTITLE \
  --include-constant AV_CODEC_ID_AAC \
  --include-constant AV_CODEC_ID_OPUS \
  --include-constant AV_CODEC_ID_MP3 \
  --include-constant AV_CODEC_ID_FLAC \
  --include-constant AV_CODEC_ID_PCM_S16LE \
  --include-constant AV_CODEC_ID_PCM_S16BE \
  --include-constant AV_CODEC_ID_VORBIS \
  --include-constant AV_CODEC_ID_H264 \
  --include-constant AV_CODEC_ID_VP8 \
  --include-constant AV_CODEC_ID_VP9 \
  --include-constant AV_CODEC_ID_MJPEG \
  --include-constant AV_CODEC_ID_WEBP \
  --include-constant AVERROR_EOF \
  --include-constant AVERROR_INVALIDDATA \
  --include-constant AVERROR_BUFFER_TOO_SMALL \
  --include-constant AV_NUM_DATA_POINTERS \
  --include-constant SWS_FAST_BILINEAR \
  --include-constant SWS_BILINEAR \
  --include-constant SWS_BICUBIC \
  --include-constant AVIO_FLAG_READ \
  --include-constant AVIO_FLAG_WRITE \
  --include-constant AVIO_FLAG_READ_WRITE \
  --include-constant AV_BUFFERSRC_FLAG_KEEP_REF \
  --include-constant AV_BUFFERSRC_FLAG_PUSH \
  --include-constant AV_BUFFERSRC_FLAG_NO_CHECK_FORMAT \
  --include-constant AV_DICT_MATCH_CASE \
  --include-constant AV_DICT_IGNORE_SUFFIX \
  --include-constant AV_CODEC_FLAG_QSCALE \
  --include-constant AV_CODEC_FLAG_GLOBAL_HEADER \
  --include-constant FF_QP2LAMBDA \
  --include-constant AV_OPT_SEARCH_CHILDREN \
  "$DIR/headers/ffmpeg_umbrella.h"

echo "wrote $PKG_DIR/Ffmpeg.java"
