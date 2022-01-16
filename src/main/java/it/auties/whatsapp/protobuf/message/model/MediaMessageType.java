package it.auties.whatsapp.protobuf.message.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * The constants of this enumerated type describe the various types of media type that a {@link MediaMessage} can hold
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum MediaMessageType {
    /**
     * The message is an image
     */
    IMAGE("image/jpeg", "mms/image"),

    /**
     * The message is a document
     */
    DOCUMENT("application/octet-stream", "mms/document"),

    /**
     * The message is an audio
     */
    AUDIO("audio/mpeg", "mms/audio"),

    /**
     * The message is a video
     */
    VIDEO("video/mp4", "mms/video"),

    /**
     * The message is a sticker
     */
    STICKER("image/webp", "mms/image");


    /**
     * The default mime type for this enumerated type.
     * Might be right, might be wrong, who knows.
     */
    @Getter
    private final String defaultMimeType;

    /**
     * The path used with a Whatsapp upload host
     */
    @Getter
    private final String uploadPath;

    /**
     * Returns a name that can be interpreted by Whatsapp Web's servers
     *
     * @return a non-null string
     */
    public String whatsappName() {
        return this == STICKER ? IMAGE.whatsappName() : this.name().toLowerCase();
    }
}
