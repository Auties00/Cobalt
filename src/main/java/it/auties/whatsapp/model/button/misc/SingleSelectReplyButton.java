package it.auties.whatsapp.model.button.misc;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A model class that represents the selection of a row
 */
public record SingleSelectReplyButton(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String rowId
) implements ProtobufMessage {

}