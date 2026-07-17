package com.github.auties00.cobalt.wire.cloud.template;

import java.util.Objects;
import java.util.Optional;

/**
 * A message-template category change, decoded from a {@code template_category_update} webhook
 * change.
 *
 * <p>Meta periodically recategorises templates (for example from {@code MARKETING} to
 * {@code UTILITY}) based on their content; the change affects pricing and delivery rules, so the
 * platform announces both the previous and the new category.
 */
public final class CloudTemplateCategoryUpdate {
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
     * The category before the change, or {@code null} when not reported.
     */
    private final String previousCategory;

    /**
     * The category after the change.
     */
    private final String newCategory;

    /**
     * The category Meta determined to be correct, or {@code null} when not reported.
     */
    private final String correctCategory;

    /**
     * Constructs a new template category update.
     *
     * @param messageTemplateId       the server-assigned template id
     * @param messageTemplateName     the template name
     * @param messageTemplateLanguage the template language code
     * @param previousCategory        the category before the change, or {@code null}
     * @param newCategory             the category after the change
     * @param correctCategory         the category Meta determined to be correct, or {@code null}
     * @throws NullPointerException if {@code newCategory} is {@code null}
     */
    public CloudTemplateCategoryUpdate(String messageTemplateId, String messageTemplateName,
                                       String messageTemplateLanguage, String previousCategory,
                                       String newCategory, String correctCategory) {
        this.messageTemplateId = messageTemplateId;
        this.messageTemplateName = messageTemplateName;
        this.messageTemplateLanguage = messageTemplateLanguage;
        this.previousCategory = previousCategory;
        this.newCategory = Objects.requireNonNull(newCategory, "newCategory must not be null");
        this.correctCategory = correctCategory;
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
     * Returns the category before the change.
     *
     * @return an {@link Optional} carrying the previous category, or empty when not reported
     */
    public Optional<String> previousCategory() {
        return Optional.ofNullable(previousCategory);
    }

    /**
     * Returns the category after the change.
     *
     * @return the new category
     */
    public String newCategory() {
        return newCategory;
    }

    /**
     * Returns the category Meta determined to be correct.
     *
     * @return an {@link Optional} carrying the correct category, or empty when not reported
     */
    public Optional<String> correctCategory() {
        return Optional.ofNullable(correctCategory);
    }
}
