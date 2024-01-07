package it.auties.whatsapp.model.button.misc;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;

/**
 * A model class that represents a section of buttons
 */
@ProtobufMessageName("Message.ListMessage.Section")
public record ButtonSection(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String title,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        List<ButtonRow> rows
) implements ProtobufMessage {

}
