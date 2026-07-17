package com.github.auties00.cobalt.wire.stanza.smax.support;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;

import java.util.Objects;
import java.util.Optional;

/**
 * Models the three documented replies to a {@link SmaxContactFormRequest}.
 *
 * <p>The sealed family enumerates the accepted-submission outcome
 * ({@link ContactFormResponseSuccess}), the retryable back-pressure outcome
 * ({@link ContactFormResponseRetryableError}) and the final-rejection outcome
 * ({@link ContactFormResponseError}). The accepted variant surfaces the acknowledgement text,
 * ticket id and routing group JID; the two error variants surface a numeric error code so the
 * caller can re-render the form or banner an error.
 *
 * @implNote
 * This implementation parses in the order {@link ContactFormResponseSuccess},
 * {@link ContactFormResponseRetryableError}, {@link ContactFormResponseError}, the first match
 * wins. An inbound stanza matching none of the three yields an empty {@link Optional} rather than
 * raising a parse failure; the caller surfaces the miss.
 */
public sealed interface SmaxContactFormResponse extends SmaxStanza.Response
        permits SmaxContactFormResponse.ContactFormResponseSuccess,
        SmaxContactFormResponse.ContactFormResponseRetryableError,
        SmaxContactFormResponse.ContactFormResponseError {

    /**
     * Parses the inbound contact-form reply against each variant and returns the first that
     * matches.
     *
     * <p>An empty result means the inbound stanza did not fit any of the three documented shapes.
     *
     * @implNote
     * This implementation short-circuits across {@link ContactFormResponseSuccess#of(Stanza, Stanza)},
     * {@link ContactFormResponseRetryableError#of(Stanza, Stanza)} and
     * {@link ContactFormResponseError#of(Stanza, Stanza)}; no parse exception is raised on a total
     * miss.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the originating outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty on no-match
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxSupportContactFormRPC",
            exports = "sendContactFormRPC",
            adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxContactFormResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = ContactFormResponseSuccess.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        var retryableError = ContactFormResponseRetryableError.of(stanza, request);
        if (retryableError.isPresent()) {
            return retryableError;
        }
        return ContactFormResponseError.of(stanza, request);
    }

    /**
     * Carries the acknowledgement text, ticket id and routing group JID of an accepted
     * contact-form submission.
     *
     * <p>The {@code <iq type="result">} envelope's id equals the originating request id, and its
     * {@code <response status="ok">} child supplies the {@code <message/>}, {@code <ticket_id/>}
     * and {@code <group_jid/>} text children surfaced here.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInSupportContactFormResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInSupportHackBaseIQResultResponseMixin")
    final class ContactFormResponseSuccess implements SmaxContactFormResponse {
        /**
         * Holds the literal {@code "ok"} status echoed by the {@code <response/>} child.
         */
        private final String responseStatus;

        /**
         * Holds the acknowledgement message body shown to the user, the text of the
         * {@code <message/>} child.
         */
        private final String responseMessageElementValue;

        /**
         * Holds the opaque ticket id assigned for follow-up correspondence, the text of the
         * {@code <ticket_id/>} child.
         */
        private final String responseTicketIdElementValue;

        /**
         * Holds the routing group JID the support backend assigned, the text of the
         * {@code <group_jid/>} child.
         */
        private final String responseGroupJidElementValue;

        /**
         * Constructs a successful-submission reply from the parsed fields.
         *
         * @param responseStatus               the response status; never {@code null}
         * @param responseMessageElementValue  the acknowledgement text; never {@code null}
         * @param responseTicketIdElementValue the ticket id; never {@code null}
         * @param responseGroupJidElementValue the routing group JID; never {@code null}
         * @throws NullPointerException if any argument is {@code null}
         */
        public ContactFormResponseSuccess(String responseStatus,
                                          String responseMessageElementValue,
                                          String responseTicketIdElementValue,
                                          String responseGroupJidElementValue) {
            this.responseStatus = Objects.requireNonNull(responseStatus, "responseStatus cannot be null");
            this.responseMessageElementValue = Objects.requireNonNull(responseMessageElementValue,
                    "responseMessageElementValue cannot be null");
            this.responseTicketIdElementValue = Objects.requireNonNull(responseTicketIdElementValue,
                    "responseTicketIdElementValue cannot be null");
            this.responseGroupJidElementValue = Objects.requireNonNull(responseGroupJidElementValue,
                    "responseGroupJidElementValue cannot be null");
        }

        /**
         * Returns the response status, always {@code "ok"} for this variant.
         *
         * @return the status; never {@code null}
         */
        public String responseStatus() {
            return responseStatus;
        }

        /**
         * Returns the acknowledgement text rendered into the submit-success banner.
         *
         * @return the text; never {@code null}
         */
        public String responseMessageElementValue() {
            return responseMessageElementValue;
        }

        /**
         * Returns the ticket id used for subsequent follow-up correspondence.
         *
         * @return the id; never {@code null}
         */
        public String responseTicketIdElementValue() {
            return responseTicketIdElementValue;
        }

        /**
         * Returns the routing group JID the user can post follow-up messages to.
         *
         * @return the JID; never {@code null}
         */
        public String responseGroupJidElementValue() {
            return responseGroupJidElementValue;
        }

        /**
         * Tries to parse an inbound stanza as a {@link ContactFormResponseSuccess}.
         *
         * <p>Returns empty when any structural precondition fails.
         *
         * @implNote
         * This implementation validates the IQ envelope ({@code description="iq"},
         * {@code type="result"}, matching {@code id}) then descends through
         * {@code <response status="ok">} into {@code <message>}, {@code <ticket_id>} and
         * {@code <group_jid>}; any missing or empty child yields an empty {@link Optional}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the originating outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on schema mismatch
         */
        @WhatsAppWebExport(moduleName = "WASmaxInSupportContactFormResponseSuccess",
                exports = "parseContactFormResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ContactFormResponseSuccess> of(Stanza stanza, Stanza request) {
            if (!stanza.hasDescription("iq")) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("type", "result")) {
                return Optional.empty();
            }
            var requestId = request.getAttributeAsString("id").orElse(null);
            if (requestId == null || !stanza.hasAttribute("id", requestId)) {
                return Optional.empty();
            }
            var responseChild = stanza.getChild("response").orElse(null);
            if (responseChild == null) {
                return Optional.empty();
            }
            if (!responseChild.hasAttribute("status", "ok")) {
                return Optional.empty();
            }
            var messageNode = responseChild.getChild("message").orElse(null);
            if (messageNode == null) {
                return Optional.empty();
            }
            var messageValue = messageNode.toContentString().orElse(null);
            if (messageValue == null) {
                return Optional.empty();
            }
            var ticketIdNode = responseChild.getChild("ticket_id").orElse(null);
            if (ticketIdNode == null) {
                return Optional.empty();
            }
            var ticketIdValue = ticketIdNode.toContentString().orElse(null);
            if (ticketIdValue == null) {
                return Optional.empty();
            }
            var groupJidNode = responseChild.getChild("group_jid").orElse(null);
            if (groupJidNode == null) {
                return Optional.empty();
            }
            var groupJidValue = groupJidNode.toContentString().orElse(null);
            if (groupJidValue == null) {
                return Optional.empty();
            }
            return Optional.of(new ContactFormResponseSuccess("ok", messageValue,
                    ticketIdValue, groupJidValue));
        }

        /**
         * Compares this variant to another for value equality across all fields.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal {@link ContactFormResponseSuccess}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ContactFormResponseSuccess) obj;
            return Objects.equals(this.responseStatus, that.responseStatus)
                    && Objects.equals(this.responseMessageElementValue, that.responseMessageElementValue)
                    && Objects.equals(this.responseTicketIdElementValue, that.responseTicketIdElementValue)
                    && Objects.equals(this.responseGroupJidElementValue, that.responseGroupJidElementValue);
        }

        /**
         * Returns a hash code derived from all fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(responseStatus, responseMessageElementValue,
                    responseTicketIdElementValue, responseGroupJidElementValue);
        }

        /**
         * Returns a debug string listing every field.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxContactFormResponse.ContactFormResponseSuccess[responseStatus="
                    + responseStatus
                    + ", responseMessageElementValue=" + responseMessageElementValue
                    + ", responseTicketIdElementValue=" + responseTicketIdElementValue
                    + ", responseGroupJidElementValue=" + responseGroupJidElementValue + ']';
        }
    }

    /**
     * Carries a transient relay refusal with a numeric error code and an optional retry-after
     * timestamp.
     *
     * <p>The caller is expected to schedule a retry no earlier than {@link #responseNextRetryTs()}
     * (epoch seconds) when that value is present.
     *
     * @implNote
     * This implementation accepts any numeric {@code error_code} attribute on the
     * {@code <response>} child; the code value is not validated at parse time.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInSupportContactFormResponseRetryableError")
    final class ContactFormResponseRetryableError implements SmaxContactFormResponse {
        /**
         * Holds the {@code error_code} attribute echoed by the {@code <response/>} child.
         */
        private final int responseErrorCode;

        /**
         * Holds the optional {@code next_retry_ts} attribute (Unix epoch seconds, text-encoded),
         * or {@code null} when the relay does not pin a retry floor.
         */
        private final String responseNextRetryTs;

        /**
         * Constructs a retryable-error reply from the parsed fields.
         *
         * @param responseErrorCode   the error code echoed by the relay
         * @param responseNextRetryTs the optional retry-after timestamp; may be {@code null}
         */
        public ContactFormResponseRetryableError(int responseErrorCode, String responseNextRetryTs) {
            this.responseErrorCode = responseErrorCode;
            this.responseNextRetryTs = responseNextRetryTs;
        }

        /**
         * Returns the error code echoed by the {@code <response/>} child.
         *
         * @return the code
         */
        public int responseErrorCode() {
            return responseErrorCode;
        }

        /**
         * Returns the optional retry-after timestamp in Unix epoch seconds.
         *
         * <p>Empty when the relay did not pin a retry floor.
         *
         * @return an {@link Optional} carrying the timestamp text, or empty when omitted
         */
        public Optional<String> responseNextRetryTs() {
            return Optional.ofNullable(responseNextRetryTs);
        }

        /**
         * Tries to parse an inbound stanza as a {@link ContactFormResponseRetryableError}.
         *
         * <p>Returns empty when the IQ envelope does not match or when the {@code <response/>}
         * child lacks an {@code error_code} attribute.
         *
         * @implNote
         * This implementation validates the IQ envelope identically to
         * {@link ContactFormResponseSuccess#of(Stanza, Stanza)} then reads
         * {@code error_code}/{@code next_retry_ts} from the {@code <response/>} attributes; no
         * {@code <message/>} text body is required.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the originating outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on schema mismatch
         */
        @WhatsAppWebExport(moduleName = "WASmaxInSupportContactFormResponseRetryableError",
                exports = "parseContactFormResponseRetryableError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ContactFormResponseRetryableError> of(Stanza stanza, Stanza request) {
            if (!stanza.hasDescription("iq")) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("type", "result")) {
                return Optional.empty();
            }
            var requestId = request.getAttributeAsString("id").orElse(null);
            if (requestId == null || !stanza.hasAttribute("id", requestId)) {
                return Optional.empty();
            }
            var responseChild = stanza.getChild("response").orElse(null);
            if (responseChild == null) {
                return Optional.empty();
            }
            var errorCode = responseChild.getAttributeAsInt("error_code");
            if (errorCode.isEmpty()) {
                return Optional.empty();
            }
            var nextRetryTs = responseChild.getAttributeAsString("next_retry_ts").orElse(null);
            return Optional.of(new ContactFormResponseRetryableError(errorCode.getAsInt(), nextRetryTs));
        }

        /**
         * Compares this variant to another for value equality across all fields.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal
         *         {@link ContactFormResponseRetryableError}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ContactFormResponseRetryableError) obj;
            return this.responseErrorCode == that.responseErrorCode
                    && Objects.equals(this.responseNextRetryTs, that.responseNextRetryTs);
        }

        /**
         * Returns a hash code derived from all fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(responseErrorCode, responseNextRetryTs);
        }

        /**
         * Returns a debug string listing every field.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxContactFormResponse.ContactFormResponseRetryableError[responseErrorCode="
                    + responseErrorCode
                    + ", responseNextRetryTs=" + responseNextRetryTs + ']';
        }
    }

    /**
     * Carries a non-retryable rejection with one of three documented error codes:
     * {@code 400} bad-request, {@code 475} notice-required, or {@code 500}
     * internal-server-error.
     *
     * <p>The caller renders an error banner. For {@code 475}, {@link #tosVersion()} drives the
     * "accept new ToS" prompt that must complete before resubmission becomes possible.
     *
     * @implNote
     * This implementation projects the three mutually exclusive WA Web error mixins into a single
     * concrete class indexed by the {@code (code, text)} pair; the {@code 475} branch reads
     * {@code tos_version} from the {@code <error/>} child and clamps it to {@code [1, 65535]}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInSupportContactFormResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInSupportContactFormError")
    @WhatsAppWebModule(moduleName = "WASmaxInSupportIQErrorBadRequestMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInSupportIQErrorNoticeRequiredMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInSupportIQErrorInternalServerErrorMixin")
    final class ContactFormResponseError implements SmaxContactFormResponse {
        /**
         * Holds the numeric error code from the {@code <error/>} envelope ({@code 400},
         * {@code 475} or {@code 500}).
         */
        private final int errorCode;

        /**
         * Holds the error text paired with {@link #errorCode}
         * ({@code "bad-request"}, {@code "notice-required"} or {@code "internal-server-error"}),
         * or {@code null} after manual construction with a {@code null} value.
         */
        private final String errorText;

        /**
         * Holds the {@code tos_version} attribute carried only by the {@code 475 notice-required}
         * sub-variant, or {@code null} for the {@code 400} and {@code 500} cases.
         */
        private final Integer tosVersion;

        /**
         * Constructs an error reply from the parsed fields.
         *
         * @param errorCode  the numeric code
         * @param errorText  the optional error text; may be {@code null}
         * @param tosVersion the optional ToS version; may be {@code null}
         */
        public ContactFormResponseError(int errorCode, String errorText, Integer tosVersion) {
            this.errorCode = errorCode;
            this.errorText = errorText;
            this.tosVersion = tosVersion;
        }

        /**
         * Returns the numeric error code, one of {@code 400}, {@code 475} or {@code 500} when
         * produced by {@link #of(Stanza, Stanza)}.
         *
         * @return the code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional error text.
         *
         * <p>Empty only when a caller constructed the variant with a {@code null} text;
         * {@link #of(Stanza, Stanza)} always emits a non-{@code null} value.
         *
         * @return an {@link Optional} carrying the text, or empty when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Returns the optional {@code tos_version} carried by the {@code 475 notice-required}
         * variant.
         *
         * <p>Empty for the {@code 400} and {@code 500} cases.
         *
         * @return an {@link Optional} carrying the version, or empty when the error is not
         *         {@code notice-required}
         */
        public Optional<Integer> tosVersion() {
            return Optional.ofNullable(tosVersion);
        }

        /**
         * Tries to parse an inbound stanza as a {@link ContactFormResponseError}.
         *
         * <p>Returns empty when the {@code (code, text)} pair does not match one of the three
         * documented combinations.
         *
         * @implNote
         * This implementation delegates IQ-envelope validation and {@code <error/>} extraction to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} (4xx) and
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} (5xx). The {@code 475}
         * branch reads {@code tos_version} from the {@code <error/>} child and discards it when
         * outside {@code [1, 65535]}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the originating outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on schema mismatch
         */
        @WhatsAppWebExport(moduleName = "WASmaxInSupportContactFormResponseError",
                exports = "parseContactFormResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ContactFormResponseError> of(Stanza stanza, Stanza request) {
            var clientEnvelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            var serverEnvelope = clientEnvelope == null
                    ? SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null)
                    : null;
            var envelope = clientEnvelope != null ? clientEnvelope : serverEnvelope;
            if (envelope == null) {
                return Optional.empty();
            }
            var code = envelope.code();
            var text = envelope.text();
            Integer tosVersion = null;
            if (code == 400 && "bad-request".equals(text)) {
                // IQErrorBadRequestMixin
            } else if (code == 475 && "notice-required".equals(text)) {
                var errorChild = stanza.getChild("error").orElse(null);
                if (errorChild != null) {
                    var tos = errorChild.getAttributeAsInt("tos_version");
                    if (tos.isPresent() && tos.getAsInt() >= 1 && tos.getAsInt() <= 65535) {
                        tosVersion = tos.getAsInt();
                    }
                }
            } else if (code == 500 && "internal-server-error".equals(text)) {
                // IQErrorInternalServerErrorMixin
            } else {
                return Optional.empty();
            }
            return Optional.of(new ContactFormResponseError(code, text, tosVersion));
        }

        /**
         * Compares this variant to another for value equality across all fields.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal {@link ContactFormResponseError}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ContactFormResponseError) obj;
            return this.errorCode == that.errorCode
                    && Objects.equals(this.errorText, that.errorText)
                    && Objects.equals(this.tosVersion, that.tosVersion);
        }

        /**
         * Returns a hash code derived from all fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText, tosVersion);
        }

        /**
         * Returns a debug string listing every field.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxContactFormResponse.ContactFormResponseError[errorCode=" + errorCode
                    + ", errorText=" + errorText
                    + ", tosVersion=" + tosVersion + ']';
        }
    }
}
