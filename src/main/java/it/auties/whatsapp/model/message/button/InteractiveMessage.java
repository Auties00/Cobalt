package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufBuilder;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.interactive.*;
import it.auties.whatsapp.model.button.template.TemplateFormatter;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

/**
 * A model class that represents a message holding an interactive message inside. Not really clear
 * how this could be used, contributions are welcomed.
 */
@ProtobufMessageName("Message.InteractiveMessage")
public record InteractiveMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        Optional<InteractiveHeader> header,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        Optional<InteractiveBody> body,
        @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
        Optional<InteractiveFooter> footer,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        Optional<InteractiveShop> contentShop,
        @ProtobufProperty(index = 5, type = ProtobufType.OBJECT)
        Optional<InteractiveCollection> contentCollection,
        @ProtobufProperty(index = 6, type = ProtobufType.OBJECT)
        Optional<InteractiveNativeFlow> contentNativeFlow,
        @ProtobufProperty(index = 15, type = ProtobufType.OBJECT)
        Optional<ContextInfo> contextInfo
) implements ContextualMessage, ButtonMessage, TemplateFormatter {
    @ProtobufBuilder(className = "InteractiveMessageSimpleBuilder")
    static InteractiveMessage simpleBuilder(@Nullable InteractiveHeader header, @Nullable String body, @Nullable String footer, @Nullable InteractiveMessageContent content, @Nullable ContextInfo contextInfo) {
        var builder = new InteractiveMessageBuilder()
                .header(header)
                .body(InteractiveBody.ofNullable(body))
                .footer(InteractiveFooter.ofNullable(footer))
                .contextInfo(contextInfo);
        switch (content){
            case InteractiveShop interactiveShop -> builder.contentShop(interactiveShop);
            case InteractiveCollection interactiveCollection -> builder.contentCollection(interactiveCollection);
            case InteractiveNativeFlow interactiveNativeFlow -> builder.contentNativeFlow(interactiveNativeFlow);
            case null -> {}
        }
        return builder.build();
    }

    /**
     * Returns the type of content that this message wraps
     *
     * @return a non-null content type
     */
    public InteractiveMessageContent.Type contentType() {
        return content()
                .map(InteractiveMessageContent::contentType)
                .orElse(InteractiveMessageContent.Type.NONE);
    }

    /**
     * Returns the content of this message if it's there
     *
     * @return a non-null content type
     */
    public Optional<? extends InteractiveMessageContent> content() {
        if (contentShop.isPresent()) {
            return contentShop;
        }

        if (contentCollection.isPresent()) {
            return contentCollection;
        }

        return contentNativeFlow;
    }

    @Override
    public Type templateType() {
        return TemplateFormatter.Type.INTERACTIVE;
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