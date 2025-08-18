package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import static it.auties.protobuf.model.ProtobufType.BYTES;
import static it.auties.protobuf.model.ProtobufType.UINT32;

@ProtobufMessage(name = "PhotoChange")
public record PhotoChange(@ProtobufProperty(index = 1, type = BYTES) byte[] oldPhoto,
                          @ProtobufProperty(index = 2, type = BYTES) byte[] newPhoto,
                          @ProtobufProperty(index = 3, type = UINT32) Integer newPhotoId) {
}
