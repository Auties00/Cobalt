package com.github.auties00.cobalt.wire.stanza.smax.support;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;

import java.util.Objects;
import java.util.Optional;

/**
 * Models the two documented replies to a {@link SmaxIndividualReportRequest}.
 *
 * <p>The sealed family enumerates the accepted-report outcome ({@link Success}, carrying the
 * optional follow-up report id) and the rejected-report outcome ({@link Error}, carrying the
 * relay's code and text for banner rendering).
 *
 * @implNote
 * This implementation parses {@link Success} first, then {@link Error}. An inbound stanza fitting
 * neither shape yields an empty {@link Optional} rather than raising a parse failure.
 */
public sealed interface SmaxIndividualReportResponse extends SmaxStanza.Response
        permits SmaxIndividualReportResponse.Success, SmaxIndividualReportResponse.Error {

    /**
     * Parses the inbound individual-report reply against each variant and returns the first that
     * matches.
     *
     * <p>An empty result means the inbound stanza did not fit either documented shape.
     *
     * @implNote
     * This implementation tries {@link Success#of(Stanza, Stanza)} first and falls back to
     * {@link Error#of(Stanza, Stanza)}; no parse exception is raised on a total miss.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the originating outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty on no-match
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxSpamIndividualReportRPC",
            exports = "sendIndividualReportRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxIndividualReportResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        return Error.of(stanza, request);
    }

    /**
     * Carries the optional opaque report id of an accepted individual report.
     *
     * <p>{@link #reportId()} is empty when the relay did not assign a follow-up id.
     *
     * @implNote
     * This implementation reads the id from the {@code <report id>} attribute.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInSpamIndividualReportResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInSpamReportIdMixin")
    final class Success implements SmaxIndividualReportResponse {
        /**
         * Holds the optional opaque report id from the {@code <report id>} attribute, or
         * {@code null} when the relay omitted it.
         */
        private final String reportId;

        /**
         * Constructs an accepted-report reply from the parsed fields.
         *
         * @param reportId the optional report id; may be {@code null}
         */
        public Success(String reportId) {
            this.reportId = reportId;
        }

        /**
         * Returns the optional opaque report id.
         *
         * <p>Empty when the relay did not assign a follow-up id.
         *
         * @return an {@link Optional} carrying the id, or empty when omitted
         */
        public Optional<String> reportId() {
            return Optional.ofNullable(reportId);
        }

        /**
         * Tries to parse an inbound stanza as a {@link Success}.
         *
         * <p>Returns empty when the IQ envelope does not match a result for {@code request}.
         *
         * @implNote
         * This implementation defers IQ-envelope validation to
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)} then reads {@code <report id>}
         * via {@link Stanza#getChild(String)} chained with {@link Stanza#getAttributeAsString(String)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the originating outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on schema mismatch
         */
        @WhatsAppWebExport(moduleName = "WASmaxInSpamIndividualReportResponseSuccess",
                exports = "parseIndividualReportResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var reportId = stanza.getChild("report")
                    .flatMap(child -> child.getAttributeAsString("id"))
                    .orElse(null);
            return Optional.of(new Success(reportId));
        }

        /**
         * Compares this variant to another for value equality.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal {@link Success}
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
            return Objects.equals(this.reportId, that.reportId);
        }

        /**
         * Returns a hash code derived from the report id.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(reportId);
        }

        /**
         * Returns a debug string carrying the report id.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxIndividualReportResponse.Success[reportId=" + reportId + ']';
        }
    }

    /**
     * Carries the relay's {@code (code, text)} error pair of a rejected individual report.
     *
     * <p>The caller renders an error banner. The raw pair is forwarded so the caller can map the
     * documented combinations to UI strings.
     *
     * @implNote
     * This implementation delegates IQ-envelope and {@code <error/>} extraction to
     * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} (4xx) and
     * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} (5xx).
     */
    @WhatsAppWebModule(moduleName = "WASmaxInSpamIndividualReportResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInSpamSpamIqErrors")
    final class Error implements SmaxIndividualReportResponse {
        /**
         * Holds the numeric error code from the {@code <error/>} envelope.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text from {@code <error text="..."/>}, or
         * {@code null} when the envelope omitted it.
         */
        private final String errorText;

        /**
         * Constructs a rejected-report reply from the parsed fields.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional error text; may be {@code null}
         */
        public Error(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code classifying the rejection.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional error text.
         *
         * <p>Empty when the {@code <error/>} envelope omitted the {@code text} attribute.
         *
         * @return an {@link Optional} carrying the text, or empty when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse an inbound stanza as an {@link Error}.
         *
         * <p>Returns empty when neither the 4xx nor 5xx envelope matched.
         *
         * @implNote
         * This implementation tries {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}
         * first and falls back to {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)};
         * the first match wins.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the originating outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on schema mismatch
         */
        @WhatsAppWebExport(moduleName = "WASmaxInSpamIndividualReportResponseError",
                exports = "parseIndividualReportResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Error> of(Stanza stanza, Stanza request) {
            var clientError = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (clientError != null) {
                return Optional.of(new Error(clientError.code(), clientError.text()));
            }
            var serverError = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (serverError != null) {
                return Optional.of(new Error(serverError.code(), serverError.text()));
            }
            return Optional.empty();
        }

        /**
         * Compares this variant to another for value equality across all fields.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal {@link Error}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Error) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash code derived from all fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string listing every field.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxIndividualReportResponse.Error[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
