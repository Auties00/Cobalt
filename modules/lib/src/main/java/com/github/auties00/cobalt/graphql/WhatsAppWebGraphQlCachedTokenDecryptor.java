package com.github.auties00.cobalt.graphql;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.stanza.mex.json.misc.CachedTokenMexResponse;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import java.util.Optional;

/**
 * Decrypts the {@code encrypted_access_tokens} RSA-with-AES bundle returned by the
 * {@code xwa2_ent_trade_canonical_nonce_for_access_tokens} mutation into {@link WhatsAppWebGraphQlCanonicalCredentials}
 * (the cached-nonce leg of the relay credential-acquisition design).
 *
 * <p>This adapts the decryption tail of {@code WAWebMexCachedTokenJob.fetchCachedNonceToken}. The
 * relay wraps a random AES-256 content-encryption key under the client's RSA public key (RSA-OAEP with
 * a SHA-1 digest and MGF1-SHA1 mask) and encrypts the access-token payload under that AES key with
 * AES-256-GCM. Recovery is therefore two-staged: unwrap the AES key with the retained
 * {@link PrivateKey}, then AES-GCM-decrypt {@code data || tag} under it with {@code nonce} as the IV.
 * The GCM concatenation idiom mirrors {@link WhatsAppWebGraphQlCanonicalNonceDecryptor}: the 16-byte tag is appended to
 * the ciphertext because the JCA expects {@code ciphertext || tag} while the wire carries them apart.
 *
 * <p>The recovered plaintext is doubly nested: the outer UTF-8 JSON object carries a single
 * {@code data} field whose string value is itself the JSON object holding the credentials
 * ({@code {access_token, fbid}}). Both layers are parsed and only the inner object is read.
 *
 * <p>The whole operation is best-effort: a missing bundle field, an RSA or GCM failure, malformed
 * JSON at either layer, or an absent {@code access_token}/{@code fbid} yields {@link Optional#empty()}
 * rather than an exception, mirroring the leniency of {@link WhatsAppWebGraphQlCanonicalNonceDecryptor}.
 *
 * @implNote The OAEP digest is SHA-1, not SHA-256: WhatsApp Web mints the RSA key with a SHA-1 hash,
 * so the unwrap must use {@code RSA/ECB/OAEPWithSHA-1AndMGF1Padding} with an
 * {@link OAEPParameterSpec} pinning both the message digest and the MGF1 digest to SHA-1. A SHA-256
 * digest silently fails the unwrap. The returned credentials carry a {@code null} nonce and a
 * {@code 0} device id: the cached-nonce reply delivers a minted {@code access_token} directly (no
 * nonce to trade), and the device id is stitched in by the caller via
 * {@link WhatsAppWebGraphQlCanonicalCredentials#withDeviceId(long)}.
 */
@WhatsAppWebModule(moduleName = "WAWebMexCachedTokenJob")
public final class WhatsAppWebGraphQlCachedTokenDecryptor {
    /**
     * The GCM authentication-tag length in bits, matching WhatsApp Web's {@code tagLength: 128}.
     */
    private static final int GCM_TAG_BITS = 128;

    /**
     * Prevents instantiation of this stateless utility.
     *
     * <p>All behaviour is exposed through the static {@link #decrypt} entry point.
     */
    private WhatsAppWebGraphQlCachedTokenDecryptor() {
        throw new AssertionError("WhatsAppWebGraphQlCachedTokenDecryptor is not instantiable");
    }

    /**
     * Decrypts the RSA-with-AES bundle into {@link WhatsAppWebGraphQlCanonicalCredentials}.
     *
     * <p>Base64-decodes the {@code key}, {@code data}, {@code nonce}, and {@code tag} scalars, unwraps
     * the AES-256 content-encryption key from {@code key} with {@code privateKey} under RSA-OAEP-SHA1,
     * AES-256-GCM-decrypts {@code data || tag} under that key with {@code nonce} as the IV, then parses
     * the doubly nested UTF-8 JSON plaintext and extracts {@code access_token} and {@code fbid}. The
     * returned credentials carry a {@code null} nonce and a {@code 0} device id.
     *
     * <p>The result is {@link Optional#empty()} when {@code privateKey} or {@code bundle} is
     * {@code null}, when any of the four bundle scalars is absent, when the RSA unwrap or the GCM
     * decrypt fails, when either JSON layer is malformed, or when the inner object lacks
     * {@code access_token} or {@code fbid}.
     *
     * @param privateKey the RSA private key paired with the {@code client_pub_key} sent in the request,
     *                   or {@code null}
     * @param bundle     the parsed {@code encrypted_access_tokens} bundle, or {@code null}
     * @return the recovered credentials with a {@code 0} device id, or empty on any missing input or
     *         failure
     */
    @WhatsAppWebExport(moduleName = "WAWebMexCachedTokenJob", exports = "fetchCachedNonceToken",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<WhatsAppWebGraphQlCanonicalCredentials> decrypt(PrivateKey privateKey,
                                                         CachedTokenMexResponse.EncryptedAccessTokens bundle) {
        if (privateKey == null || bundle == null) {
            return Optional.empty();
        }

        var keyB64 = bundle.key().orElse(null);
        var dataB64 = bundle.data().orElse(null);
        var nonceB64 = bundle.nonce().orElse(null);
        var tagB64 = bundle.tag().orElse(null);
        if (keyB64 == null || dataB64 == null || nonceB64 == null || tagB64 == null) {
            return Optional.empty();
        }

        byte[] plaintext;
        try {
            var decoder = Base64.getDecoder();
            var wrappedKey = decoder.decode(keyB64);
            var ciphertext = decoder.decode(dataB64);
            var nonce = decoder.decode(nonceB64);
            var authTag = decoder.decode(tagB64);

            var rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
            rsa.init(Cipher.DECRYPT_MODE, privateKey,
                    new OAEPParameterSpec("SHA-1", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT));
            var aesKey = rsa.doFinal(wrappedKey);

            var input = new byte[ciphertext.length + authTag.length];
            System.arraycopy(ciphertext, 0, input, 0, ciphertext.length);
            System.arraycopy(authTag, 0, input, ciphertext.length, authTag.length);

            var gcm = Cipher.getInstance("AES/GCM/NoPadding");
            gcm.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, nonce));
            plaintext = gcm.doFinal(input);
        } catch (Exception exception) {
            return Optional.empty();
        }

        return parsePayload(plaintext);
    }

    /**
     * Parses the doubly nested UTF-8 JSON plaintext into {@link WhatsAppWebGraphQlCanonicalCredentials}.
     *
     * <p>The outer object carries a single {@code data} field whose string value is the inner JSON
     * object {@code {access_token, fbid}}; only the inner object holds the credentials. The result is
     * {@link Optional#empty()} when either layer is not valid JSON or when {@code access_token} or
     * {@code fbid} is absent.
     *
     * @param plaintext the decrypted UTF-8 JSON bytes
     * @return the parsed credentials with a {@code 0} device id, or empty when the shape is invalid
     */
    private static Optional<WhatsAppWebGraphQlCanonicalCredentials> parsePayload(byte[] plaintext) {
        JSONObject outer;
        try {
            outer = JSON.parseObject(new String(plaintext, StandardCharsets.UTF_8));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
        if (outer == null) {
            return Optional.empty();
        }

        var innerJson = outer.getString("data");
        if (innerJson == null) {
            return Optional.empty();
        }

        JSONObject inner;
        try {
            inner = JSON.parseObject(innerJson);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
        if (inner == null) {
            return Optional.empty();
        }

        var accessToken = inner.getString("access_token");
        var fbid = inner.getLong("fbid");
        if (accessToken == null || fbid == null) {
            return Optional.empty();
        }
        return Optional.of(new WhatsAppWebGraphQlCanonicalCredentials(accessToken, fbid, null, 0L));
    }
}
