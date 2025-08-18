package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.base.Button;
import it.auties.whatsapp.model.button.base.ButtonBody;
import it.auties.whatsapp.model.button.base.ButtonText;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ButtonReplyMessage;
import it.auties.whatsapp.model.message.model.MessageType;

import java.util.Optional;

/**
 * A model class that represents a message that contains a newsletters to a previous
 * {@link ButtonsMessage}
 */
@ProtobufMessage(name = "Message.ButtonsResponseMessage")
public final class ButtonsResponseMessage implements ButtonReplyMessage<ButtonsResponseMessage> {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final String buttonId;
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    private final String buttonText;
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    private ContextInfo contextInfo;
    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    private final ResponseType responseType;

    public ButtonsResponseMessage(String buttonId, String buttonText, ContextInfo contextInfo, ResponseType responseType) {
        this.buttonId = buttonId;
        this.buttonText = buttonText;
        this.contextInfo = contextInfo;
        this.responseType = responseType;
    }

    public static ButtonsResponseMessage of(ChatMessageInfo quoted, Button button) {
        return new ButtonsResponseMessageBuilder()
                .buttonId(button.id())
                .buttonText(button.bodyText().map(ButtonText::content).orElse(null))
                .contextInfo(ContextInfo.of(quoted))
                .responseType(button.bodyType() == ButtonBody.Type.TEXT ? ResponseType.SELECTED_DISPLAY_TEXT : ResponseType.UNKNOWN)
                .build();
    }

    @Override
    public MessageType type() {
        return MessageType.BUTTONS_RESPONSE;
    }

    public String buttonId() {
        return buttonId;
    }

    public Optional<String> buttonText() {
        return Optional.ofNullable(buttonText);
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    @Override
    public ButtonsResponseMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
    }

    public ResponseType responseType() {
        return responseType;
    }

    @Override
    public String toString() {
        return "ButtonsResponseMessage[" +
                "buttonId=" + buttonId + ", " +
                "buttonText=" + buttonText + ", " +
                "contextInfo=" + contextInfo + ", " +
                "responseType=" + responseType + ']';
    }


    @ProtobufEnum
    public enum ResponseType {
        UNKNOWN(0),
        SELECTED_DISPLAY_TEXT(1);

        final int index;

        ResponseType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }
}