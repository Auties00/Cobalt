package it.auties.whatsapp.registration.apns;

import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

class ApnsCrypto {
    private static final byte[] FAIRPLAY_PRIVATE_KEY = Base64.getDecoder().decode("MIICWwIBAAKBgQC3BKrLPIBabhpr+4SvuQHnbF0ssqRIQ67/1bTfArVuUF6p9sdcv70N+r8yFxesDmpTmKitLP06szKNAO1k5JVk9/P1ejz08BMe9eAb4juAhVWdfAIyaJ7sGFjeSL015mAvrxTFcOM10F/qSlARBiccxHjPXtuWVr0fLGrhM+/AMQIDAQABAoGACGW3bHHPNdb9cVzt/p4Pf03SjJ15ujMY0XY9wUm/h1s6rLO8+/10MDMEGMlEdcmHiWRkwOVijRHxzNRxEAMI87AruofhjddbNVLt6ppW2nLCK7cEDQJFahTW9GQFzpVRQXXfxr4cs1X3kutlB6uY2VGltxQFYsj5djv7D+A72A0CQQDZj1RGdxbeOo4XzxfA6n42GpZavTlM3QzGFoBJgCqqVu1JQOzooAMRT+NPfgoE8+usIVVB4Io0bCUTWLpkEytTAkEA11rzIpGIhFkPtNc/33fvBFgwUbsjTs1V5G6z5ly/XnG9ENfLblgEobLmSmz3irvBRWADiwUx5zY6FN/Dmti56wJAdiScakufcnyvzwQZ7Rwp/61+erYJGNFtb2Cmt8NO6AOehcopHMZQBCWy1ecm/7uJ/oZ3avfJdWBI3fGv/kpemwJAGMXyoDBjpu3j26bDRz6xtSs767r+VctTLSL6+O4EaaXl3PEmCrx/U+aTjU45r7Dni8Z+wdhIJFPdnJcdFkwGHwJAPQ+wVqRjc4h3Hwu8I6llk9whpK9O70FLo1FMVdaytElMyqzQ2/05fMb7F6yaWhu+Q2GGXvdlURiA3tY0CsfM0w==");


    static byte[] generateCSR(KeyPair keyPair) {
        try {
            var subject = new X500NameBuilder(BCStyle.INSTANCE)
                    .addRDN(BCStyle.C, "US")
                    .addRDN(BCStyle.ST, "CA")
                    .addRDN(BCStyle.L, "Cupertino")
                    .addRDN(BCStyle.O, "Apple Inc.")
                    .addRDN(BCStyle.OU, "iPhone")
                    .addRDN(BCStyle.CN, java.util.UUID.randomUUID().toString())
                    .build();
            var signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(keyPair.getPrivate());
            var certificateRequest = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic())
                    .build(signer);
            var stringWriter = new StringWriter();
            try (var pemWriter = new PemWriter(stringWriter)) {
                pemWriter.writeObject(new PemObject("CERTIFICATE REQUEST", certificateRequest.getEncoded()));
            }
            return stringWriter.toString()
                    .getBytes(StandardCharsets.UTF_8);
        } catch (Throwable throwable) {
            throw new RuntimeException("Cannot generate csr", throwable);
        }
    }

    static byte[] getActivationSignature(byte[] activationInfoXml) {
        try {
            var keyFactory = KeyFactory.getInstance("RSA");
            var fairPlayPrivateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(FAIRPLAY_PRIVATE_KEY));
            var signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(fairPlayPrivateKey);
            signature.update(activationInfoXml);
            return signature.sign();
        }catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot generate activation signature", exception);
        }
    }
    static byte[] createNonceSignature(KeyPair keyPair, byte[] nonce) {
        try {
            var signer = Signature.getInstance("SHA1withRSA");
            signer.initSign(keyPair.getPrivate());
            signer.update(nonce);
            var signature = signer.sign();
            var result = new byte[signature.length + 2];
            result[0] = 0x01;
            result[1] = 0x01;
            System.arraycopy(signature, 0, result, 2, signature.length);
            return result;
        }catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot generate signature for nonce", exception);
        }
    }

    static byte[] createNonce() {
        var nonceBuffer = ByteBuffer.allocate(17);
        nonceBuffer.putLong(1, System.currentTimeMillis());
        var bytes = new byte[8];
        ThreadLocalRandom.current().nextBytes(bytes);
        nonceBuffer.put(9, bytes);
        return nonceBuffer.array();
    }
}
