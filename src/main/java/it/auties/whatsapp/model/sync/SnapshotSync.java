package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;

@ProtobufMessage(name = "SyncdSnapshot")
public record SnapshotSync(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        VersionSync version,
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        List<RecordSync> records,
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte[] mac,
        @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
        KeyId keyId
) {

}