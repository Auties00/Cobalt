package it.auties.whatsapp.model.button.base;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A model class that represents data about a row
 */
@ProtobufMessage(name = "MsgRowOpaqueData")
public record ButtonRowOpaqueData(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        Optional<ButtonOpaqueData> currentMessage,
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        Optional<ButtonOpaqueData> quotedMessage
) {

}