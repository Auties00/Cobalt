package it.auties.whatsapp.protobuf.sync;

import it.auties.whatsapp.crypto.Hkdf;
import lombok.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public record MutationKeys(byte[] indexKey, byte[] encKey, byte[] macKey, byte[] snapshotMacKey, byte[] patchMacKey) {
    private static final byte[] MUTATION_KEYS = "WhatsApp Mutation Keys".getBytes(StandardCharsets.UTF_8);

    public static MutationKeys of(byte @NonNull [] key) {
        var expanded = Hkdf.extractAndExpand(key, MUTATION_KEYS, 160);
        var indexKey = Arrays.copyOfRange(expanded, 0, 32);
        var encKey = Arrays.copyOfRange(expanded, 32, 64);
        var macKey = Arrays.copyOfRange(expanded, 64, 96);
        var snapshotMacKey = Arrays.copyOfRange(expanded, 96, 128);
        var patchMacKey = Arrays.copyOfRange(expanded, 128, expanded.length);
        return new MutationKeys(indexKey, encKey, macKey, snapshotMacKey, patchMacKey);
    }
}
