package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import java.util.List;

import static it.auties.protobuf.model.ProtobufType.UINT32;

@ProtobufMessage(name = "Message.AppStateSyncKeyFingerprint")
public record AppStateSyncKeyFingerprint(@ProtobufProperty(index = 1, type = UINT32) Integer rawId,
                                         @ProtobufProperty(index = 2, type = UINT32) Integer currentIndex,
                                         @ProtobufProperty(index = 3, type = UINT32, packed = true) List<Integer> deviceIndexes) {
}
