package com.github.auties00.cobalt.wire.linked.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A model representing a single WhatsApp Business "agent" — a
 * support-team member who handles incoming customer chats.
 *
 * <p>Each agent has a stable {@linkplain #agentId() identifier} and a
 * small set of fields the business inbox uses to render the team roster:
 * the human-readable {@linkplain #name() display name}, the
 * {@linkplain #deviceId() identifier of the linked device} the agent is
 * bound to, and a {@linkplain #deleted() deletion flag} that tombstones
 * removed agents without erasing them outright.
 *
 * <p>Cobalt persists each agent independently and updates the
 * corresponding record whenever the matching {@code agent} sync action
 * arrives.
 *
 * <p>This class is a local model only. Modifying its fields does not send
 * any request to the WhatsApp servers; it simply reflects the locally
 * cached state.
 */
@ProtobufMessage
public final class AgentState {
    /**
     * The non-{@code null} stable identifier of the agent. Used as the
     * primary key by Cobalt's store.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String agentId;

    /**
     * The human-readable display name of the agent as shown in the
     * business inbox UI, or {@code null} when no name is set.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String name;

    /**
     * The identifier of the linked device this agent is bound to, or
     * {@code null} when no device is known.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    Integer deviceId;

    /**
     * The tombstone flag set to {@code true} when the agent has been
     * removed but the entry is preserved for conflict resolution.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    boolean deleted;

    /**
     * Constructs a new agent state with the given fields.
     *
     * @param agentId  the non-{@code null} agent identifier
     * @param name     the display name, or {@code null}
     * @param deviceId the linked-device identifier, or {@code null}
     * @param deleted  whether the entry is tombstoned
     */
    AgentState(String agentId, String name, Integer deviceId, boolean deleted) {
        this.agentId = Objects.requireNonNull(agentId, "agentId cannot be null");
        this.name = name;
        this.deviceId = deviceId;
        this.deleted = deleted;
    }

    /**
     * Returns the non-{@code null} agent identifier.
     *
     * @return the agent identifier
     */
    public String agentId() {
        return agentId;
    }

    /**
     * Returns the human-readable display name of the agent.
     *
     * @return an {@code Optional} containing the name, or empty if not
     *         set
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Updates the agent's display name.
     *
     * @param name the new name, or {@code null} to clear it
     * @return this agent state instance for method chaining
     */
    public AgentState setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Returns the identifier of the linked device the agent is bound to.
     *
     * @return an {@code OptionalInt} containing the device identifier, or
     *         empty if not set
     */
    public OptionalInt deviceId() {
        return deviceId == null ? OptionalInt.empty() : OptionalInt.of(deviceId);
    }

    /**
     * Updates the linked-device identifier.
     *
     * @param deviceId the new device identifier, or {@code null} to clear
     * @return this agent state instance for method chaining
     */
    public AgentState setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    /**
     * Returns whether the agent entry is tombstoned.
     *
     * @return {@code true} if the entry has been marked deleted
     */
    public boolean deleted() {
        return deleted;
    }

    /**
     * Updates the tombstone flag of this agent entry.
     *
     * @param deleted the new flag
     * @return this agent state instance for method chaining
     */
    public AgentState setDeleted(boolean deleted) {
        this.deleted = deleted;
        return this;
    }

    /**
     * Returns a hash code derived from this entry's
     * {@linkplain #agentId() identifier}.
     *
     * @return the hash code of the agent identifier
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(agentId);
    }

    /**
     * Returns whether this agent state is equal to the given object.
     *
     * <p>Two agent states are considered equal when they share the same
     * {@linkplain #agentId() identifier}, regardless of the other
     * fields.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is an {@code AgentState}
     *         with the same identifier
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof AgentState that && Objects.equals(this.agentId, that.agentId);
    }
}
