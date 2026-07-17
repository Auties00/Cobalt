package com.github.auties00.cobalt.wire.cloud;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Objects;

/**
 * A WebRTC session description exchanged over the WhatsApp Cloud API Calling endpoint.
 *
 * <p>The Calling API carries SDP negotiation in a {@code session} object holding the description type
 * and the SDP blob. A business sends an {@link Type#OFFER} when it initiates a call ({@code connect})
 * and an {@link Type#ANSWER} when it accepts an inbound call ({@code pre_accept} or {@code accept}).
 */
public final class CloudCallSession {
    /**
     * The SDP description type, {@link Type#OFFER} for a connect or {@link Type#ANSWER} for a
     * pre-accept or accept.
     */
    private final Type sdpType;

    /**
     * The SDP description blob.
     */
    private final String sdp;

    /**
     * Constructs a new call session.
     *
     * @param sdpType the SDP description type, {@link Type#OFFER} or {@link Type#ANSWER}
     * @param sdp     the SDP description blob
     * @throws NullPointerException if {@code sdpType} or {@code sdp} is {@code null}
     */
    public CloudCallSession(Type sdpType, String sdp) {
        this.sdpType = Objects.requireNonNull(sdpType, "sdpType must not be null");
        this.sdp = Objects.requireNonNull(sdp, "sdp must not be null");
    }

    /**
     * Returns the SDP description type.
     *
     * @return the SDP type, {@link Type#OFFER} or {@link Type#ANSWER}
     */
    public Type sdpType() {
        return sdpType;
    }

    /**
     * Returns the SDP description blob.
     *
     * @return the SDP blob
     */
    public String sdp() {
        return sdp;
    }

    /**
     * The SDP description type of a {@link CloudCallSession}.
     *
     * <p>An {@link #OFFER} carries the caller's session proposal and an {@link #ANSWER} carries the
     * answerer's response. The {@link #UNKNOWN} constant guards against tokens this client does not yet
     * model.
     *
     * <p>The wire tokens are lowercase; {@link #OFFER} serializes as {@code offer} and {@link #ANSWER}
     * serializes as {@code answer} through {@link #token()}.
     */
    @ProtobufEnum
    public enum Type {
        /**
         * A type that this client does not recognise. Resolved for any token outside the modelled set so
         * that an unexpected value never fails decoding.
         */
        UNKNOWN(0, "unknown"),

        /**
         * The caller's session proposal.
         */
        OFFER(1, "offer"),

        /**
         * The answerer's session response.
         */
        ANSWER(2, "answer");

        /**
         * The protobuf-assigned numeric index for this type.
         */
        final int index;

        /**
         * The lowercase WhatsApp wire token for this type.
         */
        final String token;

        /**
         * Constructs a {@code Type} with the specified protobuf index and wire token.
         *
         * @param index the protobuf enum index
         * @param token the lowercase wire token
         */
        Type(@ProtobufEnumIndex int index, String token) {
            this.index = index;
            this.token = token;
        }

        /**
         * Returns the {@code Type} matching the given wire token.
         *
         * <p>The lookup matches both the constant name and the lowercase wire token case-insensitively
         * against {@code input}; any unrecognised or {@code null} token resolves to {@link #UNKNOWN} so
         * that decoding never fails on an unexpected value.
         *
         * @param input the wire token, for example {@code "offer"}, or {@code null}
         * @return the matching type, or {@link #UNKNOWN} when {@code input} matches no constant
         */
        public static Type of(String input) {
            if (input == null) {
                return UNKNOWN;
            }
            for (var value : values()) {
                if (value != UNKNOWN && (value.name().equalsIgnoreCase(input) || value.token.equalsIgnoreCase(input))) {
                    return value;
                }
            }
            return UNKNOWN;
        }

        /**
         * Returns the lowercase WhatsApp wire token for this type.
         *
         * @return the wire token, {@code "offer"} or {@code "answer"}
         */
        public String token() {
            return token;
        }

        /**
         * Returns the protobuf-assigned numeric index for this type.
         *
         * @return the protobuf enum index
         */
        public int index() {
            return index;
        }
    }
}
