package it.auties.whatsapp4j.protobuf.message.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.message.standard.*;
import it.auties.whatsapp4j.utils.internal.CypherUtils;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds media inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(buildMethodName = "create")
public abstract sealed class MediaMessage extends ContextualMessage permits AudioMessage, DocumentMessage, ImageMessage, StickerMessage, VideoMessage {
    /**
     * The cached decoded media, by default null
     */
    private byte[] decodedMedia;

    /**
     * Returns the cached decoded media wrapped by this object if available.
     * Otherwise the encoded media that this object wraps is decoded, cached and returned.
     *
     * @return a non null array of bytes
     */
    public byte @NotNull [] decodedMedia(){
        if(decodedMedia == null){
            this.decodedMedia = CypherUtils.mediaDecrypt(this);
        }

        return decodedMedia;
    }

    /**
     * Decodes the encoded media that this object wraps, caches it and returns the decoded media.
     *
     * @return a non null array of bytes
     */
    public byte @NotNull [] refreshMedia(){
        return this.decodedMedia = CypherUtils.mediaDecrypt(this);
    }

    /**
     * Returns the upload url of the encoded media that this object wraps
     *
     * @return a non null string
     */
    public abstract @NotNull String url();

    /**
     * Returns the direct path to the encoded media that this object wraps
     *
     * @return a non null string
     */
    public abstract @NotNull String directPath();


    /**
     * Returns the media type of the media that this object wraps
     *
     * @return a non null {@link MediaMessageType}
     */
    public abstract @NotNull MediaMessageType type();


    /**
     * Returns the media key of the media that this object wraps
     *
     * @return a non null array of bytes
     */
    public abstract byte @NotNull [] mediaKey();

    /**
     * Returns the timestamp, that is the seconds elapsed since {@link java.time.Instant#EPOCH}, for {@link MediaMessage#mediaKey()}
     *
     * @return an unsigned long
     */
    public abstract long mediaKeyTimestamp();


    /**
     * Returns the sha256 of the decoded media that this object wraps
     *
     * @return a non null array of bytes
     */
    public abstract byte @NotNull [] fileSha256();


    /**
     * Returns the sha256 of the encoded media that this object wraps
     *
     * @return a non null array of bytes
     */
    public abstract byte @NotNull [] fileEncSha256();


    /**
     * Returns the size of the decoded media that this object wraps
     *
     * @return an unsigned int
     */
    public abstract long fileLength();
}
