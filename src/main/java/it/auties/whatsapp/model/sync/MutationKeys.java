package it.auties.whatsapp.model.sync;

import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.util.Specification;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public record MutationKeys(byte[] indexKey, byte[] encKey, byte[] macKey, byte[] snapshotMacKey, byte[] patchMacKey) {
    private static final int EXPANDED_SIZE = 160;
    private static final byte[] MUTATION_KEYS = "WhatsApp Mutation Keys".getBytes(StandardCharsets.UTF_8);

    public static MutationKeys of(byte[] key) {
        var expanded = Hkdf.extractAndExpand(key, MUTATION_KEYS, EXPANDED_SIZE);
        var indexKey = Arrays.copyOfRange(expanded, 0, Specification.Signal.KEY_LENGTH);
        var encKey = Arrays.copyOfRange(expanded, Specification.Signal.KEY_LENGTH, Specification.Signal.KEY_LENGTH * 2);
        var macKey = Arrays.copyOfRange(expanded, Specification.Signal.KEY_LENGTH * 2, Specification.Signal.KEY_LENGTH * 3);
        var snapshotMacKey = Arrays.copyOfRange(expanded, Specification.Signal.KEY_LENGTH * 3, Specification.Signal.KEY_LENGTH * 4);
        var patchMacKey = Arrays.copyOfRange(expanded, Specification.Signal.KEY_LENGTH * 4, expanded.length);
        return new MutationKeys(indexKey, encKey, macKey, snapshotMacKey, patchMacKey);
    }
}
