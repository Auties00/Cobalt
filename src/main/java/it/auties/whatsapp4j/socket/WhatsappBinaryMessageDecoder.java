package it.auties.whatsapp4j.socket;

import it.auties.whatsapp4j.binary.BinaryArray;
import it.auties.whatsapp4j.binary.BinaryDecoder;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.response.model.BinaryResponse;
import it.auties.whatsapp4j.utils.CypherUtils;
import it.auties.whatsapp4j.utils.Validate;
import jakarta.validation.constraints.NotNull;
import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder.Binary;

import java.nio.ByteBuffer;
import java.util.Objects;

public class WhatsappBinaryMessageDecoder implements Binary<BinaryResponse> {
    private static final WhatsappKeysManager KEYS_MANAGER = WhatsappKeysManager.singletonInstance();
    private static final BinaryDecoder DECODER = new BinaryDecoder();

    @Override
    public BinaryResponse decode(@NotNull ByteBuffer msg) throws DecodeException {
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
    public boolean willDecode(@NotNull ByteBuffer byteBuffer) {
        return byteBuffer.hasArray();
    }
}
