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
 * in response to a {@link SmaxRequestSilentNonceRequest}.
 *
 * <p>The click-to-WhatsApp biz-token-nonce flow polls the relay for a silent nonce before driving
 * the Meta token-exchange surface. The four variants split the wire outcome into {@link Success}
 * (relay supplied a nonce directly without recovery), {@link RecoveryRequired} (relay refused the
 * silent path because the user must first confirm account ownership via a recovery email; carries
 * the email mask the UI must show), {@link ClientError} (relay rejected the request with a
 * {@code 4xx} envelope) and {@link ServerError} (transient {@code 5xx} relay failure).
 *
 * @implNote
 * This implementation tries each variant in priority order via {@link #of(Stanza, Stanza)} and returns
 * the first successful parse.
 */
public sealed interface SmaxRequestSilentNonceResponse extends SmaxStanza.Response
        permits SmaxRequestSilentNonceResponse.Success, SmaxRequestSilentNonceResponse.RecoveryRequired,
        SmaxRequestSilentNonceResponse.ClientError, SmaxRequestSilentNonceResponse.ServerError {

    /**
     * Tries each {@link SmaxRequestSilentNonceResponse} variant in
     * priority order and returns the first that parses cleanly.
     *
     * <p>Invoked by the smax reply pump after dispatching a {@link SmaxRequestSilentNonceRequest}.
     * The priority order ensures a malformed {@link Success} stanza falls through to
     * {@link RecoveryRequired} (then to {@link ClientError}) rather than masking the documented
     * outcome.
     *
     * @implNote
     * This implementation invokes {@link Success#of(Stanza, Stanza)}
     * first, then {@link RecoveryRequired#of(Stanza, Stanza)}, then
     * {@link ClientError#of(Stanza, Stanza)}, then
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
    @WhatsAppWebExport(moduleName = "WASmaxBizAccessTokenRequestSilentNonceRPC",
            exports = "sendRequestSilentNonceRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxRequestSilentNonceResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        var recoveryRequired = RecoveryRequired.of(stanza, request);
        if (recoveryRequired.isPresent()) {
            return recoveryRequired;
        }
        var clientError = ClientError.of(stanza, request);
        if (clientError.isPresent()) {
            return clientError;
        }
        return ServerError.of(stanza, request);
    }

    /**
     * The {@code Success} reply variant signalling that the relay
     * accepted the silent-nonce request and will push the nonce via
     * a separate notification.
     *
     * <p>Projected by {@link SmaxRequestSilentNonceResponse#of(Stanza, Stanza)} when the relay returns
     * the documented {@code <Result status="Success"/>} tree. The caller treats this branch as a
     * hand-off and waits for the asynchronous nonce-push notification (see
     * {@link SmaxNonceNotificationResponse}) before resolving the fetch.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizAccessTokenRequestSilentNonceResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInBizAccessTokenHackBaseIQResultResponseMixin")
    final class Success implements SmaxRequestSilentNonceResponse {
        /**
         * Constructs a new successful reply.
         *
         * <p>Invoked by {@link #of(Stanza, Stanza)} after the {@code <Result status="Success"/>} tree
         * has been validated; takes no arguments because the wire form carries no projected payload
         * besides the literal status string.
         */
        public Success() {
        }

        /**
         * Tries to parse a {@link Success} variant from the given
         * inbound stanza.
         *
         * @implNote
         * This implementation enforces the
         * {@link SmaxIqResultResponseMixin} envelope check, walks the
         * {@code <result>} child, and requires the {@code status}
         * attribute to be the literal {@code "Success"}; any other
         * value yields {@link Optional#empty()} so the dispatch can
         * fall through to {@link RecoveryRequired}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant,
         *         or empty when the stanza does not match the
         *         success schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizAccessTokenRequestSilentNonceResponseSuccess",
                exports = "parseRequestSilentNonceResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizAccessTokenHackBaseIQResultResponseMixin",
                exports = "parseHackBaseIQResultResponseMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            Objects.requireNonNull(request, "request cannot be null");
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var result = stanza.getChild("result").orElse(null);
            if (result == null) {
                return Optional.empty();
            }
            if (!result.hasAttribute("status", "Success")) {
                return Optional.empty();
            }
            return Optional.of(new Success());
        }

        /**
         * {@inheritDoc}
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
         */
        @Override
        public int hashCode() {
            return Success.class.hashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "SmaxRequestSilentNonceResponse.Success[]";
        }
    }

    /**
     * The {@code RecoveryRequired} reply variant signalling that the
     * relay refused the silent nonce path and instead dispatched a
     * recovery email the user must confirm before retrying.
     *
     * <p>Projected by {@link SmaxRequestSilentNonceResponse#of(Stanza, Stanza)} when the relay returns
     * the documented {@code <Result status="RecoveryRequired" email="..."/>} tree. The caller
     * surfaces the email mask to the UI so the user can complete the recovery flow that issues a
     * {@link SmaxSendAccountRecoveryNonceRequest} (see
     * {@link SmaxSendAccountRecoveryNonceResponse}).
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizAccessTokenRequestSilentNonceResponseRecoveryRequired")
    @WhatsAppWebModule(moduleName = "WASmaxInBizAccessTokenHackBaseIQResultResponseMixin")
    final class RecoveryRequired implements SmaxRequestSilentNonceResponse {
        /**
         * The masked email address the relay dispatched the recovery
         * code to.
         */
        private final String email;

        /**
         * Constructs a new recovery-required reply.
         *
         * <p>Invoked by {@link #of(Stanza, Stanza)} after the
         * {@code <Result status="RecoveryRequired"/>} envelope and the {@code email} attribute have
         * been validated.
         *
         * @param email the masked recovery-email address; never
         *              {@code null}
         * @throws NullPointerException if {@code email} is
         *                              {@code null}
         */
        public RecoveryRequired(String email) {
            this.email = Objects.requireNonNull(email, "email cannot be null");
        }

        /**
         * Returns the masked recovery-email address.
         *
         * <p>Surface this to the UI as the "we sent a code to ..." disclosure text.
         *
         * @return the email mask; never {@code null}
         */
        public String email() {
            return email;
        }

        /**
         * Tries to parse a {@link RecoveryRequired} variant from the
         * given inbound stanza.
         *
         * @implNote
         * This implementation enforces the
         * {@link SmaxIqResultResponseMixin} envelope check, walks
         * the {@code <result>} child, requires the {@code status}
         * attribute to be the literal {@code "RecoveryRequired"},
         * and then requires the {@code email} attribute to be
         * present; missing or wrong attributes yield
         * {@link Optional#empty()}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant,
         *         or empty when the stanza does not match the
         *         recovery-required schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizAccessTokenRequestSilentNonceResponseRecoveryRequired",
                exports = "parseRequestSilentNonceResponseRecoveryRequired",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizAccessTokenHackBaseIQResultResponseMixin",
                exports = "parseHackBaseIQResultResponseMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<RecoveryRequired> of(Stanza stanza, Stanza request) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            Objects.requireNonNull(request, "request cannot be null");
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var result = stanza.getChild("result").orElse(null);
            if (result == null) {
                return Optional.empty();
            }
            if (!result.hasAttribute("status", "RecoveryRequired")) {
                return Optional.empty();
            }
            var email = result.getAttributeAsString("email").orElse(null);
            if (email == null) {
                return Optional.empty();
            }
            return Optional.of(new RecoveryRequired(email));
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
            var that = (RecoveryRequired) obj;
            return Objects.equals(this.email, that.email);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(email);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "SmaxRequestSilentNonceResponse.RecoveryRequired[email=" + email + ']';
        }
    }

    /**
     * The {@code ClientError} reply variant carrying a documented
     * {@code 4xx} biz-access-token rejection.
     *
     * <p>Surfaced when the relay rejected the silent-nonce request (bad-request or forbidden). The
     * caller treats this branch as a terminal error and does not retry.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizAccessTokenRequestSilentNonceResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInBizAccessTokenRequestSilentNonceErrors")
    @WhatsAppWebModule(moduleName = "WASmaxInBizAccessTokenIQErrorBadRequestMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInBizAccessTokenIQErrorForbiddenMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInBizAccessTokenHackBaseIQErrorResponseMixin")
    final class ClientError implements SmaxRequestSilentNonceResponse {
        /**
         * The numeric server-side error code in the {@code 4xx}
         * range ({@code 400} bad-request or {@code 403} forbidden).
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
         * matching WA Web's {@code parseRequestSilentNonceErrors}
         * disjunction over the bad-request and forbidden mixins.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant,
         *         or empty when the stanza does not match the
         *         client-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizAccessTokenRequestSilentNonceResponseError",
                exports = "parseRequestSilentNonceResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizAccessTokenRequestSilentNonceErrors",
                exports = "parseRequestSilentNonceErrors",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizAccessTokenIQErrorBadRequestMixin",
                exports = "parseIQErrorBadRequestMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizAccessTokenIQErrorForbiddenMixin",
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
            return "SmaxRequestSilentNonceResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * The {@code ServerError} reply variant carrying a transient
     * {@code 5xx} relay failure.
     *
     * <p>Surfaced when the relay returned a transient internal failure while processing the
     * silent-nonce request (internal-server-error or service-unavailable); the caller can retry
     * with exponential backoff.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizAccessTokenRequestSilentNonceResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInBizAccessTokenRequestSilentNonceErrors")
    @WhatsAppWebModule(moduleName = "WASmaxInBizAccessTokenIQErrorInternalServerErrorMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInBizAccessTokenIQErrorServiceUnavailableMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInBizAccessTokenHackBaseIQErrorResponseMixin")
    final class ServerError implements SmaxRequestSilentNonceResponse {
        /**
         * The numeric server-side error code in the {@code 5xx}
         * range ({@code 500} internal-server-error or {@code 503}
         * service-unavailable).
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
        @WhatsAppWebExport(moduleName = "WASmaxInBizAccessTokenRequestSilentNonceResponseError",
                exports = "parseRequestSilentNonceResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizAccessTokenRequestSilentNonceErrors",
                exports = "parseRequestSilentNonceErrors",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizAccessTokenIQErrorInternalServerErrorMixin",
                exports = "parseIQErrorInternalServerErrorMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizAccessTokenIQErrorServiceUnavailableMixin",
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
            return "SmaxRequestSilentNonceResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
