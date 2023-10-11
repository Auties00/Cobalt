package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.misc.ButtonSection;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.info.ProductListInfo;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageType;

import java.util.List;
import java.util.Optional;

/**
 * A model class that represents a message that contains a list of buttons or a list of products
 */
@ProtobufMessageName("Message.ListMessage")
public record ListMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String title,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        Optional<String> description,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String button,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        Type listType,
        @ProtobufProperty(index = 5, type = ProtobufType.OBJECT, repeated = true)
        List<ButtonSection> sections,
        @ProtobufProperty(index = 6, type = ProtobufType.OBJECT)
        Optional<ProductListInfo> productListInfo,
        @ProtobufProperty(index = 7, type = ProtobufType.STRING)
        Optional<String> footer,
        @ProtobufProperty(index = 8, type = ProtobufType.OBJECT)
        Optional<ContextInfo> contextInfo
) implements ContextualMessage, ButtonMessage {

    @Override
    public MessageType type() {
        return MessageType.LIST;
    }

    /**
     * The constants of this enumerated type describe the various types of {@link ListMessage}
     */
    @ProtobufMessageName("Message.ListMessage.Type")
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