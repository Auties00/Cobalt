package it.auties.whatsapp.model.sync;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.BOOLEAN;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
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

    @ProtobufProperty(index = 7, type = BOOLEAN)
    private boolean sentByMe;
}
