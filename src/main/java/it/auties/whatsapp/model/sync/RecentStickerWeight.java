package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;

import static it.auties.protobuf.model.ProtobufType.FLOAT;
import static it.auties.protobuf.model.ProtobufType.STRING;

public record RecentStickerWeight(@ProtobufProperty(index = 1, type = STRING) String filehash,
                                  @ProtobufProperty(index = 2, type = FLOAT) Float weight) implements ProtobufMessage {
}
