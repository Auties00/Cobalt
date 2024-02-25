package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;

import static it.auties.protobuf.model.ProtobufType.*;

@ProtobufMessageName("Message.AppStateSyncKeyData")
public record AppStateSyncKeyData(@ProtobufProperty(index = 1, type = BYTES) byte[] keyData,
                                  @ProtobufProperty(index = 2, type = OBJECT) AppStateSyncKeyFingerprint fingerprint,
                                  @ProtobufProperty(index = 3, type = INT64) Long timestamp) implements ProtobufMessage {
}
