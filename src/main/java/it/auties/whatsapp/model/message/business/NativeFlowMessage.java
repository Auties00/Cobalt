package it.auties.whatsapp.model.message.business;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.button.NativeFlowButton;
import it.auties.whatsapp.model.message.model.BusinessMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class NativeFlowMessage implements BusinessMessage {
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = NativeFlowButton.class, repeated = true)
    private List<NativeFlowButton> buttons;

    @ProtobufProperty(index = 2, type = STRING)
    private String messageParamsJson;

    @ProtobufProperty(index = 3, type = INT32)
    private int messageVersion;

    public static class NativeFlowMessageBuilder {
        public NativeFlowMessageBuilder buttons(List<NativeFlowButton> buttons) {
            if (this.buttons == null)
                this.buttons = new ArrayList<>();
            this.buttons.addAll(buttons);
            return this;
        }
    }
}
