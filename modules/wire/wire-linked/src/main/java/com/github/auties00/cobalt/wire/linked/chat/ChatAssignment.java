package com.github.auties00.cobalt.wire.linked.chat;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * A model representing the WhatsApp Business team-inbox assignment of a
 * single customer chat.
 *
 * <p>In WhatsApp Business team inbox, each customer chat can be assigned to
 * a specific support agent and lives in either the "open" or "closed/
 * resolved" tab. This record collapses the previously-separate assignment
 * and open-tab maps into a single coherent entity keyed by the
 * {@linkplain #chatJid() customer chat JID}, so the two pieces of state
 * can no longer drift out of sync.
 *
 * <p>Cobalt persists each assignment independently so callers can resolve
 * the team-inbox state of a single chat without iterating the whole map.
 * The matching sync action updates the record whenever the chat is
 * assigned, reassigned, opened or closed.
 *
 * <p>This class is a local model only. Modifying its fields does not send any
 * request to the WhatsApp servers; it simply reflects the locally cached
 * state.
 */
@ProtobufMessage
public final class ChatAssignment {
    /**
     * The non-{@code null} JID of the customer chat this assignment refers
     * to. Used as the primary key by Cobalt's store.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid chatJid;

    /**
     * The identifier of the assigned agent, or {@code null} when the chat
     * is currently unassigned.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String agentId;

    /**
     * Whether the assigned chat is currently in the team inbox's "open" tab
     * ({@code true}) or in the closed/resolved tab ({@code false}).
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    boolean opened;

    /**
     * Constructs a new chat assignment with the given fields.
     *
     * @param chatJid the non-{@code null} customer chat JID
     * @param agentId the assigned agent identifier, or {@code null}
     * @param opened  whether the assignment is in the open tab
     */
    ChatAssignment(Jid chatJid, String agentId, boolean opened) {
        this.chatJid = Objects.requireNonNull(chatJid, "chatJid cannot be null");
        this.agentId = agentId;
        this.opened = opened;
    }

    /**
     * Returns the non-{@code null} customer chat JID this assignment refers
     * to.
     *
     * @return the chat JID
     */
    public Jid chatJid() {
        return chatJid;
    }

    /**
     * Returns the identifier of the assigned agent.
     *
     * @return an {@code Optional} containing the agent identifier, or empty
     *         when the chat is currently unassigned
     */
    public Optional<String> agentId() {
        return Optional.ofNullable(agentId);
    }

    /**
     * Updates the assigned-agent identifier.
     *
     * @param agentId the new agent identifier, or {@code null} to mark the
     *                chat unassigned
     * @return this chat-assignment instance for method chaining
     */
    public ChatAssignment setAgentId(String agentId) {
        this.agentId = agentId;
        return this;
    }

    /**
     * Returns whether the assignment is currently in the team inbox's
     * "open" tab.
     *
     * @return {@code true} if the assignment is in the open tab,
     *         {@code false} if it is in the closed/resolved tab
     */
    public boolean opened() {
        return opened;
    }

    /**
     * Updates the open-tab flag of this assignment.
     *
     * @param opened the new open-tab flag
     * @return this chat-assignment instance for method chaining
     */
    public ChatAssignment setOpened(boolean opened) {
        this.opened = opened;
        return this;
    }

    /**
     * Returns a hash code derived from this assignment's
     * {@linkplain #chatJid() chat JID}.
     *
     * @return the hash code of the chat JID
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(chatJid);
    }

    /**
     * Returns whether this chat assignment is equal to the given object.
     *
     * <p>Two assignments are considered equal when they share the same
     * {@linkplain #chatJid() chat JID}, regardless of the agent identifier
     * or open-tab flag.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is a {@code ChatAssignment}
     *         with the same chat JID
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof ChatAssignment that && Objects.equals(this.chatJid, that.chatJid);
    }
}
