package it.auties.whatsapp.socket;

import it.auties.whatsapp.api.WhatsappClientType;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.io.BinaryTokens;
import it.auties.whatsapp.util.Bytes;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
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

    static byte[] getPrologue(WhatsappClientType clientType) {
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
        try {
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE,
                    new SecretKeySpec(cryptoKey, "AES"),
                    makeIv()
            );
            cipher.updateAAD(hash);
            var result = cipher.doFinal(bytes);
            updateHash(encrypt ? result : bytes);
            return result;
        }catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot encrypt data", exception);
        }
    }

    private GCMParameterSpec makeIv() {
        var counter = this.counter++;
        var iv = new byte[12];
        iv[4] = (byte) (counter >> 56);
        iv[5] = (byte) (counter >> 48);
        iv[6] = (byte) (counter >> 40);
        iv[7] = (byte) (counter >> 32);
        iv[8] = (byte) (counter >> 24);
        iv[9] = (byte) (counter >> 16);
        iv[10] = (byte) (counter >> 8);
        iv[11] = (byte) (counter);
        return new GCMParameterSpec(128, iv);
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