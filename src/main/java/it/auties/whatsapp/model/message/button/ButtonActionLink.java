package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

/**
 * An action link for a button
 */
public record ButtonActionLink(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String url,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String buttonTitle
) implements ProtobufMessage {

}