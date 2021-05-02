package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.utils.ProtobufUtils;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Optional;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a document inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true)
@ToString
public final class WhatsappDocumentMessage extends WhatsappMediaMessage {
    /**
     * The title of the document that this message holds
     */
    private final String title;

    /**
     * The number of pages of the document that this message holds
     */
    private final int pages;

    /**
     * Constructs a WhatsappImageMessage from a raw protobuf object
     *
     * @param info the raw protobuf to wrap
     */
    public WhatsappDocumentMessage(@NotNull WhatsappProtobuf.WebMessageInfo info) {
        super(info, WhatsappMediaMessageType.DOCUMENT, info.getMessage().hasDocumentMessage());
        var doc = info.getMessage().getDocumentMessage();
        this.title = doc.getTitle();
        this.pages = doc.getPageCount();
    }

    /**
     * Constructs a new builder to create a WhatsappMediaMessage that wraps a document.
     * The result can be later sent using {@link WhatsappAPI#sendMessage(WhatsappUserMessage)}
     *
     * @param chat          the non null chat to which the new message should belong
     * @param media         the non null image that the new message holds
     * @param mimeType      the mime type of the new message, by default {@link WhatsappMediaMessageType#defaultMimeType()}
     * @param title         the title of the document that the new message holds, by default "Document"
     * @param pages         the number of pages of the document that the new message holds, by default empty
     * @param quotedMessage the message that the new message should quote, by default empty
     * @param forwarded     whether this message is forwarded or not, by default false
     */
    @Builder(builderMethodName = "newDocumentMessage", buildMethodName = "create")
    public WhatsappDocumentMessage(@NotNull(message = "Cannot create a WhatsappMediaMessage(Document) with no chat") WhatsappChat chat, byte @NotNull(message = "Cannot create a WhatsappMediaMessage(Document) with no image") [] media, @NotNull(message = "Cannot create a WhatsappMediaMessage(Document) with no mime type") String mimeType, String title, Integer pages, WhatsappUserMessage quotedMessage, boolean forwarded) {
        this(ProtobufUtils.createMessageInfo(ProtobufUtils.createDocumentMessage(media, mimeType, title, pages, quotedMessage, forwarded), chat.jid()));
    }

    /**
     * Returns an optional String representing the title of this document
     *
     * @return a non empty optional if this message has a title
     */
    public @NotNull Optional<String> title() {
        return title.isBlank() ? Optional.empty() : Optional.of(title);
    }

    /**
     * Returns an optional Integer representing the number of pages of this document
     *
     * @return a non empty optional if this message has a number of pages defined
     */
    public @NotNull Optional<Integer> pages() {
        return pages == 0 ? Optional.empty() : Optional.of(pages);
    }

    /**
     * Returns the ContextInfo of this message if available
     *
     * @return a non empty optional if this message has a context
     */
    @Override
    public Optional<WhatsappProtobuf.ContextInfo> contextInfo() {
        return info.getMessage().getDocumentMessage().hasContextInfo() ? Optional.of(info.getMessage().getDocumentMessage().getContextInfo()) : Optional.empty();
    }
}
