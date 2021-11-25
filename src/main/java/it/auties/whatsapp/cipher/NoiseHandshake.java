package it.auties.whatsapp.cipher;

import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.manager.WhatsappKeys;
import lombok.*;

import static it.auties.whatsapp.binary.BinaryArray.empty;
import static it.auties.whatsapp.binary.BinaryArray.of;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class NoiseHandshake {
    private static final BinaryArray PROTOCOL = BinaryArray.of("Noise_XX_25519_AESGCM_SHA256\0\0\0\0");
    private WhatsappKeys keys;
    private BinaryArray hash;
    private BinaryArray salt;
    private BinaryArray cryptoKey;
    private long counter;

    public void start(WhatsappKeys keys){
        this.hash = PROTOCOL;
        this.salt = PROTOCOL;
        this.cryptoKey = PROTOCOL;
        this.keys = keys;
        this.counter = 0;
        updateHash(Cipher.handshakePrologue());
    }

    @SneakyThrows
    public void updateHash(byte @NonNull [] data) {
        var input = hash.append(of(data));
        this.hash = Cipher.sha256(input);
    }

    @SneakyThrows
    public byte[] cypher(byte @NonNull [] bytes, boolean encrypt) {
        var aes = new AesGmc();
        aes.initialize(cryptoKey.data(), hash.data(), counter++, encrypt);
        var cyphered = aes.processBytes(bytes);
        if(!encrypt){
            updateHash(bytes);
            return cyphered;
        }

        updateHash(cyphered);
        return cyphered;
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
        var extracted = Cipher.hkdfExtract(key, salt.data());
        return Cipher.hkdfExpand(extracted, null, 64);
    }
}