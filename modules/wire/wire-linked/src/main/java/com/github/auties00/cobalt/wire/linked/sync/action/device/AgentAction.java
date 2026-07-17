package com.github.auties00.cobalt.wire.linked.sync.action.device;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Represents a sync action that records the state of a device agent assigned to a
 * WhatsApp Business account.
 *
 * <p>A device agent is a human operator associated with a specific linked device so
 * that incoming conversations can be routed to and handled by that operator. This
 * action is replicated across the account's devices through the app state sync
 * protocol so every device sees the same agent roster.
 *
 * <p>Each mutation targets one specific agent, identified by an agent id carried in
 * {@link AgentActionArgs}. The payload stores a human readable name, the device id
 * the agent is bound to, and a deletion flag used to tombstone the agent entry
 * without physically removing it from the mutation log.
 */
@ProtobufMessage(name = "SyncActionValue.AgentAction")
public final class AgentAction implements SyncAction<AgentActionArgs> {
    /**
     * Canonical action name used as the first component of the mutation index for
     * every {@code AgentAction} replicated through the app state sync protocol.
     */
    public static final String ACTION_NAME = "deviceAgent";

    /**
     * Schema version advertised by this action, used by sync handlers to gate
     * deserialisation and handling of newer payload shapes.
     */
    public static final int ACTION_VERSION = 7;

    /**
     * Collection this action belongs to, used by the sync protocol to route the
     * mutation into the correct replication stream.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR;

    /**
     * Returns the canonical action name for every {@code AgentAction}.
     *
     * @return the constant {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version for every {@code AgentAction}.
     *
     * @return the constant {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Human readable display name of the agent, as shown in the business inbox UI.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String name;

    /**
     * Identifier of the linked device this agent is bound to.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    Integer deviceID;

    /**
     * Tombstone flag set to {@code true} when the agent entry has been removed
     * while still preserving a record of the deletion for conflict resolution.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    Boolean isDeleted;


    /**
     * Constructs a new {@code AgentAction} from raw protobuf field values.
     *
     * @param name      the agent display name, possibly {@code null}
     * @param deviceID  the device identifier the agent is bound to, possibly {@code null}
     * @param isDeleted whether the agent entry is tombstoned, possibly {@code null}
     */
    AgentAction(String name, Integer deviceID, Boolean isDeleted) {
        this.name = name;
        this.deviceID = deviceID;
        this.isDeleted = isDeleted;
    }

    /**
     * Returns the human readable display name of the agent, if one was encoded.
     *
     * @return an {@link Optional} containing the agent name, or
     *         {@link Optional#empty()} if absent
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the identifier of the linked device the agent is bound to, if one
     * was encoded.
     *
     * @return an {@link OptionalInt} containing the device id, or
     *         {@link OptionalInt#empty()} if absent
     */
    public OptionalInt deviceID() {
        return deviceID == null ? OptionalInt.empty() : OptionalInt.of(deviceID);
    }

    /**
     * Returns whether the agent entry is tombstoned.
     *
     * @return {@code true} if the entry is marked deleted, {@code false} otherwise
     *         (including when the field was unset on the wire)
     */
    public boolean isDeleted() {
        return isDeleted != null && isDeleted;
    }

    /**
     * Sets the human readable display name of the agent.
     *
     * @param name the new agent name, or {@code null} to clear
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the identifier of the linked device the agent is bound to.
     *
     * @param deviceID the new device id, or {@code null} to clear
     */
    public void setDeviceID(Integer deviceID) {
        this.deviceID = deviceID;
    }

    /**
     * Sets the tombstone flag on this agent entry.
     *
     * @param isDeleted {@code true} to mark the entry deleted, {@code false} to
     *                  keep it active, or {@code null} to clear
     */
    public void setDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }


}
