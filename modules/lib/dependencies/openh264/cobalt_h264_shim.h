/*
 * cobalt_h264_shim.h
 *
 * Portable extern-C facade over openh264 (cisco/openh264 v2.4.1, H.264/AVC
 * encode and decode) for the Cobalt calls video stack. It re-exposes only the
 * openh264 surface the codec uses, through PORTABLE SCALAR TYPES ONLY (the
 * fixed-width <stdint.h> integers, size_t, and opaque void*), so the
 * jextract-generated Java binding is identical on every host ABI.
 *
 * Why this shim exists, reason one (ABI portability): jextract bakes the host C
 * ABI into the bindings it emits. openh264's SEncParamExt / SSourcePicture /
 * SFrameBSInfo / SDecodingParam / SBufferInfo carry C enum fields and
 * "long long" timestamps whose width and the C enum width are host-dependent, so
 * a binding generated for one ABI ClassCastExceptions or mis-reads fields on
 * another. By keeping every openh264 struct entirely C-side and exchanging only
 * fixed-width scalars and opaque handles across the boundary, the generated
 * binding contains no ABI-sensitive layout and is portable as-is.
 *
 * Why this shim exists, reason two (vtable dispatch): openh264's runtime API is
 * C++. WelsCreateSVCEncoder yields an ISVCEncoder whose methods are reached only
 * through the ISVCEncoderVtbl function-pointer table ((*enc)->InitializeExt(enc,
 * ...), (*enc)->EncodeFrame(enc, ...), (*enc)->ForceIntraFrame(enc, ...),
 * (*enc)->SetOption(enc, ...)), and WelsCreateDecoder yields an ISVCDecoder
 * driven through ISVCDecoderVtbl ((*dec)->Initialize(dec, ...),
 * (*dec)->DecodeFrameNoDelay(dec, ...)). Driving those vtables from Java FFM
 * (dereferencing the object to its vtable pointer, sizing the vtable layout,
 * reading each method slot, invoking it with the object as the implicit this) is
 * fragile and ABI-sensitive. The shim calls the vtable methods directly C-side,
 * where the C++ compiler resolves them, and exposes a flat extern-C API instead.
 *
 * How the structs and objects are hidden: the encoder and decoder are each held
 * as an opaque void* handle wrapping the C++ object pointer plus the shim's own
 * reusable scratch (the SSourcePicture, SFrameBSInfo, SBufferInfo, the decoder
 * plane-pointer array, and a growable buffer into which the SFrameBSInfo layer
 * NALs are concatenated). An encoded access unit is returned as a pointer into
 * that shim-owned concatenation buffer (valid until the next encode call on the
 * handle) with its length and IDR flag as scalars. A decoded picture is returned
 * as an opaque void* picture handle (the handle's own SBufferInfo, decoder-owned
 * plane memory, valid until the next decode call) with portable scalar getters
 * (plane pointer, stride, width, height). SEncParamExt, SSourcePicture,
 * SFrameBSInfo, SDecodingParam, SBufferInfo and SBitrateInfo are built, consumed
 * and freed entirely C-side and never reach Java.
 *
 * Symbol naming: every exported symbol is prefixed cobalt_h264_ so it coexists
 * in the combined cobalt-native library with the statically-linked real Wels*
 * symbols, which these wrappers call internally.
 *
 * Portability rule for this header: it uses ONLY uint8_t/int32_t/int64_t/size_t/
 * void*. It never names an openh264 type, never declares a C++ type, and never
 * uses bare "long", "unsigned long" or "long double" (the picture timestamp is
 * exposed as a fixed-width int64_t).
 */

#ifndef COBALT_H264_SHIM_H
#define COBALT_H264_SHIM_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Encoder usage-type selectors for cobalt_h264_encoder_create. The values match
 * openh264's EUsageType enumerators so the meaning is unambiguous, but the enum
 * itself never crosses the boundary:
 *
 *   0 = camera video, real-time communication (openh264 CAMERA_VIDEO_REAL_TIME)
 *   1 = screen content, real-time         (openh264 SCREEN_CONTENT_REAL_TIME)
 *
 * The calls codec always opens with the camera real-time usage type.
 */
#define COBALT_H264_USAGE_CAMERA_REALTIME 0
#define COBALT_H264_USAGE_SCREEN_REALTIME 1

/*
 * Rate-control mode selectors for cobalt_h264_encoder_create. The values match
 * openh264's RC_MODES enumerators, but the enum is built C-side:
 *
 *   0 = quality mode      (openh264 RC_QUALITY_MODE)
 *   1 = bitrate mode      (openh264 RC_BITRATE_MODE)
 *   2 = buffer-based mode (openh264 RC_BUFFERBASED_MODE)
 *   3 = timestamp mode    (openh264 RC_TIMESTAMP_MODE)
 *  -1 = rate control off  (openh264 RC_OFF_MODE)
 *
 * The calls codec always opens in bitrate mode.
 */
#define COBALT_H264_RC_QUALITY     0
#define COBALT_H264_RC_BITRATE     1
#define COBALT_H264_RC_BUFFERBASED 2
#define COBALT_H264_RC_TIMESTAMP   3
#define COBALT_H264_RC_OFF         (-1)

/*
 * Complexity-mode selectors for cobalt_h264_encoder_create. The values match
 * openh264's ECOMPLEXITY_MODE enumerators, but the enum is built C-side:
 *
 *   0 = lowest complexity, fastest  (openh264 LOW_COMPLEXITY)
 *   1 = medium complexity, medium   (openh264 MEDIUM_COMPLEXITY)
 *   2 = high complexity, slowest    (openh264 HIGH_COMPLEXITY)
 */
#define COBALT_H264_COMPLEXITY_LOW    0
#define COBALT_H264_COMPLEXITY_MEDIUM 1
#define COBALT_H264_COMPLEXITY_HIGH   2

/*
 * Status codes returned by the create/encode/get/decode entry points. These are
 * STABLE shim-level values, NOT raw openh264 returns: openh264's vtable methods
 * return a CM_RETURN int (0 success) for the encoder and a DECODING_STATE bitmask
 * (0 == dsErrorFree success) for the decoder, neither of which is a portable,
 * stable surface. The shim maps every native success to COBALT_H264_OK, surfaces
 * shim-level misuse and allocation failures as the negative codes below, and
 * forwards any non-zero native failure as COBALT_H264_NATIVE_ERROR; the original
 * native code is recoverable through cobalt_h264_last_native_status.
 *
 *   COBALT_H264_OK            0  operation succeeded
 *   COBALT_H264_BAD_PARAM    -1  invalid argument supplied to the shim
 *   COBALT_H264_NOMEM        -2  allocation failed inside the shim
 *   COBALT_H264_CREATE_FAIL  -3  Wels create entry point failed or returned null
 *   COBALT_H264_NATIVE_ERROR -4  an openh264 vtable method reported a failure
 */
#define COBALT_H264_OK            0
#define COBALT_H264_BAD_PARAM     (-1)
#define COBALT_H264_NOMEM         (-2)
#define COBALT_H264_CREATE_FAIL   (-3)
#define COBALT_H264_NATIVE_ERROR  (-4)

/**
 * Returns the openh264 codec version string.
 *
 * The wrapper reads the OpenH264Version returned by WelsGetCodecVersion C-side
 * and formats it into a static "major.minor.revision" buffer. The returned
 * pointer is static and must not be freed.
 *
 * @return a NUL-terminated version string such as "2.4.1".
 */
const char *cobalt_h264_version(void);

/**
 * Creates an openh264 encoder for one stream, building the SEncParamExt C-side
 * from the supplied portable scalars and initializing the encoder with it.
 *
 * The wrapper calls WelsCreateSVCEncoder, then drives the encoder vtable: it
 * reads GetDefaultParams into a C-side SEncParamExt, stamps that block from the
 * arguments (the usage type from usageType, the picture dimensions, the target
 * and ceiling bitrates, the maximum frame rate, the rate-control mode from
 * rcMode, a single spatial layer and temporalLayers temporal layers, the intra
 * period from intraPeriod, the complexity mode from complexity, the frame-skip
 * toggle, the qp window from minQp and maxQp, the IDR bitrate ratio, and the
 * long-term-reference toggle, mirroring the dimensions, frame rate and bitrates
 * onto spatial layer 0 as openh264 requires alongside the top-level fields),
 * then calls InitializeExt. The color format is fixed to I420, which is the only
 * format the calls encode path feeds. The created encoder is returned through
 * outCtx as an opaque handle.
 *
 * @param width             the picture width in pixels.
 * @param height            the picture height in pixels.
 * @param targetBitrateBps  the target bitrate in bits per second.
 * @param maxBitrateBps     the ceiling bitrate in bits per second.
 * @param frameRate         the maximum frame rate in frames per second.
 * @param usageType         one of COBALT_H264_USAGE_* selecting the usage type.
 * @param rcMode            one of COBALT_H264_RC_* selecting the rate-control mode.
 * @param complexity        one of COBALT_H264_COMPLEXITY_* selecting the speed.
 * @param temporalLayers    the temporal scalability layer count (1 is single-layer).
 * @param intraPeriod       the intra (key-frame) period in frames, or 0 for
 *                          request-only key frames.
 * @param minQp             the minimum quantizer the encoder may pick.
 * @param maxQp             the maximum quantizer the encoder may pick.
 * @param idrBitrateRatio   the percentage by which a key frame's byte budget
 *                          exceeds an inter frame's.
 * @param frameSkip         non-zero to allow the rate controller to drop frames.
 * @param longTermReference non-zero to enable long-term reference frames.
 * @param outCtx            receives the created encoder handle on success, or is
 *                          left holding NULL on failure; must not be NULL.
 * @return COBALT_H264_OK on success, COBALT_H264_BAD_PARAM for a NULL outCtx,
 *         COBALT_H264_NOMEM if the handle could not be allocated,
 *         COBALT_H264_CREATE_FAIL if WelsCreateSVCEncoder fails or returns null,
 *         COBALT_H264_NATIVE_ERROR if GetDefaultParams or InitializeExt fails.
 */
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
                                   void **outCtx);

/**
 * Reconfigures the live encoder's bitrate and frame rate without recreating the
 * encoder.
 *
 * The wrapper drives the encoder vtable SetOption slot twice: once with
 * ENCODER_OPTION_BITRATE and a C-side SBitrateInfo targeting all spatial layers
 * (SPATIAL_LAYER_ALL) carrying targetBitrateBps, then once with
 * ENCODER_OPTION_FRAME_RATE and a C-side float carrying frameRate. These are the
 * two knobs openh264 accepts on a live encoder; changing the quantizer window,
 * frame skip, or the IDR bitrate ratio requires recreating the encoder and is
 * not done here.
 *
 * @param ctx              the encoder handle from cobalt_h264_encoder_create.
 * @param targetBitrateBps the new target bitrate in bits per second.
 * @param frameRate        the new maximum frame rate in frames per second.
 * @return COBALT_H264_OK on success, COBALT_H264_BAD_PARAM for a NULL ctx,
 *         COBALT_H264_NATIVE_ERROR if either SetOption call fails.
 */
int32_t cobalt_h264_encoder_set_rates(void *ctx,
                                      int32_t targetBitrateBps,
                                      int32_t frameRate);

/**
 * Forces the encoder's next encoded picture to be an IDR (instantaneous decoder
 * refresh) key frame.
 *
 * The wrapper drives the encoder vtable ForceIntraFrame slot with a true flag.
 * The request is one-shot: it affects only the next cobalt_h264_encoder_encode
 * call. openh264's ForceIntraFrame returns 1 (not 0) when bIDR is false and
 * nothing is done; the shim only ever passes true, so any non-zero return is a
 * genuine failure.
 *
 * @param ctx the encoder handle from cobalt_h264_encoder_create.
 * @return COBALT_H264_OK on success, COBALT_H264_BAD_PARAM for a NULL ctx,
 *         COBALT_H264_NATIVE_ERROR if ForceIntraFrame fails.
 */
int32_t cobalt_h264_encoder_force_idr(void *ctx);

/**
 * Encodes one planar I420 picture.
 *
 * The wrapper points the handle's reusable C-side SSourcePicture at the supplied
 * planar I420 buffer (an I420 color format, the three plane offsets at the luma
 * and the two quarter-resolution chroma planes, the width and width/2 plane
 * strides, and the supplied millisecond timestamp), zeroes the handle's
 * SFrameBSInfo, and drives the encoder vtable EncodeFrame slot. The pixel buffer
 * is read synchronously and need not outlive this call. The compressed access
 * unit is retrieved afterwards with cobalt_h264_encoder_get_packet; this call
 * does not concatenate it.
 *
 * @param ctx  the encoder handle from cobalt_h264_encoder_create.
 * @param i420 the planar I420 pixel buffer (Y plane, then U, then V).
 * @param len  the length of i420 in bytes; must hold a full I420 picture of the
 *             configured geometry.
 * @param width  the picture width in pixels; must equal the configured geometry.
 * @param height the picture height in pixels; must equal the configured geometry.
 * @param ptsMillis the source timestamp in milliseconds stamped on the picture.
 * @return COBALT_H264_OK on success, COBALT_H264_BAD_PARAM for a NULL ctx or
 *         buffer or a too-small len, COBALT_H264_NATIVE_ERROR if EncodeFrame
 *         fails.
 */
int32_t cobalt_h264_encoder_encode(void *ctx,
                                   const uint8_t *i420,
                                   size_t len,
                                   int32_t width,
                                   int32_t height,
                                   int64_t ptsMillis);

/**
 * Retrieves the compressed access unit produced by the most recent
 * cobalt_h264_encoder_encode call as one contiguous buffer.
 *
 * The wrapper walks the iLayerNum layers of the handle's filled SFrameBSInfo,
 * sums each layer's pNalLengthInByte entries, and concatenates every layer's
 * pBsBuf bitstream (the payloads already carry Annex-B start codes) into the
 * handle's growable concatenation buffer, returning a pointer to it through
 * outBuf, its total length through outLen, and whether SFrameBSInfo.eFrameType is
 * videoFrameTypeIDR through outIsKeyframe. The returned pointer addresses
 * shim-owned memory valid only until the next cobalt_h264_encoder_encode call on
 * this handle, so the caller must copy the bytes out immediately. When the
 * encoder skipped the frame, no NALs are present: outBuf is left NULL, outLen is
 * set to 0, and outIsKeyframe to 0.
 *
 * @param ctx           the encoder handle from cobalt_h264_encoder_create.
 * @param outBuf        receives the concatenated-bytes pointer, or NULL when the
 *                      frame was skipped; must not be NULL.
 * @param outLen        receives the total length in bytes, or 0 when skipped;
 *                      must not be NULL.
 * @param outIsKeyframe receives 1 when the access unit is an IDR key frame,
 *                      otherwise 0; must not be NULL.
 * @return COBALT_H264_OK on success, COBALT_H264_BAD_PARAM for a NULL argument,
 *         COBALT_H264_NOMEM if the concatenation buffer could not be grown.
 */
int32_t cobalt_h264_encoder_get_packet(void *ctx,
                                       const uint8_t **outBuf,
                                       int32_t *outLen,
                                       int32_t *outIsKeyframe);

/**
 * Uninitializes and destroys an encoder created by cobalt_h264_encoder_create
 * and frees its handle.
 *
 * The wrapper drives the encoder vtable Uninitialize slot, calls
 * WelsDestroySVCEncoder on the C++ object, frees the handle's concatenation
 * buffer, and frees the handle. Must be called at most once per handle. A NULL
 * handle is ignored and reported as success.
 *
 * @param ctx the encoder handle, or NULL.
 * @return COBALT_H264_OK on success.
 */
int32_t cobalt_h264_encoder_destroy(void *ctx);

/**
 * Creates an openh264 decoder, initializing it with default decoding parameters.
 *
 * The wrapper calls WelsCreateDecoder, zero-initializes a C-side SDecodingParam
 * (the default decode-only configuration the calls path uses), and drives the
 * decoder vtable Initialize slot with it. The created decoder is returned through
 * outCtx as an opaque handle.
 *
 * @param outCtx receives the created decoder handle on success, or is left
 *               holding NULL on failure; must not be NULL.
 * @return COBALT_H264_OK on success, COBALT_H264_BAD_PARAM for a NULL outCtx,
 *         COBALT_H264_NOMEM if the handle could not be allocated,
 *         COBALT_H264_CREATE_FAIL if WelsCreateDecoder fails or returns null,
 *         COBALT_H264_NATIVE_ERROR if Initialize fails.
 */
int32_t cobalt_h264_decoder_create(void **outCtx);

/**
 * Feeds one compressed access unit to the decoder and reconstructs it.
 *
 * The wrapper zeroes the handle's reusable C-side SBufferInfo and its three-entry
 * plane-pointer array, then drives the decoder vtable DecodeFrameNoDelay slot
 * with the supplied bytes; openh264 parses and reconstructs a complete frame
 * immediately. A decoded picture became available only when DecodeFrameNoDelay
 * succeeded AND the SBufferInfo iBufferStatus is 1; that picture is retrieved
 * afterwards with cobalt_h264_decoder_get_frame. When iBufferStatus is 0 the
 * input was consumed but produced no displayable frame yet, which the get-frame
 * call reports as no picture ready.
 *
 * @param ctx  the decoder handle from cobalt_h264_decoder_create.
 * @param data the compressed access-unit bytes, including Annex-B start codes.
 * @param len  the length of data in bytes.
 * @return COBALT_H264_OK on success (including when no frame is ready),
 *         COBALT_H264_BAD_PARAM for a NULL ctx or buffer,
 *         COBALT_H264_NATIVE_ERROR if DecodeFrameNoDelay reports a decode error.
 */
int32_t cobalt_h264_decoder_decode(void *ctx, const uint8_t *data, size_t len);

/**
 * Retrieves the picture decoded by the most recent cobalt_h264_decoder_decode
 * call.
 *
 * The wrapper checks the handle's SBufferInfo iBufferStatus: when a picture is
 * ready it returns the handle itself through outImg as the opaque picture handle
 * (its planes and dimensions are read with cobalt_h264_img_plane /
 * cobalt_h264_img_stride / cobalt_h264_img_width / cobalt_h264_img_height),
 * otherwise it leaves outImg holding NULL. The returned picture is decoder-owned
 * and valid only until the next cobalt_h264_decoder_decode call on this handle.
 *
 * @param ctx    the decoder handle from cobalt_h264_decoder_create.
 * @param outImg receives the decoded picture handle, or NULL when none is ready;
 *               must not be NULL.
 * @return COBALT_H264_OK on success (including when no picture is ready),
 *         COBALT_H264_BAD_PARAM for a NULL argument.
 */
int32_t cobalt_h264_decoder_get_frame(void *ctx, void **outImg);

/**
 * Returns a plane data pointer of a decoded picture.
 *
 * Reads the SBufferInfo pDst[plane] of the opaque picture handle. Plane 0 is luma
 * (Y), plane 1 is the U (Cb) chroma plane, plane 2 is the V (Cr) chroma plane.
 * The pointer addresses decoder-owned memory valid until the next
 * cobalt_h264_decoder_decode call; rows carry stride padding, so reads must honor
 * cobalt_h264_img_stride.
 *
 * @param img   the picture handle from cobalt_h264_decoder_get_frame.
 * @param plane the plane index, 0, 1 or 2.
 * @return the plane base pointer, or NULL for a NULL handle or out-of-range plane.
 */
const uint8_t *cobalt_h264_img_plane(void *img, int32_t plane);

/**
 * Returns the row stride, in bytes, of a decoded picture plane.
 *
 * Reads the SBufferInfo UsrData.sSystemBuffer iStride of the opaque picture
 * handle: iStride[0] for the luma plane (plane index 0) and iStride[1] for either
 * chroma plane (plane index 1 or 2), as openh264 stores a single shared chroma
 * stride. The stride is a plain int, so no widening is needed.
 *
 * @param img   the picture handle from cobalt_h264_decoder_get_frame.
 * @param plane the plane index, 0 for luma or 1/2 for chroma.
 * @return the plane stride in bytes, or 0 for a NULL handle or out-of-range plane.
 */
int32_t cobalt_h264_img_stride(void *img, int32_t plane);

/**
 * Returns the displayed width, in pixels, of a decoded picture.
 *
 * Reads the SBufferInfo UsrData.sSystemBuffer iWidth of the opaque picture
 * handle.
 *
 * @param img the picture handle from cobalt_h264_decoder_get_frame.
 * @return the picture width in pixels, or 0 for a NULL handle.
 */
int32_t cobalt_h264_img_width(void *img);

/**
 * Returns the displayed height, in pixels, of a decoded picture.
 *
 * Reads the SBufferInfo UsrData.sSystemBuffer iHeight of the opaque picture
 * handle.
 *
 * @param img the picture handle from cobalt_h264_decoder_get_frame.
 * @return the picture height in pixels, or 0 for a NULL handle.
 */
int32_t cobalt_h264_img_height(void *img);

/**
 * Uninitializes and destroys a decoder created by cobalt_h264_decoder_create and
 * frees its handle.
 *
 * The wrapper drives the decoder vtable Uninitialize slot, calls
 * WelsDestroyDecoder on the C++ object, and frees the handle. Must be called at
 * most once per handle. A NULL handle is ignored and reported as success.
 *
 * @param ctx the decoder handle, or NULL.
 * @return COBALT_H264_OK on success.
 */
int32_t cobalt_h264_decoder_destroy(void *ctx);

/**
 * Returns the original native status code from the most recent vtable method that
 * a cobalt_h264_* call on the given handle drove.
 *
 * When a cobalt_h264_* call returns COBALT_H264_NATIVE_ERROR, this returns the
 * underlying openh264 code (the CM_RETURN value for an encoder failure or the
 * DECODING_STATE bitmask for a decoder failure), which is otherwise collapsed
 * into the stable shim surface. The value is meaningful only immediately after a
 * COBALT_H264_NATIVE_ERROR return on the same handle.
 *
 * @param ctx an encoder or decoder handle.
 * @return the last native status code, or 0 for a NULL handle.
 */
int32_t cobalt_h264_last_native_status(void *ctx);

/**
 * Returns a textual description of a status code returned by this shim.
 *
 * The description is a fixed shim string for each COBALT_H264_* code, or
 * "unknown" for an unrecognized value. The returned pointer is static and must
 * not be freed.
 *
 * @param err a status code returned by another cobalt_h264_* function.
 * @return a NUL-terminated, statically-allocated description.
 */
const char *cobalt_h264_strerror(int32_t err);

#ifdef __cplusplus
}
#endif

#endif /* COBALT_H264_SHIM_H */
