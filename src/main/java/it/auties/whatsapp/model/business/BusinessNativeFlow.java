package it.auties.whatsapp.model.business;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.button.NativeFlowButton;
import it.auties.whatsapp.model.message.button.InteractiveMessageContent;
import it.auties.whatsapp.model.message.button.InteractiveMessageContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model class that represents a native flow
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class BusinessNativeFlow implements InteractiveMessageContent {
    /**
     * The buttons of this flow
     */
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = NativeFlowButton.class, repeated = true)
    private List<NativeFlowButton> buttons;

    /**
     * The parameters of this flow as json
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String parameters;

    /**
     * The version of this flow
     */
    @ProtobufProperty(index = 3, type = INT32)
    private int version;

    @Override
    public InteractiveMessageContentType contentType() {
        return InteractiveMessageContentType.NATIVE_FLOW;
    }
}
