package it.auties.whatsapp.model.button.base;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Bytes;

import java.util.HexFormat;

/**
 * A model class that represents a row of buttons
 */
@ProtobufMessage(name = "Message.ListMessage.Row")
public record ButtonRow(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String title,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String description,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String id
) {
    public static ButtonRow of(String title, String description) {
        return new ButtonRow(title, description, HexFormat.of().formatHex(Bytes.random(5)));
    }
}
