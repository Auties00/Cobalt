package com.github.auties00.cobalt.wire.linked.setting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Server-side enforcement window throttling new-chat initiation for
 * the authenticated account.
 *
 * <p>WhatsApp throttles new-chat initiation when an account has been
 * flagged for spam reports or suspicious outreach: while the timelock
 * is active the send path is gated and the official UI surfaces a
 * cooldown notice. The relay describes the current verdict using
 * three fields:
 * <ul>
 *   <li>{@link #isActive()} — whether enforcement is currently in
 *       effect;</li>
 *   <li>{@link #timeEnforcementEnds()} — the Unix-time epoch second
 *       at which the enforcement window expires (only meaningful when
 *       enforcement is active);</li>
 *   <li>{@link #enforcementType()} — the kind of enforcement applied
 *       (soft warning, hard block, etc.).</li>
 * </ul>
 *
 * <p>Applications that want to gate their own send path on the
 * timelock should consult these fields when starting brand-new chats.
 */
@ProtobufMessage
public final class ReachoutTimelock {
    /**
     * Whether the timelock is currently being enforced.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    boolean active;

    /**
     * The Unix-time epoch second at which enforcement ends. {@code null}
     * when the relay omitted the field (typically when the timelock
     * is inactive).
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String timeEnforcementEnds;

    /**
     * The enforcement-type tag (soft warning, hard block, etc.) issued
     * by the relay. {@code null} when the relay omitted the field.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String enforcementType;

    /**
     * Constructs a new {@code ReachoutTimelock} verdict carrying the
     * relay-issued enforcement state.
     *
     * @param active              whether the timelock is currently
     *                            enforced
     * @param timeEnforcementEnds the enforcement-window end timestamp,
     *                            or {@code null} when omitted
     * @param enforcementType     the enforcement-type tag, or
     *                            {@code null} when omitted
     */
    ReachoutTimelock(boolean active, String timeEnforcementEnds, String enforcementType) {
        this.active = active;
        this.timeEnforcementEnds = timeEnforcementEnds;
        this.enforcementType = enforcementType;
    }

    /**
     * Returns whether the timelock is currently being enforced.
     *
     * @return {@code true} when enforcement is active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns the Unix-time epoch second at which enforcement ends.
     *
     * @return an {@link Optional} carrying the timestamp, or empty
     *         when the relay omitted the field
     */
    public Optional<String> timeEnforcementEnds() {
        return Optional.ofNullable(timeEnforcementEnds);
    }

    /**
     * Returns the enforcement-type tag issued by the relay.
     *
     * @return an {@link Optional} carrying the tag, or empty when the
     *         relay omitted the field
     */
    public Optional<String> enforcementType() {
        return Optional.ofNullable(enforcementType);
    }

    /**
     * Sets whether the timelock is currently being enforced.
     *
     * @param active the new enforcement flag
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Sets the Unix-time epoch second at which enforcement ends.
     *
     * @param timeEnforcementEnds the new timestamp, or {@code null}
     */
    public void setTimeEnforcementEnds(String timeEnforcementEnds) {
        this.timeEnforcementEnds = timeEnforcementEnds;
    }

    /**
     * Sets the enforcement-type tag issued by the relay.
     *
     * @param enforcementType the new tag, or {@code null}
     */
    public void setEnforcementType(String enforcementType) {
        this.enforcementType = enforcementType;
    }
}
