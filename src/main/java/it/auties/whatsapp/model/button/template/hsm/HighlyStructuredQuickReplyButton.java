package it.auties.whatsapp.model.button.template.hsm;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.template.highlyStructured.HighlyStructuredMessage;

/**
 * A model class that represents a quick reply button
 */
@ProtobufMessageName("TemplateButton.QuickReplyButton")
public record HighlyStructuredQuickReplyButton(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        HighlyStructuredMessage text,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String id
) implements HighlyStructuredButton {
    public Type buttonType() {
        return Type.QUICK_REPLY;
    }
}
