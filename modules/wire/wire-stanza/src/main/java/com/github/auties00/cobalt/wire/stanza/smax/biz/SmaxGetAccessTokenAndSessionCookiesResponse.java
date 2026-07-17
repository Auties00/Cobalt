package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqErrorResponseMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the inbound replies to the CTWA ad-account access-token-and-session-cookies request.
 * <p>
 * Enumerates every documented reply variant the relay can return for a
 * {@link SmaxGetAccessTokenAndSessionCookiesRequest}. The variants drive the click-to-WhatsApp
 * ad-creation verify-email-code modal: {@link Success} persists the strong access token and
 * dismisses the modal, {@link IncorrectNonce} shows the invalid-code inline error,
 * {@link TooManyAttempts} shows the generic something-went-wrong copy, and the {@link ClientError}
 * and {@link ServerError} fallbacks both surface the same generic copy.
 */
public sealed interface SmaxGetAccessTokenAndSessionCookiesResponse extends SmaxStanza.Response
        permits SmaxGetAccessTokenAndSessionCookiesResponse.Success, SmaxGetAccessTokenAndSessionCookiesResponse.TooManyAttempts,
        SmaxGetAccessTokenAndSessionCookiesResponse.IncorrectNonce, SmaxGetAccessTokenAndSessionCookiesResponse.ClientError, SmaxGetAccessTokenAndSessionCookiesResponse.ServerError {

    /**
     * Tries each {@link SmaxGetAccessTokenAndSessionCookiesResponse} variant in priority order and
     * returns the first that parses cleanly.
     * <p>
     * {@link Success} is tried first, then the two dedicated literal errors ({@link TooManyAttempts}
     * and {@link IncorrectNonce}), then the generic {@link ClientError} and {@link ServerError}
     * buckets. Returns empty only when none of the documented shapes match.
     *
     * @implNote This implementation short-circuits as soon as any variant matches; for a literal
     * {@code 431} / {@code "TOO_MANY_ATTEMPTS"} error the generic {@link ClientError} fallback is
     * therefore not reached even though both shapes would otherwise overlap on the same IQ envelope.
     *
     * @param stanza    the inbound IQ stanza received from the relay; never {@code null}
     * @param request the original outbound stanza, used to validate echoed identifiers; never
     *                {@code null}
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no
     *         documented variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxBizCtwaAdAccountGetAccessTokenAndSessionCookiesRPC",
            exports = "sendGetAccessTokenAndSessionCookiesRPC",
            adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGetAccessTokenAndSessionCookiesResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        var tooManyAttempts = TooManyAttempts.of(stanza, request);
        if (tooManyAttempts.isPresent()) {
            return tooManyAttempts;
        }
        var incorrectNonce = IncorrectNonce.of(stanza, request);
        if (incorrectNonce.isPresent()) {
            return incorrectNonce;
        }
        var clientError = ClientError.of(stanza, request);
        if (clientError.isPresent()) {
            return clientError;
        }
        return ServerError.of(stanza, request);
    }

    /**
     * The success variant carrying the access token, session cookies, business-person identity, and
     * the optional token-strength marker.
     * <p>
     * Drives the happy-path branch of the verify-email-code modal: {@link #accessToken()} is a
     * {@code WAA}-type Graph API bearer token, {@link #sessionCookies()} is consumed by the Facebook
     * Ads Manager web UI, and {@link #businessPersonId()} is the Facebook business-person identifier
     * the token is scoped to. The optional {@link #tokenType()} records the token strength; a
     * {@link SmaxGetAccessTokenAndSessionCookiesTokenType#WEAK} token is accepted and used as-is, so
     * the strength is stored for the caller's inspection rather than gating success.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountGetAccessTokenAndSessionCookiesResponseSuccess")
    final class Success implements SmaxGetAccessTokenAndSessionCookiesResponse {
        /**
         * The text content of the mandatory {@code <access_token>} child (Graph API bearer token).
         */
        private final String accessToken;

        /**
         * The text content of the mandatory {@code <session_cookies>} child (JSON-encoded blob of
         * cookies for the Facebook Ads Manager web UI).
         */
        private final String sessionCookies;

        /**
         * The {@code id} attribute of the mandatory {@code <business_person>} child (Facebook
         * business-person identifier the token is scoped to).
         */
        private final String businessPersonId;

        /**
         * The optional {@code <token_type>} content carrying the {@code "Strong"} / {@code "Weak"}
         * marker.
         */
        private final SmaxGetAccessTokenAndSessionCookiesTokenType tokenType;

        /**
         * Constructs a successful reply from already-validated wire values.
         * <p>
         * Callers normally obtain a reply by parsing a stanza via {@link #of(Stanza, Stanza)}; this
         * constructor is exposed for tests and for hand-built fixtures.
         *
         * @param accessToken      the Graph API bearer token; never {@code null}
         * @param sessionCookies   the JSON-encoded session-cookies blob; never {@code null}
         * @param businessPersonId the Facebook business-person identifier; never {@code null}
         * @param tokenType        the optional token-strength marker; may be {@code null}
         * @throws NullPointerException if any of {@code accessToken}, {@code sessionCookies}, or
         *                              {@code businessPersonId} is {@code null}
         */
        public Success(String accessToken, String sessionCookies,
                       String businessPersonId, SmaxGetAccessTokenAndSessionCookiesTokenType tokenType) {
            this.accessToken = Objects.requireNonNull(accessToken, "accessToken cannot be null");
            this.sessionCookies = Objects.requireNonNull(sessionCookies, "sessionCookies cannot be null");
            this.businessPersonId = Objects.requireNonNull(businessPersonId, "businessPersonId cannot be null");
            this.tokenType = tokenType;
        }

        /**
         * Returns the access token.
         * <p>
         * Used as a {@code WAA}-type Graph API bearer token, stored alongside the matching
         * {@link #businessPersonId()}.
         *
         * @return the access token; never {@code null}
         */
        public String accessToken() {
            return accessToken;
        }

        /**
         * Returns the session-cookies blob.
         * <p>
         * JSON-encoded cookies consumed by the Facebook Ads Manager web UI when it is opened in an
         * embedded surface.
         *
         * @return the session cookies; never {@code null}
         */
        public String sessionCookies() {
            return sessionCookies;
        }

        /**
         * Returns the Facebook business-person identifier the token is scoped to.
         * <p>
         * Carried alongside the access token through the token-store as the {@code bp_id} field of
         * the persisted entry.
         *
         * @return the business-person ID; never {@code null}
         */
        public String businessPersonId() {
            return businessPersonId;
        }

        /**
         * Returns the optional token-strength marker.
         * <p>
         * Records whether the relay minted a strong or weak token; both are accepted and used as-is,
         * so the strength is stored for the caller's inspection rather than gating success. Absent on
         * legacy relays.
         *
         * @return an {@link Optional} carrying the marker
         *         ({@link SmaxGetAccessTokenAndSessionCookiesTokenType#STRONG} or
         *         {@link SmaxGetAccessTokenAndSessionCookiesTokenType#WEAK}), or empty when the
         *         relay omitted the {@code <token_type>} child
         */
        public Optional<SmaxGetAccessTokenAndSessionCookiesTokenType> tokenType() {
            return Optional.ofNullable(tokenType);
        }

        /**
         * Parses a {@link Success} variant from the inbound stanza.
         * <p>
         * Returns empty when the {@code <iq type="result">} envelope shape does not match the
         * original request id, when any of the mandatory text-content children
         * ({@code <access_token>}, {@code <session_cookies>}) is missing or empty, when the
         * {@code <business_person id="...">} grandchild or its {@code id} attribute is absent, or
         * when the optional {@code <token_type>} child is present but its text content fails
         * {@link SmaxGetAccessTokenAndSessionCookiesTokenType} validation.
         *
         * @implNote This implementation delegates envelope validation ({@code type="result"} plus
         * echoed id) to {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)} so a mis-matched id
         * short-circuits before any payload field is sampled.
         *
         * @param stanza    the inbound IQ stanza; never {@code null}
         * @param request the original outbound request; never {@code null}
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the success schema
         * @throws NullPointerException if either argument is {@code null}
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountGetAccessTokenAndSessionCookiesResponseSuccess",
                exports = "parseGetAccessTokenAndSessionCookiesResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            Objects.requireNonNull(request, "request cannot be null");
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var accessTokenNode = stanza.getChild("access_token").orElse(null);
            if (accessTokenNode == null) {
                return Optional.empty();
            }
            var accessToken = accessTokenNode.toContentString().orElse(null);
            if (accessToken == null) {
                return Optional.empty();
            }
            var sessionCookiesNode = stanza.getChild("session_cookies").orElse(null);
            if (sessionCookiesNode == null) {
                return Optional.empty();
            }
            var sessionCookies = sessionCookiesNode.toContentString().orElse(null);
            if (sessionCookies == null) {
                return Optional.empty();
            }
            var businessPersonNode = stanza.getChild("business_person").orElse(null);
            if (businessPersonNode == null) {
                return Optional.empty();
            }
            var businessPersonId = businessPersonNode.getAttributeAsString("id").orElse(null);
            if (businessPersonId == null) {
                return Optional.empty();
            }
            var tokenTypeNode = stanza.getChild("token_type").orElse(null);
            SmaxGetAccessTokenAndSessionCookiesTokenType tokenType = null;
            if (tokenTypeNode != null) {
                var tokenTypeText = tokenTypeNode.toContentString().orElse(null);
                if (tokenTypeText == null) {
                    return Optional.empty();
                }
                tokenType = SmaxGetAccessTokenAndSessionCookiesTokenType.of(tokenTypeText).orElse(null);
                if (tokenType == null) {
                    return Optional.empty();
                }
            }
            return Optional.of(new Success(accessToken, sessionCookies, businessPersonId, tokenType));
        }

        /**
         * Compares this reply to {@code obj} for structural equality on all four slots.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link Success} with matching
         *         {@link #accessToken()}, {@link #sessionCookies()}, {@link #businessPersonId()},
         *         and {@link #tokenType()}
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
            return Objects.equals(this.accessToken, that.accessToken)
                    && Objects.equals(this.sessionCookies, that.sessionCookies)
                    && Objects.equals(this.businessPersonId, that.businessPersonId)
                    && Objects.equals(this.tokenType, that.tokenType);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash of all four slots
         */
        @Override
        public int hashCode() {
            return Objects.hash(accessToken, sessionCookies, businessPersonId, tokenType);
        }

        /**
         * Returns a debug-friendly rendering naming all four slots.
         *
         * @return a record-style string with the four slot values
         */
        @Override
        public String toString() {
            return "SmaxGetAccessTokenAndSessionCookiesResponse.Success[accessToken=" + accessToken
                    + ", sessionCookies=" + sessionCookies
                    + ", businessPersonId=" + businessPersonId
                    + ", tokenType=" + tokenType + ']';
        }
    }

    /**
     * The too-many-attempts variant signalling that the user has exhausted the rate limit for this
     * nonce.
     * <p>
     * Identified by the literal {@code <error code="431" text="TOO_MANY_ATTEMPTS"/>} pair on the
     * {@code <error/>} child of an {@code <iq type="error">} envelope. Surfaced as the generic
     * something-went-wrong inline error because the verify-email-code modal does not differentiate
     * per-error copy.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountGetAccessTokenAndSessionCookiesResponseTooManyAttempts")
    final class TooManyAttempts implements SmaxGetAccessTokenAndSessionCookiesResponse {
        /**
         * Constructs a marker reply.
         * <p>
         * The variant carries no payload; the constructor exists only so {@link #of(Stanza, Stanza)} and
         * tests can instantiate it.
         */
        public TooManyAttempts() {
        }

        /**
         * Parses a {@link TooManyAttempts} variant from the inbound stanza.
         * <p>
         * Returns empty when the {@code <iq type="error">} envelope shape does not match the
         * original request id, when the {@code <error/>} child is missing, or when the {@code code}
         * and {@code text} attributes are not exactly the literal {@code "431"} /
         * {@code "TOO_MANY_ATTEMPTS"} pair.
         *
         * @implNote This implementation delegates envelope validation ({@code type="error"} plus
         * echoed id) to {@link SmaxIqErrorResponseMixin#validate(Stanza, Stanza)} so a mis-matched id
         * short-circuits before the error literals are sampled.
         *
         * @param stanza    the inbound IQ stanza; never {@code null}
         * @param request the original outbound request; never {@code null}
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the literal {@code 431} / {@code TOO_MANY_ATTEMPTS} schema
         * @throws NullPointerException if either argument is {@code null}
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountGetAccessTokenAndSessionCookiesResponseTooManyAttempts",
                exports = "parseGetAccessTokenAndSessionCookiesResponseTooManyAttempts",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<TooManyAttempts> of(Stanza stanza, Stanza request) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            Objects.requireNonNull(request, "request cannot be null");
            if (!SmaxIqErrorResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var error = stanza.getChild("error").orElse(null);
            if (error == null) {
                return Optional.empty();
            }
            if (!error.hasAttribute("code", "431")) {
                return Optional.empty();
            }
            if (!error.hasAttribute("text", "TOO_MANY_ATTEMPTS")) {
                return Optional.empty();
            }
            return Optional.of(new TooManyAttempts());
        }

        /**
         * Compares this reply to {@code obj} for class-level identity.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when {@code obj} is also a {@link TooManyAttempts}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return obj != null && obj.getClass() == this.getClass();
        }

        /**
         * Returns a constant hash matching the class identity used by {@link #equals(Object)}.
         *
         * @return the class hash
         */
        @Override
        public int hashCode() {
            return TooManyAttempts.class.hashCode();
        }

        /**
         * Returns the canonical empty-record rendering.
         *
         * @return a record-style string with no slots
         */
        @Override
        public String toString() {
            return "SmaxGetAccessTokenAndSessionCookiesResponse.TooManyAttempts[]";
        }
    }

    /**
     * The incorrect-nonce variant signalling that the user typed a verification code that did not
     * match the relay-side nonce.
     * <p>
     * Identified by the literal {@code <error code="432" text="INCORRECT_NONCE"/>} pair on the
     * {@code <error/>} child of an {@code <iq type="error">} envelope. This is the only error
     * variant surfaced with differentiated copy (the inline invalid-code message); every other
     * failure falls back to the generic something-went-wrong string.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountGetAccessTokenAndSessionCookiesResponseIncorrectNonce")
    final class IncorrectNonce implements SmaxGetAccessTokenAndSessionCookiesResponse {
        /**
         * Constructs a marker reply.
         * <p>
         * The variant carries no payload; the constructor exists only so {@link #of(Stanza, Stanza)} and
         * tests can instantiate it.
         */
        public IncorrectNonce() {
        }

        /**
         * Parses an {@link IncorrectNonce} variant from the inbound stanza.
         * <p>
         * Returns empty when the {@code <iq type="error">} envelope shape does not match the
         * original request id, when the {@code <error/>} child is missing, or when the {@code code}
         * and {@code text} attributes are not exactly the literal {@code "432"} /
         * {@code "INCORRECT_NONCE"} pair.
         *
         * @implNote This implementation delegates envelope validation to
         * {@link SmaxIqErrorResponseMixin#validate(Stanza, Stanza)} so a mis-matched id short-circuits
         * before the error literals are sampled.
         *
         * @param stanza    the inbound IQ stanza; never {@code null}
         * @param request the original outbound request; never {@code null}
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the literal {@code 432} / {@code INCORRECT_NONCE} schema
         * @throws NullPointerException if either argument is {@code null}
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountGetAccessTokenAndSessionCookiesResponseIncorrectNonce",
                exports = "parseGetAccessTokenAndSessionCookiesResponseIncorrectNonce",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<IncorrectNonce> of(Stanza stanza, Stanza request) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            Objects.requireNonNull(request, "request cannot be null");
            if (!SmaxIqErrorResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var error = stanza.getChild("error").orElse(null);
            if (error == null) {
                return Optional.empty();
            }
            if (!error.hasAttribute("code", "432")) {
                return Optional.empty();
            }
            if (!error.hasAttribute("text", "INCORRECT_NONCE")) {
                return Optional.empty();
            }
            return Optional.of(new IncorrectNonce());
        }

        /**
         * Compares this reply to {@code obj} for class-level identity.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when {@code obj} is also an {@link IncorrectNonce}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return obj != null && obj.getClass() == this.getClass();
        }

        /**
         * Returns a constant hash matching the class identity used by {@link #equals(Object)}.
         *
         * @return the class hash
         */
        @Override
        public int hashCode() {
            return IncorrectNonce.class.hashCode();
        }

        /**
         * Returns the canonical empty-record rendering.
         *
         * @return a record-style string with no slots
         */
        @Override
        public String toString() {
            return "SmaxGetAccessTokenAndSessionCookiesResponse.IncorrectNonce[]";
        }
    }

    /**
     * The generic client-error variant covering documented common-ad-account 4xx errors that do not
     * match the dedicated {@link TooManyAttempts} / {@link IncorrectNonce} literals.
     * <p>
     * Surfaces the generic something-went-wrong inline message; consumers can read
     * {@link #errorCode()} and {@link #errorText()} for diagnostics but the verify-email-code modal
     * does not differentiate per-code copy.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountGetAccessTokenAndSessionCookiesResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountCommonAdAccountErrors")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountHackBaseIQErrorResponseMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountIQErrorBadRequestMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountIQErrorForbiddenMixin")
    final class ClientError implements SmaxGetAccessTokenAndSessionCookiesResponse {
        /**
         * The numeric server-side error code (parsed from {@code <error code="..."/>}).
         */
        private final int errorCode;

        /**
         * The human-readable error text (parsed from {@code <error text="..."/>}), when the relay
         * supplied one.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply from already-validated wire values.
         * <p>
         * Callers normally obtain a reply by parsing a stanza via {@link #of(Stanza, Stanza)}; this
         * constructor is exposed for tests and for hand-built fixtures.
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
         * <p>
         * One of the documented 4xx codes, excluding {@code 431} / {@code 432}, which are routed to
         * the dedicated variants.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         * <p>
         * Useful for diagnostics; not surfaced to the user, who sees a single generic copy
         * regardless.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses a {@link ClientError} variant from the inbound stanza.
         * <p>
         * Returns empty when the stanza is not a 4xx error envelope, when the request id does not
         * match, or when the error happens to match one of the dedicated {@code 431} / {@code 432}
         * literals (which the orchestrating {@link #of(Stanza, Stanza)} will already have routed to
         * {@link TooManyAttempts} / {@link IncorrectNonce}).
         *
         * @implNote This implementation delegates the envelope and 4xx-code extraction to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}; the helper enforces the
         * {@code type="error"} envelope, validates the echoed id, and rejects 5xx codes for routing
         * to {@link ServerError}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountGetAccessTokenAndSessionCookiesResponseError",
                exports = "parseGetAccessTokenAndSessionCookiesResponseError",
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
         * Compares this reply to {@code obj} for structural equality on the error code and text.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link ClientError} with matching
         *         {@link #errorCode()} and {@link #errorText()}
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
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash of the error code and text
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug-friendly rendering naming the error code and text.
         *
         * @return a record-style string with the error code and text
         */
        @Override
        public String toString() {
            return "SmaxGetAccessTokenAndSessionCookiesResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * The generic server-error variant covering transient relay-side 5xx failures.
     * <p>
     * Carries the same generic surface as {@link ClientError} but indicates a server-side fault
     * (typically internal-server-error or service-unavailable); the same something-went-wrong inline
     * message is shown and the caller may retry at the application layer.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountGetAccessTokenAndSessionCookiesResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountCommonAdAccountErrors")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountHackBaseIQErrorResponseMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountIQErrorInternalServerErrorMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountIQErrorServiceUnavailableMixin")
    final class ServerError implements SmaxGetAccessTokenAndSessionCookiesResponse {
        /**
         * The numeric server-side error code (parsed from {@code <error code="..."/>}).
         */
        private final int errorCode;

        /**
         * The human-readable error text (parsed from {@code <error text="..."/>}), when the relay
         * supplied one.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply from already-validated wire values.
         * <p>
         * Callers normally obtain a reply by parsing a stanza via {@link #of(Stanza, Stanza)}; this
         * constructor is exposed for tests and for hand-built fixtures.
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
         * <p>
         * One of the documented 5xx codes routed through
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         * <p>
         * Useful for diagnostics; not surfaced to the user.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses a {@link ServerError} variant from the inbound stanza.
         * <p>
         * Returns empty when the stanza is not a 5xx error envelope or when the request id does not
         * match.
         *
         * @implNote This implementation delegates the envelope and 5xx-code extraction to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}; the helper enforces the
         * {@code type="error"} envelope and rejects 4xx codes for routing to {@link ClientError}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountGetAccessTokenAndSessionCookiesResponseError",
                exports = "parseGetAccessTokenAndSessionCookiesResponseError",
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
         * Compares this reply to {@code obj} for structural equality on the error code and text.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link ServerError} with matching
         *         {@link #errorCode()} and {@link #errorText()}
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
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash of the error code and text
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug-friendly rendering naming the error code and text.
         *
         * @return a record-style string with the error code and text
         */
        @Override
        public String toString() {
            return "SmaxGetAccessTokenAndSessionCookiesResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
