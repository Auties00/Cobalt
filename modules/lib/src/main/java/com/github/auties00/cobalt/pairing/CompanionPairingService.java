package com.github.auties00.cobalt.pairing;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientVerificationHandler;
import com.github.auties00.cobalt.exception.linked.mobile.WhatsAppRegistrationException;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSignalStore;

import java.security.GeneralSecurityException;

/**
 * Drives the companion side of WhatsApp's alt-device-linking (eight-character pairing-code) flow.
 *
 * <p>One instance is created per {@link LinkedWhatsAppClient} when the client is configured with a
 * {@link LinkedWhatsAppClientVerificationHandler.Web.PairingCode} handler and a phone number is set on
 * the store. The service is shared between the IQ stream handler that triggers
 * {@code companion_hello} and the notification stream handler that consumes the resulting
 * {@code primary_hello} and {@code refresh_code} notifications.
 *
 * <p>The flow lets the user attach a new device by typing an eight-character code into their
 * primary phone instead of scanning a QR code. The lifecycle is:
 * <ol>
 *   <li>{@link #start()} generates the code, ships {@code companion_hello}, and delivers the code
 *       to {@link LinkedWhatsAppClientVerificationHandler.Web#handle(String)}.</li>
 *   <li>The user types the code on their primary device.</li>
 *   <li>{@link #handlePrimaryHello(byte[], byte[], byte[])} runs the {@code companion_finish}
 *       algorithm, ships the IQ, and persists the derived ADV master secret.</li>
 * </ol>
 *
 * @implSpec
 * Implementations must serialise every pairing-state transition so concurrent stream-handler
 * threads cannot observe a half-applied handshake, and must publish the pairing code only after
 * the server has acknowledged {@code companion_hello}.
 */
public interface CompanionPairingService {
    /**
     * Returns the pairing code published by the most recent {@link #start()} invocation.
     *
     * <p>This is a synchronous accessor for callers that prefer polling over the
     * {@link LinkedWhatsAppClientVerificationHandler.Web#handle(String)} push delivery. It returns
     * {@code null} before {@link #start()} runs and after the service clears its cache.
     *
     * @implSpec
     * Implementations must return {@code null} until {@link #start()} has published a code.
     *
     * @return the eight-character pairing code, or {@code null} when none is available
     */
    String pairingCode();

    /**
     * Returns whether this service should drive the pair-device flow.
     *
     * <p>This is consulted by the pair-device IQ stream handler to decide whether to route the
     * flow through pairing-code logic (this service) or to fall back to the QR path.
     *
     * @implSpec
     * Implementations must report {@code true} only when the configured verification handler is a
     * {@link LinkedWhatsAppClientVerificationHandler.Web.PairingCode} and the store carries a phone
     * number.
     *
     * @return whether the alt-device-linking flow should run
     */
    boolean isEnabled();

    /**
     * Generates a fresh pairing code, ships {@code companion_hello}, and publishes the code to the
     * verification handler.
     *
     * <p>This is the entry point of the companion-side handshake. It must be invoked before any
     * {@code primary_hello} or {@code refresh_code} notification is routed to this service. On
     * success the user sees the eight-character code via
     * {@link LinkedWhatsAppClientVerificationHandler.Web#handle(String)} and can type it on their primary
     * device. The pairing code is published only after the server has acknowledged the IQ, so the
     * caller never sees a code that the server silently rejected.
     *
     * @implSpec
     * Implementations must publish the code only after the {@code companion_hello} IQ is
     * acknowledged with a server-issued ref.
     *
     * @throws IllegalStateException         if {@link #isEnabled()} returned {@code false}
     * @throws GeneralSecurityException      if the JCE provider rejects any step of the code
     *                                       derivation or if the server fails to return a ref
     * @throws WhatsAppRegistrationException if the server answered with an {@code <iq type="error">}
     *                                       stanza
     */
    void start() throws GeneralSecurityException;

    /**
     * Handles a {@code refresh_code} notification.
     *
     * <p>This is invoked from the notification stream handler when the primary device asks the
     * relay to refresh the pairing code presentation, for example when the user re-opens the
     * linked-devices screen. It is a no-op when the cached ref matches the notification ref and
     * discards the notification otherwise.
     *
     * @implSpec
     * Implementations must treat a {@code null} or mismatched ref as a no-op.
     *
     * @param notificationRef the ref carried by the notification, or {@code null} if absent
     */
    void handleRefreshCode(byte[] notificationRef);

    /**
     * Handles a {@code primary_hello} notification by running the {@code companion_finish}
     * algorithm and shipping the resulting IQ.
     *
     * <p>This is invoked from the notification stream handler when the primary device confirms the
     * typed code. On the first successful call the store's ADV secret is persisted via
     * {@link LinkedWhatsAppSignalStore#setAdvSecretKey}, concluding the handshake. A repeat notification
     * arriving after the handshake has already finished triggers an ADV-secret regeneration and a
     * rerun, up to a bounded number of attempts per code.
     *
     * @implSpec
     * Implementations must validate the echoed ref against the cached {@code companion_hello} ref,
     * must reject an expired code, and must bound the number of {@code primary_hello} attempts per
     * code.
     *
     * @param wrappedPrimaryEphemeralPub the 32-byte salt {@code ||} 16-byte counter {@code ||}
     *                                   AES-CTR ciphertext payload from the notification
     * @param primaryIdentityPublic      the primary's long-term identity public key
     * @param notificationRef            the {@code link_code_pairing_ref} echoed by the
     *                                   notification
     * @throws IllegalStateException    if the service is not awaiting a {@code primary_hello}
     * @throws GeneralSecurityException if any of the cached preconditions fail (ref absent or
     *                                  mismatched, hello cache missing, code expired, attempts
     *                                  exceeded), or if the JCE provider rejects a step
     */
    void handlePrimaryHello(byte[] wrappedPrimaryEphemeralPub, byte[] primaryIdentityPublic, byte[] notificationRef)
            throws GeneralSecurityException;
}
