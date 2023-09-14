package it.auties.whatsapp.model.button.misc;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

/**
 * A model class that represents a section of buttons
 */
public record ButtonSection(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String title,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT, repeated = true)
        @NonNull
        List<ButtonRow> rows
) implements ProtobufMessage {

}
