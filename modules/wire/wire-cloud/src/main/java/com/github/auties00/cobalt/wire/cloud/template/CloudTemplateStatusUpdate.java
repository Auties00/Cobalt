package com.github.auties00.cobalt.wire.cloud.template;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A message-template review status transition, decoded from a
 * {@code message_template_status_update} webhook change.
 *
 * <p>Meta reviews every template before it can be sent and may later disable or pause it; each
 * transition is delivered as one update carrying the new review event, the template identity, and,
 * when the template was rejected or disabled, the reason and the disable date.
 */
public final class CloudTemplateStatusUpdate {
    /**
     * The review event, for example {@code APPROVED}, {@code REJECTED}, or {@code DISABLED}.
     */
    private final String event;

    /**
     * The server-assigned template id.
     */
    private final String messageTemplateId;

    /**
     * The template name.
     */
    private final String messageTemplateName;

    /**
     * The template language code.
     */
    private final String messageTemplateLanguage;

    /**
     * The reason behind the transition, or {@code null} when none was reported.
     */
    private final String reason;

    /**
     * The instant the template was disabled, or {@code null} when the event is not a disable.
     */
    private final Instant disableDate;

    /**
     * Constructs a new template status update.
     *
     * @param event                   the review event
     * @param messageTemplateId       the server-assigned template id
     * @param messageTemplateName     the template name
     * @param messageTemplateLanguage the template language code
     * @param reason                  the transition reason, or {@code null} when none was reported
     * @param disableDate             the disable instant, or {@code null} when not a disable
     * @throws NullPointerException if {@code event} is {@code null}
     */
    public CloudTemplateStatusUpdate(String event, String messageTemplateId, String messageTemplateName,
                                     String messageTemplateLanguage, String reason, Instant disableDate) {
        this.event = Objects.requireNonNull(event, "event must not be null");
        this.messageTemplateId = messageTemplateId;
        this.messageTemplateName = messageTemplateName;
        this.messageTemplateLanguage = messageTemplateLanguage;
        this.reason = reason;
        this.disableDate = disableDate;
    }

    /**
     * Returns the review event.
     *
     * @return the event, for example {@code APPROVED}
     */
    public String event() {
        return event;
    }

    /**
     * Returns the server-assigned template id.
     *
     * @return an {@link Optional} carrying the id, or empty when absent
     */
    public Optional<String> messageTemplateId() {
        return Optional.ofNullable(messageTemplateId);
    }

    /**
     * Returns the template name.
     *
     * @return an {@link Optional} carrying the name, or empty when absent
     */
    public Optional<String> messageTemplateName() {
        return Optional.ofNullable(messageTemplateName);
    }

    /**
     * Returns the template language code.
     *
     * @return an {@link Optional} carrying the language code, or empty when absent
     */
    public Optional<String> messageTemplateLanguage() {
        return Optional.ofNullable(messageTemplateLanguage);
    }

    /**
     * Returns the reason behind the transition.
     *
     * @return an {@link Optional} carrying the reason, or empty when none was reported
     */
    public Optional<String> reason() {
        return Optional.ofNullable(reason);
    }

    /**
     * Returns the instant the template was disabled.
     *
     * @return an {@link Optional} carrying the disable instant, or empty when the event is not a
     *         disable
     */
    public Optional<Instant> disableDate() {
        return Optional.ofNullable(disableDate);
    }
}
