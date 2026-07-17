package com.github.auties00.cobalt.client.linked;

import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;

/**
 * Produces a platform-specific device-attestation payload that the mobile
 * registration flow embeds into the outgoing request body.
 *
 * <p>The permitted sub-interfaces are aligned with the registering
 * device: {@link Android} produces
 * {@link LinkedWhatsAppClientDeviceIntegrityResult.PlayIntegrity} results and
 * {@link Ios} produces {@link LinkedWhatsAppClientDeviceIntegrityResult.AppAttest}
 * results. Both sub-interfaces are {@code non-sealed} and
 * {@link FunctionalInterface functional}, so a target-typed lambda
 * suffices to supply one, and each covariantly narrows the return type of
 * {@link #mint(LinkedWhatsAppStore)} so the wrong result type cannot be produced
 * for a given platform.
 *
 * @apiNote
 * Cobalt cannot mint attestations itself, because Google Play Integrity
 * and Apple App Attest both require cryptographic material that only the
 * platform vendor's hardware and operating system can produce. This sealed
 * interface is the seam through which an embedding application delegates
 * the work to a real device it controls.
 *
 * @implSpec
 * Implementations are not required to be thread-safe; the registration
 * code invokes {@link #mint(LinkedWhatsAppStore)} sequentially from the thread
 * that drives the registration ceremony and never concurrently.
 *
 * @see LinkedWhatsAppClientDeviceIntegrityResult
 */
public sealed interface LinkedWhatsAppClientDeviceIntegrityProvider
        permits LinkedWhatsAppClientDeviceIntegrityProvider.Android,
                LinkedWhatsAppClientDeviceIntegrityProvider.Ios {

    /**
     * Produces an attestation result bound to the current registration
     * request.
     *
     * <p>The registration code appends the result's components to the
     * request body immediately after this call returns. Implementations
     * read whatever stable credential they need (phone number, identity
     * keys, device identifier, FDID) from the supplied store and return a
     * freshly minted token; a throw aborts registration.
     *
     * @apiNote
     * Implementations that talk to a remote minter should prefer
     * short-lived, per-request tokens over cached ones, since the server
     * rebinds the attestation nonce to each request body.
     *
     * @param store the live registration store carrying the identity keys
     *              and phone number the attestation is bound to; never
     *              {@code null}
     * @return the platform-specific attestation result; never
     *         {@code null}
     * @throws RuntimeException if the supplier cannot produce a token; the
     *                          registration code treats any throw as a
     *                          fatal registration failure
     */
    LinkedWhatsAppClientDeviceIntegrityResult mint(LinkedWhatsAppStore store);

    /**
     * A supplier that mints Google Play Integrity verdicts for the Android
     * mobile registration flow.
     *
     * <p>Wired in via the Android registration variants on
     * {@link LinkedWhatsAppClientBuilder}. The narrowed return type of
     * {@link #mint(LinkedWhatsAppStore)} guarantees at compile time that an
     * Android supplier can only produce a
     * {@link LinkedWhatsAppClientDeviceIntegrityResult.PlayIntegrity}.
     */
    @FunctionalInterface
    non-sealed interface Android extends LinkedWhatsAppClientDeviceIntegrityProvider {
        /**
         * {@inheritDoc}
         *
         * @implSpec
         * Mints a Play Integrity verdict bound to the current registration
         * request, typically by calling out to a physical Android device.
         *
         * @return the Play Integrity attestation result; never
         *         {@code null}
         */
        @Override
        LinkedWhatsAppClientDeviceIntegrityResult.PlayIntegrity mint(LinkedWhatsAppStore store);
    }

    /**
     * A supplier that mints Apple App Attest assertions for the iOS mobile
     * registration flow.
     *
     * <p>Wired in via the iOS registration variants on
     * {@link LinkedWhatsAppClientBuilder}. The narrowed return type of
     * {@link #mint(LinkedWhatsAppStore)} guarantees at compile time that an iOS
     * supplier can only produce a
     * {@link LinkedWhatsAppClientDeviceIntegrityResult.AppAttest}.
     */
    @FunctionalInterface
    non-sealed interface Ios extends LinkedWhatsAppClientDeviceIntegrityProvider {
        /**
         * {@inheritDoc}
         *
         * @implSpec
         * Mints an App Attest assertion bound to the current registration
         * request, typically by calling out to a physical iOS device.
         *
         * @return the App Attest attestation result; never {@code null}
         */
        @Override
        LinkedWhatsAppClientDeviceIntegrityResult.AppAttest mint(LinkedWhatsAppStore store);
    }
}
