/*
 * Aggregator header for jextract: pulls in libvpx's VP8 encoder/decoder
 * surface so the generated bindings cover both halves in a single class.
 */
#include "vpx/vpx_codec.h"
#include "vpx/vpx_image.h"
#include "vpx/vpx_encoder.h"
#include "vpx/vpx_decoder.h"
#include "vpx/vp8.h"
#include "vpx/vp8cx.h"
#include "vpx/vp8dx.h"
