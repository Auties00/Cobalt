package com.github.auties00.cobalt.exception;

/**
 * A session security exception that is thrown when a concurrent authentication conflict is detected
 * on the WhatsApp server.
 * <p>
 * This exception indicates that another client has successfully authenticated using the same session
 * credentials (cryptographic keys and device identity) that the current session is attempting to use.
 * This typically occurs when the user logs into the same account from another device or application instance.
 * <p>
 * When this exception is thrown, the current session is typically terminated immediately and cannot
 * be recovered. The user must re-authenticate to establish a new session. This behavior enforces
 * WhatsApp's policy of maintaining exclusive session ownership and prevents session hijacking attempts.
 * <p>
 * Unlike {@link SessionBadMacException}, which indicates cryptographic validation failures,
 * this exception specifically signals that the session credentials are valid but are being
 * actively used by another client instance.
 *
 * @see SessionBadMacException
 */
public class SessionConflictException extends RuntimeException {
    /**
     * Constructs a new session conflict exception.
     * <p>
     * This constructor is invoked when the WhatsApp stream error handler detects
     * either a "conflict" error node or a "replace" stream error from the server,
     * indicating that another client has taken over the session.
     */
    public SessionConflictException() {
        super();
    }
}