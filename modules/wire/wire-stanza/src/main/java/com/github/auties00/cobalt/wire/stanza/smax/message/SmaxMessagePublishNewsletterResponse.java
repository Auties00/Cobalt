package com.github.auties00.cobalt.wire.stanza.smax.message;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Closes the set of replies the relay produces for a {@link SmaxMessagePublishNewsletterRequest}.
 *
 * <p>A publish resolves to exactly one variant: {@link Success} acknowledges the publish and hands
 * callers the relay-assigned server id of a brand-new post or the response-server-id of a
 * question-response, while {@link Negative} carries the rejection code and optional retry backoff so
 * callers can schedule a retry per the {@code backoff} hint.
 */
public sealed interface SmaxMessagePublishNewsletterResponse extends SmaxStanza.Response
        permits SmaxMessagePublishNewsletterResponse.Success, SmaxMessagePublishNewsletterResponse.Negative {

    /**
     * Resolves an inbound ack into the first matching response variant in negative-then-success
     * priority.
     *
     * <p>The negative arm is tried first because it is distinguished by the presence of an
     * {@code error} attribute that success acks omit; resolution falls through to {@link Success} when
     * no negative variant matches.
     *
     * @implNote
     * This implementation mirrors the WhatsApp Web priority order in {@code sendNewsletterRPC}: try
     * negative first, then fall through to success.
     *
     * @param stanza    the inbound ack stanza; never {@code null}
     * @param request the originating outbound message stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no
     *         documented variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxMessagePublishNewsletterRPC",
            exports = "sendNewsletterRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxMessagePublishNewsletterResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var negative = Negative.of(stanza, request);
        if (negative.isPresent()) {
            return negative;
        }
        return Success.of(stanza, request);
    }

    /**
     * Validates the {@code <ack class="message">} envelope by cross-checking the description, class,
     * id, from, and timestamp attributes against the originating request.
     *
     * <p>This gate is invoked by {@link Success#of(Stanza, Stanza)} and {@link Negative#of(Stanza, Stanza)}
     * before any payload inspection, so a failed envelope short-circuits parsing.
     *
     * @implNote
     * This implementation lets the {@code from} echo check succeed when the request lacked a
     * {@code to} attribute, mirroring WhatsApp Web's {@code parseAckMixin}.
     *
     * @param reply   the inbound ack stanza
     * @param request the originating outbound message stanza
     * @return {@code true} when the envelope passes every echo check
     */
    @WhatsAppWebExport(moduleName = "WASmaxInMessagePublishAckMixin",
            exports = "parseAckMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    private static boolean validateAckEnvelope(Stanza reply, Stanza request) {
        if (!reply.hasDescription("ack")) {
            return false;
        }
        if (!reply.hasAttribute("class", "message")) {
            return false;
        }
        var requestId = request.getAttributeAsString("id").orElse(null);
        if (requestId == null) {
            return false;
        }
        if (!reply.hasAttribute("id", requestId)) {
            return false;
        }
        var requestTo = request.getAttributeAsString("to").orElse(null);
        if (requestTo != null && !reply.hasAttribute("from", requestTo)) {
            return false;
        }
        var tOpt = reply.getAttributeAsLong("t");
        if (tOpt.isEmpty()) {
            return false;
        }
        return tOpt.getAsLong() >= 0;
    }

    /**
     * Represents the positive reply carrying the assigned server id (or response-server-id for
     * question-response publishes) and the ack timestamp.
     *
     * <p>This variant is produced when the relay landed the publish; {@link #variantName()} tells
     * callers whether it carries a {@link #serverId()} (brand-new post) or a
     * {@link #responseServerId()} (question-response).
     */
    @WhatsAppWebModule(moduleName = "WASmaxInMessagePublishNewsletterResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInMessagePublishNewsletterQuestionResponseOrNewsletterMessageAckMixinGroup")
    final class Success implements SmaxMessagePublishNewsletterResponse {
        /**
         * Holds the disjunction variant name, either {@code "QuestionResponseAck"} or
         * {@code "MessageAck"}.
         */
        private final String variantName;

        /**
         * Holds the optional relay-assigned message server id, set only on {@code "MessageAck"}
         * brand-new-post publishes; {@code null} otherwise.
         */
        private final Long serverId;

        /**
         * Holds the optional relay-assigned response server id, set only on
         * {@code "QuestionResponseAck"} publishes; {@code null} otherwise.
         */
        private final String responseServerId;

        /**
         * Holds the unix-second timestamp of the ack.
         */
        private final long timestamp;

        /**
         * Constructs a successful reply from already-parsed fields.
         *
         * <p>This constructor is invoked by {@link #of(Stanza, Stanza)} after a successful parse.
         *
         * @param variantName      the variant name; never {@code null}
         * @param serverId         the optional message server id; may be {@code null}
         * @param responseServerId the optional response server id; may be {@code null}
         * @param timestamp        the unix-second timestamp
         * @throws NullPointerException if {@code variantName} is {@code null}
         */
        public Success(String variantName, Long serverId, String responseServerId, long timestamp) {
            this.variantName = Objects.requireNonNull(variantName, "variantName cannot be null");
            this.serverId = serverId;
            this.responseServerId = responseServerId;
            this.timestamp = timestamp;
        }

        /**
         * Returns the disjunction variant name so callers can branch on a single field rather than
         * checking which of {@link #serverId()} or {@link #responseServerId()} is present.
         *
         * @return the name; never {@code null}
         */
        public String variantName() {
            return variantName;
        }

        /**
         * Returns the optional relay-assigned message server id, present only on {@code "MessageAck"}
         * variants for brand-new posts where the relay uses it as the stable key for the newly landed
         * message.
         *
         * @return an {@link Optional} carrying the id, or {@link Optional#empty()} when omitted
         */
        public Optional<Long> serverId() {
            return Optional.ofNullable(serverId);
        }

        /**
         * Returns the optional relay-assigned response server id, present only on
         * {@code "QuestionResponseAck"} variants where the relay uses it to correlate the response back
         * to its originating question.
         *
         * @return an {@link Optional} carrying the id, or {@link Optional#empty()} when omitted
         */
        public Optional<String> responseServerId() {
            return Optional.ofNullable(responseServerId);
        }

        /**
         * Returns the unix-second timestamp of the ack.
         *
         * @return the timestamp
         */
        public long timestamp() {
            return timestamp;
        }

        /**
         * Parses a {@code Success} reply from the given inbound stanza cross-checked against the
         * originating request.
         *
         * <p>The result is {@link Optional#empty()} for any deviation from the documented success
         * schema: a missing or malformed ack envelope, an out-of-range server id, or a negative
         * timestamp.
         *
         * @implNote
         * This implementation reads the {@code response_server_id} attribute first; a present value
         * pins the variant to {@code "QuestionResponseAck"} and leaves {@code serverId} unset.
         * Otherwise the parser pins {@code "MessageAck"}, optionally reading the {@code server_id}
         * attribute under the WhatsApp Web range bound ({@code 99..2147476647}).
         *
         * @param stanza    the inbound ack stanza
         * @param request the originating outbound message stanza
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInMessagePublishNewsletterResponseSuccess",
                exports = "parseNewsletterResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!validateAckEnvelope(stanza, request)) {
                return Optional.empty();
            }
            var timestamp = stanza.getAttributeAsLong("t").orElse(-1);
            if (timestamp < 0) {
                return Optional.empty();
            }
            var responseServerId = stanza.getAttributeAsString("response_server_id").orElse(null);
            if (responseServerId != null) {
                return Optional.of(new Success("QuestionResponseAck", null, responseServerId, timestamp));
            }
            Long serverId = null;
            var serverIdOpt = stanza.getAttributeAsLong("server_id");
            if (serverIdOpt.isPresent()) {
                var sv = serverIdOpt.getAsLong();
                if (sv < 99 || sv > 2147476647L) {
                    return Optional.empty();
                }
                serverId = sv;
            }
            return Optional.of(new Success("MessageAck", serverId, null, timestamp));
        }

        /**
         * Compares this success reply to another object for value equality across every field.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link Success} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Success) obj;
            return this.timestamp == that.timestamp
                    && Objects.equals(this.variantName, that.variantName)
                    && Objects.equals(this.serverId, that.serverId)
                    && Objects.equals(this.responseServerId, that.responseServerId);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(variantName, serverId, responseServerId, timestamp);
        }

        /**
         * Returns a debug-friendly representation of this success reply.
         *
         * <p>The format is intended for logging and is not part of any stable contract.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxMessagePublishNewsletterResponse.Success[variantName=" + variantName
                    + ", serverId=" + serverId
                    + ", responseServerId=" + responseServerId
                    + ", timestamp=" + timestamp + ']';
        }
    }

    /**
     * Represents the negative reply carrying the relay's rejection code and optional
     * application-error and retry-backoff hints.
     *
     * <p>This variant is produced when the relay rejected the publish; callers can schedule a retry
     * after the {@link #backoff()} value (bounded to {@code 0..86400} seconds) when it is set.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInMessagePublishNewsletterResponseNegative")
    @WhatsAppWebModule(moduleName = "WASmaxInMessagePublishNegativeAckMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInMessagePublishApplicationNegativeAckMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInMessagePublishMessageNackRetryAttributesMixin")
    final class Negative implements SmaxMessagePublishNewsletterResponse {
        /**
         * Holds the relay-supplied error code carried as the {@code error} attribute on the ack.
         */
        private final String errorCode;

        /**
         * Holds the optional application-level error integer; {@code null} when omitted.
         */
        private final Integer applicationError;

        /**
         * Holds the optional retry-backoff hint in seconds, bounded to {@code 0..86400}; {@code null}
         * when omitted.
         */
        private final Integer backoff;

        /**
         * Holds the unix-second timestamp of the ack.
         */
        private final long timestamp;

        /**
         * Constructs a negative reply from already-parsed fields.
         *
         * <p>This constructor is invoked by {@link #of(Stanza, Stanza)} after a successful parse.
         *
         * @param errorCode        the error code; never {@code null}
         * @param applicationError the optional application error; may be {@code null}
         * @param backoff          the optional retry backoff; may be {@code null}
         * @param timestamp        the unix-second timestamp
         * @throws NullPointerException if {@code errorCode} is {@code null}
         */
        public Negative(String errorCode, Integer applicationError, Integer backoff, long timestamp) {
            this.errorCode = Objects.requireNonNull(errorCode, "errorCode cannot be null");
            this.applicationError = applicationError;
            this.backoff = backoff;
            this.timestamp = timestamp;
        }

        /**
         * Returns the error code.
         *
         * @return the code; never {@code null}
         */
        public String errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional application-level error integer.
         *
         * @return an {@link Optional} carrying the error, or {@link Optional#empty()} when omitted
         */
        public Optional<Integer> applicationError() {
            return Optional.ofNullable(applicationError);
        }

        /**
         * Returns the optional retry-backoff hint in seconds, bounded to {@code 0..86400}, after which
         * callers may schedule a retry.
         *
         * @return an {@link Optional} carrying the backoff, or {@link Optional#empty()} when omitted
         */
        public Optional<Integer> backoff() {
            return Optional.ofNullable(backoff);
        }

        /**
         * Returns the unix-second timestamp of the ack.
         *
         * @return the timestamp
         */
        public long timestamp() {
            return timestamp;
        }

        /**
         * Parses a {@code Negative} reply from the given inbound stanza cross-checked against the
         * originating request.
         *
         * <p>The result is {@link Optional#empty()} for any deviation from the documented negative-ack
         * schema: a missing or malformed envelope, a missing {@code error} attribute, a negative
         * timestamp or application-error, or an out-of-range backoff.
         *
         * @implNote
         * This implementation enforces the WhatsApp Web range bounds on application-error
         * ({@code >= 0}) and backoff ({@code 0..86400}) inline rather than through a separate
         * validator.
         *
         * @param stanza    the inbound ack stanza
         * @param request the originating outbound message stanza
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInMessagePublishNewsletterResponseNegative",
                exports = "parseNewsletterResponseNegative",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Negative> of(Stanza stanza, Stanza request) {
            if (!validateAckEnvelope(stanza, request)) {
                return Optional.empty();
            }
            var errorCode = stanza.getAttributeAsString("error").orElse(null);
            if (errorCode == null) {
                return Optional.empty();
            }
            var timestamp = stanza.getAttributeAsLong("t").orElse(-1);
            if (timestamp < 0) {
                return Optional.empty();
            }
            Integer applicationError = null;
            var appErrOpt = stanza.getAttributeAsInt("application_error");
            if (appErrOpt.isPresent()) {
                var av = appErrOpt.getAsInt();
                if (av < 0) {
                    return Optional.empty();
                }
                applicationError = av;
            }
            Integer backoff = null;
            var backoffOpt = stanza.getAttributeAsInt("backoff");
            if (backoffOpt.isPresent()) {
                var bv = backoffOpt.getAsInt();
                if (bv < 0 || bv > 86400) {
                    return Optional.empty();
                }
                backoff = bv;
            }
            return Optional.of(new Negative(errorCode, applicationError, backoff, timestamp));
        }

        /**
         * Compares this negative reply to another object for value equality across every field.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link Negative} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Negative) obj;
            return this.timestamp == that.timestamp
                    && Objects.equals(this.errorCode, that.errorCode)
                    && Objects.equals(this.applicationError, that.applicationError)
                    && Objects.equals(this.backoff, that.backoff);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, applicationError, backoff, timestamp);
        }

        /**
         * Returns a debug-friendly representation of this negative reply.
         *
         * <p>The format is intended for logging and is not part of any stable contract.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxMessagePublishNewsletterResponse.Negative[errorCode=" + errorCode
                    + ", applicationError=" + applicationError
                    + ", backoff=" + backoff
                    + ", timestamp=" + timestamp + ']';
        }
    }
}
