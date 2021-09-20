package it.auties.whatsapp4j.standard.serialization;

import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.common.request.AbstractBinaryRequest;
import it.auties.whatsapp4j.common.response.BinaryResponse;
import it.auties.whatsapp4j.common.serialization.IWhatsappSerializer;
import it.auties.whatsapp4j.common.utils.CypherUtils;
import it.auties.whatsapp4j.standard.binary.BinaryFlag;
import it.auties.whatsapp4j.standard.binary.BinaryMetric;
import it.auties.whatsapp4j.standard.request.BinaryRequest;
import it.auties.whatsapp4j.common.utils.Validate;
import lombok.NonNull;

import java.nio.ByteBuffer;

public record WhatsappSerializer(WhatsappKeysManager keys) implements IWhatsappSerializer {
    private static final BinaryEncoder ENCODER = new BinaryEncoder();
    private static final BinaryDecoder DECODER = new BinaryDecoder();

    @Override
    public @NonNull ByteBuffer serialize(@NonNull AbstractBinaryRequest<?> input) {
        var messageTag = BinaryArray.forString("%s,".formatted(input.tag()));
        var encodedMessage = ENCODER.encodeMessage(input.buildBody());
        var encrypted = CypherUtils.aesEncrypt(encodedMessage, keys.encKey());
        var hmacSign = CypherUtils.hmacSha256(encrypted, keys.macKey());
        return messageTag.append(requestHeader(input)).append(hmacSign).append(encrypted).toBuffer();
    }

    private BinaryArray requestHeader(AbstractBinaryRequest<?> input) {
        if (!(input instanceof BinaryRequest<?> binaryRequest)) {
            throw new IllegalArgumentException("WhatsappSerializer only supports standard binary requests");
        }

        return BinaryMetric.toArray(binaryRequest.metrics())
                .append(BinaryArray.singleton(binaryRequest.flag().data()));
    }

    @Override
    public @NonNull BinaryResponse deserialize(@NonNull BinaryArray input) {
        var comma = input.indexOf(',').orElseThrow(() -> new IllegalArgumentException("Cannot deserialize %s: invalid message(no tag header found)".formatted(input.toHex())));
        var messageTag = input.cut(comma).toString();
        var messageContent = input.slice(comma);

        var message = messageContent.slice(32);
        var hmacValidation = CypherUtils.hmacSha256(message, keys.macKey());
        Validate.isTrue(hmacValidation.equals(messageContent.cut(32)), "Cannot read message: Hmac validation failed!", SecurityException.class);

        var decryptedMessage = CypherUtils.aesDecrypt(message, keys.encKey());
        return new BinaryResponse(messageTag, DECODER.decodeDecryptedMessage(decryptedMessage));
    }
}
