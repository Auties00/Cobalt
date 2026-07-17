package com.github.auties00.cobalt.wire.linked.bot.plugin;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Describes how the Meta AI bot processed a document attachment, indicating
 * which content extraction method was applied to the file.
 *
 * <p>When a user sends a document (PDF, spreadsheet, text file, etc.) to the
 * Meta AI bot, the bot uses a document-processing plugin to extract content
 * from it. This metadata is included in the bot's reply to report which
 * {@link DocumentPluginType} was used, so the client can display appropriate
 * indicators or adjust its UI accordingly.
 */
@ProtobufMessage(name = "BotDocumentMessageMetadata")
public final class BotDocumentMessageMetadata {
    /**
     * The content extraction method that the bot applied to the document
     * attachment.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    DocumentPluginType pluginType;

    /**
     * Constructs a new {@code BotDocumentMessageMetadata} with the specified
     * content extraction method.
     *
     * @param pluginType the document plugin type used for extraction, or
     *                   {@code null} if unknown
     */
    BotDocumentMessageMetadata(DocumentPluginType pluginType) {
        this.pluginType = pluginType;
    }

    /**
     * Returns the content extraction method that the bot applied to the
     * document attachment.
     *
     * @return an {@link Optional} describing the plugin type, or an empty
     *         {@code Optional} if not set
     */
    public Optional<DocumentPluginType> pluginType() {
        return Optional.ofNullable(pluginType);
    }

    /**
     * Sets the content extraction method that the bot applied to the document.
     *
     * @param pluginType the new plugin type, or {@code null} to clear
     */
    public void setPluginType(DocumentPluginType pluginType) {
        this.pluginType = pluginType;
    }

    /**
     * Enumerates the content extraction methods that the Meta AI bot can apply
     * to a document attachment when processing it for a response.
     */
    @ProtobufEnum(name = "BotDocumentMessageMetadata.DocumentPluginType")
    public static enum DocumentPluginType {
        /**
         * Plain text extraction from the document (e.g. reading a PDF or
         * text file as raw text).
         */
        TEXT_EXTRACTION(0),

        /**
         * Optical character recognition (OCR) combined with image extraction,
         * used for scanned documents or image-heavy files.
         */
        OCR_AND_IMAGES(1);

        /**
         * Constructs a new document plugin type constant.
         *
         * @param index the protobuf-assigned numeric index for this constant
         */
        DocumentPluginType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf-assigned numeric index for this constant.
         */
        final int index;

        /**
         * Returns the protobuf-assigned numeric index for this constant.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }
}
