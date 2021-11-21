package it.auties.whatsapp.binary;

import lombok.*;
import lombok.experimental.Accessors;

import static it.auties.whatsapp.binary.BinaryArray.*;

@AllArgsConstructor
@Value
@Accessors(fluent = true)
public class BinaryMessage {
    @NonNull BinaryArray raw;
    @NonNull BinaryArray decoded;
    int length;
    public BinaryMessage(@NonNull BinaryArray array) {
        this(array, array.slice(3), array.cut(3).toInt());
    }

    public BinaryMessage(byte @NonNull [] array) {
        this(of(array));
    }
}
