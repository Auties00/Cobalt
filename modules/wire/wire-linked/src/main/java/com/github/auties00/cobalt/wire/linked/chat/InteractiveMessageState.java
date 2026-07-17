package com.github.auties00.cobalt.wire.linked.chat;

import com.github.auties00.cobalt.wire.linked.sync.action.chat.InteractiveMessageAction.InteractiveMessageActionMode;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * A model representing a single interactive-message interaction recorded
 * by the user.
 *
 * <p>Interactive messages are the rich card menus, list pickers, CTAs
 * and quick-reply buttons that businesses send to customers. Some
 * interactions need to propagate across linked devices — for example a
 * call-to-action button that has been tapped once should appear disabled
 * on every device. Each entry pairs the interactive
 * {@linkplain #messageId() message identifier} with the
 * {@linkplain #type() interaction mode} and the agent-generated
 * {@linkplain #agmId() identifier of the specific element} (for example
 * the CTA button) within the message that the user interacted with.
 *
 * <p>Cobalt persists each entry independently so callers can resolve the
 * state of a single interactive message without iterating the whole
 * map.
 *
 * <p>This class is a local model only. Modifying its fields does not send
 * any request to the WhatsApp servers; it simply reflects the locally
 * cached state.
 */
@ProtobufMessage
public final class InteractiveMessageState {
    /**
     * The non-{@code null} stable identifier of the interactive-message
     * thread. Used as the primary key by Cobalt's store.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String messageId;

    /**
     * The non-{@code null} interaction mode (for example
     * {@link InteractiveMessageActionMode#DISABLE_CTA}) recorded by this
     * entry.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    InteractiveMessageActionMode type;

    /**
     * The agent-generated message identifier of the specific element the
     * user interacted with, or {@code null} when no specific element is
     * targeted.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String agmId;

    /**
     * Constructs a new interactive-message state with the given fields.
     *
     * @param messageId the non-{@code null} message identifier
     * @param type      the non-{@code null} interaction mode
     * @param agmId     the agent-generated element identifier, or
     *                  {@code null}
     */
    InteractiveMessageState(String messageId, InteractiveMessageActionMode type, String agmId) {
        this.messageId = Objects.requireNonNull(messageId, "messageId cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.agmId = agmId;
    }

    /**
     * Returns the non-{@code null} interactive-message identifier.
     *
     * @return the message identifier
     */
    public String messageId() {
        return messageId;
    }

    /**
     * Returns the non-{@code null} interaction mode recorded by this
     * entry.
     *
     * @return the interaction mode
     */
    public InteractiveMessageActionMode type() {
        return type;
    }

    /**
     * Updates the interaction mode of this entry.
     *
     * @param type the non-{@code null} new mode
     * @return this entry instance for method chaining
     */
    public InteractiveMessageState setType(InteractiveMessageActionMode type) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        return this;
    }

    /**
     * Returns the agent-generated identifier of the specific element the
     * user interacted with.
     *
     * @return an {@code Optional} containing the identifier, or empty if
     *         not set
     */
    public Optional<String> agmId() {
        return Optional.ofNullable(agmId);
    }

    /**
     * Updates the agent-generated element identifier.
     *
     * @param agmId the new identifier, or {@code null} to clear
     * @return this entry instance for method chaining
     */
    public InteractiveMessageState setAgmId(String agmId) {
        this.agmId = agmId;
        return this;
    }

    /**
     * Returns a hash code derived from this entry's
     * {@linkplain #messageId() message identifier}.
     *
     * @return the hash code of the message identifier
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(messageId);
    }

    /**
     * Returns whether this entry is equal to the given object.
     *
     * <p>Two entries are considered equal when they share the same
     * {@linkplain #messageId() identifier}, regardless of the other
     * fields.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is an {@code InteractiveMessageState}
     *         with the same identifier
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof InteractiveMessageState that && Objects.equals(this.messageId, that.messageId);
    }
}
