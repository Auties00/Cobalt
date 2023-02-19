package it.auties.whatsapp.crypto;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.controller.Keys;
import lombok.NonNull;

import static it.auties.bytes.Bytes.of;
import static it.auties.whatsapp.util.Specification.Whatsapp.PROTOCOL;

public class Handshake {
    private final Keys keys;
    private Bytes hash;
    private Bytes salt;
    private Bytes cryptoKey;
    private long counter;

    public Handshake(Keys keys) {
        this.keys = keys;
        this.hash = PROTOCOL;
        this.salt = PROTOCOL;
        this.cryptoKey = PROTOCOL;
        this.counter = 0;
        updateHash(keys.prologue());
    }

    public void updateHash(byte @NonNull [] data) {
        var input = hash.append(data);
        this.hash = of(Sha256.calculate(input.toByteArray()));
    }

    public byte[] cipher(byte @NonNull [] bytes, boolean encrypt) {
        var cyphered = encrypt ? AesGmc.encrypt(counter++, bytes, cryptoKey.toByteArray(), hash.toByteArray()) : AesGmc.decrypt(counter++, bytes, cryptoKey.toByteArray(), hash.toByteArray());
        if (!encrypt) {
            updateHash(bytes);
            return cyphered;
        }
        updateHash(cyphered);
        return cyphered;
    }

    public void finish() {
        var expanded = Bytes.of(Hkdf.extractAndExpand(new byte[0], salt.toByteArray(), null, 64));
        keys.writeKey(expanded.cut(32));
        keys.readKey(expanded.slice(32));
        dispose();
    }

    private void dispose() {
        this.hash = null;
        this.salt = null;
        this.cryptoKey = null;
        this.counter = 0;
    }

    public void mixIntoKey(byte @NonNull [] bytes) {
        var expanded = Bytes.of(Hkdf.extractAndExpand(bytes, salt.toByteArray(), null, 64));
        this.salt = expanded.cut(32);
        this.cryptoKey = expanded.slice(32);
        this.counter = 0;
    }
}