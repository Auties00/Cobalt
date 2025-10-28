package com.github.auties00.cobalt.socket;

import com.github.auties00.cobalt.client.WhatsAppClientVerificationHandler;
import com.github.auties00.cobalt.util.SecureBytes;
import com.github.auties00.libsignal.key.SignalIdentityPublicKey;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Objects;

public final class SocketPhonePairing {
    private final Cipher cipher;
    private final SecretKeyFactory cipherKeyFactory;
    private final String pairingKey;
    public SocketPhonePairing() {
        try {
            this.cipher = Cipher.getInstance("AES/CTR/NoPadding");
            this.cipherKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            this.pairingKey = SecureBytes.randomHex(8);
        }catch (NoSuchAlgorithmException | NoSuchPaddingException exception) {
            throw new InternalError("Cannot initialize phone pairing code", exception);
        }
    }

    public void accept(WhatsAppClientVerificationHandler.Web.PairingCode handler) {
        Objects.requireNonNull(handler, "handler cannot be null");
        handler.handle(pairingKey);
    }

    public byte[] encrypt(SignalIdentityPublicKey companionPublicKey) {
        Objects.requireNonNull(companionPublicKey, "companionPublicKey cannot be null");
        try {
            var salt = SecureBytes.random(32);
            var randomIv = SecureBytes.random(16);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    getSaltedSecretKey(salt),
                    new IvParameterSpec(randomIv)
            );
            var encoded = companionPublicKey.toEncodedPoint();
            cipher.update(encoded);
            var result = new byte[salt.length + randomIv.length + cipher.getOutputSize(encoded.length)];
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
            var secretKey = getSaltedSecretKey(Arrays.copyOfRange(primaryEphemeralPublicKeyWrapped, 0, 32));
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

    private SecretKey getSaltedSecretKey(byte[] salt) throws InvalidKeySpecException {
        Objects.requireNonNull(salt, "salt cannot be null");
        var spec = new PBEKeySpec(pairingKey.toCharArray(), salt, 2 << 16, 256);
        var secret = cipherKeyFactory.generateSecret(spec);
        return new SecretKeySpec(secret.getEncoded(), "AES");
    }
}
