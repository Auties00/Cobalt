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
 * Models the inbound reply to a {@link SmaxStatusReportRequest}.
 * <p>
 * This is a sealed family with exactly two variants: {@link Success} acknowledges that the
 * report was accepted and carries the optional follow-up report id, while {@link Error}
 * carries the relay's {@code (code, text)} rejection pair. The {@link #of(Stanza, Stanza)} factory
 * selects the matching variant for an inbound {@code spam} IQ stanza.
 *
 * @implNote
 * This implementation matches WA Web's parser order by trying {@link Success} first and
 * {@link Error} second. An inbound stanza that fits neither shape yields an empty
 * {@link Optional} rather than raising a parse failure.
 */
public sealed interface SmaxStatusReportResponse extends SmaxStanza.Response
        permits SmaxStatusReportResponse.Success, SmaxStatusReportResponse.Error {

    /**
     * Parses the inbound status-report reply and returns the first matching variant.
     * <p>
     * Callers invoke this once the relay's IQ arrives in response to a
     * {@link SmaxStatusReportRequest}. An empty {@link Optional} means the inbound stanza did
     * not fit either of the two documented shapes.
     *
     * @implNote
     * This implementation tries {@link Success#of(Stanza, Stanza)} first and falls back to
     * {@link Error#of(Stanza, Stanza)}; no parse exception is raised when both miss.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the originating outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when neither matches
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxSpamStatusReportRPC",
            exports = "sendStatusReportRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxStatusReportResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        return Error.of(stanza, request);
    }

    /**
     * Models the accepted-report outcome carrying the optional opaque report id.
     * <p>
     * This variant represents the relay's acknowledgement that the report was queued.
     * {@link #reportId()} is empty when the relay did not assign a follow-up id.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInSpamStatusReportResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInSpamReportIdMixin")
    final class Success implements SmaxStatusReportResponse {
        /**
         * Holds the optional opaque report id assigned by the relay.
         * <p>
         * This is the value of the {@code <report id>} attribute, or {@code null} when the
         * relay omitted it.
         */
        private final String reportId;

        /**
         * Constructs an accepted-report reply from the parsed report id.
         *
         * @param reportId the optional report id; may be {@code null}
         */
        public Success(String reportId) {
            this.reportId = reportId;
        }

        /**
         * Returns the optional opaque report id.
         * <p>
         * The result is empty when the relay did not assign a follow-up id.
         *
         * @return an {@link Optional} carrying the id, or empty when omitted
         */
        public Optional<String> reportId() {
            return Optional.ofNullable(reportId);
        }

        /**
         * Tries to parse an inbound stanza as a {@link Success}.
         * <p>
         * Returns empty when the IQ envelope does not validate as a result for {@code request}.
         *
         * @implNote
         * This implementation defers IQ-envelope validation to
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)} and then reads
         * {@code <report id>} via {@link Stanza#getChild(String)} chained with
         * {@link Stanza#getAttributeAsString(String)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the originating outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on schema mismatch
         */
        @WhatsAppWebExport(moduleName = "WASmaxInSpamStatusReportResponseSuccess",
                exports = "parseStatusReportResponseSuccess",
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
         * Compares this reply to another for equality by report id.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link Success} with an equal report id
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
            return "SmaxStatusReportResponse.Success[reportId=" + reportId + ']';
        }
    }

    /**
     * Models the rejected-report outcome carrying the relay's {@code (code, text)} error pair.
     * <p>
     * This variant represents the relay's refusal of the report. The numeric code and optional
     * text are surfaced verbatim so the caller can render an error banner.
     *
     * @implNote
     * This implementation forwards the raw {@code (code, text)} pair rather than mapping the
     * documented combinations to UI strings as WA Web does, leaving any such mapping to the
     * caller. Extraction is delegated to {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}
     * for the 4xx range and {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} for
     * the 5xx range.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInSpamStatusReportResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInSpamSpamIqErrors")
    final class Error implements SmaxStatusReportResponse {
        /**
         * Holds the numeric error code from the {@code <error/>} envelope.
         * <p>
         * This is the relay's classification of the rejection.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text.
         * <p>
         * This is the paired text from {@code <error text="..."/>}, or {@code null} when the
         * envelope omitted it.
         */
        private final String errorText;

        /**
         * Constructs a rejected-report reply from the parsed error code and text.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional error text; may be {@code null}
         */
        public Error(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         * <p>
         * This is the relay's classification of the rejection.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional error text.
         * <p>
         * The result is empty when the {@code <error/>} envelope omitted the {@code text}
         * attribute.
         *
         * @return an {@link Optional} carrying the text, or empty when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse an inbound stanza as an {@link Error}.
         * <p>
         * Returns empty when neither the 4xx nor the 5xx envelope matched.
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
        @WhatsAppWebExport(moduleName = "WASmaxInSpamStatusReportResponseError",
                exports = "parseStatusReportResponseError",
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
         * Compares this reply to another for equality by error code and text.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is an {@link Error} with equal code and text
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
         * Returns a hash code derived from the error code and text.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying the error code and text.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxStatusReportResponse.Error[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
