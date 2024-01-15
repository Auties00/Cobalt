package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;

import java.util.List;

import static it.auties.protobuf.model.ProtobufType.UINT32;

@ProtobufMessageName("Message.AppStateSyncKeyFingerprint")
public record AppStateSyncKeyFingerprint(@ProtobufProperty(index = 1, type = UINT32) Integer rawId,
                                         @ProtobufProperty(index = 2, type = UINT32) Integer currentIndex,
                                         @ProtobufProperty(index = 3, type = UINT32, packed = true) List<Integer> deviceIndexes) implements ProtobufMessage {
}
