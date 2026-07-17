package com.github.auties00.cobalt.wire.linked.federated;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Snapshot of the federated-identity ("Waffle") state machine for the local
 * WhatsApp account, returned by the relay in reply to a state-existence probe.
 *
 * <p>The first step of the Meta-account linking flow asks the relay whether
 * the WhatsApp account already has a federated-identity record on the bridge
 * and, if so, what stage of the lifecycle it sits in. The relay reports back a
 * single integer state code together with a pair of suspension flags. A client
 * that finds a non-zero state code can skip directly to the certificate-fetch
 * step; a client that finds a suspension flag must show the appropriate
 * recovery surface to the user instead of resuming the link.
 *
 * <p>The state code values are defined by WhatsApp's {@code WAEnum_WaffleState}
 * enum; this class carries the raw integer rather than re-encoding the enum
 * because the value space evolves with the bridge.
 */
@ProtobufMessage(name = "FederatedIdentityState")
public final class FederatedIdentityState {
    /**
     * Numeric Waffle state code reported by the relay. The value tracks the
     * federated-identity bridge's lifecycle state: {@code 0} typically means
     * "no record", positive values identify the various enrolled / linked /
     * paused stages.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    int state;

    /**
     * Whether the relay surfaced an explicit {@code <suspended_state/>}
     * marker, indicating the linked Meta account is currently suspended.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    boolean suspended;

    /**
     * The "no-personal-recovery" flag carried by the suspension marker, when
     * present. {@code true} means the user has no path to self-recover the
     * suspension; {@code false} means a personal-recovery flow is available.
     * The field is absent when the relay omits the {@code <suspended_state/>}
     * marker.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    Boolean noPersonalRecovery;

    /**
     * Constructs a new {@code FederatedIdentityState}.
     *
     * @param state              the numeric Waffle state code
     * @param suspended          whether the relay surfaced a suspension
     *                           marker
     * @param noPersonalRecovery the no-personal-recovery flag, or
     *                           {@code null} when no suspension marker was
     *                           present
     */
    FederatedIdentityState(int state, boolean suspended, Boolean noPersonalRecovery) {
        this.state = state;
        this.suspended = suspended;
        this.noPersonalRecovery = noPersonalRecovery;
    }

    /**
     * Returns the numeric Waffle state code reported by the relay.
     *
     * @return the state code
     */
    public int state() {
        return state;
    }

    /**
     * Returns whether the linked Meta account is currently suspended.
     *
     * @return {@code true} when the relay surfaced a suspension marker,
     *         {@code false} otherwise
     */
    public boolean suspended() {
        return suspended;
    }

    /**
     * Returns the "no-personal-recovery" flag carried by the suspension
     * marker, when present.
     *
     * @return an {@link Optional} containing {@code true} when the user
     *         cannot self-recover the suspension, {@code false} when a
     *         personal-recovery flow is available, or empty when the relay
     *         did not surface a suspension marker
     */
    public Optional<Boolean> noPersonalRecovery() {
        return Optional.ofNullable(noPersonalRecovery);
    }

    /**
     * Replaces the numeric Waffle state code.
     *
     * @param state the new state code
     */
    public void setState(int state) {
        this.state = state;
    }

    /**
     * Replaces the suspension flag.
     *
     * @param suspended {@code true} to mark the account as suspended,
     *                  {@code false} otherwise
     */
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    /**
     * Replaces the no-personal-recovery flag.
     *
     * @param noPersonalRecovery the new flag value, or {@code null} to clear
     */
    public void setNoPersonalRecovery(Boolean noPersonalRecovery) {
        this.noPersonalRecovery = noPersonalRecovery;
    }
}
