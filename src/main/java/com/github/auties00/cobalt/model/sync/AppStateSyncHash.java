package com.github.auties00.cobalt.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessage
public final class AppStateSyncHash implements Cloneable{
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    final PatchType type;

    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    long version;

    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] hash;

    AppStateSyncHash(PatchType type, long version, byte[] hash) {
        this.type = type;
        this.version = version;
        this.hash = hash;
    }

    public AppStateSyncHash(PatchType type) {
        this.type = type;
        this.version = 0;
        this.hash = new byte[128];
    }

    public PatchType type() {
        return type;
    }

    public long version() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public byte[] hash() {
        return hash;
    }

    public void setHash(byte[] hash) {
        this.hash = hash;
    }

    @Override
    public AppStateSyncHash clone() {
        return new AppStateSyncHash(type, version, hash.clone());
    }
}
