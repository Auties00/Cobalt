package it.auties.whatsapp.registration.gcm;

import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;

public record ECDH256KeyPair(
        byte[] publicKey,
        byte[] privateKey
) {
    public static ECDH256KeyPair random() {
        try {
            var keyPairGenerator = KeyPairGenerator.getInstance("EC");
            var ecSpec = new ECGenParameterSpec("secp256r1");
            keyPairGenerator.initialize(ecSpec, new SecureRandom());
            var keyPair = keyPairGenerator.generateKeyPair();
            var publicKey = (ECPublicKey) keyPair.getPublic();
            var affineX = publicKey.getW().getAffineX().toByteArray();
            var affineY = publicKey.getW().getAffineY().toByteArray();
            var rawPublicKey = new byte[affineX.length + affineY.length];
            rawPublicKey[0] = 0x04;
            System.arraycopy(affineX, 1, rawPublicKey, 1, affineX.length - 1);
            System.arraycopy(affineY, 0, rawPublicKey, affineX.length, affineY.length);
            var privateKey = (ECPrivateKey) keyPair.getPrivate();
            var rawPrivateKey = privateKey.getS().toByteArray();
            return new ECDH256KeyPair(rawPublicKey, rawPrivateKey);
        }catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException("Missing ECDH256 implementation", exception);
        }catch (GeneralSecurityException exception) {
            throw new RuntimeException("An error occurred while generating the keypair", exception);
        }
    }
}
