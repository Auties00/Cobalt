package com.github.auties00.cobalt.exception.linked;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;

/**
 * Thrown when a server-pushed passkey demand cannot be satisfied and the session must end.
 *
 * <p>The WhatsApp server can demand a passkey in two situations: an integrity checkpoint on a
 * connected linked session (demanding a passkey assertion, or in principle a CAPTCHA, before the
 * session may continue), and a {@code passkey_prologue_request} that begins Shortcake passkey
 * companion linking. When no passkey authenticator is configured, the configured authenticator
 * declines or fails, the relying party rejects the response, or the challenge is of a type the client
 * cannot answer, the only remaining outcome is to log the session out, mirroring the genuine client
 * whose checkpoint modal offers no escape other than passing the challenge or logging out.
 *
 * @see com.github.auties00.cobalt.wire.linked.integrity.IntegrityChallenge
 * @see com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientVerificationHandler.Web.Passkey
 */
public final class WhatsAppIntegrityChallengeException extends WhatsAppLinkedException {
    /**
     * Constructs an exception with a default message.
     */
    public WhatsAppIntegrityChallengeException() {
        super("Integrity checkpoint could not be satisfied");
    }

    /**
     * Constructs an exception with the specified detail message.
     *
     * @param message the detail message describing why the checkpoint could not be satisfied
     */
    public WhatsAppIntegrityChallengeException(String message) {
        super(message);
    }

    /**
     * Constructs an exception with the specified detail message and cause.
     *
     * @param message the detail message describing why the checkpoint could not be satisfied
     * @param cause   the underlying cause
     */
    public WhatsAppIntegrityChallengeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote An unsatisfiable integrity checkpoint leaves the session in the state the server
     * refuses to serve, so this implementation returns {@link WhatsAppLinkedClientErrorResult#LOG_OUT}
     * to invalidate the session, matching the genuine client's only non-passing outcome.
     *
     * @return {@link WhatsAppLinkedClientErrorResult#LOG_OUT}
     */
    @Override
    public WhatsAppLinkedClientErrorResult toErrorResult() {
        return WhatsAppLinkedClientErrorResult.LOG_OUT;
    }
}
