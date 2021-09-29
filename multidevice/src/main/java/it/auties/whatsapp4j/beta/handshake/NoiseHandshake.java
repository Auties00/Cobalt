package it.auties.whatsapp4j.beta.handshake;

import it.auties.whatsapp4j.beta.manager.MultiDeviceKeysManager;
import it.auties.whatsapp4j.beta.utils.MultiDeviceCypher;
import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.utils.CypherUtils;
import lombok.SneakyThrows;

import static it.auties.whatsapp4j.common.binary.BinaryArray.forArray;

public class NoiseHandshake {
    private MultiDeviceKeysManager keys;
    private BinaryArray hash;
    private BinaryArray salt;
    private BinaryArray cryptoKey;
    private long counter;

    public NoiseHandshake() {
        var encodedProtocol = BinaryArray.forString(MultiDeviceCypher.handshakeProtocol());
        this.hash = encodedProtocol;
        this.salt = encodedProtocol;
        this.cryptoKey = encodedProtocol;
        updateHash(MultiDeviceCypher.handshakePrologue());
    }

    public void start(MultiDeviceKeysManager keys){
        this.keys = keys;
    }

    @SneakyThrows
    public void updateHash(byte[] data) {
        var input = hash.append(forArray(data));
        this.hash = CypherUtils.sha256(input);
    }

    @SneakyThrows
    public byte[] cypher(byte[] bytes, boolean encrypt) {
        var cipher = MultiDeviceCypher.aesGmc(cryptoKey.data(), hash.data(), counter++, encrypt);
        var result = MultiDeviceCypher.aesGmcEncrypt(cipher, bytes);
        if(!encrypt){
            updateHash(bytes);
            return result;
        }

        updateHash(result);
        return result;
    }

    public void finish()  {
        var expanded = CypherUtils.hkdfExtractAndExpand(salt, 64);
        keys.initializeKeys(expanded.cut(32), expanded.slice(32));
    }

    public void mixIntoKey(byte[] bytes)  {
        this.counter = 0;
        var key = CypherUtils.hkdfExtract(forArray(bytes), salt.data());
        var expanded = CypherUtils.hkdfExpand(key, null, 64);
        this.salt = expanded.cut(32);
        this.cryptoKey = expanded.slice(32);
    }
}