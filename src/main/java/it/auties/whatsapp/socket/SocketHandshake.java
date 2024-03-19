package it.auties.whatsapp.socket;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.crypto.AesGcm;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.crypto.Sha256;
import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.Specification;

import java.util.Arrays;

class SocketHandshake {
    private final Keys keys;
    private byte[] hash;
    private byte[] salt;
    private byte[] cryptoKey;
    private long counter;

    SocketHandshake(Keys keys, byte[] prologue) {
        this.keys = keys;
        this.hash = Specification.Whatsapp.NOISE_PROTOCOL;
        this.salt = Specification.Whatsapp.NOISE_PROTOCOL;
        this.cryptoKey = Specification.Whatsapp.NOISE_PROTOCOL;
        this.counter = 0;
        updateHash(prologue);
    }

    void updateHash(byte[] data) {
        var input = Bytes.concat(hash, data);
        this.hash = Sha256.calculate(input);
    }

    byte[] cipher(byte[] bytes, boolean encrypt) {
        var cyphered = encrypt ? AesGcm.encrypt(counter++, bytes, cryptoKey, hash) : AesGcm.decrypt(counter++, bytes, cryptoKey, hash);
        if (!encrypt) {
            updateHash(bytes);
            return cyphered;
        }
        updateHash(cyphered);
        return cyphered;
    }

    void finish() {
        var expanded = Hkdf.extractAndExpand(new byte[0], salt, null, 64);
        keys.setWriteKey(Arrays.copyOfRange(expanded, 0, 32));
        keys.setReadKey(Arrays.copyOfRange(expanded, 32, 64));
        dispose();
    }

    void mixIntoKey(byte[] bytes) {
        var expanded = Hkdf.extractAndExpand(bytes, salt, null, 64);
        this.salt = Arrays.copyOfRange(expanded, 0, 32);
        this.cryptoKey = Arrays.copyOfRange(expanded, 32, 64);
        this.counter = 0;
    }

    void dispose() {
        this.hash = null;
        this.salt = null;
        this.cryptoKey = null;
        this.counter = 0;
    }
}