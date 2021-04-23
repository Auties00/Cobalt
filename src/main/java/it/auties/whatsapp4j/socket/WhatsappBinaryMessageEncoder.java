package it.auties.whatsapp4j.socket;

import it.auties.whatsapp4j.binary.BinaryArray;
import it.auties.whatsapp4j.binary.BinaryEncoder;
import it.auties.whatsapp4j.binary.BinaryMetric;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.request.model.BinaryRequest;
import it.auties.whatsapp4j.utils.CypherUtils;
import jakarta.validation.constraints.NotNull;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder.Binary;

import java.nio.ByteBuffer;
import java.util.Objects;

public class WhatsappBinaryMessageEncoder implements Binary<BinaryRequest<?>> {
    private static final WhatsappKeysManager KEYS_MANAGER = WhatsappKeysManager.singletonInstance();
    private static final BinaryEncoder ENCODER = new BinaryEncoder();

    @Override
    public @NotNull ByteBuffer encode(@NotNull BinaryRequest<?> request) throws EncodeException {
        var messageTag = BinaryArray.forString("%s,".formatted(request.tag()));
        var encodedMessage = ENCODER.encodeMessage(request.buildBody());
        var encrypted = CypherUtils.aesEncrypt(encodedMessage, Objects.requireNonNull(KEYS_MANAGER.encKey()));
        var hmacSign = CypherUtils.hmacSha256(encrypted, Objects.requireNonNull(KEYS_MANAGER.macKey()));
        return messageTag.merged(BinaryMetric.toArray(request.tags())
                .merged(BinaryArray.singleton(request.flag().data())))
                .merged(hmacSign)
                .merged(encrypted)
                .toBuffer();
    }
}
