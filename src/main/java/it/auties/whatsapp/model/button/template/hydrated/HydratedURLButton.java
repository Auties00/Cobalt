package it.auties.whatsapp.model.button.template.hydrated;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model class that represents a hydrated url button
 */
@ProtobufMessage(name = "HydratedTemplateButton.HydratedURLButton")
public final class HydratedURLButton implements HydratedButton {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String text;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String url;

    HydratedURLButton(String text, String url) {
        this.text = Objects.requireNonNull(text, "text cannot be null");
        this.url = Objects.requireNonNull(url, "url cannot be null");
    }

    public String text() {
        return text;
    }

    public String url() {
        return url;
    }

    @Override
    public Type buttonType() {
        return Type.URL;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HydratedURLButton that
                && Objects.equals(text, that.text)
                && Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, url);
    }

    @Override
    public String toString() {
        return "HydratedURLButton[" +
                "text=" + text + ", " +
                "url=" + url + ']';
    }
}