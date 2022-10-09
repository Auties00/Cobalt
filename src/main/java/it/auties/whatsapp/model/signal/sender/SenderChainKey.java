package it.auties.whatsapp.model.signal.sender;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.crypto.Hmac;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.BYTES;
import static it.auties.protobuf.base.ProtobufType.UINT32;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class SenderChainKey implements ProtobufMessage {
    private static final byte[] MESSAGE_KEY_SEED = {0x01};
    private static final byte[] CHAIN_KEY_SEED = {0x02};

    @ProtobufProperty(index = 1, type = UINT32)
    private Integer iteration;

    @ProtobufProperty(index = 2, type = BYTES)
    private byte[] seed;

    public SenderMessageKey toMessageKey() {
        var hmac = Hmac.calculateSha256(MESSAGE_KEY_SEED, seed);
        return new SenderMessageKey(iteration, hmac);
    }

    public SenderChainKey next() {
        var hmac = Hmac.calculateSha256(CHAIN_KEY_SEED, seed);
        return new SenderChainKey(iteration + 1, hmac);
    }
}
