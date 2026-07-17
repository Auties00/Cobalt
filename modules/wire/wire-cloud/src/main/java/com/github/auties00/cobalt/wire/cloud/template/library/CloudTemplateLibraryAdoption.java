package com.github.auties00.cobalt.wire.cloud.template.library;

import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateCategory;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;

/**
 * The request to create a message template by adopting a WhatsApp Cloud API Template Library entry.
 *
 * <p>Rather than authoring and submitting a template from scratch, a business can adopt a pre-written,
 * pre-approved Template Library entry. Adoption supplies the business's own template
 * {@link #name() name}, the target {@link #language() language}, the {@link #category() category} the new
 * template is filed under, the {@link #libraryTemplateName() library template name} that selects the
 * source entry, and the {@link #libraryButtons() button bindings} that fill in the entry's buttons. This
 * model is the input to {@code CloudWhatsAppClient.createTemplateFromLibrary}; the name, language,
 * category, and library template name are required, while an empty button list means the entry declares no
 * caller-bound buttons.
 */
@ProtobufMessage
public final class CloudTemplateLibraryAdoption {
    /**
     * The business's own template name.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    /**
     * The target language code, for example {@code en_US}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String language;

    /**
     * The category the new template is filed under.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    final CloudTemplateCategory category;

    /**
     * The library template name that selects the source entry.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String libraryTemplateName;

    /**
     * The button bindings that fill in the source entry's buttons, empty when it declares none.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final List<CloudTemplateLibraryButtonInput> libraryButtons;

    /**
     * Constructs a new template-library adoption request.
     *
     * @param name                the business's own template name
     * @param language            the target language code
     * @param category            the category the new template is filed under
     * @param libraryTemplateName the library template name that selects the source entry
     * @param libraryButtons      the button bindings, or {@code null} when the entry declares none
     * @throws NullPointerException if {@code name}, {@code language}, {@code category}, or
     *                              {@code libraryTemplateName} is {@code null}
     */
    CloudTemplateLibraryAdoption(String name, String language, CloudTemplateCategory category,
                                 String libraryTemplateName, List<CloudTemplateLibraryButtonInput> libraryButtons) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.language = Objects.requireNonNull(language, "language must not be null");
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.libraryTemplateName = Objects.requireNonNull(libraryTemplateName, "libraryTemplateName must not be null");
        this.libraryButtons = libraryButtons == null ? List.of() : List.copyOf(libraryButtons);
    }

    /**
     * Returns the business's own template name.
     *
     * @return the name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the target language code.
     *
     * @return the language code
     */
    public String language() {
        return language;
    }

    /**
     * Returns the category the new template is filed under.
     *
     * @return the category
     */
    public CloudTemplateCategory category() {
        return category;
    }

    /**
     * Returns the library template name that selects the source entry.
     *
     * @return the library template name
     */
    public String libraryTemplateName() {
        return libraryTemplateName;
    }

    /**
     * Returns the button bindings that fill in the source entry's buttons.
     *
     * @return an unmodifiable list of button bindings, empty when the entry declares none
     */
    public List<CloudTemplateLibraryButtonInput> libraryButtons() {
        return libraryButtons;
    }
}
