package it.auties.whatsapp.socket;

import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.crypto.AesGcm;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.io.BinaryTokens;
import it.auties.whatsapp.util.Bytes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

class SocketHandshake {
    private static final byte[] NOISE_PROTOCOL = "Noise_XX_25519_AESGCM_SHA256\0\0\0\0".getBytes(StandardCharsets.UTF_8);
    private static final byte[] WHATSAPP_VERSION_HEADER = "WA".getBytes(StandardCharsets.UTF_8);
    private static final byte[] WEB_VERSION = new byte[]{6, BinaryTokens.DICTIONARY_VERSION};
    private static final byte[] WEB_PROLOGUE = Bytes.concat(WHATSAPP_VERSION_HEADER, WEB_VERSION);
    private static final byte[] MOBILE_VERSION = new byte[]{5, BinaryTokens.DICTIONARY_VERSION};
    private static final byte[] MOBILE_PROLOGUE = Bytes.concat(WHATSAPP_VERSION_HEADER, MOBILE_VERSION);

    static byte[] getPrologue(ClientType clientType) {
        return switch (clientType) {
            case WEB -> WEB_PROLOGUE;
            case MOBILE -> MOBILE_PROLOGUE;
        };
    }

    private final Keys keys;
    private byte[] hash;
    private byte[] salt;
    private byte[] cryptoKey;
    private long counter;

    SocketHandshake(Keys keys, byte[] prologue) {
        this.keys = keys;
        this.hash = NOISE_PROTOCOL;
        this.salt = NOISE_PROTOCOL;
        this.cryptoKey = NOISE_PROTOCOL;
        this.counter = 0;
        updateHash(prologue);
    }

    void updateHash(byte[] data) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            digest.update(hash);
            digest.update(data);
            this.hash = digest.digest();
        } catch (NoSuchAlgorithmException exception) {
            throw new UnsupportedOperationException("Missing sha256 implementation");
        }
    }

    byte[] cipher(byte[] bytes, boolean encrypt) {
        if (encrypt) {
            var ciphered = AesGcm.encrypt(counter++, bytes, cryptoKey, hash);
            updateHash(ciphered);
            return ciphered;
        } else {
            var deciphered = AesGcm.decrypt(counter++, bytes, cryptoKey, hash);
            updateHash(bytes);
            return deciphered;
        }
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