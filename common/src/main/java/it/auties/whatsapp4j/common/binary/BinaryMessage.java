package it.auties.whatsapp4j.common.binary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * An abstract model class that represents messages sent by whatsapp.
 * Everything except the constructor is up to the implementation class.
 */
@AllArgsConstructor
@Data
@Accessors(fluent = true)
public abstract class BinaryMessage {
    private final @NonNull BinaryArray raw;
}
