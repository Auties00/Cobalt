package it.auties.whatsapp.model.sync;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

import static it.auties.protobuf.base.ProtobufType.BYTES;
import static it.auties.protobuf.base.ProtobufType.MESSAGE;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class SnapshotSync implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = VersionSync.class)
    private VersionSync version;

    @ProtobufProperty(index = 2, type = MESSAGE, implementation = RecordSync.class, repeated = true)
    private List<RecordSync> records;

    @ProtobufProperty(index = 3, type = BYTES)
    private byte[] mac;

    @ProtobufProperty(index = 4, type = MESSAGE, implementation = KeyId.class)
    private KeyId keyId;

    public static class SnapshotSyncBuilder {
        public SnapshotSyncBuilder records(List<RecordSync> records) {
            if (this.records == null)
                this.records = new ArrayList<>();
            this.records.addAll(records);
            return this;
        }
    }
}