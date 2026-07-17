/*
 * cobalt_rav1e_shim.h
 *
 * Portable extern-C facade over rav1e (the Rust AV1 ENCODER, exposed through its
 * cargo-c generated C API rav1e.h) for the Cobalt calls video stack. It
 * re-exposes only the rav1e surface the AV1 encoder uses, through PORTABLE
 * SCALAR TYPES ONLY (the fixed-width <stdint.h> integers, size_t, and opaque
 * void*), so the jextract-generated Java binding is identical on every host ABI.
 *
 * Why this shim exists (ABI portability): jextract bakes the host C ABI into the
 * bindings it emits. rav1e's C API is ABI-sensitive in several places:
 * rav1e_frame_fill_plane takes a `ptrdiff_t stride` and a `size_t data_len`;
 * rav1e_send_frame / rav1e_receive_packet return a C enum (RaEncoderStatus)
 * whose width is host-dependent; rav1e_config_set_pixel_format takes the C enums
 * RaChromaSampling / RaChromaSamplePosition / RaPixelRange; rav1e_config_set_time_base
 * takes a RaRational struct by value; and the RaPacket the caller must read
 * (const uint8_t* data; size_t len; uint64_t input_frameno; RaFrameType frame_type;
 * void* opaque; RaFrame* rec; RaFrame* source) is a struct whose layout carries a
 * C enum field. A binding generated for one ABI ClassCastExceptions or mis-reads
 * fields on another. By keeping every Ra* type entirely C-side and exchanging
 * only fixed-width scalars and opaque handles across the boundary, the generated
 * binding contains no ABI-sensitive layout and is portable as-is.
 *
 * How the rav1e types are hidden: the encoder is held as an opaque void* handle
 * wrapping a heap-allocated struct that owns the RaContext (and the RaConfig is
 * unref'd C-side once the context is built, since rav1e_context_new copies the
 * configuration it needs). A RaFrame is allocated, filled and unref'd entirely
 * inside cobalt_rav1e_encoder_send and never reaches Java. A compressed RaPacket
 * is returned by pointer into rav1e-owned memory (its data/len/frame_type read
 * C-side and handed back as portable scalars), with the opaque RaPacket* itself
 * returned so the caller can release it with cobalt_rav1e_packet_unref. RaConfig,
 * RaFrame, RaRational and the RaChromaSampling / RaChromaSamplePosition /
 * RaPixelRange / RaEncoderStatus / RaFrameType enums are built, consumed and
 * freed entirely C-side.
 *
 * Symbol naming: every exported symbol is prefixed cobalt_rav1e_ so it coexists
 * in the combined cobalt-native library with the statically-linked real rav1e_*
 * symbols, which these wrappers call internally.
 *
 * Portability rule for this header: it uses ONLY uint8_t/int32_t/int64_t/size_t/
 * void*. It never names a rav1e type, and never uses bare `long`,
 * `unsigned long`, `long double` or `ptrdiff_t` (the plane stride is handled
 * C-side; the compressed length is exposed as a fixed-width int32_t).
 */

#ifndef COBALT_RAV1E_SHIM_H
#define COBALT_RAV1E_SHIM_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Status codes returned by the create/send/receive/destroy entry points. These
 * are STABLE shim-level values, NOT raw rav1e returns: the shim maps rav1e's
 * RaEncoderStatus (RA_ENCODER_STATUS_SUCCESS = 0, _NEED_MORE_DATA, _ENOUGH_DATA,
 * _LIMIT_REACHED, _ENCODED, _FAILURE = -1, _NOT_READY = -2) onto this fixed set
 * so the Java side compares against host-independent constants.
 *
 *   COBALT_RAV1E_OK         0   operation succeeded; for cobalt_rav1e_encoder_receive
 *                               this is also returned when no packet is ready yet
 *                               (the back-pressure states RA_ENCODER_STATUS_NEED_MORE_DATA
 *                               and RA_ENCODER_STATUS_ENOUGH_DATA), with *outLen = 0
 *                               and *outPacket = NULL
 *   COBALT_RAV1E_BAD_PARAM -1   invalid argument supplied to the shim
 *   COBALT_RAV1E_NOMEM     -2   allocation failed (shim handle, RaFrame or RaContext)
 *   COBALT_RAV1E_FAILURE   -3   any other rav1e encode failure
 *                               (RA_ENCODER_STATUS_FAILURE, _NOT_READY, _LIMIT_REACHED)
 *
 * On cobalt_rav1e_encoder_receive the caller branches on the return being
 * COBALT_RAV1E_OK and then on whether *outPacket is non-NULL: a non-NULL packet
 * is one encoded access unit to send and release, a NULL packet means feed more
 * input. The other functions return COBALT_RAV1E_OK or one of the negatives.
 */
#define COBALT_RAV1E_OK         0
#define COBALT_RAV1E_BAD_PARAM  (-1)
#define COBALT_RAV1E_NOMEM      (-2)
#define COBALT_RAV1E_FAILURE    (-3)

/**
 * Creates a rav1e AV1 encoder for one stream, building the rav1e configuration
 * C-side from default settings stamped with the supplied scalars.
 *
 * The wrapper allocates a RaConfig with rav1e_config_default, sets the pixel
 * format to 8-bit 4:2:0 (RA_CHROMA_SAMPLING_CS420, unknown chroma position,
 * limited range) with rav1e_config_set_pixel_format, sets a fpsNum/fpsDen
 * timebase with rav1e_config_set_time_base, and applies the geometry and rate
 * controls through rav1e_config_parse_int using the rav1e key strings "width",
 * "height", "bitrate", "speed", "threads", and (conditionally) "low_latency"
 * and "key_frame_interval". It then builds the RaContext with rav1e_context_new
 * and releases the RaConfig with rav1e_config_unref. The created encoder is
 * returned through outCtx as an opaque handle.
 *
 * <p>speed is clamped to rav1e's 0..10 range (10 is fastest, lowest quality).
 * When lowLatency is non-zero the wrapper sets rav1e's "low_latency" flag to 1.
 * When keyFrameInterval is positive the wrapper sets rav1e's "key_frame_interval"
 * to that value; when it is 0 or negative rav1e's default keyframe cadence is
 * left in place. threads is forwarded verbatim as rav1e's "threads" (0 lets
 * rav1e pick a thread count). targetBitrateBps is forwarded verbatim as rav1e's
 * "bitrate" (bits per second).
 *
 * @param width            the picture width in pixels.
 * @param height           the picture height in pixels.
 * @param targetBitrateBps the target bitrate in bits per second.
 * @param fpsNum           the timebase numerator (frames-per-second numerator).
 * @param fpsDen           the timebase denominator (frames-per-second denominator).
 * @param speed            the rav1e speed preset, clamped C-side to 0..10.
 * @param keyFrameInterval the maximum keyframe interval in frames, or 0 to leave
 *                         rav1e's default cadence.
 * @param lowLatency       non-zero to enable rav1e's low-latency mode.
 * @param threads          the rav1e thread count, or 0 to let rav1e choose.
 * @param outCtx           receives the created encoder handle on success, or is
 *                         left holding NULL on failure; must not be NULL.
 * @return COBALT_RAV1E_OK on success, COBALT_RAV1E_BAD_PARAM for a NULL outCtx,
 *         COBALT_RAV1E_NOMEM if the handle, RaConfig or RaContext could not be
 *         allocated, otherwise COBALT_RAV1E_FAILURE if rav1e rejected the
 *         configuration or context creation.
 */
int32_t cobalt_rav1e_encoder_create(int32_t width,
                                    int32_t height,
                                    int32_t targetBitrateBps,
                                    int32_t fpsNum,
                                    int32_t fpsDen,
                                    int32_t speed,
                                    int32_t keyFrameInterval,
                                    int32_t lowLatency,
                                    int32_t threads,
                                    void **outCtx);

/**
 * Sends one planar I420 picture to the encoder.
 *
 * The wrapper allocates a RaFrame with rav1e_frame_new, fills its three planes
 * from the supplied I420 buffer with rav1e_frame_fill_plane (plane 0 = Y with
 * stride width, plane 1 = U with stride width/2, plane 2 = V with stride
 * width/2, each at bytewidth 1 for 8-bit samples), submits it with
 * rav1e_send_frame, and releases the RaFrame with rav1e_frame_unref. The pixel
 * buffer is read synchronously and need not outlive this call. Compressed output
 * is retrieved afterwards with cobalt_rav1e_encoder_receive. The RaFrame never
 * reaches Java.
 *
 * <p>The buffer must be a contiguous I420 layout of width*height luma bytes
 * followed by two (width/2)*(height/2) chroma planes; len is validated against
 * that expected size.
 *
 * @param ctx    the encoder handle from cobalt_rav1e_encoder_create.
 * @param i420   the planar I420 pixel buffer (Y plane, then U, then V).
 * @param len    the length of i420 in bytes; must be the I420 size for the
 *               geometry.
 * @param width  the picture width in pixels; must equal the encoder geometry.
 * @param height the picture height in pixels; must equal the encoder geometry.
 * @return COBALT_RAV1E_OK on success, COBALT_RAV1E_BAD_PARAM for a NULL ctx or
 *         buffer or a len that does not match the geometry, COBALT_RAV1E_NOMEM
 *         if the RaFrame could not be allocated, otherwise COBALT_RAV1E_FAILURE
 *         if rav1e_send_frame reported a failure.
 */
int32_t cobalt_rav1e_encoder_send(void *ctx,
                                  const uint8_t *i420,
                                  size_t len,
                                  int32_t width,
                                  int32_t height);

/**
 * Retrieves the next compressed access unit the encoder produced.
 *
 * The wrapper calls rav1e_receive_packet. On RA_ENCODER_STATUS_SUCCESS it sets
 * outBuf to the packet's data pointer, outLen to its length, outIsKey to 1 when
 * the packet's frame type is RA_FRAME_TYPE_KEY (otherwise 0), and outPacket to
 * the opaque RaPacket pointer the caller must later release with
 * cobalt_rav1e_packet_unref; the returned data pointer addresses rav1e-owned
 * memory valid until that release. On the back-pressure statuses
 * RA_ENCODER_STATUS_NEED_MORE_DATA and RA_ENCODER_STATUS_ENOUGH_DATA it sets
 * outLen to 0 and outPacket to NULL and returns COBALT_RAV1E_OK, signalling the
 * caller to feed another frame. On any other status it returns COBALT_RAV1E_FAILURE
 * with outPacket NULL.
 *
 * @param ctx       the encoder handle from cobalt_rav1e_encoder_create.
 * @param outBuf    receives the compressed-bytes pointer when a packet is ready,
 *                  otherwise left unchanged; must not be NULL.
 * @param outLen    receives the compressed length in bytes, or 0 when no packet
 *                  is ready; must not be NULL.
 * @param outIsKey  receives 1 when the packet is a key frame, otherwise 0; must
 *                  not be NULL.
 * @param outPacket receives the opaque RaPacket pointer to release, or NULL when
 *                  no packet is ready; must not be NULL.
 * @return COBALT_RAV1E_OK on success (including when no packet is ready),
 *         COBALT_RAV1E_BAD_PARAM for a NULL argument, otherwise
 *         COBALT_RAV1E_FAILURE for a rav1e encode failure.
 */
int32_t cobalt_rav1e_encoder_receive(void *ctx,
                                     const uint8_t **outBuf,
                                     int32_t *outLen,
                                     int32_t *outIsKey,
                                     void **outPacket);

/**
 * Releases a compressed packet obtained from cobalt_rav1e_encoder_receive.
 *
 * Calls rav1e_packet_unref on the opaque RaPacket handle, freeing the packet and
 * the rav1e-owned compressed bytes it referenced. Must be called at most once per
 * handle, after the caller has copied the bytes out. A NULL handle is ignored.
 *
 * @param packet the RaPacket handle from cobalt_rav1e_encoder_receive, or NULL.
 */
void cobalt_rav1e_packet_unref(void *packet);

/**
 * Destroys an encoder created by cobalt_rav1e_encoder_create and frees its
 * handle.
 *
 * Calls rav1e_context_unref on the wrapped RaContext and frees the heap-allocated
 * handle. Must be called at most once per handle. A NULL handle is ignored and
 * reported as success. Any packets obtained from this encoder must be released
 * with cobalt_rav1e_packet_unref independently.
 *
 * @param ctx the encoder handle from cobalt_rav1e_encoder_create, or NULL.
 * @return COBALT_RAV1E_OK on success.
 */
int32_t cobalt_rav1e_encoder_destroy(void *ctx);

/**
 * Returns a textual description of a status code returned by this shim.
 *
 * For each COBALT_RAV1E_* value it returns a fixed shim string; for an
 * unrecognized value it returns "unknown". The returned pointer is static and
 * must not be freed.
 *
 * @param status a status code returned by another cobalt_rav1e_* function.
 * @return a NUL-terminated, statically-allocated description.
 */
const char *cobalt_rav1e_strerror(int32_t status);

#ifdef __cplusplus
}
#endif

#endif /* COBALT_RAV1E_SHIM_H */
