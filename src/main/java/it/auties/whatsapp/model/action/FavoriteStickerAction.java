package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.PatchType;
import it.auties.whatsapp.util.Clock;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * A model clas that represents a favourite sticker
 */
public record FavoriteStickerAction(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String directPath,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        @NonNull
        String lastUploadTimestamp,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        @NonNull
        String handle,
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        @NonNull
        String encFileHash,
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        @NonNull
        String stickerHashWithoutMeta,
        @ProtobufProperty(index = 6, type = ProtobufType.STRING)
        @NonNull
        String mediaKey,
        @ProtobufProperty(index = 7, type = ProtobufType.INT64)
        long mediaKeyTimestampSeconds,
        @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
        boolean favourite
) implements Action {
    /**
     * Returns the media key's timestampSeconds
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> mediaKeyTimestamp() {
        return Clock.parseSeconds(mediaKeyTimestampSeconds);
    }

    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "favoriteSticker";
    }

    /**
     * The version of this action
     *
     * @return a non-null string
     */
    @Override
    public int actionVersion() {
        return 7;
    }

    /**
     * The type of this action
     *
     * @return a non-null string
     */
    @Override
    public PatchType actionType() {
        return null;
    }
}
