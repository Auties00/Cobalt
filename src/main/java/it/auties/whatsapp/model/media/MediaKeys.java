package it.auties.whatsapp.model.media;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.util.SignalSpecification;
import lombok.NonNull;

import java.nio.charset.StandardCharsets;

public record MediaKeys(byte[] mediaKey, byte[] iv, byte[] cipherKey, byte[] macKey, byte[] ref)
        implements SignalSpecification {
    private static final int EXPANDED_SIZE = 112;

    public static MediaKeys random(@NonNull String type) {
        return of(Bytes.ofRandom(32)
                .toByteArray(), type);
    }

    public static MediaKeys of(byte @NonNull [] key, @NonNull String type) {
        var encodedKey = type.getBytes(StandardCharsets.UTF_8);
        var buffer = Bytes.of(Hkdf.extractAndExpand(key, encodedKey, EXPANDED_SIZE));
        var iv = buffer.cut(IV_LENGTH)
                .toByteArray();
        var cipherKey = buffer.slice(IV_LENGTH, IV_LENGTH + KEY_LENGTH)
                .toByteArray();
        var macKey = buffer.slice(IV_LENGTH + KEY_LENGTH, IV_LENGTH + (KEY_LENGTH * 2))
                .toByteArray();
        var ref = buffer.slice(IV_LENGTH + (KEY_LENGTH * 2))
                .toByteArray();
        return new MediaKeys(key, iv, cipherKey, macKey, ref);
    }
}
