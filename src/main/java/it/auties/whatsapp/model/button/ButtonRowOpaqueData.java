package it.auties.whatsapp.model.button;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

/**
 * A model class that represents data about a row
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder(builderMethodName = "newButtonRowOpaqueDataBuilder")
@Jacksonized
@Accessors(fluent = true)
public class ButtonRowOpaqueData implements ProtobufMessage {
    /**
     * The current message
     */
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = ButtonOpaqueData.class)
    private ButtonOpaqueData currentMessage;

    /**
     * The quoted message
     */
    @ProtobufProperty(index = 2, type = MESSAGE, concreteType = ButtonOpaqueData.class)
    private ButtonOpaqueData quotedMessage;
}
