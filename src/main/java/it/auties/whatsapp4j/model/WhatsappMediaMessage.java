package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.utils.CypherUtils;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds media inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(fluent = true)
@ToString
public abstract sealed class WhatsappMediaMessage extends WhatsappUserMessage permits WhatsappImageMessage, WhatsappDocumentMessage, WhatsappAudioMessage, WhatsappVideoMessage, WhatsappStickerMessage {
    /**
     * The raw media that this message holds
     */
    protected final byte @NotNull [] media;

    /**
     * The type of media that this object wraps
     */
    protected final @NotNull WhatsappMediaMessageType type;

    /**
     * Constructs a WhatsappMediaMessage from a raw protobuf object
     *
     * @param info      the raw protobuf to wrap
     * @param condition the condition to meet
     */
    public WhatsappMediaMessage(@NotNull WhatsappProtobuf.WebMessageInfo info, @NotNull WhatsappMediaMessageType type, boolean condition) {
        super(info, condition);
        this.type = type;
        this.media = CypherUtils.mediaDecrypt(this);
    }

    /**
     * Saves the media that this message wraps to a file
     *
     * @param file the file where the data will be written, if the file doesn't exist it will be created automatically
     */
    @SneakyThrows
    public void saveMediaToFile(@NotNull File file) {
        Files.write(file.toPath(), media, StandardOpenOption.CREATE);
    }
}
