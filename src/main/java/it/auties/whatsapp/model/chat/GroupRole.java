package it.auties.whatsapp.model.chat;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.whatsapp.api.Whatsapp;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * The constants of this enumerated type describe the various roles that a {@link GroupParticipant} can have in a group.
 * Said roles can be changed using various methods in {@link Whatsapp}.
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum GroupRole implements ProtobufMessage {
    /**
     * A participant of the group with no special powers
     */
    USER(null),

    /**
     * A participant of the group with administration powers
     */
    ADMIN("admin"),

    /**
     * The founder of the group, also known as super admin
     */
    FOUNDER("superadmin");

    /**
     * The name of the role according to Whatsapp
     */
    @Getter
    private final String data;

    /**
     * Returns a GroupRole based on a String value obtained from Whatsapp
     *
     * @param input the nullable value obtained from Whatsapp
     * @return a non-null GroupRole
     */
    public static GroupRole forData(String input) {
        return Arrays.stream(values())
                .filter(entry -> Objects.equals(entry.data(), input))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Cannot find GroupRole for %s".formatted(input)));
    }
}
