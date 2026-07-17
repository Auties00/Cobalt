package com.github.auties00.cobalt.wire.cloud.template;

import java.util.Objects;
import java.util.Optional;

/**
 * One per-language template created or updated by an upsert request.
 *
 * <p>The upsert edge returns a {@code data} array with one entry per language the request fanned out
 * to. This model carries one such entry: the server-assigned id, the review status, and, where the
 * server reports them, the language code and category of the affected template version.
 */
public final class CloudUpsertedTemplate {
    /**
     * The server-assigned template id.
     */
    private final String id;

    /**
     * The review status.
     */
    private final CloudTemplateStatus status;

    /**
     * The language code of the affected version, or {@code null} when none was returned.
     */
    private final String language;

    /**
     * The template category, or {@code null} when none was returned.
     */
    private final CloudTemplateCategory category;

    /**
     * Constructs a new upserted-template entry.
     *
     * @param id       the server-assigned template id
     * @param status   the review status
     * @param language the language code, or {@code null} when none was returned
     * @param category the template category, or {@code null} when none was returned
     * @throws NullPointerException if {@code id} or {@code status} is {@code null}
     */
    public CloudUpsertedTemplate(String id, CloudTemplateStatus status, String language,
                                 CloudTemplateCategory category) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.language = language;
        this.category = category;
    }

    /**
     * Returns the server-assigned template id.
     *
     * @return the id
     */
    public String id() {
        return id;
    }

    /**
     * Returns the review status.
     *
     * @return the {@link CloudTemplateStatus}
     */
    public CloudTemplateStatus status() {
        return status;
    }

    /**
     * Returns the language code of the affected version.
     *
     * @return an {@link Optional} carrying the language code, or empty when none was returned
     */
    public Optional<String> language() {
        return Optional.ofNullable(language);
    }

    /**
     * Returns the template category.
     *
     * @return an {@link Optional} carrying the {@link CloudTemplateCategory}, or empty when none was
     *         returned
     */
    public Optional<CloudTemplateCategory> category() {
        return Optional.ofNullable(category);
    }
}
