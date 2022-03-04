package it.auties.whatsapp.crypto;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.manager.WhatsappKeys;
import lombok.NonNull;
import lombok.SneakyThrows;

import static it.auties.bytes.Bytes.of;

public class Handshake {
    public static final byte[] PROLOGUE = new byte[]{87, 65, 5, 2};
    private static final Bytes PROTOCOL = Bytes.of("Noise_XX_25519_AESGCM_SHA256\0\0\0\0");

    private WhatsappKeys keys;
    private Bytes hash;
    private Bytes salt;
    private Bytes cryptoKey;
    private long counter;

    public void start(WhatsappKeys keys) {
        this.hash = PROTOCOL;
        this.salt = PROTOCOL;
        this.cryptoKey = PROTOCOL;
        this.keys = keys;
        this.counter = 0;
        updateHash(PROLOGUE);
    }

    @SneakyThrows
    public void updateHash(byte @NonNull [] data) {
        var input = hash.append(data);
        this.hash = of(Sha256.calculate(input.toByteArray()));
    }

    @SneakyThrows
    public byte[] cipher(byte @NonNull [] bytes, boolean encrypt) {
        var cyphered = AesGmc.with(cryptoKey, hash.toByteArray(), counter++, encrypt)
                .process(bytes);
        if (!encrypt) {
            updateHash(bytes);
            return cyphered;
        }

        updateHash(cyphered);
        return cyphered;
    }

    public void finish() {
        var expanded = extractAndExpandWithHash(new byte[0]);
        keys.writeKey(expanded.cut(32))
                .readKey(expanded.slice(32));
    }

    public void mixIntoKey(byte @NonNull [] bytes) {
        var expanded = extractAndExpandWithHash(bytes);
        this.salt = expanded.cut(32);
        this.cryptoKey = expanded.slice(32);
        this.counter = 0;
    }

    private Bytes extractAndExpandWithHash(byte @NonNull [] key) {
        var extracted = Hkdf.extract(salt.toByteArray(), key);
        var expanded = Hkdf.expand(extracted, null, 64);
        return of(expanded);
    }
}