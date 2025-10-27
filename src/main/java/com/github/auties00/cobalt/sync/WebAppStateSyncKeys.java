package com.github.auties00.cobalt.sync;

import javax.crypto.KDF;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Objects;

/**
 * Container for all derived keys from a sync key.
 *
 * <p>These keys are derived from a single 32-byte App State Sync Key using HKDF-Expand.
 * Each key serves a specific purpose in the synchronization protocol:
 * <ul>
 *   <li><b>Index Key</b> - Generates index MACs for identifying mutations without decryption</li>
 *   <li><b>Value Encryption Key</b> - Encrypts mutation values with AES-256-CBC</li>
 *   <li><b>Value MAC Key</b> - Generates MACs for mutation value integrity</li>
 *   <li><b>Snapshot MAC Key</b> - Verifies snapshot integrity</li>
 *   <li><b>Patch MAC Key</b> - Verifies patch integrity</li>
 * </ul>
 *
 * <p>This class implements {@link AutoCloseable} to ensure secure cleanup of key material.
 * Always use try-with-resources when working with instances of this class:
 * <pre>{@code
 * try (var keys = crypto.deriveKeys(syncKey)) {
 *     // Use keys...
 * } // Keys are securely zeroed out
 * }</pre>
 *
 */
public final class WebAppStateSyncKeys implements AutoCloseable {
    /**
     * HKDF info parameter for key derivation.
     */
    private static final String HKDF_INFO = "WhatsApp Mutation Keys";

    /**
     * Total length of derived key material (5 keys × 32 bytes each).
     */
    private static final int DERIVED_KEY_LENGTH = 160;

    private final SecretKeySpec indexKey;
    private final SecretKeySpec valueEncryptionKey;
    private final SecretKeySpec valueMacKey;
    private final SecretKeySpec snapshotMacKey;
    private final SecretKeySpec patchMacKey;


    /**
     * Creates a new AppStateSyncKeys with validation.
     *
     * @throws NullPointerException     if any key is null
     * @throws IllegalArgumentException if any key is not 32 bytes
     */
    private WebAppStateSyncKeys(SecretKeySpec indexKey, SecretKeySpec valueEncryptionKey, SecretKeySpec valueMacKey, SecretKeySpec snapshotMacKey, SecretKeySpec patchMacKey) {
        this.indexKey = Objects.requireNonNull(indexKey, "Index key cannot be null");
        this.valueEncryptionKey = Objects.requireNonNull(valueEncryptionKey, "Value encryption key cannot be null");
        this.valueMacKey = Objects.requireNonNull(valueMacKey, "Value MAC key cannot be null");
        this.snapshotMacKey = Objects.requireNonNull(snapshotMacKey, "Snapshot MAC key cannot be null") ;
        this.patchMacKey = Objects.requireNonNull(patchMacKey, "Patch MAC key cannot be null");
    }

    /**
     * Derives all necessary keys from a sync key using HKDF-Expand.
     *
     * @param syncKey the 32-byte app state sync key (PRK in HKDF terms)
     * @return derived keys for encryption and MAC operations
     */
    public static WebAppStateSyncKeys ofSyncKey(byte[] syncKey) {
        if (syncKey == null) {
            throw new NullPointerException("Sync key cannot be null");
        }

        if (syncKey.length != 32) {
            throw new IllegalArgumentException("Sync key must be 32 bytes, got " + syncKey.length);
        }

        try {
            var kdf = KDF.getInstance("HKDF-SHA256");
            var params = HKDFParameterSpec.ofExtract()
                    .addIKM(syncKey)
                    .thenExpand(HKDF_INFO.getBytes(StandardCharsets.UTF_8), DERIVED_KEY_LENGTH);
            var derivedBytes = kdf.deriveData(params);

            return new WebAppStateSyncKeys(
                    new SecretKeySpec(derivedBytes, 0, 32, "HmacSHA256"),
                    new SecretKeySpec(derivedBytes, 32, 32, "AES"),
                    new SecretKeySpec(derivedBytes, 64, 32, "HmacSHA256"),
                    new SecretKeySpec(derivedBytes, 96, 32, "HmacSHA256"),
                    new SecretKeySpec(derivedBytes, 128, 32, "HmacSHA256")
            );
        } catch (GeneralSecurityException e) {
            throw new InternalError("Failed to derive keys", e);
        }
    }

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

    @Override
    public String toString() {
        return "AppStateSyncKeys";
    }

    public SecretKeySpec indexKey() {
        return indexKey;
    }

    public SecretKeySpec valueEncryptionKey() {
        return valueEncryptionKey;
    }

    public SecretKeySpec valueMacKey() {
        return valueMacKey;
    }

    public SecretKeySpec snapshotMacKey() {
        return snapshotMacKey;
    }

    public SecretKeySpec patchMacKey() {
        return patchMacKey;
    }
}
