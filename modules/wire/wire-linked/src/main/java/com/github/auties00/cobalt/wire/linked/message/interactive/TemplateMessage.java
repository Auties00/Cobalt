package com.github.auties00.cobalt.wire.linked.message.interactive;

import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;
import com.github.auties00.cobalt.wire.linked.message.location.LocationMessage;
import com.github.auties00.cobalt.wire.linked.message.media.DocumentMessage;
import com.github.auties00.cobalt.wire.linked.message.media.ImageMessage;
import com.github.auties00.cobalt.wire.linked.message.media.VideoMessage;
import com.github.auties00.cobalt.wire.linked.message.text.HighlyStructuredMessage;

import java.util.Collections;
import java.util.List;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents a template message: a rich, button-driven piece of content that business
 * accounts can send to their customers.
 *
 * <p>Template messages render as a structured card with up to four rows (title, content,
 * footer and buttons) that offer the recipient tappable actions such as quick replies, URL
 * navigation or phone calls. The concrete layout is selected from a sealed
 * {@link TemplateFormat} hierarchy:
 * <ul>
 *   <li>{@link FourRowTemplate} the unhydrated layout, where textual fields are
 *       {@link HighlyStructuredMessage} templates with placeholders</li>
 *   <li>{@link HydratedFourRowTemplate} the hydrated layout, where placeholders are already
 *       replaced with literal strings ready for display</li>
 *   <li>{@link InteractiveMessage} a richer interactive message embedded as the template
 *       body</li>
 * </ul>
 *
 * <p>Template messages are contextual: they can carry a {@link ContextInfo} that chains them
 * to an existing conversation thread and an optional {@link #templateId()} that helps
 * businesses correlate responses with the template that produced them.
 */
@ProtobufMessage(name = "Message.TemplateMessage")
public final class TemplateMessage implements ContextualMessage {
    /**
     * Contextual information linking this message to a specific conversation context.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    /**
     * The hydrated four-row template delivered alongside the raw template, for clients that
     * render only hydrated content.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    HydratedFourRowTemplate hydratedTemplate;

    /**
     * Business-defined identifier that links this message back to the original template
     * definition.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    String templateId;

    /**
     * The unhydrated four-row template variant.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    FourRowTemplate fourRowTemplate;

    /**
     * The hydrated four-row template variant.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    HydratedFourRowTemplate hydratedFourRowTemplate;

    /**
     * The interactive message variant that wraps a richer interactive experience.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    InteractiveMessage interactiveMessageTemplate;


    /**
     * Constructs a new template message with the supplied context and format variants.
     *
     * <p>At most one of {@code fourRowTemplate}, {@code hydratedFourRowTemplate} and
     * {@code interactiveMessageTemplate} should be non-null to keep the oneof semantics well
     * defined.
     *
     * @param contextInfo                contextual information, possibly {@code null}
     * @param hydratedTemplate           the companion hydrated template, possibly {@code null}
     * @param templateId                 the business template identifier, possibly {@code null}
     * @param fourRowTemplate            the unhydrated template variant, possibly {@code null}
     * @param hydratedFourRowTemplate    the hydrated template variant, possibly {@code null}
     * @param interactiveMessageTemplate the interactive message variant, possibly {@code null}
     */
    TemplateMessage(ContextInfo contextInfo, HydratedFourRowTemplate hydratedTemplate, String templateId, FourRowTemplate fourRowTemplate, HydratedFourRowTemplate hydratedFourRowTemplate, InteractiveMessage interactiveMessageTemplate) {
        this.contextInfo = contextInfo;
        this.hydratedTemplate = hydratedTemplate;
        this.templateId = templateId;
        this.fourRowTemplate = fourRowTemplate;
        this.hydratedFourRowTemplate = hydratedFourRowTemplate;
        this.interactiveMessageTemplate = interactiveMessageTemplate;
    }

    /**
     * Returns the contextual information attached to this template message.
     *
     * @return an {@code Optional} with the context, or empty if not set
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Returns the hydrated four-row template delivered alongside the raw template variant.
     *
     * @return an {@code Optional} with the hydrated template, or empty if not set
     */
    public Optional<HydratedFourRowTemplate> hydratedTemplate() {
        return Optional.ofNullable(hydratedTemplate);
    }

    /**
     * Returns the business-defined identifier that links this message back to the original
     * template definition.
     *
     * @return an {@code Optional} with the template identifier, or empty if not set
     */
    public Optional<String> templateId() {
        return Optional.ofNullable(templateId);
    }

    /**
     * Returns the concrete layout selected for this template message.
     *
     * <p>Variants are checked in a fixed order: four-row, hydrated four-row, interactive.
     * If multiple variants are present on the same instance, only the first non-null one is
     * returned.
     *
     * @return an {@code Optional} with the active layout, or empty if none is set
     */
    public Optional<? extends TemplateFormat> format() {
        if (fourRowTemplate != null) return Optional.of(fourRowTemplate);
        if (hydratedFourRowTemplate != null) return Optional.of(hydratedFourRowTemplate);
        if (interactiveMessageTemplate != null) return Optional.of(interactiveMessageTemplate);
        return Optional.empty();
    }

    /**
     * Updates the contextual information attached to this template message.
     *
     * @param contextInfo the new context, or {@code null} to clear the field
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Updates the companion hydrated four-row template delivered alongside the raw template.
     *
     * @param hydratedTemplate the new hydrated template, or {@code null} to clear the field
     */
    public void setHydratedTemplate(HydratedFourRowTemplate hydratedTemplate) {
        this.hydratedTemplate = hydratedTemplate;
    }

    /**
     * Updates the business-defined template identifier.
     *
     * @param templateId the new identifier, or {@code null} to clear the field
     */
    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    /**
     * Sets the unhydrated four-row template variant, clearing the other format fields is the
     * caller's responsibility.
     *
     * @param fourRowTemplate the new variant, or {@code null} to clear the field
     */
    public void setFourRowTemplate(FourRowTemplate fourRowTemplate) {
        this.fourRowTemplate = fourRowTemplate;
    }

    /**
     * Sets the hydrated four-row template variant, clearing the other format fields is the
     * caller's responsibility.
     *
     * @param hydratedFourRowTemplate the new variant, or {@code null} to clear the field
     */
    public void setHydratedFourRowTemplate(HydratedFourRowTemplate hydratedFourRowTemplate) {
        this.hydratedFourRowTemplate = hydratedFourRowTemplate;
    }

    /**
     * Sets the interactive message variant, clearing the other format fields is the caller's
     * responsibility.
     *
     * @param interactiveMessageTemplate the new variant, or {@code null} to clear the field
     */
    public void setInteractiveMessageTemplate(InteractiveMessage interactiveMessageTemplate) {
        this.interactiveMessageTemplate = interactiveMessageTemplate;
    }

    /**
     * Enumerates the concrete title variants supported by a {@link FourRowTemplate}.
     *
     * <p>The title slot of an unhydrated template can be filled with a document preview,
     * a highly structured text template, an image, a video or a map snippet. Only one
     * variant is allowed per template instance.
     */
    public sealed interface Title permits DocumentMessage, HighlyStructuredMessage, ImageMessage, VideoMessage, LocationMessage {
    }

    /**
     * Enumerates the concrete title variants supported by a {@link HydratedFourRowTemplate}.
     *
     * <p>The hydrated title can be a document preview, a hydrated plain-text string wrapped
     * in {@link HydratedTitleText}, an image, a video or a map snippet. Only one variant is
     * allowed per template instance.
     */
    public sealed interface TitleSpec permits DocumentMessage, TitleSpec.HydratedTitleText, ImageMessage, VideoMessage, LocationMessage {

        /**
         * Wraps a plain-text title used as the header of a
         * {@link HydratedFourRowTemplate}.
         *
         * <p>This variant is serialized as a single {@code string} field by the protobuf
         * layer and is produced when the original template title is a simple text template
         * without media.
         */
        final class HydratedTitleText implements TitleSpec {
            /**
             * The literal hydrated title text.
             */
            String hydratedTitleText;

            /**
             * Constructs a new hydrated title text wrapper with the supplied value.
             *
             * @param hydratedTitleText the literal title text
             */
            HydratedTitleText(String hydratedTitleText) {
                this.hydratedTitleText = hydratedTitleText;
            }

            /**
             * Returns the literal hydrated title text.
             *
             * @return the title rendered to the recipient
             */
            @ProtobufSerializer
            public String hydratedTitleText() {
                return hydratedTitleText;
            }

            /**
             * Creates a new hydrated title text from the supplied string.
             *
             * <p>This factory is invoked by the protobuf deserialization layer when
             * materializing a hydrated template with a text title.
             *
             * @param hydratedTitleText the literal title text
             * @return a new wrapper around the title text
             */
            @ProtobufDeserializer
            public static HydratedTitleText of(String hydratedTitleText) {
                return new HydratedTitleText(hydratedTitleText);
            }
        }
    }

    /**
     * Represents the unhydrated four-row template layout.
     *
     * <p>Text fields inside this variant are {@link HighlyStructuredMessage} templates that
     * may still contain placeholders, and buttons are unhydrated {@link TemplateButton}
     * entries. Before delivery to the recipient, the server typically produces a matching
     * {@link HydratedFourRowTemplate} with placeholders resolved to literal strings.
     */
    @ProtobufMessage(name = "Message.TemplateMessage.FourRowTemplate")
    public static final class FourRowTemplate implements TemplateFormat {
        /**
         * Structured content text displayed as the main body of the template.
         */
        @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
        HighlyStructuredMessage content;

        /**
         * Structured footer text shown below the main body.
         */
        @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
        HighlyStructuredMessage footer;

        /**
         * The list of template buttons rendered under the body.
         */
        @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
        List<TemplateButton> buttons;

        /**
         * Document title variant.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        DocumentMessage documentMessage;

        /**
         * Structured text title variant.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        HighlyStructuredMessage highlyStructuredMessage;

        /**
         * Image title variant.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        ImageMessage imageMessage;

        /**
         * Video title variant.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
        VideoMessage videoMessage;

        /**
         * Location title variant.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
        LocationMessage locationMessage;


        /**
         * Constructs a new unhydrated four-row template with the supplied sections and title
         * variants.
         *
         * <p>At most one of the title variants should be non-null to keep the oneof
         * semantics well defined.
         *
         * @param content                 the structured body text, possibly {@code null}
         * @param footer                  the structured footer text, possibly {@code null}
         * @param buttons                 the list of template buttons, possibly {@code null}
         * @param documentMessage         the document title variant, possibly {@code null}
         * @param highlyStructuredMessage the structured text title variant, possibly
         *                                {@code null}
         * @param imageMessage            the image title variant, possibly {@code null}
         * @param videoMessage            the video title variant, possibly {@code null}
         * @param locationMessage         the location title variant, possibly {@code null}
         */
        FourRowTemplate(HighlyStructuredMessage content, HighlyStructuredMessage footer, List<TemplateButton> buttons, DocumentMessage documentMessage, HighlyStructuredMessage highlyStructuredMessage, ImageMessage imageMessage, VideoMessage videoMessage, LocationMessage locationMessage) {
            this.content = content;
            this.footer = footer;
            this.buttons = buttons;
            this.documentMessage = documentMessage;
            this.highlyStructuredMessage = highlyStructuredMessage;
            this.imageMessage = imageMessage;
            this.videoMessage = videoMessage;
            this.locationMessage = locationMessage;
        }

        /**
         * Returns the structured content shown as the main body of the template.
         *
         * @return an {@code Optional} with the body content, or empty if not set
         */
        public Optional<HighlyStructuredMessage> content() {
            return Optional.ofNullable(content);
        }

        /**
         * Returns the structured footer shown below the body.
         *
         * @return an {@code Optional} with the footer, or empty if not set
         */
        public Optional<HighlyStructuredMessage> footer() {
            return Optional.ofNullable(footer);
        }

        /**
         * Returns an unmodifiable view of the template buttons rendered under the body.
         *
         * @return the list of buttons, or an empty list if none were set
         */
        public List<TemplateButton> buttons() {
            return buttons == null ? List.of() : Collections.unmodifiableList(buttons);
        }

        /**
         * Returns the concrete title variant used to decorate this template.
         *
         * <p>Variants are checked in a fixed order: document, structured text, image, video,
         * location. If multiple variants are present on the same instance, only the first
         * non-null one is returned.
         *
         * @return an {@code Optional} containing the title variant, or empty if none is set
         */
        public Optional<? extends Title> title() {
            if (documentMessage != null) return Optional.of(documentMessage);
            if (highlyStructuredMessage != null) return Optional.of(highlyStructuredMessage);
            if (imageMessage != null) return Optional.of(imageMessage);
            if (videoMessage != null) return Optional.of(videoMessage);
            if (locationMessage != null) return Optional.of(locationMessage);
            return Optional.empty();
        }

        /**
         * Updates the structured body content.
         *
         * @param content the new content, or {@code null} to clear the field
         */
        public void setContent(HighlyStructuredMessage content) {
            this.content = content;
    }

        /**
         * Updates the structured footer shown below the body.
         *
         * @param footer the new footer, or {@code null} to clear the field
         */
        public void setFooter(HighlyStructuredMessage footer) {
            this.footer = footer;
    }

        /**
         * Updates the list of template buttons rendered under the body.
         *
         * @param buttons the new list of buttons, or {@code null} to clear the field
         */
        public void setButtons(List<TemplateButton> buttons) {
            this.buttons = buttons;
    }

        /**
         * Sets the document title variant, clearing the other title fields is the caller's
         * responsibility.
         *
         * @param documentMessage the new variant, or {@code null} to clear the field
         */
        public void setDocumentMessage(DocumentMessage documentMessage) {
            this.documentMessage = documentMessage;
    }

        /**
         * Sets the structured text title variant, clearing the other title fields is the
         * caller's responsibility.
         *
         * @param highlyStructuredMessage the new variant, or {@code null} to clear the field
         */
        public void setHighlyStructuredMessage(HighlyStructuredMessage highlyStructuredMessage) {
            this.highlyStructuredMessage = highlyStructuredMessage;
    }

        /**
         * Sets the image title variant, clearing the other title fields is the caller's
         * responsibility.
         *
         * @param imageMessage the new variant, or {@code null} to clear the field
         */
        public void setImageMessage(ImageMessage imageMessage) {
            this.imageMessage = imageMessage;
    }

        /**
         * Sets the video title variant, clearing the other title fields is the caller's
         * responsibility.
         *
         * @param videoMessage the new variant, or {@code null} to clear the field
         */
        public void setVideoMessage(VideoMessage videoMessage) {
            this.videoMessage = videoMessage;
    }

        /**
         * Sets the location title variant, clearing the other title fields is the caller's
         * responsibility.
         *
         * @param locationMessage the new variant, or {@code null} to clear the field
         */
        public void setLocationMessage(LocationMessage locationMessage) {
            this.locationMessage = locationMessage;
    }
    }

    /**
     * Represents the hydrated four-row template layout.
     *
     * <p>Every field is ready for immediate rendering: textual content is stored as plain
     * strings and button labels as {@link HydratedTemplateButton} entries. The
     * {@link #maskLinkedDevices()} flag hints that the template should be displayed only on
     * the primary device and hidden on linked devices.
     */
    @ProtobufMessage(name = "Message.TemplateMessage.HydratedFourRowTemplate")
    public static final class HydratedFourRowTemplate implements TemplateFormat {
        /**
         * Hydrated main body text.
         */
        @ProtobufProperty(index = 6, type = ProtobufType.STRING)
        String hydratedContentText;

        /**
         * Hydrated footer text shown below the body.
         */
        @ProtobufProperty(index = 7, type = ProtobufType.STRING)
        String hydratedFooterText;

        /**
         * Hydrated buttons rendered under the body.
         */
        @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
        List<HydratedTemplateButton> hydratedButtons;

        /**
         * Business-defined identifier linking this hydrated template back to its source
         * definition.
         */
        @ProtobufProperty(index = 9, type = ProtobufType.STRING)
        String templateId;

        /**
         * Flag requesting that the template remain hidden on linked devices.
         */
        @ProtobufProperty(index = 10, type = ProtobufType.BOOL)
        Boolean maskLinkedDevices;

        /**
         * Document title variant.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        DocumentMessage documentMessage;

        /**
         * Hydrated plain-text title variant.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String hydratedTitleText;

        /**
         * Image title variant.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        ImageMessage imageMessage;

        /**
         * Video title variant.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
        VideoMessage videoMessage;

        /**
         * Location title variant.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
        LocationMessage locationMessage;


        /**
         * Constructs a new hydrated four-row template with the supplied sections and title
         * variants.
         *
         * <p>At most one of the title variants should be non-null to keep the oneof
         * semantics well defined.
         *
         * @param hydratedContentText the hydrated body text, possibly {@code null}
         * @param hydratedFooterText  the hydrated footer text, possibly {@code null}
         * @param hydratedButtons     the list of hydrated buttons, possibly {@code null}
         * @param templateId          the business template identifier, possibly {@code null}
         * @param maskLinkedDevices   the mask-linked-devices flag, possibly {@code null}
         * @param documentMessage     the document title variant, possibly {@code null}
         * @param hydratedTitleText   the plain-text title variant, possibly {@code null}
         * @param imageMessage        the image title variant, possibly {@code null}
         * @param videoMessage        the video title variant, possibly {@code null}
         * @param locationMessage     the location title variant, possibly {@code null}
         */
        HydratedFourRowTemplate(String hydratedContentText, String hydratedFooterText, List<HydratedTemplateButton> hydratedButtons, String templateId, Boolean maskLinkedDevices, DocumentMessage documentMessage, String hydratedTitleText, ImageMessage imageMessage, VideoMessage videoMessage, LocationMessage locationMessage) {
            this.hydratedContentText = hydratedContentText;
            this.hydratedFooterText = hydratedFooterText;
            this.hydratedButtons = hydratedButtons;
            this.templateId = templateId;
            this.maskLinkedDevices = maskLinkedDevices;
            this.documentMessage = documentMessage;
            this.hydratedTitleText = hydratedTitleText;
            this.imageMessage = imageMessage;
            this.videoMessage = videoMessage;
            this.locationMessage = locationMessage;
        }

        /**
         * Returns the hydrated main body text.
         *
         * @return an {@code Optional} with the body text, or empty if not set
         */
        public Optional<String> hydratedContentText() {
            return Optional.ofNullable(hydratedContentText);
        }

        /**
         * Returns the hydrated footer text.
         *
         * @return an {@code Optional} with the footer text, or empty if not set
         */
        public Optional<String> hydratedFooterText() {
            return Optional.ofNullable(hydratedFooterText);
        }

        /**
         * Returns an unmodifiable view of the hydrated buttons rendered under the body.
         *
         * @return the list of hydrated buttons, or an empty list if none were set
         */
        public List<HydratedTemplateButton> hydratedButtons() {
            return hydratedButtons == null ? List.of() : Collections.unmodifiableList(hydratedButtons);
        }

        /**
         * Returns the business-defined identifier for this hydrated template.
         *
         * @return an {@code Optional} with the template identifier, or empty if not set
         */
        public Optional<String> templateId() {
            return Optional.ofNullable(templateId);
        }

        /**
         * Returns whether this template should be masked on linked devices.
         *
         * <p>A missing value is treated as {@code false}.
         *
         * @return {@code true} if the template is hidden on linked devices, {@code false}
         *         otherwise
         */
        public boolean maskLinkedDevices() {
            return maskLinkedDevices != null && maskLinkedDevices;
        }

        /**
         * Returns the concrete title variant used to decorate this hydrated template.
         *
         * <p>Variants are checked in a fixed order: document, hydrated text, image, video,
         * location. If multiple variants are present on the same instance, only the first
         * non-null one is returned. The plain text variant is wrapped into a dedicated
         * {@link TitleSpec.HydratedTitleText} instance on the fly.
         *
         * @return an {@code Optional} containing the title variant, or empty if none is set
         */
        public Optional<? extends TitleSpec> title() {
            if (documentMessage != null) return Optional.of(documentMessage);
            if (hydratedTitleText != null) return Optional.of(TitleSpec.HydratedTitleText.of(hydratedTitleText));
            if (imageMessage != null) return Optional.of(imageMessage);
            if (videoMessage != null) return Optional.of(videoMessage);
            if (locationMessage != null) return Optional.of(locationMessage);
            return Optional.empty();
        }

        /**
         * Updates the hydrated main body text.
         *
         * @param hydratedContentText the new body text, or {@code null} to clear the field
         */
        public void setHydratedContentText(String hydratedContentText) {
            this.hydratedContentText = hydratedContentText;
    }

        /**
         * Updates the hydrated footer text.
         *
         * @param hydratedFooterText the new footer text, or {@code null} to clear the field
         */
        public void setHydratedFooterText(String hydratedFooterText) {
            this.hydratedFooterText = hydratedFooterText;
    }

        /**
         * Updates the list of hydrated buttons rendered under the body.
         *
         * @param hydratedButtons the new list of buttons, or {@code null} to clear the field
         */
        public void setHydratedButtons(List<HydratedTemplateButton> hydratedButtons) {
            this.hydratedButtons = hydratedButtons;
    }

        /**
         * Updates the business-defined template identifier.
         *
         * @param templateId the new identifier, or {@code null} to clear the field
         */
        public void setTemplateId(String templateId) {
            this.templateId = templateId;
    }

        /**
         * Updates the mask-linked-devices flag.
         *
         * @param maskLinkedDevices the new flag value, or {@code null} to clear the field
         */
        public void setMaskLinkedDevices(Boolean maskLinkedDevices) {
            this.maskLinkedDevices = maskLinkedDevices;
    }

        /**
         * Sets the document title variant, clearing the other title fields is the caller's
         * responsibility.
         *
         * @param documentMessage the new variant, or {@code null} to clear the field
         */
        public void setDocumentMessage(DocumentMessage documentMessage) {
            this.documentMessage = documentMessage;
    }

        /**
         * Sets the hydrated plain-text title variant, clearing the other title fields is the
         * caller's responsibility.
         *
         * @param hydratedTitleText the new text, or {@code null} to clear the field
         */
        public void setHydratedTitleText(String hydratedTitleText) {
            this.hydratedTitleText = hydratedTitleText;
    }

        /**
         * Sets the image title variant, clearing the other title fields is the caller's
         * responsibility.
         *
         * @param imageMessage the new variant, or {@code null} to clear the field
         */
        public void setImageMessage(ImageMessage imageMessage) {
            this.imageMessage = imageMessage;
    }

        /**
         * Sets the video title variant, clearing the other title fields is the caller's
         * responsibility.
         *
         * @param videoMessage the new variant, or {@code null} to clear the field
         */
        public void setVideoMessage(VideoMessage videoMessage) {
            this.videoMessage = videoMessage;
    }

        /**
         * Sets the location title variant, clearing the other title fields is the caller's
         * responsibility.
         *
         * @param locationMessage the new variant, or {@code null} to clear the field
         */
        public void setLocationMessage(LocationMessage locationMessage) {
            this.locationMessage = locationMessage;
    }
    }
}
