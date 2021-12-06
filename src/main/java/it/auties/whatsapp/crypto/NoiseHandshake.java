package it.auties.whatsapp.crypto;

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
        var expanded = extractAndExpandWithHash(new byte[0]);
        keys.initializeKeys(expanded.cut(32), expanded.slice(32));
    }

    public void mixIntoKey(byte @NonNull [] bytes)  {
        this.counter = 0;
        var expanded = extractAndExpandWithHash(bytes);
        this.salt = expanded.cut(32);
        this.cryptoKey = expanded.slice(32);
    }

    private BinaryArray extractAndExpandWithHash(byte @NonNull [] key) {
        var extracted = Hkdf.extract(salt.data(), key);
        return of(Hkdf.expand(extracted, null, 64));
    }
}