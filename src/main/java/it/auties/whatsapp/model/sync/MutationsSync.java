package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;

@ProtobufMessage(name = "SyncdMutations")
public record MutationsSync(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        List<MutationSync> mutations
) {

}