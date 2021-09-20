package it.auties.whatsapp4j.common.binary;

import lombok.NonNull;

/**
 * An abstract model class that represents messages sent by whatsapp.
 * Everything except the constructor is up to the implementation class.
 */
public abstract class BinaryMessage {
    /**
     * Constructs a new BinaryMessage from a {@code array}
     *
     * @param array the binary array that represents a message
     */
    public BinaryMessage(@NonNull BinaryArray array){

    }
}
