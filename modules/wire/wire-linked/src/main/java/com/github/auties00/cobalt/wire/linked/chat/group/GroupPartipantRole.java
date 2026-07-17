package com.github.auties00.cobalt.wire.linked.chat.group;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Arrays;
import java.util.Optional;

/**
 * Represents the administrative role a participant can hold in a WhatsApp
 * group or community.
 *
 * <p>Each participant in a group has one of three roles that determine their
 * permissions:
 * <ul>
 *   <li>{@link #MEMBER} - a regular member with no administrative privileges</li>
 *   <li>{@link #ADMIN} - an administrator who can manage group settings and
 *       participants</li>
 *   <li>{@link #FOUNDER} - the original creator of the group (also called
 *       super-admin), who has all admin privileges and cannot be demoted by
 *       other admins</li>
 * </ul>
 *
 * <p>Roles can be changed through the participant management methods in
 * {@code LinkedWhatsAppClient}. The {@link #data()} method returns the
 * protocol-level string identifier used in XMPP stanzas.
 *
 * @see GroupParticipant
 */
@ProtobufEnum(name = "GroupParticipant.Rank")
public enum GroupPartipantRole {
    /**
     * A regular group participant with no administrative privileges.
     * This is the default role for newly added members.
     */
    MEMBER(0, "member"),

    /**
     * A group administrator who can modify group settings, add and remove
     * participants, and promote or demote other members.
     */
    ADMIN(1, "admin"),

    /**
     * The original creator of the group, also known as the super-admin.
     * The founder has all administrator privileges and cannot be demoted
     * or removed by other administrators.
     */
    FOUNDER(2, "superadmin");

    /**
     * The protobuf-assigned numeric index for this role.
     */
    final int index;

    /**
     * The protocol-level string identifier for this role, or {@code null}
     * for regular users.
     */
    private final String data;

    /**
     * Constructs a {@code GroupPartipantRole} with the specified protobuf
     * index and protocol identifier.
     *
     * @param index the protobuf enum index
     * @param data  the protocol-level string identifier, or {@code null}
     */
    GroupPartipantRole(@ProtobufEnumIndex int index, String data) {
        this.index = index;
        this.data = data;
    }

    /**
     * Returns the {@code GroupPartipantRole} matching the given protocol-level
     * string identifier.
     *
     * @param input the protocol-level role identifier
     * @return the matching role constant
     */
    public static Optional<GroupPartipantRole> of(String input) {
        return Arrays.stream(values())
                .filter(entry -> entry.data().equals(input))
                .findFirst();
    }

    /**
     * Returns the protobuf-assigned numeric index for this role.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }

    /**
     * Returns the protocol-level string identifier for this role.
     *
     * <p>Returns {@code "admin"} for {@link #ADMIN}, {@code "superadmin"} for
     * {@link #FOUNDER}, and {@code null} for {@link #MEMBER}.
     *
     * @return the protocol identifier, or {@code null} for regular users
     */
    public String data() {
        return data;
    }
}