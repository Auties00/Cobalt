package it.auties.whatsapp.model.signal.sender;

import it.auties.whatsapp.crypto.Hmac;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record SenderChainKey(int iteration, byte[] seed) {
    private static final byte[] MESSAGE_KEY_SEED = {0x01};
    private static final byte[] CHAIN_KEY_SEED = {0x02};

    public SenderMessageKey toMessageKey() {
        var hmac = Hmac.calculateSha256(MESSAGE_KEY_SEED, seed);
        return new SenderMessageKey(iteration, hmac);
    }

    public SenderChainKey next() {
        var hmac = Hmac.calculateSha256(CHAIN_KEY_SEED, seed);
        return new SenderChainKey(iteration + 1, hmac);
    }
}