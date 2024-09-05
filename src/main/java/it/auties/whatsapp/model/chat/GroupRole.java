package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.whatsapp.api.Whatsapp;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * The constants of this enumerated type describe the various roles that a {@link GroupParticipant}
 * can have in a group. Said roles can be changed using various methods in {@link Whatsapp}.
 */
@ProtobufEnum(name = "GroupParticipant.Rank")
public enum GroupRole {
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

    final int index;
    private final String data;

    GroupRole(@ProtobufEnumIndex int index, String data) {
        this.index = index;
        this.data = data;
    }

    public static GroupRole of(String input) {
        return Arrays.stream(values())
                .filter(entry -> Objects.equals(entry.data(), input))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Cannot find GroupRole for %s".formatted(input)));
    }

    public int index() {
        return index;
    }

    public String data() {
        return data;
    }
}