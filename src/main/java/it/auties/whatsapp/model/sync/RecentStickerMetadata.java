package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;

import static it.auties.protobuf.model.ProtobufType.BOOL;
import static it.auties.protobuf.model.ProtobufType.STRING;

public record RecentStickerMetadata(@ProtobufProperty(index = 1, type = STRING) String directPath,
                                    @ProtobufProperty(index = 2, type = STRING) String encFileHash,
                                    @ProtobufProperty(index = 3, type = STRING) String mediaKey,
                                    @ProtobufProperty(index = 4, type = STRING) String stanzaId,
                                    @ProtobufProperty(index = 5, type = STRING) String chatJid,
                                    @ProtobufProperty(index = 6, type = STRING) String participant,
                                    @ProtobufProperty(index = 7, type = BOOL) boolean isSentByMe) implements ProtobufMessage {
}
