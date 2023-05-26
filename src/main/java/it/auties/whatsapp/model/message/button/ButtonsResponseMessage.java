package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.button.base.Button;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.ButtonReplyMessage;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.util.Validate;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that represents a message that contains a response to a previous
 * {@link ButtonsMessage}
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Jacksonized
@SuperBuilder
@Accessors(fluent = true)
public final class ButtonsResponseMessage extends ButtonReplyMessage {
    /**
     * The id of the button that was selected
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String buttonId;

    /**
     * The display text of the button that was selected
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String buttonText;

    /**
     * The context info of this message
     */
    @ProtobufProperty(index = 3, type = MESSAGE, implementation = ContextInfo.class)
    private ContextInfo contextInfo;

    /**
     * The type of the response
     */
    @ProtobufProperty(index = 4, name = "type", type = MESSAGE)
    private ResponseType responseType;

    /**
     * Constructs a response message from a buttons message and a selected button
     *
     * @param quoted the non-null message info wrapping a {@link ButtonsMessage}
     * @param button the non-null button to select
     * @return a non-null buttons response message
     */
    public static ButtonsResponseMessage of(@NonNull MessageInfo quoted, @NonNull Button button) {
        Validate.isTrue(quoted.message()
                .content() instanceof ButtonsMessage, "Cannot select buttons message, erroneous type: %s" + quoted.message()
                .content());
        return ButtonsResponseMessage.builder()
                .buttonId(button.id())
                .buttonText(button.bodyText().content())
                .contextInfo(ContextInfo.of(quoted))
                .build();
    }

    @Override
    public MessageType type() {
        return MessageType.BUTTONS_RESPONSE;
    }

    public ButtonsResponseMessage.ResponseType responseType() {
        if (buttonText != null) {
            return ButtonsResponseMessage.ResponseType.SELECTED_DISPLAY_TEXT;
        }
        return ResponseType.UNKNOWN;
    }

    @AllArgsConstructor
    public enum ResponseType implements ProtobufMessage {
        UNKNOWN(0),
        SELECTED_DISPLAY_TEXT(1);
        
        @Getter
        private final int index;
    }
}