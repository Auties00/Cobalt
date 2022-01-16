package it.auties.whatsapp.protobuf.media;

import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.crypto.Hkdf;
import lombok.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public record MediaKeys(byte[] iv, byte[] cipherKey, byte[] macKey, byte[] ref) {
    public static MediaKeys random(@NonNull String type){
        return of(BinaryArray.random(32).data(), type);
    }

    public static MediaKeys of(byte @NonNull [] key, @NonNull String type){
        var keyName = type.getBytes(StandardCharsets.UTF_8);
        var expanded = Hkdf.extractAndExpand(key, keyName, 112);
        var iv = Arrays.copyOfRange(expanded, 0, 16);
        var cipherKey = Arrays.copyOfRange(expanded, 16, 48);
        var macKey = Arrays.copyOfRange(expanded, 48, 80);
        var ref = Arrays.copyOfRange(expanded, 80, expanded.length);
        return new MediaKeys(iv, cipherKey, macKey, ref);
    }
}
