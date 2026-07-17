package com.github.auties00.cobalt.wire.cloud.content;

import com.github.auties00.cobalt.wire.core.message.ButtonsResponseContent;

import java.util.Optional;

/**
 * The Cloud transport's inbound reply-button selection body.
 */
public final class CloudButtonsResponseContent implements ButtonsResponseContent {
    /**
     * The id of the tapped button, or {@code null} when unset.
     */
    private final String selectedButtonId;

    /**
     * The display text of the tapped button, or {@code null} when unset.
     */
    private final String selectedDisplayText;

    /**
     * Constructs a Cloud reply-button selection body.
     *
     * @param selectedButtonId    the id of the tapped button, or {@code null} when unset
     * @param selectedDisplayText the display text of the tapped button, or {@code null} when unset
     */
    public CloudButtonsResponseContent(String selectedButtonId, String selectedDisplayText) {
        this.selectedButtonId = selectedButtonId;
        this.selectedDisplayText = selectedDisplayText;
    }

    @Override
    public Optional<String> selectedButtonId() {
        return Optional.ofNullable(selectedButtonId);
    }

    @Override
    public Optional<String> selectedDisplayText() {
        return Optional.ofNullable(selectedDisplayText);
    }
}
