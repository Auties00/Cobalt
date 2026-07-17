// SPDX-License-Identifier: MIT

#ifndef RAV1E_H
#define RAV1E_H


#define RAV1E_MAJOR 0
#define RAV1E_MINOR 7
#define RAV1E_PATCH 1


#include <stddef.h>
#include <stdint.h>
#include <stdlib.h>

/**
 * Chroma subsampling format
 */
typedef enum {
    /**
     * Both vertically and horizontally subsampled.
     */
    RA_CHROMA_SAMPLING_CS420,
    /**
     * Horizontally subsampled.
     */
    RA_CHROMA_SAMPLING_CS422,
    /**
     * Not subsampled.
     */
    RA_CHROMA_SAMPLING_CS444,
    /**
     * Monochrome.
     */
    RA_CHROMA_SAMPLING_CS400,
} RaChromaSampling;

/**
 * Sample position for subsampled chroma
 */
typedef enum {
    /**
     * The source video transfer function must be signaled
     * outside the AV1 bitstream.
     */
    RA_CHROMA_SAMPLE_POSITION_UNKNOWN,
    /**
     * Horizontally co-located with (0, 0) luma sample, vertically positioned
     * in the middle between two luma samples.
     */
    RA_CHROMA_SAMPLE_POSITION_VERTICAL,
    /**
     * Co-located with (0, 0) luma sample.
     */
    RA_CHROMA_SAMPLE_POSITION_COLOCATED,
} RaChromaSamplePosition;

/**
 * Allowed pixel value range
 *
 * C.f. `VideoFullRangeFlag` variable specified in ISO/IEC 23091-4/ITU-T H.273
 */
typedef enum {
    /**
     * Studio swing representation
     */
    RA_PIXEL_RANGE_LIMITED,
    /**
     * Full swing representation
     */
    RA_PIXEL_RANGE_FULL,
} RaPixelRange;

/**
 * Matrix coefficients
 *
 * As defined by the “Matrix coefficients” section of ISO/IEC 23091-4/ITU-TH.273.
 */
typedef enum {
    /**
     * Identity matrix
     */
    RA_MATRIX_COEFFICIENTS_IDENTITY = 0,
    /**
     * BT.709
     */
    RA_MATRIX_COEFFICIENTS_BT709,
    /**
     * Unspecified, must be signaled or inferred outside of the bitstream.
     */
    RA_MATRIX_COEFFICIENTS_UNSPECIFIED,
    /**
     * US FCC 73.628
     */
    RA_MATRIX_COEFFICIENTS_FCC = 4,
    /**
     * BT.470 System B, G (historical)
     */
    RA_MATRIX_COEFFICIENTS_BT470BG,
    /**
     * BT.601-7 525 (SMPTE 170 M)
     */
    RA_MATRIX_COEFFICIENTS_BT601,
    /**
     * SMPTE 240 M
     */
    RA_MATRIX_COEFFICIENTS_SMPTE240,
    /**
     * YCgCo
     */
    RA_MATRIX_COEFFICIENTS_Y_CG_CO,
    /**
     * BT.2020 non-constant luminance, BT.2100 YCbCr
     */
    RA_MATRIX_COEFFICIENTS_BT2020NCL,
    /**
     * BT.2020 constant luminance
     */
    RA_MATRIX_COEFFICIENTS_BT2020CL,
    /**
     * SMPTE ST 2085 YDzDx
     */
    RA_MATRIX_COEFFICIENTS_SMPTE2085,
    /**
     * Chromaticity-derived non-constant luminance
     */
    RA_MATRIX_COEFFICIENTS_CHROMAT_NCL,
    /**
     * Chromaticity-derived constant luminance
     */
    RA_MATRIX_COEFFICIENTS_CHROMAT_CL,
    /**
     * BT.2020 ICtCp
     */
    RA_MATRIX_COEFFICIENTS_I_CT_CP,
} RaMatrixCoefficients;

/**
 * Supported Color Primaries
 *
 * As defined by “Color primaries” section of ISO/IEC 23091-4/ITU-T H.273
 */
typedef enum {
    /**
     * BT.709
     */
    RA_COLOR_PRIMARIES_BT709 = 1,
    /**
     * Unspecified, must be signaled or inferred outside of the bitstream
     */
    RA_COLOR_PRIMARIES_UNSPECIFIED,
    /**
     * BT.470 System M (historical)
     */
    RA_COLOR_PRIMARIES_BT470M = 4,
    /**
     * BT.470 System B, G (historical)
     */
    RA_COLOR_PRIMARIES_BT470BG,
    /**
     * BT.601-7 525 (SMPTE 170 M)
     */
    RA_COLOR_PRIMARIES_BT601,
    /**
     * SMPTE 240M (historical)
     */
    RA_COLOR_PRIMARIES_SMPTE240,
    /**
     * Generic film
     */
    RA_COLOR_PRIMARIES_GENERIC_FILM,
    /**
     * BT.2020, BT.2100
     */
    RA_COLOR_PRIMARIES_BT2020,
    /**
     * SMPTE 248 (CIE 1921 XYZ)
     */
    RA_COLOR_PRIMARIES_XYZ,
    /**
     * SMPTE RP 431-2
     */
    RA_COLOR_PRIMARIES_SMPTE431,
    /**
     * SMPTE EG 432-1
     */
    RA_COLOR_PRIMARIES_SMPTE432,
    /**
     * EBU Tech. 3213-E
     */
    RA_COLOR_PRIMARIES_EBU3213 = 22,
} RaColorPrimaries;

/**
 * Supported Transfer Characteristics
 *
 * As defined by “Transfer characteristics” section of ISO/IEC 23091-4/ITU-TH.273.
 */
typedef enum {
    /**
     * BT.709
     */
    RA_TRANSFER_CHARACTERISTICS_BT709 = 1,
    /**
     * Unspecified, must be signaled or inferred outside of the bitstream
     */
    RA_TRANSFER_CHARACTERISTICS_UNSPECIFIED,
    /**
     * BT.470 System M (historical)
     */
    RA_TRANSFER_CHARACTERISTICS_BT470M = 4,
    /**
     * BT.470 System B, G (historical)
     */
    RA_TRANSFER_CHARACTERISTICS_BT470BG,
    /**
     * BT.601-7 525 (SMPTE 170 M)
     */
    RA_TRANSFER_CHARACTERISTICS_BT601,
    /**
     * SMPTE 240 M
     */
    RA_TRANSFER_CHARACTERISTICS_SMPTE240,
    /**
     * Linear
     */
    RA_TRANSFER_CHARACTERISTICS_LINEAR,
    /**
     * Logarithmic (100:1 range)
     */
    RA_TRANSFER_CHARACTERISTICS_LOG100,
    /**
     * Logarithmic ((100 * √10):1 range)
     */
    RA_TRANSFER_CHARACTERISTICS_LOG100_SQRT10,
    /**
     * IEC 61966-2-4
     */
    RA_TRANSFER_CHARACTERISTICS_IEC61966,
    /**
     * BT.1361 extended color gamut system (historical)
     */
    RA_TRANSFER_CHARACTERISTICS_BT1361,
    /**
     * sRGB or sYCC
     */
    RA_TRANSFER_CHARACTERISTICS_SRGB,
    /**
     * BT.2020 10-bit systems
     */
    RA_TRANSFER_CHARACTERISTICS_BT2020_10BIT,
    /**
     * BT.2020 12-bit systems
     */
    RA_TRANSFER_CHARACTERISTICS_BT2020_12BIT,
    /**
     * SMPTE ST 2084, ITU BT.2100 PQ
     */
    RA_TRANSFER_CHARACTERISTICS_SMPTE2084,
    /**
     * SMPTE ST 428
     */
    RA_TRANSFER_CHARACTERISTICS_SMPTE428,
    /**
     * BT.2100 HLG (Hybrid Log Gamma), ARIB STD-B67
     */
    RA_TRANSFER_CHARACTERISTICS_HLG,
} RaTransferCharacteristics;

/**
 * Override the frame type decision
 *
 * Only certain frame types can be selected.
 */
typedef enum {
    /**
     * Do not force any decision.
     */
    RA_FRAME_TYPE_OVERRIDE_NO,
    /**
     * Force the frame to be a Keyframe.
     */
    RA_FRAME_TYPE_OVERRIDE_KEY,
} RaFrameTypeOverride;

/**
 * Rate Control Data
 */
typedef enum {
    /**
     * A Rate Control Summary Packet
     *
     * It is emitted once, after the encoder is flushed.
     *
     * It contains a summary of the rate control information for the
     * encoding process that just terminated.
     */
    RA_RC_DATA_KIND_SUMMARY,
    /**
     * A Rate Control Frame-specific Packet
     *
     * It is emitted every time a frame is processed.
     *
     * The information contained is required to encode its matching
     * frame in a second pass encoding.
     */
    RA_RC_DATA_KIND_FRAME,
    /**
     * There is no pass data available for now
     *
     * This is emitted if `rav1e_rc_receive_pass_data` is called more
     * often than it should.
     */
    RA_RC_DATA_KIND_EMPTY,
} RaRcDataKind;

/**
 * Status that can be returned by encoder functions.
 */
typedef enum {
    /**
     * Normal operation.
     */
    RA_ENCODER_STATUS_SUCCESS = 0,
    /**
     * The encoder needs more data to produce an output packet.
     *
     * May be emitted by `rav1e_receive_packet` when frame reordering is
     * enabled.
     */
    RA_ENCODER_STATUS_NEED_MORE_DATA,
    /**
     * There are enough frames in the queue.
     *
     * May be emitted by `rav1e_send_frame` when trying to send a frame after
     * the encoder has been flushed or the internal queue is full.
     */
    RA_ENCODER_STATUS_ENOUGH_DATA,
    /**
     * The encoder has already produced the number of frames requested.
     *
     * May be emitted by `rav1e_receive_packet` after a flush request had been
     * processed or the frame limit had been reached.
     */
    RA_ENCODER_STATUS_LIMIT_REACHED,
    /**
     * A Frame had been encoded but not emitted yet.
     */
    RA_ENCODER_STATUS_ENCODED,
    /**
     * Generic fatal error.
     */
    RA_ENCODER_STATUS_FAILURE = -1,
    /**
     * A frame was encoded in the first pass of a 2-pass encode, but its stats
     * data was not retrieved with `rav1e_twopass_out`, or not enough stats data
     * was provided in the second pass of a 2-pass encode to encode the next
     * frame.
     */
    RA_ENCODER_STATUS_NOT_READY = -2,
} RaEncoderStatus;

/**
 * Possible types of a frame.
 */
typedef enum {
    /**
     * Key frame.
     */
    RA_FRAME_TYPE_KEY,
    /**
     * Inter-frame.
     */
    RA_FRAME_TYPE_INTER,
    /**
     * Intra-only frame.
     */
    RA_FRAME_TYPE_INTRA_ONLY,
    /**
     * Switching frame.
     */
    RA_FRAME_TYPE_SWITCH,
} RaFrameType;

typedef struct RaBlockSize RaBlockSize;

/**
 * Encoder configuration
 *
 * Instantiate it using `rav1e_config_default()` and fine-tune it using
 * `rav1e_config_parse()`.
 *
 * Use `rav1e_config_unref()` to free its memory.
 */
typedef struct RaConfig RaConfig;

/**
 * Encoder context
 *
 * Contains the encoding state, it is created by `rav1e_context_new()` using an
 * Encoder configuration.
 *
 * Use `rav1e_context_unref()` to free its memory.
 */
typedef struct RaContext RaContext;

/**
 * Raw video Frame
 *
 * It can be allocated through `rav1e_frame_new()`,
 * populated using `rav1e_frame_fill_plane()`,
 * and freed using `rav1e_frame_unref()`.
 */
typedef struct RaFrame RaFrame;

typedef struct RaRefType RaRefType;

/**
 * Transform Size
 */
typedef struct RaTxSize RaTxSize;

/**
 * Simple Data
 *
 * Use `rav1e_data_unref()` to free its memory.
 */
typedef struct {
    /**
     * Pointer to the data buffer
     */
    const uint8_t *data;
    /**
     * Data buffer size
     */
    size_t len;
} RaData;

/**
 * A rational number.
 */
typedef struct {
    /**
     * Numerator.
     */
    uint64_t num;
    /**
     * Denominator.
     */
    uint64_t den;
} RaRational;

/**
 * Chromaticity coordinates as defined by CIE 1931, expressed as 0.16
 * fixed-point values.
 */
typedef struct {
    /**
     * The X coordinate.
     */
    uint16_t x;
    /**
     * The Y coordinate.
     */
    uint16_t y;
} RaChromaticityPoint;

typedef void (*RaFrameOpaqueCb)(void*);

/**
 * Encoded Packet
 *
 * The encoded packets are retrieved using `rav1e_receive_packet()`.
 *
 * Use `rav1e_packet_unref()` to free its memory.
 */
typedef struct {
    /**
     * Encoded data buffer
     */
    const uint8_t *data;
    /**
     * Encoded data buffer size
     */
    size_t len;
    /**
     * Frame sequence number
     */
    uint64_t input_frameno;
    /**
     * Frame type
     */
    RaFrameType frame_type;
    /**
     * User provided opaque data
     */
    void *opaque;
    /**
     * The reconstruction of the shown frame.
     * This is freed automatically by `rav1e_packet_unref()`.
     */
    RaFrame *rec;
    /**
     * The Reference Frame
     * This is freed automatically by `rav1e_packet_unref()`.
     */
    RaFrame *source;
} RaPacket;

typedef int8_t RaTxfmShift[3];

typedef RaTxfmShift RaTxfmShifts[3];

#ifdef __cplusplus
extern "C" {
#endif // __cplusplus

/**
 * Version information as presented in `[package]` `version`.
 *
 * e.g. `0.1.0`
 *
 * Can be parsed by [semver](https://crates.io/crates/semver).
 * This returns the version of the loaded library, regardless
 * of which version the library user was built against.
 */
const char *rav1e_version_short(void);

/**
 * Version information with the information
 * provided by `git describe --tags`.
 *
 * e.g. `0.1.0 (v0.1.0-1-g743d464)`
 *
 * This returns the version of the loaded library, regardless
 * of which version the library user was built against.
 */
const char *rav1e_version_full(void);

/**
 * Free a `RaData` buffer
 */
void rav1e_data_unref(RaData *data);

/**
 * Create a `RaConfig` filled with default parameters.
 */
RaConfig *rav1e_config_default(void);

/**
 * Setup a second pass rate control using the provided summary
 *
 * Passing `NULL` data resets the rate control settings.
 *
 * If additional data is required, pointer and len stay unchanged, otherwise
 * they are updated.
 *
 * Return:
 * `0` on success
 * `> 0` if the buffer has to be larger
 * `< 0` on failure
 */
int rav1e_config_set_rc_summary(RaConfig *cfg, const uint8_t **data, size_t *len);

/**
 * Request to emit pass data
 *
 * Set emit to 0 to not emit pass data, non-zero to emit pass data.
 *
 */
void rav1e_config_set_emit_data(RaConfig *cfg, int emit);

/**
 * Set the display aspect ratio of the stream
 *
 * Needed for anamorphic video.
 */
void rav1e_config_set_sample_aspect_ratio(RaConfig *cfg, RaRational sample_aspect_ratio);

/**
 * Set the time base of the stream
 *
 * Needed for rate control.
 */
void rav1e_config_set_time_base(RaConfig *cfg, RaRational time_base);

/**
 * Set pixel format of the stream.
 *
 * Supported values for `subsampling` and `chroma_pos` are defined by the
 * enum types `RaChromaSampling` and `RaChromaSamplePosition` respectively.
 * Valid values for `pixel_range` are 0 and 1.
 *
 * Returns a negative value on error or 0.
 */
int rav1e_config_set_pixel_format(RaConfig *cfg,
                                  uint8_t bit_depth,
                                  RaChromaSampling subsampling,
                                  RaChromaSamplePosition chroma_pos,
                                  RaPixelRange pixel_range);

/**
 * Set color properties of the stream.
 *
 * Supported values are defined by the enum types
 * `RaMatrixCoefficients`, `RaColorPrimaries`, and `RaTransferCharacteristics`
 * respectively.
 *
 * Return a negative value on error or 0.
 */
int rav1e_config_set_color_description(RaConfig *cfg,
                                       RaMatrixCoefficients matrix,
                                       RaColorPrimaries primaries,
                                       RaTransferCharacteristics transfer);

/**
 * Set the content light level information for HDR10 streams.
 *
 * Return a negative value on error or 0.
 */
int rav1e_config_set_content_light(RaConfig *cfg,
                                   uint16_t max_content_light_level,
                                   uint16_t max_frame_average_light_level);

/**
 * Set the mastering display information for HDR10 streams.
 *
 * `primaries` and `white_point` arguments are `RaChromaticityPoint`,
 * containing 0.16 fixed point values.
 * `max_luminance` is a 24.8 fixed point value.
 * `min_luminance` is a 18.14 fixed point value.
 *
 * Returns a negative value on error or 0.
 */
int rav1e_config_set_mastering_display(RaConfig *cfg,
                                       const RaChromaticityPoint primaries[3],
                                       RaChromaticityPoint white_point,
                                       uint32_t max_luminance,
                                       uint32_t min_luminance);

/**
 * Free the `RaConfig`.
 */
void rav1e_config_unref(RaConfig *cfg);

/**
 * Set a configuration parameter using its key and value as string.
 *
 * Available keys and values
 * - `"width"`: width of the frame, default `640`
 * - `"height"`: height of the frame, default `480`
 * - `"speed"`: 0-10, default `6`
 * - `"threads"`: maximum number of threads to be used, default auto
 * - `"tune"`: `"psnr"` or `"psychovisual"`, default `"psychovisual"`
 * - `"quantizer"`: 0-255, default `100`
 * - `"tiles"`: total number of tiles desired (0 denotes auto), default `0`
 * - `"tile_rows"`: number of tiles horizontally (must be a power of two, overridden by tiles if present), default `0`
 * - `"tile_cols"`: number of tiles vertically (must be a power of two, overridden by tiles if present), default `0`
 * - `"min_quantizer"`: minimum allowed base quantizer to use in bitrate mode, default `0`
 * - `"bitrate"`: target bitrate for the bitrate mode (required for two pass mode), default `0`
 * - `"key_frame_interval"`: maximum interval between two keyframes, default `240`
 * - `"min_key_frame_interval"`: minimum interval between two keyframes, default `12`
 * - `"switch_frame_interval"`: interval between switch frames, default `0`
 * - `"reservoir_frame_delay"`: number of temporal units over which to distribute the reservoir usage, default `None`
 * - `"rdo_lookahead_frames"`: number of frames to read ahead for the RDO lookahead computation, default `40`
 * - `"low_latency"`: flag to enable low latency mode, default `false`
 * - `"enable_timing_info"`: flag to enable signaling timing info in the bitstream, default `false`
 * - `"still_picture"`: flag for still picture mode, default `false`
 *
 * Return a negative value on error or 0.
 */
int rav1e_config_parse(RaConfig *cfg,
                       const char *key,
                       const char *value);

/**
 * Set a configuration parameter using its key and value as integer.
 *
 * Available keys and values are the same as `rav1e_config_parse()`
 *
 * Return a negative value on error or 0.
 */
int rav1e_config_parse_int(RaConfig *cfg, const char *key, int value);

/**
 * Generate a new encoding context from a populated encoder configuration
 *
 * Multiple contexts can be generated through it.
 * Returns `Null` if context creation failed, e.g. by passing
 * an invalid `Config`.
 */
RaContext *rav1e_context_new(const RaConfig *cfg);

/**
 * Free the `RaContext`.
 */
void rav1e_context_unref(RaContext *ctx);

/**
 * Produce a new frame from the encoding context
 *
 * It must be populated using `rav1e_frame_fill_plane()`.
 *
 * The frame is reference counted and must be released passing it to `rav1e_frame_unref()`,
 * see `rav1e_send_frame()`.
 */
RaFrame *rav1e_frame_new(const RaContext *ctx);

/**
 * Free the `RaFrame`.
 */
void rav1e_frame_unref(RaFrame *frame);

/**
 * Overrides the encoders frame type decision for a frame
 *
 * Must be called before `rav1e_send_frame()` if used.
 */
int rav1e_frame_set_type(RaFrame *frame, RaFrameTypeOverride frame_type);

/**
 * Register an opaque data and a destructor to the frame
 *
 * It takes the ownership of its memory:
 * - it will relinquish the ownership to the context if
 *   `rav1e_send_frame` is called.
 * - it will call the destructor if `rav1e_frame_unref` is called
 *   otherwise.
 */
void rav1e_frame_set_opaque(RaFrame *frame, void *opaque, RaFrameOpaqueCb cb);

/**
 * Add generic T35 metadata to a frame
 *
 * The buffer will be copied into the frame and can be freed
 * immediately after this call.
 *
 * Can be called multiple times to add multiple T35 metadata
 * blocks.
 */
void rav1e_frame_add_t35_metadata(RaFrame *frame,
                                  uint8_t country_code,
                                  uint8_t country_code_extension_byte,
                                  const uint8_t *data,
                                  size_t data_len);

/**
 * Retrieve the first-pass data of a two-pass encode for the frame that was
 * just encoded. This should be called BEFORE every call to `rav1e_receive_packet()`
 * (including the very first one), even if no packet was produced by the
 * last call to `rav1e_receive_packet`, if any (i.e., `RA_ENCODER_STATUS_ENCODED`
 * was returned). It needs to be called once more after
 * `RA_ENCODER_STATUS_LIMIT_REACHED` is returned, to retrieve the header that
 * should be written to the front of the stats file (overwriting the
 * placeholder header that was emitted at the start of encoding).
 *
 * It is still safe to call this function when `rav1e_receive_packet()` returns any
 * other error. It will return `NULL` instead of returning a duplicate copy
 * of the previous frame's data.
 *
 * Must be freed with `rav1e_data_unref()`.
 */
RaData *rav1e_twopass_out(RaContext *ctx);

/**
 * Return the Rate Control Summary Packet size
 *
 * It is useful mainly to preserve space when saving
 * both Rate Control Summary and Frame Packets in a single file
 */
size_t rav1e_rc_summary_size(const RaContext *ctx);

/**
 * Return the first pass data
 *
 * Call it after `rav1e_receive_packet()` returns a normal condition status:
 * - `EncoderStatus::Encoded`,
 * - `EncoderStatus::Success`,
 * - `EncoderStatus::LimitReached`.
 *
 * use `rav1e_data_unref()` to free the data.
 *
 * It will return a `RcDataKind::Summary` once the encoder is flushed.
 */
RaRcDataKind rav1e_rc_receive_pass_data(RaContext *ctx, RaData **data);

/**
 * Number of pass data packets required to progress the encoding process.
 *
 * At least that number of packets must be passed before the encoder can
 * progress.
 *
 * Stop feeding-in pass data packets once the function returns 0.
 *
 * ``` c
 * while (rav1e_rc_second_pass_data_required(ctx) > 0) {
 *   int more = rav1e_rc_send_pass_data(ctx, &data, &len);
 *   if (more > 0) {
 *      refill(&data, &len);
 *   } else if (more < 0) {
 *     goto fail;
 *   }
 * }
 * ```
 *
 */
int32_t rav1e_rc_second_pass_data_required(const RaContext *ctx);

/**
 * Feed the first pass Rate Control data to the encoder,
 * Frame-specific Packets only.
 *
 * Call it before `receive_packet()`
 *
 * If additional data is required, pointer and len stay unchanged, otherwise
 * they are updated.
 *
 * Returns:
 * - `0` on success,
 * - `> 0` the amount of bytes needed
 * - `< 0` on unrecoverable failure
 */
int rav1e_rc_send_pass_data(RaContext *ctx, const uint8_t **data, size_t *len);

/**
 * Ask how many bytes of the stats file are needed before the next frame
 * of the second pass in a two-pass encode can be encoded. This is a lower
 * bound (more might be required), but if 0 is returned, then encoding can
 * proceed. This is just a hint to the application, and does not need to
 * be called for encoding the second pass to work, so long as the
 * application continues to provide more data to `rav1e_twopass_in()` in a loop
 * until `rav1e_twopass_in()` returns 0.
 */
size_t rav1e_twopass_bytes_needed(RaContext *ctx);

/**
 * Provide stats data produced in the first pass of a two-pass encode to the
 * second pass. On success this returns the number of bytes of that data
 * which were consumed. When encoding the second pass of a two-pass encode,
 * this should be called repeatedly in a loop before every call to
 * `rav1e_receive_packet()` (including the very first one) until no bytes are
 * consumed, or until `twopass_bytes_needed()` returns 0. Returns -1 on failure.
 */
int rav1e_twopass_in(RaContext *ctx, uint8_t *buf, size_t buf_size);

/**
 * Send the frame for encoding
 *
 * The function increases the frame internal reference count and it can be passed multiple
 * times to different `rav1e_send_frame()` with a caveat:
 *
 * The opaque data, if present, will be moved from the `Frame` to the `Context`
 * and returned by `rav1e_receive_packet` in the `Packet` `opaque` field or
 * the destructor will be called on `rav1e_context_unref` if the frame is
 * still pending in the encoder.
 *
 * Returns:
 * - `0` on success,
 * - `> 0` if the input queue is full
 * - `< 0` on unrecoverable failure
 */
RaEncoderStatus rav1e_send_frame(RaContext *ctx, RaFrame *frame);

/**
 * Return the last encoder status
 */
RaEncoderStatus rav1e_last_status(const RaContext *ctx);

/**
 * Return a static string matching the `EncoderStatus` variant.
 *
 */
const char *rav1e_status_to_str(RaEncoderStatus status);

/**
 * Receive encoded data
 *
 * Returns:
 * - `0` on success
 * - `> 0` if additional frame data is required
 * - `< 0` on unrecoverable failure
 */
RaEncoderStatus rav1e_receive_packet(RaContext *ctx, RaPacket **pkt);

/**
 * Free the `RaPacket`.
 */
void rav1e_packet_unref(RaPacket *pkt);

/**
 * Produce a sequence header matching the current encoding context
 *
 * Its format is compatible with the AV1 Matroska and ISOBMFF specification.
 *
 * Use `rav1e_data_unref()` to free it.
 */
RaData *rav1e_container_sequence_header(const RaContext *ctx);

/**
 * Fill a frame plane
 *
 * Currently the frame contains 3 planes, the first is luminance followed by
 * chrominance.
 *
 * The data is copied and this function has to be called for each plane.
 *
 * `frame`: A frame provided by `rav1e_frame_new()`
 * `plane`: The index of the plane starting from 0
 * `data`: The data to be copied
 * `data_len`: Length of the buffer
 * `stride`: Plane line in bytes, including padding
 * `bytewidth`: Number of bytes per component, either 1 or 2
 */
void rav1e_frame_fill_plane(RaFrame *frame,
                            int plane,
                            const uint8_t *data,
                            size_t data_len,
                            ptrdiff_t stride,
                            int bytewidth);

/**
 * Extract a frame plane
 *
 * This is the reverse of `rav1e_frame_fill_plane()`, primarily used for
 * extracting the source and reconstruction data from a `RaPacket`.
 *
 * Currently the frame contains 3 planes, the first is luminance followed by
 * chrominance.
 *
 * The data is copied out of the frame for a single plane.
 *
 * `frame`: A frame provided inside a packet returned by `rav1e_receive_packet()`
 * `plane`: The index of the plane starting from 0
 * `data`: The destination for the data
 * `data_len`: Length of the buffer
 * `stride`: Plane line in bytes, including padding
 * `bytewidth`: Number of bytes per component, either 1 or 2
 */
void rav1e_frame_extract_plane(const RaFrame *frame,
                               int plane,
                               uint8_t *data,
                               size_t data_len,
                               ptrdiff_t stride,
                               int bytewidth);

extern void rav1e_cdef_filter_4x4_avx2(uint8_t *dst,
                                       intptr_t dst_stride,
                                       const uint16_t *tmp,
                                       intptr_t tmp_stride,
                                       int32_t pri_strength,
                                       int32_t sec_strength,
                                       int32_t dir,
                                       int32_t damping);

extern void rav1e_cdef_filter_4x8_avx2(uint8_t *dst,
                                       intptr_t dst_stride,
                                       const uint16_t *tmp,
                                       intptr_t tmp_stride,
                                       int32_t pri_strength,
                                       int32_t sec_strength,
                                       int32_t dir,
                                       int32_t damping);

extern void rav1e_cdef_filter_8x8_avx2(uint8_t *dst,
                                       intptr_t dst_stride,
                                       const uint16_t *tmp,
                                       intptr_t tmp_stride,
                                       int32_t pri_strength,
                                       int32_t sec_strength,
                                       int32_t dir,
                                       int32_t damping);

extern int32_t rav1e_cdef_dir_8bpc_ssse3(const uint8_t *tmp, intptr_t tmp_stride, uint32_t *var);

extern int32_t rav1e_cdef_dir_8bpc_avx2(const uint8_t *tmp, intptr_t tmp_stride, uint32_t *var);

extern int32_t rav1e_cdef_dir_16bpc_ssse3(const uint16_t *tmp,
                                          intptr_t tmp_stride,
                                          uint32_t *var,
                                          int32_t bitdepth_max);

extern int32_t rav1e_cdef_dir_16bpc_sse4(const uint16_t *tmp,
                                         intptr_t tmp_stride,
                                         uint32_t *var,
                                         int32_t bitdepth_max);

extern int32_t rav1e_cdef_dir_16bpc_avx2(const uint16_t *tmp,
                                         intptr_t tmp_stride,
                                         uint32_t *var,
                                         int32_t bitdepth_max);

extern void rav1e_cdef_dist_kernel_4x4_sse2(const uint8_t *src,
                                            intptr_t src_stride,
                                            const uint8_t *dst,
                                            intptr_t dst_stride,
                                            uint32_t *ret_ptr);

extern void rav1e_cdef_dist_kernel_4x8_sse2(const uint8_t *src,
                                            intptr_t src_stride,
                                            const uint8_t *dst,
                                            intptr_t dst_stride,
                                            uint32_t *ret_ptr);

extern void rav1e_cdef_dist_kernel_8x4_sse2(const uint8_t *src,
                                            intptr_t src_stride,
                                            const uint8_t *dst,
                                            intptr_t dst_stride,
                                            uint32_t *ret_ptr);

extern void rav1e_cdef_dist_kernel_8x8_sse2(const uint8_t *src,
                                            intptr_t src_stride,
                                            const uint8_t *dst,
                                            intptr_t dst_stride,
                                            uint32_t *ret_ptr);

extern void rav1e_avg_8bpc_ssse3(uint8_t *dst,
                                 ptrdiff_t dst_stride,
                                 const int16_t *tmp1,
                                 const int16_t *tmp2,
                                 int32_t w,
                                 int32_t h);

extern void rav1e_avg_8bpc_avx2(uint8_t *dst,
                                ptrdiff_t dst_stride,
                                const int16_t *tmp1,
                                const int16_t *tmp2,
                                int32_t w,
                                int32_t h);

extern void rav1e_avg_8bpc_avx512icl(uint8_t *dst,
                                     ptrdiff_t dst_stride,
                                     const int16_t *tmp1,
                                     const int16_t *tmp2,
                                     int32_t w,
                                     int32_t h);

extern void rav1e_avg_16bpc_ssse3(uint16_t *dst,
                                  ptrdiff_t dst_stride,
                                  const int16_t *tmp1,
                                  const int16_t *tmp2,
                                  int32_t w,
                                  int32_t h,
                                  int32_t bitdepth_max);

extern void rav1e_avg_16bpc_avx2(uint16_t *dst,
                                 ptrdiff_t dst_stride,
                                 const int16_t *tmp1,
                                 const int16_t *tmp2,
                                 int32_t w,
                                 int32_t h,
                                 int32_t bitdepth_max);

extern void rav1e_ipred_z2_8bpc_ssse3(uint8_t *dst,
                                      ptrdiff_t stride,
                                      const uint8_t *topleft,
                                      int width,
                                      int height,
                                      int angle,
                                      int dx,
                                      int dy);

extern void rav1e_ipred_z2_8bpc_avx2(uint8_t *dst,
                                     ptrdiff_t stride,
                                     const uint8_t *topleft,
                                     int width,
                                     int height,
                                     int angle,
                                     int dx,
                                     int dy);

extern void rav1e_ipred_z2_16bpc_avx2(uint16_t *dst,
                                      ptrdiff_t stride,
                                      const uint16_t *topleft,
                                      int width,
                                      int height,
                                      int angle,
                                      int dx,
                                      int dy,
                                      int bit_depth_max);

extern void rav1e_cdef_filter4_8bpc_neon(uint8_t *dst,
                                         intptr_t dst_stride,
                                         const uint16_t *tmp,
                                         int32_t pri_strength,
                                         int32_t sec_strength,
                                         int32_t dir,
                                         int32_t damping,
                                         int32_t h,
                                         intptr_t edges);

extern void rav1e_cdef_padding4_8bpc_neon(uint16_t *tmp,
                                          const uint8_t *src,
                                          intptr_t src_stride,
                                          const uint8_t (*left)[2],
                                          const uint8_t *top,
                                          const uint8_t *bottom,
                                          int32_t h,
                                          intptr_t edges);

extern void rav1e_cdef_filter8_8bpc_neon(uint8_t *dst,
                                         intptr_t dst_stride,
                                         const uint16_t *tmp,
                                         int32_t pri_strength,
                                         int32_t sec_strength,
                                         int32_t dir,
                                         int32_t damping,
                                         int32_t h,
                                         intptr_t edges);

extern void rav1e_cdef_padding8_8bpc_neon(uint16_t *tmp,
                                          const uint8_t *src,
                                          intptr_t src_stride,
                                          const uint8_t (*left)[2],
                                          const uint8_t *top,
                                          const uint8_t *bottom,
                                          int32_t h,
                                          intptr_t edges);

extern void rav1e_cdef_filter4_16bpc_neon(uint16_t *dst,
                                          intptr_t dst_stride,
                                          const uint16_t *tmp,
                                          int32_t pri_strength,
                                          int32_t sec_strength,
                                          int32_t dir,
                                          int32_t damping,
                                          int32_t h,
                                          intptr_t edges,
                                          int32_t bd);

extern void rav1e_cdef_padding4_16bpc_neon(uint16_t *tmp,
                                           const uint16_t *src,
                                           intptr_t src_stride,
                                           const uint16_t (*left)[2],
                                           const uint16_t *top,
                                           const uint16_t *bottom,
                                           int32_t h,
                                           intptr_t edges);

extern void rav1e_cdef_filter8_16bpc_neon(uint16_t *dst,
                                          intptr_t dst_stride,
                                          const uint16_t *tmp,
                                          int32_t pri_strength,
                                          int32_t sec_strength,
                                          int32_t dir,
                                          int32_t damping,
                                          int32_t h,
                                          intptr_t edges,
                                          int32_t bd);

extern void rav1e_cdef_padding8_16bpc_neon(uint16_t *tmp,
                                           const uint16_t *src,
                                           intptr_t src_stride,
                                           const uint16_t (*left)[2],
                                           const uint16_t *top,
                                           const uint16_t *bottom,
                                           int32_t h,
                                           intptr_t edges);

extern int32_t rav1e_cdef_find_dir_8bpc_neon(const uint8_t *tmp,
                                             intptr_t tmp_stride,
                                             uint32_t *var);

extern int32_t rav1e_cdef_find_dir_16bpc_neon(const uint16_t *tmp,
                                              intptr_t tmp_stride,
                                              uint32_t *var,
                                              int32_t max_bitdepth);

extern void rav1e_cdef_dist_kernel_4x4_neon(const uint8_t *src,
                                            intptr_t src_stride,
                                            const uint8_t *dst,
                                            intptr_t dst_stride,
                                            uint32_t *ret_ptr);

extern void rav1e_cdef_dist_kernel_4x8_neon(const uint8_t *src,
                                            intptr_t src_stride,
                                            const uint8_t *dst,
                                            intptr_t dst_stride,
                                            uint32_t *ret_ptr);

extern void rav1e_cdef_dist_kernel_8x4_neon(const uint8_t *src,
                                            intptr_t src_stride,
                                            const uint8_t *dst,
                                            intptr_t dst_stride,
                                            uint32_t *ret_ptr);

extern void rav1e_cdef_dist_kernel_8x8_neon(const uint8_t *src,
                                            intptr_t src_stride,
                                            const uint8_t *dst,
                                            intptr_t dst_stride,
                                            uint32_t *ret_ptr);

extern void rav1e_cdef_dist_kernel_4x4_hbd_neon(const uint16_t *src,
                                                intptr_t src_stride,
                                                const uint16_t *dst,
                                                intptr_t dst_stride,
                                                uint32_t *ret_ptr);

extern void rav1e_cdef_dist_kernel_4x8_hbd_neon(const uint16_t *src,
                                                intptr_t src_stride,
                                                const uint16_t *dst,
                                                intptr_t dst_stride,
                                                uint32_t *ret_ptr);

extern void rav1e_cdef_dist_kernel_8x4_hbd_neon(const uint16_t *src,
                                                intptr_t src_stride,
                                                const uint16_t *dst,
                                                intptr_t dst_stride,
                                                uint32_t *ret_ptr);

extern void rav1e_cdef_dist_kernel_8x8_hbd_neon(const uint16_t *src,
                                                intptr_t src_stride,
                                                const uint16_t *dst,
                                                intptr_t dst_stride,
                                                uint32_t *ret_ptr);

extern void rav1e_avg_8bpc_neon(uint8_t *dst,
                                ptrdiff_t dst_stride,
                                const int16_t *tmp1,
                                const int16_t *tmp2,
                                int32_t w,
                                int32_t h);

extern void rav1e_avg_16bpc_neon(uint16_t *dst,
                                 ptrdiff_t dst_stride,
                                 const int16_t *tmp1,
                                 const int16_t *tmp2,
                                 int32_t w,
                                 int32_t h,
                                 int32_t bitdepth_max);

extern void rav1e_ipred_z1_upsample_edge_8bpc_neon(uint8_t *out,
                                                   int hsz,
                                                   const uint8_t *_in,
                                                   int end);

extern void rav1e_ipred_z1_upsample_edge_16bpc_neon(uint16_t *out,
                                                    int hsz,
                                                    const uint16_t *_in,
                                                    int end,
                                                    int bit_depth_max);

extern void rav1e_ipred_z1_filter_edge_8bpc_neon(uint8_t *out,
                                                 int sz,
                                                 const uint8_t *_in,
                                                 int end,
                                                 int strength);

extern void rav1e_ipred_z1_filter_edge_16bpc_neon(uint16_t *out,
                                                  int sz,
                                                  const uint16_t *_in,
                                                  int end,
                                                  int strength);

extern void rav1e_ipred_z1_fill1_8bpc_neon(uint8_t *dst,
                                           ptrdiff_t stride,
                                           const uint8_t *top,
                                           int width,
                                           int height,
                                           int dx,
                                           int max_base_x);

extern void rav1e_ipred_z1_fill1_16bpc_neon(uint16_t *dst,
                                            ptrdiff_t stride,
                                            const uint16_t *top,
                                            int width,
                                            int height,
                                            int dx,
                                            int max_base_x);

extern void rav1e_ipred_z1_fill2_8bpc_neon(uint8_t *dst,
                                           ptrdiff_t stride,
                                           const uint8_t *top,
                                           int width,
                                           int height,
                                           int dx,
                                           int max_base_x);

extern void rav1e_ipred_z1_fill2_16bpc_neon(uint16_t *dst,
                                            ptrdiff_t stride,
                                            const uint16_t *top,
                                            int width,
                                            int height,
                                            int dx,
                                            int max_base_x);

extern void rav1e_ipred_z2_upsample_edge_8bpc_neon(uint8_t *out, int sz, const uint8_t *_in);

extern void rav1e_ipred_z2_upsample_edge_16bpc_neon(uint16_t *out,
                                                    int sz,
                                                    const uint16_t *_in,
                                                    int bit_depth_max);

extern void rav1e_ipred_z2_fill1_8bpc_neon(uint8_t *dst,
                                           ptrdiff_t stride,
                                           const uint8_t *top,
                                           const uint8_t *left,
                                           int width,
                                           int height,
                                           int dx,
                                           int dy);

extern void rav1e_ipred_z2_fill1_16bpc_neon(uint16_t *dst,
                                            ptrdiff_t stride,
                                            const uint16_t *top,
                                            const uint16_t *left,
                                            int width,
                                            int height,
                                            int dx,
                                            int dy);

extern void rav1e_ipred_z2_fill2_8bpc_neon(uint8_t *dst,
                                           ptrdiff_t stride,
                                           const uint8_t *top,
                                           const uint8_t *left,
                                           int width,
                                           int height,
                                           int dx,
                                           int dy);

extern void rav1e_ipred_z2_fill2_16bpc_neon(uint16_t *dst,
                                            ptrdiff_t stride,
                                            const uint16_t *top,
                                            const uint16_t *left,
                                            int width,
                                            int height,
                                            int dx,
                                            int dy);

extern void rav1e_ipred_z2_fill3_8bpc_neon(uint8_t *dst,
                                           ptrdiff_t stride,
                                           const uint8_t *top,
                                           const uint8_t *left,
                                           int width,
                                           int height,
                                           int dx,
                                           int dy);

extern void rav1e_ipred_z2_fill3_16bpc_neon(uint16_t *dst,
                                            ptrdiff_t stride,
                                            const uint16_t *top,
                                            const uint16_t *left,
                                            int width,
                                            int height,
                                            int dx,
                                            int dy);

extern void rav1e_ipred_z3_fill1_8bpc_neon(uint8_t *dst,
                                           ptrdiff_t stride,
                                           const uint8_t *left,
                                           int width,
                                           int height,
                                           int dy,
                                           int max_base_y);

extern void rav1e_ipred_z3_fill1_16bpc_neon(uint16_t *dst,
                                            ptrdiff_t stride,
                                            const uint16_t *left,
                                            int width,
                                            int height,
                                            int dy,
                                            int max_base_y);

extern void rav1e_ipred_z3_fill2_8bpc_neon(uint8_t *dst,
                                           ptrdiff_t stride,
                                           const uint8_t *left,
                                           int width,
                                           int height,
                                           int dy,
                                           int max_base_y);

extern void rav1e_ipred_z3_fill2_16bpc_neon(uint16_t *dst,
                                            ptrdiff_t stride,
                                            const uint16_t *left,
                                            int width,
                                            int height,
                                            int dy,
                                            int max_base_y);

extern void rav1e_ipred_reverse_8bpc_neon(uint8_t *dst, const uint8_t *src, int n);

extern void rav1e_ipred_reverse_16bpc_neon(uint16_t *dst, const uint16_t *src, int n);

#ifdef __cplusplus
}  // extern "C"
#endif  // __cplusplus

#endif  /* RAV1E_H */
