package com.github.auties00.cobalt.exception.linked;

import com.github.auties00.cobalt.calls.media.audio.codec.opus.bindings.CobaltOpus;
import com.github.auties00.cobalt.calls.media.video.codec.vpx.bindings.CobaltVpx;
import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;

import java.lang.foreign.MemorySegment;

/**
 * Sealed root for failures on Cobalt's voice and video call stack: the
 * audio and video codecs, the RTP/SRTP packetisation layer, and the
 * WebRTC transports (ICE, DTLS, SCTP, DataChannel) underneath.
 *
 * A WhatsApp voice or video call lives on a separate set of connections
 * from the messaging WebSocket. The flow gathers ICE candidates,
 * negotiates a DTLS-SRTP session over the winning candidate pair,
 * exchanges media as encrypted RTP packets, and optionally carries control
 * traffic on a DataChannel layered over SCTP. Each step can fail
 * independently, and each failure surfaces through one of the nested
 * subtypes; native codec failures (libopus, libvpx, openh264, libspeexdsp)
 * follow the same pattern. The permits list is closed, so a {@code switch}
 * over a {@code WhatsAppCallException} can be exhaustive.
 *
 * @apiNote
 * Catch this base type to react to every call-layer failure mode at once.
 * Because every subtype returns {@link WhatsAppLinkedClientErrorResult#DISCARD}
 * from {@link #toErrorResult()}, the configured error handler can retry,
 * fall back to a different transport, or surface the failure to the user
 * without tearing the messaging session down.
 *
 * @implNote
 * This implementation always returns
 * {@link WhatsAppLinkedClientErrorResult#DISCARD}: call failures are isolated to a
 * single call and never tear the messaging session down. The call subsystem
 * is Cobalt-native; WA Web routes the equivalent failures through its
 * WASM-loaded {@code libwebrtc} build rather than through a public exception
 * hierarchy.
 *
 * @see Opus
 * @see AudioProcessing
 * @see Rtp
 * @see Srtp
 * @see DtlsHandshake
 * @see Ice
 * @see DataChannel
 * @see Sctp
 * @see H264
 * @see Vpx
 * @see Av1
 */
public sealed abstract class WhatsAppCallException
        extends WhatsAppLinkedException
        permits WhatsAppCallException.Opus,
                WhatsAppCallException.AudioProcessing,
                WhatsAppCallException.Rtp,
                WhatsAppCallException.Srtp,
                WhatsAppCallException.DtlsHandshake,
                WhatsAppCallException.Ice,
                WhatsAppCallException.DataChannel,
                WhatsAppCallException.Sctp,
                WhatsAppCallException.H264,
                WhatsAppCallException.Vpx,
                WhatsAppCallException.Av1 {

    /**
     * Constructs a new call exception with the specified detail message.
     *
     * @param message the detail message describing the call error
     */
    protected WhatsAppCallException(String message) {
        super(message);
    }

    /**
     * Constructs a new call exception with the specified detail message and cause.
     *
     * @param message the detail message describing the call error
     * @param cause   the underlying cause of this exception
     */
    protected WhatsAppCallException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns
     * {@link WhatsAppLinkedClientErrorResult#DISCARD}: call failures are isolated
     * to a single call and never tear the messaging session down.
     */
    @Override
    public WhatsAppLinkedClientErrorResult toErrorResult() {
        return WhatsAppLinkedClientErrorResult.DISCARD;
    }

    /**
     * Thrown when libopus encode or decode fails.
     *
     * Reports either a non-zero {@code OPUS_*} error code or a Java-side
     * invariant violation surfaced by the Foreign Function and Memory
     * downcall.
     *
     * @apiNote
     * Callers that already hold a libopus error code can use
     * {@link #fromErr(String, int)} to build a message that includes
     * libopus's own textual description from {@code opus_strerror}.
     */
    public static final class Opus extends WhatsAppCallException {
        /**
         * Constructs a new Opus exception with the specified message.
         *
         * @param message the detail message describing the codec failure
         */
        public Opus(String message) {
            super(message);
        }

        /**
         * Constructs a new Opus exception with the specified message and cause.
         *
         * @param message the detail message describing the codec failure
         * @param cause   the underlying cause
         */
        public Opus(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Builds an exception whose message embeds libopus's textual
         * description of the given {@code OPUS_*} error code.
         *
         * @apiNote
         * Use at native call sites where a non-zero return code needs to
         * be turned into a thrown exception with the canonical libopus
         * message ("buffer too small", "invalid packet", and so on).
         *
         * @param prefix  human-readable context (e.g. "encode failed")
         * @param errCode the libopus error code (negative for failures)
         * @return a new exception ready to throw
         */
        public static Opus fromErr(String prefix, int errCode) {
            return new Opus(prefix + ": " + opusErrString(errCode) + " (code " + errCode + ")");
        }

        /**
         * Reads the shim's static error string for the given error code.
         *
         * @implNote
         * This implementation calls {@link CobaltOpus#cobalt_opus_strerror},
         * which forwards to libopus's own {@code opus_strerror}. It reinterprets
         * the returned pointer for the full address space and returns the
         * literal {@code "unknown"} if the lookup throws or yields
         * {@link MemorySegment#NULL}, so exception construction can never itself
         * fail.
         *
         * @param errCode the libopus error code
         * @return the error string, or {@code "unknown"} if the lookup fails
         */
        private static String opusErrString(int errCode) {
            try {
                var ptr = CobaltOpus.cobalt_opus_strerror(errCode);
                if (ptr.equals(MemorySegment.NULL)) {
                    return "unknown";
                }
                return ptr.reinterpret(Long.MAX_VALUE).getString(0);
            } catch (Throwable t) {
                return "unknown";
            }
        }
    }

    /**
     * Thrown when the WebRTC Audio Processing Module call-capture conditioner fails or returns an error
     * code.
     *
     * Wraps the {@link Throwable}s thrown by the Foreign Function and Memory downcalls into the
     * {@code cobalt_webrtc_apm_*} shim so callers do not have to catch {@link Throwable} themselves. The
     * WebRTC APM surface Cobalt uses is the live-capture echo canceller (AEC3), the noise suppressor with
     * the ML denoiser, and the gain controller, the conditioning WhatsApp applies through
     * {@code wa_mobile_audio_processing.cc}.
     */
    public static final class AudioProcessing extends WhatsAppCallException {
        /**
         * Constructs a new WebRTC audio-processing exception with the specified message.
         *
         * @param message the detail message describing the failure
         */
        public AudioProcessing(String message) {
            super(message);
        }

        /**
         * Constructs a new WebRTC audio-processing exception with the specified message and cause.
         *
         * @param message the detail message describing the failure
         * @param cause   the underlying cause
         */
        public AudioProcessing(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when RTP packet encode/decode, jitter-buffer ordering, or
     * SRTP protect/unprotect fails at the RTP layer.
     *
     * Covers both protocol-level errors (truncated header, wrong version,
     * SSRC mismatch) and Java-side invariant violations from the
     * packetiser.
     */
    public static final class Rtp extends WhatsAppCallException {
        /**
         * Constructs a new RTP exception with the specified message.
         *
         * @param message the detail message describing the failure
         */
        public Rtp(String message) {
            super(message);
        }

        /**
         * Constructs a new RTP exception with the specified message and cause.
         *
         * @param message the detail message describing the failure
         * @param cause   the underlying cause
         */
        public Rtp(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when SRTP/SRTCP packet protection or unprotection fails.
     *
     * Wraps the underlying {@link java.security.GeneralSecurityException}
     * (cipher or HMAC initialisation or transformation errors), or stands
     * on its own to report packet-format violations, replay detection, and
     * authentication-tag mismatches.
     */
    public static final class Srtp extends WhatsAppCallException {
        /**
         * Constructs a new SRTP exception with the specified message.
         *
         * @param message the detail message describing the failure
         */
        public Srtp(String message) {
            super(message);
        }

        /**
         * Constructs a new SRTP exception with the specified message and cause.
         *
         * @param message the detail message describing the failure
         * @param cause   the underlying cause
         */
        public Srtp(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when the DTLS-SRTP handshake fails.
     *
     * Triggered by peer fingerprint mismatch, unsupported SRTP profile, an
     * alert received from the peer, or any other handshake-layer fault that
     * prevents the SRTP keys from being derived.
     */
    public static final class DtlsHandshake extends WhatsAppCallException {
        /**
         * Constructs a new DTLS handshake exception with the specified message.
         *
         * @param message the detail message describing the handshake failure
         */
        public DtlsHandshake(String message) {
            super(message);
        }

        /**
         * Constructs a new DTLS handshake exception with the specified message and cause.
         *
         * @param message the detail message describing the handshake failure
         * @param cause   the underlying cause
         */
        public DtlsHandshake(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when ICE candidate gathering, connectivity checks, or
     * candidate-pair nomination fail.
     *
     * Covers both protocol-level errors (malformed STUN response, missing
     * {@code MESSAGE-INTEGRITY}) and Java-side invariant violations from the
     * candidate-pair state machine.
     */
    public static final class Ice extends WhatsAppCallException {
        /**
         * Constructs a new ICE exception with the specified message.
         *
         * @param message the detail message describing the failure
         */
        public Ice(String message) {
            super(message);
        }

        /**
         * Constructs a new ICE exception with the specified message and cause.
         *
         * @param message the detail message describing the failure
         * @param cause   the underlying cause
         */
        public Ice(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when a WebRTC DataChannel operation fails.
     *
     * Covers a malformed DCEP message on the wire, attempts to use a
     * channel in the wrong state, stream-id collisions, and unsupported
     * channel types. Failures from the underlying SCTP association are
     * chained as a {@link Sctp} cause.
     */
    public static final class DataChannel extends WhatsAppCallException {
        /**
         * Constructs a new DataChannel exception with the specified message.
         *
         * @param message the detail message describing the failure
         */
        public DataChannel(String message) {
            super(message);
        }

        /**
         * Constructs a new DataChannel exception with the specified message and cause.
         *
         * @param message the detail message describing the failure
         * @param cause   the underlying cause
         */
        public DataChannel(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when a usrsctp operation fails.
     *
     * Reports either a non-zero return code from the C library or a
     * Java-side invariant violation (closed socket, wrong-sized buffer,
     * association in an unexpected state).
     */
    public static final class Sctp extends WhatsAppCallException {
        /**
         * Constructs a new SCTP exception with the specified message.
         *
         * @param message the detail message describing the failure
         */
        public Sctp(String message) {
            super(message);
        }

        /**
         * Constructs a new SCTP exception with the specified message and cause.
         *
         * @param message the detail message describing the failure
         * @param cause   the underlying cause
         */
        public Sctp(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when an openh264 operation fails.
     *
     * <p>Reports a non-zero status from the portable {@code cobalt_h264_*} shim that drives the OpenH264
     * encoder and decoder (a shim-level error such as a bad argument or a create failure, or a forwarded
     * OpenH264 vtable-method failure from create, encode, or decode) or a Java-side invariant violation
     * (closed codec, wrong frame size).
     */
    public static final class H264 extends WhatsAppCallException {
        /**
         * Constructs a new H.264 exception with the specified message.
         *
         * @param message the detail message describing the failure
         */
        public H264(String message) {
            super(message);
        }

        /**
         * Constructs a new H.264 exception with the specified message and cause.
         *
         * @param message the detail message describing the failure
         * @param cause   the underlying cause
         */
        public H264(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when an AV1 decode or encode operation fails.
     *
     * Reports a non-zero status from the AV1 native path: the dav1d decoder
     * (via the {@code CobaltDav1d} shim) or the rav1e encoder (via the
     * {@code CobaltRav1e} shim), or a Java-side invariant violation such as a
     * closed codec or an unsupported pixel layout.
     */
    public static final class Av1 extends WhatsAppCallException {
        /**
         * Constructs a new AV1 exception with the specified message.
         *
         * @param message the detail message describing the failure
         */
        public Av1(String message) {
            super(message);
        }

        /**
         * Constructs a new AV1 exception with the specified message and cause.
         *
         * @param message the detail message describing the failure
         * @param cause   the underlying cause
         */
        public Av1(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Builds an exception whose message embeds a non-zero AV1 shim status
         * code.
         *
         * @apiNote
         * Use at native call sites where a dav1d or rav1e shim status needs to
         * be turned into a thrown exception; the shim status codes are stable
         * small integers documented on the {@code COBALT_DAV1D_*} and
         * {@code COBALT_RAV1E_*} constants.
         *
         * @param prefix human-readable context (e.g. "cobalt_dav1d_send_data")
         * @param status the shim status code
         * @return a new exception ready to throw
         */
        public static Av1 fromErr(String prefix, int status) {
            return new Av1(prefix + " (status " + status + ")");
        }
    }

    /**
     * Thrown when a libvpx operation fails.
     *
     * Reports a non-zero {@code vpx_codec_err_t} return code or a Java-side
     * invariant violation (closed codec, wrong frame dimensions).
     *
     * @apiNote
     * Callers holding a {@code vpx_codec_err_t} value can use
     * {@link #fromErr(String, int)} to build a message that includes
     * libvpx's own textual description from {@code vpx_codec_err_to_string}.
     */
    public static final class Vpx extends WhatsAppCallException {
        /**
         * Constructs a new VPx exception with the specified message.
         *
         * @param message the detail message describing the failure
         */
        public Vpx(String message) {
            super(message);
        }

        /**
         * Constructs a new VPx exception with the specified message and cause.
         *
         * @param message the detail message describing the failure
         * @param cause   the underlying cause
         */
        public Vpx(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Builds an exception whose message embeds libvpx's textual
         * description of the given {@code vpx_codec_err_t} code.
         *
         * @apiNote
         * Use at native call sites where a non-zero return code needs to
         * be turned into a thrown exception with the canonical libvpx
         * message.
         *
         * @param prefix  human-readable context (e.g. "encode failed")
         * @param errCode the {@code vpx_codec_err_t} return value
         * @return a new exception ready to throw
         */
        public static Vpx fromErr(String prefix, int errCode) {
            return new Vpx(prefix + ": " + vpxErrString(errCode) + " (code " + errCode + ")");
        }

        /**
         * Reads the shim's static error string for the given status code.
         *
         * @implNote
         * This implementation calls {@link CobaltVpx#cobalt_vpx_strerror}, which returns libvpx's own
         * description for a positive {@code vpx_codec_err_t} and a fixed shim string for the negative
         * shim-level codes. It reinterprets the returned pointer for the full address space and returns
         * the literal {@code "unknown"} if the lookup throws or yields {@link MemorySegment#NULL}, so
         * exception construction can never itself fail.
         *
         * @param errCode the status code returned by a {@code cobalt_vpx_*} call
         * @return the error string, or {@code "unknown"} if the lookup fails
         */
        private static String vpxErrString(int errCode) {
            try {
                var ptr = CobaltVpx.cobalt_vpx_strerror(errCode);
                if (ptr.equals(MemorySegment.NULL)) {
                    return "unknown";
                }
                return ptr.reinterpret(Long.MAX_VALUE).getString(0);
            } catch (Throwable t) {
                return "unknown";
            }
        }
    }
}
