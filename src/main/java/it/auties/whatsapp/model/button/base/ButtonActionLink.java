package it.auties.whatsapp.model.button.base;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * An action link for a button
 */
@ProtobufMessage(name = "ActionLink")
public record ButtonActionLink(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String url,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String buttonTitle
) {

}