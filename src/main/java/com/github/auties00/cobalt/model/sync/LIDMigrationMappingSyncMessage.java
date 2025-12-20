package com.github.auties00.cobalt.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

@ProtobufMessage(name = "LIDMigrationMappingSyncMessage")
public final class LIDMigrationMappingSyncMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    final byte[] encodedMappingPayload;

    public LIDMigrationMappingSyncMessage(byte[] encodedMappingPayload) {
        this.encodedMappingPayload = encodedMappingPayload;
    }

    public Optional<byte[]> encodedMappingPayload() {
        return Optional.ofNullable(encodedMappingPayload);
    }
}
