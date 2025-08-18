package it.auties.whatsapp.model.button.base;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * An action link for a button
 */
@ProtobufMessage(name = "ActionLink")
public final class ButtonActionLink {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String url;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String buttonTitle;

    ButtonActionLink(String url, String buttonTitle) {
        this.url = Objects.requireNonNull(url, "url cannot be null");
        this.buttonTitle = Objects.requireNonNull(buttonTitle, "buttonTitle cannot be null");
    }

    public String url() {
        return url;
    }

    public String buttonTitle() {
        return buttonTitle;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ButtonActionLink that
                && Objects.equals(url, that.url)
                && Objects.equals(buttonTitle, that.buttonTitle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, buttonTitle);
    }

    @Override
    public String toString() {
        return "ButtonActionLink[" +
                "url=" + url + ", " +
                "buttonTitle=" + buttonTitle + ']';
    }
}