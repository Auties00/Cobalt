package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import static it.auties.protobuf.model.ProtobufType.*;

@ProtobufMessage(name = "Message.AppStateSyncKeyData")
public record AppStateSyncKeyData(@ProtobufProperty(index = 1, type = BYTES) byte[] keyData,
                                  @ProtobufProperty(index = 2, type = MESSAGE) AppStateSyncKeyFingerprint fingerprint,
                                  @ProtobufProperty(index = 3, type = INT64) Long timestamp) {
}
