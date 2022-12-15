package it.auties.whatsapp.model.sync;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("SyncdMutation")
public final class MutationSync implements ProtobufMessage , Syncable {
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = RecordSync.Operation.class)
    private RecordSync.Operation operation;

    @ProtobufProperty(index = 2, type = MESSAGE, implementation = RecordSync.class)
    private RecordSync record;
}