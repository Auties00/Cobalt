package it.auties.whatsapp.model.contact;


import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Arrays;
import java.util.Optional;

/**
 * The constants of this enumerated type describe the various status that a {@link Contact} can be
 * in
 */
@ProtobufEnum
public enum ContactStatus {
    /**
     * When the contact is online
     */
    AVAILABLE(0),

    /**
     * When the contact is offline
     */
    UNAVAILABLE(1),

    /**
     * When the contact is writing a text message
     */
    COMPOSING(2),

    /**
     * When the contact is recording an audio message
     */
    RECORDING(3);

    final int index;

    ContactStatus(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    public static Optional<ContactStatus> of(String name) {
        return Arrays.stream(values())
                .filter(entry -> entry.name().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
