package com.github.auties00.cobalt.wire.cloud;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A user marketing preference change, decoded from one entry of a {@code user_preferences} webhook
 * change.
 *
 * <p>Recipients can opt out of (or back into) marketing messages from a business; each change is
 * delivered as one entry carrying the user, the affected category, and the new preference value.
 */
public final class CloudUserPreferenceUpdate {
    /**
     * The WhatsApp id of the user whose preference changed.
     */
    private final String waId;

    /**
     * The human-readable description of the change, or {@code null} when not reported.
     */
    private final String detail;

    /**
     * The preference category, for example {@code marketing_messages}.
     */
    private final String category;

    /**
     * The new preference value.
     */
    private final CloudMarketingPreference value;

    /**
     * The instant the preference changed, or {@code null} when not reported.
     */
    private final Instant timestamp;

    /**
     * Constructs a new user preference update.
     *
     * @param waId      the WhatsApp id of the user
     * @param detail    the human-readable description, or {@code null}
     * @param category  the preference category
     * @param value     the new preference value
     * @param timestamp the change instant, or {@code null}
     * @throws NullPointerException if {@code waId}, {@code category}, or {@code value} is
     *                              {@code null}
     */
    public CloudUserPreferenceUpdate(String waId, String detail, String category, CloudMarketingPreference value,
                                     Instant timestamp) {
        this.waId = Objects.requireNonNull(waId, "waId must not be null");
        this.detail = detail;
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.value = Objects.requireNonNull(value, "value must not be null");
        this.timestamp = timestamp;
    }

    /**
     * Returns the WhatsApp id of the user whose preference changed.
     *
     * @return the WhatsApp id
     */
    public String waId() {
        return waId;
    }

    /**
     * Returns the human-readable description of the change.
     *
     * @return an {@link Optional} carrying the description, or empty when not reported
     */
    public Optional<String> detail() {
        return Optional.ofNullable(detail);
    }

    /**
     * Returns the preference category.
     *
     * @return the category, for example {@code marketing_messages}
     */
    public String category() {
        return category;
    }

    /**
     * Returns the new preference value.
     *
     * @return the {@link CloudMarketingPreference}
     */
    public CloudMarketingPreference value() {
        return value;
    }

    /**
     * Returns whether the user opted out of the category.
     *
     * @return {@code true} if the preference value is {@link CloudMarketingPreference#STOP},
     *         {@code false} otherwise
     */
    public boolean isOptOut() {
        return value == CloudMarketingPreference.STOP;
    }

    /**
     * Returns the instant the preference changed.
     *
     * @return an {@link Optional} carrying the instant, or empty when not reported
     */
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }
}
