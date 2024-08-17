package it.auties.whatsapp.registration.cloudVerification.gcm;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

record ECDH256KeyPair(
        byte[] publicKey,
        byte[] privateKey,
        ECPublicKey jcaPublicKey,
        ECPrivateKey jcaPrivateKey
) {
    private static final int KEY_PART_LENGTH = 32;

    static ECDH256KeyPair random() {
        try {
            var keyPairGenerator = KeyPairGenerator.getInstance("EC");
            var ecSpec = new ECGenParameterSpec("secp256r1");
            keyPairGenerator.initialize(ecSpec, new SecureRandom());
            var keyPair = keyPairGenerator.generateKeyPair();
            var publicKey = (ECPublicKey) keyPair.getPublic();
            var affineX = publicKey.getW().getAffineX().toByteArray();
            var affineY = publicKey.getW().getAffineY().toByteArray();
            var rawPublicKey = new byte[KEY_PART_LENGTH * 2 + 1];
            rawPublicKey[0] = 0x04;
            System.arraycopy(affineX, affineX.length == KEY_PART_LENGTH + 1 ? 1 : 0, rawPublicKey, 1, KEY_PART_LENGTH);
            System.arraycopy(affineY, affineY.length == KEY_PART_LENGTH + 1 ? 1 : 0, rawPublicKey, KEY_PART_LENGTH + 1, KEY_PART_LENGTH);
            var privateKey = (ECPrivateKey) keyPair.getPrivate();
            var encodedPrivateKey = privateKey.getS().toByteArray();
            var rawPrivateKey = encodedPrivateKey.length == KEY_PART_LENGTH ? encodedPrivateKey : Arrays.copyOfRange(encodedPrivateKey, encodedPrivateKey.length - KEY_PART_LENGTH, encodedPrivateKey.length);
            return new ECDH256KeyPair(rawPublicKey, rawPrivateKey, publicKey, privateKey);
        }catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException("Missing ECDH256 implementation", exception);
        }catch (Throwable exception) {
            throw new RuntimeException("An error occurred while generating the keypair", exception);
        }
    }
}
