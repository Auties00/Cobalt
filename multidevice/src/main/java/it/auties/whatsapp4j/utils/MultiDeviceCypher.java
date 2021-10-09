package it.auties.whatsapp4j.utils;

import it.auties.whatsapp4j.common.binary.BinaryBuffer;
import it.auties.whatsapp4j.common.binary.BinaryArray;
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

    public @NonNull String handshakeProtocol(){
        return HANDSHAKE_PROTOCOL;
    }

    public byte @NonNull [] handshakePrologue(){
        return HANDSHAKE_PROLOGUE;
    }

    @SneakyThrows
    public @NonNull BinaryArray encryptMessage(byte @NonNull [] message, BinaryArray writeKey, long count, boolean prologue) {
        if(writeKey != null){
            var cipher = aesGmc(writeKey.data(), new byte[0], count, true);
            message = aesGmcEncrypt(cipher, message);
        }

        return new BinaryBuffer()
                .writeBytes(prologue ? handshakePrologue() : new byte[0])
                .writeUInt8(message.length >> 16)
                .writeUInt16(65535 & message.length)
                .writeBytes(message)
                .readWrittenBytesToArray();
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
    private byte[] calculateSignature(PrivateKey key, byte[] data) {
        var rawPrivateKey = ((XECPrivateKey) key).getScalar().orElseThrow();

        var keyFactory = KeyFactory.getInstance(ED_CURVE);
        var privateKeyInfo = new PrivateKeyInfo(new AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), new DEROctetString(rawPrivateKey));
        var privateKeySpec = new PKCS8EncodedKeySpec(privateKeyInfo.getEncoded());
        var privateKey = keyFactory.generatePrivate(privateKeySpec);

        var signer = Signature.getInstance(ED_CURVE);
        signer.initSign(privateKey);
        signer.update(data);
        return signer.sign();
    }
}