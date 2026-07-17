/*
 * cobalt_rav1e_shim.c
 *
 * Implementation of the portable extern-C rav1e facade declared in
 * cobalt_rav1e_shim.h. Each wrapper builds the rav1e types (RaConfig, RaFrame,
 * RaRational) on the C side from the portable scalar arguments, calls the real
 * rav1e_* function, and returns only fixed-width scalars and opaque void*
 * handles. No rav1e type ever crosses the FFM boundary, so the
 * jextract-generated Java binding is host-ABI independent. The RaContext is held
 * behind an opaque handle; the compressed RaPacket is handed to Java as an opaque
 * pointer with its data/len/key-flag read C-side as portable scalars.
 *
 * Compiled into the combined cobalt-native shared library by
 * .github/scripts/build-natives.sh (build_rav1e). librav1e.a is NOT part of
 * ffmpeg's static link closure, so build_combined must add BOTH librav1e.a and
 * libcobalt_rav1e_shim.a to extra_archives: the former supplies the real rav1e_*
 * symbols, the latter forces the cobalt_rav1e_* symbols (drawn from generate.sh's
 * --include-function list) into the library's export table.
 */

#include "cobalt_rav1e_shim.h"

#include <stdlib.h>

#include "rav1e.h"

/* Encoder handle: the rav1e context. The RaConfig does not need to outlive
 * rav1e_context_new (it copies the configuration it needs), so it is unref'd at
 * the end of create and is not stored here. */
typedef struct {
    RaContext *ctx;
} cobalt_rav1e_encoder;

/*
 * Maps a rav1e RaEncoderStatus onto the stable COBALT_RAV1E_* surface for the
 * non-receive entry points (create/send), where any status other than success is
 * a failure. rav1e returns RA_ENCODER_STATUS_SUCCESS (0) on success; everything
 * else (FAILURE, NOT_READY, LIMIT_REACHED, and the back-pressure states, which
 * are not expected on send) collapses to COBALT_RAV1E_FAILURE.
 */
static int32_t cobalt_rav1e_map_status(RaEncoderStatus status) {
    return status == RA_ENCODER_STATUS_SUCCESS ? COBALT_RAV1E_OK : COBALT_RAV1E_FAILURE;
}

int32_t cobalt_rav1e_encoder_create(int32_t width,
                                    int32_t height,
                                    int32_t targetBitrateBps,
                                    int32_t fpsNum,
                                    int32_t fpsDen,
                                    int32_t speed,
                                    int32_t keyFrameInterval,
                                    int32_t lowLatency,
                                    int32_t threads,
                                    void **outCtx) {
    RaConfig *cfg;
    RaContext *ctx;
    cobalt_rav1e_encoder *h;
    RaRational time_base;

    if (outCtx == NULL) {
        return COBALT_RAV1E_BAD_PARAM;
    }
    *outCtx = NULL;
    if (width <= 0 || height <= 0 || fpsNum <= 0 || fpsDen <= 0) {
        return COBALT_RAV1E_BAD_PARAM;
    }

    cfg = rav1e_config_default();
    if (cfg == NULL) {
        return COBALT_RAV1E_NOMEM;
    }

    // 8-bit 4:2:0 input: the call video path is I420 8-bit. Chroma sample
    // position is left unknown and the pixel range limited, matching the studio
    // range the upstream pipeline feeds.
    if (rav1e_config_set_pixel_format(cfg, 8, RA_CHROMA_SAMPLING_CS420,
                                      RA_CHROMA_SAMPLE_POSITION_UNKNOWN,
                                      RA_PIXEL_RANGE_LIMITED) != 0) {
        rav1e_config_unref(cfg);
        return COBALT_RAV1E_FAILURE;
    }

    // rav1e's time base is expressed as a duration (the reciprocal of the frame
    // rate), so the numerator is the fps denominator and vice versa.
    time_base.num = (uint64_t) fpsDen;
    time_base.den = (uint64_t) fpsNum;
    rav1e_config_set_time_base(cfg, time_base);

    // Clamp speed to rav1e's documented 0..10 preset range.
    if (speed < 0) {
        speed = 0;
    } else if (speed > 10) {
        speed = 10;
    }

    if (rav1e_config_parse_int(cfg, "width", (int) width) != 0
        || rav1e_config_parse_int(cfg, "height", (int) height) != 0
        || rav1e_config_parse_int(cfg, "speed", (int) speed) != 0
        || rav1e_config_parse_int(cfg, "bitrate", (int) targetBitrateBps) != 0
        || rav1e_config_parse_int(cfg, "threads", (int) (threads < 0 ? 0 : threads)) != 0) {
        rav1e_config_unref(cfg);
        return COBALT_RAV1E_FAILURE;
    }
    if (lowLatency != 0) {
        if (rav1e_config_parse_int(cfg, "low_latency", 1) != 0) {
            rav1e_config_unref(cfg);
            return COBALT_RAV1E_FAILURE;
        }
    }
    if (keyFrameInterval > 0) {
        if (rav1e_config_parse_int(cfg, "key_frame_interval", (int) keyFrameInterval) != 0) {
            rav1e_config_unref(cfg);
            return COBALT_RAV1E_FAILURE;
        }
    }

    ctx = rav1e_context_new(cfg);
    rav1e_config_unref(cfg);
    if (ctx == NULL) {
        return COBALT_RAV1E_FAILURE;
    }

    h = (cobalt_rav1e_encoder *) calloc(1, sizeof(*h));
    if (h == NULL) {
        rav1e_context_unref(ctx);
        return COBALT_RAV1E_NOMEM;
    }
    h->ctx = ctx;
    *outCtx = h;
    return COBALT_RAV1E_OK;
}

int32_t cobalt_rav1e_encoder_send(void *ctx,
                                  const uint8_t *i420,
                                  size_t len,
                                  int32_t width,
                                  int32_t height) {
    cobalt_rav1e_encoder *h = (cobalt_rav1e_encoder *) ctx;
    RaFrame *frame;
    RaEncoderStatus status;
    size_t y_size;
    size_t c_size;
    int cw;
    int ch;

    if (h == NULL || i420 == NULL || width <= 0 || height <= 0) {
        return COBALT_RAV1E_BAD_PARAM;
    }
    cw = width / 2;
    ch = height / 2;
    y_size = (size_t) width * (size_t) height;
    c_size = (size_t) cw * (size_t) ch;
    if (len != y_size + 2u * c_size) {
        return COBALT_RAV1E_BAD_PARAM;
    }

    frame = rav1e_frame_new(h->ctx);
    if (frame == NULL) {
        return COBALT_RAV1E_NOMEM;
    }

    // I420: plane 0 = Y (full resolution), plane 1 = U, plane 2 = V (each half
    // resolution in both dimensions); strides equal the plane widths and the
    // samples are one byte wide.
    rav1e_frame_fill_plane(frame, 0, i420, y_size, (ptrdiff_t) width, 1);
    rav1e_frame_fill_plane(frame, 1, i420 + y_size, c_size, (ptrdiff_t) cw, 1);
    rav1e_frame_fill_plane(frame, 2, i420 + y_size + c_size, c_size, (ptrdiff_t) cw, 1);

    status = rav1e_send_frame(h->ctx, frame);
    rav1e_frame_unref(frame);
    return cobalt_rav1e_map_status(status);
}

int32_t cobalt_rav1e_encoder_receive(void *ctx,
                                     const uint8_t **outBuf,
                                     int32_t *outLen,
                                     int32_t *outIsKey,
                                     void **outPacket) {
    cobalt_rav1e_encoder *h = (cobalt_rav1e_encoder *) ctx;
    RaPacket *pkt = NULL;
    RaEncoderStatus status;

    if (h == NULL || outBuf == NULL || outLen == NULL || outIsKey == NULL || outPacket == NULL) {
        return COBALT_RAV1E_BAD_PARAM;
    }
    *outLen = 0;
    *outIsKey = 0;
    *outPacket = NULL;

    status = rav1e_receive_packet(h->ctx, &pkt);
    switch (status) {
        case RA_ENCODER_STATUS_SUCCESS:
            if (pkt == NULL) {
                return COBALT_RAV1E_FAILURE;
            }
            *outBuf = pkt->data;
            *outLen = (int32_t) pkt->len;
            *outIsKey = (pkt->frame_type == RA_FRAME_TYPE_KEY) ? 1 : 0;
            *outPacket = (void *) pkt;
            return COBALT_RAV1E_OK;
        case RA_ENCODER_STATUS_NEED_MORE_DATA:
        case RA_ENCODER_STATUS_ENOUGH_DATA:
            // Back-pressure: no packet is ready, the caller must feed more input.
            return COBALT_RAV1E_OK;
        default:
            return COBALT_RAV1E_FAILURE;
    }
}

void cobalt_rav1e_packet_unref(void *packet) {
    if (packet == NULL) {
        return;
    }
    rav1e_packet_unref((RaPacket *) packet);
}

int32_t cobalt_rav1e_encoder_destroy(void *ctx) {
    cobalt_rav1e_encoder *h = (cobalt_rav1e_encoder *) ctx;
    if (h == NULL) {
        return COBALT_RAV1E_OK;
    }
    if (h->ctx != NULL) {
        rav1e_context_unref(h->ctx);
    }
    free(h);
    return COBALT_RAV1E_OK;
}

const char *cobalt_rav1e_strerror(int32_t status) {
    switch (status) {
        case COBALT_RAV1E_OK:        return "cobalt_rav1e: ok";
        case COBALT_RAV1E_BAD_PARAM: return "cobalt_rav1e: invalid argument";
        case COBALT_RAV1E_NOMEM:     return "cobalt_rav1e: out of memory";
        case COBALT_RAV1E_FAILURE:   return "cobalt_rav1e: rav1e encode failure";
        default:                     return "unknown";
    }
}
