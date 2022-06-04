package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.api.model.ProtobufMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * The constants of this enumerated type describe the various types of media type that a {@link MediaMessage} can hold
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum MediaMessageType implements ProtobufMessage {
    /**
     * The message is an image
     */
    IMAGE("image/jpeg"),

    /**
     * The message is a document
     */
    DOCUMENT("application/octet-stream"),

    /**
     * The message is an audio
     */
    AUDIO("audio/mpeg"),

    /**
     * The message is a video
     */
    VIDEO("video/mp4"),

    /**
     * The message is a sticker
     */
    STICKER("image/webp");

    /**
     * The default mime type for this enumerated type.
     * Might be right, might be wrong, who knows.
     */
    @Getter
    private final String defaultMimeType;

    /**
     * Returns the path for an encrypted url
     *
     * @return a non-null string
     */
    public String path() {
        return this == STICKER ? IMAGE.path()
                : "mms/%s".formatted(this.name().toLowerCase());
    }

    /**
     * Returns the path for an encrypted url
     *
     * @return a non-null string
     */
    public String keyName() {
        var name = (this == STICKER ? IMAGE : this).name().toLowerCase();
        return "WhatsApp %s Keys".formatted(Character.toUpperCase(name.charAt(0)) + name.substring(1));
    }
}
