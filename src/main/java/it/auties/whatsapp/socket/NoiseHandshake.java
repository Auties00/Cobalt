package it.auties.whatsapp.socket;

import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.utils.CypherUtils;
import it.auties.whatsapp.utils.MultiDeviceCypher;
import lombok.NonNull;
import lombok.SneakyThrows;

import static it.auties.whatsapp.binary.BinaryArray.empty;
import static it.auties.whatsapp.binary.BinaryArray.of;

public class NoiseHandshake {
    private WhatsappKeys keys;
    private BinaryArray hash;
    private BinaryArray salt;
    private BinaryArray cryptoKey;
    private long counter;

    public void start(WhatsappKeys keys){
        var encodedProtocol = of(MultiDeviceCypher.handshakeProtocol());
        this.hash = encodedProtocol;
        this.salt = encodedProtocol;
        this.cryptoKey = encodedProtocol;
        this.keys = keys;
        updateHash(MultiDeviceCypher.handshakePrologue());
    }

    @SneakyThrows
    public void updateHash(byte @NonNull [] data) {
        var input = hash.append(of(data));
        this.hash = CypherUtils.sha256(input);
    }

    @SneakyThrows
    public byte[] cypher(byte @NonNull [] bytes, boolean encrypt) {
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
        var expanded = extractAndExpandWithHash(empty());
        keys.initializeKeys(expanded.cut(32), expanded.slice(32));
    }

    public void mixIntoKey(byte @NonNull [] bytes)  {
        this.counter = 0;
        var expanded = extractAndExpandWithHash(of(bytes));
        this.salt = expanded.cut(32);
        this.cryptoKey = expanded.slice(32);
    }

    private BinaryArray extractAndExpandWithHash(@NonNull BinaryArray key) {
        var extracted = CypherUtils.hkdfExtract(key, salt.data());
        return CypherUtils.hkdfExpand(extracted, null, 64);
    }
}