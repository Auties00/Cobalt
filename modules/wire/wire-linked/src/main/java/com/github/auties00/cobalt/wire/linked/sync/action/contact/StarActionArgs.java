package com.github.auties00.cobalt.wire.linked.sync.action.contact;


import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments that locate a specific {@link StarAction} inside a sync
 * patch.
 *
 * <p>A starred message is addressed by the components of the original
 * message key: the chat JID, the message identifier, whether the message was
 * sent by the current user, and (for group messages) the participant JID of
 * the sender. When building or reading a patch the sync engine translates
 * these arguments into an index tuple where {@code fromMe} is encoded as
 * {@code "1"} or {@code "0"} and the {@code participant} slot is
 * {@code "0"} when it is not relevant (the message is from the current user
 * or the participant is absent).
 *
 * @param remote      the JID of the chat where the message was exchanged
 * @param id          the identifier of the message
 * @param fromMe      {@code true} if the message was sent by the current
 *                    user, {@code false} if it was received
 * @param participant for group messages received from someone else, the JID
 *                    of the participant that sent the message; may be
 *                    {@code null} for direct chats or outgoing messages
 */
public record StarActionArgs(Jid remote, String id, boolean fromMe, Jid participant) implements SyncActionArgs {
    /**
     * Returns the index components used by the sync engine to address this
     * starred message.
     *
     * @return a four-element array encoding the message key in the order
     *         expected by the sync engine
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{
                remote.toString(),
                id,
                fromMe ? "1" : "0",
                participant != null && !fromMe ? participant.toString() : "0"
        };
    }
}
