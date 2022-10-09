package it.auties.whatsapp.model.message.standard;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.business.BusinessCollection;
import it.auties.whatsapp.model.business.BusinessNativeFlow;
import it.auties.whatsapp.model.business.BusinessShop;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.product.ProductBody;
import it.auties.whatsapp.model.product.ProductFooter;
import it.auties.whatsapp.model.product.ProductHeader;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static java.util.Objects.requireNonNullElseGet;

/**
 * A model class that represents a message holding an interactive message inside.
 * Not really clear how this could be used, contributions are welcomed.
 */
@AllArgsConstructor
@Data
@Builder(builderMethodName = "newRawInteractiveMessageBuilder")
@Jacksonized
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true)
public final class InteractiveMessage extends ContextualMessage implements ButtonMessage {
    /**
     * Product header
     */
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = ProductHeader.class)
    private ProductHeader header;

    /**
     * Product body
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = ProductBody.class)
    private ProductBody body;

    /**
     * Product footer
     */
    @ProtobufProperty(index = 3, type = MESSAGE, implementation = ProductFooter.class)
    private ProductFooter footer;

    /**
     * Shop store message
     */
    @ProtobufProperty(index = 4, type = MESSAGE, implementation = BusinessShop.class)
    private BusinessShop shopContent;

    /**
     * Collection message
     */
    @ProtobufProperty(index = 5, type = MESSAGE, implementation = BusinessCollection.class)
    private BusinessCollection collectionContent;

    /**
     * Native flow message
     */
    @ProtobufProperty(index = 6, type = MESSAGE, implementation = BusinessNativeFlow.class)
    private BusinessNativeFlow nativeFlowContent;

    /**
     * The context info of this message
     */
    @ProtobufProperty(index = 15, type = MESSAGE, implementation = ContextInfo.class)
    @Default
    private ContextInfo contextInfo = new ContextInfo();  // Overrides ContextualMessage's context info

    /**
     * Constructs a new builder to create an interactive message with a shop
     *
     * @param header      the header of this message
     * @param body        the body of this message
     * @param footer      the footer of this message
     * @param content     the content of this message
     * @param contextInfo the context info of this message
     * @return a non-null new message
     */
    @Builder(builderClassName = "ShopInteractiveMessageBuilder", builderMethodName = "newInteractiveWithShopMessageBuilder")
    private static InteractiveMessage shopBuilder(ProductHeader header, String body, String footer,
                                                  BusinessShop content, ContextInfo contextInfo) {
        return InteractiveMessage.newRawInteractiveMessageBuilder()
                .header(header)
                .body(ProductBody.of(body))
                .footer(ProductFooter.of(footer))
                .shopContent(content)
                .contextInfo(requireNonNullElseGet(contextInfo, ContextInfo::new))
                .build();
    }

    /**
     * Constructs a new builder to create an interactive message with a collection
     *
     * @param header      the header of this message
     * @param body        the body of this message
     * @param footer      the footer of this message
     * @param content     the content of this message
     * @param contextInfo the context info of this message
     * @return a non-null new message
     */
    @Builder(builderClassName = "CollectionInteractiveMessageBuilder", builderMethodName = "newInteractiveWithCollectionMessageBuilder")
    private static InteractiveMessage collectionBuilder(ProductHeader header, String body, String footer,
                                                        BusinessCollection content, ContextInfo contextInfo) {
        return InteractiveMessage.newRawInteractiveMessageBuilder()
                .header(header)
                .body(ProductBody.of(body))
                .footer(ProductFooter.of(footer))
                .collectionContent(content)
                .contextInfo(requireNonNullElseGet(contextInfo, ContextInfo::new))
                .build();
    }

    /**
     * Constructs a new builder to create an interactive message with a native flow
     *
     * @param header      the header of this message
     * @param body        the body of this message
     * @param footer      the footer of this message
     * @param content     the content of this message
     * @param contextInfo the context info of this message
     * @return a non-null new message
     */
    @Builder(builderClassName = "NativeFlowInteractiveMessageBuilder", builderMethodName = "newInteractiveWithNativeFlowMessageBuilder")
    private static InteractiveMessage nativeFlowBuilder(ProductHeader header, String body, String footer,
                                                        BusinessNativeFlow content, ContextInfo contextInfo) {
        return InteractiveMessage.newRawInteractiveMessageBuilder()
                .header(header)
                .body(ProductBody.of(body))
                .footer(ProductFooter.of(footer))
                .nativeFlowContent(content)
                .contextInfo(requireNonNullElseGet(contextInfo, ContextInfo::new))
                .build();
    }

    /**
     * Returns the type of content that this message wraps
     *
     * @return a non-null content type
     */
    public ContentType contentType() {
        if (shopContent != null)
            return ContentType.SHOP_MESSAGE;
        if (collectionContent != null)
            return ContentType.COLLECTION_MESSAGE;
        if (nativeFlowContent != null)
            return ContentType.NATIVE_FLOW_MESSAGE;
        return ContentType.NONE;
    }

    @Override
    public MessageType type() {
        return MessageType.INTERACTIVE;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }

    /**
     * The constants of this enumerated type describe the various types of content that an interactive message can wrap
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum ContentType implements ProtobufMessage {
        /**
         * No content
         */
        NONE(0),

        /**
         * Shop
         */
        SHOP_MESSAGE(1),

        /**
         * Collection
         */
        COLLECTION_MESSAGE(2),

        /**
         * Native flow
         */
        NATIVE_FLOW_MESSAGE(3);

        @Getter
        private final int index;

        @JsonCreator
        public static ContentType forIndex(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(NONE);
        }
    }
}
