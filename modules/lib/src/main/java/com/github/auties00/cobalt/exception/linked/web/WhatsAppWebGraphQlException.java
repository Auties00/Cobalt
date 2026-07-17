package com.github.auties00.cobalt.exception.linked.web;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;

/**
 * Sealed root for failures encountered while refreshing the WhatsApp Web
 * WhatsApp Web GraphQL session credentials.
 *
 * <p>The WhatsApp Web GraphQL transport authenticates against the WhatsApp Web browser
 * surface with a session cookie and an {@code lsd} anti-CSRF token. The
 * refresh pipeline trades the durable canonical access token seeded at
 * pairing for a fresh cookie/token pair through the
 * {@code /auth/token/} exchange. Each stage has its own documented
 * failure mode: the page bootstrap that yields the {@code lsd} token can
 * raise an HTTP failure ({@link LsdFetchFailed}), the canonical exchange
 * itself can be rejected by the server ({@link ExchangeFailed}), or the
 * durable canonical-access-token seed needed to start the refresh can be
 * absent ({@link SessionUnseeded}).
 *
 * @apiNote
 * Every subtype returns {@link WhatsAppLinkedClientErrorResult#DISCARD} from
 * {@link #toErrorResult()}: a relay refresh failure scopes to the
 * business-catalog/order surface and does not invalidate the underlying
 * Noise messaging channel. Pattern-match
 * the concrete subtype to decide whether to retry the refresh
 * ({@link LsdFetchFailed}, {@link ExchangeFailed}) or to abandon the
 * refresh and require a fresh pairing ({@link SessionUnseeded}).
 *
 * @see LsdFetchFailed
 * @see ExchangeFailed
 * @see SessionUnseeded
 */
public sealed abstract class WhatsAppWebGraphQlException
        extends WhatsAppWebException
        permits WhatsAppWebGraphQlException.ExchangeFailed,
                WhatsAppWebGraphQlException.LsdFetchFailed,
                WhatsAppWebGraphQlException.SessionUnseeded {

    /**
     * Constructs a new relay exception with the specified detail message.
     *
     * @param message the detail message describing the WhatsApp Web GraphQL refresh failure
     */
    protected WhatsAppWebGraphQlException(String message) {
        super(message);
    }

    /**
     * Constructs a new relay exception with the specified detail message and cause.
     *
     * @param message the detail message describing the WhatsApp Web GraphQL refresh failure
     * @param cause   the underlying cause of this exception
     */
    protected WhatsAppWebGraphQlException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns
     * {@link WhatsAppLinkedClientErrorResult#DISCARD}: a credential refresh failure
     * scopes to the catalog and order surfaces and leaves the encrypted
     * messaging channel intact.
     */
    @Override
    public WhatsAppLinkedClientErrorResult toErrorResult() {
        return WhatsAppLinkedClientErrorResult.DISCARD;
    }

    /**
     * Thrown when the {@code lsd} anti-CSRF token bootstrap HTTP call
     * raised an underlying transport failure.
     *
     * <p>The wrapped {@link Throwable} carries the originating failure
     * (typically an {@link java.io.IOException} or a JSON parse error
     * from the bootstrap page). Callers can retry the refresh with
     * exponential backoff.
     */
    public static final class LsdFetchFailed extends WhatsAppWebGraphQlException {
        /**
         * Constructs a new LSD-fetch-failed exception wrapping the
         * underlying transport failure.
         *
         * @param cause the underlying transport failure; never
         *              {@code null}
         */
        public LsdFetchFailed(Throwable cause) {
            super("Failed to fetch the lsd anti-CSRF token from the WhatsApp Web bootstrap", cause);
        }

        /**
         * Constructs a new LSD-fetch-failed exception with the specified
         * detail message and cause.
         *
         * @param message the detail message describing the failure
         * @param cause   the underlying transport failure
         */
        public LsdFetchFailed(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when the canonical {@code /auth/token/} exchange that
     * trades the durable canonical access token for a fresh session
     * cookie and {@code lsd} token was rejected by the server.
     *
     * <p>The exchange returns a boolean accept/reject; this exception is
     * raised on a server-reported rejection. Callers can retry but
     * repeated rejections typically mean the durable seed has been
     * revoked and a fresh pairing is required.
     */
    public static final class ExchangeFailed extends WhatsAppWebGraphQlException {
        /**
         * Constructs a new exchange-failed exception with a default
         * message.
         */
        public ExchangeFailed() {
            super("Canonical /auth/token/ exchange rejected by server");
        }

        /**
         * Constructs a new exchange-failed exception with the specified
         * detail message.
         *
         * @param message the detail message describing the failure
         */
        public ExchangeFailed(String message) {
            super(message);
        }

        /**
         * Constructs a new exchange-failed exception with the specified
         * detail message and cause.
         *
         * @param message the detail message describing the failure
         * @param cause   the underlying cause of the failure
         */
        public ExchangeFailed(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when a refresh was attempted before the WhatsApp Web GraphQL session has
     * any canonical credentials to refresh.
     *
     * <p>The WhatsApp Web GraphQL refresh path requires the canonical access token
     * seeded at pairing on a WhatsApp Web client to scope its
     * {@code /auth/token/} exchange against; on a fresh session the
     * caller must first pair on a WhatsApp Web client or call
     * {@code establishWhatsAppWebGraphQlSession(cookie, lsd)} with credentials
     * extracted out of band from a browser session.
     */
    public static final class SessionUnseeded extends WhatsAppWebGraphQlException {
        /**
         * Constructs a new session-unseeded exception with a default
         * message.
         */
        public SessionUnseeded() {
            super("WhatsApp Web GraphQL refresh requires a canonical access-token seed; pair on a WhatsApp Web client or call establishWhatsAppWebGraphQlSession(cookie, lsd)");
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
