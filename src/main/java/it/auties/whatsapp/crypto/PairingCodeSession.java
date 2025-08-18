package it.auties.whatsapp.crypto;

import it.auties.whatsapp.api.WhatsappVerificationHandler;
import it.auties.whatsapp.util.Bytes;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;

public final class PairingCodeSession {
    private final String pairingKey;
    public PairingCodeSession() {
        this.pairingKey = Bytes.randomHex(5);
    }

    public void accept(WhatsappVerificationHandler.Web.PairingCode handler) {
        Objects.requireNonNull(handler, "handler cannot be null");
        handler.handle(pairingKey);
    }

    public byte[] encrypt(byte[] companionPublicKey) {
        Objects.requireNonNull(companionPublicKey, "companionPublicKey cannot be null");
        try {
            var salt = Bytes.random(32);
            var randomIv = Bytes.random(16);
            var cipher = Cipher.getInstance("AES/CTR/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    getKey(salt),
                    new IvParameterSpec(randomIv)
            );
            cipher.update(companionPublicKey);
            var result = new byte[salt.length + randomIv.length + cipher.getOutputSize(companionPublicKey.length)];
            System.arraycopy(salt, 0, result, 0, salt.length);
            System.arraycopy(randomIv, 0, result, salt.length, randomIv.length);
            cipher.doFinal(result, salt.length + randomIv.length);
            return result;
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot cipher link code pairing key", exception);
        }
    }

    public byte[] decrypt(byte[] primaryEphemeralPublicKeyWrapped) {
        Objects.requireNonNull(primaryEphemeralPublicKeyWrapped, "primaryEphemeralPublicKeyWrapped cannot be null");
        try {
            // There is no override in PBEKeySpec that takes an offset and a length, so we have to copy the key
            var secretKey = getKey(Arrays.copyOfRange(primaryEphemeralPublicKeyWrapped, 0, 32));
            var cipher = Cipher.getInstance("AES/CTR/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    new IvParameterSpec(primaryEphemeralPublicKeyWrapped, 32, 16)
            );
            return cipher.doFinal(primaryEphemeralPublicKeyWrapped, 48, 32);
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot decipher link code pairing key", exception);
        }
    }

    private SecretKey getKey(byte[] salt) {
        Objects.requireNonNull(salt, "salt cannot be null");
        try {
            var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            var spec = new PBEKeySpec(pairingKey.toCharArray(), salt, 2 << 16, 256);
            var secret = factory.generateSecret(spec);
            return new SecretKeySpec(secret.getEncoded(), "AES");
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot compute pairing key", exception);
        }
    }
}
