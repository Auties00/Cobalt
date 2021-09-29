package it.auties.whatsapp4j.beta.utils;

import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.utils.CypherUtils;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import java.nio.ByteBuffer;
import java.security.*;
import java.security.interfaces.XECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

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

    @SneakyThrows
    public @NonNull BinaryArray encryptMessage(byte @NonNull [] message, BinaryArray writeKey, long count) {
        if(writeKey != null){
            System.out.println("Using cypher...");
            var cipher = aesGmc(writeKey.data(), null, count, true);
            message = aesGmcEncrypt(cipher, message);
        }

        System.out.printf("Sending request number %s%n", count);
        return BinaryArray.empty()
                .append(count != 0 ? new byte[0] : handshakePrologue())
                .append(BinaryArray.forInt(message.length, 3))
                .append(message);
    }

    public GCMBlockCipher aesGmc(byte @NonNull [] key, byte[] data, long count, boolean forEncryption) {
        var secretKey = new KeyParameter(key);
        var iv = createIv(count);

        var cipher = new AESEngine();
        cipher.init(forEncryption, secretKey);

        var gcm = new GCMBlockCipher(cipher);
        var params = new AEADParameters(secretKey, 128, iv, data);
        gcm.init(forEncryption, params);
        return gcm;
    }

    public byte[] aesGmcEncrypt(GCMBlockCipher cipher, byte[] bytes) throws InvalidCipherTextException {
        var outputLength = cipher.getOutputSize(bytes.length);
        var output = new byte[outputLength];
        var outputOffset = cipher.processBytes(bytes, 0, bytes.length, output, 0);
        cipher.doFinal(output, outputOffset);
        return output;
    }

    private byte[] createIv(long count) {
        return ByteBuffer.allocate(12)
                .putLong(4, count)
                .array();
    }

    @SneakyThrows
    public @NonNull SignedKeyPair randomSignedPreKey(@NonNull KeyPair signedIdentityKey) {
        var keyPair = CypherUtils.randomKeyPair();
        var publicKey = CypherUtils.raw(keyPair.getPublic());
        var privateKey = CypherUtils.raw(keyPair.getPrivate());
        var signKey = calculateSignature(signedIdentityKey.getPrivate(), createSignKey(publicKey));
        return new SignedKeyPair(publicKey, privateKey, signKey);
    }

    @SneakyThrows
    private byte[] calculateSignature(PrivateKey key, byte[] data) {
        var rawPrivateKey = ((XECPrivateKey) key).getScalar().orElseThrow();

        var keyFactory = KeyFactory.getInstance("Ed25519");
        var privateKeyInfo = new PrivateKeyInfo(new AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), new DEROctetString(rawPrivateKey));
        var privateKeySpec = new PKCS8EncodedKeySpec(privateKeyInfo.getEncoded());
        var privateKey = keyFactory.generatePrivate(privateKeySpec);

        var signer = Signature.getInstance("Ed25519");
        signer.initSign(privateKey);
        signer.update(data);
        return signer.sign();
    }


    private byte @NonNull [] createSignKey(byte @NonNull [] publicKey) {
        return BinaryArray.empty()
                .append((byte) 5)
                .append(publicKey)
                .data();
    }
}