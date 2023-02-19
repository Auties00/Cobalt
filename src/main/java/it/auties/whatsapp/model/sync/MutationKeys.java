package it.auties.whatsapp.model.sync;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.crypto.Hkdf;
import lombok.NonNull;

import java.nio.charset.StandardCharsets;

import static it.auties.whatsapp.util.Specification.Signal.KEY_LENGTH;

public record MutationKeys(byte[] indexKey, byte[] encKey, byte[] macKey, byte[] snapshotMacKey, byte[] patchMacKey) {
    private static final int EXPANDED_SIZE = 160;
    private static final byte[] MUTATION_KEYS = "WhatsApp Mutation Keys".getBytes(StandardCharsets.UTF_8);

    public static MutationKeys of(byte @NonNull [] key) {
        var buffer = Bytes.of(Hkdf.extractAndExpand(key, MUTATION_KEYS, EXPANDED_SIZE));
        var indexKey = buffer.cut(KEY_LENGTH).toByteArray();
        var encKey = buffer.slice(KEY_LENGTH, KEY_LENGTH * 2).toByteArray();
        var macKey = buffer.slice(KEY_LENGTH * 2, KEY_LENGTH * 3).toByteArray();
        var snapshotMacKey = buffer.slice(KEY_LENGTH * 3, KEY_LENGTH * 4).toByteArray();
        var patchMacKey = buffer.slice(KEY_LENGTH * 4).toByteArray();
        return new MutationKeys(indexKey, encKey, macKey, snapshotMacKey, patchMacKey);
    }
}
