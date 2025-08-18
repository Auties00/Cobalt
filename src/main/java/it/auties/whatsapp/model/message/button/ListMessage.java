package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.base.ButtonSection;
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
@ProtobufMessage(name = "Message.ListMessage")
public final class ListMessage implements ContextualMessage<ListMessage>, ButtonMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final String title;
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    private final String description;
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    private final String button;
    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    private final Type listType;
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    private final List<ButtonSection> sections;
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    private final ProductListInfo productListInfo;
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    private final String footer;
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    private ContextInfo contextInfo;

    public ListMessage(String title, String description, String button, Type listType, List<ButtonSection> sections, ProductListInfo productListInfo, String footer, ContextInfo contextInfo) {
        this.title = title;
        this.description = description;
        this.button = button;
        this.listType = listType;
        this.sections = sections;
        this.productListInfo = productListInfo;
        this.footer = footer;
        this.contextInfo = contextInfo;
    }

    @Override
    public MessageType type() {
        return MessageType.LIST;
    }

    public String title() {
        return title;
    }

    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    public String button() {
        return button;
    }

    public Type listType() {
        return listType;
    }

    public List<ButtonSection> sections() {
        return sections;
    }

    public Optional<ProductListInfo> productListInfo() {
        return Optional.ofNullable(productListInfo);
    }

    public Optional<String> footer() {
        return Optional.ofNullable(footer);
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    @Override
    public ListMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
    }

    @Override
    public String toString() {
        return "ListMessage[" +
                "title=" + title + ", " +
                "description=" + description + ", " +
                "button=" + button + ", " +
                "listType=" + listType + ", " +
                "sections=" + sections + ", " +
                "productListInfo=" + productListInfo + ", " +
                "footer=" + footer + ", " +
                "contextInfo=" + contextInfo + ']';
    }


    /**
     * The constants of this enumerated type describe the various types of {@link ListMessage}
     */
    @ProtobufEnum(name = "Message.ListMessage.Type")
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