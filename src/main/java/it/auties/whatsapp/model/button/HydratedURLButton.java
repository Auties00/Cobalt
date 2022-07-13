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
 * A model class that represents a hydrated url button
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder(builderMethodName = "newHydratedURLButton")
@Jacksonized
@Accessors(fluent = true)
public class HydratedURLButton implements ProtobufMessage {
    /**
     * The text of this button
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String text;

    /**
     * The url of this button
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String url;
}
