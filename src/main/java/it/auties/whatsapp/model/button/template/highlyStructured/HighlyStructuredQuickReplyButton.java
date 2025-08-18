package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model class that represents a quick reply button
 */
@ProtobufMessage(name = "TemplateButton.QuickReplyButton")
public final class HighlyStructuredQuickReplyButton implements HighlyStructuredButton {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final HighlyStructuredMessage text;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String id;

    HighlyStructuredQuickReplyButton(HighlyStructuredMessage text, String id) {
        this.text = Objects.requireNonNull(text, "text cannot be null");
        this.id = Objects.requireNonNull(id, "id cannot be null");
    }

    public HighlyStructuredMessage text() {
        return text;
    }

    public String id() {
        return id;
    }

    public Type buttonType() {
        return Type.QUICK_REPLY;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HighlyStructuredQuickReplyButton that
                && Objects.equals(text, that.text)
                && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, id);
    }

    @Override
    public String toString() {
        return "HighlyStructuredQuickReplyButton[" +
                "text=" + text + ", " +
                "id=" + id + ']';
    }
}