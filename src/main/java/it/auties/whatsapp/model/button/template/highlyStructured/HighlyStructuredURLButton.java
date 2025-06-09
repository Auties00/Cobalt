package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model class that represents an url button
 */
@ProtobufMessage(name = "TemplateButton.URLButton")
public final class HighlyStructuredURLButton implements HighlyStructuredButton {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final HighlyStructuredMessage text;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final HighlyStructuredMessage url;

    HighlyStructuredURLButton(HighlyStructuredMessage text, HighlyStructuredMessage url) {
        this.text = Objects.requireNonNull(text, "text cannot be null");
        this.url = Objects.requireNonNull(url, "url cannot be null");
    }

    public HighlyStructuredMessage text() {
        return text;
    }

    public HighlyStructuredMessage url() {
        return url;
    }

    @Override
    public Type buttonType() {
        return Type.URL;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HighlyStructuredURLButton that
                && Objects.equals(text, that.text)
                && Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, url);
    }

    @Override
    public String toString() {
        return "HighlyStructuredURLButton[" +
                "text=" + text + ", " +
                "url=" + url + ']';
    }
}