/*
 * cobalt_vpx_shim.h
 *
 * Portable extern-C facade over libvpx (webmproject/libvpx v1.15.1, VP8 and VP9
 * encode and decode) for the Cobalt calls video stack. It re-exposes only the
 * libvpx surface the codec uses, through PORTABLE SCALAR TYPES ONLY (the
 * fixed-width <stdint.h> integers, size_t, and opaque void*), so the
 * jextract-generated Java binding is identical on every host ABI.
 *
 * Why this shim exists: jextract bakes the host C ABI into the bindings it
 * emits. libvpx's public API exchanges several ABI-sensitive shapes:
 * vpx_codec_encode takes `unsigned long duration` and a `vpx_enc_deadline_t`
 * (also `unsigned long`), which is 64-bit on LP64 (Linux, macOS) but 32-bit on
 * LLP64 (Windows); vpx_enc_frame_flags_t is a bare `long`; the configuration
 * (vpx_codec_enc_cfg_t), image (vpx_image_t), context (vpx_codec_ctx_t) and the
 * vpx_codec_cx_pkt_t output union are structs whose layouts carry C enums and
 * size_t members and so differ across ABIs. A binding generated for one ABI
 * ClassCastExceptions or mis-reads fields on another. By keeping every libvpx
 * struct entirely C-side and exchanging only fixed-width scalars and opaque
 * handles across the boundary, the generated binding contains no ABI-sensitive
 * layout and is portable as-is.
 *
 * How the structs are hidden: the encoder and decoder are each held as an opaque
 * void* handle wrapping a heap-allocated vpx_codec_ctx_t (plus, for the encoder,
 * its reusable vpx_image_t header). The configuration is built C-side from
 * default settings stamped with portable scalar arguments and never reaches
 * Java. A compressed packet is returned by pointer into the encoder's own buffer
 * (valid until the next encode call) with its length and key-frame flag as
 * scalars. A decoded picture is returned as the opaque vpx_image_t* (decoder
 * owned, valid until the next decode call) with portable scalar getters (plane
 * pointer, stride, width, height); vpx_image_t, vpx_codec_enc_cfg_t,
 * vpx_codec_dec_cfg_t, vpx_codec_cx_pkt_t and vpx_codec_iter_t are built,
 * consumed and freed entirely C-side.
 *
 * Symbol naming: every exported symbol is prefixed cobalt_vpx_ so it coexists in
 * the combined cobalt-native library with the statically-linked real vpx_codec_*
 * / vpx_img_* symbols, which these wrappers call internally.
 *
 * Portability rule for this header: it uses ONLY uint8_t/int32_t/int64_t/size_t/
 * void*. It never names a libvpx type, and never uses bare `long`,
 * `unsigned long` or `long double`.
 */

#ifndef COBALT_VPX_SHIM_H
#define COBALT_VPX_SHIM_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Codec selectors for cobalt_vpx_encoder_create and cobalt_vpx_decoder_create.
 * The wrapper maps each to the matching libvpx interface getter C-side
 * (vpx_codec_vp8_cx / vpx_codec_vp9_cx for encode, vpx_codec_vp8_dx /
 * vpx_codec_vp9_dx for decode), so the vpx_codec_iface_t pointer never crosses
 * the boundary:
 *
 *   0 = VP8
 *   1 = VP9
 */
#define COBALT_VPX_CODEC_VP8 0
#define COBALT_VPX_CODEC_VP9 1

/*
 * Plane selectors for cobalt_vpx_img_plane and cobalt_vpx_img_stride. The values
 * match libvpx's VPX_PLANE_Y/U/V so a decoded I420 picture is read luma-first:
 *
 *   0 = Y (luma)
 *   1 = U (Cb chroma)
 *   2 = V (Cr chroma)
 */
#define COBALT_VPX_PLANE_Y 0
#define COBALT_VPX_PLANE_U 1
#define COBALT_VPX_PLANE_V 2

/*
 * Status codes returned by the create/encode/decode entry points. Zero is
 * success and equals libvpx's VPX_CODEC_OK. A POSITIVE non-zero value is a
 * libvpx vpx_codec_err_t forwarded verbatim (VPX_CODEC_ERROR = 1,
 * VPX_CODEC_MEM_ERROR = 2, ... VPX_CODEC_INVALID_PARAM and the rest), so the
 * Java side can recover libvpx's own textual description with
 * cobalt_vpx_strerror. NEGATIVE values are shim-level failures that have no
 * libvpx code: an invalid argument supplied to the shim, an allocation failure
 * in the shim, or a failed vpx_img_wrap. Both kinds are handled by
 * cobalt_vpx_strerror.
 */
#define COBALT_VPX_OK              0
#define COBALT_VPX_BAD_PARAM       (-1)
#define COBALT_VPX_NOMEM           (-2)
#define COBALT_VPX_IMG_WRAP_FAILED (-3)

/**
 * Creates a libvpx encoder for one stream, building the encoder configuration
 * C-side from default settings stamped with the supplied scalars.
 *
 * The wrapper resolves the VP8 or VP9 encoder interface from codec, seeds a
 * vpx_codec_enc_cfg_t with vpx_codec_enc_config_default, stamps the picture
 * dimensions, a 1/frameRate timebase, single-pass error-resilient constant
 * settings, the constant-bitrate target (targetBitrateBps converted to libvpx's
 * kilobits per second, floored at 1), the quantizer window, the frame-drop
 * threshold and the keyframe cadence (automatic placement with kfMaxDist when
 * kfMaxDist is positive, otherwise keyframes disabled for request-only mode),
 * pins the encoder to a single thread, initializes the context, and applies the
 * cpuUsed speed control. The created encoder is returned through outCtx as an
 * opaque handle.
 *
 * @param codec            one of COBALT_VPX_CODEC_VP8 / COBALT_VPX_CODEC_VP9.
 * @param width            the picture width in pixels.
 * @param height           the picture height in pixels.
 * @param targetBitrateBps the target bitrate in bits per second.
 * @param frameRate        the frame rate in frames per second; the timebase
 *                         denominator.
 * @param minQuantizer     the minimum (best quality) quantizer.
 * @param maxQuantizer     the maximum (worst quality) quantizer.
 * @param dropframeThresh  the frame-drop threshold percentage, or 0 to disable.
 * @param kfMaxDist        the maximum keyframe interval in frames, or 0 to
 *                         disable automatic keyframes.
 * @param cpuUsed          the VP8E_SET_CPUUSED speed (higher is faster, lower
 *                         quality).
 * @param outCtx           receives the created encoder handle on success, or is
 *                         left holding NULL on failure; must not be NULL.
 * @return COBALT_VPX_OK on success, COBALT_VPX_BAD_PARAM for a NULL outCtx or an
 *         unknown codec, COBALT_VPX_NOMEM if the handle could not be allocated,
 *         otherwise the libvpx vpx_codec_err_t from configuration,
 *         initialization or the speed control.
 */
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
                                  void **outCtx);

/**
 * Reconfigures a live encoder, re-deriving the configuration from libvpx
 * defaults and the supplied scalars and applying it without recreating the
 * context.
 *
 * The wrapper rebuilds the vpx_codec_enc_cfg_t exactly as cobalt_vpx_encoder_create
 * does, applies it with vpx_codec_enc_config_set, and re-applies the cpuUsed
 * speed control. libvpx forbids changing the codec or geometry on a live
 * encoder; the caller must pass the same width and height the encoder was
 * created with.
 *
 * @param ctx              the encoder handle from cobalt_vpx_encoder_create.
 * @param width            the picture width in pixels; must equal the created width.
 * @param height           the picture height in pixels; must equal the created height.
 * @param targetBitrateBps the target bitrate in bits per second.
 * @param frameRate        the frame rate in frames per second.
 * @param minQuantizer     the minimum quantizer.
 * @param maxQuantizer     the maximum quantizer.
 * @param dropframeThresh  the frame-drop threshold percentage, or 0 to disable.
 * @param kfMaxDist        the maximum keyframe interval in frames, or 0 to disable.
 * @param cpuUsed          the VP8E_SET_CPUUSED speed.
 * @return COBALT_VPX_OK on success, COBALT_VPX_BAD_PARAM for a NULL ctx,
 *         otherwise the libvpx vpx_codec_err_t from the reconfiguration or the
 *         speed control.
 */
int32_t cobalt_vpx_encoder_reconfigure(void *ctx,
                                       int32_t width,
                                       int32_t height,
                                       int32_t targetBitrateBps,
                                       int32_t frameRate,
                                       int32_t minQuantizer,
                                       int32_t maxQuantizer,
                                       int32_t dropframeThresh,
                                       int32_t kfMaxDist,
                                       int32_t cpuUsed);

/**
 * Encodes one planar I420 picture at the realtime deadline.
 *
 * The wrapper points the encoder's reusable vpx_image_t header at the supplied
 * planar I420 buffer with vpx_img_wrap, then calls vpx_codec_encode with a
 * duration of one timebase tick, the VPX_DL_REALTIME deadline, and the
 * VPX_EFLAG_FORCE_KF flag when forceKeyframe is non-zero. The pixel buffer is
 * read synchronously and need not outlive this call. Compressed output is
 * retrieved afterwards with cobalt_vpx_encoder_get_packet.
 *
 * @param ctx          the encoder handle from cobalt_vpx_encoder_create.
 * @param i420         the planar I420 pixel buffer (Y plane, then U, then V).
 * @param len          the length of i420 in bytes.
 * @param width        the picture width in pixels; must equal the encoder geometry.
 * @param height       the picture height in pixels; must equal the encoder geometry.
 * @param pts          the strictly-increasing presentation timestamp in timebase units.
 * @param forceKeyframe non-zero to force this picture to a key frame.
 * @return COBALT_VPX_OK on success, COBALT_VPX_BAD_PARAM for a NULL ctx or
 *         buffer, COBALT_VPX_IMG_WRAP_FAILED if vpx_img_wrap rejects the image,
 *         otherwise the libvpx vpx_codec_err_t from vpx_codec_encode.
 */
int32_t cobalt_vpx_encoder_encode(void *ctx,
                                  const uint8_t *i420,
                                  size_t len,
                                  int32_t width,
                                  int32_t height,
                                  int64_t pts,
                                  int32_t forceKeyframe);

/**
 * Retrieves the single compressed-frame packet the realtime encoder produced
 * for the most recent cobalt_vpx_encoder_encode call.
 *
 * The wrapper iterates vpx_codec_get_cx_data with a fresh local iterator until
 * it finds the one VPX_CODEC_CX_FRAME_PKT the realtime encoder emits per input
 * frame, then returns a pointer to that packet's compressed bytes through outBuf,
 * its length through outLen, and its VPX_FRAME_IS_KEY state through outIsKey. The
 * returned pointer addresses libvpx-owned memory valid only until the next
 * cobalt_vpx_* call on this encoder, so the caller must copy the bytes out
 * immediately. When the rate controller dropped the frame, no packet is
 * produced: outBuf is left NULL and outLen is set to 0.
 *
 * @param ctx      the encoder handle from cobalt_vpx_encoder_create.
 * @param outBuf   receives the compressed-bytes pointer, or NULL when no packet
 *                 was produced; must not be NULL.
 * @param outLen   receives the compressed length in bytes, or 0 when no packet
 *                 was produced; must not be NULL.
 * @param outIsKey receives 1 when the packet is a key frame, otherwise 0; must
 *                 not be NULL.
 * @return COBALT_VPX_OK on success, COBALT_VPX_BAD_PARAM for a NULL argument.
 */
int32_t cobalt_vpx_encoder_get_packet(void *ctx,
                                      const uint8_t **outBuf,
                                      int32_t *outLen,
                                      int32_t *outIsKey);

/**
 * Destroys an encoder created by cobalt_vpx_encoder_create and frees its handle.
 *
 * Calls vpx_codec_destroy on the wrapped context and frees the heap-allocated
 * handle. Must be called at most once per handle. A NULL handle is ignored and
 * reported as success.
 *
 * @param ctx the encoder handle, or NULL.
 * @return COBALT_VPX_OK on success, otherwise the libvpx vpx_codec_err_t from
 *         vpx_codec_destroy.
 */
int32_t cobalt_vpx_encoder_destroy(void *ctx);

/**
 * Creates a libvpx decoder for one stream, building the decoder configuration
 * C-side from the supplied scalars.
 *
 * The wrapper resolves the VP8 or VP9 decoder interface from codec, fills a
 * vpx_codec_dec_cfg_t with a single thread and the picture dimensions, and
 * initializes the context. The created decoder is returned through outCtx as an
 * opaque handle.
 *
 * @param codec  one of COBALT_VPX_CODEC_VP8 / COBALT_VPX_CODEC_VP9.
 * @param width  the picture width in pixels.
 * @param height the picture height in pixels.
 * @param outCtx receives the created decoder handle on success, or is left
 *               holding NULL on failure; must not be NULL.
 * @return COBALT_VPX_OK on success, COBALT_VPX_BAD_PARAM for a NULL outCtx or an
 *         unknown codec, COBALT_VPX_NOMEM if the handle could not be allocated,
 *         otherwise the libvpx vpx_codec_err_t from vpx_codec_dec_init.
 */
int32_t cobalt_vpx_decoder_create(int32_t codec,
                                  int32_t width,
                                  int32_t height,
                                  void **outCtx);

/**
 * Feeds one compressed access unit to the decoder.
 *
 * Forwards to vpx_codec_decode with the supplied bytes and an unlimited
 * deadline. Decoded pictures, if any became available, are retrieved afterwards
 * with cobalt_vpx_decoder_get_frame.
 *
 * @param ctx  the decoder handle from cobalt_vpx_decoder_create.
 * @param data the compressed access-unit bytes.
 * @param len  the length of data in bytes.
 * @return COBALT_VPX_OK on success, COBALT_VPX_BAD_PARAM for a NULL ctx,
 *         otherwise the libvpx vpx_codec_err_t from vpx_codec_decode.
 */
int32_t cobalt_vpx_decoder_decode(void *ctx, const uint8_t *data, size_t len);

/**
 * Retrieves the next decoded picture from the decoder.
 *
 * The wrapper iterates vpx_codec_get_frame with a fresh local iterator and
 * returns the first displayable picture through outImg as an opaque handle, or
 * NULL when no picture is ready. The returned picture is decoder-owned and valid
 * only until the next cobalt_vpx_decoder_decode call; its planes are read with
 * cobalt_vpx_img_plane / cobalt_vpx_img_stride and its dimensions with
 * cobalt_vpx_img_width / cobalt_vpx_img_height.
 *
 * @param ctx    the decoder handle from cobalt_vpx_decoder_create.
 * @param outImg receives the decoded picture handle, or NULL when none is ready;
 *               must not be NULL.
 * @return COBALT_VPX_OK on success (including when no picture is ready),
 *         COBALT_VPX_BAD_PARAM for a NULL argument.
 */
int32_t cobalt_vpx_decoder_get_frame(void *ctx, void **outImg);

/**
 * Returns a plane data pointer of a decoded picture.
 *
 * Reads vpx_image_t.planes[plane] of the opaque picture handle. The pointer
 * addresses decoder-owned memory valid until the next cobalt_vpx_decoder_decode
 * call; rows may carry stride padding, so reads must honor cobalt_vpx_img_stride.
 *
 * @param img   the picture handle from cobalt_vpx_decoder_get_frame.
 * @param plane one of COBALT_VPX_PLANE_Y / _U / _V.
 * @return the plane base pointer, or NULL for a NULL handle or out-of-range plane.
 */
const uint8_t *cobalt_vpx_img_plane(void *img, int32_t plane);

/**
 * Returns the row stride, in bytes, of a decoded picture plane.
 *
 * Reads vpx_image_t.stride[plane] of the opaque picture handle. libvpx stores
 * the stride as a plain int, so no widening is needed.
 *
 * @param img   the picture handle from cobalt_vpx_decoder_get_frame.
 * @param plane one of COBALT_VPX_PLANE_Y / _U / _V.
 * @return the plane stride in bytes, or 0 for a NULL handle or out-of-range plane.
 */
int32_t cobalt_vpx_img_stride(void *img, int32_t plane);

/**
 * Returns the displayed width, in pixels, of a decoded picture.
 *
 * Reads vpx_image_t.d_w of the opaque picture handle.
 *
 * @param img the picture handle from cobalt_vpx_decoder_get_frame.
 * @return the picture width in pixels, or 0 for a NULL handle.
 */
int32_t cobalt_vpx_img_width(void *img);

/**
 * Returns the displayed height, in pixels, of a decoded picture.
 *
 * Reads vpx_image_t.d_h of the opaque picture handle.
 *
 * @param img the picture handle from cobalt_vpx_decoder_get_frame.
 * @return the picture height in pixels, or 0 for a NULL handle.
 */
int32_t cobalt_vpx_img_height(void *img);

/**
 * Destroys a decoder created by cobalt_vpx_decoder_create and frees its handle.
 *
 * Calls vpx_codec_destroy on the wrapped context and frees the heap-allocated
 * handle. Must be called at most once per handle. A NULL handle is ignored and
 * reported as success.
 *
 * @param ctx the decoder handle, or NULL.
 * @return COBALT_VPX_OK on success, otherwise the libvpx vpx_codec_err_t from
 *         vpx_codec_destroy.
 */
int32_t cobalt_vpx_decoder_destroy(void *ctx);

/**
 * Returns a textual description of a status code returned by this shim.
 *
 * For a positive libvpx vpx_codec_err_t the description is libvpx's own from
 * vpx_codec_err_to_string; for the negative shim-level codes it is a fixed
 * shim string; for an unrecognized value it is "unknown". The returned pointer
 * is static and must not be freed.
 *
 * @param err a status code returned by another cobalt_vpx_* function.
 * @return a NUL-terminated, statically-allocated description.
 */
const char *cobalt_vpx_strerror(int32_t err);

#ifdef __cplusplus
}
#endif

#endif /* COBALT_VPX_SHIM_H */
