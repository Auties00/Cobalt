package it.auties.whatsapp4j.binary.message;

import it.auties.whatsapp4j.binary.model.BinaryArray;
import it.auties.whatsapp4j.binary.model.BinaryMessage;
import lombok.NonNull;

public record StandardBinaryMessage(@NonNull String tag, @NonNull BinaryArray message) implements BinaryMessage {
    public static char DIVIDER = ',';
}
