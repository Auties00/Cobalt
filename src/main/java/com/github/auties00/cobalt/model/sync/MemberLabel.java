package com.github.auties00.cobalt.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

@ProtobufMessage(name = "MemberLabel")
public final class MemberLabel {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String label;

    @ProtobufProperty(index = 2, type = ProtobufType.INT64)
    final long labelTimestamp;

    public MemberLabel(String label, long labelTimestamp) {
        this.label = label;
        this.labelTimestamp = labelTimestamp;
    }

    public Optional<String> label() {
        return Optional.ofNullable(label);
    }

    public long labelTimestamp() {
        return labelTimestamp;
    }
}
