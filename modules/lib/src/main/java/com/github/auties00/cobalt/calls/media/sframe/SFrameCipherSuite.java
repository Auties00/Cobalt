package com.github.auties00.cobalt.calls.media.sframe;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumerates the five SFrame cipher suites the call media path can negotiate, each binding a
 * BoringSSL EVP cipher identity to a truncated authentication tag length.
 *
 * <p>SFrame seals every media payload with {@code AES-128} in counter mode for confidentiality and a
 * truncated {@code HMAC-SHA256} for authentication. The suite index selects which EVP cipher variant
 * the native engine instantiates and how many leading bytes of the HMAC are kept as the tag. The AES
 * key is always {@value #AES_KEY_LENGTH} bytes, the HMAC key always {@value #HMAC_KEY_LENGTH} bytes,
 * and the AES counter block always {@value #IV_LENGTH} bytes, so only the EVP variant and the tag
 * length differ across suites. Suite {@link #SUITE_0} carries a zero length tag, meaning no
 * authentication.
 *
 * <p>The 1:1 and group call media path negotiated by WhatsApp uses {@link #SUITE_3}: {@code AES-128-CTR}
 * with a four byte truncated {@code HMAC-SHA256} tag (IETF SFrame cipher suite 3 in RFC 9605
 * numbering). The per stream suite is negotiated in signaling through the {@code sframe_cipher_suite}
 * stream setting, an index in {@code 0..4}. The index to suite mapping is:
 *
 * {@snippet lang="text" :
 * index | EVP cipher id | tag length (bytes)
 *   0   |       2       |        0 (no authentication)
 *   1   |       3       |        4
 *   2   |       4       |        8
 *   3   |       0       |        4 (WhatsApp default)
 *   4   |       1       |        8
 * }
 *
 * @implNote This implementation retains the BoringSSL EVP cipher enum value only for parity and
 * diagnostics: Cobalt resolves the cipher purely through JCA ({@code Cipher("AES/CTR/NoPadding")} and
 * {@code Mac("HmacSHA256")}), so the operative difference Cobalt acts on is the
 * {@linkplain #tagLength() tag length}.
 */
public enum SFrameCipherSuite {
    /**
     * Suite {@code 0}: EVP cipher id {@code 2}, no authentication tag ({@code tagLength == 0}).
     *
     * <p>With a zero length tag this suite applies no HMAC authentication; it is the unauthenticated
     * variant and is not used by the standard WhatsApp media path.
     */
    SUITE_0(0, 2, 0),

    /**
     * Suite {@code 1}: EVP cipher id {@code 3}, four byte truncated {@code HMAC-SHA256} tag.
     */
    SUITE_1(1, 3, 4),

    /**
     * Suite {@code 2}: EVP cipher id {@code 4}, eight byte truncated {@code HMAC-SHA256} tag.
     */
    SUITE_2(2, 4, 8),

    /**
     * Suite {@code 3}: EVP cipher id {@code 0}, four byte truncated {@code HMAC-SHA256} tag; the suite
     * the WhatsApp call media path negotiates.
     *
     * <p>This is {@code AES-128-CTR} with a four byte tag, IETF SFrame cipher suite 3 in RFC 9605
     * numbering, and is the {@linkplain #defaultSuite() default} for Cobalt's media transform.
     */
    SUITE_3(3, 0, 4),

    /**
     * Suite {@code 4}: EVP cipher id {@code 1}, eight byte truncated {@code HMAC-SHA256} tag.
     */
    SUITE_4(4, 1, 8);

    /**
     * Resolves a wire suite index to its suite, backing {@link #ofIndex(int)}.
     *
     * <p>Built once at class initialization from each constant's {@link #index}, so a parsed suite index
     * resolves to its constant in constant time rather than by scanning {@link #values()}.
     */
    private static final Map<Integer, SFrameCipherSuite> BY_INDEX;

    static {
        var byIndex = new HashMap<Integer, SFrameCipherSuite>();
        for (var suite : values()) {
            if (byIndex.put(suite.index, suite) != null) {
                throw new AssertionError("Conflict");
            }
        }
        BY_INDEX = Map.copyOf(byIndex);
    }

    /**
     * Holds the AES encryption key length, in bytes, shared by every suite ({@code AES-128}).
     *
     * <p>The cipher uses exactly this many bytes from the resolved key as the AES key regardless of
     * the negotiated suite.
     */
    public static final int AES_KEY_LENGTH = 16;

    /**
     * Holds the {@code HMAC-SHA256} authentication key length, in bytes, shared by every suite.
     */
    public static final int HMAC_KEY_LENGTH = 32;

    /**
     * Holds the AES counter block (IV) length, in bytes, shared by every suite.
     */
    public static final int IV_LENGTH = 16;

    /**
     * Holds the suite index used on the wire and as the array index into the EVP and tag length
     * tables.
     */
    private final int index;

    /**
     * Holds the BoringSSL EVP cipher enum value the native engine selects for this suite.
     */
    private final int evpCipherId;

    /**
     * Holds the truncated authentication tag length, in bytes, for this suite.
     */
    private final int tagLength;

    /**
     * Constructs a suite constant bound to its wire index, native EVP cipher id, and tag length.
     *
     * @param index       the suite index in {@code 0..4}, used on the wire and as a table index
     * @param evpCipherId the BoringSSL EVP cipher enum value, retained for parity only
     * @param tagLength   the truncated {@code HMAC-SHA256} tag length in bytes
     */
    SFrameCipherSuite(int index, int evpCipherId, int tagLength) {
        this.index = index;
        this.evpCipherId = evpCipherId;
        this.tagLength = tagLength;
    }

    /**
     * Returns the suite index used on the wire and as the index into the suite tables.
     *
     * @return the suite index, {@code 0} through {@code 4}
     */
    public int index() {
        return index;
    }

    /**
     * Returns the BoringSSL EVP cipher enum value the native engine selects for this suite.
     *
     * <p>Cobalt resolves the cipher through JCA and does not consume this value operationally; it is
     * exposed for parity and diagnostics.
     *
     * @return the EVP cipher id, one of {@code 0,1,2,3,4}
     */
    public int evpCipherId() {
        return evpCipherId;
    }

    /**
     * Returns the truncated authentication tag length, in bytes, this suite appends after the
     * ciphertext.
     *
     * <p>The values are {@code SUITE_0} 0, {@code SUITE_1} 4, {@code SUITE_2} 8, {@code SUITE_3} 4,
     * and {@code SUITE_4} 8; a value of {@code 0} means the suite performs no authentication.
     *
     * @return the tag length in bytes
     */
    public int tagLength() {
        return tagLength;
    }

    /**
     * Returns whether this suite appends an authentication tag.
     *
     * @return {@code true} unless the {@linkplain #tagLength() tag length} is zero
     */
    public boolean isAuthenticated() {
        return tagLength > 0;
    }

    /**
     * Returns the suite for the given wire index.
     *
     * @implNote This implementation resolves through the prebuilt {@link #BY_INDEX} map rather than
     * scanning {@link #values()}.
     * @param index the suite index in {@code 0..4}
     * @return the matching suite
     * @throws IllegalArgumentException if {@code index} is outside {@code 0..4}
     */
    public static SFrameCipherSuite ofIndex(int index) {
        var suite = BY_INDEX.get(index);
        if (suite == null) {
            throw new IllegalArgumentException("Unknown SFrame cipher suite index: " + index);
        }
        return suite;
    }

    /**
     * Returns the suite the WhatsApp call media path negotiates by default.
     *
     * <p>This is {@link #SUITE_3} ({@code AES-128-CTR} plus four byte {@code HMAC-SHA256} tag), the
     * suite both the 1:1 and group media paths use.
     *
     * @return {@link #SUITE_3}
     */
    public static SFrameCipherSuite defaultSuite() {
        return SUITE_3;
    }
}
