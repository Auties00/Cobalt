/*
 * Umbrella header that pulls in every FFmpeg public surface Cobalt
 * binds to (both the call toolkit's file-source / file-sink layer and
 * the upload media transcoder). Used as the single input to jextract
 * so the whole transitive type graph (AVRational -> AVPacket ->
 * AVCodecParameters -> AVCodec -> AVStream -> AVFormatContext, plus
 * the libsw* state objects, plus the libavfilter graph types) is
 * visible in one compilation unit.
 *
 * Per-library invocations don't work here -- declaring
 * --include-struct AVStream from libavformat fails when AVRational
 * (libavutil) and AVPacket (libavcodec) are not also included, and
 * those in turn pull in AVCodec, AVCodecParameters, AVRational,
 * AVChannelLayout, AVPixelFormat, AVSampleFormat -- all the way
 * across the public ABI. One umbrella header lets jextract resolve
 * those transitively from the function signatures we actually bind.
 *
 * The corresponding generated class is `Ffmpeg.java`; the toolkit
 * wrappers and the upload transcoder reference it directly. Re-run
 * dependencies/ffmpeg/generate.sh whenever this list changes.
 */
#ifndef COBALT_FFMPEG_UMBRELLA_H
#define COBALT_FFMPEG_UMBRELLA_H

#include <libavutil/avutil.h>
#include <libavutil/frame.h>
#include <libavutil/imgutils.h>
#include <libavutil/rational.h>
#include <libavutil/channel_layout.h>
#include <libavutil/samplefmt.h>
#include <libavutil/pixfmt.h>
#include <libavutil/error.h>
#include <libavutil/opt.h>
#include <libavutil/dict.h>
#include <libavutil/mem.h>

#include <libavcodec/avcodec.h>
#include <libavcodec/codec.h>
#include <libavcodec/codec_par.h>
#include <libavcodec/codec_id.h>
#include <libavcodec/packet.h>

#include <libavformat/avformat.h>
#include <libavformat/avio.h>

#include <libavdevice/avdevice.h>

#include <libavfilter/avfilter.h>
#include <libavfilter/buffersrc.h>
#include <libavfilter/buffersink.h>

#include <libswscale/swscale.h>
#include <libswresample/swresample.h>

#endif /* COBALT_FFMPEG_UMBRELLA_H */
