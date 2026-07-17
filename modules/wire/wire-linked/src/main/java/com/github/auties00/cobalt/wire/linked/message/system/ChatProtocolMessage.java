package com.github.auties00.cobalt.wire.linked.message.system;

import com.github.auties00.cobalt.wire.linked.message.Message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A system-level message that identifies a chat by its display name and identifier.
 *
 * <p>This message is used internally by WhatsApp to attach chat metadata to
 * protocol-level operations where a chat reference is needed alongside a human
 * readable label, for example when surfacing a chat in a cross-chat
 * notification. Both fields are optional because the message may carry either
 * only the display name, only the identifier, or both depending on the
 * originating flow.
 */
@ProtobufMessage(name = "Message.Chat")
public final class ChatProtocolMessage implements Message {
    /**
     * The human readable name of the chat as it should be presented to the user.
     *
     * <p>This value is typically the contact's push name, the group subject, or
     * any other label that identifies the chat visually. It may be {@code null}
     * when the chat is referenced only by identifier.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String displayName;

    /**
     * The opaque identifier of the chat as assigned by the server.
     *
     * <p>This value may be {@code null} when only the display name is known.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String id;


    /**
     * Constructs a new chat protocol message with the provided display name and identifier.
     *
     * @param displayName the human readable name of the chat, may be {@code null}
     * @param id          the opaque chat identifier, may be {@code null}
     */
    ChatProtocolMessage(String displayName, String id) {
        this.displayName = displayName;
        this.id = id;
    }

    /**
     * Returns the human readable name of the chat.
     *
     * @return an {@link Optional} containing the display name, or
     *         {@link Optional#empty()} if no display name is set
     */
    public Optional<String> displayName() {
        return Optional.ofNullable(displayName);
    }

    /**
     * Returns the opaque identifier of the chat.
     *
     * @return an {@link Optional} containing the identifier, or
     *         {@link Optional#empty()} if no identifier is set
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Sets the human readable name of the chat.
     *
     * @param displayName the new display name, or {@code null} to clear it
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Sets the opaque identifier of the chat.
     *
     * @param id the new identifier, or {@code null} to clear it
     */
    public void setId(String id) {
        this.id = id;
    }
}
