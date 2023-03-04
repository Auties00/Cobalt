package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.business.BusinessCollection;
import it.auties.whatsapp.model.business.BusinessNativeFlow;
import it.auties.whatsapp.model.business.BusinessShop;
import it.auties.whatsapp.model.button.TemplateFormatter;
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

import java.util.Optional;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static java.util.Objects.requireNonNullElseGet;

/**
 * A model class that represents a message holding an interactive message inside. Not really clear
 * how this could be used, contributions are welcomed.
 */
@AllArgsConstructor
@NoArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true)
public final class InteractiveMessage extends ContextualMessage implements ButtonMessage, TemplateFormatter {
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
    private BusinessShop contentShop;

    /**
     * Collection message
     */
    @ProtobufProperty(index = 5, type = MESSAGE, implementation = BusinessCollection.class)
    private BusinessCollection contentCollection;

    /**
     * Native flow message
     */
    @ProtobufProperty(index = 6, type = MESSAGE, implementation = BusinessNativeFlow.class)
    private BusinessNativeFlow contentNativeFlow;

    /**
     * The context info of this message
     */
    @ProtobufProperty(index = 15, type = MESSAGE, implementation = ContextInfo.class)
    @Default
    private ContextInfo contextInfo = new ContextInfo();

    /**
     * Constructs a new builder to create an interactive message
     *
     * @param header      the header of this message
     * @param body        the body of this message
     * @param footer      the footer of this message
     * @param content     the content of this message
     * @param contextInfo the context info of this message
     * @return a non-null new message
     */
    @Builder(builderClassName = "InteractiveMessageSimpleBuilder", builderMethodName = "simpleBuilder")
    private static InteractiveMessage customBuilder(ProductHeader header, String body, String footer, InteractiveMessageContent content, ContextInfo contextInfo) {
        var builder = InteractiveMessage.builder()
                .header(header)
                .body(ProductBody.of(body))
                .footer(ProductFooter.of(footer))
                .contextInfo(requireNonNullElseGet(contextInfo, ContextInfo::new));
        switch (content){
            case BusinessShop businessShop -> builder.contentShop(businessShop);
            case BusinessCollection businessCollection -> builder.contentCollection(businessCollection);
            case BusinessNativeFlow businessNativeFlow -> builder.contentNativeFlow(businessNativeFlow);
            case null -> {}
        }
        return builder.build();
    }

    /**
     * Returns the type of content that this message wraps
     *
     * @return a non-null content type
     */
    public InteractiveMessageContentType contentType() {
        return content()
                .map(InteractiveMessageContent::contentType)
                .orElse(InteractiveMessageContentType.NONE);
    }

    /**
     * Returns the header
     *
     * @return a non-null optional
     */
    public Optional<ProductHeader> header(){
        return Optional.ofNullable(header);
    }

    /**
     * Returns the body
     *
     * @return a non-null optional
     */
    public Optional<ProductBody> body(){
        return Optional.ofNullable(body);
    }

    /**
     * Returns the footer
     *
     * @return a non-null optional
     */
    public Optional<ProductFooter> footer(){
        return Optional.ofNullable(footer);
    }

    /**
     * Returns the content of this message if it's there
     *
     * @return a non-null content type
     */
    public Optional<InteractiveMessageContent> content() {
        if (contentShop != null) {
            return Optional.of(contentShop);
        }
        if (contentCollection != null) {
            return Optional.of(contentCollection);
        }
        if (contentNativeFlow != null) {
            return Optional.of(contentNativeFlow);
        }
        return Optional.empty();
    }

    /**
     * Returns the shop content of this message if present
     *
     * @return an optional
     */
    public Optional<BusinessShop> contentShop() {
        return Optional.ofNullable(contentShop);
    }

    /**
     * Returns the collection content of this message if present
     *
     * @return an optional
     */
    public Optional<BusinessCollection> contentCollection() {
        return Optional.ofNullable(contentCollection);
    }

    /**
     * Returns the native flow content of this message if present
     *
     * @return an optional
     */
    public Optional<BusinessNativeFlow> contentNativeFlow() {
        return Optional.ofNullable(contentNativeFlow);
    }

    @Override
    public TemplateFormatterType templateType() {
        return TemplateFormatterType.INTERACTIVE;
    }

    @Override
    public MessageType type() {
        return MessageType.INTERACTIVE;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }
}