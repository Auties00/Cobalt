package com.github.auties00.cobalt.wire.cloud.template;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A webhook notification that a WhatsApp Cloud API message template was paused or unpaused.
 *
 * <p>The server pauses a message template whose recent sends triggered too many negative signals, halting
 * further sends until the pause lifts; the matching unpause notification reports that sending may resume.
 * This model carries the affected template's {@link #messageTemplateId() id},
 * {@link #messageTemplateName() name}, and {@link #messageTemplateLanguage() language}, the
 * {@link #reason() reason} the server reported, and, when present, the {@link #pauseDate() pause date}. The
 * id, name, language, and reason are always present; the pause date is populated only when the
 * notification carried one.
 */
public final class CloudTemplatePauseUpdate {
    /**
     * The affected template's id.
     */
    private final String messageTemplateId;

    /**
     * The affected template's name.
     */
    private final String messageTemplateName;

    /**
     * The affected template's language code.
     */
    private final String messageTemplateLanguage;

    /**
     * The reason the server reported for the pause or unpause.
     */
    private final String reason;

    /**
     * The instant the pause took effect, or {@code null} when the notification carried none.
     */
    private final Instant pauseDate;

    /**
     * Constructs a new template-pause update.
     *
     * @param messageTemplateId       the affected template's id
     * @param messageTemplateName     the affected template's name
     * @param messageTemplateLanguage the affected template's language code
     * @param reason                  the reason the server reported
     * @param pauseDate               the instant the pause took effect, or {@code null} when none
     * @throws NullPointerException if {@code messageTemplateId}, {@code messageTemplateName},
     *                              {@code messageTemplateLanguage}, or {@code reason} is {@code null}
     */
    public CloudTemplatePauseUpdate(String messageTemplateId, String messageTemplateName,
                                    String messageTemplateLanguage, String reason, Instant pauseDate) {
        this.messageTemplateId = Objects.requireNonNull(messageTemplateId, "messageTemplateId must not be null");
        this.messageTemplateName = Objects.requireNonNull(messageTemplateName, "messageTemplateName must not be null");
        this.messageTemplateLanguage = Objects.requireNonNull(messageTemplateLanguage,
                "messageTemplateLanguage must not be null");
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
        this.pauseDate = pauseDate;
    }

    /**
     * Returns the affected template's id.
     *
     * @return the template id
     */
    public String messageTemplateId() {
        return messageTemplateId;
    }

    /**
     * Returns the affected template's name.
     *
     * @return the template name
     */
    public String messageTemplateName() {
        return messageTemplateName;
    }

    /**
     * Returns the affected template's language code.
     *
     * @return the template language code
     */
    public String messageTemplateLanguage() {
        return messageTemplateLanguage;
    }

    /**
     * Returns the reason the server reported for the pause or unpause.
     *
     * @return the reason
     */
    public String reason() {
        return reason;
    }

    /**
     * Returns the instant the pause took effect.
     *
     * @return an {@link Optional} carrying the pause date, or empty when the notification carried none
     */
    public Optional<Instant> pauseDate() {
        return Optional.ofNullable(pauseDate);
    }
}
