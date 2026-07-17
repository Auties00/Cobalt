package com.github.auties00.cobalt.graphql;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Base64;

/**
 * Holds the ephemeral RSA key pair minted for the cached-nonce access-token recovery leg, exposing the
 * wire-ready public key encoding and retaining the private key for the reply decryption.
 *
 * <p>This backs the {@code client_pub_key} field of the
 * {@code xwa2_ent_trade_canonical_nonce_for_access_tokens} mutation. WhatsApp Web generates a fresh
 * 2048-bit RSA key pair (public exponent {@code 65537}), exports the public key as X.509/SPKI DER,
 * base64-encodes it, wraps it in a {@code PEM} armour block, then base64-encodes that armoured text a
 * second time; the doubly base64-encoded PEM is what the relay expects. The relay encrypts the
 * access-token payload against that public key, so the matching {@link #privateKey()} must survive
 * until the reply is decrypted by {@link WhatsAppWebGraphQlCachedTokenDecryptor}.
 *
 * @implNote The public key exported by {@code KeyPair.getPublic().getEncoded()} is already
 * X.509/SubjectPublicKeyInfo DER, so it maps directly onto WebCrypto's {@code exportKey("spki", ...)}
 * output without any restructuring; the only adaptation is the PEM armour and the second base64 layer,
 * both applied verbatim to match {@code WAWebMexCachedTokenJob}. The OAEP hash is not bound at key
 * generation time in the JCA (it is supplied at cipher time by {@link WhatsAppWebGraphQlCachedTokenDecryptor}), so the
 * SHA-1 OAEP digest WhatsApp Web pins on the WebCrypto key is a decryption-side concern, not a
 * key-generation parameter here.
 */
@WhatsAppWebModule(moduleName = "WAWebMexCachedTokenJob")
public final class WhatsAppWebGraphQlCachedTokenKeyPair {
    /**
     * The RSA modulus length in bits, matching WhatsApp Web's {@code modulusLength: 2048}.
     */
    private static final int RSA_MODULUS_BITS = 2048;

    /**
     * The base64-encoded PEM-armoured public key sent as the {@code client_pub_key} mutation field.
     */
    private final String clientPubKey;

    /**
     * The RSA private key retained to unwrap the AES content-encryption key from the relay reply.
     */
    private final PrivateKey privateKey;

    /**
     * Constructs a key-pair holder from the encoded public key and the retained private key.
     *
     * <p>Instances are produced only by the {@link #generate()} factory.
     *
     * @param clientPubKey the base64-encoded PEM-armoured public key
     * @param privateKey   the RSA private key paired with {@code clientPubKey}
     */
    private WhatsAppWebGraphQlCachedTokenKeyPair(String clientPubKey, PrivateKey privateKey) {
        this.clientPubKey = clientPubKey;
        this.privateKey = privateKey;
    }

    /**
     * Generates a fresh 2048-bit RSA key pair and derives the wire-ready public key encoding.
     *
     * <p>Initializes {@link KeyPairGenerator} with {@code RSAKeyGenParameterSpec(2048, F4)} where
     * {@code F4} is the public exponent {@code 65537}, exports the public key as X.509/SPKI DER,
     * base64-encodes it, wraps it in a {@code -----BEGIN PUBLIC KEY-----} PEM block, then base64-encodes
     * the UTF-8 bytes of that armoured text a second time to produce {@link #clientPubKey()}. The
     * generated {@link #privateKey()} is retained on the returned holder.
     *
     * @return a fresh key-pair holder carrying the doubly base64-encoded public key and its private key
     * @throws IllegalStateException if the JCA cannot provide an RSA key-pair generator or reject the
     *                               fixed 2048-bit F4 parameters
     */
    @WhatsAppWebExport(moduleName = "WAWebMexCachedTokenJob", exports = "fetchCachedNonceToken",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static WhatsAppWebGraphQlCachedTokenKeyPair generate() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(new RSAKeyGenParameterSpec(RSA_MODULUS_BITS, RSAKeyGenParameterSpec.F4));
            var keyPair = generator.generateKeyPair();

            var spkiB64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            var pem = "-----BEGIN PUBLIC KEY-----\n" + spkiB64 + "\n-----END PUBLIC KEY-----\n";
            var clientPubKey = Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8));
            return new WhatsAppWebGraphQlCachedTokenKeyPair(clientPubKey, keyPair.getPrivate());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to generate the cached-token RSA key pair", exception);
        }
    }

    /**
     * Returns the base64-encoded PEM-armoured public key sent as the {@code client_pub_key} field.
     *
     * @return the doubly base64-encoded public key
     */
    public String clientPubKey() {
        return clientPubKey;
    }

    /**
     * Returns the RSA private key used to unwrap the relay reply.
     *
     * @return the RSA private key paired with {@link #clientPubKey()}
     */
    public PrivateKey privateKey() {
        return privateKey;
    }
}
