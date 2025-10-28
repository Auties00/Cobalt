package com.github.auties00.cobalt.sync.crypto;

import javax.crypto.KDF;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Objects;

public final class MutationKeys implements AutoCloseable {
    private static final String HKDF_INFO = "WhatsApp Mutation Keys";
    private static final int DERIVED_KEY_LENGTH = 160;

    private final SecretKeySpec indexKey;
    private final SecretKeySpec valueEncryptionKey;
    private final SecretKeySpec valueMacKey;
    private final SecretKeySpec snapshotMacKey;
    private final SecretKeySpec patchMacKey;


    private MutationKeys(SecretKeySpec indexKey, SecretKeySpec valueEncryptionKey, SecretKeySpec valueMacKey, SecretKeySpec snapshotMacKey, SecretKeySpec patchMacKey) {
        this.indexKey = Objects.requireNonNull(indexKey, "Index key cannot be null");
        this.valueEncryptionKey = Objects.requireNonNull(valueEncryptionKey, "Value encryption key cannot be null");
        this.valueMacKey = Objects.requireNonNull(valueMacKey, "Value MAC key cannot be null");
        this.snapshotMacKey = Objects.requireNonNull(snapshotMacKey, "Snapshot MAC key cannot be null") ;
        this.patchMacKey = Objects.requireNonNull(patchMacKey, "Patch MAC key cannot be null");
    }

    public static MutationKeys ofSyncKey(byte[] syncKey) {
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

            return new MutationKeys(
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
