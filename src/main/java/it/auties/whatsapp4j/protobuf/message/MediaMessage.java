package it.auties.whatsapp4j.protobuf.message;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.utils.CypherUtils;
import jakarta.validation.constraints.NotNull;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds media inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
public abstract class MediaMessage implements ContextualMessage {
    private byte @NotNull [] cached;
    public byte @NotNull [] decodedMedia(){
        if(cached == null){
            this.cached = CypherUtils.mediaDecrypt(this);
        }

        return cached;
    }

    public byte @NotNull [] refreshMedia(){
        return this.cached = CypherUtils.mediaDecrypt(this);
    }

    public abstract @NotNull String url();
    public abstract @NotNull MediaMessageType type();
    public abstract byte @NotNull [] mediaKey();
    public abstract byte @NotNull [] fileSha256();
    public abstract byte @NotNull [] fileEncSha256();
    public abstract long fileLength();
}
