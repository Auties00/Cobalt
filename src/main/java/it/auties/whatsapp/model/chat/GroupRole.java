package it.auties.whatsapp.model.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
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
@ProtobufName("Rank")
public enum GroupRole
        implements ProtobufMessage {
    /**
     * A participant of the group with no special powers
     */
    USER(0, null),

    /**
     * A participant of the group with administration powers
     */
    ADMIN(1, "admin"),

    /**
     * The founder of the group, also known as super admin
     */
    FOUNDER(2, "superadmin");

    /**
     * The name of the role according to Whatsapp
     */
    @Getter
    private final int index;

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
    public static GroupRole of(String input) {
        return Arrays.stream(values())
                .filter(entry -> Objects.equals(entry.data(), input))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Cannot find GroupRole for %s".formatted(input)));
    }

    @JsonCreator
    public static GroupRole of(int index) {
        return Arrays.stream(values())
                .filter(entry -> entry.index() == index)
                .findFirst()
                .orElse(null);
    }
}
