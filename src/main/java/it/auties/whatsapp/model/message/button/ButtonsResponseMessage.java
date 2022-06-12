package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

/**
 * A model class that represents a message that contains a response to a previous {@link ButtonsMessage}
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Jacksonized
@Builder
@Accessors(fluent = true)
public final class ButtonsResponseMessage extends ContextualMessage implements ButtonMessage {
    /**
     * The jid of the button that was selected
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String buttonId;

    /**
     * The display text of the button that was selected
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String displayText;

    /**
     * The context info of this message
     */
    @ProtobufProperty(index = 3, type = MESSAGE, concreteType = ContextInfo.class)
    private ContextInfo contextInfo; // Overrides ContextualMessage's context info
}
