package it.auties.whatsapp.model.contact;


import java.util.Arrays;
import java.util.Optional;

/**
 * The constants of this enumerated type describe the various status that a {@link Contact} can be
 * in
 */
public enum ContactStatus {
    /**
     * When the contact is online
     */
    AVAILABLE,
    /**
     * When the contact is offline
     */
    UNAVAILABLE,
    /**
     * When the contact is writing a text message
     */
    COMPOSING,
    /**
     * When the contact is recording an audio message
     */
    RECORDING;

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
