package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * A model class that represents a quick reply button
 */
@ProtobufMessage(name = "TemplateButton.QuickReplyButton")
public record HighlyStructuredQuickReplyButton(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        HighlyStructuredMessage text,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String id
) implements HighlyStructuredButton {
    public Type buttonType() {
        return Type.QUICK_REPLY;
    }
}
