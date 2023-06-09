package it.auties.whatsapp.model.sync;

import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.util.Spec;
import lombok.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public record MutationKeys(byte[] indexKey, byte[] encKey, byte[] macKey, byte[] snapshotMacKey, byte[] patchMacKey) {
    private static final int EXPANDED_SIZE = 160;
    private static final byte[] MUTATION_KEYS = "WhatsApp Mutation Keys".getBytes(StandardCharsets.UTF_8);

    public static MutationKeys of(byte @NonNull [] key) {
        var expanded = Hkdf.extractAndExpand(key, MUTATION_KEYS, EXPANDED_SIZE);
        var indexKey = Arrays.copyOfRange(expanded, 0, Spec.Signal.KEY_LENGTH);
        var encKey = Arrays.copyOfRange(expanded, Spec.Signal.KEY_LENGTH, Spec.Signal.KEY_LENGTH * 2);
        var macKey = Arrays.copyOfRange(expanded, Spec.Signal.KEY_LENGTH * 2, Spec.Signal.KEY_LENGTH * 3);
        var snapshotMacKey = Arrays.copyOfRange(expanded, Spec.Signal.KEY_LENGTH * 3, Spec.Signal.KEY_LENGTH * 4);
        var patchMacKey = Arrays.copyOfRange(expanded, Spec.Signal.KEY_LENGTH * 4, expanded.length);
        return new MutationKeys(indexKey, encKey, macKey, snapshotMacKey, patchMacKey);
    }
}
