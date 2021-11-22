package it.auties.whatsapp.binary;

import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;

import static it.auties.whatsapp.binary.BinaryArray.of;

/**
 * A wrapper object used to represent messages received by Whatsapp
 */
@Value
@Accessors(fluent = true)
public class BinaryMessage {
    /**
     * The raw buffer array used to construct this object
     */
    @NonNull BinaryArray raw;

    /**
     * The raw buffer array sliced at [3, {@code length})
     */
    @NonNull BinaryArray decoded;

    /**
     * The length of the decoded message
     */
    int length;

    /**
     * Constructs a new instance of this wrapper from a buffer array
     *
     * @param array the non-null buffer array
     */
    public BinaryMessage(@NonNull BinaryArray array) {
        this.raw = raw();
        this.length = array.cut(3).toInt();
        this.decoded = array.slice(3, length + 3);
    }

    /**
     * Constructs a new instance of this wrapper from an array of bytes
     *
     * @param array the non-null array of bytes
     */
    public BinaryMessage(byte @NonNull [] array) {
        this(of(array));
    }
}
