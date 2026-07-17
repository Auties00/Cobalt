package com.github.auties00.cobalt.calls.transport.srtp;

import com.github.auties00.cobalt.calls.platform.VoipCryptoNative;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import com.github.auties00.cobalt.calls.transport.warp.WarpMessageIntegrity;

/**
 * Derives the per domain transport keys of a call from a base secret using the WhatsApp SFU key
 * derivation, an HKDF SHA256 expansion keyed by a fixed per domain ASCII info label.
 *
 * <p>The voip key derivation expands one base secret into several thirty two byte keys, each selected
 * by a domain label. Most of those keys, the hop by hop SRTP/SRTCP masters and the WARP authentication
 * key, are not a flat single step expansion: they are a two step chained HKDF over a split of the relay
 * hop by hop key; the end to end SFrame key is produced by the SFrame key schedule. Those are
 * deliberately absent here. This holder carries only the Web P2P
 * {@link Domain#CERT_FINGERPRINT_HMAC data-channel certificate fingerprint HMAC} label.
 *
 * <p>The {@link Domain#CERT_FINGERPRINT_HMAC data-channel certificate fingerprint HMAC} domain the Web
 * P2P path uses to bind its DTLS certificate is not byte correct under {@link #derive(byte[], Domain)}:
 * its derivation splits the raw end to end key material into a sixteen byte salt and the remaining bytes
 * as input keying material, whereas {@link #derive(byte[], Domain)} applies the zero salt. Its label is
 * carried here for the eventual Web P2P consumer, which must perform the salted derivation rather than
 * calling {@link #derive(byte[], Domain)}.
 *
 * <p>The {@link #derive(byte[], Domain)} derivation is a single RFC 5869 HKDF SHA256 computation
 * through {@link VoipCryptoNative}: the base secret is the input keying material, the salt is the RFC
 * no salt default (a thirty two byte zero salt), the info is the domain's ASCII label, and the output
 * length is {@value #DERIVED_KEY_LENGTH} bytes. The class is stateless and thread safe.
 */
public final class SfuKeyDeriver {
    /**
     * The output length, in bytes, of every key this deriver produces.
     */
    public static final int DERIVED_KEY_LENGTH = 32;

    /**
     * The RFC 5869 no salt default applied to every derivation: a thirty two byte all zero salt.
     */
    private static final byte[] ZERO_SALT = new byte[32];

    /**
     * Enumerates the transport layer key derivation domains, each bound to its fixed ASCII info label.
     *
     * <p>Only the {@link #CERT_FINGERPRINT_HMAC} label is listed. It belongs to a separate salted
     * derivation rather than to the single step expansion of {@link SfuKeyDeriver#derive(byte[], Domain)};
     * the hop by hop SRTP/SRTCP masters, the WARP authentication key, and the end to end SFrame key are two
     * step chained or SFrame scheduled derivations produced elsewhere, not flat single step expansions of
     * this helper.
     */
    public enum Domain {
        /**
         * The data channel certificate fingerprint HMAC domain, label
         * {@code "data-channel cert fingerprint hmac"}.
         *
         * <p>The key it derives binds the Web P2P DTLS certificate fingerprint so a peer can verify the
         * fingerprint it learned through signaling against the one presented in the DTLS handshake. That
         * derivation uses a sixteen byte salt taken from the raw end to end key material and the remaining
         * bytes as input keying material, so it cannot be produced by the zero salt
         * {@link SfuKeyDeriver#derive(byte[], Domain)} helper; a Web P2P consumer must perform the salted
         * HKDF explicitly.
         */
        // TODO: implement the salted HKDF that binds the Web P2P DTLS certificate fingerprint; derive(byte[], Domain) applies the zero salt and does not reproduce this domain's key
        CERT_FINGERPRINT_HMAC("data-channel cert fingerprint hmac");

        /**
         * Holds the fixed ASCII info label of this domain.
         */
        private final byte[] label;

        /**
         * Constructs a domain bound to its ASCII info label.
         *
         * @param label the ASCII info label, used verbatim as the HKDF info
         */
        Domain(String label) {
            this.label = label.getBytes(StandardCharsets.US_ASCII);
        }

        /**
         * Returns this domain's ASCII info label bytes, the verbatim HKDF info for its derivation.
         *
         * <p>The returned array is the shared label backing this domain, lent for read only use as the
         * HKDF info input the {@link SfuKeyDeriver#derive(byte[], Domain)} expansion reads; a caller must
         * not mutate it.
         *
         * @return the HKDF info bytes for this domain
         */
        byte[] info() {
            return label;
        }
    }

    /**
     * Prevents instantiation of this stateless derivation holder.
     */
    private SfuKeyDeriver() {
        throw new AssertionError("SfuKeyDeriver is not instantiable");
    }

    /**
     * Derives the thirty two byte key for a domain from a base secret.
     *
     * <p>The derivation is {@code HKDF-SHA256(ikm = baseSecret, salt = 32 zero bytes, info = domain
     * label, L = 32)}.
     *
     * {@snippet :
     *   key = HKDF-SHA256(baseSecret, new byte[32], domain.info(), 32)
     * }
     *
     * @param baseSecret the input keying material the domain key is expanded from
     * @param domain     the key derivation domain selecting the info label
     * @return the {@value #DERIVED_KEY_LENGTH}-byte derived key
     * @throws NullPointerException if {@code baseSecret} or {@code domain} is {@code null}
     */
    public static byte[] derive(byte[] baseSecret, Domain domain) {
        Objects.requireNonNull(baseSecret, "baseSecret cannot be null");
        Objects.requireNonNull(domain, "domain cannot be null");
        return VoipCryptoNative.hkdfSha256(baseSecret, ZERO_SALT, domain.info(), DERIVED_KEY_LENGTH);
    }
}
