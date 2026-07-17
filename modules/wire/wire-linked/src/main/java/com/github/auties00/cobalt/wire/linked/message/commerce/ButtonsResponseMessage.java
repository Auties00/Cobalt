package com.github.auties00.cobalt.wire.linked.message.commerce;

import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveResponse;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A reply sent by the recipient of a {@link ButtonsMessage} after tapping
 * one of its quick-reply buttons.
 *
 * <p>The message echoes the identifier of the selected button so that the
 * original sender can correlate the reply with the offered options, and
 * optionally carries the display text that was shown on the tapped button
 * at the time of tapping.
 */
@ProtobufMessage(name = "Message.ButtonsResponseMessage")
public final class ButtonsResponseMessage implements ContextualMessage {
    /**
     * The identifier of the button tapped by the recipient, as originally
     * declared on the source {@link ButtonsMessage}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String selectedButtonId;

    /**
     * Contextual metadata attached to this response message, typically
     * linking back to the original {@link ButtonsMessage}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    /**
     * Declares which response variant this message carries.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    Type type;

    /**
     * The display text of the button that was tapped when the response
     * type is {@link Type#DISPLAY_TEXT}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String selectedDisplayText;


    /**
     * Constructs a new buttons response message with every field set
     * explicitly.
     *
     * <p>This constructor is package-private; callers should use the
     * generated {@code ButtonsResponseMessageBuilder} to create instances.
     *
     * @param selectedButtonId the identifier of the tapped button
     * @param contextInfo the contextual metadata of the message
     * @param type the declared response variant
     * @param selectedDisplayText the display text of the tapped button, or {@code null}
     */
    ButtonsResponseMessage(String selectedButtonId, ContextInfo contextInfo, Type type, String selectedDisplayText) {
        this.selectedButtonId = selectedButtonId;
        this.contextInfo = contextInfo;
        this.type = type;
        this.selectedDisplayText = selectedDisplayText;
    }

    /**
     * Returns the identifier of the button that was tapped by the recipient.
     *
     * @return an {@link Optional} containing the button identifier, or empty if not set
     */
    public Optional<String> selectedButtonId() {
        return Optional.ofNullable(selectedButtonId);
    }

    /**
     * Returns the contextual metadata of this response, typically linking
     * back to the {@link ButtonsMessage} that offered the buttons.
     *
     * @return an {@link Optional} containing the context info, or empty if not set
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Returns the declared response variant.
     *
     * @return an {@link Optional} containing the response type, or empty if not set
     */
    public Optional<Type> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Returns the populated response content wrapped in the shared
     * {@link InteractiveResponse} contract.
     *
     * <p>Currently only the display-text variant is defined; if the
     * display text is not set an empty {@code Optional} is returned.
     *
     * @return an {@link Optional} containing the response, or empty if not set
     */
    public Optional<? extends InteractiveResponse> response() {
        if (selectedDisplayText != null) return Optional.of(InteractiveResponse.SelectedDisplayText.of(selectedDisplayText));
        return Optional.empty();
    }

    /**
     * Sets the identifier of the tapped button.
     *
     * @param selectedButtonId the button identifier, or {@code null} to clear it
     */
    public void setSelectedButtonId(String selectedButtonId) {
        this.selectedButtonId = selectedButtonId;
    }

    /**
     * Sets the contextual metadata attached to this response.
     *
     * @param contextInfo the context info, or {@code null} to clear it
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Sets the declared response variant.
     *
     * @param type the response type, or {@code null} to clear it
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Sets the display text of the tapped button.
     *
     * @param selectedDisplayText the display text, or {@code null} to clear it
     */
    public void setSelectedDisplayText(String selectedDisplayText) {
        this.selectedDisplayText = selectedDisplayText;
    }

    /**
     * Declares the variant of a {@link ButtonsResponseMessage}.
     */
    @ProtobufEnum(name = "Message.ButtonsResponseMessage.Type")
    public static enum Type {
        /**
         * The response variant is not specified.
         */
        UNKNOWN(0),
        /**
         * The response carries the display text of the tapped button.
         */
        DISPLAY_TEXT(1);

        /**
         * Constructs a response type with the given protobuf wire index.
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
}
