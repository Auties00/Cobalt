package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents the deletion of a sticker from the recent list
 */
@ProtobufMessage(name = "SyncActionValue.RemoveRecentStickerAction")
public final class RemoveRecentStickerAction implements Action {
    @ProtobufProperty(index = 1, type = ProtobufType.INT64)
    final long lastStickerSentTimestampSeconds;

    RemoveRecentStickerAction(long lastStickerSentTimestampSeconds) {
        this.lastStickerSentTimestampSeconds = lastStickerSentTimestampSeconds;
    }

    @Override
    public String indexName() {
        return "removeRecentSticker";
    }

    @Override
    public int actionVersion() {
        return 7;
    }

    public long lastStickerSentTimestampSeconds() {
        return lastStickerSentTimestampSeconds;
    }

    public Optional<ZonedDateTime> lastStickerSentTimestamp() {
        return Clock.parseSeconds(lastStickerSentTimestampSeconds);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RemoveRecentStickerAction that
                && lastStickerSentTimestampSeconds == that.lastStickerSentTimestampSeconds;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(lastStickerSentTimestampSeconds);
    }

    @Override
    public String toString() {
        return "RemoveRecentStickerAction[" +
                "lastStickerSentTimestampSeconds=" + lastStickerSentTimestampSeconds + ']';
    }
}
