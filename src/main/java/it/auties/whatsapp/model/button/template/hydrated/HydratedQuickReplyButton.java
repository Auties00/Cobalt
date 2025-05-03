package it.auties.whatsapp.model.button.template.hydrated;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Bytes;

import java.util.Objects;

/**
 * A model class that represents a hydrated quick reply button
 */
@ProtobufMessage(name = "HydratedTemplateButton.HydratedQuickReplyButton")
public final class HydratedQuickReplyButton implements HydratedButton {

    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String text;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String id;

    HydratedQuickReplyButton(String text, String id) {
        this.text = Objects.requireNonNull(text, "text cannot be null");
        this.id = Objects.requireNonNullElseGet(id, () -> Bytes.randomHex(6));
    }

    public String text() {
        return text;
    }

    public String id() {
        return id;
    }

    @Override
    public Type buttonType() {
        return Type.QUICK_REPLY;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HydratedQuickReplyButton that
                && Objects.equals(text, that.text)
                && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, id);
    }

    @Override
    public String toString() {
        return "HydratedQuickReplyButton[" +
                "text=" + text + ", " +
                "id=" + id + ']';
    }
}