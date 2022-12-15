package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ButtonReplyMessage;
import it.auties.whatsapp.model.message.model.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model class that represents a message that contains a response to a previous {@link HighlyStructuredMessage}
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Accessors(fluent = true)
public final class TemplateReplyMessage extends ButtonReplyMessage {
    /**
     * The id of the button that was selected from the previous template message
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String id;

    /**
     * The text of the button that was selected from the previous template message
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String buttonText;

    /**
     * The context info of this message
     */
    @ProtobufProperty(index = 3, type = MESSAGE, implementation = ContextInfo.class)
    @Default
    private ContextInfo contextInfo = new ContextInfo();  

    /**
     * The index of the button that was selected from the previous template message
     */
    @ProtobufProperty(index = 4, type = UINT32)
    private int index;

    @Override
    public MessageType type() {
        return MessageType.TEMPLATE_REPLY;
    }
}
