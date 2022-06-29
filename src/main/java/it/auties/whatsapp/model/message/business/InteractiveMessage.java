package it.auties.whatsapp.model.message.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.BusinessMessage;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.product.ProductBody;
import it.auties.whatsapp.model.product.ProductFooter;
import it.auties.whatsapp.model.product.ProductHeader;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

/**
 * A model class that represents a message holding an interactive message inside
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true)
public final class InteractiveMessage extends ContextualMessage implements BusinessMessage {
    /**
     * Product header
     */
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = ProductHeader.class)
    private ProductHeader header;

    /**
     * Product body
     */
    @ProtobufProperty(index = 2, type = MESSAGE, concreteType = ProductBody.class)
    private ProductBody body;

    /**
     * Product footer
     */
    @ProtobufProperty(index = 3, type = MESSAGE, concreteType = ProductFooter.class)
    private ProductFooter footer;

    /**
     * Shop store message
     */
    @ProtobufProperty(index = 4, type = MESSAGE, concreteType = ShopMessage.class)
    private ShopMessage shopStoreFrontMessage;

    /**
     * Collection message
     */
    @ProtobufProperty(index = 5, type = MESSAGE, concreteType = CollectionMessage.class)
    private CollectionMessage collectionMessage;

    /**
     * Native flow message
     */
    @ProtobufProperty(index = 6, type = MESSAGE, concreteType = NativeFlowMessage.class)
    private NativeFlowMessage nativeFlowMessage;

    /**
     * The context info of this message
     */
    @ProtobufProperty(index = 15, type = MESSAGE, concreteType = ContextInfo.class)
    @Builder.Default
    private ContextInfo contextInfo = new ContextInfo();  // Overrides ContextualMessage's context info

    public InteractiveMessageType type() {
        if (shopStoreFrontMessage != null)
            return InteractiveMessageType.SHOP_MESSAGE;
        if (collectionMessage != null)
            return InteractiveMessageType.COLLECTION_MESSAGE;
        if (nativeFlowMessage != null)
            return InteractiveMessageType.NATIVE_FLOW_MESSAGE;
        return InteractiveMessageType.UNKNOWN;
    }

    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum InteractiveMessageType implements ProtobufMessage {
        UNKNOWN(0),
        SHOP_MESSAGE(1),
        COLLECTION_MESSAGE(2),
        NATIVE_FLOW_MESSAGE(3);

        @Getter
        private final int index;

        @JsonCreator
        public static InteractiveMessageType forIndex(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(UNKNOWN);
        }
    }
}
