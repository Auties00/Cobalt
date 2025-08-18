package it.auties.whatsapp.model.signal.sender;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.crypto.Hmac;

@ProtobufMessage
public record SenderChainKey(
        @ProtobufProperty(index = 1, type = ProtobufType.INT32)
        int iteration,
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] seed
) {
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