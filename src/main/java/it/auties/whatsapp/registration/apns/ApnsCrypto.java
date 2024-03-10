package it.auties.whatsapp.registration.apns;

import it.auties.whatsapp.crypto.DerOutputStream;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class ApnsCrypto {
    private static final PrivateKey FAIRPLAY_PRIVATE_KEY;
    private static final byte[] FAIRPLAY_PRIVATE_KEY_MODULUS = {0, -73, 4, -86, -53, 60, -128, 90, 110, 26, 107, -5, -124, -81, -71, 1, -25, 108, 93, 44, -78, -92, 72, 67, -82, -1, -43, -76, -33, 2, -75, 110, 80, 94, -87, -10, -57, 92, -65, -67, 13, -6, -65, 50, 23, 23, -84, 14, 106, 83, -104, -88, -83, 44, -3, 58, -77, 50, -115, 0, -19, 100, -28, -107, 100, -9, -13, -11, 122, 60, -12, -16, 19, 30, -11, -32, 27, -30, 59, -128, -123, 85, -99, 124, 2, 50, 104, -98, -20, 24, 88, -34, 72, -67, 53, -26, 96, 47, -81, 20, -59, 112, -29, 53, -48, 95, -22, 74, 80, 17, 6, 39, 28, -60, 120, -49, 94, -37, -106, 86, -67, 31, 44, 106, -31, 51, -17, -64, 49};
    private static final byte[] FAIRPLAY_PRIVATE_KEY_PUBLIC_EXPONENT = {1, 0, 1};
    private static final byte[] FAIRPLAY_PRIVATE_KEY_PRIVATE_EXPONENT = {8, 101, -73, 108, 113, -49, 53, -42, -3, 113, 92, -19, -2, -98, 15, 127, 77, -46, -116, -99, 121, -70, 51, 24, -47, 118, 61, -63, 73, -65, -121, 91, 58, -84, -77, -68, -5, -3, 116, 48, 51, 4, 24, -55, 68, 117, -55, -121, -119, 100, 100, -64, -27, 98, -115, 17, -15, -52, -44, 113, 16, 3, 8, -13, -80, 43, -70, -121, -31, -115, -41, 91, 53, 82, -19, -22, -102, 86, -38, 114, -62, 43, -73, 4, 13, 2, 69, 106, 20, -42, -12, 100, 5, -50, -107, 81, 65, 117, -33, -58, -66, 28, -77, 85, -9, -110, -21, 101, 7, -85, -104, -39, 81, -91, -73, 20, 5, 98, -56, -7, 118, 59, -5, 15, -32, 59, -40, 13};
    private static final byte[] FAIRPLAY_PRIVATE_KEY_PRIME_P = {0, -39, -113, 84, 70, 119, 22, -34, 58, -114, 23, -49, 23, -64, -22, 126, 54, 26, -106, 90, -67, 57, 76, -35, 12, -58, 22, -128, 73, -128, 42, -86, 86, -19, 73, 64, -20, -24, -96, 3, 17, 79, -29, 79, 126, 10, 4, -13, -21, -84, 33, 85, 65, -32, -118, 52, 108, 37, 19, 88, -70, 100, 19, 43, 83};
    private static final byte[] FAIRPLAY_PRIVATE_KEY_PRIME_Q = {0, -41, 90, -13, 34, -111, -120, -124, 89, 15, -76, -41, 63, -33, 119, -17, 4, 88, 48, 81, -69, 35, 78, -51, 85, -28, 110, -77, -26, 92, -65, 94, 113, -67, 16, -41, -53, 110, 88, 4, -95, -78, -26, 74, 108, -9, -118, -69, -63, 69, 96, 3, -117, 5, 49, -25, 54, 58, 20, -33, -61, -102, -40, -71, -21};
    private static final byte[] SHA1_WITH_RSA_ALGORITHM_DATA = {48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 11, 5, 0};
    private static final byte[] FAIRPLAY_PRIVATE_KEY_PRIME_EXPONENT_P = {118, 36, -100, 106, 75, -97, 114, 124, -81, -49, 4, 25, -19, 28, 41, -1, -83, 126, 122, -74, 9, 24, -47, 109, 111, 96, -90, -73, -61, 78, -24, 3, -98, -123, -54, 41, 28, -58, 80, 4, 37, -78, -43, -25, 38, -1, -69, -119, -2, -122, 119, 106, -9, -55, 117, 96, 72, -35, -15, -81, -2, 74, 94, -101};
    private static final byte[] FAIRPLAY_PRIVATE_KEY_PRIME_EXPONENT_Q = {24, -59, -14, -96, 48, 99, -90, -19, -29, -37, -90, -61, 71, 62, -79, -75, 43, 59, -21, -70, -2, 85, -53, 83, 45, 34, -6, -8, -18, 4, 105, -91, -27, -36, -15, 38, 10, -68, 127, 83, -26, -109, -115, 78, 57, -81, -80, -25, -117, -58, 126, -63, -40, 72, 36, 83, -35, -100, -105, 29, 22, 76, 6, 31};
    private static final byte[] FAIRPLAY_PRIVATE_KEY_CRT_EXPONENT = {61, 15, -80, 86, -92, 99, 115, -120, 119, 31, 11, -68, 35, -87, 101, -109, -36, 33, -92, -81, 78, -17, 65, 75, -93, 81, 76, 85, -42, -78, -76, 73, 76, -54, -84, -48, -37, -3, 57, 124, -58, -5, 23, -84, -102, 90, 27, -66, 67, 97, -122, 94, -9, 101, 81, 24, -128, -34, -42, 52, 10, -57, -52, -45};
    private static final int BLOCK_SIZE = 64;

    static {
       try {
           var keySpec = new RSAPrivateCrtKeySpec(
                   new BigInteger(FAIRPLAY_PRIVATE_KEY_MODULUS),
                   new BigInteger(FAIRPLAY_PRIVATE_KEY_PUBLIC_EXPONENT),
                   new BigInteger(FAIRPLAY_PRIVATE_KEY_PRIVATE_EXPONENT),
                   new BigInteger(FAIRPLAY_PRIVATE_KEY_PRIME_P),
                   new BigInteger(FAIRPLAY_PRIVATE_KEY_PRIME_Q),
                   new BigInteger(FAIRPLAY_PRIVATE_KEY_PRIME_EXPONENT_P),
                   new BigInteger(FAIRPLAY_PRIVATE_KEY_PRIME_EXPONENT_Q),
                   new BigInteger(FAIRPLAY_PRIVATE_KEY_CRT_EXPONENT)
           );
           var keyFactory = KeyFactory.getInstance("RSA");
           FAIRPLAY_PRIVATE_KEY = keyFactory.generatePrivate(keySpec);
       }catch (GeneralSecurityException exception) {
           throw new RuntimeException("Cannot decode fairplay private key", exception);
       }
    }

    public static byte[] generateCSR(KeyPair keyPair) {
        var outputStream = new ByteArrayOutputStream();
        try(var printStream = new PrintStream(outputStream)) {
            var subject = new X500Principal("C=US,ST=CA,L=Cupertino,O=Apple Inc.,OU=iPhone,CN=" + UUID.randomUUID());
            var certReqInfo = createCertificationRequestInfo(subject.getEncoded(), keyPair.getPublic());
            var certReqInfoSignature = getCertificateSignature(keyPair, certReqInfo);
            var data = createCertificationRequestValue(certReqInfo, certReqInfoSignature);
            printStream.println("-----BEGIN CERTIFICATE REQUEST-----");
            printStream.println(padContent(data));
            printStream.println("-----END CERTIFICATE REQUEST-----");
            return outputStream.toString().getBytes();
        } catch (Throwable throwable) {
            throw new RuntimeException("Cannot generate csr", throwable);
        }
    }

    public static String padContent(byte[] data) {
        var base64 = Base64.getEncoder().encodeToString(data);
        var ret = new StringBuilder();
        for (var start = 0; start < base64.length(); start += BLOCK_SIZE) {
            if(start != 0) {
                ret.append("\n");
            }
            ret.append(base64, start, Math.min(base64.length(), start + BLOCK_SIZE));
        }
        return ret.toString();
    }

    private static byte[] getCertificateSignature(KeyPair keyPair, byte[] certReqInfo) throws GeneralSecurityException {
        var signature = Signature.getInstance("SHA256WithRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(certReqInfo);
        return signature.sign();
    }

    private static byte[] createCertificationRequestInfo(byte[] x500Name, PublicKey publicKey) throws IOException {
        try (
                var requestDataOutputStream = new DerOutputStream();
                var requestOutputStream = new DerOutputStream()
        ) {
            requestDataOutputStream.putInteger(BigInteger.ZERO);
            requestDataOutputStream.write(x500Name);
            requestDataOutputStream.write(publicKey.getEncoded());
            requestDataOutputStream.writeBytes(new byte[]{-96, 0});
            requestOutputStream.writeSequence(requestDataOutputStream.toByteArray());
            return requestOutputStream.toByteArray();
        }
    }

    private static byte[] createCertificationRequestValue(byte[] certReqInfo, byte[] signature) throws IOException {
        try(
                var dataOutputStream = new DerOutputStream();
                var outputStream = new DerOutputStream()
        ) {
            dataOutputStream.write(certReqInfo);
            dataOutputStream.write(SHA1_WITH_RSA_ALGORITHM_DATA);
            dataOutputStream.putBitString(signature);
            outputStream.writeSequence(dataOutputStream.toByteArray());
            return outputStream.toByteArray();
        }
    }

    static byte[] getActivationSignature(byte[] activationInfoXml) {
        try {
            var signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(FAIRPLAY_PRIVATE_KEY);
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

    static byte[] getCertificateBytes(byte[] certificateBytes) {
        try (var byteArrayInputStream = new ByteArrayInputStream(certificateBytes)) {
            var certificateFactory = CertificateFactory.getInstance("X.509");
            var certificate = (X509Certificate) certificateFactory.generateCertificate(byteArrayInputStream);
            return certificate.getEncoded();
        } catch (Throwable exception) {
            throw new RuntimeException("Cannot get certificate bytes", exception);
        }
    }
}
