package com.github.auties00.cobalt.wire.cloud.template;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A registered WhatsApp Cloud API message template.
 *
 * <p>Templates are created under a WhatsApp Business Account, approved by Meta, and then referenced by
 * name and language when sending. This model carries the management view: the server-assigned id, the
 * name, the BCP-47 language code, the {@link CloudTemplateCategory category}, the
 * {@link CloudTemplateStatus review status}, and the ordered list of typed
 * {@link CloudTemplateComponent} definitions (header, body, footer, buttons, and carousel).
 *
 * <p>Sending a message that uses an approved template is done through the universal message model
 * rather than this type: build a template message container and pass it to the client's send method.
 * This type governs the template library, not the act of sending.
 */
public final class CloudMessageTemplate {
    /**
     * The server-assigned template id, or {@code null} before creation.
     */
    private final String id;

    /**
     * The template name.
     */
    private final String name;

    /**
     * The BCP-47 language code, for example {@code en_US}.
     */
    private final String language;

    /**
     * The template category.
     */
    private final CloudTemplateCategory category;

    /**
     * The review status, or {@code null} before review.
     */
    private final CloudTemplateStatus status;

    /**
     * The ordered typed component definitions of the template, empty when none were declared.
     */
    private final List<CloudTemplateComponent> components;

    /**
     * Constructs a new message template.
     *
     * @param id         the server-assigned id, or {@code null} before creation
     * @param name       the template name
     * @param language   the BCP-47 language code
     * @param category   the template category
     * @param status     the review status, or {@code null} before review
     * @param components the ordered typed component definitions, or {@code null} when none were declared
     * @throws NullPointerException if {@code name}, {@code language}, or {@code category} is
     *                              {@code null}
     */
    public CloudMessageTemplate(String id, String name, String language, CloudTemplateCategory category,
                                CloudTemplateStatus status, List<CloudTemplateComponent> components) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.language = Objects.requireNonNull(language, "language must not be null");
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.status = status;
        this.components = components == null ? List.of() : List.copyOf(components);
    }

    /**
     * Returns the server-assigned template id.
     *
     * @return an {@link Optional} carrying the id, or empty before creation
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the template name.
     *
     * @return the name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the BCP-47 language code.
     *
     * @return the language code
     */
    public String language() {
        return language;
    }

    /**
     * Returns the template category.
     *
     * @return the {@link CloudTemplateCategory}
     */
    public CloudTemplateCategory category() {
        return category;
    }

    /**
     * Returns the review status.
     *
     * @return an {@link Optional} carrying the {@link CloudTemplateStatus}, or empty before review
     */
    public Optional<CloudTemplateStatus> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns the ordered typed component definitions of the template.
     *
     * @return an unmodifiable list of components, empty when none were declared
     */
    public List<CloudTemplateComponent> components() {
        return components;
    }
}
