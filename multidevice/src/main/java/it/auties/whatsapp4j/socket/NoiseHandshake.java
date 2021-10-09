package it.auties.whatsapp4j.socket;

import it.auties.whatsapp4j.manager.MultiDeviceKeysManager;
import it.auties.whatsapp4j.common.binary.BinaryArray;
import lombok.NonNull;
import lombok.SneakyThrows;

import static it.auties.whatsapp4j.utils.MultiDeviceCypher.*;
import static it.auties.whatsapp4j.common.binary.BinaryArray.*;
import static it.auties.whatsapp4j.common.utils.CypherUtils.*;

public class NoiseHandshake {
    private MultiDeviceKeysManager keys;
    private BinaryArray hash;
    private BinaryArray salt;
    private BinaryArray cryptoKey;
    private long counter;

    public void start(MultiDeviceKeysManager keys){
        var encodedProtocol = forString(handshakeProtocol());
        this.hash = encodedProtocol;
        this.salt = encodedProtocol;
        this.cryptoKey = encodedProtocol;
        this.keys = keys;
        updateHash(handshakePrologue());
    }

    @SneakyThrows
    public void updateHash(byte @NonNull [] data) {
        var input = hash.append(forArray(data));
        this.hash = sha256(input);
    }

    @SneakyThrows
    public byte[] cypher(byte @NonNull [] bytes, boolean encrypt) {
        var cipher = aesGmc(cryptoKey.data(), hash.data(), counter++, encrypt);
        var result = aesGmcEncrypt(cipher, bytes);
        if(!encrypt){
            updateHash(bytes);
            return result;
        }

        updateHash(result);
        return result;
    }

    public void finish()  {
        var expanded = extractAndExpandWithHash(empty());
        keys.initializeKeys(expanded.cut(32), expanded.slice(32));
    }

    public void mixIntoKey(byte @NonNull [] bytes)  {
        this.counter = 0;
        var expanded = extractAndExpandWithHash(forArray(bytes));
        this.salt = expanded.cut(32);
        this.cryptoKey = expanded.slice(32);
    }

    private BinaryArray extractAndExpandWithHash(@NonNull BinaryArray key) {
        var extracted = hkdfExtract(key, salt.data());
        return hkdfExpand(extracted, null, 64);
    }
}