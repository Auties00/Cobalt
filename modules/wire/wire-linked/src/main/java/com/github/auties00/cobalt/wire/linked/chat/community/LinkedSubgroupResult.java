package com.github.auties00.cobalt.wire.linked.chat.community;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the per-subgroup outcome row returned when linking one or more
 * existing groups into a parent community.
 *
 * <p>The relay's link-sub-groups response carries one of these rows per
 * candidate sub-group. The row records the sub-group's JID, whether the
 * relay echoed the {@code <hidden_group/>} marker (signalling the
 * sub-group is hidden from the community directory), and the per-participant
 * errors encountered while transferring the candidate's roster into the
 * parent community. Roster transfer is implicit: linking a sub-group adds
 * its members to the parent community as well, and a participant whose
 * privacy settings forbid the implicit add is reported back here as a
 * {@link Map} entry from the participant JID to the relay-reported error
 * code (always {@code "403"} in current schemas).
 *
 * @see CommunityMetadata
 */
@ProtobufMessage(name = "LinkedSubgroupResult")
public final class LinkedSubgroupResult {
    /**
     * The sub-group JID echoed by the relay. Required, never {@code null}
     * after construction.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid jid;

    /**
     * Whether the relay echoed the {@code <hidden_group/>} marker for
     * this sub-group.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    boolean hiddenGroup;

    /**
     * Per-participant error mapping — populated only for participants the
     * relay could not transfer to the parent community due to privacy
     * settings. The mapping is keyed by participant JID and carries the
     * relay-reported error code (typically the literal {@code "403"}).
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.STRING)
    Map<Jid, String> participantErrors;

    /**
     * Constructs a new linked-subgroup result row.
     *
     * @param jid               the sub-group JID; must not be {@code null}
     * @param hiddenGroup       whether the hidden-group marker was echoed
     * @param participantErrors the per-participant error mapping; never
     *                          {@code null}
     * @throws NullPointerException if any reference argument is
     *                              {@code null}
     */
    LinkedSubgroupResult(Jid jid, boolean hiddenGroup, Map<Jid, String> participantErrors) {
        this.jid = Objects.requireNonNull(jid, "jid cannot be null");
        this.hiddenGroup = hiddenGroup;
        Objects.requireNonNull(participantErrors, "participantErrors cannot be null");
        this.participantErrors = Map.copyOf(participantErrors);
    }

    /**
     * Returns the sub-group JID.
     *
     * @return the non-{@code null} JID
     */
    public Jid jid() {
        return jid;
    }

    /**
     * Returns whether the relay echoed the {@code <hidden_group/>}
     * marker for this sub-group.
     *
     * @return {@code true} when the sub-group is hidden from the community
     *         directory
     */
    public boolean hiddenGroup() {
        return hiddenGroup;
    }

    /**
     * Returns the per-participant error mapping.
     *
     * @return an unmodifiable map keyed by the participant JID and
     *         carrying the relay-reported error code; empty when every
     *         participant was admitted into the parent community cleanly
     */
    public Map<Jid, String> participantErrors() {
        return participantErrors == null ? Map.of() : Collections.unmodifiableMap(participantErrors);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LinkedSubgroupResult that
                && hiddenGroup == that.hiddenGroup
                && Objects.equals(jid, that.jid)
                && Objects.equals(participantErrors, that.participantErrors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jid, hiddenGroup, participantErrors);
    }
}
