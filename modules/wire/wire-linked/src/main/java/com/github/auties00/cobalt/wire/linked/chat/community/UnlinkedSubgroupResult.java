package com.github.auties00.cobalt.wire.linked.chat.community;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the per-subgroup outcome row returned when detaching one or
 * more sub-groups from a parent community.
 *
 * <p>The relay's unlink-groups response carries one of these rows per
 * candidate sub-group. Each row records the sub-group's JID, whether the
 * relay echoed the {@code remove_orphaned_members="true"} attribute
 * (signalling that members who only belonged to the parent community via
 * this sub-group were evicted from the community), and an optional error
 * discriminator captured when the relay failed to detach the sub-group.
 *
 * <p>The {@link Reason} discriminator follows the WA Web sub-group
 * unlink-error mixin family:
 * <ul>
 *   <li>{@link Reason#BAD_REQUEST} — the request was rejected as
 *   malformed;</li>
 *   <li>{@link Reason#NOT_AUTHORIZED} — the caller lacks the admin
 *   privileges required to detach the sub-group;</li>
 *   <li>{@link Reason#NOT_EXIST} — the candidate is not a sub-group of
 *   the supplied parent community;</li>
 *   <li>{@link Reason#NOT_ACCEPTABLE} — the relay refused the unlink for
 *   policy reasons (the sub-group still hosts active sub-content);</li>
 *   <li>{@link Reason#PARTIAL_SERVER_ERROR} — the relay encountered a
 *   transient internal failure that affected only this sub-group;</li>
 *   <li>{@link Reason#SERVER_ERROR} — the relay encountered a transient
 *   internal failure for the entire request.</li>
 * </ul>
 *
 * @see CommunityMetadata
 */
@ProtobufMessage(name = "UnlinkedSubgroupResult")
public final class UnlinkedSubgroupResult {
    /**
     * The sub-group JID echoed by the relay. Required, never {@code null}
     * after construction.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid jid;

    /**
     * Whether the relay echoed the
     * {@code remove_orphaned_members="true"} attribute for this
     * sub-group.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    boolean removeOrphanedMembers;

    /**
     * The optional per-row failure discriminator. {@code null} when the
     * unlink succeeded for this sub-group.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    Reason reason;

    /**
     * Constructs a new unlinked-subgroup result row.
     *
     * @param jid                   the sub-group JID; must not be
     *                              {@code null}
     * @param removeOrphanedMembers whether the eviction attribute was
     *                              echoed
     * @param reason                the optional failure discriminator;
     *                              {@code null} when the unlink
     *                              succeeded for this sub-group
     * @throws NullPointerException if {@code jid} is {@code null}
     */
    UnlinkedSubgroupResult(Jid jid, boolean removeOrphanedMembers, Reason reason) {
        this.jid = Objects.requireNonNull(jid, "jid cannot be null");
        this.removeOrphanedMembers = removeOrphanedMembers;
        this.reason = reason;
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
     * Returns whether the eviction attribute was echoed.
     *
     * @return {@code true} when the relay echoed
     *         {@code remove_orphaned_members="true"}
     */
    public boolean removeOrphanedMembers() {
        return removeOrphanedMembers;
    }

    /**
     * Returns the optional per-row failure discriminator.
     *
     * @return an {@link Optional} carrying the {@link Reason}, or empty
     *         when the unlink succeeded for this sub-group
     */
    public Optional<Reason> reason() {
        return Optional.ofNullable(reason);
    }

    /**
     * Returns whether the unlink succeeded for this sub-group.
     *
     * @return {@code true} when the relay reported no failure
     *         discriminator
     */
    public boolean succeeded() {
        return reason == null;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UnlinkedSubgroupResult that
                && removeOrphanedMembers == that.removeOrphanedMembers
                && Objects.equals(jid, that.jid)
                && reason == that.reason;
    }

    @Override
    public int hashCode() {
        return Objects.hash(jid, removeOrphanedMembers, reason);
    }

    /**
     * Identifies why a single sub-group failed to unlink.
     */
    @ProtobufEnum
    public enum Reason {
        /**
         * The request was rejected as malformed.
         */
        BAD_REQUEST(0),

        /**
         * The caller lacks the admin privileges required to detach the
         * sub-group.
         */
        NOT_AUTHORIZED(1),

        /**
         * The candidate is not a sub-group of the supplied parent
         * community.
         */
        NOT_EXIST(2),

        /**
         * The relay refused the unlink for policy reasons (typically
         * because the sub-group still hosts active sub-content).
         */
        NOT_ACCEPTABLE(3),

        /**
         * The relay encountered a transient internal failure that
         * affected only this sub-group.
         */
        PARTIAL_SERVER_ERROR(4),

        /**
         * The relay encountered a transient internal failure for the
         * entire request.
         */
        SERVER_ERROR(5);

        /**
         * The protobuf wire-format index associated with this reason.
         */
        final int index;

        /**
         * Constructs a new {@code Reason} with the supplied protobuf
         * index.
         *
         * @param index the protobuf wire-format index
         */
        Reason(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Returns the protobuf wire-format index associated with this
         * value.
         *
         * @return the protobuf wire-format index
         */
        public int index() {
            return index;
        }

        /**
         * Returns the {@link Reason} matching the given on-the-wire
         * discriminator tag, or {@code null} when the tag is absent or
         * unrecognised.
         *
         * @param tag the discriminator tag emitted by the relay; may be
         *            {@code null}
         * @return the matching {@link Reason}, or {@code null} when the
         *         tag does not match any known reason
         */
        public static Reason of(String tag) {
            if (tag == null) {
                return null;
            }
            return switch (tag) {
                case "bad_request" -> BAD_REQUEST;
                case "not_authorized" -> NOT_AUTHORIZED;
                case "not_exist" -> NOT_EXIST;
                case "not_acceptable" -> NOT_ACCEPTABLE;
                case "partial_server_error" -> PARTIAL_SERVER_ERROR;
                case "server_error" -> SERVER_ERROR;
                default -> null;
            };
        }
    }
}
