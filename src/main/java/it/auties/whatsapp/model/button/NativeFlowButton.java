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
 * A model class that represents a native flow button
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder(builderMethodName = "newNativeFlowButtonBuilder")
@Jacksonized
@Accessors(fluent = true)
public class NativeFlowButton implements ProtobufMessage {
    /**
     * The name of this button
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String name;

    /**
     * The parameters of this button as json
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String parameters;
}
