package com.github.auties00.cobalt.exception.linked;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;

/**
 * Sealed root for failures that invalidate the active WhatsApp session
 * as a whole.
 *
 * <p>A session in Cobalt is the encrypted Noise protocol channel that
 * sits on top of the WebSocket connection: it carries the registration
 * identity, the cipher state used for every frame, and the authentication
 * of the current device with the server. The nested subtypes enumerate
 * the distinct ways the server can abort a logged-in stream: an
 * authentication-tag mismatch ({@link BadMac}), the connection being torn
 * down ({@link Closed}), another instance taking over the slot
 * ({@link Conflict}), the device being logged out ({@link LoggedOut}),
 * the account being banned ({@link Banned}), and the server asking for an
 * immediate reconnect ({@link Reconnect}).
 *
 * @apiNote
 * Raised when the encrypted channel can no longer be used; every subtype
 * returns a session-control {@link WhatsAppLinkedClientErrorResult} from
 * {@link #toErrorResult()} rather than
 * {@link WhatsAppLinkedClientErrorResult#DISCARD}, so a configured
 * {@code WhatsAppClientErrorHandler} reconnects ({@link BadMac},
 * {@link Closed}, {@link Reconnect}), disconnects ({@link Conflict}), logs
 * the device out ({@link LoggedOut}), or treats the account as banned
 * ({@link Banned}).
 *
 * @implNote
 * This implementation maps each session-level subtype to its own
 * session-control result; none map to
 * {@link WhatsAppLinkedClientErrorResult#DISCARD} because each one leaves the
 * Noise channel in an unusable state.
 *
 * @see BadMac
 * @see Closed
 * @see Conflict
 * @see LoggedOut
 * @see Banned
 * @see Reconnect
 */
public sealed abstract class WhatsAppSessionException
        extends WhatsAppLinkedException
        permits WhatsAppSessionException.BadMac,
                WhatsAppSessionException.Banned,
                WhatsAppSessionException.Closed,
                WhatsAppSessionException.Conflict,
                WhatsAppSessionException.LoggedOut,
                WhatsAppSessionException.Reconnect {

    /**
     * Constructs a new session exception with the specified detail message.
     *
     * @param message the detail message describing the session error
     */
    protected WhatsAppSessionException(String message) {
        super(message);
    }

    /**
     * Constructs a new session exception with the specified detail message and cause.
     *
     * @param message the detail message describing the session error
     * @param cause   the underlying cause of this exception
     */
    protected WhatsAppSessionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Thrown when the authentication code on a Noise frame did not
     * validate.
     *
     * <p>Each Noise frame carries an AEAD authentication tag computed over
     * the ciphertext. A mismatch typically means the keys at the two ends
     * of the channel drifted out of sync (often after a brief interruption
     * that prevented a key rotation from completing) or that the bytes
     * were modified in transit.
     */
    public static final class BadMac extends WhatsAppSessionException {
        /**
         * Constructs a new bad MAC exception with a default message.
         */
        public BadMac() {
            super("Bad MAC: message authentication code validation failed");
        }

        /**
         * Constructs a new bad MAC exception with the specified message.
         *
         * @param message the detail message describing the MAC failure
         */
        public BadMac(String message) {
            super(message);
        }

        /**
         * Constructs a new bad MAC exception with the specified message and cause.
         *
         * @param message the detail message describing the MAC failure
         * @param cause   the underlying cause of the MAC failure
         */
        public BadMac(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation returns
         * {@link WhatsAppLinkedClientErrorResult#RECONNECT}: a Noise MAC drift
         * clears once the connection is re-opened with fresh cipher state,
         * collapsing to WhatsApp Web's {@code CLOSE_SOCKET} resolution.
         */
        @Override
        public WhatsAppLinkedClientErrorResult toErrorResult() {
            return WhatsAppLinkedClientErrorResult.RECONNECT;
        }
    }

    /**
     * Thrown when an operation is attempted on a session whose underlying
     * connection has already been closed.
     *
     * <p>The closure may have been initiated by the application, by the
     * server (typically for maintenance or load balancing), or by the
     * keep-alive watchdog after the socket stopped responding.
     */
    public static final class Closed extends WhatsAppSessionException {
        /**
         * Constructs a new session closed exception with a default message.
         */
        public Closed() {
            super("Session closed: cannot perform operation on closed connection");
        }

        /**
         * Constructs a new session closed exception with the specified message.
         *
         * @param message the detail message describing why the session is closed
         */
        public Closed(String message) {
            super(message);
        }

        /**
         * Constructs a new session closed exception with the specified message and cause.
         *
         * @param message the detail message describing why the session is closed
         * @param cause   the underlying cause of the session closure
         */
        public Closed(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation returns
         * {@link WhatsAppLinkedClientErrorResult#RECONNECT}: the reader loop
         * re-opens the closed connection with fresh Noise state rather than
         * abandoning the session.
         */
        @Override
        public WhatsAppLinkedClientErrorResult toErrorResult() {
            return WhatsAppLinkedClientErrorResult.RECONNECT;
        }
    }

    /**
     * Thrown when the server signals that another instance has taken over
     * the slot for this device kind.
     *
     * <p>WhatsApp allows only one active web client (or one active mobile
     * client) at a time. When a second one logs in, the server sends a
     * conflict stream error to the existing session, after which the local
     * credentials cannot be reused without re-pairing.
     */
    public static final class Conflict extends WhatsAppSessionException {
        /**
         * Constructs a new session conflict exception with a default message.
         */
        public Conflict() {
            super("Session conflict: another device has taken over the connection");
        }

        /**
         * Constructs a new session conflict exception with the specified message.
         *
         * @param message the detail message describing the conflict
         */
        public Conflict(String message) {
            super(message);
        }

        /**
         * Constructs a new session conflict exception with the specified message and cause.
         *
         * @param message the detail message describing the conflict
         * @param cause   the underlying cause of the conflict
         */
        public Conflict(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation returns
         * {@link WhatsAppLinkedClientErrorResult#DISCONNECT}: a {@code replaced}
         * conflict means another device took the slot, so WhatsApp Web stops
         * the socket without resuming the connection loop.
         */
        @Override
        public WhatsAppLinkedClientErrorResult toErrorResult() {
            return WhatsAppLinkedClientErrorResult.DISCONNECT;
        }
    }

    /**
     * Thrown when the server reports that the account has been logged out
     * of this device.
     *
     * <p>The server pushes this notification when the user logs out from
     * another device or when WhatsApp itself revokes the session as part
     * of an enforcement flow.
     *
     * @apiNote
     * Raised on a server-driven logout; the stored credentials must be
     * cleared before the device can be paired again, so a configured
     * {@code WhatsAppClientErrorHandler} typically returns a log-out
     * verdict.
     */
    public static final class LoggedOut extends WhatsAppSessionException {
        /**
         * Constructs a new logged-out exception with a default message.
         */
        public LoggedOut() {
            super("Session invalidated: logged out by server");
        }

        /**
         * Constructs a new logged-out exception with the specified detail message.
         *
         * @param message the detail message describing the logout condition
         */
        public LoggedOut(String message) {
            super(message);
        }

        /**
         * Constructs a new logged-out exception with the specified detail message
         * and cause.
         *
         * @param message the detail message describing the logout condition
         * @param cause   the underlying cause of the logout
         */
        public LoggedOut(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation returns
         * {@link WhatsAppLinkedClientErrorResult#LOG_OUT}: the server revoked the
         * session, so the stored credentials are cleared before the device
         * can pair again.
         */
        @Override
        public WhatsAppLinkedClientErrorResult toErrorResult() {
            return WhatsAppLinkedClientErrorResult.LOG_OUT;
        }
    }

    /**
     * Thrown when the server refuses traffic for the account because it
     * has been banned.
     *
     * <p>A ban is a terminal state: the client cannot reconnect until the
     * ban is lifted or appealed through WhatsApp's support channels.
     *
     * @apiNote
     * Raised when the account is banned; reconnecting cannot recover it,
     * so a configured {@code WhatsAppClientErrorHandler} should return a
     * ban verdict rather than reconnect.
     */
    public static final class Banned extends WhatsAppSessionException {
        /**
         * Constructs a new banned exception with a default message.
         */
        public Banned() {
            super("Session invalidated: account banned by server");
        }

        /**
         * Constructs a new banned exception with the specified detail message.
         *
         * @param message the detail message describing the ban condition
         */
        public Banned(String message) {
            super(message);
        }

        /**
         * Constructs a new banned exception with the specified detail message and cause.
         *
         * @param message the detail message describing the ban condition
         * @param cause   the underlying cause of the ban
         */
        public Banned(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation returns {@link WhatsAppLinkedClientErrorResult#BAN}:
         * a banned account cannot reconnect until the ban is lifted, so the
         * session store is deleted rather than retried.
         */
        @Override
        public WhatsAppLinkedClientErrorResult toErrorResult() {
            return WhatsAppLinkedClientErrorResult.BAN;
        }
    }

    /**
     * Thrown when the server requests that the client drop the current
     * connection and immediately re-establish it.
     *
     * <p>The server uses this during load balancing, configuration
     * rollouts, or scheduled maintenance. Credentials remain valid and the
     * next connection attempt should succeed once the previous channel has
     * been torn down.
     *
     * @apiNote
     * Raised on a server-requested reconnect; a configured
     * {@code WhatsAppClientErrorHandler} should reconnect rather than log
     * out, since the credentials stay valid.
     */
    public static final class Reconnect extends WhatsAppSessionException {
        /**
         * Constructs a new reconnect exception with a default message.
         */
        public Reconnect() {
            super("Session requires reconnect");
        }

        /**
         * Constructs a new reconnect exception with the specified detail message.
         *
         * @param message the detail message describing the reconnect request
         */
        public Reconnect(String message) {
            super(message);
        }

        /**
         * Constructs a new reconnect exception with the specified detail message and cause.
         *
         * @param message the detail message describing the reconnect request
         * @param cause   the underlying cause of the reconnect request
         */
        public Reconnect(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation returns
         * {@link WhatsAppLinkedClientErrorResult#RECONNECT}: the credentials remain
         * valid, so the client drops the channel and immediately
         * re-establishes it, matching WhatsApp Web's stream code {@code 515}
         * path.
         */
        @Override
        public WhatsAppLinkedClientErrorResult toErrorResult() {
            return WhatsAppLinkedClientErrorResult.RECONNECT;
        }
    }
}
