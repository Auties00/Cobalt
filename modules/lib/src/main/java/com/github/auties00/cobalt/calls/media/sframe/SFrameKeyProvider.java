package com.github.auties00.cobalt.calls.media.sframe;

import com.github.auties00.cobalt.calls.platform.VoipCryptoNative;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Holds the SFrame chain key for one stream and resolves the per key id {@link SFrameCipher} from it,
 * caching ciphers by key id and evicting stale ones by counter distance.
 *
 * <p>A call participant device installs a {@value #CHAIN_KEY_LENGTH} byte chain key (the
 * {@linkplain SFrameKeyDerivation#deriveBaseKey(byte[], String) derived base key}) into its provider,
 * keyed by the signaling transaction id that delivered it. Installing a chain key for a transaction id
 * already present is a no op, so a duplicated rekey delivery is idempotent. When a frame names a key id,
 * the provider returns the cached cipher for that key id, or derives a fresh one from the current chain
 * key on the first reference. A rekey installs a new chain key under a new key id; old ciphers stay
 * cached and serve late arriving frames until their counter falls too far behind the newest accepted
 * counter, at which point they are evicted.
 *
 * <p>The chain key is {@value #CHAIN_KEY_LENGTH} bytes, whereas the cipher needs a
 * {@value SFrameCipherSuite#AES_KEY_LENGTH} byte AES key, a {@value SFrameCipherSuite#HMAC_KEY_LENGTH}
 * byte HMAC key, and a 12 byte salt that drives the cipher's counter mask. This provider expands the
 * chain key into the AES and HMAC keys with a single {@code HKDF-SHA256} expand and installs an all zero
 * salt, so the cipher's counter mask is all zero. An instance is single writer, driven by the encode or
 * decode path of one stream, and holds no internal lock.
 */
public final class SFrameKeyProvider {
    /**
     * The logger for {@link SFrameKeyProvider}.
     */
    private static final System.Logger LOGGER = Log.get(SFrameKeyProvider.class);

    /**
     * Holds the ratchet/window depth the native provider is created with.
     */
    public static final int PROVIDER_DEPTH = 30000;

    /**
     * Holds the ratchet mode the native provider is created with.
     */
    public static final int PROVIDER_MODE = 3;

    /**
     * Holds the chain key length, in bytes, the provider stores ({@code 0x20}).
     */
    public static final int CHAIN_KEY_LENGTH = 32;

    /**
     * Holds the key id and counter byte width the provider is configured with.
     */
    public static final int KEY_ID_COUNTER_WIDTH = 8;

    /**
     * Holds the counter distance beyond which a cached cipher is evicted ({@code 0x6fc23ac01},
     * about thirty billion).
     */
    public static final long EVICTION_DISTANCE = 0x6fc23ac01L;

    /**
     * Holds the total length, in bytes, of the cipher material expanded from the chain key: the AES
     * key followed by the HMAC key.
     */
    private static final int CIPHER_MATERIAL_LENGTH =
            SFrameCipherSuite.AES_KEY_LENGTH + SFrameCipherSuite.HMAC_KEY_LENGTH;

    /**
     * Holds the ASCII info label for the chain key to cipher HKDF expansion.
     *
     * <p>This labels the single expansion that turns the {@value #CHAIN_KEY_LENGTH} byte chain key into
     * the {@value #CIPHER_MATERIAL_LENGTH} byte cipher material. It is internal to the provider and not
     * carried on the wire.
     */
    private static final byte[] CIPHER_EXPAND_INFO =
            "e2e sframe key-material".getBytes(StandardCharsets.US_ASCII);

    /**
     * Holds the cipher suite the provider applies to every key id it resolves.
     */
    private final SFrameCipherSuite suite;

    /**
     * Holds the current chain key, or {@code null} until a chain key is installed.
     */
    private byte[] chainKey;

    /**
     * Holds the transaction ids whose chain keys have been installed, so a duplicate install is a
     * no op.
     */
    private final Map<Long, byte[]> chainKeysByTransaction = new LinkedHashMap<>();

    /**
     * Holds the per key id cipher cache, ordered by insertion for deterministic eviction scans.
     */
    private final Map<Long, SFrameCipher> ciphersByKeyId = new LinkedHashMap<>();

    /**
     * Holds the newest counter observed across all key ids, the reference point for eviction by
     * counter distance.
     */
    private long newestCounter;

    /**
     * Constructs a key provider applying the default cipher suite.
     */
    public SFrameKeyProvider() {
        this(SFrameCipherSuite.defaultSuite());
    }

    /**
     * Constructs a key provider applying the given cipher suite to every key id it resolves.
     *
     * @param suite the cipher suite to apply
     * @throws NullPointerException if {@code suite} is {@code null}
     */
    public SFrameKeyProvider(SFrameCipherSuite suite) {
        this.suite = Objects.requireNonNull(suite, "suite cannot be null");
    }

    /**
     * Installs a {@value #CHAIN_KEY_LENGTH} byte chain key for a signaling transaction id, making it the
     * current chain key.
     *
     * <p>Installing a chain key for a transaction id already present is a no op and returns
     * {@code false}, which makes a duplicated rekey delivery idempotent. A fresh transaction id stores
     * the chain key, makes it current, and clears the cached ciphers so the next frame derives against
     * the new key.
     *
     * @param chainKey      the {@value #CHAIN_KEY_LENGTH} byte chain key, the participant's derived
     *                      SFrame base key
     * @param transactionId the signaling transaction id that delivered the chain key
     * @return {@code true} if the chain key was installed, {@code false} if the transaction id was
     *         already present
     * @throws NullPointerException     if {@code chainKey} is {@code null}
     * @throws IllegalArgumentException if {@code chainKey} is not {@value #CHAIN_KEY_LENGTH} bytes
     */
    public boolean setChainKey(byte[] chainKey, long transactionId) {
        Objects.requireNonNull(chainKey, "chainKey cannot be null");
        if (chainKey.length != CHAIN_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "chainKey must be " + CHAIN_KEY_LENGTH + " bytes, got " + chainKey.length);
        }
        if (chainKeysByTransaction.containsKey(transactionId)) {
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "sframe chain key install skipped, duplicate transaction {0}", transactionId);
            }
            return false;
        }
        var stored = chainKey.clone();
        chainKeysByTransaction.put(transactionId, stored);
        this.chainKey = stored;
        ciphersByKeyId.clear();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sframe chain key installed, transaction {0}", transactionId);
        return true;
    }

    /**
     * Returns whether a chain key has been installed and is available to resolve ciphers.
     *
     * @return {@code true} if a chain key is current
     */
    public boolean hasChainKey() {
        return chainKey != null;
    }

    /**
     * Returns the cipher for a key id, deriving and caching it from the current chain key on the
     * first reference.
     *
     * <p>A cached cipher is returned directly; otherwise the chain key is expanded into the cipher's
     * AES and HMAC keys and the resulting cipher is cached under the key id. The {@code counter} is
     * the frame counter associated with the request and advances the newest counter reference used to
     * evict ciphers whose key ids have fallen {@value #EVICTION_DISTANCE} counters or more behind.
     *
     * @param keyId   the key id naming which SFrame key the frame uses
     * @param counter the frame counter associated with the request
     * @return the cipher for {@code keyId}, or {@code null} if no chain key has been installed
     */
    public SFrameCipher cipherForKeyId(long keyId, long counter) {
        if (Long.compareUnsigned(counter, newestCounter) > 0) {
            newestCounter = counter;
        }
        evictStaleCiphers();
        var cached = ciphersByKeyId.get(keyId);
        if (cached != null) {
            return cached;
        }
        if (chainKey == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "sframe cipher requested for key id {0} with no chain key installed", keyId);
            return null;
        }
        var cipher = deriveCipher(chainKey);
        ciphersByKeyId.put(keyId, cipher);
        if (Log.TRACE) LOGGER.log(Level.TRACE, "sframe cipher derived for key id {0}", keyId);
        return cipher;
    }

    /**
     * Derives an {@link SFrameCipher} from a chain key by expanding it into the AES and HMAC keys and
     * installing the counter mask.
     *
     * <p>A single {@code HKDF-SHA256} expand of the chain key yields {@value #CIPHER_MATERIAL_LENGTH}
     * bytes, sliced into the {@value SFrameCipherSuite#AES_KEY_LENGTH} byte AES key followed by the
     * {@value SFrameCipherSuite#HMAC_KEY_LENGTH} byte HMAC key. The cipher's counter mask is the all zero
     * mask ({@link SFrameCipher#zeroCounterMask()}), which leaves the counter block unmasked.
     *
     * <p>The sliced AES and HMAC keys and the fresh all zero mask are derived here solely to key the
     * one returned cipher and are not retained or mutated afterward, so they are handed to
     * {@link SFrameCipher#ofOwned(SFrameCipherSuite, byte[], byte[], byte[])} by ownership transfer
     * rather than through the cloning constructor.
     *
     * @param key the {@value #CHAIN_KEY_LENGTH} byte chain key
     * @return the cipher keyed for the stream
     */
    private SFrameCipher deriveCipher(byte[] key) {
        // TODO: recover the native chain-key -> cipher-key ratchet so the counter mask salt matches WA.
        //  RE so far (re/calls decompile; the stack is facebook::sframe + wa::sframe::{cipher,crypto}): the
        //  native WASframeAESCipher consumes a 28-byte cipher key laid out as the 16-byte AES key immediately
        //  followed by the 12-byte counter mask salt (initCipher rejects keySize != 0x1c, and initCounterMask
        //  copies 12 bytes from key+16), plus a separate 32-byte HMAC-SHA256 auth key. So the salt is a
        //  derived value contiguous with the AES key, not zero. Still unrecovered: the exact HKDF (info label
        //  and output layout) the facebook::sframe SFrameKeyProvider ratchet uses to expand the 32-byte chain
        //  key into that 28 + 32 material. The participant base key uses info "e2e sframe key", but the
        //  ratchet's cipher-key label is internal to facebook::sframe and does not surface as a data string,
        //  so it cannot be reproduced byte for byte yet. Until it is, this expands the chain key with one
        //  HKDF-SHA256 expand (Cobalt's own "e2e sframe key-material" label) and installs an all-zero mask,
        //  which is Cobalt-self-consistent but not byte-compatible with native WhatsApp.
        var material = VoipCryptoNative.hkdfExpand(key, CIPHER_EXPAND_INFO, CIPHER_MATERIAL_LENGTH);
        var aesKey = new byte[SFrameCipherSuite.AES_KEY_LENGTH];
        var authKey = new byte[SFrameCipherSuite.HMAC_KEY_LENGTH];
        System.arraycopy(material, 0, aesKey, 0, SFrameCipherSuite.AES_KEY_LENGTH);
        System.arraycopy(material, SFrameCipherSuite.AES_KEY_LENGTH, authKey, 0,
                SFrameCipherSuite.HMAC_KEY_LENGTH);
        return SFrameCipher.ofOwned(suite, aesKey, authKey, SFrameCipher.zeroCounterMask());
    }

    /**
     * Evicts cached ciphers whose key ids have fallen at least {@value #EVICTION_DISTANCE} counters
     * behind the newest observed counter.
     *
     * <p>The cache evicts by counter distance rather than by key id count: the newest counter is the
     * reference, and any key id older than {@code newestCounter - EVICTION_DISTANCE} is dropped, since no
     * in window frame can still name it.
     */
    private void evictStaleCiphers() {
        if (Long.compareUnsigned(newestCounter, EVICTION_DISTANCE) < 0) {
            return;
        }
        var oldest = newestCounter - EVICTION_DISTANCE;
        var sizeBefore = ciphersByKeyId.size();
        ciphersByKeyId.keySet().removeIf(keyId -> Long.compareUnsigned(keyId, oldest) < 0);
        if (Log.DEBUG && ciphersByKeyId.size() != sizeBefore) {
            LOGGER.log(Level.DEBUG, "sframe evicted {0} stale ciphers", sizeBefore - ciphersByKeyId.size());
        }
    }
}
