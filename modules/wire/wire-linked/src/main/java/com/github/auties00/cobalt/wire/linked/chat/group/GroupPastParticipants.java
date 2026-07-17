package com.github.auties00.cobalt.wire.linked.chat.group;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Contains the list of past participants for a specific WhatsApp group.
 *
 * <p>This class associates a group's JID with its collection of
 * {@link GroupPastParticipant} records, each representing a user who
 * previously left or was removed from the group. The records are typically
 * ordered by departure time (most recent first) and are subject to
 * expiration.
 *
 * <p>Past participant data is synced during history sync and updated as
 * participants leave or are removed from the group. The data powers
 * features like the group member updates panel, which shows recent
 * membership changes.
 *
 * @see GroupPastParticipant
 * @see GroupMetadata
 */
@ProtobufMessage(name = "PastParticipants")
public final class GroupPastParticipants {
    /**
     * The JID of the group these past participants belong to, or
     * {@code null} if not available.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid groupJid;

    /**
     * The list of past participant records for this group. May be
     * {@code null}, in which case an empty list is returned by the
     * accessor.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<GroupPastParticipant> pastParticipants;


    /**
     * Constructs a new {@code GroupPastParticipants} for the specified group
     * with the given list of past participant records.
     *
     * @param groupJid         the group JID, or {@code null}
     * @param pastParticipants the list of past participants, or {@code null}
     */
    GroupPastParticipants(Jid groupJid, List<GroupPastParticipant> pastParticipants) {
        this.groupJid = groupJid;
        this.pastParticipants = pastParticipants;
    }

    /**
     * Returns the JID of the group these past participants belong to, if
     * available.
     *
     * @return an {@code Optional} containing the group JID, or empty if not
     *         available
     */
    public Optional<Jid> groupJid() {
        return Optional.ofNullable(groupJid);
    }

    /**
     * Returns an unmodifiable view of the past participant records for this
     * group.
     *
     * <p>If no past participants have been recorded, an empty list is
     * returned.
     *
     * @return an unmodifiable list of past participant records, never
     *         {@code null}
     */
    public List<GroupPastParticipant> pastParticipants() {
        return pastParticipants == null ? List.of() : Collections.unmodifiableList(pastParticipants);
    }

    /**
     * Sets the JID of the group these past participants belong to.
     *
     * @param groupJid the group JID to set, or {@code null} to clear
     */
    public void setGroupJid(Jid groupJid) {
        this.groupJid = groupJid;
    }

    /**
     * Sets the list of past participant records for this group.
     *
     * @param pastParticipants the list to set, or {@code null} to clear
     */
    public void setPastParticipants(List<GroupPastParticipant> pastParticipants) {
        this.pastParticipants = pastParticipants;
    }
}
