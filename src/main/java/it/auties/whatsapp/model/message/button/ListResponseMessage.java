package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.button.SingleSelectReplyButton;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

/**
 * A model class that represents a message that contains a response to a previous {@link ListMessage}
 */
@AllArgsConstructor
@Data
@Builder(builderMethodName = "newListResponseMessageBuilder")
@Jacksonized
@Accessors(fluent = true)
public final class ListResponseMessage implements ButtonMessage {
    /**
     * The title of this message
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String title;

    /**
     * The selected option
     */
    @ProtobufProperty(index = 3, type = MESSAGE, concreteType = SingleSelectReplyButton.class)
    private SingleSelectReplyButton reply;

    /**
     * The context info of this message
     */
    @ProtobufProperty(index = 4, type = MESSAGE, concreteType = ContextInfo.class)
    @Builder.Default
    private ContextInfo contextInfo = new ContextInfo();  // Overrides ContextualMessage's context info

    /**
     * The description of this message
     */
    @ProtobufProperty(index = 5, type = STRING)
    private String description;

    @Override
    public MessageType type() {
        return MessageType.LIST_RESPONSE;
    }
}
