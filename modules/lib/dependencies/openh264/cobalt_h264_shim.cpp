/*
 * cobalt_h264_shim.cpp
 *
 * Implementation of the portable extern-C openh264 facade declared in
 * cobalt_h264_shim.h. openh264's runtime API is C++: this translation unit is
 * compiled as C++, includes openh264's codec_api.h, and calls the ISVCEncoder /
 * ISVCDecoder methods directly as C++ virtual calls (encoder->InitializeExt(...),
 * encoder->EncodeFrame(...), decoder->DecodeFrameNoDelay(...)), which the C++
 * compiler resolves through the object vtables. Each wrapper builds the openh264
 * structs (SEncParamExt, SSourcePicture, SFrameBSInfo, SDecodingParam,
 * SBufferInfo, SBitrateInfo) on the C side from the portable scalar arguments and
 * returns only fixed-width scalars and opaque void* handles. No openh264 type and
 * no C++ type ever crosses the FFM boundary, so the jextract-generated Java
 * binding is host-ABI independent.
 *
 * The wrapper functions themselves carry extern "C" linkage so the exported
 * symbols are plain C names; the C++ machinery (the openh264 classes, their
 * vtables) stays inside this object and is reached only through these wrappers.
 *
 * Compiled into the combined cobalt-native shared library by
 * .github/scripts/build-natives.sh, linked against the static openh264 archive;
 * build_combined forces the cobalt_h264_* symbols (drawn from generate.sh's
 * --include-function list) into the library's export table.
 *
 * Build note: this file is compiled as C++ (it drives openh264's C++ ISVCEncoder
 * / ISVCDecoder objects). It needs the openh264 public headers on the include
 * path: codec_api.h (and the codec_app_def.h / codec_def.h it pulls in) live
 * directly under codec/api/wels in the openh264 source, vendored to this
 * directory's headers/ by build_openh264's vendor_headers. The compile must pass
 * -I pointing at the directory that holds codec_api.h (the openh264 source's
 * codec/api/wels, or this directory's headers/) and -I pointing at this directory
 * for cobalt_h264_shim.h.
 */

#include "cobalt_h264_shim.h"

#include <cstdlib>
#include <cstring>
#include <cstdio>

#include "codec_api.h"

/*
 * Common handle prefix shared by the encoder and decoder handles. Placing it as
 * the first member of both structs gives them a common initial sequence, so
 * cobalt_h264_last_native_status can read lastNativeStatus through a pointer to
 * either handle without knowing which kind it is. lastNativeStatus holds the most
 * recent native status (a CM_RETURN for an encoder failure or a DECODING_STATE
 * for a decoder failure) so it can be reported after a COBALT_H264_NATIVE_ERROR.
 */
typedef struct {
    int lastNativeStatus;
} cobalt_h264_common;

/*
 * Encoder handle: the openh264 C++ object plus the shim's reusable scratch. The
 * SSourcePicture and SFrameBSInfo are reused across encode calls; concatBuf is a
 * growable buffer the emitted layer NALs are concatenated into and owned by the
 * handle, so its pointer stays valid until the next encode.
 */
typedef struct {
    cobalt_h264_common common;
    ISVCEncoder *enc;
    SSourcePicture pic;
    SFrameBSInfo bsInfo;
    unsigned char *concatBuf;
    size_t concatCap;
} cobalt_h264_encoder;

/*
 * Decoder handle: the openh264 C++ object plus the shim's reusable scratch. The
 * SBufferInfo and the three plane pointers are reused across decode calls; the
 * handle itself is the opaque picture handle the getters read, so its SBufferInfo
 * stays valid until the next decode.
 */
typedef struct {
    cobalt_h264_common common;
    ISVCDecoder *dec;
    SBufferInfo bufInfo;
    unsigned char *planes[3];
} cobalt_h264_decoder;

/* Maps a COBALT_H264_USAGE_* selector onto an openh264 EUsageType. */
static EUsageType cobalt_h264_usage(int32_t usageType) {
    switch (usageType) {
        case COBALT_H264_USAGE_SCREEN_REALTIME: return SCREEN_CONTENT_REAL_TIME;
        case COBALT_H264_USAGE_CAMERA_REALTIME:
        default:                                return CAMERA_VIDEO_REAL_TIME;
    }
}

/* Maps a COBALT_H264_RC_* selector onto an openh264 RC_MODES. */
static RC_MODES cobalt_h264_rc_mode(int32_t rcMode) {
    switch (rcMode) {
        case COBALT_H264_RC_QUALITY:     return RC_QUALITY_MODE;
        case COBALT_H264_RC_BUFFERBASED: return RC_BUFFERBASED_MODE;
        case COBALT_H264_RC_TIMESTAMP:   return RC_TIMESTAMP_MODE;
        case COBALT_H264_RC_OFF:         return RC_OFF_MODE;
        case COBALT_H264_RC_BITRATE:
        default:                         return RC_BITRATE_MODE;
    }
}

/* Maps a COBALT_H264_COMPLEXITY_* selector onto an openh264 ECOMPLEXITY_MODE. */
static ECOMPLEXITY_MODE cobalt_h264_complexity(int32_t complexity) {
    switch (complexity) {
        case COBALT_H264_COMPLEXITY_MEDIUM: return MEDIUM_COMPLEXITY;
        case COBALT_H264_COMPLEXITY_HIGH:   return HIGH_COMPLEXITY;
        case COBALT_H264_COMPLEXITY_LOW:
        default:                            return LOW_COMPLEXITY;
    }
}

extern "C" {

const char *cobalt_h264_version(void) {
    static char buf[32];
    OpenH264Version v = WelsGetCodecVersion();
    std::snprintf(buf, sizeof(buf), "%u.%u.%u", v.uMajor, v.uMinor, v.uRevision);
    return buf;
}

int32_t cobalt_h264_encoder_create(int32_t width,
                                   int32_t height,
                                   int32_t targetBitrateBps,
                                   int32_t maxBitrateBps,
                                   int32_t frameRate,
                                   int32_t usageType,
                                   int32_t rcMode,
                                   int32_t complexity,
                                   int32_t temporalLayers,
                                   int32_t intraPeriod,
                                   int32_t minQp,
                                   int32_t maxQp,
                                   int32_t idrBitrateRatio,
                                   int32_t frameSkip,
                                   int32_t longTermReference,
                                   void **outCtx) {
    if (outCtx == NULL) {
        return COBALT_H264_BAD_PARAM;
    }
    *outCtx = NULL;

    cobalt_h264_encoder *h = (cobalt_h264_encoder *) std::calloc(1, sizeof(*h));
    if (h == NULL) {
        return COBALT_H264_NOMEM;
    }

    ISVCEncoder *enc = NULL;
    int rc = WelsCreateSVCEncoder(&enc);
    if (rc != 0 || enc == NULL) {
        std::free(h);
        return COBALT_H264_CREATE_FAIL;
    }

    SEncParamExt param;
    std::memset(&param, 0, sizeof(param));
    rc = enc->GetDefaultParams(&param);
    if (rc != 0) {
        h->common.lastNativeStatus = rc;
        WelsDestroySVCEncoder(enc);
        std::free(h);
        return COBALT_H264_NATIVE_ERROR;
    }

    param.iUsageType = cobalt_h264_usage(usageType);
    param.iPicWidth = width;
    param.iPicHeight = height;
    param.iTargetBitrate = targetBitrateBps;
    param.iMaxBitrate = maxBitrateBps;
    param.fMaxFrameRate = (float) frameRate;
    param.iRCMode = cobalt_h264_rc_mode(rcMode);
    param.iSpatialLayerNum = 1;
    param.iTemporalLayerNum = temporalLayers;
    param.uiIntraPeriod = (unsigned int) intraPeriod;
    param.iComplexityMode = cobalt_h264_complexity(complexity);
    param.bEnableFrameSkip = frameSkip ? true : false;
    param.iMinQp = minQp;
    param.iMaxQp = maxQp;
    param.iIdrBitrateRatio = idrBitrateRatio;
    param.bEnableLongTermReference = longTermReference ? true : false;

    SSpatialLayerConfig *layer0 = &param.sSpatialLayers[0];
    layer0->iVideoWidth = width;
    layer0->iVideoHeight = height;
    layer0->fFrameRate = (float) frameRate;
    layer0->iSpatialBitrate = targetBitrateBps;
    layer0->iMaxSpatialBitrate = maxBitrateBps;

    rc = enc->InitializeExt(&param);
    if (rc != 0) {
        h->common.lastNativeStatus = rc;
        WelsDestroySVCEncoder(enc);
        std::free(h);
        return COBALT_H264_NATIVE_ERROR;
    }

    h->enc = enc;
    *outCtx = h;
    return COBALT_H264_OK;
}

int32_t cobalt_h264_encoder_set_rates(void *ctx,
                                      int32_t targetBitrateBps,
                                      int32_t frameRate) {
    cobalt_h264_encoder *h = (cobalt_h264_encoder *) ctx;
    if (h == NULL || h->enc == NULL) {
        return COBALT_H264_BAD_PARAM;
    }

    SBitrateInfo bitrate;
    std::memset(&bitrate, 0, sizeof(bitrate));
    bitrate.iLayer = SPATIAL_LAYER_ALL;
    bitrate.iBitrate = targetBitrateBps;
    int rc = h->enc->SetOption(ENCODER_OPTION_BITRATE, &bitrate);
    if (rc != 0) {
        h->common.lastNativeStatus = rc;
        return COBALT_H264_NATIVE_ERROR;
    }

    float fps = (float) frameRate;
    rc = h->enc->SetOption(ENCODER_OPTION_FRAME_RATE, &fps);
    if (rc != 0) {
        h->common.lastNativeStatus = rc;
        return COBALT_H264_NATIVE_ERROR;
    }
    return COBALT_H264_OK;
}

int32_t cobalt_h264_encoder_force_idr(void *ctx) {
    cobalt_h264_encoder *h = (cobalt_h264_encoder *) ctx;
    if (h == NULL || h->enc == NULL) {
        return COBALT_H264_BAD_PARAM;
    }
    int rc = h->enc->ForceIntraFrame(true);
    if (rc != 0) {
        h->common.lastNativeStatus = rc;
        return COBALT_H264_NATIVE_ERROR;
    }
    return COBALT_H264_OK;
}

int32_t cobalt_h264_encoder_encode(void *ctx,
                                   const uint8_t *i420,
                                   size_t len,
                                   int32_t width,
                                   int32_t height,
                                   int64_t ptsMillis) {
    cobalt_h264_encoder *h = (cobalt_h264_encoder *) ctx;
    if (h == NULL || h->enc == NULL || i420 == NULL) {
        return COBALT_H264_BAD_PARAM;
    }
    size_t lumaSize = (size_t) width * (size_t) height;
    size_t chromaSize = (size_t) (width / 2) * (size_t) (height / 2);
    if (len < lumaSize + 2 * chromaSize) {
        return COBALT_H264_BAD_PARAM;
    }

    // The encoder reads the planes synchronously during EncodeFrame, so pointing
    // pData into the caller's buffer for the duration of the call is safe; the
    // const is dropped because openh264's pData is unsigned char* but the encoder
    // never writes through it.
    unsigned char *base = (unsigned char *) i420;
    std::memset(&h->pic, 0, sizeof(h->pic));
    h->pic.iColorFormat = videoFormatI420;
    h->pic.iPicWidth = width;
    h->pic.iPicHeight = height;
    h->pic.iStride[0] = width;
    h->pic.iStride[1] = width / 2;
    h->pic.iStride[2] = width / 2;
    h->pic.pData[0] = base;
    h->pic.pData[1] = base + lumaSize;
    h->pic.pData[2] = base + lumaSize + chromaSize;
    h->pic.uiTimeStamp = (long long) ptsMillis;

    std::memset(&h->bsInfo, 0, sizeof(h->bsInfo));
    int rc = h->enc->EncodeFrame(&h->pic, &h->bsInfo);
    if (rc != 0) {
        h->common.lastNativeStatus = rc;
        return COBALT_H264_NATIVE_ERROR;
    }
    return COBALT_H264_OK;
}

int32_t cobalt_h264_encoder_get_packet(void *ctx,
                                       const uint8_t **outBuf,
                                       int32_t *outLen,
                                       int32_t *outIsKeyframe) {
    cobalt_h264_encoder *h = (cobalt_h264_encoder *) ctx;
    if (h == NULL || h->enc == NULL || outBuf == NULL || outLen == NULL || outIsKeyframe == NULL) {
        return COBALT_H264_BAD_PARAM;
    }
    *outBuf = NULL;
    *outLen = 0;
    *outIsKeyframe = 0;

    int layerCount = h->bsInfo.iLayerNum;
    size_t total = 0;
    for (int i = 0; i < layerCount; i++) {
        SLayerBSInfo *layer = &h->bsInfo.sLayerInfo[i];
        for (int n = 0; n < layer->iNalCount; n++) {
            total += (size_t) layer->pNalLengthInByte[n];
        }
    }
    if (total == 0) {
        return COBALT_H264_OK;
    }

    if (h->concatCap < total) {
        unsigned char *grown = (unsigned char *) std::realloc(h->concatBuf, total);
        if (grown == NULL) {
            return COBALT_H264_NOMEM;
        }
        h->concatBuf = grown;
        h->concatCap = total;
    }

    size_t offset = 0;
    for (int i = 0; i < layerCount; i++) {
        SLayerBSInfo *layer = &h->bsInfo.sLayerInfo[i];
        size_t layerSize = 0;
        for (int n = 0; n < layer->iNalCount; n++) {
            layerSize += (size_t) layer->pNalLengthInByte[n];
        }
        std::memcpy(h->concatBuf + offset, layer->pBsBuf, layerSize);
        offset += layerSize;
    }

    *outBuf = h->concatBuf;
    *outLen = (int32_t) total;
    *outIsKeyframe = (h->bsInfo.eFrameType == videoFrameTypeIDR) ? 1 : 0;
    return COBALT_H264_OK;
}

int32_t cobalt_h264_encoder_destroy(void *ctx) {
    cobalt_h264_encoder *h = (cobalt_h264_encoder *) ctx;
    if (h == NULL) {
        return COBALT_H264_OK;
    }
    if (h->enc != NULL) {
        h->enc->Uninitialize();
        WelsDestroySVCEncoder(h->enc);
    }
    std::free(h->concatBuf);
    std::free(h);
    return COBALT_H264_OK;
}

int32_t cobalt_h264_decoder_create(void **outCtx) {
    if (outCtx == NULL) {
        return COBALT_H264_BAD_PARAM;
    }
    *outCtx = NULL;

    cobalt_h264_decoder *h = (cobalt_h264_decoder *) std::calloc(1, sizeof(*h));
    if (h == NULL) {
        return COBALT_H264_NOMEM;
    }

    ISVCDecoder *dec = NULL;
    long rc = WelsCreateDecoder(&dec);
    if (rc != 0 || dec == NULL) {
        std::free(h);
        return COBALT_H264_CREATE_FAIL;
    }

    SDecodingParam decParam;
    std::memset(&decParam, 0, sizeof(decParam));
    long initRc = dec->Initialize(&decParam);
    if (initRc != 0) {
        h->common.lastNativeStatus = (int) initRc;
        WelsDestroyDecoder(dec);
        std::free(h);
        return COBALT_H264_NATIVE_ERROR;
    }

    h->dec = dec;
    *outCtx = h;
    return COBALT_H264_OK;
}

int32_t cobalt_h264_decoder_decode(void *ctx, const uint8_t *data, size_t len) {
    cobalt_h264_decoder *h = (cobalt_h264_decoder *) ctx;
    if (h == NULL || h->dec == NULL || data == NULL) {
        return COBALT_H264_BAD_PARAM;
    }

    std::memset(&h->bufInfo, 0, sizeof(h->bufInfo));
    h->planes[0] = NULL;
    h->planes[1] = NULL;
    h->planes[2] = NULL;

    DECODING_STATE state = h->dec->DecodeFrameNoDelay(data, (int) len, h->planes, &h->bufInfo);
    if (state != dsErrorFree) {
        h->common.lastNativeStatus = (int) state;
        return COBALT_H264_NATIVE_ERROR;
    }
    return COBALT_H264_OK;
}

int32_t cobalt_h264_decoder_get_frame(void *ctx, void **outImg) {
    cobalt_h264_decoder *h = (cobalt_h264_decoder *) ctx;
    if (h == NULL || outImg == NULL) {
        return COBALT_H264_BAD_PARAM;
    }
    *outImg = (h->bufInfo.iBufferStatus == 1) ? (void *) h : NULL;
    return COBALT_H264_OK;
}

const uint8_t *cobalt_h264_img_plane(void *img, int32_t plane) {
    cobalt_h264_decoder *h = (cobalt_h264_decoder *) img;
    if (h == NULL || plane < 0 || plane > 2) {
        return NULL;
    }
    return h->bufInfo.pDst[plane];
}

int32_t cobalt_h264_img_stride(void *img, int32_t plane) {
    cobalt_h264_decoder *h = (cobalt_h264_decoder *) img;
    if (h == NULL || plane < 0 || plane > 2) {
        return 0;
    }
    // openh264 stores two strides: index 0 is luma, index 1 is the shared chroma
    // stride, so plane 2 (V) reads the same stride cell as plane 1 (U).
    int strideIndex = (plane == 0) ? 0 : 1;
    return h->bufInfo.UsrData.sSystemBuffer.iStride[strideIndex];
}

int32_t cobalt_h264_img_width(void *img) {
    cobalt_h264_decoder *h = (cobalt_h264_decoder *) img;
    return h == NULL ? 0 : h->bufInfo.UsrData.sSystemBuffer.iWidth;
}

int32_t cobalt_h264_img_height(void *img) {
    cobalt_h264_decoder *h = (cobalt_h264_decoder *) img;
    return h == NULL ? 0 : h->bufInfo.UsrData.sSystemBuffer.iHeight;
}

int32_t cobalt_h264_decoder_destroy(void *ctx) {
    cobalt_h264_decoder *h = (cobalt_h264_decoder *) ctx;
    if (h == NULL) {
        return COBALT_H264_OK;
    }
    if (h->dec != NULL) {
        h->dec->Uninitialize();
        WelsDestroyDecoder(h->dec);
    }
    std::free(h);
    return COBALT_H264_OK;
}

int32_t cobalt_h264_last_native_status(void *ctx) {
    // cobalt_h264_encoder and cobalt_h264_decoder both embed cobalt_h264_common as
    // their first member, giving them a common initial sequence; reading
    // lastNativeStatus through the shared prefix is well defined for a handle of
    // either kind.
    if (ctx == NULL) {
        return 0;
    }
    return ((cobalt_h264_common *) ctx)->lastNativeStatus;
}

const char *cobalt_h264_strerror(int32_t err) {
    switch (err) {
        case COBALT_H264_OK:           return "cobalt_h264: ok";
        case COBALT_H264_BAD_PARAM:    return "cobalt_h264: invalid argument";
        case COBALT_H264_NOMEM:        return "cobalt_h264: out of memory";
        case COBALT_H264_CREATE_FAIL:  return "cobalt_h264: Wels create failed";
        case COBALT_H264_NATIVE_ERROR: return "cobalt_h264: openh264 vtable method failed";
        default:                       return "unknown";
    }
}

} // extern "C"
