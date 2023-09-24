package it.auties.whatsapp.model.button.misc;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A model class that represents data about a row
 */
@ProtobufMessageName("MsgRowOpaqueData")
public record ButtonRowOpaqueData(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        Optional<ButtonOpaqueData> currentMessage,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        Optional<ButtonOpaqueData> quotedMessage
) implements ProtobufMessage {

}