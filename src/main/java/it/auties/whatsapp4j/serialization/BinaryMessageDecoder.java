package it.auties.whatsapp4j.serialization;

import it.auties.whatsapp4j.binary.BinaryArray;
import it.auties.whatsapp4j.binary.BinaryDecoder;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.response.model.binary.BinaryResponse;
import it.auties.whatsapp4j.utils.internal.CypherUtils;
import it.auties.whatsapp4j.utils.internal.Validate;
import lombok.NonNull;
import jakarta.websocket.Decoder.Binary;

import java.nio.ByteBuffer;
import java.util.Objects;

public class BinaryMessageDecoder implements Binary<BinaryResponse> {
    private static final WhatsappKeysManager KEYS_MANAGER = WhatsappKeysManager.singletonInstance();
    private static final BinaryDecoder DECODER = new BinaryDecoder();

    @Override
    public BinaryResponse decode(@NonNull ByteBuffer msg) {
        Validate.isTrue(msg.get(0) != '!', "Server pong from whatsapp, why did this get through?");

        var binaryMessage = BinaryArray.forArray(msg.array());
        var tagAndMessagePair = binaryMessage.indexOf(',').map(binaryMessage::split).orElseThrow();

        var messageTag = tagAndMessagePair.key().toString();
        var messageContent = tagAndMessagePair.value();

        var message = messageContent.slice(32);
        var hmacValidation = CypherUtils.hmacSha256(message, Objects.requireNonNull(KEYS_MANAGER.macKey()));
        Validate.isTrue(hmacValidation.equals(messageContent.cut(32)), "Cannot read message: Hmac validation failed!", SecurityException.class);

        var decryptedMessage = CypherUtils.aesDecrypt(message, Objects.requireNonNull(KEYS_MANAGER.encKey()));
        return new BinaryResponse(messageTag, DECODER.decodeDecryptedMessage(decryptedMessage));
    }

    @Override
    public boolean willDecode(@NonNull ByteBuffer byteBuffer) {
        return true;
    }
}
