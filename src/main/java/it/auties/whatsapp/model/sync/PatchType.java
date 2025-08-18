package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Arrays;
import java.util.Locale;
import java.util.NoSuchElementException;

@ProtobufEnum
public enum PatchType {
    CRITICAL_BLOCK(0),
    CRITICAL_UNBLOCK_LOW(1),
    REGULAR_HIGH(2),
    REGULAR_LOW(3),
    REGULAR(4);

    final int index;

    PatchType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    public static PatchType of(String name) {
        return Arrays.stream(values())
                .filter(entry -> entry.toString().equals(name))
                .findAny()
                .orElseThrow(() -> new NoSuchElementException("No sync matches %s".formatted(name)));
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
