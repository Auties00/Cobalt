/*
 * cobalt_vpx_shim.c
 *
 * Implementation of the portable extern-C libvpx facade declared in
 * cobalt_vpx_shim.h. Each wrapper builds the libvpx structs (vpx_codec_ctx_t,
 * vpx_codec_enc_cfg_t, vpx_codec_dec_cfg_t, vpx_image_t, vpx_codec_cx_pkt_t) on
 * the C side from the portable scalar arguments, calls the real vpx_codec_* /
 * vpx_img_* functions, and returns only fixed-width scalars and opaque void*
 * handles. No libvpx type ever crosses the FFM boundary, so the
 * jextract-generated Java binding is host-ABI independent.
 *
 * Compiled into the combined cobalt-native shared library by
 * .github/scripts/build-natives.sh (build_libvpx). The real vpx_codec_* symbols
 * are pulled from libvpx.a through ffmpeg's static link closure in the same
 * --start-group; build_combined adds libcobalt_vpx_shim.a to extra_archives so
 * the cobalt_vpx_* symbols (drawn from generate.sh's --include-function list)
 * are forced into the library's export table.
 */

#include "cobalt_vpx_shim.h"

#include <stdlib.h>
#include <string.h>

#include "vpx/vpx_codec.h"
#include "vpx/vpx_image.h"
#include "vpx/vpx_encoder.h"
#include "vpx/vpx_decoder.h"
#include "vpx/vp8cx.h"
#include "vpx/vp8dx.h"

/* Encoder handle: the libvpx context plus its reusable image header. */
typedef struct {
    vpx_codec_ctx_t ctx;
    vpx_image_t img;
    int codec;
} cobalt_vpx_encoder;

/* Decoder handle: just the libvpx context. */
typedef struct {
    vpx_codec_ctx_t ctx;
} cobalt_vpx_decoder;

/* Resolves the libvpx encoder interface for a COBALT_VPX_CODEC_* selector. */
static vpx_codec_iface_t *cobalt_vpx_enc_iface(int32_t codec) {
    switch (codec) {
        case COBALT_VPX_CODEC_VP8: return vpx_codec_vp8_cx();
        case COBALT_VPX_CODEC_VP9: return vpx_codec_vp9_cx();
        default:                   return NULL;
    }
}

/* Resolves the libvpx decoder interface for a COBALT_VPX_CODEC_* selector. */
static vpx_codec_iface_t *cobalt_vpx_dec_iface(int32_t codec) {
    switch (codec) {
        case COBALT_VPX_CODEC_VP8: return vpx_codec_vp8_dx();
        case COBALT_VPX_CODEC_VP9: return vpx_codec_vp9_dx();
        default:                   return NULL;
    }
}

/*
 * Stamps an encoder configuration from the portable scalar arguments. Mirrors
 * the realtime, error-resilient, single-thread constant-bitrate setup the call
 * encoder uses: a 1/frameRate timebase, the target bitrate in libvpx's kilobits
 * per second (floored at 1), the quantizer window, the frame-drop threshold, and
 * automatic keyframes at kfMaxDist when positive (otherwise disabled).
 */
static void cobalt_vpx_apply_cfg(vpx_codec_enc_cfg_t *cfg,
                                 int32_t width, int32_t height,
                                 int32_t targetBitrateBps, int32_t frameRate,
                                 int32_t minQuantizer, int32_t maxQuantizer,
                                 int32_t dropframeThresh, int32_t kfMaxDist) {
    unsigned int kbps;
    cfg->g_w = (unsigned int) width;
    cfg->g_h = (unsigned int) height;
    cfg->g_threads = 1;
    cfg->g_lag_in_frames = 0;
    cfg->g_pass = VPX_RC_ONE_PASS;
    cfg->g_error_resilient = VPX_ERROR_RESILIENT_DEFAULT;
    cfg->g_timebase.num = 1;
    cfg->g_timebase.den = frameRate;
    kbps = (unsigned int) (targetBitrateBps / 1000);
    cfg->rc_target_bitrate = kbps < 1u ? 1u : kbps;
    cfg->rc_min_quantizer = (unsigned int) minQuantizer;
    cfg->rc_max_quantizer = (unsigned int) maxQuantizer;
    cfg->rc_dropframe_thresh = (unsigned int) dropframeThresh;
    if (kfMaxDist > 0) {
        cfg->kf_mode = VPX_KF_AUTO;
        cfg->kf_max_dist = (unsigned int) kfMaxDist;
    } else {
        cfg->kf_mode = VPX_KF_DISABLED;
    }
}

int32_t cobalt_vpx_encoder_create(int32_t codec,
                                  int32_t width,
                                  int32_t height,
                                  int32_t targetBitrateBps,
                                  int32_t frameRate,
                                  int32_t minQuantizer,
                                  int32_t maxQuantizer,
                                  int32_t dropframeThresh,
                                  int32_t kfMaxDist,
                                  int32_t cpuUsed,
                                  void **outCtx) {
    vpx_codec_iface_t *iface;
    vpx_codec_enc_cfg_t cfg;
    vpx_codec_err_t err;
    cobalt_vpx_encoder *h;

    if (outCtx == NULL) {
        return COBALT_VPX_BAD_PARAM;
    }
    *outCtx = NULL;
    iface = cobalt_vpx_enc_iface(codec);
    if (iface == NULL) {
        return COBALT_VPX_BAD_PARAM;
    }

    memset(&cfg, 0, sizeof(cfg));
    err = vpx_codec_enc_config_default(iface, &cfg, 0);
    if (err != VPX_CODEC_OK) {
        return (int32_t) err;
    }
    cobalt_vpx_apply_cfg(&cfg, width, height, targetBitrateBps, frameRate,
                         minQuantizer, maxQuantizer, dropframeThresh, kfMaxDist);

    h = (cobalt_vpx_encoder *) calloc(1, sizeof(*h));
    if (h == NULL) {
        return COBALT_VPX_NOMEM;
    }
    h->codec = codec;
    err = vpx_codec_enc_init_ver(&h->ctx, iface, &cfg, 0, VPX_ENCODER_ABI_VERSION);
    if (err != VPX_CODEC_OK) {
        free(h);
        return (int32_t) err;
    }
    err = vpx_codec_control_(&h->ctx, VP8E_SET_CPUUSED, cpuUsed);
    if (err != VPX_CODEC_OK) {
        vpx_codec_destroy(&h->ctx);
        free(h);
        return (int32_t) err;
    }
    *outCtx = h;
    return COBALT_VPX_OK;
}

int32_t cobalt_vpx_encoder_reconfigure(void *ctx,
                                       int32_t width,
                                       int32_t height,
                                       int32_t targetBitrateBps,
                                       int32_t frameRate,
                                       int32_t minQuantizer,
                                       int32_t maxQuantizer,
                                       int32_t dropframeThresh,
                                       int32_t kfMaxDist,
                                       int32_t cpuUsed) {
    cobalt_vpx_encoder *h = (cobalt_vpx_encoder *) ctx;
    vpx_codec_iface_t *iface;
    vpx_codec_enc_cfg_t cfg;
    vpx_codec_err_t err;

    if (h == NULL) {
        return COBALT_VPX_BAD_PARAM;
    }
    iface = cobalt_vpx_enc_iface(h->codec);
    if (iface == NULL) {
        return COBALT_VPX_BAD_PARAM;
    }
    memset(&cfg, 0, sizeof(cfg));
    err = vpx_codec_enc_config_default(iface, &cfg, 0);
    if (err != VPX_CODEC_OK) {
        return (int32_t) err;
    }
    cobalt_vpx_apply_cfg(&cfg, width, height, targetBitrateBps, frameRate,
                         minQuantizer, maxQuantizer, dropframeThresh, kfMaxDist);
    err = vpx_codec_enc_config_set(&h->ctx, &cfg);
    if (err != VPX_CODEC_OK) {
        return (int32_t) err;
    }
    err = vpx_codec_control_(&h->ctx, VP8E_SET_CPUUSED, cpuUsed);
    return (int32_t) err;
}

int32_t cobalt_vpx_encoder_encode(void *ctx,
                                  const uint8_t *i420,
                                  size_t len,
                                  int32_t width,
                                  int32_t height,
                                  int64_t pts,
                                  int32_t forceKeyframe) {
    cobalt_vpx_encoder *h = (cobalt_vpx_encoder *) ctx;
    vpx_image_t *wrapped;
    vpx_enc_frame_flags_t flags;
    vpx_codec_err_t err;

    (void) len;
    if (h == NULL || i420 == NULL) {
        return COBALT_VPX_BAD_PARAM;
    }
    wrapped = vpx_img_wrap(&h->img, VPX_IMG_FMT_I420,
                           (unsigned int) width, (unsigned int) height,
                           1, (unsigned char *) i420);
    if (wrapped == NULL) {
        return COBALT_VPX_IMG_WRAP_FAILED;
    }
    flags = forceKeyframe ? VPX_EFLAG_FORCE_KF : 0;
    err = vpx_codec_encode(&h->ctx, &h->img, (vpx_codec_pts_t) pts, 1, flags,
                           VPX_DL_REALTIME);
    return (int32_t) err;
}

int32_t cobalt_vpx_encoder_get_packet(void *ctx,
                                      const uint8_t **outBuf,
                                      int32_t *outLen,
                                      int32_t *outIsKey) {
    cobalt_vpx_encoder *h = (cobalt_vpx_encoder *) ctx;
    vpx_codec_iter_t iter = NULL;
    const vpx_codec_cx_pkt_t *pkt;

    if (h == NULL || outBuf == NULL || outLen == NULL || outIsKey == NULL) {
        return COBALT_VPX_BAD_PARAM;
    }
    *outBuf = NULL;
    *outLen = 0;
    *outIsKey = 0;
    while ((pkt = vpx_codec_get_cx_data(&h->ctx, &iter)) != NULL) {
        if (pkt->kind != VPX_CODEC_CX_FRAME_PKT) {
            continue;
        }
        *outBuf = (const uint8_t *) pkt->data.frame.buf;
        *outLen = (int32_t) pkt->data.frame.sz;
        *outIsKey = (pkt->data.frame.flags & VPX_FRAME_IS_KEY) ? 1 : 0;
        break;
    }
    return COBALT_VPX_OK;
}

int32_t cobalt_vpx_encoder_destroy(void *ctx) {
    cobalt_vpx_encoder *h = (cobalt_vpx_encoder *) ctx;
    vpx_codec_err_t err;

    if (h == NULL) {
        return COBALT_VPX_OK;
    }
    err = vpx_codec_destroy(&h->ctx);
    free(h);
    return (int32_t) err;
}

int32_t cobalt_vpx_decoder_create(int32_t codec,
                                  int32_t width,
                                  int32_t height,
                                  void **outCtx) {
    vpx_codec_iface_t *iface;
    vpx_codec_dec_cfg_t cfg;
    vpx_codec_err_t err;
    cobalt_vpx_decoder *h;

    if (outCtx == NULL) {
        return COBALT_VPX_BAD_PARAM;
    }
    *outCtx = NULL;
    iface = cobalt_vpx_dec_iface(codec);
    if (iface == NULL) {
        return COBALT_VPX_BAD_PARAM;
    }

    h = (cobalt_vpx_decoder *) calloc(1, sizeof(*h));
    if (h == NULL) {
        return COBALT_VPX_NOMEM;
    }
    memset(&cfg, 0, sizeof(cfg));
    cfg.threads = 1;
    cfg.w = (unsigned int) width;
    cfg.h = (unsigned int) height;
    err = vpx_codec_dec_init_ver(&h->ctx, iface, &cfg, 0, VPX_DECODER_ABI_VERSION);
    if (err != VPX_CODEC_OK) {
        free(h);
        return (int32_t) err;
    }
    *outCtx = h;
    return COBALT_VPX_OK;
}

int32_t cobalt_vpx_decoder_decode(void *ctx, const uint8_t *data, size_t len) {
    cobalt_vpx_decoder *h = (cobalt_vpx_decoder *) ctx;
    vpx_codec_err_t err;

    if (h == NULL) {
        return COBALT_VPX_BAD_PARAM;
    }
    err = vpx_codec_decode(&h->ctx, data, (unsigned int) len, NULL, 0);
    return (int32_t) err;
}

int32_t cobalt_vpx_decoder_get_frame(void *ctx, void **outImg) {
    cobalt_vpx_decoder *h = (cobalt_vpx_decoder *) ctx;
    vpx_codec_iter_t iter = NULL;

    if (h == NULL || outImg == NULL) {
        return COBALT_VPX_BAD_PARAM;
    }
    *outImg = vpx_codec_get_frame(&h->ctx, &iter);
    return COBALT_VPX_OK;
}

const uint8_t *cobalt_vpx_img_plane(void *img, int32_t plane) {
    vpx_image_t *i = (vpx_image_t *) img;
    if (i == NULL || plane < 0 || plane > 2) {
        return NULL;
    }
    return i->planes[plane];
}

int32_t cobalt_vpx_img_stride(void *img, int32_t plane) {
    vpx_image_t *i = (vpx_image_t *) img;
    if (i == NULL || plane < 0 || plane > 2) {
        return 0;
    }
    return (int32_t) i->stride[plane];
}

int32_t cobalt_vpx_img_width(void *img) {
    vpx_image_t *i = (vpx_image_t *) img;
    return i == NULL ? 0 : (int32_t) i->d_w;
}

int32_t cobalt_vpx_img_height(void *img) {
    vpx_image_t *i = (vpx_image_t *) img;
    return i == NULL ? 0 : (int32_t) i->d_h;
}

int32_t cobalt_vpx_decoder_destroy(void *ctx) {
    cobalt_vpx_decoder *h = (cobalt_vpx_decoder *) ctx;
    vpx_codec_err_t err;

    if (h == NULL) {
        return COBALT_VPX_OK;
    }
    err = vpx_codec_destroy(&h->ctx);
    free(h);
    return (int32_t) err;
}

const char *cobalt_vpx_strerror(int32_t err) {
    switch (err) {
        case COBALT_VPX_BAD_PARAM:       return "cobalt_vpx: invalid argument";
        case COBALT_VPX_NOMEM:           return "cobalt_vpx: out of memory";
        case COBALT_VPX_IMG_WRAP_FAILED: return "cobalt_vpx: vpx_img_wrap failed";
        default: break;
    }
    if (err >= 0) {
        return vpx_codec_err_to_string((vpx_codec_err_t) err);
    }
    return "unknown";
}
