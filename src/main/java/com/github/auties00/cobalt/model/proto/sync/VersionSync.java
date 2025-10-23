package com.github.auties00.cobalt.model.proto.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessage(name = "SyncdVersion")
public record VersionSync(
        @ProtobufProperty(index = 1, type = ProtobufType.UINT64)
        Long version
) {

}
