package it.auties.whatsapp.model.contact;

import it.auties.protobuf.base.ProtobufMessage;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.Arrays;

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
    RECORDING,

    /**
     * When the contact stops writing or recording
     */
    PAUSED;

    private static ContactStatus of(int index) {
        return Arrays.stream(values())
                .filter(entry -> entry.ordinal() == index)
                .findFirst()
                .orElse(null);
    }

    public static ContactStatus of(@NonNull String jsonValue) {
        return Arrays.stream(values())
                .filter(entry -> entry.name()
                        .equalsIgnoreCase(jsonValue))
                .findFirst()
                .orElseThrow();
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
