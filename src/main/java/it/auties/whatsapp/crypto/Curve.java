package it.auties.whatsapp.crypto;

import it.auties.whatsapp.binary.BinaryArray;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.whispersystems.curve25519.Curve25519;

import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

// TODO: Migrate off Curve25519 library
@UtilityClass
public class Curve {
    private final String CURVE_25519 = "X25519";
    private final String HMAC_SHA256 = "HmacSHA256";

    @SneakyThrows
    public BinaryArray calculateSharedSecret(byte @NonNull [] publicKey, byte @NonNull [] privateKey) {
        var keyAgreement = KeyAgreement.getInstance(CURVE_25519);
        keyAgreement.init(toPKCS8Encoded(privateKey));
        keyAgreement.doPhase(toX509Encoded(publicKey), true);
        return BinaryArray.of(keyAgreement.generateSecret());
    }

    @SneakyThrows
    public boolean verifySignature(byte @NonNull [] publicKey, byte @NonNull [] message, byte @NonNull [] signature) {
        return Curve25519.getInstance(Curve25519.BEST)
                .verifySignature(publicKey, message, signature);
    }

    @SneakyThrows
    public BinaryArray verifyHmac(@NonNull BinaryArray plain, @NonNull BinaryArray key) {
        var localMac = Mac.getInstance(HMAC_SHA256);
        localMac.init(new SecretKeySpec(key.data(), HMAC_SHA256));
        return BinaryArray.of(localMac.doFinal(plain.data()));
    }

    @SneakyThrows
    public byte[] calculateSignature(byte @NonNull [] privateKey, byte @NonNull [] message) {
        return Curve25519.getInstance(Curve25519.BEST)
                .calculateSignature(privateKey, message);
    }

    @SneakyThrows
    private PublicKey toX509Encoded(byte @NonNull [] rawPublicKey) {
        var keyFactory = KeyFactory.getInstance(CURVE_25519);
        var publicKeyInfo = new SubjectPublicKeyInfo(new AlgorithmIdentifier(EdECObjectIdentifiers.id_X25519), rawPublicKey);
        var publicKeySpec = new X509EncodedKeySpec(publicKeyInfo.getEncoded());
        return keyFactory.generatePublic(publicKeySpec);
    }

    @SneakyThrows
    private PrivateKey toPKCS8Encoded(byte @NonNull [] rawPrivateKey) {
        var keyFactory = KeyFactory.getInstance(CURVE_25519);
        var privateKeyInfo = new PrivateKeyInfo(new AlgorithmIdentifier(EdECObjectIdentifiers.id_X25519), new DEROctetString(rawPrivateKey));
        var privateKey = new PKCS8EncodedKeySpec(privateKeyInfo.getEncoded());
        return keyFactory.generatePrivate(privateKey);
    }
}
