package it.auties.whatsapp.model.contact;

import it.auties.protobuf.base.ProtobufMessage;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.Optional;

/**
 * The constants of this enumerated type describe the various status that a {@link Contact} can be in
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum ContactStatus implements ProtobufMessage {
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

    private static ContactStatus of(int index) {
        return Arrays.stream(values())
                .filter(entry -> entry.ordinal() == index)
                .findFirst()
                .orElse(null);
    }

    public static Optional<ContactStatus> of(@NonNull String jsonValue) {
        return Arrays.stream(values())
                .filter(entry -> entry.name()
                        .equalsIgnoreCase(jsonValue))
                .findFirst();
    }

    /**
     * Returns the name of this enumerated constant
     *
     * @return a lowercase non-null String
     */
    public String data() {
        return name().toLowerCase();
    }
}
