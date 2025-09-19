package it.auties.whatsapp.model.sync;

import it.auties.whatsapp.model.signal.SignalProtocol;

import javax.crypto.KDF;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public record MutationKeys(byte[] indexKey, byte[] encKey, byte[] macKey, byte[] snapshotMacKey, byte[] patchMacKey) {
    private static final int EXPANDED_SIZE = 160;
    private static final byte[] MUTATION_KEYS = "WhatsApp Mutation Keys".getBytes(StandardCharsets.UTF_8);

    public static MutationKeys of(byte[] key) {
        var hkdf = KDF.getInstance("HKDF-SHA256");
        var params = HKDFParameterSpec.ofExtract()
                .addIKM(new SecretKeySpec(key, "AES"))
                .thenExpand(MUTATION_KEYS, EXPANDED_SIZE);
        var expanded = (byte[]) hkdf.deriveKey("AES", params);
        var indexKey = Arrays.copyOfRange(expanded, 0, SignalProtocol.KEY_LENGTH);
        var encKey = Arrays.copyOfRange(expanded, SignalProtocol.KEY_LENGTH, SignalProtocol.KEY_LENGTH * 2);
        var macKey = Arrays.copyOfRange(expanded, SignalProtocol.KEY_LENGTH * 2, SignalProtocol.KEY_LENGTH * 3);
        var snapshotMacKey = Arrays.copyOfRange(expanded, SignalProtocol.KEY_LENGTH * 3, SignalProtocol.KEY_LENGTH * 4);
        var patchMacKey = Arrays.copyOfRange(expanded, SignalProtocol.KEY_LENGTH * 4, expanded.length);
        return new MutationKeys(indexKey, encKey, macKey, snapshotMacKey, patchMacKey);
    }
}
