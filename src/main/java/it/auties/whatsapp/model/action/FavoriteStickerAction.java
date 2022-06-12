package it.auties.whatsapp.model.action;

import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class FavoriteStickerAction implements Action {
    @ProtobufProperty(index = 1, type = STRING)
    private String directPath;

    @ProtobufProperty(index = 2, type = STRING)
    private String lastUploadTimestamp;

    @ProtobufProperty(index = 3, type = STRING)
    private String handle;

    @ProtobufProperty(index = 4, type = STRING)
    private String encFileHash;

    @ProtobufProperty(index = 5, type = STRING)
    private String stickerHashWithoutMeta;

    @ProtobufProperty(index = 6, type = STRING)
    private String mediaKey;

    @ProtobufProperty(index = 7, type = INT64)
    private long mediaKeyTimestamp;

    @ProtobufProperty(index = 8, type = BOOLEAN)
    private boolean isFavourite;

    @Override
    public String indexName() {
        return "star";
    }
}
