package com.github.auties00.cobalt.wire.linked.message.interactive;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents a "tap to open link" action bound to an {@link InteractiveAnnotation}.
 *
 * <p>A tap link action turns a region of a media attachment into a clickable call to action:
 * the recipient sees the {@link #title()} as a label, and tapping it opens the URL returned
 * by {@link #tapUrl()} in the system browser or in an in-app web view. Both fields are
 * optional at the protocol level, but a valid action should provide at least a URL.
 */
@ProtobufMessage(name = "TapLinkAction")
public final class TapLinkAction implements InteractiveAction {
    /**
     * The label displayed to the recipient as the call-to-action.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String title;

    /**
     * The URL opened when the recipient taps the action.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String tapUrl;


    /**
     * Constructs a new tap link action with the supplied title and URL.
     *
     * @param title  the call-to-action label, possibly {@code null}
     * @param tapUrl the URL opened on tap, possibly {@code null}
     */
    TapLinkAction(String title, String tapUrl) {
        this.title = title;
        this.tapUrl = tapUrl;
    }

    /**
     * Returns the call-to-action label displayed to the recipient.
     *
     * @return an {@code Optional} containing the title, or empty if not set
     */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /**
     * Returns the URL opened when the recipient taps the action.
     *
     * @return an {@code Optional} containing the URL, or empty if not set
     */
    public Optional<String> tapUrl() {
        return Optional.ofNullable(tapUrl);
    }

    /**
     * Updates the call-to-action label.
     *
     * @param title the new title, or {@code null} to clear the field
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Updates the URL opened when the recipient taps the action.
     *
     * @param tapUrl the new URL, or {@code null} to clear the field
     */
    public void setTapUrl(String tapUrl) {
        this.tapUrl = tapUrl;
    }
}
