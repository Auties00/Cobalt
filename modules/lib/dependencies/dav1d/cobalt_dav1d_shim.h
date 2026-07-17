/*
 * cobalt_dav1d_shim.h
 *
 * Portable extern-C facade over dav1d (VideoLAN dav1d v1.4.3, AV1 DECODE only)
 * for the Cobalt calls video stack. It re-exposes only the dav1d surface the
 * decoder uses, through PORTABLE SCALAR TYPES ONLY (the fixed-width <stdint.h>
 * integers, size_t, and opaque void*), so the jextract-generated Java binding
 * is identical on every host ABI.
 *
 * Why this shim exists, reason one (ABI portability): jextract bakes the host C
 * ABI into the bindings it emits. dav1d's Dav1dPicture carries ptrdiff_t stride
 * members and enum fields, and Dav1dSettings carries `unsigned` and embedded
 * struct members; ptrdiff_t and the C enum width are host-dependent, so a
 * binding generated for one ABI ClassCastExceptions on another. By keeping every
 * dav1d struct entirely C-side and exchanging only fixed-width scalars and
 * opaque handles across the boundary, the generated binding contains no
 * ABI-sensitive layout and is portable as-is.
 *
 * Why this shim exists, reason two (regen unblock): dav1d's raw generate.sh fed
 * the public structs to jextract via --include-struct without listing their
 * dependency structs (Dav1dDataProps, Dav1dRef, Dav1dSequenceHeader, the
 * picture metadata structs), so jextract aborted with "Dav1dPicture depends on
 * Dav1dDataProps which has been excluded". Binding the shim instead of the raw
 * header removes every struct from the binding, so that whole dependency-closure
 * problem disappears.
 *
 * How the structs are hidden: Dav1dContext is held as an opaque void* decoder
 * handle; the decoded Dav1dPicture is heap-allocated C-side and returned as an
 * opaque void* picture handle, with portable scalar getters (plane pointer,
 * stride, width, height, bitdepth, layout) reading its fields. Dav1dData,
 * Dav1dSettings, Dav1dDataProps, Dav1dPicAllocator, Dav1dLogger, Dav1dRef and
 * the picture metadata structs are built, consumed and freed entirely C-side and
 * never reach Java.
 *
 * Symbol naming: every exported symbol is prefixed cobalt_dav1d_ so it coexists
 * in the combined cobalt-native library with the statically-linked real dav1d_*
 * symbols, which these wrappers call internally.
 *
 * Portability rule for this header: it uses ONLY uint8_t/int32_t/int64_t/size_t/
 * void*. It never names a dav1d type, and never uses bare `long`,
 * `unsigned long`, `long double` or `ptrdiff_t` (the picture stride is exposed
 * as a fixed-width int64_t).
 */

#ifndef COBALT_DAV1D_SHIM_H
#define COBALT_DAV1D_SHIM_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Pixel layout selectors returned by cobalt_dav1d_pic_layout. The values match
 * dav1d's Dav1dPixelLayout enumerators so the meaning is unambiguous, but the
 * enum itself never crosses the boundary:
 *
 *   0 = monochrome (I400)
 *   1 = 4:2:0 (I420)
 *   2 = 4:2:2 (I422)
 *   3 = 4:4:4 (I444)
 */
#define COBALT_DAV1D_LAYOUT_I400 0
#define COBALT_DAV1D_LAYOUT_I420 1
#define COBALT_DAV1D_LAYOUT_I422 2
#define COBALT_DAV1D_LAYOUT_I444 3

/*
 * Status codes returned by the open/send/get/close entry points. These are
 * STABLE shim-level values, NOT raw dav1d returns: the shim maps dav1d's negated
 * POSIX error codes (whose numeric values vary by host errno layout) onto this
 * fixed set so the Java side compares against host-independent constants.
 *
 *   COBALT_DAV1D_OK         0   operation succeeded
 *   COBALT_DAV1D_EAGAIN    -1   send: input not yet consumed, drain pictures;
 *                               get: no picture ready, feed more input
 *   COBALT_DAV1D_EINVAL    -2   invalid argument supplied to the shim or dav1d
 *   COBALT_DAV1D_ENOMEM    -3   allocation failed (shim or dav1d)
 *   COBALT_DAV1D_ERROR     -4   any other dav1d decode error
 *
 * COBALT_DAV1D_EAGAIN is the one code callers must branch on (it drives the
 * send/drain decode loop); the rest collapse the various dav1d failures into a
 * stable, portable surface.
 */
#define COBALT_DAV1D_OK      0
#define COBALT_DAV1D_EAGAIN  (-1)
#define COBALT_DAV1D_EINVAL  (-2)
#define COBALT_DAV1D_ENOMEM  (-3)
#define COBALT_DAV1D_ERROR   (-4)

/**
 * Returns the dav1d library version string.
 *
 * Forwards to dav1d_version. The returned pointer is owned by dav1d and is valid
 * for the lifetime of the process; the caller must not free it.
 *
 * @return a NUL-terminated version string such as "1.4.3".
 */
const char *cobalt_dav1d_version(void);

/**
 * Opens an AV1 decoder instance configured for the given thread count.
 *
 * The wrapper initializes a Dav1dSettings with dav1d_default_settings, stamps
 * its n_threads field from nThreads (a value of 0 lets dav1d pick a thread count
 * from the host's logical core count, clamped C-side to dav1d's supported
 * range), then calls dav1d_open. The created decoder is returned through outCtx
 * as an opaque handle. The Dav1dSettings never reaches Java.
 *
 * @param nThreads number of decode threads, or 0 to let dav1d choose.
 * @param outCtx   receives the created decoder handle on success, or is left
 *                 holding NULL on failure; must not be NULL.
 * @return COBALT_DAV1D_OK on success, COBALT_DAV1D_EINVAL for a NULL outCtx,
 *         COBALT_DAV1D_ENOMEM on allocation failure, otherwise
 *         COBALT_DAV1D_ERROR if dav1d_open fails.
 */
int32_t cobalt_dav1d_open(int32_t nThreads, void **outCtx);

/**
 * Feeds one or more AV1 Open Bitstream Units to the decoder.
 *
 * The wrapper allocates a Dav1dData of len bytes with dav1d_data_create, copies
 * the supplied bitstream into it, and calls dav1d_send_data; on success dav1d
 * takes ownership of that reference. A return of COBALT_DAV1D_EAGAIN means the
 * decoder cannot accept this packet until at least one picture is drained with
 * cobalt_dav1d_get_picture; the caller must retry the same data after draining.
 * The Dav1dData never reaches Java.
 *
 * @param ctx  the decoder handle from cobalt_dav1d_open.
 * @param data pointer to the AV1 bitstream bytes; must not be NULL when len > 0.
 * @param len  number of bytes at data.
 * @return COBALT_DAV1D_OK on success, COBALT_DAV1D_EAGAIN if the packet must be
 *         retried after draining, COBALT_DAV1D_EINVAL for an invalid argument,
 *         COBALT_DAV1D_ENOMEM if the data buffer could not be allocated,
 *         otherwise COBALT_DAV1D_ERROR for a dav1d_send_data decode error.
 */
int32_t cobalt_dav1d_send_data(void *ctx, const uint8_t *data, size_t len);

/**
 * Retrieves the next decoded picture from the decoder.
 *
 * The wrapper heap-allocates a Dav1dPicture, calls dav1d_get_picture into it,
 * and on success returns that allocation through outPic as an opaque handle; the
 * caller assumes ownership and must release it with cobalt_dav1d_picture_unref.
 * A return of COBALT_DAV1D_EAGAIN means no picture is ready and more input must
 * be supplied with cobalt_dav1d_send_data; to drain buffered frames at end of
 * stream, call this until it returns COBALT_DAV1D_EAGAIN. On any non-success
 * return outPic is left holding NULL and the internal allocation is freed.
 *
 * @param ctx    the decoder handle from cobalt_dav1d_open.
 * @param outPic receives the decoded picture handle on success, or is left
 *               holding NULL otherwise; must not be NULL.
 * @return COBALT_DAV1D_OK on success, COBALT_DAV1D_EAGAIN if no picture is ready,
 *         COBALT_DAV1D_EINVAL for a NULL outPic, COBALT_DAV1D_ENOMEM if the
 *         picture holder could not be allocated, otherwise COBALT_DAV1D_ERROR
 *         for a dav1d_get_picture decode error.
 */
int32_t cobalt_dav1d_get_picture(void *ctx, void **outPic);

/**
 * Returns a plane data pointer of a decoded picture.
 *
 * Reads Dav1dPicture.data[plane] of the opaque picture handle. Plane 0 is luma
 * (Y), plane 1 is the first chroma plane (U/Cb), plane 2 is the second chroma
 * plane (V/Cr). For 8 bpc the bytes are samples; for 10 bpc they are little
 * words with the sample in the low 10 bits. The pointer addresses dav1d-owned
 * memory valid until cobalt_dav1d_picture_unref is called on the handle.
 *
 * @param pic   the picture handle from cobalt_dav1d_get_picture.
 * @param plane the plane index, 0, 1 or 2.
 * @return the plane base pointer, or NULL for a NULL handle or out-of-range
 *         plane index.
 */
uint8_t *cobalt_dav1d_pic_plane(void *pic, int32_t plane);

/**
 * Returns the row stride, in bytes, of a decoded picture plane.
 *
 * Reads Dav1dPicture.stride[0] for the luma plane (plane index 0) or
 * Dav1dPicture.stride[1] for either chroma plane (plane index 1 or 2), as dav1d
 * stores a single shared chroma stride. The dav1d stride is a ptrdiff_t; it is
 * widened to a fixed-width int64_t so the binding carries no host-dependent
 * type.
 *
 * @param pic   the picture handle from cobalt_dav1d_get_picture.
 * @param plane the plane index, 0 for luma or 1/2 for chroma.
 * @return the plane stride in bytes, or 0 for a NULL handle or out-of-range
 *         plane index.
 */
int64_t cobalt_dav1d_pic_stride(void *pic, int32_t plane);

/**
 * Returns the width, in pixels, of a decoded picture.
 *
 * Reads Dav1dPicture.p.w of the opaque picture handle.
 *
 * @param pic the picture handle from cobalt_dav1d_get_picture.
 * @return the picture width in pixels, or 0 for a NULL handle.
 */
int32_t cobalt_dav1d_pic_width(void *pic);

/**
 * Returns the height, in pixels, of a decoded picture.
 *
 * Reads Dav1dPicture.p.h of the opaque picture handle.
 *
 * @param pic the picture handle from cobalt_dav1d_get_picture.
 * @return the picture height in pixels, or 0 for a NULL handle.
 */
int32_t cobalt_dav1d_pic_height(void *pic);

/**
 * Returns the bit depth, in bits per component, of a decoded picture.
 *
 * Reads Dav1dPicture.p.bpc of the opaque picture handle; the value is 8 or 10.
 *
 * @param pic the picture handle from cobalt_dav1d_get_picture.
 * @return the bits-per-component value (8 or 10), or 0 for a NULL handle.
 */
int32_t cobalt_dav1d_pic_bitdepth(void *pic);

/**
 * Returns the chroma subsampling layout of a decoded picture.
 *
 * Reads Dav1dPicture.p.layout of the opaque picture handle and returns it as one
 * of the COBALT_DAV1D_LAYOUT_* selectors, which share dav1d's enumerator values.
 *
 * @param pic the picture handle from cobalt_dav1d_get_picture.
 * @return one of COBALT_DAV1D_LAYOUT_I400/I420/I422/I444, or -1 for a NULL
 *         handle.
 */
int32_t cobalt_dav1d_pic_layout(void *pic);

/**
 * Releases a decoded picture and frees its holder.
 *
 * Calls dav1d_picture_unref on the opaque handle, releasing dav1d's reference to
 * the underlying frame memory, then frees the heap-allocated Dav1dPicture holder
 * itself. Must be called at most once per handle. A NULL handle is ignored.
 *
 * @param pic the picture handle from cobalt_dav1d_get_picture, or NULL.
 */
void cobalt_dav1d_picture_unref(void *pic);

/**
 * Closes a decoder instance and frees all associated memory.
 *
 * Calls dav1d_close on the opaque decoder handle. Must be called at most once
 * per handle. A NULL handle is ignored and reported as success. Any pictures
 * obtained from this decoder must be released with cobalt_dav1d_picture_unref
 * before or after this call, independently.
 *
 * @param ctx the decoder handle from cobalt_dav1d_open, or NULL.
 * @return COBALT_DAV1D_OK on success.
 */
int32_t cobalt_dav1d_close(void *ctx);

#ifdef __cplusplus
}
#endif

#endif /* COBALT_DAV1D_SHIM_H */
