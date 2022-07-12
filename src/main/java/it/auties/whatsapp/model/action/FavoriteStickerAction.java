package it.auties.whatsapp.model.action;

import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

/**
 * A model clas that represents a favourite sticker
 */
@AllArgsConstructor
@Data
@Builder(builderMethodName = "newFavoriteStickerActionBuilder")
@Jacksonized
@Accessors(fluent = true)
public final class FavoriteStickerAction implements Action {
    /**
     * The direct path to the sticker
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String directPath;

    /**
     * The last upload timestamp
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String lastUploadTimestamp;

    /**
     * The handle of the sticker
     */
    @ProtobufProperty(index = 3, type = STRING)
    private String handle;

    /**
     * The hash of the sticker
     */
    @ProtobufProperty(index = 4, type = STRING)
    private String encFileHash;

    /**
     * The hash of the sticker without metadata
     */
    @ProtobufProperty(index = 5, type = STRING)
    private String stickerHashWithoutMeta;

    /**
     * The media key of the sticker
     */
    @ProtobufProperty(index = 6, type = STRING)
    private String mediaKey;

    /**
     * The timestamp of the media key of the sticker
     */
    @ProtobufProperty(index = 7, type = INT64)
    private long mediaKeyTimestamp;

    /**
     * Whether the sticker should be marked as favourite or not
     */
    @ProtobufProperty(index = 8, type = BOOLEAN)
    private boolean favourite;

    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "star";
    }
}
