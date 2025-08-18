package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.PatchType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * A model class that represents the deletion of a sticker from the recent list
 */
@ProtobufMessage(name = "SyncActionValue.RemoveRecentStickerAction")
public record RemoveRecentStickerAction(
        @ProtobufProperty(index = 1, type = ProtobufType.INT64)
        long lastStickerSentTimestampSeconds
) implements Action {
    /**
     * Returns when the sticker was last sent
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> lastStickerSentTimestamp() {
        return Clock.parseSeconds(lastStickerSentTimestampSeconds);
    }

    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "removeRecentSticker";
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
