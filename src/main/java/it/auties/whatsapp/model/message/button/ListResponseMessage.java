package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.base.SingleSelectReplyButton;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ButtonReplyMessage;
import it.auties.whatsapp.model.message.model.MessageType;

import java.util.Optional;

/**
 * A model class that represents a message that contains a newsletters to a previous
 * {@link ListMessage}
 */
@ProtobufMessage(name = "Message.ListResponseMessage")
public final class ListResponseMessage implements ButtonReplyMessage<ListResponseMessage> {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final String title;
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    private final Type listType;
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    private final SingleSelectReplyButton reply;
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    private ContextInfo contextInfo;
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    private final String description;

    public ListResponseMessage(String title, Type listType, SingleSelectReplyButton reply, ContextInfo contextInfo, String description) {
        this.title = title;
        this.listType = listType;
        this.reply = reply;
        this.contextInfo = contextInfo;
        this.description = description;
    }

    @Override
    public MessageType type() {
        return MessageType.LIST_RESPONSE;
    }

    public String title() {
        return title;
    }

    public SingleSelectReplyButton reply() {
        return reply;
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    @Override
    public ListResponseMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
    }

    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    public Type listType() {
        return listType;
    }

    @Override
    public String toString() {
        return "ListResponseMessage[" +
                "title=" + title + ", " +
                "reply=" + reply + ", " +
                "contextInfo=" + contextInfo + ", " +
                "description=" + description + ", " +
                "listType=" + listType + ']';
    }


    /**
     * The constants of this enumerated type describe the various types of {@link ListMessage}
     */
    @ProtobufEnum(name = "Message.ListResponseMessage.Type")
    public enum Type {
        /**
         * Unknown
         */
        UNKNOWN(0),
        /**
         * Only one option can be selected
         */
        SINGLE_SELECT(1),
        /**
         * A list of products
         */
        PRODUCT_LIST(2);

        final int index;

        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }
}