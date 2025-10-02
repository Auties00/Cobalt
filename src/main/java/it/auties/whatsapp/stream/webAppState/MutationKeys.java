package it.auties.whatsapp.stream.webAppState;

import javax.crypto.KDF;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public record MutationKeys(byte[] indexKey, byte[] encKey, byte[] macKey, byte[] snapshotMacKey, byte[] patchMacKey) {
    private static final int EXPANDED_SIZE = 160;
    private static final int KEY_LENGTH = 32;
    private static final byte[] MUTATION_KEYS = "WhatsApp Mutation Keys".getBytes(StandardCharsets.UTF_8);

    public static MutationKeys of(byte[] key) {
        try {
            var hkdf = KDF.getInstance("HKDF-SHA256");
            var params = HKDFParameterSpec.ofExtract()
                    .addIKM(new SecretKeySpec(key, "AES"))
                    .thenExpand(MUTATION_KEYS, EXPANDED_SIZE);
            var expanded = hkdf.deriveData(params);
            var indexKey = Arrays.copyOfRange(expanded, 0, KEY_LENGTH);
            var encKey = Arrays.copyOfRange(expanded, KEY_LENGTH, KEY_LENGTH * 2);
            var macKey = Arrays.copyOfRange(expanded, KEY_LENGTH * 2, KEY_LENGTH * 3);
            var snapshotMacKey = Arrays.copyOfRange(expanded, KEY_LENGTH * 3, KEY_LENGTH * 4);
            var patchMacKey = Arrays.copyOfRange(expanded, KEY_LENGTH * 4, expanded.length);
            return new MutationKeys(indexKey, encKey, macKey, snapshotMacKey, patchMacKey);
        }catch (GeneralSecurityException e) {
            throw new RuntimeException("Cannot derive mutation keys", e);
        }
    }
}
