/*
 * cobalt_opus_shim.h
 *
 * Portable extern-C facade over libopus (xiph/opus v1.5.2, the interactive
 * speech and audio codec) for the Cobalt calls audio stack. It re-exposes only
 * the libopus surface the call path uses, through PORTABLE SCALAR TYPES ONLY
 * (the fixed-width <stdint.h> integers and opaque void*), so the
 * jextract-generated Java binding is identical on every host ABI.
 *
 * Why this shim exists: jextract bakes the host C ABI into the bindings it
 * emits. libopus's public API exchanges several ABI-sensitive shapes. The
 * encode/decode entry points take and return opus_int32, which is a fixed-width
 * typedef and so portable on its own, but the codec, decoder and repacketizer
 * states (OpusEncoder, OpusDecoder, OpusRepacketizer) are opaque structs whose
 * size differs per build, and far worse, the configuration interface is the
 * VARIADIC opus_encoder_ctl(st, request, ...) / opus_decoder_ctl(st, request,
 * ...). A variadic native call cannot be bound to one stable FFM descriptor;
 * jextract emits a manual variadic-invoker shape the caller must materialize per
 * argument list, and the bound integer request codes (the *_REQUEST symbols) are
 * the only handle on the otherwise macro-only OPUS_SET_* / OPUS_GET_* surface.
 * opus_packet_parse additionally returns an array of pointers INTO the input
 * packet (const unsigned char *frames[48]), a shape no portable binding can read
 * back. By hiding every opus object behind an opaque void* handle and converting
 * each variadic control the call path uses into a TYPED cobalt_opus_* setter or
 * getter taking a plain int32_t, and by returning opus_packet_parse's framing as
 * fixed-width offset and size arrays, the generated binding contains no
 * ABI-sensitive or variadic shape and is portable as-is.
 *
 * How the opus objects are hidden: the encoder, decoder and repacketizer are
 * each held as an opaque void* handle wrapping the libopus state (allocated by
 * the matching opus_*_create); the create wrappers return a status code and the
 * handle through an out-parameter, mirroring the cobalt_vpx_* / cobalt_dav1d_*
 * create pattern. Every OPUS_SET_* / OPUS_GET_* request is applied C-side from the
 * typed scalar argument and never reaches Java. The encode/decode buffers are
 * plain int16_t / uint8_t arrays the caller owns.
 *
 * Symbol naming: every exported symbol is prefixed cobalt_opus_ so it coexists
 * in the combined cobalt-native library with the statically-linked real opus_*
 * symbols, which these wrappers call internally.
 *
 * Portability rule for this header: it uses ONLY int16_t/int32_t/uint8_t/void*.
 * It never names a libopus type, and never uses bare `long`, `unsigned long` or
 * `long double`.
 */

#ifndef COBALT_OPUS_SHIM_H
#define COBALT_OPUS_SHIM_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * libopus return/error codes, forwarded verbatim. These are STABLE small ints
 * defined by RFC 6716's reference implementation and are identical on every
 * host, so the shim re-exposes opus's own values rather than remapping them. A
 * value of 0 is success; the negative values are opus errors recoverable as text
 * through cobalt_opus_strerror.
 *
 *   COBALT_OPUS_OK                 0   no error
 *   COBALT_OPUS_BAD_ARG          (-1)  one or more invalid/out-of-range arguments
 *   COBALT_OPUS_BUFFER_TOO_SMALL (-2)  not enough bytes allocated in the buffer
 *   COBALT_OPUS_INTERNAL_ERROR   (-3)  an internal error was detected
 *   COBALT_OPUS_INVALID_PACKET   (-4)  the compressed data passed is corrupted
 *   COBALT_OPUS_UNIMPLEMENTED    (-5)  invalid/unsupported request number
 *   COBALT_OPUS_INVALID_STATE    (-6)  an encoder or decoder state is invalid
 *   COBALT_OPUS_ALLOC_FAIL       (-7)  memory allocation has failed
 */
#define COBALT_OPUS_OK                 0
#define COBALT_OPUS_BAD_ARG          (-1)
#define COBALT_OPUS_BUFFER_TOO_SMALL (-2)
#define COBALT_OPUS_INTERNAL_ERROR   (-3)
#define COBALT_OPUS_INVALID_PACKET   (-4)
#define COBALT_OPUS_UNIMPLEMENTED    (-5)
#define COBALT_OPUS_INVALID_STATE    (-6)
#define COBALT_OPUS_ALLOC_FAIL       (-7)

/*
 * Encoder application (coding mode) selectors for cobalt_opus_encoder_create.
 * The values match libopus's OPUS_APPLICATION_* constants so the meaning is
 * unambiguous:
 *
 *   COBALT_OPUS_APPLICATION_VOIP                2048  voice intelligibility
 *   COBALT_OPUS_APPLICATION_AUDIO              2049  faithfulness to the input
 *   COBALT_OPUS_APPLICATION_RESTRICTED_LOWDELAY 2051  lowest achievable delay
 */
#define COBALT_OPUS_APPLICATION_VOIP                 2048
#define COBALT_OPUS_APPLICATION_AUDIO                2049
#define COBALT_OPUS_APPLICATION_RESTRICTED_LOWDELAY  2051

/*
 * Signal-type hint selectors for cobalt_opus_encoder_set_signal. The values
 * match libopus's OPUS_SIGNAL_* constants:
 *
 *   COBALT_OPUS_SIGNAL_VOICE  3001  bias toward LPC/Hybrid (speech)
 *   COBALT_OPUS_SIGNAL_MUSIC  3002  bias toward MDCT (music)
 */
#define COBALT_OPUS_SIGNAL_VOICE  3001
#define COBALT_OPUS_SIGNAL_MUSIC  3002

/*
 * Bandwidth selectors for cobalt_opus_encoder_set_bandwidth and
 * cobalt_opus_encoder_set_max_bandwidth, and the value returned by
 * cobalt_opus_packet_get_bandwidth. The values match libopus's
 * OPUS_BANDWIDTH_* constants:
 *
 *   COBALT_OPUS_BANDWIDTH_NARROWBAND     1101   4 kHz passband
 *   COBALT_OPUS_BANDWIDTH_MEDIUMBAND     1102   6 kHz passband
 *   COBALT_OPUS_BANDWIDTH_WIDEBAND       1103   8 kHz passband
 *   COBALT_OPUS_BANDWIDTH_SUPERWIDEBAND  1104  12 kHz passband
 *   COBALT_OPUS_BANDWIDTH_FULLBAND       1105  20 kHz passband
 */
#define COBALT_OPUS_BANDWIDTH_NARROWBAND     1101
#define COBALT_OPUS_BANDWIDTH_MEDIUMBAND     1102
#define COBALT_OPUS_BANDWIDTH_WIDEBAND       1103
#define COBALT_OPUS_BANDWIDTH_SUPERWIDEBAND  1104
#define COBALT_OPUS_BANDWIDTH_FULLBAND       1105

/*
 * Expert frame-duration selectors for cobalt_opus_encoder_set_expert_frame_duration.
 * The values match libopus's OPUS_FRAMESIZE_* constants:
 *
 *   COBALT_OPUS_FRAMESIZE_ARG     5000  select frame size from the argument
 *   COBALT_OPUS_FRAMESIZE_2_5_MS  5001
 *   COBALT_OPUS_FRAMESIZE_5_MS    5002
 *   COBALT_OPUS_FRAMESIZE_10_MS   5003
 *   COBALT_OPUS_FRAMESIZE_20_MS   5004
 *   COBALT_OPUS_FRAMESIZE_40_MS   5005
 *   COBALT_OPUS_FRAMESIZE_60_MS   5006
 *   COBALT_OPUS_FRAMESIZE_80_MS   5007
 *   COBALT_OPUS_FRAMESIZE_100_MS  5008
 *   COBALT_OPUS_FRAMESIZE_120_MS  5009
 */
#define COBALT_OPUS_FRAMESIZE_ARG     5000
#define COBALT_OPUS_FRAMESIZE_2_5_MS  5001
#define COBALT_OPUS_FRAMESIZE_5_MS    5002
#define COBALT_OPUS_FRAMESIZE_10_MS   5003
#define COBALT_OPUS_FRAMESIZE_20_MS   5004
#define COBALT_OPUS_FRAMESIZE_40_MS   5005
#define COBALT_OPUS_FRAMESIZE_60_MS   5006
#define COBALT_OPUS_FRAMESIZE_80_MS   5007
#define COBALT_OPUS_FRAMESIZE_100_MS  5008
#define COBALT_OPUS_FRAMESIZE_120_MS  5009

/*
 * Pre-defined CTL value selectors shared across several controls. The values
 * match libopus's OPUS_AUTO / OPUS_BITRATE_MAX:
 *
 *   COBALT_OPUS_AUTO        (-1000)  auto/default setting (bitrate, bandwidth,
 *                                    force-channels, signal)
 *   COBALT_OPUS_BITRATE_MAX    (-1)  use as much rate as the output buffer allows
 */
#define COBALT_OPUS_AUTO        (-1000)
#define COBALT_OPUS_BITRATE_MAX    (-1)

/**
 * Creates and initializes a libopus encoder for one stream.
 *
 * Forwards to opus_encoder_create with the supplied sampling rate, channel count
 * and application mode. The created encoder is returned through outEnc as an
 * opaque handle. All controllable settings start at libopus's recommended
 * defaults and are adjusted with the cobalt_opus_encoder_set_* functions.
 *
 * @param fs          the input sampling rate in hertz; one of 8000, 12000,
 *                    16000, 24000 or 48000.
 * @param channels    the number of channels in the input signal, 1 or 2.
 * @param application one of COBALT_OPUS_APPLICATION_VOIP /
 *                    COBALT_OPUS_APPLICATION_AUDIO /
 *                    COBALT_OPUS_APPLICATION_RESTRICTED_LOWDELAY.
 * @param outEnc      receives the created encoder handle on success, or is left
 *                    holding NULL on failure; must not be NULL.
 * @return COBALT_OPUS_OK on success, COBALT_OPUS_BAD_ARG for a NULL outEnc,
 *         otherwise the libopus error code from opus_encoder_create.
 */
int32_t cobalt_opus_encoder_create(int32_t fs, int32_t channels, int32_t application, void **outEnc);

/**
 * Destroys an encoder created by cobalt_opus_encoder_create.
 *
 * Forwards to opus_encoder_destroy. Must be called at most once per handle. A
 * NULL handle is ignored.
 *
 * @param enc the encoder handle, or NULL.
 */
void cobalt_opus_encoder_destroy(void *enc);

/**
 * Encodes one Opus frame from interleaved signed 16-bit PCM.
 *
 * Forwards to opus_encode. The input holds frameSize samples per channel
 * (frameSize*channels int16 values) and must be one of the Opus frame sizes for
 * the encoder's sampling rate. The compressed packet is written into data, up to
 * maxBytes; the return value is the number of bytes written, which may be 0, 1 or
 * 2 for a discontinuous-transmission or comfort-noise frame that need not be
 * transmitted, or negative on error.
 *
 * @param enc       the encoder handle from cobalt_opus_encoder_create.
 * @param pcm       the interleaved signed 16-bit input signal.
 * @param frameSize the number of samples per channel in pcm.
 * @param data      the output buffer the compressed packet is written into.
 * @param maxBytes  the capacity of data in bytes.
 * @return the encoded packet length in bytes (zero or more) on success, or a
 *         negative libopus error code on failure.
 */
int32_t cobalt_opus_encode(void *enc, const int16_t *pcm, int32_t frameSize, uint8_t *data, int32_t maxBytes);

/**
 * Sets the encoder's target bitrate.
 *
 * Applies OPUS_SET_BITRATE through opus_encoder_ctl. Rates from 500 to 512000
 * bits per second are meaningful, as are COBALT_OPUS_AUTO and
 * COBALT_OPUS_BITRATE_MAX.
 *
 * @param enc        the encoder handle from cobalt_opus_encoder_create.
 * @param bitrateBps the target bitrate in bits per second, or one of
 *                   COBALT_OPUS_AUTO / COBALT_OPUS_BITRATE_MAX.
 * @return COBALT_OPUS_OK on success, COBALT_OPUS_BAD_ARG for a NULL encoder,
 *         otherwise the libopus error code from opus_encoder_ctl.
 */
int32_t cobalt_opus_encoder_set_bitrate(void *enc, int32_t bitrateBps);

/**
 * Enables or disables variable bitrate.
 *
 * Applies OPUS_SET_VBR through opus_encoder_ctl. The exact VBR type is further
 * controlled by cobalt_opus_encoder_set_vbr_constraint.
 *
 * @param enc the encoder handle from cobalt_opus_encoder_create.
 * @param vbr 1 to enable variable bitrate, 0 for hard constant bitrate.
 * @return COBALT_OPUS_OK on success, COBALT_OPUS_BAD_ARG for a NULL encoder,
 *         otherwise the libopus error code from opus_encoder_ctl.
 */
int32_t cobalt_opus_encoder_set_vbr(void *enc, int32_t vbr);

/**
 * Enables or disables constrained variable bitrate.
 *
 * Applies OPUS_SET_VBR_CONSTRAINT through opus_encoder_ctl. Ignored while the
 * encoder is in constant-bitrate mode.
 *
 * @param enc        the encoder handle from cobalt_opus_encoder_create.
 * @param constraint 1 for constrained VBR, 0 for unconstrained VBR.
 * @return COBALT_OPUS_OK on success, COBALT_OPUS_BAD_ARG for a NULL encoder,
 *         otherwise the libopus error code from opus_encoder_ctl.
 */
int32_t cobalt_opus_encoder_set_vbr_constraint(void *enc, int32_t constraint);

/**
 * Sets the encoder's computational complexity.
 *
 * Applies OPUS_SET_COMPLEXITY through opus_encoder_ctl. The supported range is 0
 * to 10 inclusive, with 10 the highest complexity and quality.
 *
 * @param enc        the encoder handle from cobalt_opus_encoder_create.
 * @param complexity the complexity level in the range 0 to 10.
 * @return COBALT_OPUS_OK on success, COBALT_OPUS_BAD_ARG for a NULL encoder,
 *         otherwise the libopus error code from opus_encoder_ctl.
 */
int32_t cobalt_opus_encoder_set_complexity(void *enc, int32_t complexity);

/**
 * Enables or disables in-band forward error correction.
 *
 * Applies OPUS_SET_INBAND_FEC through opus_encoder_ctl. In-band FEC applies only
 * to the LPC (SILK) layer.
 *
 * @param enc the encoder handle from cobalt_opus_encoder_create.
 * @param fec 0 to disable, 1 to enable (switching to SILK under loss when
 *            beneficial), 2 to enable without forcing SILK for music.
 * @return COBALT_OPUS_OK on success, COBALT_OPUS_BAD_ARG for a NULL encoder,
 *         otherwise the libopus error code from opus_encoder_ctl.
 */
int32_t cobalt_opus_encoder_set_inband_fec(void *enc, int32_t fec);

/**
 * Sets the encoder's expected packet-loss percentage.
 *
 * Applies OPUS_SET_PACKET_LOSS_PERC through opus_encoder_ctl. Higher values
 * trigger progressively more loss-resistant coding at the expense of quality in
 * the absence of loss.
 *
 * @param enc      the encoder handle from cobalt_opus_encoder_create.
 * @param lossPerc the expected loss percentage in the range 0 to 100.
 * @return COBALT_OPUS_OK on success, COBALT_OPUS_BAD_ARG for a NULL encoder,
 *         otherwise the libopus error code from opus_encoder_ctl.
 */
int32_t cobalt_opus_encoder_set_packet_loss_perc(void *enc, int32_t lossPerc);

/**
 * Enables or disables discontinuous transmission.
 *
 * Applies OPUS_SET_DTX through opus_encoder_ctl. DTX applies only to the LPC
 * (SILK) layer.
 *
 * @param enc the encoder handle from cobalt_opus_encoder_create.
 * @param dtx 1 to enable discontinuous transmission, 0 to disable.
 * @return COBALT_OPUS_OK on success, COBALT_OPUS_BAD_ARG for a NULL encoder,
 *         otherwise the libopus error code from opus_encoder_ctl.
 */
int32_t cobalt_opus_encoder_set_dtx(void *enc, int32_t dtx);

/**
 * Forces the encoder to code as mono or stereo regardless of the input format.
 *
 * Applies OPUS_SET_FORCE_CHANNELS through opus_encoder_ctl.
 *
 * @param enc      the encoder handle from cobalt_opus_encoder_create.
 * @param channels COBALT_OPUS_AUTO for not forced, 1 for forced mono, 2 for
 *                 forced stereo.
 * @return COBALT_OPUS_OK on success, COBALT_OPUS_BAD_ARG for a NULL encoder,
 *         otherwise the libopus error code from opus_encoder_ctl.
 */
int32_t cobalt_opus_encoder_set_force_channels(void *enc, int32_t channels);

/**
 * Sets the type-of-signal hint that biases the encoder's mode selection.
 *
 * Applies OPUS_SET_SIGNAL through opus_encoder_ctl.
 *
 * @param enc    the encoder handle from cobalt_opus_encoder_create.
 * @param signal COBALT_OPUS_AUTO, COBALT_OPUS_SIGNAL_VOICE or
 *               COBALT_OPUS_SIGNAL_MUSIC.
 * @return COBALT_OPUS_OK on success, COBALT_OPUS_BAD_ARG for a NULL encoder,
 *         otherwise the libopus error code from opus_encoder_ctl.
 */
int32_t cobalt_opus_encoder_set_signal(void *enc, int32_t signal);

/**
 * Sets the encoder's hinted signal depth in significant bits.
 *
 * Applies OPUS_SET_LSB_DEPTH through opus_encoder_ctl. The value helps the
 * encoder identify near-silence; for opus_encode (integer) input the effective
 * value is the minimum of this and 16.
 *
 * @param enc   the encoder handle from cobalt_opus_encoder_create.
 * @param depth the input precision in bits, between 8 and 24.
 * @return COBALT_OPUS_OK on success, COBALT_OPUS_BAD_ARG for a NULL encoder,
 *         otherwise the libopus error code from opus_encoder_ctl.
 */
int32_t cobalt_opus_encoder_set_lsb_depth(void *enc, int32_t depth);

/**
 * Pins the encoder's coded bandpass to a specific value.
 *
 * Applies OPUS_SET_BANDWIDTH through opus_encoder_ctl. Most callers should
 * instead leave this at COBALT_OPUS_AUTO and bound the bandwidth with
 * cobalt_opus_encoder_set_max_bandwidth.
 *
 * @param enc       the encoder handle from cobalt_opus_encoder_create.
 * @param bandwidth COBALT_OPUS_AUTO or one of the COBALT_OPUS_BANDWIDTH_*
 *                  selectors.
 * @return COBALT_OPUS_OK on success, COBALT_OPUS_BAD_ARG for a NULL encoder,
 *         otherwise the libopus error code from opus_encoder_ctl.
 */
int32_t cobalt_opus_encoder_set_bandwidth(void *enc, int32_t bandwidth);

/**
 * Sets the maximum bandpass the encoder may select automatically.
 *
 * Applies OPUS_SET_MAX_BANDWIDTH through opus_encoder_ctl. Unlike
 * cobalt_opus_encoder_set_bandwidth, this leaves the encoder free to reduce the
 * bandpass when the bitrate becomes too low.
 *
 * @param enc       the encoder handle from cobalt_opus_encoder_create.
 * @param bandwidth one of the COBALT_OPUS_BANDWIDTH_* selectors.
 * @return COBALT_OPUS_OK on success, COBALT_OPUS_BAD_ARG for a NULL encoder,
 *         otherwise the libopus error code from opus_encoder_ctl.
 */
int32_t cobalt_opus_encoder_set_max_bandwidth(void *enc, int32_t bandwidth);

/**
 * Selects the encoder's use of variable-duration frames.
 *
 * Applies OPUS_SET_EXPERT_FRAME_DURATION through opus_encoder_ctl. When set to
 * anything other than COBALT_OPUS_FRAMESIZE_ARG the encoder is free to emit a
 * shorter frame than requested and the caller must inspect the encoded TOC.
 *
 * @param enc           the encoder handle from cobalt_opus_encoder_create.
 * @param frameDuration COBALT_OPUS_FRAMESIZE_ARG or one of the
 *                      COBALT_OPUS_FRAMESIZE_*_MS selectors.
 * @return COBALT_OPUS_OK on success, COBALT_OPUS_BAD_ARG for a NULL encoder,
 *         otherwise the libopus error code from opus_encoder_ctl.
 */
int32_t cobalt_opus_encoder_set_expert_frame_duration(void *enc, int32_t frameDuration);

/**
 * Enables or disables the encoder's use of inter-frame prediction.
 *
 * Applies OPUS_SET_PREDICTION_DISABLED through opus_encoder_ctl. Disabling
 * prediction makes frames nearly independent at a quality cost.
 *
 * @param enc      the encoder handle from cobalt_opus_encoder_create.
 * @param disabled 1 to disable prediction, 0 to enable it (the default).
 * @return COBALT_OPUS_OK on success, COBALT_OPUS_BAD_ARG for a NULL encoder,
 *         otherwise the libopus error code from opus_encoder_ctl.
 */
int32_t cobalt_opus_encoder_set_prediction_disabled(void *enc, int32_t disabled);

/**
 * Reads the total codec lookahead, in samples, the encoder adds.
 *
 * Applies OPUS_GET_LOOKAHEAD through opus_encoder_ctl. The value lets a caller
 * align encoder input and decoder output by skipping this many samples.
 *
 * @param enc the encoder handle from cobalt_opus_encoder_create.
 * @param out receives the lookahead sample count on success; must not be NULL.
 * @return COBALT_OPUS_OK on success, COBALT_OPUS_BAD_ARG for a NULL argument,
 *         otherwise the libopus error code from opus_encoder_ctl.
 */
int32_t cobalt_opus_encoder_get_lookahead(void *enc, int32_t *out);

/**
 * Resets the encoder to a freshly initialized state.
 *
 * Applies OPUS_RESET_STATE through opus_encoder_ctl, clearing the encode history
 * without changing the sampling rate, channel count or application mode.
 *
 * @param enc the encoder handle from cobalt_opus_encoder_create.
 * @return COBALT_OPUS_OK on success, COBALT_OPUS_BAD_ARG for a NULL encoder,
 *         otherwise the libopus error code from opus_encoder_ctl.
 */
int32_t cobalt_opus_encoder_reset_state(void *enc);

/**
 * Creates and initializes a libopus decoder for one stream.
 *
 * Forwards to opus_decoder_create with the supplied sampling rate and channel
 * count. The created decoder is returned through outDec as an opaque handle.
 *
 * @param fs       the output sampling rate in hertz; one of 8000, 12000, 16000,
 *                 24000 or 48000.
 * @param channels the number of channels to decode, 1 or 2.
 * @param outDec   receives the created decoder handle on success, or is left
 *                 holding NULL on failure; must not be NULL.
 * @return COBALT_OPUS_OK on success, COBALT_OPUS_BAD_ARG for a NULL outDec,
 *         otherwise the libopus error code from opus_decoder_create.
 */
int32_t cobalt_opus_decoder_create(int32_t fs, int32_t channels, void **outDec);

/**
 * Destroys a decoder created by cobalt_opus_decoder_create.
 *
 * Forwards to opus_decoder_destroy. Must be called at most once per handle. A
 * NULL handle is ignored.
 *
 * @param dec the decoder handle, or NULL.
 */
void cobalt_opus_decoder_destroy(void *dec);

/**
 * Decodes one Opus packet into interleaved signed 16-bit PCM.
 *
 * Forwards to opus_decode. A NULL data pointer with len 0 requests packet-loss
 * concealment for one missing frame; a non-NULL packet with decodeFec set to 1
 * reconstructs the previous lost frame from this packet's in-band LBRR copy.
 * frameSize is the per-channel capacity of pcm in samples; for the concealment
 * and forward-error-correction cases it must equal the duration of the missing
 * audio and be a multiple of 2.5 ms. The return value is the number of samples
 * per channel decoded, or negative on error.
 *
 * @param dec       the decoder handle from cobalt_opus_decoder_create.
 * @param data      the compressed packet bytes, or NULL to request concealment.
 * @param len       the packet length in bytes, or 0 when data is NULL.
 * @param pcm       the output buffer the decoded interleaved PCM is written into.
 * @param frameSize the per-channel sample capacity of pcm.
 * @param decodeFec 1 to decode in-band forward error correction, 0 for a normal
 *                  decode.
 * @return the per-channel decoded sample count on success, or a negative libopus
 *         error code on failure.
 */
int32_t cobalt_opus_decode(void *dec, const uint8_t *data, int32_t len, int16_t *pcm, int32_t frameSize, int32_t decodeFec);

/**
 * Resets the decoder to a freshly initialized state.
 *
 * Applies OPUS_RESET_STATE through opus_decoder_ctl, clearing the decode history
 * including the concealment buffer without changing the sampling rate or channel
 * count.
 *
 * @param dec the decoder handle from cobalt_opus_decoder_create.
 * @return COBALT_OPUS_OK on success, COBALT_OPUS_BAD_ARG for a NULL decoder,
 *         otherwise the libopus error code from opus_decoder_ctl.
 */
int32_t cobalt_opus_decoder_reset_state(void *dec);

/**
 * Allocates and initializes a libopus repacketizer.
 *
 * Forwards to opus_repacketizer_create. The created repacketizer is returned
 * through outRp as an opaque handle and is ready to accept packets with
 * cobalt_opus_repacketizer_cat; the queue is cleared between uses with
 * cobalt_opus_repacketizer_init.
 *
 * @param outRp receives the created repacketizer handle on success, or is left
 *              holding NULL on failure; must not be NULL.
 * @return COBALT_OPUS_OK on success, COBALT_OPUS_BAD_ARG for a NULL outRp,
 *         COBALT_OPUS_ALLOC_FAIL if libopus could not allocate the state.
 */
int32_t cobalt_opus_repacketizer_create(void **outRp);

/**
 * Destroys a repacketizer created by cobalt_opus_repacketizer_create.
 *
 * Forwards to opus_repacketizer_destroy. Must be called at most once per handle.
 * A NULL handle is ignored.
 *
 * @param rp the repacketizer handle, or NULL.
 */
void cobalt_opus_repacketizer_destroy(void *rp);

/**
 * Re-initializes a repacketizer, clearing its queue of submitted frames.
 *
 * Forwards to opus_repacketizer_init. Must be called between independent
 * combine or split operations, and whenever the submitted-packet configuration
 * (coding mode, bandwidth, frame size or channel count) changes or the 120 ms
 * total-duration limit is reached.
 *
 * @param rp the repacketizer handle from cobalt_opus_repacketizer_create.
 * @return COBALT_OPUS_OK on success, COBALT_OPUS_BAD_ARG for a NULL handle.
 */
int32_t cobalt_opus_repacketizer_init(void *rp);

/**
 * Appends one Opus packet to the repacketizer's queue.
 *
 * Forwards to opus_repacketizer_cat. The packet must match the coding mode,
 * bandwidth, frame size and channel count of any packets submitted since the
 * last cobalt_opus_repacketizer_init, and the running total duration must not
 * exceed 120 ms. libopus does NOT copy the bytes; the data buffer must remain
 * valid until the next cobalt_opus_repacketizer_init, _out_range, _out or
 * _destroy on this handle.
 *
 * @param rp   the repacketizer handle from cobalt_opus_repacketizer_create.
 * @param data the Opus packet bytes to append.
 * @param len  the packet length in bytes.
 * @return COBALT_OPUS_OK on success, COBALT_OPUS_BAD_ARG for a NULL argument,
 *         COBALT_OPUS_INVALID_PACKET if the packet is malformed or incompatible
 *         with the queued packets.
 */
int32_t cobalt_opus_repacketizer_cat(void *rp, const uint8_t *data, int32_t len);

/**
 * Returns the total number of frames currently queued in the repacketizer.
 *
 * Forwards to opus_repacketizer_get_nb_frames. This bounds the frame indices
 * valid for cobalt_opus_repacketizer_out_range.
 *
 * @param rp the repacketizer handle from cobalt_opus_repacketizer_create.
 * @return the queued frame count (zero or more) on success, or
 *         COBALT_OPUS_BAD_ARG for a NULL handle.
 */
int32_t cobalt_opus_repacketizer_get_nb_frames(void *rp);

/**
 * Builds one packet from a contiguous range of the repacketizer's queued frames.
 *
 * Forwards to opus_repacketizer_out_range, writing a packet holding frames
 * [begin, end) into data up to maxLen bytes. The return value is the packet
 * length in bytes, or negative on error.
 *
 * @param rp     the repacketizer handle from cobalt_opus_repacketizer_create.
 * @param begin  the index of the first frame to include.
 * @param end    one past the index of the last frame to include.
 * @param data   the output buffer the packet is written into.
 * @param maxLen the capacity of data in bytes.
 * @return the output packet length in bytes on success, or a negative libopus
 *         error code (COBALT_OPUS_BAD_ARG for an invalid range or NULL argument,
 *         COBALT_OPUS_BUFFER_TOO_SMALL if maxLen is insufficient).
 */
int32_t cobalt_opus_repacketizer_out_range(void *rp, int32_t begin, int32_t end, uint8_t *data, int32_t maxLen);

/**
 * Builds one packet from all of the repacketizer's queued frames.
 *
 * Forwards to opus_repacketizer_out, the convenience equivalent of
 * cobalt_opus_repacketizer_out_range over the full queued range.
 *
 * @param rp     the repacketizer handle from cobalt_opus_repacketizer_create.
 * @param data   the output buffer the packet is written into.
 * @param maxLen the capacity of data in bytes.
 * @return the output packet length in bytes on success, or a negative libopus
 *         error code (COBALT_OPUS_BAD_ARG for a NULL argument,
 *         COBALT_OPUS_BUFFER_TOO_SMALL if maxLen is insufficient).
 */
int32_t cobalt_opus_repacketizer_out(void *rp, uint8_t *data, int32_t maxLen);

/**
 * Parses an Opus packet into its constituent frames, returning a portable
 * framing description.
 *
 * Wraps opus_packet_parse, whose native output is an array of pointers INTO the
 * input packet (const unsigned char *frames[48]) that no portable binding can
 * read back. This wrapper instead writes, for each parsed frame, its byte OFFSET
 * from the start of data into outFrameOffsets and its length into outFrameSizes,
 * and writes the table-of-contents byte through outToc and the payload start
 * offset through outPayloadOffset. The caller-supplied arrays must each hold at
 * least 48 entries (the maximum frames per Opus packet). The frame bytes are not
 * copied; the caller reads them from data using the returned offsets and sizes.
 *
 * @param data             the Opus packet to parse.
 * @param len              the packet length in bytes.
 * @param outToc           receives the table-of-contents byte; may be NULL to
 *                         skip.
 * @param outFrameOffsets  receives each frame's byte offset from data; must hold
 *                         at least 48 int32 entries and must not be NULL.
 * @param outFrameSizes    receives each frame's length in bytes; must hold at
 *                         least 48 int32 entries and must not be NULL.
 * @param outPayloadOffset receives the payload start offset from data; may be
 *                         NULL to skip.
 * @return the number of frames parsed (zero or more) on success, or a negative
 *         libopus error code (COBALT_OPUS_BAD_ARG for a NULL required argument,
 *         COBALT_OPUS_INVALID_PACKET for a malformed packet).
 */
int32_t cobalt_opus_packet_parse(const uint8_t *data,
                                 int32_t len,
                                 uint8_t *outToc,
                                 int32_t *outFrameOffsets,
                                 int32_t *outFrameSizes,
                                 int32_t *outPayloadOffset);

/**
 * Returns the number of frames in an Opus packet.
 *
 * Forwards to opus_packet_get_nb_frames.
 *
 * @param data the Opus packet bytes.
 * @param len  the packet length in bytes.
 * @return the frame count on success, or a negative libopus error code
 *         (COBALT_OPUS_BAD_ARG for a NULL packet or insufficient data,
 *         COBALT_OPUS_INVALID_PACKET for a malformed packet).
 */
int32_t cobalt_opus_packet_get_nb_frames(const uint8_t *data, int32_t len);

/**
 * Returns the number of samples per frame of an Opus packet at a sampling rate.
 *
 * Forwards to opus_packet_get_samples_per_frame.
 *
 * @param data the Opus packet bytes; at least one byte must be readable.
 * @param fs   the sampling rate in hertz; must be a multiple of 400 for an exact
 *             result.
 * @return the per-frame sample count.
 */
int32_t cobalt_opus_packet_get_samples_per_frame(const uint8_t *data, int32_t fs);

/**
 * Returns the coded bandwidth of an Opus packet.
 *
 * Forwards to opus_packet_get_bandwidth.
 *
 * @param data the Opus packet bytes.
 * @return one of the COBALT_OPUS_BANDWIDTH_* selectors on success, or
 *         COBALT_OPUS_INVALID_PACKET for a malformed packet.
 */
int32_t cobalt_opus_packet_get_bandwidth(const uint8_t *data);

/**
 * Returns the number of channels coded in an Opus packet.
 *
 * Forwards to opus_packet_get_nb_channels.
 *
 * @param data the Opus packet bytes.
 * @return the channel count on success, or COBALT_OPUS_INVALID_PACKET for a
 *         malformed packet.
 */
int32_t cobalt_opus_packet_get_nb_channels(const uint8_t *data);

/**
 * Reports whether an Opus packet carries an in-band forward-error-correction
 * (LBRR) copy of the previous frame.
 *
 * Forwards to opus_packet_has_lbrr, the direct primitive for the per-packet FEC
 * read; it inspects the TOC and per-SILK-frame LBRR flags.
 *
 * @param data the Opus packet bytes.
 * @param len  the packet length in bytes.
 * @return 1 if an LBRR copy is present, 0 if not, or COBALT_OPUS_INVALID_PACKET
 *         for a malformed packet.
 */
int32_t cobalt_opus_packet_has_lbrr(const uint8_t *data, int32_t len);

/**
 * Returns a textual description of a libopus error code.
 *
 * Forwards to opus_strerror. The returned pointer is static and must not be
 * freed.
 *
 * @param err a libopus error code, such as one returned by another
 *            cobalt_opus_* function.
 * @return a NUL-terminated, statically-allocated description.
 */
const char *cobalt_opus_strerror(int32_t err);

/**
 * Returns the libopus version string.
 *
 * Forwards to opus_get_version_string. The returned pointer is static and must
 * not be freed; the substring "-fixed" indicates a fixed-point build.
 *
 * @return a NUL-terminated, statically-allocated version string.
 */
const char *cobalt_opus_get_version_string(void);

#ifdef __cplusplus
}
#endif

#endif /* COBALT_OPUS_SHIM_H */
