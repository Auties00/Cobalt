package com.github.auties00.cobalt.wire.linked.message.interactive;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents an action link that pairs a URL with a labelled button.
 *
 * <p>Action links are used inside rich message content (for example location messages with a
 * "call-to-action" overlay) to give the recipient a tappable button that opens a URL in the
 * system browser or in an in-app web view. Both the URL and the button title are optional at
 * the protocol level, but a well-formed action link should provide at least a URL and a
 * title.
 */
@ProtobufMessage(name = "ActionLink")
public final class InteractiveActionLink {
    /**
     * The URL that is opened when the button is tapped.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String url;

    /**
     * The human-readable label rendered on the button.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String buttonTitle;


    /**
     * Constructs a new interactive action link with the supplied URL and button label.
     *
     * @param url         the URL to open when the button is tapped, possibly {@code null}
     * @param buttonTitle the label rendered on the button, possibly {@code null}
     */
    InteractiveActionLink(String url, String buttonTitle) {
        this.url = url;
        this.buttonTitle = buttonTitle;
    }

    /**
     * Returns the URL associated with this action link.
     *
     * @return an {@code Optional} containing the URL, or empty if not set
     */
    public Optional<String> url() {
        return Optional.ofNullable(url);
    }

    /**
     * Returns the label rendered on the action button.
     *
     * @return an {@code Optional} containing the button title, or empty if not set
     */
    public Optional<String> buttonTitle() {
        return Optional.ofNullable(buttonTitle);
    }

    /**
     * Updates the URL opened when the button is tapped.
     *
     * @param url the new URL, or {@code null} to clear the field
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Updates the label rendered on the action button.
     *
     * @param buttonTitle the new button title, or {@code null} to clear the field
     */
    public void setButtonTitle(String buttonTitle) {
        this.buttonTitle = buttonTitle;
    }
}
