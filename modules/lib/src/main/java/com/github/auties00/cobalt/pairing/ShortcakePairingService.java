package com.github.auties00.cobalt.pairing;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;

import java.security.GeneralSecurityException;

/**
 * Drives the companion side of WhatsApp's Shortcake passkey device-linking ceremony.
 *
 * <p>Shortcake is the passkey-authenticated alternative to QR-code and pairing-code linking. The
 * companion proves control of the account with a WebAuthn assertion (a "prologue"), then runs a
 * commitment-based short-authentication-string handshake with the primary device to establish an
 * encrypted channel over which it sends its pairing request. One instance is created per
 * {@link LinkedWhatsAppClient} configured with a
 * {@link com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientVerificationHandler.Web.Passkey}
 * handler, and it is shared between the IQ stream handler that begins the ceremony and the
 * notification stream handler that feeds it the primary's reply.
 *
 * <p>The lifecycle is:
 * <ol>
 *   <li>{@link #start()} asserts the passkey, ships the prologue, and waits for the primary.</li>
 *   <li>{@link #handlePrimaryEphemeralIdentity(byte[])} runs the key agreement, derives and confirms
 *       the verification code, and ships the encrypted pairing request.</li>
 * </ol>
 *
 * @implNote This is the passkey sibling of {@link CompanionPairingService}; it follows the same
 * instance-scoped, lock-serialised shape so multiple Cobalt clients can run independent ceremonies.
 */
public interface ShortcakePairingService {
    /**
     * Returns whether this service should drive linking for the current client.
     *
     * @implSpec
     * Implementations return {@code true} only when the configured verification handler is a passkey
     * handler and a passkey authenticator is available to assert with.
     *
     * @return {@code true} when Shortcake passkey linking is configured
     */
    boolean isEnabled();

    /**
     * Asserts the passkey, ships the prologue IQ, and transitions to waiting for the primary device.
     *
     * @throws GeneralSecurityException if a cryptographic step fails
     * @throws IllegalStateException    if the service is not enabled or is already mid-ceremony
     */
    void start() throws GeneralSecurityException;

    /**
     * Continues the ceremony with the primary's ephemeral identity delivered by the
     * {@code crsc_continuation} notification.
     *
     * <p>Reveals the companion nonce, derives the verification code and encryption key, asks the
     * configured handler to confirm the code, then ships the encrypted pairing request.
     *
     * @param primaryEphemeralIdentity the serialised {@code PrimaryEphemeralIdentity} from the
     *                                 notification
     * @throws GeneralSecurityException if a cryptographic step fails
     */
    void handlePrimaryEphemeralIdentity(byte[] primaryEphemeralIdentity) throws GeneralSecurityException;
}
