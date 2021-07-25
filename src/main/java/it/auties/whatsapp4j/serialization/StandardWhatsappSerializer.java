package it.auties.whatsapp4j.serialization;

import it.auties.whatsapp4j.binary.constant.BinaryMetric;
import it.auties.whatsapp4j.binary.message.StandardBinaryMessage;
import it.auties.whatsapp4j.binary.model.BinaryArray;
import it.auties.whatsapp4j.binary.worker.BinaryDecoder;
import it.auties.whatsapp4j.binary.worker.BinaryEncoder;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.request.model.BinaryRequest;
import it.auties.whatsapp4j.response.model.binary.BinaryResponse;
import it.auties.whatsapp4j.utils.internal.CypherUtils;
import it.auties.whatsapp4j.utils.internal.Validate;
import lombok.NonNull;

import java.nio.ByteBuffer;
import java.util.Objects;

public record StandardWhatsappSerializer(BinaryEncoder encoder,
                                         BinaryDecoder decoder,
                                         WhatsappKeysManager whatsappKeys) implements WhatsappSerializer {
    public StandardWhatsappSerializer(WhatsappKeysManager whatsappKeys) {
        this(new BinaryEncoder(), new BinaryDecoder(), whatsappKeys);
    }

    @Override
    public @NonNull ByteBuffer serialize(@NonNull BinaryRequest<?> input) {
        var messageTag = BinaryArray.forString("%s,".formatted(input.tag()));
        var encodedMessage = encoder.encodeMessage(input.buildBody());
        var encrypted = CypherUtils.aesEncrypt(encodedMessage, whatsappKeys.encKey());
        var hmacSign = CypherUtils.hmacSha256(encrypted, whatsappKeys.macKey());
        return messageTag.merged(BinaryMetric.toArray(input.tags())
                .merged(BinaryArray.singleton(input.flag().data())))
                .merged(hmacSign)
                .merged(encrypted)
                .toBuffer();
    }

    @Override
    public @NonNull BinaryResponse deserialize(@NonNull BinaryArray array) {
        var msg = (StandardBinaryMessage) array.toMessage(false);
        var messageContent = msg.message();
        Validate.isTrue(messageContent.at(0) != '!', "Server pong from whatsapp, why did this get through?");

        var message = messageContent.slice(32);
        var hmacValidation = CypherUtils.hmacSha256(message, Objects.requireNonNull(whatsappKeys.macKey()));
        Validate.isTrue(hmacValidation.equals(messageContent.cut(32)), "Cannot read message: Hmac validation failed!", SecurityException.class);

        var decryptedMessage = CypherUtils.aesDecrypt(message, Objects.requireNonNull(whatsappKeys.encKey()));
        return new BinaryResponse(msg.tag(), decoder.decodeDecryptedMessage(decryptedMessage));
    }
}
