package com.github.auties00.cobalt.client.linked;

/**
 * The platform-specific device-attestation payload produced by a
 * {@link LinkedWhatsAppClientDeviceIntegrityProvider}.
 *
 * <p>The sealed hierarchy has one variant per attestation scheme:
 * {@link PlayIntegrity} for Android and {@link AppAttest} for iOS. Both
 * variants reject {@code null} components at construction so that
 * registration code can append every field to a request body without a
 * preliminary null check.
 *
 * @apiNote
 * Cobalt cannot mint attestation tokens itself, because Play Integrity
 * and App Attest both require platform-controlled cryptographic material
 * that is unavailable to a plain JVM. Produce instances through a
 * {@link LinkedWhatsAppClientDeviceIntegrityProvider} the embedding application
 * supplies, typically by delegating the minting to a real device it
 * controls.
 *
 * @see LinkedWhatsAppClientDeviceIntegrityProvider
 */
public sealed interface LinkedWhatsAppClientDeviceIntegrityResult
        permits LinkedWhatsAppClientDeviceIntegrityResult.PlayIntegrity,
                LinkedWhatsAppClientDeviceIntegrityResult.AppAttest {

    /**
     * The Android Play Integrity verdict and the three adjacent Google
     * Mobile Services tokens that accompany it.
     *
     * <p>Each component maps one-to-one to a form field that the Android
     * registration path appends to its request bodies: {@code gpia}
     * carries the base64url-encoded Play Integrity JWS signed by Google
     * over a server-rederived nonce, while {@code gg}, {@code gi}, and
     * {@code gp} carry the Gaia, Instance, and Package Manager tokens read
     * from Google Play Services. A supplier that cannot produce a given
     * token returns the empty string for it; the server tolerates the
     * empty value but treats it as a low-trust signal.
     *
     * @param gpia the Play Integrity verdict token, or empty string
     * @param gg   the value for the {@code _gg} form field, or empty
     *             string
     * @param gi   the value for the {@code _gi} form field, or empty
     *             string
     * @param gp   the value for the {@code _gp} form field, or empty
     *             string
     */
    record PlayIntegrity(String gpia, String gg, String gi, String gp)
            implements LinkedWhatsAppClientDeviceIntegrityResult {
        /**
         * Rejects {@code null} components so the contract stays uniform
         * with {@link AppAttest}.
         *
         * @throws NullPointerException if any component is {@code null}
         */
        public PlayIntegrity {
            if (gpia == null || gg == null || gi == null || gp == null) {
                throw new NullPointerException("PlayIntegrity components must not be null");
            }
        }
    }

    /**
     * The Apple App Attest assertion together with the key identifier
     * under which it was produced.
     *
     * <p>The assertion binds the outgoing registration request to a key
     * pair that Apple's DeviceCheck attestation service has previously
     * certified as originating from a genuine iOS device. The server
     * rebuilds the assertion nonce from the request body and validates the
     * assertion against Apple's attestation root, so both fields must come
     * from the same attested key.
     *
     * @param attestation the base64-encoded App Attest assertion
     * @param keyId       the base64-encoded App Attest key identifier the
     *                    assertion was produced under
     */
    record AppAttest(String attestation, String keyId)
            implements LinkedWhatsAppClientDeviceIntegrityResult {
        /**
         * Rejects {@code null} components so the contract stays uniform
         * with {@link PlayIntegrity}.
         *
         * @throws NullPointerException if any component is {@code null}
         */
        public AppAttest {
            if (attestation == null || keyId == null) {
                throw new NullPointerException("AppAttest components must not be null");
            }
        }
    }
}
