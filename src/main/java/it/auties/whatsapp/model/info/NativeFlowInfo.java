package it.auties.whatsapp.model.info;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.button.base.ButtonBody;
import it.auties.whatsapp.model.button.base.ButtonBodyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that holds the information related to a native flow.
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class NativeFlowInfo implements ProtobufMessage, ButtonBody, Info {
    /**
     * The name of the flow
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String name;

    /**
     * The params of the flow, encoded as json
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String parameters;

    @Override
    public ButtonBodyType bodyType() {
        return ButtonBodyType.NATIVE_FLOW;
    }
}
