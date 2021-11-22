package it.auties.whatsapp.binary;

import lombok.*;
import lombok.experimental.Accessors;

import static it.auties.whatsapp.binary.BinaryArray.*;

@Value
@Accessors(fluent = true)
public class BinaryMessage {
    @NonNull BinaryArray raw;
    @NonNull BinaryArray decoded;
    int length;
    public BinaryMessage(@NonNull BinaryArray array) {
        this.raw = raw();
        this.length = array.cut(3).toInt();
        this.decoded = array.slice(3, length + 3);
    }

    public BinaryMessage(byte @NonNull [] array) {
        this(of(array));
    }
}
