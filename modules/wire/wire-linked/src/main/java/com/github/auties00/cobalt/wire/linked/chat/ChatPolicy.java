package com.github.auties00.cobalt.wire.linked.chat;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Represents a permission policy for group or community actions in WhatsApp.
 *
 * <p>WhatsApp groups and communities use boolean toggles to control which
 * members can perform certain actions, such as editing the group subject,
 * changing the group description, sending messages in announcement-only
 * groups, adding new participants, or sharing the group's invite link.
 * When the toggle is on, the action is restricted to administrators; when
 * the toggle is off, every member of the group is allowed to perform it.
 * This enum provides a human-readable mapping over those boolean toggles.
 */
@ProtobufEnum
public enum ChatPolicy {
    /**
     * The action is open to every group or community member, including
     * regular participants and administrators.
     */
    ANYONE(0),

    /**
     * The action is restricted to group or community administrators only;
     * regular participants are not allowed to perform it.
     */
    ADMINS(1);

    /**
     * The protobuf-assigned numeric index for this policy.
     */
    final int index;

    /**
     * Constructs a {@code ChatPolicy} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    ChatPolicy(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code ChatPolicy} corresponding to a WhatsApp boolean toggle.
     *
     * <p>A value of {@code true} maps to {@link #ADMINS} (restricted), while
     * {@code false} maps to {@link #ANYONE} (open to all members).
     *
     * @param input the boolean toggle value from WhatsApp
     * @return {@link #ADMINS} if {@code input} is {@code true},
     *         {@link #ANYONE} otherwise
     */
    public static ChatPolicy of(boolean input) {
        return input ? ADMINS : ANYONE;
    }

    /**
     * Returns the protobuf-assigned numeric index for this policy.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }
}
