package it.auties.whatsapp.model.sync;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("StickerMetadata")
public class RecentStickerMetadata implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = STRING)
    private String mediaDirectPath;

    @ProtobufProperty(index = 2, type = STRING)
    private String encodedFileHash;

    @ProtobufProperty(index = 3, type = STRING)
    private String mediaKey;

    @ProtobufProperty(index = 4, type = STRING)
    private String stanzaId;

    @ProtobufProperty(index = 5, type = STRING)
    private String chatJid;

    @ProtobufProperty(index = 6, type = STRING)
    private String participant;

    @ProtobufProperty(index = 7, type = BOOL)
    private boolean sentByMe;

    @ProtobufProperty(index = 8, name = "directPath", type = STRING)
    private String directPath;

    @ProtobufProperty(index = 9, name = "fileLength", type = UINT64)
    private Long fileLength;

    @ProtobufProperty(index = 10, name = "weight", type = FLOAT)
    private Float weight;

    @ProtobufProperty(index = 11, name = "lastStickerSentTs", type = INT64)
    private Long lastStickerSentTs;
}