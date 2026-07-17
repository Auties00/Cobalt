package com.github.auties00.cobalt.wire.stanza.iq.privacy;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.Objects;
import java.util.Optional;

/**
 * Sealed family of inbound reply variants produced by the relay for an
 * {@link IqSetPrivacyTokensRequest}.
 * <p>
 * Pattern matching against the three permitted subtypes ({@link Success}, {@link ClientError},
 * {@link ServerError}) surfaces either a token-issued acknowledgement or the error envelope. The
 * reply carries no per-token payload; the success signal is the envelope itself.
 *
 * @implNote
 * This implementation collapses WA Web's parse-and-throw flow ({@code setPrivacyTokensParser} plus
 * the {@code ServerStatusCodeError} thrown by {@code WAWebSetPrivacyTokensJob.issuePrivacyToken} on
 * a {@code 4xx} or {@code 5xx} reply) into a single sealed hierarchy.
 */
public sealed interface IqSetPrivacyTokensResponse extends IqStanza.Response
        permits IqSetPrivacyTokensResponse.Success, IqSetPrivacyTokensResponse.ClientError, IqSetPrivacyTokensResponse.ServerError {

    /**
     * Tries each {@link IqSetPrivacyTokensResponse} variant in priority order and returns the first
     * that parses cleanly.
     * <p>
     * The dispatcher calls this immediately after receiving an inbound {@code <iq>} stanza whose id
     * matches an outstanding {@link IqSetPrivacyTokensRequest}; the empty {@link Optional} indicates
     * the stanza did not match any documented schema.
     *
     * @implNote
     * This implementation tries {@link Success} first, then {@link ClientError}, then
     * {@link ServerError}.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return the parsed variant, or {@link Optional#empty()} when no documented variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSetPrivacyTokensJob",
            exports = "issuePrivacyToken", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqSetPrivacyTokensResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        var clientError = ClientError.of(stanza, request);
        if (clientError.isPresent()) {
            return clientError;
        }
        return ServerError.of(stanza, request);
    }

    /**
     * The {@code Success} reply variant; the relay accepted the batch and registered every token.
     * <p>
     * Carries no payload beyond the envelope echo; the only information conveyed is that the batch
     * was accepted. WA Web's parser reads the {@code id} attribute and returns {@code {stanzaId}};
     * Cobalt's caller obtains the same id from the dispatcher and does not need to surface it on the
     * typed value.
     */
    @WhatsAppWebModule(moduleName = "WAWebSetPrivacyTokensJob")
    final class Success implements IqSetPrivacyTokensResponse {
        /**
         * Constructs a successful reply.
         * <p>
         * Instances are normally obtained via {@link #of(Stanza, Stanza)}; this constructor is also
         * reachable for tests and synthetic fixtures.
         */
        public Success() {
        }

        /**
         * Tries to parse a {@link Success} variant from the given inbound stanza.
         * <p>
         * The {@link Optional#empty()} return signals the stanza is not a success envelope; callers
         * fall through to {@link ClientError#of(Stanza, Stanza)} and {@link ServerError#of(Stanza, Stanza)}.
         *
         * @implNote
         * This implementation only verifies the {@code <iq type="result">} envelope via
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}; no children are read, matching WA
         * Web's {@code setPrivacyTokensParser} which only echoes the stanza id.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return the parsed variant, or {@link Optional#empty()} when the stanza does not match the
         *         success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebSetPrivacyTokensJob",
                exports = "setPrivacyTokensParser",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            return Optional.of(new Success());
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation treats the variant as a singleton value type; all instances are equal.
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return obj != null && obj.getClass() == this.getClass();
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation returns a class-stable hash consistent with {@link #equals(Object)}.
         */
        @Override
        public int hashCode() {
            return Success.class.hashCode();
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation emits a parameterless debug representation; the format is not stable
         * and must not be parsed.
         */
        @Override
        public String toString() {
            return "IqSetPrivacyTokensResponse.Success[]";
        }
    }

    /**
     * The {@code ClientError} reply variant; the relay rejected the request with a {@code 4xx} error
     * code.
     * <p>
     * Surfaces the {@code <error code=... text=.../>} envelope as typed fields so the caller can
     * decide whether to retry, escalate, or surface to the UI; WA Web's
     * {@code WAWebSetPrivacyTokensJob.issuePrivacyToken} throws a {@code ServerStatusCodeError} with
     * the same payload.
     */
    @WhatsAppWebModule(moduleName = "WAWebSetPrivacyTokensJob")
    final class ClientError implements IqSetPrivacyTokensResponse {
        /**
         * The numeric server-side error code (typically {@code 4xx}).
         */
        private final int errorCode;

        /**
         * The optional human-readable error text echoed back by the relay.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply.
         * <p>
         * Instances are normally obtained via {@link #of(Stanza, Stanza)}; this constructor is also
         * reachable for tests and synthetic fixtures.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional error text.
         *
         * @return the text, or {@link Optional#empty()} when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the given inbound stanza.
         *
         * @implNote
         * This implementation delegates the envelope match to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return the parsed variant, or {@link Optional#empty()} when the schema does not match
         */
        @WhatsAppWebExport(moduleName = "WAWebSetPrivacyTokensJob",
                exports = "issuePrivacyToken",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation compares both error code and error text by value.
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ClientError) obj;
            return this.errorCode == that.errorCode
                    && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation hashes both fields consistently with {@link #equals(Object)}.
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation emits a debug-only representation; the format is not stable and must
         * not be parsed.
         */
        @Override
        public String toString() {
            return "IqSetPrivacyTokensResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * The {@code ServerError} reply variant; the relay encountered a transient internal failure (a
     * {@code 5xx} error code).
     * <p>
     * Distinguished from {@link ClientError} so callers can choose a different retry policy.
     */
    @WhatsAppWebModule(moduleName = "WAWebSetPrivacyTokensJob")
    final class ServerError implements IqSetPrivacyTokensResponse {
        /**
         * The numeric server-side error code (typically {@code 5xx}).
         */
        private final int errorCode;

        /**
         * The optional human-readable error text echoed back by the relay.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply.
         * <p>
         * Instances are normally obtained via {@link #of(Stanza, Stanza)}; this constructor is also
         * reachable for tests and synthetic fixtures.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional error text.
         *
         * @return the text, or {@link Optional#empty()} when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the given inbound stanza.
         *
         * @implNote
         * This implementation delegates the envelope match to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return the parsed variant, or {@link Optional#empty()} when the schema does not match
         */
        @WhatsAppWebExport(moduleName = "WAWebSetPrivacyTokensJob",
                exports = "issuePrivacyToken",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation compares both error code and error text by value.
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ServerError) obj;
            return this.errorCode == that.errorCode
                    && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation hashes both fields consistently with {@link #equals(Object)}.
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation emits a debug-only representation; the format is not stable and must
         * not be parsed.
         */
        @Override
        public String toString() {
            return "IqSetPrivacyTokensResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
