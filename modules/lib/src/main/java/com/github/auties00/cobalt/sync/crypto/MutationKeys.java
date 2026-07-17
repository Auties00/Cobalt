package com.github.auties00.cobalt.sync.crypto;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.core.util.DataUtils;

import javax.crypto.Cipher;
import javax.crypto.KDF;
import javax.crypto.Mac;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Holds the five HKDF-derived keys that protect a single app state sync key
 * along with the primitives that encrypt, decrypt, and MAC sync mutations.
 *
 * <p>One instance corresponds to one {@code (keyId, keyData)} pair: callers
 * obtain a raw 32-byte sync key from the {@code AppStateSyncKey} table and
 * derive an instance via {@link #ofSyncKey(byte[])}. The derived material is
 * never persisted; it is destroyed on {@link #close()}.
 *
 * <p>The sync pipeline drives this class from {@link EncryptedMutation#of},
 * {@link DecryptedMutation.Untrusted#of}, and {@link MutationIntegrityVerifier}.
 * It is also reachable by test fixtures and tools that round-trip wire bytes
 * against a captured sync key.
 *
 * @implNote
 * This implementation derives the keys eagerly on every {@link #ofSyncKey}
 * call. WA Web's {@code WAWebSyncdCrypto.generateEncryptionKeys} memoizes the
 * HKDF expansion keyed on the base64 of the raw key, and the higher-level
 * {@code WAWebSyncdKeyCache} memoizes the raw {@code keyData} lookup against
 * IndexedDB. Cobalt collapses both layers: the caller obtains a
 * {@link MutationKeys} once and reuses it across every mutation that shares
 * the same {@code keyId}, which is structurally equivalent to the two-tier
 * WA Web cache plus a forced cache miss on key destruction.
 */
@WhatsAppWebModule(moduleName = "WAWebSyncdCryptoConst")
@WhatsAppWebModule(moduleName = "WAWebSyncdMutationsCryptoUtils")
@WhatsAppWebModule(moduleName = "WAWebSyncdCryptoHelper")
@WhatsAppWebModule(moduleName = "WAWebSyncdCrypto")
@WhatsAppWebModule(moduleName = "WAWebSyncdEncryptionManager")
@WhatsAppWebModule(moduleName = "WAWebSyncdKeyCache")
public final class MutationKeys implements AutoCloseable {
    /**
     * The logger for {@link MutationKeys}.
     */
    private static final System.Logger LOGGER = Log.get(MutationKeys.class);

    /**
     * The HKDF info string consumed by {@link #ofSyncKey(byte[])}.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCryptoConst", exports = "HKDF_INFO", adaptation = WhatsAppAdaptation.DIRECT)
    private static final String HKDF_INFO = "WhatsApp Mutation Keys";

    /**
     * The total length of the HKDF-expanded key block, in bytes.
     *
     * @implNote
     * This implementation matches {@code WAWebSyncdCryptoConst.DERIVED_KEY_LENGTH}
     * exactly, so the five 32-byte slices land at the offsets {@link #INDEX_KEY_END},
     * {@link #VALUE_ENCRYPTION_KEY_END}, {@link #VALUE_MAC_KEY_END},
     * {@link #SNAPSHOT_MAC_KEY_END}, and {@link #PATCH_MAC_KEY_END}.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCryptoConst", exports = "DERIVED_KEY_LENGTH", adaptation = WhatsAppAdaptation.DIRECT)
    private static final int DERIVED_KEY_LENGTH = 160;

    /**
     * The exclusive end offset of the index-key slice within the HKDF output.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCryptoConst", exports = "INDEX_KEY_END", adaptation = WhatsAppAdaptation.DIRECT)
    private static final int INDEX_KEY_END = 32;

    /**
     * The exclusive end offset of the value-encryption-key slice within the HKDF output.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCryptoConst", exports = "VALUE_ENCRYPTION_KEY_END", adaptation = WhatsAppAdaptation.DIRECT)
    private static final int VALUE_ENCRYPTION_KEY_END = 64;

    /**
     * The exclusive end offset of the value-MAC-key slice within the HKDF output.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCryptoConst", exports = "VALUE_MAC_KEY_END", adaptation = WhatsAppAdaptation.DIRECT)
    private static final int VALUE_MAC_KEY_END = 96;

    /**
     * The exclusive end offset of the snapshot-MAC-key slice within the HKDF output.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCryptoConst", exports = "SNAPSHOT_MAC_KEY_END", adaptation = WhatsAppAdaptation.DIRECT)
    private static final int SNAPSHOT_MAC_KEY_END = 128;

    /**
     * The exclusive end offset of the patch-MAC-key slice within the HKDF output.
     *
     * @implNote
     * This implementation places the patch-MAC key as the final slice, so this
     * constant equals {@link #DERIVED_KEY_LENGTH}.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCryptoConst", exports = "PATCH_MAC_KEY_END", adaptation = WhatsAppAdaptation.DIRECT)
    private static final int PATCH_MAC_KEY_END = 160;

    /**
     * The hex literal that WA Web parses to obtain the SET operation byte for
     * the encryption AAD.
     *
     * @implNote
     * This implementation does not consume this constant at runtime;
     * {@link #generateAssociatedData(SyncdOperation, byte[])} reads the byte
     * directly from {@link SyncdOperation#content()}. The field is retained so
     * the wire-protocol constant is visible at source-read time and tracked
     * by the source manifest.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCryptoConst", exports = "OPERATION_SET_HEX", adaptation = WhatsAppAdaptation.ADAPTED)
    @SuppressWarnings("unused")
    private static final String OPERATION_SET_HEX = "0x01";

    /**
     * The hex literal that WA Web parses to obtain the REMOVE operation byte for
     * the encryption AAD.
     *
     * @implNote
     * This implementation does not consume this constant at runtime;
     * {@link #generateAssociatedData(SyncdOperation, byte[])} reads the byte
     * directly from {@link SyncdOperation#content()}. The field is retained so
     * the wire-protocol constant is visible at source-read time and tracked
     * by the source manifest.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCryptoConst", exports = "OPERATION_REMOVE_HEX", adaptation = WhatsAppAdaptation.ADAPTED)
    @SuppressWarnings("unused")
    private static final String OPERATION_REMOVE_HEX = "0x02";

    /**
     * The length of every truncated value MAC, snapshot MAC, patch MAC, and
     * index MAC produced by this class, in bytes.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCryptoConst", exports = "MAC_LENGTH", adaptation = WhatsAppAdaptation.DIRECT)
    public static final int MAC_LENGTH = 32;

    /**
     * The length of the trailing length-suffix buffer that
     * {@link #generateMac(byte[], byte[])} appends to the HMAC input, in bytes.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCryptoConst", exports = "OCTET_LENGTH", adaptation = WhatsAppAdaptation.DIRECT)
    public static final int OCTET_LENGTH = 8;

    /**
     * The length of the AES-CBC initialisation vector prepended to every
     * encrypted mutation value, in bytes.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCryptoConst", exports = "IV_LENGTH", adaptation = WhatsAppAdaptation.DIRECT)
    public static final int IV_LENGTH = 16;

    /**
     * The minimum combined index plus value length that
     * {@link #generatePadding(int, int)} pads up to.
     *
     * @implNote
     * This implementation observes that the current server value is {@code 0},
     * so {@link #generatePadding(int, int)} always returns an empty array.
     * Kept as a named constant so a future server bump produces a one-line
     * code change rather than a literal sprinkled across call sites.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCryptoConst", exports = "MAX_OF_MIN_DATA_LENGTH", adaptation = WhatsAppAdaptation.DIRECT)
    public static final int MAX_OF_MIN_DATA_LENGTH = 0;

    /**
     * The HMAC-SHA256 key used by {@link #generateIndexMac(byte[])}.
     */
    private final SecretKeySpec indexKey;

    /**
     * The AES-256-CBC key used by {@link #generateCipherText(byte[])} and
     * {@link #decryptCipherText(byte[], byte[])}.
     */
    private final SecretKeySpec valueEncryptionKey;

    /**
     * The HMAC-SHA512 key used by {@link #generateMac(byte[], byte[])}.
     */
    private final SecretKeySpec valueMacKey;

    /**
     * The HMAC-SHA256 key used by
     * {@link MutationIntegrityVerifier#computeSnapshotMac(SecretKeySpec, byte[], long, com.github.auties00.cobalt.wire.linked.sync.SyncPatchType)}.
     */
    private final SecretKeySpec snapshotMacKey;

    /**
     * The HMAC-SHA256 key used by
     * {@link MutationIntegrityVerifier#computePatchMac(SecretKeySpec, byte[], java.util.SequencedCollection, long, com.github.auties00.cobalt.wire.linked.sync.SyncPatchType)}.
     */
    private final SecretKeySpec patchMacKey;


    /**
     * Constructs an instance bound to the five pre-sliced key specs.
     *
     * @param indexKey           the HMAC-SHA256 key for index MACs
     * @param valueEncryptionKey the AES-256-CBC key for mutation values
     * @param valueMacKey        the HMAC-SHA512 key for value MACs
     * @param snapshotMacKey     the HMAC-SHA256 key for snapshot MACs
     * @param patchMacKey        the HMAC-SHA256 key for patch MACs
     * @throws NullPointerException if any argument is {@code null}
     */
    private MutationKeys(SecretKeySpec indexKey, SecretKeySpec valueEncryptionKey, SecretKeySpec valueMacKey, SecretKeySpec snapshotMacKey, SecretKeySpec patchMacKey) {
        this.indexKey = Objects.requireNonNull(indexKey, "Index key cannot be null");
        this.valueEncryptionKey = Objects.requireNonNull(valueEncryptionKey, "Value encryption key cannot be null");
        this.valueMacKey = Objects.requireNonNull(valueMacKey, "Value MAC key cannot be null");
        this.snapshotMacKey = Objects.requireNonNull(snapshotMacKey, "Snapshot MAC key cannot be null") ;
        this.patchMacKey = Objects.requireNonNull(patchMacKey, "Patch MAC key cannot be null");
    }

    /**
     * Derives the five mutation keys for a single sync key via HKDF-SHA256.
     *
     * <p>Called by the sync pipeline at the boundary between the
     * {@code AppStateSyncKey} table (raw 32-byte secrets shared with the
     * companion device) and the per-mutation crypto helpers in this class. The
     * returned instance is reusable across every mutation that names the same
     * {@code keyId} on the wire.
     *
     * @implNote
     * This implementation performs HKDF-Extract with no salt (RFC 5869 default
     * of 32 zero bytes), then HKDF-Expand with info {@code "WhatsApp Mutation
     * Keys"} to {@value #DERIVED_KEY_LENGTH} bytes, and slices the output at
     * the WA Web offsets. WA Web wraps the same expansion in
     * {@code WAMemoizeCache.memoizeWithArgs} keyed on the base64 of the raw
     * key; Cobalt skips the cache and instead derives once per pipeline pass.
     *
     * @param syncKey the raw 32-byte sync key bytes
     * @return a fresh {@link MutationKeys} bound to the five derived slices
     * @throws NullPointerException     if {@code syncKey} is {@code null}
     * @throws IllegalArgumentException if {@code syncKey} is not exactly 32 bytes
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCrypto", exports = "generateEncryptionKeys", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSyncdCryptoHelper", exports = "generateEncryptionKeysUnmemoized", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebSyncdKeyCache", exports = "getKeyData", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSyncdKeyCache", exports = "clearSyncKeysCache", adaptation = WhatsAppAdaptation.ADAPTED)
    public static MutationKeys ofSyncKey(byte[] syncKey) {
        if (syncKey == null) {
            throw new NullPointerException("Sync key cannot be null");
        }

        if (syncKey.length != 32) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "invalid sync key length {0}, expected 32", syncKey.length);
            throw new IllegalArgumentException("Sync key must be 32 bytes, got " + syncKey.length);
        }

        try {
            var kdf = KDF.getInstance("HKDF-SHA256");
            var params = HKDFParameterSpec.ofExtract()
                    .addIKM(syncKey)
                    .thenExpand(HKDF_INFO.getBytes(StandardCharsets.UTF_8), DERIVED_KEY_LENGTH);
            var derivedBytes = kdf.deriveData(params);

            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "derived mutation keys via hkdf-sha256");

            return new MutationKeys(
                    new SecretKeySpec(derivedBytes, 0, INDEX_KEY_END, "HmacSHA256"),
                    new SecretKeySpec(derivedBytes, INDEX_KEY_END, VALUE_ENCRYPTION_KEY_END - INDEX_KEY_END, "AES"),
                    new SecretKeySpec(derivedBytes, VALUE_ENCRYPTION_KEY_END, VALUE_MAC_KEY_END - VALUE_ENCRYPTION_KEY_END, "HmacSHA512"),
                    new SecretKeySpec(derivedBytes, VALUE_MAC_KEY_END, SNAPSHOT_MAC_KEY_END - VALUE_MAC_KEY_END, "HmacSHA256"),
                    new SecretKeySpec(derivedBytes, SNAPSHOT_MAC_KEY_END, PATCH_MAC_KEY_END - SNAPSHOT_MAC_KEY_END, "HmacSHA256")
            );
        } catch (GeneralSecurityException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to derive mutation keys", e);
            throw new InternalError("Failed to derive keys", e);
        }
    }

    /**
     * Returns the HMAC-SHA256 of {@code indexBytes} under this instance's
     * index key.
     *
     * <p>Used both to compute the {@code indexMac} attached to every outgoing
     * mutation (by {@link EncryptedMutation#of}) and to verify the wire
     * {@code indexMac} on an incoming mutation (by
     * {@link DecryptedMutation.Untrusted#of}). The index bytes are the UTF-8
     * encoding of the JSON-array index string, for example
     * {@code ["archive","jid"]}.
     *
     * @param indexBytes the raw index bytes to authenticate
     * @return the 32-byte HMAC-SHA256 value
     * @throws GeneralSecurityException if the JCE HMAC primitive fails
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCrypto", exports = "generateIndexMac", adaptation = WhatsAppAdaptation.DIRECT)
    public byte[] generateIndexMac(byte[] indexBytes) throws GeneralSecurityException {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(indexKey);
        return mac.doFinal(indexBytes);
    }

    /**
     * Slices the trailing {@value #MAC_LENGTH} bytes off an
     * {@code IV || ciphertext || valueMac} buffer.
     *
     * <p>Used by both the encryption and decryption paths to recover the
     * authenticator without recomputing it. The argument is the wire
     * {@code indexAndValueCipherText} blob exactly as it appears in
     * {@code SyncdMutation.value.blob}.
     *
     * @param encryptedValue the wire-format encrypted value
     * @return the {@value #MAC_LENGTH}-byte trailing MAC
     * @throws IllegalArgumentException if {@code encryptedValue} is shorter than {@value #MAC_LENGTH} bytes
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCrypto", exports = "valueMacFromIndexAndValueCipherText", adaptation = WhatsAppAdaptation.DIRECT)
    public static byte[] valueMacFromIndexAndValueCipherText(byte[] encryptedValue) {
        if (encryptedValue.length < MAC_LENGTH) {
            throw new IllegalArgumentException("Encrypted value too short to contain a MAC");
        }
        return Arrays.copyOfRange(
                encryptedValue,
                encryptedValue.length - MAC_LENGTH,
                encryptedValue.length
        );
    }

    /**
     * Builds the associated-data prefix that authenticates the
     * {@code (operation, keyId)} pair against the encrypted value.
     *
     * <p>Mixed into the value MAC so that flipping a SET into a REMOVE, or
     * replaying a ciphertext under a different sync key, fails MAC verification
     * rather than silently succeeding. The layout is
     * {@snippet :
     *     // [0x01 for SET, 0x02 for REMOVE]
     *     // || keyId (raw bytes, typically a 6-byte timestamp-based id)
     * }
     *
     * @implNote
     * This implementation reads the operation byte from
     * {@link SyncdOperation#content()}, sidestepping WA Web's
     * {@code parseInt(OPERATION_*_HEX, 16)} indirection. The output is byte
     * equivalent.
     *
     * @param operation the mutation operation
     * @param keyId     the raw sync key id bytes
     * @return a new byte array {@code [opByte] || keyId}
     * @throws NullPointerException if {@code operation} or {@code keyId} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdMutationsCryptoUtils", exports = "generateAssociatedData", adaptation = WhatsAppAdaptation.DIRECT)
    public static byte[] generateAssociatedData(SyncdOperation operation, byte[] keyId) {
        Objects.requireNonNull(operation, "Operation cannot be null");
        Objects.requireNonNull(keyId, "Key ID cannot be null");
        var opByte = operation.content();
        var result = new byte[1 + keyId.length];
        result[0] = opByte;
        System.arraycopy(keyId, 0, result, 1, keyId.length);
        return result;
    }

    /**
     * Produces the random padding appended to a plaintext before AES-CBC encryption.
     *
     * <p>Encryption-side helper used by {@link EncryptedMutation#of}. The
     * padding is a length-disguising filler: the combined
     * {@code (indexLength + valueLength)} is padded up to a server-defined
     * minimum so that very small mutations do not leak their size to a wire
     * observer.
     *
     * @implNote
     * This implementation observes that the current server value
     * {@link #MAX_OF_MIN_DATA_LENGTH} is {@code 0}, so the returned array is
     * always empty and the random fill branch is dead code. The branch is
     * retained against a future non-zero bump.
     *
     * @param indexLength the length of the UTF-8-encoded index in bytes
     * @param valueLength the length of the encoded {@code SyncActionValue} in bytes
     * @return the padding bytes
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdMutationsCryptoUtils", exports = "generatePadding", adaptation = WhatsAppAdaptation.DIRECT)
    public static byte[] generatePadding(int indexLength, int valueLength) {
        var paddingLength = Math.max(0, MAX_OF_MIN_DATA_LENGTH - indexLength - valueLength);
        var padding = new byte[paddingLength];
        if (paddingLength > 0) {
            DataUtils.randomByteArray(padding, 0, paddingLength);
        }
        return padding;
    }

    /**
     * Encrypts a plaintext under this instance's value-encryption key, drawing
     * a fresh random IV.
     *
     * <p>Convenience overload around
     * {@link #generateCipherText(byte[], byte[])} used on the outgoing path.
     * Every call returns a new {@code [IV || ciphertext]} pair, so the wire
     * value differs even when the plaintext is identical across calls.
     *
     * @param plaintext the plaintext to encrypt
     * @return the concatenation {@code IV (16 bytes) || ciphertext}
     * @throws GeneralSecurityException if the JCE AES-CBC primitive fails
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdMutationsCryptoUtils", exports = "generateCipherText", adaptation = WhatsAppAdaptation.ADAPTED)
    public byte[] generateCipherText(byte[] plaintext) throws GeneralSecurityException {
        var iv = new byte[IV_LENGTH];
        DataUtils.randomByteArray(iv, 0, IV_LENGTH);
        return generateCipherText(iv, plaintext);
    }

    /**
     * Encrypts a plaintext under this instance's value-encryption key with a
     * caller-supplied IV and prepends the IV to the ciphertext.
     *
     * <p>The fixed-IV overload exists for byte-exact fixture replay against a
     * captured ciphertext; production paths always go through
     * {@link #generateCipherText(byte[])}.
     *
     * @param iv        the {@value #IV_LENGTH}-byte IV
     * @param plaintext the plaintext to encrypt
     * @return the concatenation {@code iv || AES-CBC(plaintext)}
     * @throws GeneralSecurityException if the JCE AES-CBC primitive fails
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdMutationsCryptoUtils", exports = "generateCipherText", adaptation = WhatsAppAdaptation.DIRECT)
    public byte[] generateCipherText(byte[] iv, byte[] plaintext) throws GeneralSecurityException {
        var ivSpec = new IvParameterSpec(iv);
        var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, valueEncryptionKey, ivSpec);
        var ciphertext = cipher.doFinal(plaintext);
        var result = new byte[IV_LENGTH + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, IV_LENGTH);
        System.arraycopy(ciphertext, 0, result, IV_LENGTH, ciphertext.length);
        return result;
    }

    /**
     * Computes the truncated HMAC-SHA512 over the AAD, the ciphertext, and a
     * trailing 8-byte length suffix.
     *
     * <p>Produces the trailing 32 bytes of every wire {@code encryptedValue}.
     * The same routine runs on both sides: encryption emits it, decryption
     * recomputes it and compares against the wire value.
     *
     * @implNote
     * This implementation appends an 8-byte buffer whose last byte holds
     * {@code associatedData.length}. The buffer's other bytes are zero; the
     * length therefore caps at 255, which is consistent with the WA Web
     * encoding because the AAD is always {@code 1 + keyId.length} and the
     * key id is itself short. The full HMAC-SHA512 output is then truncated
     * to {@link #MAC_LENGTH} bytes.
     *
     * @param associatedData the AAD bytes (typically from {@link #generateAssociatedData})
     * @param ciphertext     the {@code IV || ciphertext} bytes to authenticate
     * @return the {@value #MAC_LENGTH}-byte truncated MAC
     * @throws GeneralSecurityException if the JCE HMAC primitive fails
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdMutationsCryptoUtils", exports = "generateMac", adaptation = WhatsAppAdaptation.DIRECT)
    public byte[] generateMac(byte[] associatedData, byte[] ciphertext) throws GeneralSecurityException {
        var lengthSuffix = new byte[OCTET_LENGTH];
        lengthSuffix[OCTET_LENGTH - 1] = (byte) associatedData.length;
        var mac = Mac.getInstance("HmacSHA512");
        mac.init(valueMacKey);
        mac.update(associatedData);
        mac.update(ciphertext);
        mac.update(lengthSuffix);
        var fullMac = mac.doFinal();
        return Arrays.copyOf(fullMac, MAC_LENGTH);
    }

    /**
     * Decrypts an AES-CBC ciphertext under this instance's value-encryption key.
     *
     * <p>Driven by {@link DecryptedMutation.Untrusted#of} after the value MAC
     * has validated. The IV must be the leading {@value #IV_LENGTH} bytes of
     * the wire {@code encryptedValue} and the ciphertext must be the slice that
     * sits between the IV and the trailing MAC.
     *
     * @param iv         the {@value #IV_LENGTH}-byte IV
     * @param ciphertext the ciphertext slice (no IV, no trailing MAC)
     * @return the plaintext bytes
     * @throws GeneralSecurityException if the JCE AES-CBC primitive fails or the padding is invalid
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdMutationsCryptoUtils", exports = "decryptCipherText", adaptation = WhatsAppAdaptation.DIRECT)
    public byte[] decryptCipherText(byte[] iv, byte[] ciphertext) throws GeneralSecurityException {
        var ivSpec = new IvParameterSpec(iv);
        var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, valueEncryptionKey, ivSpec);
        return cipher.doFinal(ciphertext);
    }

    /**
     * Destroys every {@link SecretKeySpec} held by this instance.
     *
     * @implNote
     * This implementation calls {@link javax.security.auth.Destroyable#destroy()}
     * on each key in turn and swallows {@link DestroyFailedException}. The
     * JDK's default {@link SecretKeySpec} does not implement destruction and
     * throws unconditionally; the swallow is therefore expected on a stock
     * JCE provider. Idempotent: a second {@link #close()} call has no effect.
     */
    @Override
    public void close() {
        try {
            indexKey.destroy();
        }catch (DestroyFailedException _) {

        }
        try {
            valueEncryptionKey.destroy();
        }catch (DestroyFailedException _) {

        }
        try {
            valueMacKey.destroy();
        }catch (DestroyFailedException _) {

        }
        try {
            snapshotMacKey.destroy();
        }catch (DestroyFailedException _) {

        }
        try {
            patchMacKey.destroy();
        }catch (DestroyFailedException _) {

        }
    }

    /**
     * Returns a fixed placeholder string.
     *
     * @implNote
     * This implementation deliberately returns a constant so that accidental
     * inclusion of a {@link MutationKeys} instance in a log statement does
     * not leak hex-encoded key material via the auto-generated
     * {@code Object.toString} on {@link SecretKeySpec}.
     *
     * @return the placeholder {@code "AppStateSyncKeys"}
     */
    @Override
    public String toString() {
        return "AppStateSyncKeys";
    }

    /**
     * Returns the HMAC-SHA256 key used for index MACs.
     *
     * @return the index MAC key
     */
    public SecretKeySpec indexKey() {
        return indexKey;
    }

    /**
     * Returns the AES-256-CBC key used for value encryption and decryption.
     *
     * @return the value encryption key
     */
    public SecretKeySpec valueEncryptionKey() {
        return valueEncryptionKey;
    }

    /**
     * Returns the HMAC-SHA512 key used for value MACs.
     *
     * @return the value MAC key
     */
    public SecretKeySpec valueMacKey() {
        return valueMacKey;
    }

    /**
     * Returns the HMAC-SHA256 key used for snapshot MACs.
     *
     * <p>Consumed by {@link MutationIntegrityVerifier#computeSnapshotMac} and
     * the outgoing-patch MAC computation helpers, not by the encrypt or decrypt
     * paths.
     *
     * @return the snapshot MAC key
     */
    public SecretKeySpec snapshotMacKey() {
        return snapshotMacKey;
    }

    /**
     * Returns the HMAC-SHA256 key used for patch MACs.
     *
     * <p>Consumed by {@link MutationIntegrityVerifier#computePatchMac} and the
     * outgoing-patch MAC computation helpers, not by the encrypt or decrypt
     * paths.
     *
     * @return the patch MAC key
     */
    public SecretKeySpec patchMacKey() {
        return patchMacKey;
    }
}
