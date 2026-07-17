/*
 * cobalt_opus_shim.c
 *
 * Implementation of the portable extern-C libopus facade declared in
 * cobalt_opus_shim.h. Each wrapper carries the libopus encoder, decoder and
 * repacketizer as opaque void* handles, applies each OPUS_SET_* / OPUS_GET_*
 * request C-side from the typed scalar arguments by building the variadic
 * opus_encoder_ctl / opus_decoder_ctl call here, and exchanges only fixed-width
 * scalars and opaque handles across the FFM boundary, so the jextract-generated
 * Java binding is host-ABI independent. opus_packet_parse's array-of-pointers
 * output is converted into fixed-width offset and size arrays.
 *
 * Compiled into the combined cobalt-native shared library by
 * .github/scripts/build-natives.sh (build_opus), linked against the static
 * libopus.a; build_combined forces the cobalt_opus_* symbols (drawn from
 * generate.sh's --include-function list) into the library's export table.
 */

#include "cobalt_opus_shim.h"

#include <stddef.h>

#include "opus/opus.h"

int32_t cobalt_opus_encoder_create(int32_t fs, int32_t channels, int32_t application, void **outEnc) {
    OpusEncoder *enc;
    int err;
    if (outEnc == NULL) {
        return COBALT_OPUS_BAD_ARG;
    }
    *outEnc = NULL;
    err = OPUS_OK;
    enc = opus_encoder_create((opus_int32) fs, (int) channels, (int) application, &err);
    if (err != OPUS_OK || enc == NULL) {
        if (enc != NULL) {
            opus_encoder_destroy(enc);
        }
        return (int32_t) err;
    }
    *outEnc = enc;
    return COBALT_OPUS_OK;
}

void cobalt_opus_encoder_destroy(void *enc) {
    if (enc != NULL) {
        opus_encoder_destroy((OpusEncoder *) enc);
    }
}

int32_t cobalt_opus_encode(void *enc, const int16_t *pcm, int32_t frameSize, uint8_t *data, int32_t maxBytes) {
    if (enc == NULL || pcm == NULL || data == NULL) {
        return COBALT_OPUS_BAD_ARG;
    }
    return (int32_t) opus_encode((OpusEncoder *) enc,
                                 (const opus_int16 *) pcm,
                                 (int) frameSize,
                                 (unsigned char *) data,
                                 (opus_int32) maxBytes);
}

/* Applies one integer-valued OPUS_SET_* request to the encoder C-side. */
static int32_t cobalt_opus_encoder_set_int(void *enc, int request, int32_t value) {
    if (enc == NULL) {
        return COBALT_OPUS_BAD_ARG;
    }
    return (int32_t) opus_encoder_ctl((OpusEncoder *) enc, request, (opus_int32) value);
}

int32_t cobalt_opus_encoder_set_bitrate(void *enc, int32_t bitrateBps) {
    return cobalt_opus_encoder_set_int(enc, OPUS_SET_BITRATE_REQUEST, bitrateBps);
}

int32_t cobalt_opus_encoder_set_vbr(void *enc, int32_t vbr) {
    return cobalt_opus_encoder_set_int(enc, OPUS_SET_VBR_REQUEST, vbr);
}

int32_t cobalt_opus_encoder_set_vbr_constraint(void *enc, int32_t constraint) {
    return cobalt_opus_encoder_set_int(enc, OPUS_SET_VBR_CONSTRAINT_REQUEST, constraint);
}

int32_t cobalt_opus_encoder_set_complexity(void *enc, int32_t complexity) {
    return cobalt_opus_encoder_set_int(enc, OPUS_SET_COMPLEXITY_REQUEST, complexity);
}

int32_t cobalt_opus_encoder_set_inband_fec(void *enc, int32_t fec) {
    return cobalt_opus_encoder_set_int(enc, OPUS_SET_INBAND_FEC_REQUEST, fec);
}

int32_t cobalt_opus_encoder_set_packet_loss_perc(void *enc, int32_t lossPerc) {
    return cobalt_opus_encoder_set_int(enc, OPUS_SET_PACKET_LOSS_PERC_REQUEST, lossPerc);
}

int32_t cobalt_opus_encoder_set_dtx(void *enc, int32_t dtx) {
    return cobalt_opus_encoder_set_int(enc, OPUS_SET_DTX_REQUEST, dtx);
}

int32_t cobalt_opus_encoder_set_force_channels(void *enc, int32_t channels) {
    return cobalt_opus_encoder_set_int(enc, OPUS_SET_FORCE_CHANNELS_REQUEST, channels);
}

int32_t cobalt_opus_encoder_set_signal(void *enc, int32_t signal) {
    return cobalt_opus_encoder_set_int(enc, OPUS_SET_SIGNAL_REQUEST, signal);
}

int32_t cobalt_opus_encoder_set_lsb_depth(void *enc, int32_t depth) {
    return cobalt_opus_encoder_set_int(enc, OPUS_SET_LSB_DEPTH_REQUEST, depth);
}

int32_t cobalt_opus_encoder_set_bandwidth(void *enc, int32_t bandwidth) {
    return cobalt_opus_encoder_set_int(enc, OPUS_SET_BANDWIDTH_REQUEST, bandwidth);
}

int32_t cobalt_opus_encoder_set_max_bandwidth(void *enc, int32_t bandwidth) {
    return cobalt_opus_encoder_set_int(enc, OPUS_SET_MAX_BANDWIDTH_REQUEST, bandwidth);
}

int32_t cobalt_opus_encoder_set_expert_frame_duration(void *enc, int32_t frameDuration) {
    return cobalt_opus_encoder_set_int(enc, OPUS_SET_EXPERT_FRAME_DURATION_REQUEST, frameDuration);
}

int32_t cobalt_opus_encoder_set_prediction_disabled(void *enc, int32_t disabled) {
    return cobalt_opus_encoder_set_int(enc, OPUS_SET_PREDICTION_DISABLED_REQUEST, disabled);
}

int32_t cobalt_opus_encoder_get_lookahead(void *enc, int32_t *out) {
    opus_int32 lookahead;
    int rc;
    if (enc == NULL || out == NULL) {
        return COBALT_OPUS_BAD_ARG;
    }
    lookahead = 0;
    rc = opus_encoder_ctl((OpusEncoder *) enc, OPUS_GET_LOOKAHEAD_REQUEST, &lookahead);
    if (rc == OPUS_OK) {
        *out = (int32_t) lookahead;
    }
    return (int32_t) rc;
}

int32_t cobalt_opus_encoder_reset_state(void *enc) {
    if (enc == NULL) {
        return COBALT_OPUS_BAD_ARG;
    }
    return (int32_t) opus_encoder_ctl((OpusEncoder *) enc, OPUS_RESET_STATE);
}

int32_t cobalt_opus_decoder_create(int32_t fs, int32_t channels, void **outDec) {
    OpusDecoder *dec;
    int err;
    if (outDec == NULL) {
        return COBALT_OPUS_BAD_ARG;
    }
    *outDec = NULL;
    err = OPUS_OK;
    dec = opus_decoder_create((opus_int32) fs, (int) channels, &err);
    if (err != OPUS_OK || dec == NULL) {
        if (dec != NULL) {
            opus_decoder_destroy(dec);
        }
        return (int32_t) err;
    }
    *outDec = dec;
    return COBALT_OPUS_OK;
}

void cobalt_opus_decoder_destroy(void *dec) {
    if (dec != NULL) {
        opus_decoder_destroy((OpusDecoder *) dec);
    }
}

int32_t cobalt_opus_decode(void *dec, const uint8_t *data, int32_t len, int16_t *pcm, int32_t frameSize, int32_t decodeFec) {
    if (dec == NULL || pcm == NULL) {
        return COBALT_OPUS_BAD_ARG;
    }
    return (int32_t) opus_decode((OpusDecoder *) dec,
                                 (const unsigned char *) data,
                                 (opus_int32) len,
                                 (opus_int16 *) pcm,
                                 (int) frameSize,
                                 (int) decodeFec);
}

int32_t cobalt_opus_decoder_reset_state(void *dec) {
    if (dec == NULL) {
        return COBALT_OPUS_BAD_ARG;
    }
    return (int32_t) opus_decoder_ctl((OpusDecoder *) dec, OPUS_RESET_STATE);
}

int32_t cobalt_opus_repacketizer_create(void **outRp) {
    OpusRepacketizer *rp;
    if (outRp == NULL) {
        return COBALT_OPUS_BAD_ARG;
    }
    *outRp = NULL;
    rp = opus_repacketizer_create();
    if (rp == NULL) {
        return COBALT_OPUS_ALLOC_FAIL;
    }
    *outRp = rp;
    return COBALT_OPUS_OK;
}

void cobalt_opus_repacketizer_destroy(void *rp) {
    if (rp != NULL) {
        opus_repacketizer_destroy((OpusRepacketizer *) rp);
    }
}

int32_t cobalt_opus_repacketizer_init(void *rp) {
    if (rp == NULL) {
        return COBALT_OPUS_BAD_ARG;
    }
    opus_repacketizer_init((OpusRepacketizer *) rp);
    return COBALT_OPUS_OK;
}

int32_t cobalt_opus_repacketizer_cat(void *rp, const uint8_t *data, int32_t len) {
    if (rp == NULL || data == NULL) {
        return COBALT_OPUS_BAD_ARG;
    }
    return (int32_t) opus_repacketizer_cat((OpusRepacketizer *) rp,
                                           (const unsigned char *) data,
                                           (opus_int32) len);
}

int32_t cobalt_opus_repacketizer_get_nb_frames(void *rp) {
    if (rp == NULL) {
        return COBALT_OPUS_BAD_ARG;
    }
    return (int32_t) opus_repacketizer_get_nb_frames((OpusRepacketizer *) rp);
}

int32_t cobalt_opus_repacketizer_out_range(void *rp, int32_t begin, int32_t end, uint8_t *data, int32_t maxLen) {
    if (rp == NULL || data == NULL) {
        return COBALT_OPUS_BAD_ARG;
    }
    return (int32_t) opus_repacketizer_out_range((OpusRepacketizer *) rp,
                                                 (int) begin,
                                                 (int) end,
                                                 (unsigned char *) data,
                                                 (opus_int32) maxLen);
}

int32_t cobalt_opus_repacketizer_out(void *rp, uint8_t *data, int32_t maxLen) {
    if (rp == NULL || data == NULL) {
        return COBALT_OPUS_BAD_ARG;
    }
    return (int32_t) opus_repacketizer_out((OpusRepacketizer *) rp,
                                           (unsigned char *) data,
                                           (opus_int32) maxLen);
}

int32_t cobalt_opus_packet_parse(const uint8_t *data,
                                 int32_t len,
                                 uint8_t *outToc,
                                 int32_t *outFrameOffsets,
                                 int32_t *outFrameSizes,
                                 int32_t *outPayloadOffset) {
    unsigned char toc;
    const unsigned char *frames[48];
    opus_int16 sizes[48];
    int payloadOffset;
    int nbFrames;
    int i;

    if (data == NULL || outFrameOffsets == NULL || outFrameSizes == NULL) {
        return COBALT_OPUS_BAD_ARG;
    }
    toc = 0;
    payloadOffset = 0;
    nbFrames = opus_packet_parse((const unsigned char *) data,
                                 (opus_int32) len,
                                 &toc,
                                 frames,
                                 sizes,
                                 &payloadOffset);
    if (nbFrames < 0) {
        return (int32_t) nbFrames;
    }
    for (i = 0; i < nbFrames && i < 48; i++) {
        outFrameOffsets[i] = (int32_t) (frames[i] - (const unsigned char *) data);
        outFrameSizes[i] = (int32_t) sizes[i];
    }
    if (outToc != NULL) {
        *outToc = (uint8_t) toc;
    }
    if (outPayloadOffset != NULL) {
        *outPayloadOffset = (int32_t) payloadOffset;
    }
    return (int32_t) nbFrames;
}

int32_t cobalt_opus_packet_get_nb_frames(const uint8_t *data, int32_t len) {
    if (data == NULL) {
        return COBALT_OPUS_BAD_ARG;
    }
    return (int32_t) opus_packet_get_nb_frames((const unsigned char *) data, (opus_int32) len);
}

int32_t cobalt_opus_packet_get_samples_per_frame(const uint8_t *data, int32_t fs) {
    if (data == NULL) {
        return COBALT_OPUS_BAD_ARG;
    }
    return (int32_t) opus_packet_get_samples_per_frame((const unsigned char *) data, (opus_int32) fs);
}

int32_t cobalt_opus_packet_get_bandwidth(const uint8_t *data) {
    if (data == NULL) {
        return COBALT_OPUS_BAD_ARG;
    }
    return (int32_t) opus_packet_get_bandwidth((const unsigned char *) data);
}

int32_t cobalt_opus_packet_get_nb_channels(const uint8_t *data) {
    if (data == NULL) {
        return COBALT_OPUS_BAD_ARG;
    }
    return (int32_t) opus_packet_get_nb_channels((const unsigned char *) data);
}

int32_t cobalt_opus_packet_has_lbrr(const uint8_t *data, int32_t len) {
    if (data == NULL) {
        return COBALT_OPUS_BAD_ARG;
    }
    return (int32_t) opus_packet_has_lbrr((const unsigned char *) data, (opus_int32) len);
}

const char *cobalt_opus_strerror(int32_t err) {
    return opus_strerror((int) err);
}

const char *cobalt_opus_get_version_string(void) {
    return opus_get_version_string();
}
