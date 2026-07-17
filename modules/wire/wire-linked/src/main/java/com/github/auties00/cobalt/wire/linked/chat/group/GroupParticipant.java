package com.github.auties00.cobalt.wire.linked.chat.group;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a participant in a WhatsApp group or community chat.
 *
 * <p>Each participant in a group is identified by their {@link Jid} and may
 * hold an administrative role within the group (regular user, admin, or
 * founder/super-admin). Participants may also have an optional
 * {@link GroupParticipantLabel} assigned by group administrators for
 * organizational purposes.
 *
 * <p>Instances are stored in the participant set of {@link GroupMetadata} and
 * are used throughout the group management APIs to identify who belongs to a
 * group and what their permissions are.
 *
 * @see GroupMetadata
 * @see GroupPartipantRole
 * @see GroupParticipantLabel
 */
@ProtobufMessage(name = "GroupParticipant")
public final class GroupParticipant {
    /**
     * The JID that uniquely identifies this participant. Never {@code null}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid userJid;

    /**
     * The administrative role of this participant within the group, or
     * {@code null} if the participant is a regular member with no special
     * role.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    GroupPartipantRole rank;

    /**
     * An optional label assigned to this participant by a group
     * administrator, or {@code null} if no label has been assigned.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    GroupParticipantLabel memberLabel;


    /**
     * Constructs a new {@code GroupParticipant} with the specified JID, role,
     * and label.
     *
     * @param userJid     the non-{@code null} JID identifying the participant
     * @param rank        the administrative role, or {@code null} for a
     *                    regular member
     * @param memberLabel the assigned label, or {@code null} if none
     * @throws NullPointerException if {@code userJid} is {@code null}
     */
    GroupParticipant(Jid userJid, GroupPartipantRole rank, GroupParticipantLabel memberLabel) {
        this.userJid = Objects.requireNonNull(userJid);
        this.rank = rank;
        this.memberLabel = memberLabel;
    }

    /**
     * Returns the JID that uniquely identifies this participant.
     *
     * @return the non-{@code null} participant JID
     */
    public Jid userJid() {
        return userJid;
    }

    /**
     * Returns the administrative role of this participant within the group,
     * if one has been assigned.
     *
     * <p>An empty result indicates the participant is a regular member with
     * no administrative privileges. A present value of
     * {@link GroupPartipantRole#ADMIN} indicates an administrator, and
     * {@link GroupPartipantRole#FOUNDER} indicates the group creator
     * (super-admin).
     *
     * @return an {@code Optional} containing the role, or empty for a
     *         regular member
     */
    public Optional<GroupPartipantRole> rank() {
        return Optional.ofNullable(rank);
    }

    /**
     * Returns the label assigned to this participant by a group
     * administrator, if any.
     *
     * @return an {@code Optional} containing the member label, or empty if
     *         no label has been assigned
     */
    public Optional<GroupParticipantLabel> memberLabel() {
        return Optional.ofNullable(memberLabel);
    }

    /**
     * Sets the JID that identifies this participant.
     *
     * @param userJid the participant JID to set
     */
    public void setUserJid(Jid userJid) {
        this.userJid = userJid;
    }

    /**
     * Sets the administrative role of this participant within the group.
     *
     * @param rank the role to set, or {@code null} to make the participant
     *             a regular member
     */
    public void setRank(GroupPartipantRole rank) {
        this.rank = rank;
    }

    /**
     * Sets the label assigned to this participant.
     *
     * @param memberLabel the label to set, or {@code null} to clear any
     *                    existing label
     */
    public void setMemberLabel(GroupParticipantLabel memberLabel) {
        this.memberLabel = memberLabel;
    }
}
