package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.media.AttachmentType;

/**
 * The constants of this enumerated type describe the various types of media type that a
 * {@link MediaMessage} can hold
 */
public enum MediaMessageType {
    /**
     * No media
     */
    NONE("", "", MessageType.EMPTY, AttachmentType.NONE),
    /**
     * The message is an image
     */
    IMAGE("jpg", "image/jpeg", MessageType.IMAGE, AttachmentType.IMAGE),
    /**
     * The message is a document
     */
    DOCUMENT("", "application/octet-stream", MessageType.DOCUMENT, AttachmentType.DOCUMENT),
    /**
     * The message is an audio
     */
    AUDIO("mp3", "audio/mpeg", MessageType.AUDIO, AttachmentType.AUDIO),
    /**
     * The message is a video
     */
    VIDEO("mp4", "video/mp4", MessageType.VIDEO, AttachmentType.VIDEO),
    /**
     * The message is a sticker
     */
    STICKER("webp", "image/webp", MessageType.STICKER, AttachmentType.IMAGE);

    /**
     * The default extension for this enumerated type. Might be right, might be wrong, who knows.
     */
    private final String extension;

    /**
     * The default mime type for this enumerated type. Might be right, might be wrong, who knows.
     */
    private final String mimeType;

    /**
     * The message type for this media
     */
    private final MessageType messageType;

    /**
     * The attachment type for this media
     */
    private final AttachmentType attachmentType;

    MediaMessageType(String extension, String mimeType, MessageType messageType, AttachmentType attachmentType) {
        this.extension = extension;
        this.mimeType = mimeType;
        this.messageType = messageType;
        this.attachmentType = attachmentType;
    }

    /**
     * The message type for this media
     *
     * @return a message type
     */
    public MessageType toMessageType() {
        return messageType;
    }

    /**
     * The attachment type for this media
     *
     * @return an attachment type
     */
    public AttachmentType toAttachmentType() {
        return attachmentType;
    }

    /**
     * Returns the extension of this media
     *
     * @return a string
     */
    public String extension() {
        return extension;
    }

    /**
     * Returns the mime type of this media
     *
     * @return a string
     */
    public String mimeType() {
        return this.mimeType;
    }
}
