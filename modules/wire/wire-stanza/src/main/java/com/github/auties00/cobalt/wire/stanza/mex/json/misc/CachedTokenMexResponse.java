package com.github.auties00.cobalt.wire.stanza.mex.json.misc;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the MEX response of the cached-token mutation built by {@link CachedTokenMexRequest}.
 *
 * <p>Projects the {@code data.xwa2_ent_trade_canonical_nonce_for_access_tokens} envelope, exposing
 * the single nested {@link EncryptedAccessTokens} RSA-with-AES bundle. The bundle carries the
 * AES-wrapped key, the ciphertext, the GCM tag, the nonce and the algorithm identifier; the caller
 * decrypts it with the RSA private key paired to the {@code client_pub_key} it sent in the request.
 *
 * @see CachedTokenMexRequest
 */
@WhatsAppWebModule(moduleName = "WAWebMexCachedTokenJob")
public final class CachedTokenMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the parsed RSA-with-AES bundle projected from {@code encrypted_access_tokens}.
     */
    private final EncryptedAccessTokens encryptedAccessTokens;

    /**
     * Constructs a response wrapping the parsed RSA-with-AES bundle.
     *
     * <p>Instances are produced only by the {@link #of(Stanza)} parser.
     *
     * @param encryptedAccessTokens the parsed RSA-with-AES bundle, may be {@code null}
     */
    private CachedTokenMexResponse(EncryptedAccessTokens encryptedAccessTokens) {
        this.encryptedAccessTokens = encryptedAccessTokens;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser; the returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_ent_trade_canonical_nonce_for_access_tokens} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result payload
     */
    @WhatsAppWebExport(moduleName = "WAWebMexCachedTokenJob", exports = "fetchCachedNonceToken",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<CachedTokenMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(CachedTokenMexResponse::of);
    }

    /**
     * Returns the RSA-with-AES bundle carrying the encrypted access tokens.
     *
     * @return the parsed {@link EncryptedAccessTokens}, or empty when the relay omitted the field
     */
    public Optional<EncryptedAccessTokens> encryptedAccessTokens() {
        return Optional.ofNullable(encryptedAccessTokens);
    }

    /**
     * Parses the response from the raw UTF-8 JSON payload of the {@code <result>} child.
     *
     * <p>Reserved for the public {@link #of(Stanza)} overload.
     *
     * @implNote This implementation guards every nested object lookup so a malformed envelope
     * produces {@link Optional#empty()} rather than a parser exception.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return the parsed response, or empty when the envelope lacks the expected
     *         {@code data.xwa2_ent_trade_canonical_nonce_for_access_tokens} root
     */
    private static Optional<CachedTokenMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_ent_trade_canonical_nonce_for_access_tokens");
        if (root == null) {
            return Optional.empty();
        }

        var encryptedAccessTokens = EncryptedAccessTokens.of(root.getJSONObject("encrypted_access_tokens")).orElse(null);
        return Optional.of(new CachedTokenMexResponse(encryptedAccessTokens));
    }

    /**
     * Wraps the {@code encrypted_access_tokens} RSA-with-AES sub-object.
     *
     * <p>Carries the AES-wrapped key, the ciphertext, the GCM authentication tag, the nonce and the
     * algorithm identifier. The caller hex- or base64-decodes each scalar per the algorithm, unwraps
     * the AES key with its RSA private key, then decrypts the ciphertext to recover the access-token
     * payload.
     */
    public static final class EncryptedAccessTokens {
        /**
         * Holds the {@code key} scalar: the AES content-encryption key wrapped under the client's RSA
         * public key.
         */
        private final String key;

        /**
         * Holds the {@code data} scalar: the AES-encrypted access-token ciphertext.
         */
        private final String data;

        /**
         * Holds the {@code tag} scalar: the AEAD authentication tag covering the ciphertext.
         */
        private final String tag;

        /**
         * Holds the {@code nonce} scalar: the AEAD nonce paired with the ciphertext.
         */
        private final String nonce;

        /**
         * Holds the {@code algorithm} scalar: the identifier of the RSA-with-AES suite used to wrap
         * the payload.
         */
        private final String algorithm;

        /**
         * Constructs an encrypted-access-tokens bundle from the parsed scalar fields.
         *
         * <p>Reserved for the static parser.
         *
         * @param key       the AES-wrapped content-encryption key
         * @param data      the AES-encrypted ciphertext
         * @param tag       the AEAD authentication tag
         * @param nonce     the AEAD nonce
         * @param algorithm the RSA-with-AES suite identifier
         */
        private EncryptedAccessTokens(String key, String data, String tag, String nonce, String algorithm) {
            this.key = key;
            this.data = data;
            this.tag = tag;
            this.nonce = nonce;
            this.algorithm = algorithm;
        }

        /**
         * Returns the AES content-encryption key wrapped under the client's RSA public key.
         *
         * @return the wrapped key, or empty when the relay omitted the field
         */
        public Optional<String> key() {
            return Optional.ofNullable(key);
        }

        /**
         * Returns the AES-encrypted access-token ciphertext.
         *
         * @return the ciphertext, or empty when the relay omitted the field
         */
        public Optional<String> data() {
            return Optional.ofNullable(data);
        }

        /**
         * Returns the AEAD authentication tag covering the ciphertext.
         *
         * @return the authentication tag, or empty when the relay omitted the field
         */
        public Optional<String> tag() {
            return Optional.ofNullable(tag);
        }

        /**
         * Returns the AEAD nonce paired with the ciphertext.
         *
         * @return the nonce, or empty when the relay omitted the field
         */
        public Optional<String> nonce() {
            return Optional.ofNullable(nonce);
        }

        /**
         * Returns the identifier of the RSA-with-AES suite used to wrap the payload.
         *
         * @return the algorithm identifier, or empty when the relay omitted the field
         */
        public Optional<String> algorithm() {
            return Optional.ofNullable(algorithm);
        }

        /**
         * Parses an {@link EncryptedAccessTokens} bundle from the given JSON object.
         *
         * <p>Used by {@link CachedTokenMexResponse#of(byte[])} to hydrate the nested
         * {@code encrypted_access_tokens} entry.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link EncryptedAccessTokens}, or empty when {@code obj} is {@code null}
         */
        static Optional<EncryptedAccessTokens> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var key = obj.getString("key");
            var data = obj.getString("data");
            var tag = obj.getString("tag");
            var nonce = obj.getString("nonce");
            var algorithm = obj.getString("algorithm");
            return Optional.of(new EncryptedAccessTokens(key, data, tag, nonce, algorithm));
        }

        /**
         * Parses a list of {@link EncryptedAccessTokens} bundles from the given JSON array.
         *
         * <p>The cached-token envelope carries a single bundle rather than an array; this overload
         * exists for symmetry with the other sub-object parsers.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<EncryptedAccessTokens> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<EncryptedAccessTokens>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }
}
