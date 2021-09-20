package it.auties.whatsapp4j.beta.utils;

import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.utils.CypherUtils;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

import javax.crypto.KeyAgreement;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HexFormat;
import java.util.stream.IntStream;

/**
 * This utility class provides helper functionality to easily encrypt and decrypt data
 * This class should only be used for WhatsappWeb's WebSocket binary operations
 */
@UtilityClass
public class MultiDeviceCypher {
    private final String HANDSHAKE_PROTOCOL = "Noise_XX_25519_AESGCM_SHA256\0\0\0\0";
    private final byte[] HANDSHAKE_PROLOGUE = new byte[]{87, 65, 5, 2};
    private final String ED_CURVE = "Ed25519";
    private final String ED_DSA = "EdDSA";

    public @NonNull String handshakeProtocol(){
        return HANDSHAKE_PROTOCOL;
    }

    public byte @NonNull [] handshakePrologue(){
        return HANDSHAKE_PROLOGUE;
    }

    public @NonNull BinaryArray encryptMessage(byte @NonNull [] message) {
        return BinaryArray.forArray(handshakePrologue())
                .append(BinaryArray.forInt(message.length, 3))
                .append(message)
                .fill(64);
    }

    public @NonNull SignedKeyPair randomSignedPreKey() {
        try {
            var keyPair = CypherUtils.randomKeyPair();
            var parsedPublicKey = CypherUtils.parseKey(keyPair.getPublic());
            var message = new byte[parsedPublicKey.length + 1];
            IntStream.range(0, message.length).forEach(index -> message[index] = index == 0 ? 5 : parsedPublicKey[index - 1]);
            var sign = Signature.getInstance(ED_CURVE);
            var keyFactory = KeyFactory.getInstance(ED_DSA);
            var parsedPrivateKey = CypherUtils.parseKey(keyPair.getPrivate());
            var privateKeyInfo = new PrivateKeyInfo(new AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), new DEROctetString(parsedPrivateKey));
            var privateKeySpec = new PKCS8EncodedKeySpec(privateKeyInfo.getEncoded());
            var privateKey = keyFactory.generatePrivate(privateKeySpec);
            sign.initSign(privateKey);
            sign.update(message);
            return new SignedKeyPair(parsedPublicKey, parsedPrivateKey, sign.sign());
        }catch (Exception ex){
            throw new RuntimeException("Cannot generate random signed key pair", ex);
        }
    }
}