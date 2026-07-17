package com.github.auties00.cobalt.quarantine;

import java.util.Optional;

/**
 * The outcome of classifying an inbound message against the Defense Mode quarantine policy.
 */
public final class QuarantineAction {
    /**
     * The action for a message that is not quarantined.
     */
    public static final QuarantineAction NO_QUARANTINE = new QuarantineAction(false, null);

    /**
     * The action for a quarantined message that surfaces no replacement text.
     */
    public static final QuarantineAction WITHOUT_TEXT = new QuarantineAction(true, null);

    /**
     * Whether the message must be withheld.
     */
    private final boolean shouldQuarantine;

    /**
     * The plain text to surface in place of the withheld message, or {@code null} when the action
     * surfaces no replacement text.
     */
    private final String extractedText;

    /**
     * Constructs a quarantine action with the given verdict and replacement text.
     *
     * @param shouldQuarantine whether the message must be withheld
     * @param extractedText    the replacement text, or {@code null}
     */
    private QuarantineAction(boolean shouldQuarantine, String extractedText) {
        this.shouldQuarantine = shouldQuarantine;
        this.extractedText = extractedText;
    }

    /**
     * Returns a quarantine action that surfaces the given replacement text, or
     * {@link #WITHOUT_TEXT} when the text is {@code null} or empty.
     *
     * @param text the replacement text, or {@code null}
     * @return the with-text action, or {@link #WITHOUT_TEXT}
     */
    public static QuarantineAction of(String text) {
        return text == null || text.isEmpty() ? WITHOUT_TEXT : new QuarantineAction(true, text);
    }

    /**
     * Returns whether the message must be withheld.
     *
     * @return {@code true} when the message must be quarantined, {@code false} otherwise
     */
    public boolean shouldQuarantine() {
        return shouldQuarantine;
    }

    /**
     * Returns the plain text to surface in place of the withheld message.
     *
     * @return an {@code Optional} with the replacement text, or empty when the action surfaces no
     *         replacement text
     */
    public Optional<String> extractedText() {
        return Optional.ofNullable(extractedText);
    }
}
