package it.auties.whatsapp4j.model;

import org.jetbrains.annotations.NotNull;

/**
 * The constants of this enumerated type describe the various types of media type that a {@link WhatsappMediaMessage} can hold
 */
public enum WhatsappMediaMessageType {
    /**
     * The message is an image
     */
    IMAGE,

    /**
     * The message is a document
     */
    DOCUMENT,

    /**
     * The message is an audio
     */
    AUDIO,

    /**
     * The message is a video
     */
    VIDEO,

    /**
     * The message is a sticker
     */
    STICKER;

    /**
     * Returns the type of media that a raw protobuf object holds
     *
     * @param message the message to analyze
     * @throws IllegalArgumentException if the message to analyze doesn't hold a media file
     * @return a non null enumerated type
     */
    public static @NotNull WhatsappMediaMessageType fromMessage(@NotNull WhatsappProtobuf.Message message){
        if (message.hasImageMessage()){
            return IMAGE;
        }

        if (message.hasDocumentMessage()){
            return DOCUMENT;
        }

        if (message.hasAudioMessage()){
            return AUDIO;
        }

        if (message.hasVideoMessage()){
            return VIDEO;
        }

        if (message.hasStickerMessage()){
            return STICKER;
        }

        throw new IllegalArgumentException("WhatsappAPI: Cannot deduce type of WhatsappMediaMessage");
    }

    /**
     * Returns the key used to encrypt a media message
     *
     * @return a non null array of bytes
     */
    public byte @NotNull [] key(){
        return "WhatsApp %s Keys".formatted(this == STICKER ? "Image" : this.name().charAt(0) + this.name().substring(1).toLowerCase()).getBytes();
    }
}
