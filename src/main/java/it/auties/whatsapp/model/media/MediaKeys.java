package it.auties.whatsapp.model.media;

import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.util.Bytes;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static it.auties.whatsapp.util.SignalConstants.IV_LENGTH;
import static it.auties.whatsapp.util.SignalConstants.KEY_LENGTH;

public record MediaKeys(byte[] mediaKey, byte[] iv, byte[] cipherKey, byte[] macKey, byte[] ref) {
    private static final int EXPANDED_SIZE = 112;

    public static MediaKeys random(String type) {
        return of(Bytes.random(32), type);
    }

    public static MediaKeys of(byte[] key, String type) {
        var keyName = type.getBytes(StandardCharsets.UTF_8);
        var expanded = Hkdf.extractAndExpand(key, keyName, EXPANDED_SIZE);
        var iv = Arrays.copyOfRange(expanded, 0, IV_LENGTH);
        var cipherKey = Arrays.copyOfRange(expanded, IV_LENGTH, IV_LENGTH + KEY_LENGTH);
        var macKey = Arrays.copyOfRange(expanded, IV_LENGTH + KEY_LENGTH, IV_LENGTH + KEY_LENGTH * 2);
        var ref = Arrays.copyOfRange(expanded, IV_LENGTH + KEY_LENGTH * 2, expanded.length);
        return new MediaKeys(key, iv, cipherKey, macKey, ref);
    }
}
