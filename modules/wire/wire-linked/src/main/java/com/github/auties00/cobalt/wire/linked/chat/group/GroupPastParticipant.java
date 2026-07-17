package com.github.auties00.cobalt.wire.linked.chat.group;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Represents a user who used to be a participant of a WhatsApp group but is
 * no longer in it.
 *
 * <p>WhatsApp keeps a record of every member who recently left or was removed
 * from a group so that the "members updates" panel can show who has departed
 * and when. Each record captures the participant's JID, the reason they left
 * (voluntary departure or removal by an administrator) and the moment at
 * which the departure happened.
 *
 * <p>Past-participant records are not retained forever: the client prunes
 * records older than a fixed expiration window. {@link #isExpired()} reports
 * whether a record has aged past that window and may be safely discarded
 * during periodic cleanup.
 *
 * @see GroupPastParticipants
 * @see GroupMetadata
 */
@ProtobufMessage(name = "PastParticipant")
public final class GroupPastParticipant {
    /**
     * The maximum age of a past-participant record before it is considered
     * expired and eligible for pruning from the local store.
     */
    private static final Duration EXPIRATION = Duration.ofDays(60);

    /**
     * The JID of the user who left or was removed from the group. Populated
     * for every record received from the server; may be {@code null} only on
     * partially constructed instances.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid userJid;

    /**
     * The reason the participant departed: voluntary leave or admin removal.
     * May be {@code null} when the server does not report a reason.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    LeaveReason leaveReason;

    /**
     * The instant at which the participant left or was removed from the
     * group. Populated for every record received from the server; may be
     * {@code null} only on partially constructed instances.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant timestamp;


    /**
     * Constructs a new {@code GroupPastParticipant} with the specified JID,
     * leave reason, and timestamp.
     *
     * @param userJid     the JID of the departed participant, or {@code null}
     * @param leaveReason the reason for departure, or {@code null}
     * @param timestamp   the departure instant, or {@code null}
     */
    GroupPastParticipant(Jid userJid, LeaveReason leaveReason, Instant timestamp) {
        this.userJid = userJid;
        this.leaveReason = leaveReason;
        this.timestamp = timestamp;
    }

    /**
     * Returns the JID of the user who left or was removed from the group.
     *
     * @return an {@code Optional} containing the user JID, or empty if not
     *         available
     */
    public Optional<Jid> userJid() {
        return Optional.ofNullable(userJid);
    }

    /**
     * Returns the reason the participant left the group.
     *
     * @return an {@code Optional} containing the leave reason, or empty if
     *         not known
     */
    public Optional<LeaveReason> leaveReason() {
        return Optional.ofNullable(leaveReason);
    }

    /**
     * Returns the instant at which the participant left or was removed from
     * the group.
     *
     * @return an {@code Optional} containing the departure timestamp, or
     *         empty if not available
     */
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }

    /**
     * Sets the JID of the departed participant.
     *
     * @param userJid the user JID to set, or {@code null} to clear
     */
    public void setUserJid(Jid userJid) {
        this.userJid = userJid;
    }

    /**
     * Sets the reason the participant left the group.
     *
     * @param leaveReason the leave reason to set, or {@code null} to clear
     */
    public void setLeaveReason(LeaveReason leaveReason) {
        this.leaveReason = leaveReason;
    }

    /**
     * Sets the instant at which the participant left or was removed.
     *
     * @param timestamp the departure timestamp to set, or {@code null} to
     *                  clear
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns whether this past-participant record has aged past the
     * expiration window (60 days) and may be safely discarded.
     *
     * @return {@code true} if the record is older than the expiration
     *         threshold, {@code false} otherwise
     */
    public boolean isExpired() {
        var age = Duration.between(timestamp, Instant.now());
        return age.compareTo(EXPIRATION) > 0;
    }

    /**
     * Represents the reason a participant departed from a WhatsApp group.
     *
     * <p>A participant may either leave the group voluntarily ({@link #LEFT})
     * or be removed by an administrator ({@link #REMOVED}).
     */
    @ProtobufEnum(name = "PastParticipant.LeaveReason")
    public static enum LeaveReason {
        /**
         * The participant left the group voluntarily.
         */
        LEFT(0),

        /**
         * The participant was removed from the group by an administrator.
         */
        REMOVED(1);

        /**
         * Constructs a {@code LeaveReason} with the given protobuf index.
         *
         * @param index the protobuf enum index
         */
        LeaveReason(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf-assigned numeric index for this leave reason.
         */
        final int index;

        /**
         * Returns the protobuf-assigned numeric index for this leave reason.
         *
         * @return the protobuf enum index
         */
        public int index() {
            return this.index;
        }
    }
}
