package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessage(name = "SyncdMutation")
public record MutationSync(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        RecordSync.Operation operation,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        RecordSync record
) implements Syncable {
}