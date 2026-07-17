package com.github.auties00.cobalt.exception.linked;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Sealed root for failures encountered while refreshing the WhatsApp
 * Business click-to-WhatsApp Facebook GraphQL session credentials.
 *
 * <p>The Facebook GraphQL transport authenticates against Meta's graph endpoint with
 * a Facebook access token that the WhatsApp client mints from a silent
 * nonce pushed by the relay. The refresh pipeline is multi-stage (silent
 * nonce request, asynchronous nonce-push correlation, token exchange) and
 * each stage has its own documented failure mode. The nested subtypes
 * enumerate the distinct failures: the user must run the interactive
 * recovery flow ({@link SilentNonceRecoveryRequired}), the relay refused
 * the nonce request with a documented {@code 4xx}
 * ({@link SilentNonceClientError}) or transient {@code 5xx}
 * ({@link SilentNonceServerError}) error envelope, the pushed nonce never
 * arrived within the configured wait window
 * ({@link SilentNonceTimeout}), the downstream token-exchange call
 * returned no session ({@link TokenExchangeFailed}), or the durable
 * canonical-access-token seed needed to start the refresh is missing
 * ({@link SessionUnseeded}).
 *
 * @apiNote
 * Every subtype returns {@link WhatsAppLinkedClientErrorResult#DISCARD} from
 * {@link #toErrorResult()}: a refresh failure scopes to the Facebook GraphQL
 * credential slot and does not invalidate the underlying Noise messaging
 * channel. Pattern-match the concrete
 * subtype to decide whether to surface a recovery UI
 * ({@link SilentNonceRecoveryRequired#emailMask()}), retry with backoff
 * ({@link SilentNonceServerError}, {@link SilentNonceTimeout}), or
 * abandon the refresh and seed a fresh session
 * ({@link SessionUnseeded}, {@link TokenExchangeFailed}).
 *
 * @see SilentNonceRecoveryRequired
 * @see SilentNonceClientError
 * @see SilentNonceServerError
 * @see SilentNonceTimeout
 * @see TokenExchangeFailed
 * @see SessionUnseeded
 */
public sealed abstract class WhatsAppFacebookGraphQlException
        extends WhatsAppLinkedException
        permits WhatsAppFacebookGraphQlException.SessionUnseeded,
                WhatsAppFacebookGraphQlException.SilentNonceClientError,
                WhatsAppFacebookGraphQlException.SilentNonceRecoveryRequired,
                WhatsAppFacebookGraphQlException.SilentNonceServerError,
                WhatsAppFacebookGraphQlException.SilentNonceTimeout,
                WhatsAppFacebookGraphQlException.TokenExchangeFailed {

    /**
     * Constructs a new Facebook GraphQL exception with the specified detail message.
     *
     * @param message the detail message describing the Facebook GraphQL refresh failure
     */
    protected WhatsAppFacebookGraphQlException(String message) {
        super(message);
    }

    /**
     * Constructs a new Facebook GraphQL exception with the specified detail message and cause.
     *
     * @param message the detail message describing the Facebook GraphQL refresh failure
     * @param cause   the underlying cause of this exception
     */
    protected WhatsAppFacebookGraphQlException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns
     * {@link WhatsAppLinkedClientErrorResult#DISCARD}: a credential refresh failure
     * scopes to the click-to-WhatsApp ads surface and leaves the encrypted
     * messaging channel intact.
     */
    @Override
    public WhatsAppLinkedClientErrorResult toErrorResult() {
        return WhatsAppLinkedClientErrorResult.DISCARD;
    }

    /**
     * Thrown when the relay refuses the silent-nonce path and instead
     * dispatches a recovery email the user must confirm before the next
     * refresh can succeed.
     *
     * <p>Carries the masked email address the relay routed the recovery
     * code to; the caller surfaces it to the UI as the "we sent a code
     * to ..." disclosure so the user can complete the recovery flow.
     */
    public static final class SilentNonceRecoveryRequired extends WhatsAppFacebookGraphQlException {
        /**
         * The masked email address the relay dispatched the recovery
         * code to.
         */
        private final String emailMask;

        /**
         * Constructs a new recovery-required exception.
         *
         * @param emailMask the masked recovery-email address; never
         *                  {@code null}
         * @throws NullPointerException if {@code emailMask} is
         *                              {@code null}
         */
        public SilentNonceRecoveryRequired(String emailMask) {
            super("Silent-nonce refresh declined; recovery email dispatched to " + Objects.requireNonNull(emailMask, "emailMask cannot be null"));
            this.emailMask = emailMask;
        }

        /**
         * Returns the masked recovery-email address.
         *
         * @return the email mask; never {@code null}
         */
        public String emailMask() {
            return emailMask;
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
            var that = (SilentNonceRecoveryRequired) obj;
            return Objects.equals(this.emailMask, that.emailMask);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(emailMask);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "WhatsAppFacebookGraphQlException.SilentNonceRecoveryRequired[emailMask=" + emailMask + ']';
        }
    }

    /**
     * Thrown when the relay rejected the silent-nonce request with a
     * documented {@code 4xx} error envelope.
     *
     * <p>A {@code 4xx} response is a terminal client-side rejection: the
     * caller should not retry without first changing the request shape
     * (typically by completing the interactive recovery flow). The error
     * code and optional human-readable text are surfaced so callers can
     * log or display the relay's classification.
     */
    public static final class SilentNonceClientError extends WhatsAppFacebookGraphQlException {
        /**
         * The numeric server-side error code in the {@code 4xx} range.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text supplied by the relay.
         */
        private final String errorText;

        /**
         * Constructs a new client-error exception.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be
         *                  {@code null}
         */
        public SilentNonceClientError(int errorCode, String errorText) {
            super("Silent-nonce refresh rejected by relay: code=" + errorCode + ", text=" + errorText);
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code in the {@code 4xx} range
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or empty
         *         when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
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
            var that = (SilentNonceClientError) obj;
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
            return "WhatsAppFacebookGraphQlException.SilentNonceClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Thrown when the relay reported a transient {@code 5xx} failure
     * while processing the silent-nonce request.
     *
     * <p>A {@code 5xx} response is transient: the caller can retry with
     * exponential backoff. The error code and optional human-readable
     * text are preserved so callers can log the relay's classification.
     */
    public static final class SilentNonceServerError extends WhatsAppFacebookGraphQlException {
        /**
         * The numeric server-side error code in the {@code 5xx} range,
         * or {@code 0} when the relay reply was unparseable.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text supplied by the relay.
         */
        private final String errorText;

        /**
         * Constructs a new server-error exception.
         *
         * @param errorCode the numeric error code, or {@code 0} when the
         *                  reply was unparseable
         * @param errorText the optional human-readable text; may be
         *                  {@code null}
         */
        public SilentNonceServerError(int errorCode, String errorText) {
            super("Silent-nonce refresh failed at relay: code=" + errorCode + ", text=" + errorText);
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code in the {@code 5xx} range, or {@code 0}
         *         when the relay reply was unparseable
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or empty
         *         when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
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
            var that = (SilentNonceServerError) obj;
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
            return "WhatsAppFacebookGraphQlException.SilentNonceServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Thrown when the silent nonce, granted by the relay over a separate
     * push notification, did not arrive within the configured wait
     * window.
     *
     * <p>Carries the duration the refresh thread waited for the push
     * before giving up. A timeout typically indicates a network blip or
     * a stalled relay; the caller can retry the full refresh on a fresh
     * round.
     */
    public static final class SilentNonceTimeout extends WhatsAppFacebookGraphQlException {
        /**
         * The wait duration that elapsed without the push arriving.
         */
        private final Duration waited;

        /**
         * Constructs a new timeout exception.
         *
         * @param waited the wait duration that elapsed; never
         *               {@code null}
         * @throws NullPointerException if {@code waited} is
         *                              {@code null}
         */
        public SilentNonceTimeout(Duration waited) {
            super("Silent-nonce push did not arrive within " + Objects.requireNonNull(waited, "waited cannot be null"));
            this.waited = waited;
        }

        /**
         * Constructs a new timeout exception with the specified cause.
         *
         * @param waited the wait duration that elapsed; never
         *               {@code null}
         * @param cause  the underlying cause of the timeout, typically
         *               an {@link InterruptedException}
         * @throws NullPointerException if {@code waited} is
         *                              {@code null}
         */
        public SilentNonceTimeout(Duration waited, Throwable cause) {
            super("Silent-nonce push did not arrive within " + Objects.requireNonNull(waited, "waited cannot be null"), cause);
            this.waited = waited;
        }

        /**
         * Returns the wait duration that elapsed without the push
         * arriving.
         *
         * @return the wait duration; never {@code null}
         */
        public Duration waited() {
            return waited;
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
            var that = (SilentNonceTimeout) obj;
            return Objects.equals(this.waited, that.waited);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(waited);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "WhatsAppFacebookGraphQlException.SilentNonceTimeout[waited=" + waited + ']';
        }
    }

    /**
     * Thrown when the silent-nonce stage succeeded but the downstream
     * call that trades the fresh nonce for a Facebook access token
     * returned no session.
     *
     * <p>This indicates a relay-side anomaly at the token-exchange
     * surface: the silent nonce was issued but the access-token endpoint
     * declined to mint a session for it. Callers cannot reuse the same
     * nonce twice; the next refresh must start from a new silent-nonce
     * request.
     */
    public static final class TokenExchangeFailed extends WhatsAppFacebookGraphQlException {
        /**
         * Constructs a new token-exchange-failed exception with a default
         * message.
         */
        public TokenExchangeFailed() {
            super("Token exchange returned no session for the fresh silent nonce");
        }

        /**
         * Constructs a new token-exchange-failed exception with the
         * specified detail message.
         *
         * @param message the detail message describing the failure
         */
        public TokenExchangeFailed(String message) {
            super(message);
        }

        /**
         * Constructs a new token-exchange-failed exception with the
         * specified detail message and cause.
         *
         * @param message the detail message describing the failure
         * @param cause   the underlying cause of the failure
         */
        public TokenExchangeFailed(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when a refresh was attempted before the Facebook GraphQL session has
     * any seed credentials to refresh.
     *
     * <p>The silent-nonce path requires a previously-established access
     * token to scope its refresh against; on a fresh session, or after
     * the store has been wiped, the caller must first run the
     * interactive recovery flow once via
     * {@code queryAccessTokenAndSessionCookies(code, jid)} to seed the
     * canonical access-token slot.
     */
    public static final class SessionUnseeded extends WhatsAppFacebookGraphQlException {
        /**
         * Constructs a new session-unseeded exception with a default
         * message.
         */
        public SessionUnseeded() {
            super("Facebook GraphQL refresh requires an existing access-token seed; call queryAccessTokenAndSessionCookies(code, jid) first");
        }

        /**
         * Constructs a new session-unseeded exception with the specified
         * detail message.
         *
         * @param message the detail message describing the missing seed
         */
        public SessionUnseeded(String message) {
            super(message);
        }
    }
}
