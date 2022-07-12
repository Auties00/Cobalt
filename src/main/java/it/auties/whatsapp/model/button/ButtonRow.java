package it.auties.whatsapp.model.button;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

/**
 * A model class that represents a row of buttons
 */
@AllArgsConstructor
@Data
@Builder(builderMethodName = "newButtonRowBuilder")
@Jacksonized
@Accessors(fluent = true)
public class ButtonRow implements ProtobufMessage {
    /**
     * The title of the row
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String title;

    /**
     * The description of the row
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String description;

    /**
     * The id of the row
     */
    @ProtobufProperty(index = 3, type = STRING)
    private String id;
}
