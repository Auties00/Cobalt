package com.github.auties00.cobalt.model.sync;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@ProtobufEnum
public enum PatchType {
    CRITICAL_BLOCK(0),
    CRITICAL_UNBLOCK_LOW(1),
    REGULAR_HIGH(2),
    REGULAR_LOW(3),
    REGULAR(4);

    private static final Map<String, PatchType> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(PatchType::toString, Function.identity()));

    final int index;
    private final byte[] bytes;

    PatchType(@ProtobufEnumIndex int index) {
        this.index = index;
        this.bytes = toString().getBytes();
    }

    public int index() {
        return index;
    }

    public static Optional<PatchType> of(String name) {
        return name == null ? Optional.empty() : Optional.ofNullable(BY_NAME.get(name));
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }

    public byte[] toBytes() {
        return bytes;
    }
}
