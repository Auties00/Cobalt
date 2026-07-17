package com.github.auties00.cobalt.wire.linked.message.commerce;

import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveHeader;
import com.github.auties00.cobalt.wire.linked.message.location.LocationMessage;
import com.github.auties00.cobalt.wire.linked.message.media.DocumentMessage;
import com.github.auties00.cobalt.wire.linked.message.media.ImageMessage;
import com.github.auties00.cobalt.wire.linked.message.media.VideoMessage;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A message that presents the recipient with a set of tappable quick-reply
 * buttons alongside optional body text, footer text, and a rich header
 * (text, image, video, document, or location).
 *
 * <p>Buttons messages are commonly used by WhatsApp Business accounts to
 * offer the user a short list of predefined responses. When the recipient
 * taps a button, their client sends back a {@link ButtonsResponseMessage}
 * carrying the identifier of the selected button.
 *
 * <p>Only one of the header fields (text, document, image, video or
 * location) should be populated at a time; the helper {@link #header()}
 * returns the one that is set, wrapped in the shared
 * {@link InteractiveHeader} contract.
 */
@ProtobufMessage(name = "Message.ButtonsMessage")
public final class ButtonsMessage implements ContextualMessage {
    /**
     * The main body text shown above the buttons.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String contentText;

    /**
     * The footer text shown underneath the body and above the buttons.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String footerText;

    /**
     * Contextual metadata attached to this message such as quoted message
     * information, mentioned users, and forwarding details.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    /**
     * The ordered list of buttons presented to the recipient.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
    List<TemplateButtonVariant> buttons;

    /**
     * Declares which header variant this message carries, i.e. whether the
     * header is plain text, a document, an image, a video, or a location.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.ENUM)
    HeaderType headerType;

    /**
     * The plain-text header shown at the top of the message when the header
     * type is {@link HeaderType#TEXT}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String text;

    /**
     * The document rendered as the header when the header type is
     * {@link HeaderType#DOCUMENT}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    DocumentMessage documentMessage;

    /**
     * The image rendered as the header when the header type is
     * {@link HeaderType#IMAGE}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    ImageMessage imageMessage;

    /**
     * The video rendered as the header when the header type is
     * {@link HeaderType#VIDEO}.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    VideoMessage videoMessage;

    /**
     * The location rendered as the header when the header type is
     * {@link HeaderType#LOCATION}.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    LocationMessage locationMessage;


    /**
     * Constructs a new buttons message with every field set explicitly.
     *
     * <p>This constructor is package-private; callers should use the
     * generated {@code ButtonsMessageBuilder} to create instances.
     *
     * @param contentText the body text shown above the buttons
     * @param footerText the footer text shown below the body
     * @param contextInfo the contextual metadata of the message
     * @param buttons the list of buttons offered to the recipient
     * @param headerType the declared header variant
     * @param text the plain-text header, or {@code null}
     * @param documentMessage the document header, or {@code null}
     * @param imageMessage the image header, or {@code null}
     * @param videoMessage the video header, or {@code null}
     * @param locationMessage the location header, or {@code null}
     */
    ButtonsMessage(String contentText, String footerText, ContextInfo contextInfo, List<TemplateButtonVariant> buttons, HeaderType headerType, String text, DocumentMessage documentMessage, ImageMessage imageMessage, VideoMessage videoMessage, LocationMessage locationMessage) {
        this.contentText = contentText;
        this.footerText = footerText;
        this.contextInfo = contextInfo;
        this.buttons = buttons;
        this.headerType = headerType;
        this.text = text;
        this.documentMessage = documentMessage;
        this.imageMessage = imageMessage;
        this.videoMessage = videoMessage;
        this.locationMessage = locationMessage;
    }

    /**
     * Returns the body text shown above the buttons.
     *
     * @return an {@link Optional} containing the body text, or empty if not set
     */
    public Optional<String> contentText() {
        return Optional.ofNullable(contentText);
    }

    /**
     * Returns the footer text shown below the body and above the buttons.
     *
     * @return an {@link Optional} containing the footer text, or empty if not set
     */
    public Optional<String> footerText() {
        return Optional.ofNullable(footerText);
    }

    /**
     * Returns the contextual metadata of this message, such as the quoted
     * message, mentioned users, and forwarding information.
     *
     * @return an {@link Optional} containing the context info, or empty if not set
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Returns an unmodifiable view of the buttons presented to the recipient.
     *
     * @return the list of buttons, or an empty list if none were set
     */
    public List<TemplateButtonVariant> buttons() {
        return buttons == null ? List.of() : Collections.unmodifiableList(buttons);
    }

    /**
     * Returns the declared header variant.
     *
     * <p>The returned value indicates which of the mutually exclusive
     * header fields is populated.
     *
     * @return an {@link Optional} containing the header type, or empty if not set
     */
    public Optional<HeaderType> headerType() {
        return Optional.ofNullable(headerType);
    }

    /**
     * Returns the populated header wrapped in the shared
     * {@link InteractiveHeader} contract.
     *
     * <p>Only the first non-null header field is returned, tested in this
     * order: text, document, image, video, location. If no header field is
     * set an empty {@code Optional} is returned.
     *
     * @return an {@link Optional} containing the header, or empty if none is set
     */
    public Optional<? extends InteractiveHeader> header() {
        if (text != null) return Optional.of(InteractiveHeader.Text.of(text));
        if (documentMessage != null) return Optional.of(documentMessage);
        if (imageMessage != null) return Optional.of(imageMessage);
        if (videoMessage != null) return Optional.of(videoMessage);
        if (locationMessage != null) return Optional.of(locationMessage);
        return Optional.empty();
    }

    /**
     * Sets the body text shown above the buttons.
     *
     * @param contentText the body text, or {@code null} to clear it
     */
    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    /**
     * Sets the footer text shown below the body.
     *
     * @param footerText the footer text, or {@code null} to clear it
     */
    public void setFooterText(String footerText) {
        this.footerText = footerText;
    }

    /**
     * Sets the contextual metadata attached to this message.
     *
     * @param contextInfo the context info, or {@code null} to clear it
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Sets the list of buttons presented to the recipient.
     *
     * @param buttons the list of buttons, or {@code null} to clear it
     */
    public void setButtons(List<TemplateButtonVariant> buttons) {
        this.buttons = buttons;
    }

    /**
     * Sets the declared header variant.
     *
     * @param headerType the header type, or {@code null} to clear it
     */
    public void setHeaderType(HeaderType headerType) {
        this.headerType = headerType;
    }

    /**
     * Sets the plain-text header shown when the header type is
     * {@link HeaderType#TEXT}.
     *
     * @param text the plain-text header, or {@code null} to clear it
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Sets the document rendered as the header when the header type is
     * {@link HeaderType#DOCUMENT}.
     *
     * @param documentMessage the document header, or {@code null} to clear it
     */
    public void setDocumentMessage(DocumentMessage documentMessage) {
        this.documentMessage = documentMessage;
    }

    /**
     * Sets the image rendered as the header when the header type is
     * {@link HeaderType#IMAGE}.
     *
     * @param imageMessage the image header, or {@code null} to clear it
     */
    public void setImageMessage(ImageMessage imageMessage) {
        this.imageMessage = imageMessage;
    }

    /**
     * Sets the video rendered as the header when the header type is
     * {@link HeaderType#VIDEO}.
     *
     * @param videoMessage the video header, or {@code null} to clear it
     */
    public void setVideoMessage(VideoMessage videoMessage) {
        this.videoMessage = videoMessage;
    }

    /**
     * Sets the location rendered as the header when the header type is
     * {@link HeaderType#LOCATION}.
     *
     * @param locationMessage the location header, or {@code null} to clear it
     */
    public void setLocationMessage(LocationMessage locationMessage) {
        this.locationMessage = locationMessage;
    }

    /**
     * Declares the variant of header attached to a {@link ButtonsMessage}.
     *
     * <p>The value selects which of the mutually exclusive header fields on
     * the parent message is populated, allowing clients to render the
     * correct UI element above the body and buttons.
     */
    @ProtobufEnum(name = "Message.ButtonsMessage.HeaderType")
    public static enum HeaderType {
        /**
         * The header variant is not specified.
         */
        UNKNOWN(0),
        /**
         * The message has no header; only the body and footer are shown.
         */
        EMPTY(1),
        /**
         * The header is a plain-text string.
         */
        TEXT(2),
        /**
         * The header is a document attachment.
         */
        DOCUMENT(3),
        /**
         * The header is an image attachment.
         */
        IMAGE(4),
        /**
         * The header is a video attachment.
         */
        VIDEO(5),
        /**
         * The header is a location pin.
         */
        LOCATION(6);

        /**
         * Constructs a header type with the given protobuf wire index.
         *
         * @param index the protobuf wire index
         */
        HeaderType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index of this enum constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this enum constant.
         *
         * @return the protobuf wire index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * A single tappable button attached to a {@link ButtonsMessage}.
     *
     * <p>Each button carries a unique identifier that is echoed back in a
     * {@link ButtonsResponseMessage} when the recipient taps it, the text
     * rendered on the button, a type declaring how the button behaves, and
     * optional native-flow metadata for buttons that trigger a structured
     * client-side flow.
     */
    @ProtobufMessage(name = "Message.ButtonsMessage.Button")
    public static final class TemplateButtonVariant {
        /**
         * The identifier that the recipient's client echoes back when the
         * button is tapped.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String buttonId;

        /**
         * The visible text rendered on the button.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        TemplateButtonVariant.ButtonText buttonText;

        /**
         * Declares how the button behaves when tapped.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
        TemplateButtonVariant.Type type;

        /**
         * Optional native-flow metadata for buttons that launch a
         * structured client-side flow.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
        TemplateButtonVariant.NativeFlowInfo nativeFlowInfo;


        /**
         * Constructs a new button with every field set explicitly.
         *
         * <p>This constructor is package-private; callers should use the
         * generated builder.
         *
         * @param buttonId the identifier echoed back when tapped
         * @param buttonText the visible text rendered on the button
         * @param type the behavioural type of the button
         * @param nativeFlowInfo the native-flow metadata, or {@code null}
         */
        TemplateButtonVariant(String buttonId, ButtonText buttonText, Type type, NativeFlowInfo nativeFlowInfo) {
            this.buttonId = buttonId;
            this.buttonText = buttonText;
            this.type = type;
            this.nativeFlowInfo = nativeFlowInfo;
        }

        /**
         * Returns the identifier echoed back when the button is tapped.
         *
         * @return an {@link Optional} containing the button identifier, or empty if not set
         */
        public Optional<String> buttonId() {
            return Optional.ofNullable(buttonId);
        }

        /**
         * Returns the visible text rendered on the button.
         *
         * @return an {@link Optional} containing the button text, or empty if not set
         */
        public Optional<ButtonText> buttonText() {
            return Optional.ofNullable(buttonText);
        }

        /**
         * Returns the behavioural type of this button.
         *
         * @return an {@link Optional} containing the button type, or empty if not set
         */
        public Optional<Type> type() {
            return Optional.ofNullable(type);
        }

        /**
         * Returns the native-flow metadata, if any, carried by this button.
         *
         * @return an {@link Optional} containing the native-flow info, or empty if not set
         */
        public Optional<NativeFlowInfo> nativeFlowInfo() {
            return Optional.ofNullable(nativeFlowInfo);
        }

        /**
         * Sets the identifier echoed back when the button is tapped.
         *
         * @param buttonId the button identifier, or {@code null} to clear it
         */
        public void setButtonId(String buttonId) {
            this.buttonId = buttonId;
    }

        /**
         * Sets the visible text rendered on the button.
         *
         * @param buttonText the button text, or {@code null} to clear it
         */
        public void setButtonText(ButtonText buttonText) {
            this.buttonText = buttonText;
    }

        /**
         * Sets the behavioural type of this button.
         *
         * @param type the button type, or {@code null} to clear it
         */
        public void setType(Type type) {
            this.type = type;
    }

        /**
         * Sets the native-flow metadata carried by this button.
         *
         * @param nativeFlowInfo the native-flow info, or {@code null} to clear it
         */
        public void setNativeFlowInfo(NativeFlowInfo nativeFlowInfo) {
            this.nativeFlowInfo = nativeFlowInfo;
    }

        /**
         * Declares how a {@link TemplateButtonVariant} behaves when tapped
         * by the recipient.
         */
        @ProtobufEnum(name = "Message.ButtonsMessage.Button.Type")
        public static enum Type {
            /**
             * The button type is not specified.
             */
            UNKNOWN(0),
            /**
             * Tapping the button sends a plain quick-reply
             * {@link ButtonsResponseMessage} back to the sender.
             */
            RESPONSE(1),
            /**
             * Tapping the button launches a structured client-side native
             * flow described by {@link NativeFlowInfo}.
             */
            NATIVE_FLOW(2);

            /**
             * Constructs a button type with the given protobuf wire index.
             *
             * @param index the protobuf wire index
             */
            Type(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * The protobuf wire index of this enum constant.
             */
            final int index;

            /**
             * Returns the protobuf wire index of this enum constant.
             *
             * @return the protobuf wire index
             */
            public int index() {
                return this.index;
            }
        }

        /**
         * The rendered text of a {@link TemplateButtonVariant}.
         *
         * <p>This wrapper exists because the WhatsApp protocol models
         * button labels as a distinct nested message to leave room for
         * future styling fields.
         */
        @ProtobufMessage(name = "Message.ButtonsMessage.Button.ButtonText")
        public static final class ButtonText {
            /**
             * The string shown on the button.
             */
            @ProtobufProperty(index = 1, type = ProtobufType.STRING)
            String displayText;


            /**
             * Constructs a new button text with the given display string.
             *
             * <p>This constructor is package-private; callers should use
             * the generated builder.
             *
             * @param displayText the string shown on the button
             */
            ButtonText(String displayText) {
                this.displayText = displayText;
            }

            /**
             * Returns the string shown on the button.
             *
             * @return an {@link Optional} containing the display text, or empty if not set
             */
            public Optional<String> displayText() {
                return Optional.ofNullable(displayText);
            }

            /**
             * Sets the string shown on the button.
             *
             * @param displayText the display text, or {@code null} to clear it
             */
            public void setDisplayText(String displayText) {
                this.displayText = displayText;
    }
        }

        /**
         * Metadata describing a client-side native flow that is launched
         * when the user taps a {@link TemplateButtonVariant} whose type is
         * {@link Type#NATIVE_FLOW}.
         *
         * <p>The flow is identified by a name and configured by a JSON
         * payload carrying the flow-specific parameters.
         */
        @ProtobufMessage(name = "Message.ButtonsMessage.Button.NativeFlowInfo")
        public static final class NativeFlowInfo {
            /**
             * The name of the native flow to launch.
             */
            @ProtobufProperty(index = 1, type = ProtobufType.STRING)
            String name;

            /**
             * The JSON-encoded parameters passed to the native flow.
             */
            @ProtobufProperty(index = 2, type = ProtobufType.STRING)
            String paramsJson;


            /**
             * Constructs a new native-flow descriptor.
             *
             * <p>This constructor is package-private; callers should use
             * the generated builder.
             *
             * @param name the name of the native flow
             * @param paramsJson the JSON-encoded flow parameters
             */
            NativeFlowInfo(String name, String paramsJson) {
                this.name = name;
                this.paramsJson = paramsJson;
            }

            /**
             * Returns the name of the native flow to launch.
             *
             * @return an {@link Optional} containing the flow name, or empty if not set
             */
            public Optional<String> name() {
                return Optional.ofNullable(name);
            }

            /**
             * Returns the JSON-encoded parameters passed to the native flow.
             *
             * @return an {@link Optional} containing the JSON parameters, or empty if not set
             */
            public Optional<String> paramsJson() {
                return Optional.ofNullable(paramsJson);
            }

            /**
             * Sets the name of the native flow to launch.
             *
             * @param name the flow name, or {@code null} to clear it
             */
            public void setName(String name) {
                this.name = name;
    }

            /**
             * Sets the JSON-encoded parameters passed to the native flow.
             *
             * @param paramsJson the JSON parameters, or {@code null} to clear it
             */
            public void setParamsJson(String paramsJson) {
                this.paramsJson = paramsJson;
    }
        }
    }
}
