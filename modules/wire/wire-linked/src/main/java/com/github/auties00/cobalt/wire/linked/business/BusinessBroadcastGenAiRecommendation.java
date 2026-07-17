package com.github.auties00.cobalt.wire.linked.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Generative-AI message recommendation returned for a WhatsApp Business
 * broadcast draft.
 *
 * <p>When a merchant composes a paid marketing broadcast, the composer can
 * ask the WhatsApp server to suggest message wording. The server replies
 * with one of two payloads, selected by the
 * {@linkplain #typename() type marker}: a success payload carrying the
 * suggested {@linkplain #toneMessagePair() tone-and-message pairs} and the
 * conversational {@linkplain #followUps() follow-up suggestions}, or an
 * error payload carrying the {@linkplain #errorMessage() error message} and
 * {@linkplain #errorCode() error code} the composer surfaces to the
 * merchant.
 *
 * <p>This model is that recommendation exactly as the server reports it.
 * Cobalt flattens both payloads into one record and leaves the fields of
 * the inactive branch empty so callers can read either side without a
 * branch on the type marker.
 */
@ProtobufMessage(name = "BusinessBroadcastGenAiRecommendation")
public final class BusinessBroadcastGenAiRecommendation {
    /**
     * Server-defined type marker selecting which payload is populated. The
     * full marker set is not recoverable from the WhatsApp client, so the
     * raw marker is exposed as a string. {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String typename;

    /**
     * Suggested tone-and-message pairs returned on the success payload, or
     * {@code null} when the server omitted them (typically on the error
     * payload).
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final List<ToneMessagePair> toneMessagePair;

    /**
     * Conversational follow-up suggestions returned on the success payload,
     * or {@code null} when the server omitted them (typically on the error
     * payload).
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final List<String> followUps;

    /**
     * Error message returned on the error payload, or {@code null} when
     * the server omitted it (typically on the success payload).
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String errorMessage;

    /**
     * Error code returned on the error payload, or {@code null} when the
     * server omitted it (typically on the success payload).
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String errorCode;

    /**
     * Constructs a new {@code BusinessBroadcastGenAiRecommendation}. Any
     * reference argument may be {@code null} when the server omitted the
     * corresponding field, in particular all fields of the inactive
     * payload branch.
     *
     * @param typename        the type marker, or {@code null}
     * @param toneMessagePair the suggested tone-and-message pairs, or {@code null}
     * @param followUps       the follow-up suggestions, or {@code null}
     * @param errorMessage    the error message, or {@code null}
     * @param errorCode       the error code, or {@code null}
     */
    BusinessBroadcastGenAiRecommendation(String typename, List<ToneMessagePair> toneMessagePair,
                                         List<String> followUps, String errorMessage, String errorCode) {
        this.typename = typename;
        this.toneMessagePair = toneMessagePair;
        this.followUps = followUps;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    /**
     * Returns the server-defined type marker selecting which payload is
     * populated.
     *
     * @return the type marker, or empty when the server omitted it
     */
    public Optional<String> typename() {
        return Optional.ofNullable(typename);
    }

    /**
     * Returns an unmodifiable view of the suggested tone-and-message pairs.
     *
     * @return the suggested pairs, empty when the server reported none
     */
    public List<ToneMessagePair> toneMessagePair() {
        return toneMessagePair == null ? List.of() : Collections.unmodifiableList(toneMessagePair);
    }

    /**
     * Returns an unmodifiable view of the conversational follow-up
     * suggestions.
     *
     * @return the follow-up suggestions, empty when the server reported
     *         none
     */
    public List<String> followUps() {
        return followUps == null ? List.of() : Collections.unmodifiableList(followUps);
    }

    /**
     * Returns the error message returned on the error payload.
     *
     * @return the error message, or empty when the server omitted it
     */
    public Optional<String> errorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    /**
     * Returns the error code returned on the error payload.
     *
     * @return the error code, or empty when the server omitted it
     */
    public Optional<String> errorCode() {
        return Optional.ofNullable(errorCode);
    }

    /**
     * Returns a hash code derived from this recommendation's type marker
     * and error fields.
     *
     * @return the hash code of the type marker and error fields
     */
    @Override
    public int hashCode() {
        return Objects.hash(typename, errorMessage, errorCode);
    }

    /**
     * Returns whether this recommendation is equal to the given object.
     *
     * <p>Two recommendations are considered equal when they share the same
     * type marker and the same error message and error code.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is a
     *         {@code BusinessBroadcastGenAiRecommendation} with the same
     *         identifying fields
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof BusinessBroadcastGenAiRecommendation that
                && Objects.equals(this.typename, that.typename)
                && Objects.equals(this.errorMessage, that.errorMessage)
                && Objects.equals(this.errorCode, that.errorCode);
    }

    /**
     * Returns a debug string describing this recommendation.
     *
     * @return a debug string
     */
    @Override
    public String toString() {
        return "BusinessBroadcastGenAiRecommendation[" +
                "typename=" + typename +
                ", toneMessagePair=" + toneMessagePair +
                ", followUps=" + followUps +
                ", errorMessage=" + errorMessage +
                ", errorCode=" + errorCode +
                "]";
    }

    /**
     * Tone-and-message pair suggested by the generative-AI recommendation.
     *
     * <p>Pairs a {@linkplain #tone() tone label} with the suggested
     * {@linkplain #message() message text} written in that tone. The
     * composer renders these as picker chips so the merchant can choose a
     * suggestion to seed the draft with.
     */
    @ProtobufMessage(name = "BusinessBroadcastGenAiRecommendation.ToneMessagePair")
    public static final class ToneMessagePair {
        /**
         * Server-defined tone label for the suggestion. The full label set
         * is not recoverable from the WhatsApp client, so the raw label is
         * exposed as a string. {@code null} when the server omitted it.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String tone;

        /**
         * Suggested message text written in the paired tone, or
         * {@code null} when the server omitted it.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        final String message;

        /**
         * Constructs a new {@code ToneMessagePair}. Any reference argument
         * may be {@code null} when the server omitted the corresponding
         * field.
         *
         * @param tone    the tone label, or {@code null}
         * @param message the message text, or {@code null}
         */
        ToneMessagePair(String tone, String message) {
            this.tone = tone;
            this.message = message;
        }

        /**
         * Returns the tone label for the suggestion.
         *
         * @return the tone label, or empty when the server omitted it
         */
        public Optional<String> tone() {
            return Optional.ofNullable(tone);
        }

        /**
         * Returns the suggested message text.
         *
         * @return the message text, or empty when the server omitted it
         */
        public Optional<String> message() {
            return Optional.ofNullable(message);
        }
    }
}
