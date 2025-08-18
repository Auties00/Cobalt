package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import java.util.List;

import static it.auties.protobuf.model.ProtobufType.MESSAGE;

@ProtobufMessage(name = "Message.AppStateSyncKeyShare")
public record AppStateSyncKeyShare(
        @ProtobufProperty(index = 1, type = MESSAGE) List<AppStateSyncKey> keys) {
}
