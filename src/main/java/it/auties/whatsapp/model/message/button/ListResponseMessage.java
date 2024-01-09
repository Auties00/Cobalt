package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.misc.SingleSelectReplyButton;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ButtonReplyMessage;
import it.auties.whatsapp.model.message.model.MessageType;

import java.util.Optional;

/**
 * A model class that represents a message that contains a newsletters to a previous
 * {@link ListMessage}
 */
@ProtobufMessageName("Message.ListResponseMessage")
public final class ListResponseMessage implements ButtonReplyMessage<ListResponseMessage> {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final String title;
    @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
    private final SingleSelectReplyButton reply;
    @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
    private ContextInfo contextInfo;
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    private final String description;
    @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
    private final Type listType;

    public ListResponseMessage(String title, SingleSelectReplyButton reply, ContextInfo contextInfo, String description, Type listType) {
        this.title = title;
        this.reply = reply;
        this.contextInfo = contextInfo;
        this.description = description;
        this.listType = listType;
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
    @ProtobufMessageName("Message.ListResponseMessage.Type")
    public enum Type implements ProtobufEnum {
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