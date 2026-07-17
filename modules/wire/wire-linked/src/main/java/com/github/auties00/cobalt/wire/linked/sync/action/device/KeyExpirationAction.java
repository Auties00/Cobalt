package com.github.auties00.cobalt.wire.linked.sync.action.device;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.OptionalInt;

/**
 * Represents a sync action that records the epoch at which the account's signal
 * identity keys are considered expired.
 *
 * <p>WhatsApp uses this sentinel to coordinate forced key refreshes across every
 * linked device. When a peer observes an expired key epoch it is expected to
 * re-run the key handshake before sending further encrypted traffic. The
 * mutation is singleton, so the sync index is composed solely of
 * {@link #ACTION_NAME} with no trailing arguments.
 */
@ProtobufMessage(name = "SyncActionValue.KeyExpiration")
public final class KeyExpirationAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name used as the sole component of the singleton mutation
     * index for this action type.
     */
    public static final String ACTION_NAME = "sentinel";

    /**
     * Schema version advertised by this action, used by sync handlers to gate
     * deserialisation and handling of newer payload shapes.
     */
    public static final int ACTION_VERSION = 3;

    /**
     * Collection this action belongs to, used by the sync protocol to route the
     * mutation into the correct replication stream.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_LOW;

    /**
     * Returns the canonical action name for every {@code KeyExpirationAction}.
     *
     * @return the constant {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version for every {@code KeyExpirationAction}.
     *
     * @return the constant {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Monotonic epoch value identifying the generation of signal identity keys
     * that has been marked as expired.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    Integer expiredKeyEpoch;


    /**
     * Constructs a new {@code KeyExpirationAction} from raw protobuf field
     * values.
     *
     * @param expiredKeyEpoch the expired key epoch, possibly {@code null}
     */
    KeyExpirationAction(Integer expiredKeyEpoch) {
        this.expiredKeyEpoch = expiredKeyEpoch;
    }

    /**
     * Returns the monotonic epoch at which the account's signal identity keys
     * are considered expired, if one was encoded.
     *
     * @return an {@link OptionalInt} containing the expired key epoch, or
     *         {@link OptionalInt#empty()} if absent
     */
    public OptionalInt expiredKeyEpoch() {
        return expiredKeyEpoch == null ? OptionalInt.empty() : OptionalInt.of(expiredKeyEpoch);
    }

    /**
     * Sets the monotonic epoch at which the account's signal identity keys are
     * considered expired.
     *
     * @param expiredKeyEpoch the new expired key epoch, or {@code null} to clear
     */
    public void setExpiredKeyEpoch(Integer expiredKeyEpoch) {
        this.expiredKeyEpoch = expiredKeyEpoch;
    }
}
