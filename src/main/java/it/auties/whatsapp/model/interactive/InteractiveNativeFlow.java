package it.auties.whatsapp.model.interactive;

import it.auties.protobuf.base.ProtobufProperty;
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
 * <a href="https://docs.360dialog.com/partner/messaging/interactive-messages/beta-receive-whatsapp-payments-via-stripe">Here</a>> is an explanation on how to use this kind of message
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class InteractiveNativeFlow implements InteractiveMessageContent {
    /**
     * The buttons of this flow
     */
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = InteractiveButton.class, repeated = true)
    private List<InteractiveButton> buttons;

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
