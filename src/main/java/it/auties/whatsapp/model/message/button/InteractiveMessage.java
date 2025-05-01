package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufBuilder;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.interactive.*;
import it.auties.whatsapp.model.button.base.TemplateFormatter;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;

import java.util.Optional;

/**
 * A model class that represents a message holding an interactive message inside. Not really clear
 * how this could be used, contributions are welcomed.
 */
@ProtobufMessage(name = "Message.InteractiveMessage")
public final class InteractiveMessage implements ContextualMessage<InteractiveMessage>, ButtonMessage, TemplateFormatter {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    private final InteractiveHeader header;
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    private final InteractiveBody body;
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    private final InteractiveFooter footer;
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    private final InteractiveShop contentShop;
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    private final InteractiveCollection contentCollection;
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    private final InteractiveNativeFlow contentNativeFlow;
    @ProtobufProperty(index = 15, type = ProtobufType.MESSAGE)
    private ContextInfo contextInfo;

    public InteractiveMessage(InteractiveHeader header, InteractiveBody body, InteractiveFooter footer, InteractiveShop contentShop, InteractiveCollection contentCollection, InteractiveNativeFlow contentNativeFlow, ContextInfo contextInfo) {
        this.header = header;
        this.body = body;
        this.footer = footer;
        this.contentShop = contentShop;
        this.contentCollection = contentCollection;
        this.contentNativeFlow = contentNativeFlow;
        this.contextInfo = contextInfo;
    }

    @ProtobufBuilder(className = "InteractiveMessageSimpleBuilder")
    static InteractiveMessage simpleBuilder(InteractiveHeader header, String body, String footer, InteractiveMessageContent content, ContextInfo contextInfo) {
        var builder = new InteractiveMessageBuilder()
                .header(header)
                .body(InteractiveBody.ofNullable(body).orElse(null))
                .footer(InteractiveFooter.ofNullable(footer).orElse(null))
                .contextInfo(contextInfo);
        switch (content) {
            case InteractiveShop interactiveShop -> builder.contentShop(interactiveShop);
            case InteractiveCollection interactiveCollection -> builder.contentCollection(interactiveCollection);
            case InteractiveNativeFlow interactiveNativeFlow -> builder.contentNativeFlow(interactiveNativeFlow);
            case null -> {
            }
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
        if (contentShop != null) {
            return Optional.of(contentShop);
        }

        if (contentCollection != null) {
            return Optional.of(contentCollection);
        }

        return Optional.ofNullable(contentNativeFlow);
    }

    @Override
    public Type templateType() {
        return Type.INTERACTIVE;
    }

    @Override
    public MessageType type() {
        return MessageType.INTERACTIVE;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }

    public Optional<InteractiveHeader> header() {
        return Optional.ofNullable(header);
    }

    public Optional<InteractiveBody> body() {
        return Optional.ofNullable(body);
    }

    public Optional<InteractiveFooter> footer() {
        return Optional.ofNullable(footer);
    }

    public Optional<InteractiveShop> contentShop() {
        return Optional.ofNullable(contentShop);
    }

    public Optional<InteractiveCollection> contentCollection() {
        return Optional.ofNullable(contentCollection);
    }

    public Optional<InteractiveNativeFlow> contentNativeFlow() {
        return Optional.ofNullable(contentNativeFlow);
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    @Override
    public InteractiveMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
    }

    @Override
    public String toString() {
        return "InteractiveMessage[" +
                "header=" + header + ", " +
                "body=" + body + ", " +
                "footer=" + footer + ", " +
                "contentShop=" + contentShop + ", " +
                "contentCollection=" + contentCollection + ", " +
                "contentNativeFlow=" + contentNativeFlow + ", " +
                "contextInfo=" + contextInfo + ']';
    }

}