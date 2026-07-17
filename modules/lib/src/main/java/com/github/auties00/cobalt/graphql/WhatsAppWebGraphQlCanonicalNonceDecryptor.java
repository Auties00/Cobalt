package com.github.auties00.cobalt.graphql;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.stanza.smax.mdcompanion.SmaxMdSetRegEncryptionMetadata;

import javax.crypto.Cipher;
import javax.crypto.KDF;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Decrypts the canonical-registration nonce blob delivered inside a {@code <pair-success/>} stanza's
 * {@code <encryption-metadata/>} child (PATH A of the relay credential-acquisition design).
 *
 * <p>This adapts {@code WAWebHandleCanonicalRegistration.decryptNonce} ({@code C}/{@code b}) and
 * {@code parseNoncePayload} ({@code v}). The decryption key is derived purely by HKDF (there is no
 * ECDH): the AES-256-GCM key is {@code HKDF-SHA256(ikm = advSecretKey, salt = noiseStaticPublicKey,
 * info = "Canonical Ent Companion Nonce Encrypt", L = 32)}. The {@code <encrypted_key/>} element that
 * {@link SmaxMdSetRegEncryptionMetadata} parses is ignored by this path; only {@code <nonce/>},
 * {@code <encrypted_data/>}, and {@code <auth_tag/>} feed the GCM unwrap. The recovered plaintext is
 * UTF-8 JSON of the shape {@code {access_token?, fbid, nonce, timestamp?}}; the device id is not part
 * of the payload and is filled in by the caller via {@link WhatsAppWebGraphQlCanonicalCredentials#withDeviceId(long)}.
 *
 * <p>The whole operation is best-effort: any GCM failure, malformed JSON, or a payload missing the
 * required {@code fbid} or {@code nonce} yields {@link Optional#empty()} rather than an exception,
 * mirroring the leniency of WhatsApp Web's {@code parseNoncePayload}.
 *
 * @implNote The byte form of the Noise static public key fed as the HKDF salt is not fully pinned down
 * from source: WhatsApp Web's noise static keys are stored without the {@code 0x05} type prefix, so
 * callers pass {@code store.signalStore().noiseKeyPair().publicKey().toEncodedPoint()} (the raw 32-byte X25519
 * point). If the GCM tag fails to verify on a real pairing capture, the 33-byte type-prefixed form is
 * the likely fix; strip or add a leading {@code 0x05} accordingly. Cobalt stores {@code advSecretKey}
 * as the raw 32-byte array (minted in {@code LiveCompanionPairingService.companionFinish}); unlike
 * WhatsApp Web there is no base64 decode of the IKM here, because WhatsApp Web's
 * {@code decodeB64(advSecret)} recovers exactly those raw bytes.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleCanonicalRegistration")
public final class WhatsAppWebGraphQlCanonicalNonceDecryptor {
    /**
     * The HKDF {@code info} label that binds the derived key to the canonical-companion-nonce purpose.
     *
     * <p>Matches WhatsApp Web's {@code Binary.build("Canonical Ent Companion Nonce Encrypt")} argument
     * to {@code WACryptoHkdfSync.hkdf}; encoded as US-ASCII bytes for the {@code thenExpand} step.
     */
    private static final String HKDF_INFO = "Canonical Ent Companion Nonce Encrypt";

    /**
     * The HKDF output length in bytes; a 256-bit key for AES-256-GCM.
     */
    private static final int HKDF_OUTPUT_LENGTH = 32;

    /**
     * The GCM authentication-tag length in bits, matching WhatsApp Web's {@code tagLength: 128}.
     */
    private static final int GCM_TAG_BITS = 128;

    /**
     * Prevents instantiation of this stateless utility.
     *
     * <p>All behaviour is exposed through the static {@link #decrypt} entry point.
     */
    private WhatsAppWebGraphQlCanonicalNonceDecryptor() {
        throw new AssertionError("WhatsAppWebGraphQlCanonicalNonceDecryptor is not instantiable");
    }

    /**
     * Decrypts and parses the canonical-registration nonce blob into {@link WhatsAppWebGraphQlCanonicalCredentials}.
     *
     * <p>Derives the AES-256-GCM key as {@code HKDF-SHA256(ikm = advSecretKey,
     * salt = noiseStaticPublicKey, info = "Canonical Ent Companion Nonce Encrypt", L = 32)}, then
     * decrypts {@code metadata.encryptedData() || metadata.authTag()} under that key with
     * {@code metadata.nonce()} as the GCM IV, parses the resulting UTF-8 bytes as JSON, and extracts
     * {@code access_token} (optional), {@code fbid} (required), and {@code nonce} (required). The
     * returned credentials carry a {@code deviceId} of {@code 0}; the caller stitches in the real
     * device id via {@link WhatsAppWebGraphQlCanonicalCredentials#withDeviceId(long)}.
     *
     * <p>The result is {@link Optional#empty()} when {@code advSecretKey} or {@code noiseStaticPublicKey}
     * is {@code null}, when the GCM unwrap fails (wrong key bytes, tampered ciphertext, bad nonce form),
     * when the plaintext is not valid JSON, or when the payload lacks {@code fbid} or {@code nonce}.
     *
     * @param advSecretKey         the raw 32-byte ADV secret key (HKDF input keying material), or
     *                             {@code null} when the store has none
     * @param noiseStaticPublicKey the Noise static public key bytes used as the HKDF salt, typically
     *                             {@code store.signalStore().noiseKeyPair().publicKey().toEncodedPoint()}, or
     *                             {@code null}
     * @param metadata             the parsed {@code <encryption-metadata/>} envelope; must not be
     *                             {@code null}
     * @return the recovered credentials with a {@code 0} device id, or empty on any missing input or
     *         failure
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleCanonicalRegistration",
            exports = "decryptNonce",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<WhatsAppWebGraphQlCanonicalCredentials> decrypt(byte[] advSecretKey,
                                                         byte[] noiseStaticPublicKey,
                                                         SmaxMdSetRegEncryptionMetadata metadata) {
        if (advSecretKey == null || noiseStaticPublicKey == null || metadata == null) {
            return Optional.empty();
        }

        byte[] plaintext;
        try {
            var kdf = KDF.getInstance("HKDF-SHA256");
            var keyParams = HKDFParameterSpec.ofExtract()
                    .addIKM(advSecretKey)
                    .addSalt(noiseStaticPublicKey)
                    .thenExpand(HKDF_INFO.getBytes(StandardCharsets.US_ASCII), HKDF_OUTPUT_LENGTH);
            var key = kdf.deriveData(keyParams);

            var ciphertext = metadata.encryptedData();
            var authTag = metadata.authTag();
            var input = new byte[ciphertext.length + authTag.length];
            System.arraycopy(ciphertext, 0, input, 0, ciphertext.length);
            System.arraycopy(authTag, 0, input, ciphertext.length, authTag.length);

            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, metadata.nonce()));
            plaintext = cipher.doFinal(input);
        } catch (Exception exception) {
            return Optional.empty();
        }

        return parsePayload(plaintext);
    }

    /**
     * Parses the decrypted UTF-8 JSON payload into {@link WhatsAppWebGraphQlCanonicalCredentials}.
     *
     * <p>Adapts {@code parseNoncePayload}: the payload is {@code {access_token?, fbid, nonce,
     * timestamp?}}. The {@code timestamp} is ignored. The result is {@link Optional#empty()} when the
     * bytes are not valid JSON or when either required field ({@code fbid}, {@code nonce}) is absent.
     *
     * @param plaintext the decrypted UTF-8 JSON bytes
     * @return the parsed credentials with a {@code 0} device id, or empty when the shape is invalid
     */
    private static Optional<WhatsAppWebGraphQlCanonicalCredentials> parsePayload(byte[] plaintext) {
        JSONObject json;
        try {
            json = JSON.parseObject(new String(plaintext, StandardCharsets.UTF_8));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
        if (json == null) {
            return Optional.empty();
        }

        var fbid = json.getLong("fbid");
        var nonce = json.getString("nonce");
        if (fbid == null || nonce == null) {
            return Optional.empty();
        }

        var accessToken = json.getString("access_token");
        return Optional.of(new WhatsAppWebGraphQlCanonicalCredentials(accessToken, fbid, nonce, 0L));
    }
}
