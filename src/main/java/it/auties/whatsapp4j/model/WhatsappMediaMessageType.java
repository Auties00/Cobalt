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
     * The URL used to build the media request
     */
    private static final String WHATSAPP_URL = "https://media-mxp1-1.cdn.whatsapp.net/mms/%s";

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
        var name = whatsappName();
        return "WhatsApp %s Keys".formatted(Character.toUpperCase(name.charAt(0)) + name.substring(1)).getBytes();
    }


    /**
     * Returns the URL used to upload this enumerated type to Whatsapp Web's servers
     *
     * @return a non null string
     */
    public @NotNull String url(){
        return WHATSAPP_URL.formatted(whatsappName());
    }


    /**
     * Returns a name that can be interpreted by Whatsapp Web's servers
     *
     * @return a non null string
     */
    public @NotNull String whatsappName(){
        return this == STICKER ? IMAGE.whatsappName() : this.name().toLowerCase();
    }

    /**
     * Returns whether this enumerated type is streamable
     *
     * @return true if streamable
     */
    public boolean isStreamable(){
        return this == AUDIO || this == VIDEO;
    }
}
