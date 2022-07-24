package it.auties.whatsapp.model.sync;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class MutationSync implements ProtobufMessage, Syncable {
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = RecordSync.Operation.class)
    private RecordSync.Operation operation;

    @ProtobufProperty(index = 2, type = MESSAGE, concreteType = RecordSync.class)
    private RecordSync record;
}
