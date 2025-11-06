package com.github.auties00.cobalt.socket;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import java.nio.charset.StandardCharsets;
import java.security.*;

final class SocketHandshake implements AutoCloseable {
    private static final byte[] NOISE_PROTOCOL = "Noise_XX_25519_AESGCM_SHA256\0\0\0\0".getBytes(StandardCharsets.UTF_8);
    private static final byte[] FINISH_KEY = new byte[0];

    private final KDF kdf;
    private final MessageDigest hashDigest;
    private final Cipher cipher;

    private byte[] hash;
    private SecretKeySpec salt;
    private SecretKeySpec cryptoKey;
    private long counter;

    SocketHandshake(byte[] prologue) throws NoSuchAlgorithmException, NoSuchPaddingException {
        this.kdf = KDF.getInstance("HKDF-SHA256");
        this.hashDigest = MessageDigest.getInstance("SHA-256");
        this.cipher = Cipher.getInstance("AES/GCM/NoPadding");
        this.hash = NOISE_PROTOCOL;
        this.salt = new SecretKeySpec(NOISE_PROTOCOL, "AES");
        this.cryptoKey = new SecretKeySpec(NOISE_PROTOCOL, 0, 32, "AES");
        this.counter = 0;
        updateHash(prologue);
    }

    void updateHash(byte[] data) {
        hashDigest.update(hash);
        hashDigest.update(data);
        this.hash = hashDigest.digest();
    }

    byte[] cipher(byte[] text, boolean encrypt) throws IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        cipher.init(
                encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE,
                cryptoKey,
                createGcmIv(counter++)
        );
        cipher.updateAAD(hash);
        var result = cipher.doFinal(text);
        updateHash(encrypt ? result : text);
        return result;
    }

    private GCMParameterSpec createGcmIv(long counter) {
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

    byte[] finish() throws GeneralSecurityException {
        var params = HKDFParameterSpec.ofExtract()
                .addSalt(salt)
                .addIKM(FINISH_KEY)
                .thenExpand(null, 64);
        return kdf.deriveData(params);
    }

    void mixIntoKey(byte[] bytes) throws GeneralSecurityException {
        var params = HKDFParameterSpec.ofExtract()
                .addSalt(salt)
                .addIKM(new SecretKeySpec(bytes, "AES"))
                .thenExpand(null, 64);
        var expanded = kdf.deriveData(params);
        this.salt = new SecretKeySpec(expanded, 0, 32, "AES");
        this.cryptoKey = new SecretKeySpec(expanded, 32, 32, "AES");
        this.counter = 0;
    }

    @Override
    public void close() {
        this.hash = null;
        this.salt = null;
        try {
            cryptoKey.destroy();
        } catch (DestroyFailedException _) {

        }
        this.cryptoKey = null;
        this.counter = 0;
    }
}
