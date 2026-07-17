/*
 * cobalt_dav1d_shim.c
 *
 * Implementation of the portable extern-C dav1d facade declared in
 * cobalt_dav1d_shim.h. Each wrapper builds the dav1d structs (Dav1dSettings,
 * Dav1dData, Dav1dPicture) on the C side, calls the real dav1d_* function, and
 * returns only fixed-width scalars and opaque void* handles. No dav1d type ever
 * crosses the FFM boundary, so the jextract-generated Java binding is host-ABI
 * independent. The decoded Dav1dPicture is heap-allocated here and handed to
 * Java as an opaque pointer; portable getters read its plane pointers, strides
 * and parameters.
 *
 * Compiled into the combined cobalt-native shared library by
 * .github/scripts/build-natives.sh (build_av1), linked against the static
 * libdav1d.a; the export union in build_combined forces the cobalt_dav1d_*
 * symbols (drawn from generate.sh's --include-function list) into the library's
 * export table.
 */

#include "cobalt_dav1d_shim.h"

#include <errno.h>
#include <stdlib.h>
#include <string.h>

#include "dav1d.h"

/*
 * Maps a dav1d return value (0 or a negated POSIX error code) onto the stable
 * COBALT_DAV1D_* status surface. dav1d negates errno values via DAV1D_ERR, so
 * the comparisons use the host's EAGAIN/EINVAL/ENOMEM; the caller never sees
 * those host-dependent numbers, only the fixed shim codes.
 */
static int32_t cobalt_dav1d_map_status(int rc) {
    if (rc == 0) {
        return COBALT_DAV1D_OK;
    }
    if (rc == DAV1D_ERR(EAGAIN)) {
        return COBALT_DAV1D_EAGAIN;
    }
    if (rc == DAV1D_ERR(EINVAL)) {
        return COBALT_DAV1D_EINVAL;
    }
    if (rc == DAV1D_ERR(ENOMEM)) {
        return COBALT_DAV1D_ENOMEM;
    }
    return COBALT_DAV1D_ERROR;
}

/*
 * Maps a dav1d Dav1dPixelLayout enumerator onto the COBALT_DAV1D_LAYOUT_*
 * surface. The numeric values already coincide, but the indirection keeps the
 * dav1d enum type out of the boundary and out of the header.
 */
static int32_t cobalt_dav1d_map_layout(enum Dav1dPixelLayout layout) {
    switch (layout) {
        case DAV1D_PIXEL_LAYOUT_I400:
            return COBALT_DAV1D_LAYOUT_I400;
        case DAV1D_PIXEL_LAYOUT_I420:
            return COBALT_DAV1D_LAYOUT_I420;
        case DAV1D_PIXEL_LAYOUT_I422:
            return COBALT_DAV1D_LAYOUT_I422;
        case DAV1D_PIXEL_LAYOUT_I444:
            return COBALT_DAV1D_LAYOUT_I444;
        default:
            return COBALT_DAV1D_LAYOUT_I420;
    }
}

const char *cobalt_dav1d_version(void) {
    return dav1d_version();
}

int32_t cobalt_dav1d_open(int32_t nThreads, void **outCtx) {
    if (outCtx == NULL) {
        return COBALT_DAV1D_EINVAL;
    }
    *outCtx = NULL;

    Dav1dSettings settings;
    dav1d_default_settings(&settings);
    if (nThreads < 0) {
        nThreads = 0;
    } else if (nThreads > DAV1D_MAX_THREADS) {
        nThreads = DAV1D_MAX_THREADS;
    }
    settings.n_threads = (int) nThreads;

    Dav1dContext *ctx = NULL;
    int rc = dav1d_open(&ctx, &settings);
    if (rc != 0) {
        return cobalt_dav1d_map_status(rc);
    }
    *outCtx = (void *) ctx;
    return COBALT_DAV1D_OK;
}

int32_t cobalt_dav1d_send_data(void *ctx, const uint8_t *data, size_t len) {
    if (ctx == NULL || (data == NULL && len > 0)) {
        return COBALT_DAV1D_EINVAL;
    }

    Dav1dData in;
    memset(&in, 0, sizeof(in));
    // dav1d_data_create returns a writable buffer owned by the reference-counted
    // Dav1dData; copy the caller's bitstream into it so the caller's pointer need
    // not outlive this call. On success dav1d_send_data takes ownership of the
    // reference; on failure it is released below so no reference leaks.
    uint8_t *buf = dav1d_data_create(&in, len);
    if (buf == NULL) {
        return COBALT_DAV1D_ENOMEM;
    }
    if (len > 0) {
        memcpy(buf, data, len);
    }

    int rc = dav1d_send_data((Dav1dContext *) ctx, &in);
    if (rc != 0) {
        dav1d_data_unref(&in);
    }
    return cobalt_dav1d_map_status(rc);
}

int32_t cobalt_dav1d_get_picture(void *ctx, void **outPic) {
    if (outPic == NULL) {
        return COBALT_DAV1D_EINVAL;
    }
    *outPic = NULL;
    if (ctx == NULL) {
        return COBALT_DAV1D_EINVAL;
    }

    Dav1dPicture *pic = (Dav1dPicture *) calloc(1, sizeof(Dav1dPicture));
    if (pic == NULL) {
        return COBALT_DAV1D_ENOMEM;
    }

    int rc = dav1d_get_picture((Dav1dContext *) ctx, pic);
    if (rc != 0) {
        free(pic);
        return cobalt_dav1d_map_status(rc);
    }
    *outPic = (void *) pic;
    return COBALT_DAV1D_OK;
}

uint8_t *cobalt_dav1d_pic_plane(void *pic, int32_t plane) {
    if (pic == NULL || plane < 0 || plane > 2) {
        return NULL;
    }
    return (uint8_t *) ((Dav1dPicture *) pic)->data[plane];
}

int64_t cobalt_dav1d_pic_stride(void *pic, int32_t plane) {
    if (pic == NULL || plane < 0 || plane > 2) {
        return 0;
    }
    // dav1d stores two strides: [0] for luma, [1] shared by both chroma planes.
    int idx = (plane == 0) ? 0 : 1;
    return (int64_t) ((Dav1dPicture *) pic)->stride[idx];
}

int32_t cobalt_dav1d_pic_width(void *pic) {
    if (pic == NULL) {
        return 0;
    }
    return (int32_t) ((Dav1dPicture *) pic)->p.w;
}

int32_t cobalt_dav1d_pic_height(void *pic) {
    if (pic == NULL) {
        return 0;
    }
    return (int32_t) ((Dav1dPicture *) pic)->p.h;
}

int32_t cobalt_dav1d_pic_bitdepth(void *pic) {
    if (pic == NULL) {
        return 0;
    }
    return (int32_t) ((Dav1dPicture *) pic)->p.bpc;
}

int32_t cobalt_dav1d_pic_layout(void *pic) {
    if (pic == NULL) {
        return -1;
    }
    return cobalt_dav1d_map_layout(((Dav1dPicture *) pic)->p.layout);
}

void cobalt_dav1d_picture_unref(void *pic) {
    if (pic == NULL) {
        return;
    }
    dav1d_picture_unref((Dav1dPicture *) pic);
    free(pic);
}

int32_t cobalt_dav1d_close(void *ctx) {
    if (ctx == NULL) {
        return COBALT_DAV1D_OK;
    }
    Dav1dContext *c = (Dav1dContext *) ctx;
    dav1d_close(&c);
    return COBALT_DAV1D_OK;
}
