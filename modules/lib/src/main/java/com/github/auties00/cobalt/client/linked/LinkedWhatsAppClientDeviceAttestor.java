package com.github.auties00.cobalt.client.linked;

import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;

/**
 * Bridge between Cobalt and an embedder-supplied real mobile device for
 * the platform-attestation data the registration body needs but that
 * Cobalt cannot produce off-device on its own.
 *
 * <p>The native WhatsApp mobile clients embed a mix of hardware-rooted
 * and platform-service-derived values in each registration request:
 * <ol>
 *   <li>a <b>verdict token</b> minted by the platform vendor's
 *       attestation service (Google Play Integrity on Android, Apple
 *       App Attest on iOS), carried as form fields alongside the outer
 *       encrypted body;</li>
 *   <li>a <b>device-held signature</b> over the outer encrypted body
 *       produced by a key that lives in the platform's trusted
 *       execution environment (Android Keystore on Android, Apple
 *       Secure Enclave on iOS), together with the attestation
 *       certificate chain for that key;</li>
 *   <li>on Android only, the <b>install-source</b> string the app
 *       reports as its campaign download source.</li>
 * </ol>
 * The first two transitively chain back to a Google- or Apple-controlled
 * root certificate and therefore cannot be produced off-device: the
 * trusted execution environment holds the private key material, and the
 * vendor's backend signs the verdict. The install source can in
 * principle be faked, but Cobalt has no meaningful default. Cobalt
 * consequently cannot produce any of these values itself. This sealed
 * interface is the seam through which an embedding application supplies
 * them all from a real device it controls.
 *
 * <p>The push-notification token and the silent-push verification code
 * are produced separately by {@link LinkedWhatsAppClientDevicePushClient}, which is
 * platform-agnostic at the type level (FCM on Android, APNS on iOS, but
 * both expose the same surface).
 *
 * <p>The sealed hierarchy pins each attestor to a single platform at
 * compile time:
 * <ul>
 *   <li>{@link Android} is used with any
 *       {@link com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType#ANDROID}
 *       or {@link com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType#ANDROID_BUSINESS}
 *       device and produces {@link Android.PlayIntegrityData},
 *       {@link Android.KeystoreAttestation}, and a
 *       {@link Android.DownloadSource};</li>
 *   <li>{@link Ios} is used with any
 *       {@link com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType#IOS}
 *       or {@link com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType#IOS_BUSINESS}
 *       device and produces {@link Ios.AppAttestData}.</li>
 * </ul>
 * Both sub-interfaces are {@code non-sealed} so embedders can supply
 * implementations freely.
 *
 * <p>The record types each sub-interface consumes or produces are
 * nested directly under that sub-interface: the Android records live
 * under {@link Android}, the iOS records live under {@link Ios}. This
 * mirrors the natural one-platform-per-record ownership and keeps
 * platform-specific types out of the generic parent's namespace.
 *
 * @apiNote Implementations are not required to be stateless or
 *          thread-safe: the registration code calls each method
 *          sequentially from the thread that drives the registration
 *          ceremony and never concurrently.
 */
public sealed interface LinkedWhatsAppClientDeviceAttestor
        permits LinkedWhatsAppClientDeviceAttestor.Android, LinkedWhatsAppClientDeviceAttestor.Ios {

    /**
     * Attestor for an Android mobile registration.
     *
     * <p>Android requires the two attestation legs the native client
     * sends today: the Play Integrity verdict triple, and a TEE-backed
     * signature over the encrypted body together with that key's
     * certificate chain. It additionally carries an install-source
     * string. The platform-specific record and enum types
     * ({@link PlayIntegrityData}, {@link KeystoreAttestation},
     * {@link DownloadSource}) live directly under this interface.
     */
    non-sealed interface Android extends LinkedWhatsAppClientDeviceAttestor {
        /**
         * Low-trust Android attestor used when the embedder has not
         * supplied one. Returns {@link PlayIntegrityData#EMPTY} for the
         * Play Integrity verdict, {@link KeystoreAttestation#EMPTY} for
         * the body signature, and {@link DownloadSource#UNKNOWN} for
         * the install source; these are the same values an
         * off-Play-Services sideloaded device would report. The
         * registration server tolerates them but treats the request as a
         * low-trust signal.
         */
        Android NONE = new Android() {
            @Override
            public PlayIntegrityData attest(LinkedWhatsAppStore store) {
                return PlayIntegrityData.EMPTY;
            }

            @Override
            public KeystoreAttestation sign(LinkedWhatsAppStore store, byte[] encBody) {
                return KeystoreAttestation.EMPTY;
            }

            @Override
            public DownloadSource downloadSource() {
                return DownloadSource.UNKNOWN;
            }
        };

        /**
         * Produces the Google Play Integrity verdict bound to the current
         * registration request.
         *
         * <p>Called by the Android registration code exactly once per
         * outgoing {@code /v2/*} request that needs attestation (that is,
         * every endpoint except the two funnel-log endpoints). The four
         * fields of the result map directly to the {@code gpia},
         * {@code _gg}, {@code _gi}, and {@code _gp} form fields of the
         * outgoing body, before the body is AES-GCM-sealed into the outer
         * {@code ENC=} envelope.
         *
         * @param store the live registration store carrying the identity
         *              keys and phone number the verdict is bound to;
         *              never {@code null}
         * @return the Play Integrity data tuple; never {@code null}
         * @throws RuntimeException if the attestor cannot produce a
         *                          verdict; the registration code treats
         *                          any throw as a fatal registration
         *                          failure
         */
        PlayIntegrityData attest(LinkedWhatsAppStore store);

        /**
         * Signs the outer AES-GCM-encrypted body with a TEE-backed
         * Android Keystore key and returns both the signature and the
         * key's attestation certificate chain.
         *
         * <p>Called by the Android registration code after the outer
         * encryption step, on every request that needs attestation. The
         * signature bytes are hex-encoded and appended to the body as the
         * {@code H=} form field; the certificate chain is base64-encoded
         * and sent as the {@code Authorization:} request header. The
         * server re-derives the chain's expected attestation structure
         * and verifies that its root is the Google hardware attestation
         * root CA.
         *
         * @param store the live registration store; never {@code null}
         * @param encBody the base64 bytes of the outer {@code ENC=}
         *                envelope that the signature must cover; never
         *                {@code null}
         * @return the signature and certificate chain produced by the
         *         device's hardware-backed key; never {@code null}
         * @throws RuntimeException if the attestor cannot produce a
         *                          signature
         */
        KeystoreAttestation sign(LinkedWhatsAppStore store, byte[] encBody);

        /**
         * Returns the install-source value the registration body reports
         * inside {@code client_metrics.app_campaign_download_source}.
         *
         * <p>Live captures show {@code "google-play|unknown"} for a Play
         * Store install and {@code "unknown|unknown"} when the Play Store
         * package is absent (Huawei-style or sideloaded). Embedders
         * representing a stock Play-Store install should return
         * {@link DownloadSource#GOOGLE_PLAY}; embedders simulating a
         * sideload or Huawei-style device should return
         * {@link DownloadSource#UNKNOWN}.
         *
         * @return the install source enum; never {@code null}
         */
        DownloadSource downloadSource();

        /**
         * Install-source enumeration mapped one-to-one to the wire string
         * the native Android client emits inside
         * {@code client_metrics.app_campaign_download_source}.
         */
        enum DownloadSource {
            /**
             * The app was installed from the Google Play Store. The wire
             * value is {@code "google-play|unknown"}; the second segment
             * is the campaign sub-source which the native client reports
             * as {@code "unknown"} unless deep-link install attribution
             * was used.
             */
            GOOGLE_PLAY("google-play|unknown"),
            /**
             * The Play Store is not the install source (sideloaded APK,
             * Huawei AppGallery, third-party APK mirror, or any
             * non-Google channel). The wire value is
             * {@code "unknown|unknown"}.
             */
            UNKNOWN("unknown|unknown");

            private final String wireValue;

            DownloadSource(String wireValue) {
                this.wireValue = wireValue;
            }

            /**
             * Returns the literal string that goes into the
             * {@code app_campaign_download_source} field of the
             * {@code client_metrics} JSON.
             *
             * @return the wire-encoded install source
             */
            public String wireValue() {
                return wireValue;
            }
        }

        /**
         * Google Play Integrity verdict the Android registration code
         * appends to every attested request body.
         *
         * <p>Each component maps one-to-one to a form field the Android
         * registration code appends before AES-GCM sealing:
         * <ul>
         *   <li>{@code gpia} carries the Play Integrity JWS,
         *       base64url-encoded, signed by Google over a nonce the
         *       server re-derives from the request body;</li>
         *   <li>{@code _gg}, {@code _gi}, and {@code _gp} carry the
         *       Gaia, Instance, and Package Manager tokens the native
         *       client reads from Google Play Services;</li>
         *   <li>{@code _ge} carries the base64-encoded Play Protect
         *       environment flags (e.g. {@code {"sb":false,"sv":false}}
         *       for SafetyBrowsing and Safety Verify state);</li>
         *   <li>{@code _ga} carries the base64-encoded GMS / app metadata
         *       blob (e.g. {@code {"bi":"...","ap":642,"ai":1342,"mp":false}}
         *       for install backend identifier, app + ai versions, etc.).</li>
         * </ul>
         * Each component is never {@code null}; attestors that cannot
         * produce a particular token should return the empty string for
         * it, which the registration server tolerates but treats as a
         * low-trust signal.
         *
         * @param gpia the Play Integrity verdict token or empty string
         * @param gg the value for the {@code _gg} form field, or empty
         *           string
         * @param gi the value for the {@code _gi} form field, or empty
         *           string
         * @param gp the value for the {@code _gp} form field, or empty
         *           string
         * @param ge the value for the {@code _ge} form field
         *           (base64-encoded Play Protect environment flags), or
         *           empty string
         * @param ga the value for the {@code _ga} form field
         *           (base64-encoded GMS metadata blob), or empty string
         */
        record PlayIntegrityData(String gpia, String gg, String gi, String gp,
                                 String ge, String ga) {
            /**
             * All-empty Play Integrity verdict, used by attestors that
             * cannot mint a real Play Integrity token (no Google Play
             * Services, Huawei-style or sideloaded device). The
             * registration server tolerates an all-empty verdict but
             * treats it as a low-trust signal.
             */
            public static final PlayIntegrityData EMPTY =
                    new PlayIntegrityData("", "", "", "", "", "");

            /**
             * Compact canonical constructor that rejects {@code null}
             * components so the registration code never emits literal
             * {@code null} strings on the wire.
             *
             * @throws NullPointerException if any component is {@code null}
             */
            public PlayIntegrityData {
                if (gpia == null || gg == null || gi == null || gp == null
                        || ge == null || ga == null) {
                    throw new NullPointerException("PlayIntegrityData components must not be null");
                }
            }
        }

        /**
         * Result of {@link Android#sign(LinkedWhatsAppStore, byte[])}: the
         * TEE-backed signature over the outer encrypted body together
         * with the attestation certificate chain for the signing key.
         *
         * @param signature the raw signature bytes produced by the
         *                  keystore; hex-encoded by the registration
         *                  code and appended as the {@code H=} form
         *                  field
         * @param certificateChain the raw certificate chain bytes for
         *                         the signing key; base64-encoded by
         *                         the registration code and sent as the
         *                         {@code Authorization:} header
         */
        record KeystoreAttestation(byte[] signature, byte[] certificateChain) {
            /**
             * Empty keystore attestation: zero-length signature plus
             * zero-length certificate chain. Used by attestors that
             * cannot produce a TEE-backed signature (no hardware
             * keystore, sideloaded device). The registration code's
             * {@code attestBody} step interprets this as "no signature
             * available" and skips both the {@code &H=} body suffix
             * and the {@code Authorization} request header.
             */
            public static final KeystoreAttestation EMPTY =
                    new KeystoreAttestation(new byte[0], new byte[0]);

            /**
             * Compact canonical constructor that rejects {@code null}
             * components.
             *
             * @throws NullPointerException if either component is
             *                              {@code null}
             */
            public KeystoreAttestation {
                if (signature == null || certificateChain == null) {
                    throw new NullPointerException("KeystoreAttestation components must not be null");
                }
            }
        }
    }

    /**
     * Attestor for an iOS mobile registration.
     *
     * <p>iOS registration attaches a single App Attest assertion. The
     * App Attest key identifier and the Secure-Enclave-produced
     * assertion together constitute the proof that the request
     * originates from a genuine Apple-certified device. The
     * platform-specific record type {@link AppAttestData} lives directly
     * under this interface.
     */
    non-sealed interface Ios extends LinkedWhatsAppClientDeviceAttestor {
        /**
         * Low-trust iOS attestor used when the embedder has not supplied
         * one. Returns {@link AppAttestData#EMPTY} for the App Attest
         * payload; this is the same value a simulator or jailbroken
         * device with App Attest revoked would report. The registration
         * server tolerates the empty payload but treats the request as a
         * low-trust signal.
         */
        Ios NONE = new Ios() {
            @Override
            public AppAttestData attest(LinkedWhatsAppStore store) {
                return AppAttestData.EMPTY;
            }
        };

        /**
         * Produces the Apple App Attest assertion bound to the current
         * registration request.
         *
         * <p>Called by the iOS registration code exactly once per
         * outgoing {@code /v2/*} request that needs attestation. The
         * fields of the result map directly to the iOS-specific form
         * fields the native client appends to the body.
         *
         * @param store the live registration store; never {@code null}
         * @return the App Attest data tuple; never {@code null}
         * @throws RuntimeException if the attestor cannot produce an
         *                          assertion
         */
        AppAttestData attest(LinkedWhatsAppStore store);

        /**
         * Triple of Apple App Attest payloads the iOS registration
         * code attaches to every attested request: the CBOR
         * attestation object produced by
         * {@code DCAppAttestService.attestKey:clientDataHash:}, the
         * CBOR assertion object produced by
         * {@code DCAppAttestService.generateAssertion:clientDataHash:}
         * under the same {@code keyId}, and the {@code keyId} itself.
         *
         * <p>The three values map onto the wire as follows, confirmed
         * by Frida runtime tracing of {@code -[NSMutableURLRequest
         * setHTTPBody:]} and {@code setValue:forHTTPHeaderField:} on a
         * live iOS WhatsApp registration:
         * <ul>
         *   <li>{@link #attestation} and {@link #keyId} together
         *       become the {@code Authorization} request header on
         *       every attested endpoint, joined by a literal
         *       {@code "|"}: {@code <base64 attestation>|<base64 keyId>}.
         *       Both halves use regular base64 (not URL-safe). The
         *       native iOS client caches this attestation across
         *       requests in the same registration session under the
         *       Keychain service {@code app-attestation-reg} with the
         *       account name
         *       {@code "app-attestation_reg-" + keyId} (note the
         *       deliberate underscore in the account-name prefix),
         *       and only re-mints via {@code attestKey:} when the
         *       cached entry has expired.</li>
         *   <li>{@link #assertion} becomes the {@code H=} form-field
         *       suffix on the outgoing body, wrapped in a JSON
         *       envelope: {@code {"assertion":"<base64 assertion>"}}.
         *       Freshly produced per request by signing a
         *       {@code clientDataHash} that is the SHA-256 of the
         *       base64-decoded {@code authkey} bytes (the raw
         *       Curve25519 noise public key, not the form-field
         *       string). The assertion proves possession of the
         *       identity key on a real device but does <em>not</em>
         *       sign the outer AES-GCM-encrypted request body,
         *       unlike Android's {@code H=} suffix which is an
         *       HMAC over the body.</li>
         * </ul>
         * All three components are never {@code null}; attestors that
         * cannot produce a particular payload should return the empty
         * string for it. The registration server tolerates empty
         * values but treats the request as a low-trust signal.
         *
         * @param attestation the base64-encoded CBOR attestation
         *                    object produced by
         *                    {@code DCAppAttestService.attestKey}
         * @param assertion   the base64-encoded CBOR assertion object
         *                    produced by
         *                    {@code DCAppAttestService.generateAssertion}
         * @param keyId       the base64-encoded App Attest key
         *                    identifier returned by
         *                    {@code DCAppAttestService.generateKeyWithCompletionHandler:}
         */
        record AppAttestData(String attestation, String assertion, String keyId) {
            /**
             * All-empty App Attest payload, used by attestors that
             * cannot mint a real Apple App Attest assertion (no
             * Secure Enclave, simulator, jailbroken device that
             * revoked App Attest). The registration server tolerates
             * empty payloads but treats them as a low-trust signal.
             */
            public static final AppAttestData EMPTY = new AppAttestData("", "", "");

            /**
             * Compact canonical constructor that rejects {@code null}
             * components.
             *
             * @throws NullPointerException if any component is
             *                              {@code null}
             */
            public AppAttestData {
                if (attestation == null || assertion == null || keyId == null) {
                    throw new NullPointerException("AppAttestData components must not be null");
                }
            }
        }
    }
}
