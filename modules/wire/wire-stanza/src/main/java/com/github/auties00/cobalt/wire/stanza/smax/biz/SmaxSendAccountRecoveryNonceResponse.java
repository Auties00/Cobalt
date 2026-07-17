package com.github.auties00.cobalt.wire.stanza.smax.biz;

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
 * The sealed family of inbound reply variants produced by the relay
 * in response to a {@link SmaxSendAccountRecoveryNonceRequest}.
 *
 * <p>The CTWA ad-account recovery flow asks the relay to dispatch a one-time recovery email after
 * the silent-nonce path (see {@link SmaxRequestSilentNonceResponse}) surfaces a
 * {@link SmaxRequestSilentNonceResponse.RecoveryRequired} outcome. The three variants split the
 * wire outcome into {@link Success} (relay tried to dispatch the email; the embedded
 * {@link SmaxSendAccountRecoveryNonceStatus} indicates whether the dispatch actually succeeded),
 * {@link ClientError} (relay rejected the request with a {@code 4xx} common-ad-account error) and
 * {@link ServerError} (relay returned a transient {@code 5xx} failure).
 *
 * @implNote
 * This implementation tries each variant in priority order via {@link #of(Stanza, Stanza)} and returns
 * the first successful parse.
 */
public sealed interface SmaxSendAccountRecoveryNonceResponse extends SmaxStanza.Response
        permits SmaxSendAccountRecoveryNonceResponse.Success, SmaxSendAccountRecoveryNonceResponse.ClientError, SmaxSendAccountRecoveryNonceResponse.ServerError {

    /**
     * Tries each {@link SmaxSendAccountRecoveryNonceResponse} variant
     * in priority order and returns the first that parses cleanly.
     *
     * <p>Invoked by the smax reply pump after dispatching a
     * {@link SmaxSendAccountRecoveryNonceRequest}. The priority order ensures a malformed
     * {@link Success} stanza falls through to {@link ClientError} rather than masking an error.
     *
     * @implNote
     * This implementation invokes {@link Success#of(Stanza, Stanza)}
     * first, then {@link ClientError#of(Stanza, Stanza)}, then
     * {@link ServerError#of(Stanza, Stanza)}; an unrecognised stanza
     * shape returns {@link Optional#empty()}.
     *
     * @param stanza    the inbound IQ stanza received from the relay;
     *                never {@code null}
     * @param request the original outbound stanza, used to validate
     *                echoed identifiers; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or
     *         {@link Optional#empty()} when no documented variant
     *         matched the stanza shape
     * @throws NullPointerException if either argument is
     *                              {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxBizCtwaAdAccountSendAccountRecoveryNonceRPC",
            exports = "sendSendAccountRecoveryNonceRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxSendAccountRecoveryNonceResponse> of(Stanza stanza, Stanza request) {
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
     * The {@code Success} reply variant carrying the recovery-email
     * dispatch status.
     *
     * <p>Projected by {@link SmaxSendAccountRecoveryNonceResponse#of(Stanza, Stanza)} when the relay
     * returns the documented {@code <Result><status>} tree. The embedded
     * {@link SmaxSendAccountRecoveryNonceStatus} distinguishes a dispatched email
     * ({@link SmaxSendAccountRecoveryNonceStatus#SUCCESS}) from one the relay accepted but failed
     * to send ({@link SmaxSendAccountRecoveryNonceStatus#FAIL}).
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountSendAccountRecoveryNonceResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountSendAccountRecoveryNonceResponseMixin")
    final class Success implements SmaxSendAccountRecoveryNonceResponse {
        /**
         * The recovery-email dispatch status read from the
         * {@code <Result><status>...</status></Result>} child
         * content; one of the
         * {@link SmaxSendAccountRecoveryNonceStatus} dictionary
         * values.
         */
        private final SmaxSendAccountRecoveryNonceStatus status;

        /**
         * Constructs a new successful reply.
         *
         * <p>Invoked by {@link #of(Stanza, Stanza)} after the {@code <status>} content has been
         * validated against the recovery-status dictionary.
         *
         * @param status the recovery-dispatch status; never
         *               {@code null}
         * @throws NullPointerException if {@code status} is
         *                              {@code null}
         */
        public Success(SmaxSendAccountRecoveryNonceStatus status) {
            this.status = Objects.requireNonNull(status, "status cannot be null");
        }

        /**
         * Returns the recovery-dispatch status.
         *
         * <p>Drives the recovery-code request UI:
         * {@link SmaxSendAccountRecoveryNonceStatus#SUCCESS} means the relay accepted and
         * dispatched the email, while {@link SmaxSendAccountRecoveryNonceStatus#FAIL} means the
         * relay accepted the IQ but the dispatch attempt failed.
         *
         * @return the status; never {@code null}
         */
        public SmaxSendAccountRecoveryNonceStatus status() {
            return status;
        }

        /**
         * Tries to parse a {@link Success} variant from the given
         * inbound stanza.
         *
         * @implNote
         * This implementation enforces the
         * {@link SmaxIqResultResponseMixin} envelope check, walks
         * the {@code <Result><status>} tree, and validates the
         * status content string against
         * {@link SmaxSendAccountRecoveryNonceStatus#of(String)};
         * any other content yields {@link Optional#empty()} rather
         * than an exception, matching the strict dictionary
         * semantics of
         * {@code WASmaxParseUtils.contentStringEnum(n.value, ENUM_FAIL_SUCCESS)}
         * in
         * {@code WASmaxInBizCtwaAdAccountSendAccountRecoveryNonceResponseMixin.parseSendAccountRecoveryNonceResponseMixin}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant,
         *         or empty when the stanza does not match the
         *         success schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountSendAccountRecoveryNonceResponseSuccess",
                exports = "parseSendAccountRecoveryNonceResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountSendAccountRecoveryNonceResponseMixin",
                exports = "parseSendAccountRecoveryNonceResponseMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            Objects.requireNonNull(request, "request cannot be null");
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var result = stanza.getChild("Result").orElse(null);
            if (result == null) {
                return Optional.empty();
            }
            var statusNode = result.getChild("status").orElse(null);
            if (statusNode == null) {
                return Optional.empty();
            }
            var statusText = statusNode.toContentString().orElse(null);
            if (statusText == null) {
                return Optional.empty();
            }
            var status = SmaxSendAccountRecoveryNonceStatus.of(statusText).orElse(null);
            if (status == null) {
                return Optional.empty();
            }
            return Optional.of(new Success(status));
        }

        /**
         * {@inheritDoc}
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
            return Objects.equals(this.status, that.status);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(status);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "SmaxSendAccountRecoveryNonceResponse.Success[status=" + status + ']';
        }
    }

    /**
     * The {@code ClientError} reply variant carrying a documented
     * {@code 4xx} common-ad-account rejection.
     *
     * <p>Surfaced when the relay rejected the recovery-email request (bad-request or forbidden).
     * The caller treats any non-success branch as a terminal error.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountSendAccountRecoveryNonceResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountCommonAdAccountErrors")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountHackBaseIQErrorResponseMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountIQErrorBadRequestMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountIQErrorForbiddenMixin")
    final class ClientError implements SmaxSendAccountRecoveryNonceResponse {
        /**
         * The numeric server-side error code in the {@code 4xx}
         * range.
         */
        private final int errorCode;

        /**
         * The human-readable error text, when the relay supplied
         * one.
         */
        private final String errorText;

        /**
         * Constructs a new client-error reply.
         *
         * <p>Invoked by {@link #of(Stanza, Stanza)} after the {@code 4xx} envelope has been validated.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may
         *                  be {@code null}
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
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or
         *         empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the given
         * inbound stanza.
         *
         * @implNote
         * This implementation routes the {@code <iq>}/{@code <error>}
         * extraction through
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}
         * and admits the full {@code 4xx} range as a catch-all,
         * matching WA Web's
         * {@code parseCommonAdAccountErrors} disjunction over the
         * bad-request and forbidden mixins.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant,
         *         or empty when the stanza does not match the
         *         client-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountSendAccountRecoveryNonceResponseError",
                exports = "parseSendAccountRecoveryNonceResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountCommonAdAccountErrors",
                exports = "parseCommonAdAccountErrors",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountIQErrorBadRequestMixin",
                exports = "parseIQErrorBadRequestMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountIQErrorForbiddenMixin",
                exports = "parseIQErrorForbiddenMixin",
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
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "SmaxSendAccountRecoveryNonceResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * The {@code ServerError} reply variant carrying a transient
     * {@code 5xx} relay failure.
     *
     * <p>Surfaced when the relay returned a transient internal failure while processing the
     * recovery-email request (internal-server-error or service-unavailable); the caller can
     * re-issue the request with backoff.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountSendAccountRecoveryNonceResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountCommonAdAccountErrors")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountHackBaseIQErrorResponseMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountIQErrorInternalServerErrorMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountIQErrorServiceUnavailableMixin")
    final class ServerError implements SmaxSendAccountRecoveryNonceResponse {
        /**
         * The numeric server-side error code in the {@code 5xx}
         * range.
         */
        private final int errorCode;

        /**
         * The human-readable error text, when the relay supplied
         * one.
         */
        private final String errorText;

        /**
         * Constructs a new server-error reply.
         *
         * <p>Invoked by {@link #of(Stanza, Stanza)} after the {@code 5xx} envelope has been validated.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may
         *                  be {@code null}
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
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or
         *         empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the given
         * inbound stanza.
         *
         * @implNote
         * This implementation delegates the {@code 5xx} range check
         * to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)};
         * any stanza outside the {@code 5xx} range yields
         * {@link Optional#empty()}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant,
         *         or empty when the stanza does not match the
         *         server-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountSendAccountRecoveryNonceResponseError",
                exports = "parseSendAccountRecoveryNonceResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountCommonAdAccountErrors",
                exports = "parseCommonAdAccountErrors",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountIQErrorInternalServerErrorMixin",
                exports = "parseIQErrorInternalServerErrorMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountIQErrorServiceUnavailableMixin",
                exports = "parseIQErrorServiceUnavailableMixin",
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
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "SmaxSendAccountRecoveryNonceResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
