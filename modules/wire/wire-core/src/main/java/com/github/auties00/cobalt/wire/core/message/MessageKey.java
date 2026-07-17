package com.github.auties00.cobalt.wire.core.message;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Unique identifier for a WhatsApp message within a given conversation.
 *
 * <p>Every message sent or received on WhatsApp is addressed by a triple
 * consisting of the chat it belongs to, the direction of travel
 * (outgoing if sent by the logged-in user, otherwise incoming), and an
 * application-level identifier assigned by the sender. Together these
 * three pieces of information form a {@code MessageKey} and are used
 * throughout the protocol whenever a message must be referenced: when
 * quoting a message, reacting to it, editing it, revoking it, forwarding
 * it, or acknowledging its delivery.
 *
 * <p>The key also records the sender JID separately, which matters in
 * group conversations where the chat JID identifies the group rather
 * than the participant who actually posted the message.
 */
@ProtobufMessage(name = "MessageKey")
public final class MessageKey {
    /**
     * The JID of the chat, group, or newsletter that hosts the message.
     *
     * <p>Also serves as the fallback sender when
     * {@link #senderJid} is not populated (for example in one-to-one
     * chats).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid parentJid;

    /**
     * Whether the message was sent by the logged-in user.
     *
     * <p>{@code true} for outgoing messages, {@code false} or
     * {@code null} for incoming ones.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    Boolean fromMe;

    /**
     * The application-level identifier assigned by the sender when the
     * message was created.
     *
     * <p>Message identifiers are unique within the sender's scope and
     * are used by the protocol to correlate the original message with
     * any later references such as reactions, quotes, or revocations.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String id;

    /**
     * The JID of the individual participant who authored the message.
     *
     * <p>Populated in group conversations where the chat JID points to
     * the group rather than the sender. For one-to-one chats this
     * field is typically left empty and the sender coincides with the
     * chat JID.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    Jid senderJid;


    /**
     * Constructs a new {@code MessageKey}.
     *
     * <p>The constructor is package-private; use
     * {@code MessageKeyBuilder} to instantiate new keys.
     *
     * @param parentJid the chat JID that hosts the message
     * @param fromMe    whether the message was sent by the logged-in user
     * @param id        the application-level identifier of the message
     * @param senderJid the JID of the participant that authored the
     *                  message, or {@code null} for one-to-one chats
     */
    MessageKey(Jid parentJid, Boolean fromMe, String id, Jid senderJid) {
        this.parentJid = parentJid;
        this.fromMe = fromMe;
        this.id = id;
        this.senderJid = senderJid;
    }

    /**
     * Returns the JID of the chat, group, or newsletter that hosts the
     * message.
     *
     * @return an {@link Optional} holding the chat JID, or empty if
     *         none was set
     */
    public Optional<Jid> parentJid() {
        return Optional.ofNullable(parentJid);
    }

    /**
     * Returns whether the message was sent by the logged-in user.
     *
     * <p>An unset flag is treated as {@code false}.
     *
     * @return {@code true} if the message is outgoing, {@code false}
     *         otherwise
     */
    public boolean fromMe() {
        return fromMe != null && fromMe;
    }

    /**
     * Returns the application-level identifier of the message.
     *
     * @return an {@link Optional} holding the identifier, or empty if
     *         none was set
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the JID of the participant that authored the message.
     *
     * <p>If no dedicated sender is recorded, falls back to
     * {@link #parentJid()}, matching the convention used in one-to-one
     * chats where the chat JID equals the sender.
     *
     * @return an {@link Optional} holding the sender JID, or empty if
     *         neither a dedicated sender nor a parent JID is recorded
     */
    public Optional<Jid> senderJid() {
        if(senderJid != null) {
            return Optional.of(senderJid);
        } else {
            return Optional.ofNullable(parentJid);
        }
    }

    /**
     * Updates the JID of the chat that hosts the message.
     *
     * @param chatJid the new chat JID, or {@code null} to clear
     */
    public void setParentJid(Jid chatJid) {
        this.parentJid = chatJid;
    }

    /**
     * Updates whether the message was sent by the logged-in user.
     *
     * @param fromMe the new flag, or {@code null} to clear
     */
    public void setFromMe(Boolean fromMe) {
        this.fromMe = fromMe;
    }

    /**
     * Updates the application-level identifier of the message.
     *
     * @param id the new identifier, or {@code null} to clear
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Updates the sender JID.
     *
     * @param senderJid the new sender JID, or {@code null} to clear
     */
    public void setSenderJid(Jid senderJid) {
        this.senderJid = senderJid;
    }
}
