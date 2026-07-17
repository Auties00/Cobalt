package com.github.auties00.cobalt.wire.cloud.template.library;

import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateCategory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A template offered by the WhatsApp Cloud API Template Library.
 *
 * <p>The Template Library is Meta's catalog of pre-written, pre-approved templates a business can adopt
 * without authoring and submitting its own. This model carries the browse view of one library entry:
 * the library id, the library template name to pass back when creating a template from it, the topic,
 * use case, industries, language, category, header and body text, the body placeholder example values,
 * and the buttons the entry declares.
 *
 * <p>To adopt a library entry a business creates a template from it, supplying its own name and the
 * caller-bindable button values; the entry's {@link #name()} is the library template name that selects
 * the source entry.
 */
public final class CloudTemplateLibraryItem {
    /**
     * The library template id, or {@code null} when the catalog did not carry one.
     */
    private final String id;

    /**
     * The library template name passed back to select this entry when creating a template.
     */
    private final String name;

    /**
     * The catalog topic, or {@code null} when unset.
     */
    private final String topic;

    /**
     * The catalog use case, or {@code null} when unset.
     */
    private final String usecase;

    /**
     * The industries the entry applies to.
     */
    private final List<String> industry;

    /**
     * The BCP-47 language code, or {@code null} when unset.
     */
    private final String language;

    /**
     * The category, or {@code null} when unset.
     */
    private final CloudTemplateCategory category;

    /**
     * The header text, or {@code null} when the entry has no header.
     */
    private final String header;

    /**
     * The body text, or {@code null} when unset.
     */
    private final String body;

    /**
     * The example values for the body placeholders.
     */
    private final List<String> bodyExampleParams;

    /**
     * The buttons the entry declares.
     */
    private final List<CloudTemplateLibraryItemButton> buttons;

    /**
     * Constructs a new library item.
     *
     * @param id                the library template id, or {@code null} when none
     * @param name              the library template name
     * @param topic             the catalog topic, or {@code null} when unset
     * @param usecase           the catalog use case, or {@code null} when unset
     * @param industry          the industries, or {@code null} for none
     * @param language          the BCP-47 language code, or {@code null} when unset
     * @param category          the category, or {@code null} when unset
     * @param header            the header text, or {@code null} when none
     * @param body              the body text, or {@code null} when unset
     * @param bodyExampleParams the body placeholder example values, or {@code null} for none
     * @param buttons           the declared buttons, or {@code null} for none
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public CloudTemplateLibraryItem(String id, String name, String topic, String usecase, List<String> industry,
                                    String language, CloudTemplateCategory category, String header, String body,
                                    List<String> bodyExampleParams, List<CloudTemplateLibraryItemButton> buttons) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.topic = topic;
        this.usecase = usecase;
        this.industry = industry == null ? List.of() : List.copyOf(industry);
        this.language = language;
        this.category = category;
        this.header = header;
        this.body = body;
        this.bodyExampleParams = bodyExampleParams == null ? List.of() : List.copyOf(bodyExampleParams);
        this.buttons = buttons == null ? List.of() : List.copyOf(buttons);
    }

    /**
     * Returns the library template id.
     *
     * @return an {@link Optional} carrying the id, or empty when none
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the library template name.
     *
     * @return the name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the catalog topic.
     *
     * @return an {@link Optional} carrying the topic, or empty when unset
     */
    public Optional<String> topic() {
        return Optional.ofNullable(topic);
    }

    /**
     * Returns the catalog use case.
     *
     * @return an {@link Optional} carrying the use case, or empty when unset
     */
    public Optional<String> usecase() {
        return Optional.ofNullable(usecase);
    }

    /**
     * Returns the industries the entry applies to.
     *
     * @return an unmodifiable list of industries, empty when none were declared
     */
    public List<String> industry() {
        return industry;
    }

    /**
     * Returns the BCP-47 language code.
     *
     * @return an {@link Optional} carrying the language code, or empty when unset
     */
    public Optional<String> language() {
        return Optional.ofNullable(language);
    }

    /**
     * Returns the category.
     *
     * @return an {@link Optional} carrying the {@link CloudTemplateCategory}, or empty when unset
     */
    public Optional<CloudTemplateCategory> category() {
        return Optional.ofNullable(category);
    }

    /**
     * Returns the header text.
     *
     * @return an {@link Optional} carrying the header, or empty when none
     */
    public Optional<String> header() {
        return Optional.ofNullable(header);
    }

    /**
     * Returns the body text.
     *
     * @return an {@link Optional} carrying the body, or empty when unset
     */
    public Optional<String> body() {
        return Optional.ofNullable(body);
    }

    /**
     * Returns the example values for the body placeholders.
     *
     * @return an unmodifiable list of example values, empty when none were declared
     */
    public List<String> bodyExampleParams() {
        return bodyExampleParams;
    }

    /**
     * Returns the buttons the entry declares.
     *
     * @return an unmodifiable list of {@link CloudTemplateLibraryItemButton}, empty when none were
     *         declared
     */
    public List<CloudTemplateLibraryItemButton> buttons() {
        return buttons;
    }
}
