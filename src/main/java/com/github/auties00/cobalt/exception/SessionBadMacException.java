package com.github.auties00.cobalt.exception;

/**
 * A security exception that is thrown when Message Authentication Code (MAC) validation fails
 * during active session communication with the WhatsApp server.
 * <p>
 * This exception indicates a critical cryptographic failure in the protocol stream, typically occurring when:
 * <ul>
 *   <li>The session's read/write counter goes out of sync with the server</li>
 *   <li>Encrypted frames cannot be properly authenticated during stream communication</li>
 *   <li>The server sends a "bad-mac" error node in response to invalid message authentication</li>
 * </ul>
 * <p>
 *
 * @see SessionConflictException
 */
public class SessionBadMacException extends RuntimeException {
    /**
     * Constructs a new session bad MAC exception.
     * <p>
     * This constructor is invoked when the WhatsApp stream error handler detects
     * a "bad-mac" error node from the server, indicating that MAC validation has
     * failed for the current session.
     */
    public SessionBadMacException() {
        super();
    }
}
