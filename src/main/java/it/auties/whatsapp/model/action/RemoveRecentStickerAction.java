package it.auties.whatsapp.model.action;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.INT64;

/**
 * A model class that represents the deletion of a sticker from the recent list
 */
@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("RemoveRecentStickerAction")
public final class RemoveRecentStickerAction implements Action {
    @ProtobufProperty(index = 1, name = "lastStickerSentTs", type = INT64)
    private long lastStickerSentTimestamp;

    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "removeRecentSticker";
    }
}
