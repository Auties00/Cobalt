package it.auties.whatsapp.model.button;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
/**
 * A model class that represents data about a row
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("MsgRowOpaqueData")
public class ButtonRowOpaqueData implements ProtobufMessage {
    /**
     * The current message
     */
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = ButtonOpaqueData.class)
    private ButtonOpaqueData currentMessage;

    /**
     * The quoted message
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = ButtonOpaqueData.class)
    private ButtonOpaqueData quotedMessage;
}