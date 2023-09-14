package it.auties.whatsapp.model.interactive;

import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.VideoOrGifMessage;

/**
 * A sealed class that describes the various types of headers
 */
public sealed interface InteractiveHeaderAttachment permits DocumentMessage, ImageMessage, InteractiveHeaderThumbnail, VideoOrGifMessage {
    InteractiveHeaderAttachmentType interactiveHeaderType();
}
