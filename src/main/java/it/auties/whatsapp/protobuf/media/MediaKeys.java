package it.auties.whatsapp.protobuf.media;

import it.auties.whatsapp.crypto.Hkdf;
import lombok.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public record MediaKeys(byte[] iv, byte[] cipherKey, byte[] macKey, byte[] ref) {
    public static MediaKeys ofProvider(@NonNull AttachmentProvider provider){
        var keyName = provider.keyName().getBytes(StandardCharsets.UTF_8);
        var expanded = Hkdf.expand(provider.key(), keyName, 112);
        var iv = Arrays.copyOfRange(expanded, 0, 16);
        var cipherKey = Arrays.copyOfRange(expanded, 16, 48);
        var macKey = Arrays.copyOfRange(expanded, 48, 80);
        var ref = Arrays.copyOfRange(expanded, 80, expanded.length);
        return new MediaKeys(iv, cipherKey, macKey, ref);
    }
}
