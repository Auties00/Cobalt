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
public record ListResponseMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
                String title,
        @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
                SingleSelectReplyButton reply,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        Optional<ContextInfo> contextInfo,
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        Optional<String> description,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
                Type listType
) implements ButtonReplyMessage {
    @Override
    public MessageType type() {
        return MessageType.LIST_RESPONSE;
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