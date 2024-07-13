package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import static it.auties.protobuf.model.ProtobufType.*;

@ProtobufMessage(name = "StickerMetadata")
public record StickerMetadata(@ProtobufProperty(index = 1, type = STRING) String url,
                              @ProtobufProperty(index = 2, type = BYTES) byte[] fileSha256,
                              @ProtobufProperty(index = 3, type = BYTES) byte[] fileEncSha256,
                              @ProtobufProperty(index = 4, type = BYTES) byte[] mediaKey,
                              @ProtobufProperty(index = 5, type = STRING) String mimetype,
                              @ProtobufProperty(index = 6, type = UINT32) int height,
                              @ProtobufProperty(index = 7, type = UINT32) int width,
                              @ProtobufProperty(index = 8, type = STRING) String directPath,
                              @ProtobufProperty(index = 9, type = UINT64) long fileLength,
                              @ProtobufProperty(index = 10, type = FLOAT) float weight,
                              @ProtobufProperty(index = 11, type = INT64) long lastStickerSentTs) {
}
