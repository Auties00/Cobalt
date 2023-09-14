package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.base.Button;
import it.auties.whatsapp.model.button.base.ButtonBodyType;
import it.auties.whatsapp.model.button.base.ButtonText;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.ButtonReplyMessage;
import it.auties.whatsapp.model.message.model.MessageType;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;

/**
 * A model class that represents a message that contains a response to a previous
 * {@link ButtonsMessage}
 */
public record ButtonsResponseMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String buttonId,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        Optional<String> buttonText,
        @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
        Optional<ContextInfo> contextInfo,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        @NonNull
        ButtonsResponseType responseType
) implements ButtonReplyMessage {
    public static ButtonsResponseMessage of(@NonNull MessageInfo quoted, @NonNull Button button) {
        return new ButtonsResponseMessageBuilder()
                .buttonId(button.id())
                .buttonText(button.bodyText().map(ButtonText::content))
                .contextInfo(ContextInfo.of(quoted))
                .responseType(button.bodyType() == ButtonBodyType.TEXT ? ButtonsResponseType.SELECTED_DISPLAY_TEXT : ButtonsResponseType.UNKNOWN)
                .build();
    }

    @Override
    public MessageType type() {
        return MessageType.BUTTONS_RESPONSE;
    }
}