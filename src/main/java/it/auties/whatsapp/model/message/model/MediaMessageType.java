package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.media.AttachmentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * The constants of this enumerated type describe the various types of media type that a
 * {@link MediaMessage} can hold
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum MediaMessageType {
    /**
     * The message is an image
     */
    IMAGE("image/jpeg", MessageType.IMAGE, AttachmentType.IMAGE),
    /**
     * The message is a document
     */
    DOCUMENT("application/octet-stream", MessageType.DOCUMENT, AttachmentType.DOCUMENT),
    /**
     * The message is an audio
     */
    AUDIO("audio/mpeg", MessageType.AUDIO, AttachmentType.AUDIO),
    /**
     * The message is a video
     */
    VIDEO("video/mp4", MessageType.VIDEO, AttachmentType.VIDEO),
    /**
     * The message is a sticker
     */
    STICKER("image/webp", MessageType.STICKER, AttachmentType.IMAGE);

    /**
     * The default mime type for this enumerated type. Might be right, might be wrong, who knows.
     */
    @Getter
    private final String defaultMimeType;

    /**
     * The message type for this media
     */
    private final MessageType messageType;

    /**
     * The attachment type for this media
     */
    private final AttachmentType attachmentType;

    /**
     * The message type for this media
     *
     * @return a message type
     */
    public MessageType toMessageType(){
        return messageType;
    }

    /**
     * The attachment type for this media
     *
     * @return an attachment type
     */
    public AttachmentType toAttachmentType(){
        return attachmentType;
    }
}
