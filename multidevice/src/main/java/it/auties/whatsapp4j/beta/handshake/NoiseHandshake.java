package it.auties.whatsapp4j.beta.handshake;

import it.auties.whatsapp4j.beta.utils.MultiDeviceCypher;
import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.utils.CypherUtils;
import lombok.SneakyThrows;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static it.auties.whatsapp4j.common.binary.BinaryArray.forArray;

public class NoiseHandshake {
    private BinaryArray hash;
    private BinaryArray salt;
    private BinaryArray cryptoKey;
    private long counter;

    public void start() {
        var encodedProtocol = BinaryArray.forString(MultiDeviceCypher.handshakeProtocol());
        this.hash = encodedProtocol;
        this.salt = encodedProtocol;
        this.cryptoKey = encodedProtocol;
        updateHash(MultiDeviceCypher.handshakePrologue());
    }

    @SneakyThrows
    public void updateHash(byte[] data) {
        var input = hash.append(forArray(data));
        this.hash = CypherUtils.sha256(input);
    }

    @SneakyThrows
    public BinaryArray encrypt(byte[] bytes) {
        var cipher = createAESGCMCipher(true);
        var outputLength = cipher.getOutputSize(bytes.length);
        var output = new byte[outputLength];
        var outputOffset = cipher.processBytes(bytes, 0, bytes.length, output, 0);
        cipher.doFinal(output, outputOffset);
        var result = forArray(output);
        updateHash(result.data());
        return result;
    }

    @SneakyThrows
    public BinaryArray decrypt(byte[] bytes) {
        var cipher = createAESGCMCipher(false);
        var outputLength = cipher.getOutputSize(bytes.length);
        var output = new byte[outputLength];
        var outputOffset = cipher.processBytes(bytes, 0, bytes.length, output, 0);
        cipher.doFinal(output, outputOffset);
        var result = forArray(output);
        updateHash(result.data());
        System.out.printf("Iv: %s%nCrypto key: %s%nHash: %s%nData: %s%nResult: %s%n", counter - 1, cryptoKey.toHex(), hash.toHex(), forArray(bytes).toHex(), result.toHex());
        return result;
    }

    public void finish()  {
        var expanded = CypherUtils.hkdfExtractAndExpand(salt, 64);
        var writeKey = expanded.cut(32).data();
        var readKey = expanded.slice(32).data();
        System.err.println("Write key: " + Arrays.toString(writeKey));
        System.err.println("Read key: " + Arrays.toString(readKey));
    }

    public void mixIntoKey(byte[] bytes)  {
        this.counter = 0;
        var key = CypherUtils.hkdfExtract(forArray(bytes), salt.data());
        var expanded = CypherUtils.hkdfExpand(key, null, 64);
        this.salt = expanded.cut(32);
        this.cryptoKey = expanded.slice(32);
    }

    private GCMBlockCipher createAESGCMCipher(boolean forEncryption) {
        var secretKey = new KeyParameter(cryptoKey.data());
        var iv = createIv();

        var cipher = new AESEngine();
        cipher.init(forEncryption, secretKey);

        var gcm = new GCMBlockCipher(cipher);
        var params = new AEADParameters(secretKey, 128, iv, hash.data());
        gcm.init(forEncryption, params);
        return gcm;
    }

    private byte[] createIv() {
        return ByteBuffer.allocate(12)
                .putLong(4, counter++)
                .array();
    }
}