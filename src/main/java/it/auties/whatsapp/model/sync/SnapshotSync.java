package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;

@ProtobufMessage(name = "SyncdSnapshot")
public record SnapshotSync(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        VersionSync version,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        List<RecordSync> records,
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte[] mac,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        KeyId keyId
) {

}