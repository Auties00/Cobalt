package com.github.auties00.cobalt.wire.cloud.template;

import java.util.Objects;
import java.util.Optional;

/**
 * A message-template quality score change, decoded from a
 * {@code message_template_quality_update} webhook change.
 *
 * <p>Meta scores templates by recipient feedback (blocks, reports, read rates); a degrading score
 * warns that the template risks being paused or disabled.
 */
public final class CloudTemplateQualityUpdate {
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
     * The quality score before the change, or {@code null} when not reported.
     */
    private final String previousQualityScore;

    /**
     * The quality score after the change, for example {@code GREEN}, {@code YELLOW}, or {@code RED}.
     */
    private final String newQualityScore;

    /**
     * Constructs a new template quality update.
     *
     * @param messageTemplateId       the server-assigned template id
     * @param messageTemplateName     the template name
     * @param messageTemplateLanguage the template language code
     * @param previousQualityScore    the score before the change, or {@code null}
     * @param newQualityScore         the score after the change
     * @throws NullPointerException if {@code newQualityScore} is {@code null}
     */
    public CloudTemplateQualityUpdate(String messageTemplateId, String messageTemplateName,
                                      String messageTemplateLanguage, String previousQualityScore,
                                      String newQualityScore) {
        this.messageTemplateId = messageTemplateId;
        this.messageTemplateName = messageTemplateName;
        this.messageTemplateLanguage = messageTemplateLanguage;
        this.previousQualityScore = previousQualityScore;
        this.newQualityScore = Objects.requireNonNull(newQualityScore, "newQualityScore must not be null");
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
     * Returns the quality score before the change.
     *
     * @return an {@link Optional} carrying the previous score, or empty when not reported
     */
    public Optional<String> previousQualityScore() {
        return Optional.ofNullable(previousQualityScore);
    }

    /**
     * Returns the quality score after the change.
     *
     * @return the new score, for example {@code GREEN}
     */
    public String newQualityScore() {
        return newQualityScore;
    }
}
