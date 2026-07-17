package com.github.auties00.cobalt.wire.linked.sync.action.chat;


import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments for a {@link DeleteMessageForMeAction}.
 *
 * <p>A delete-for-me operation targets a specific message identified by the
 * standard WhatsApp message key: remote JID, message id, from-me flag and
 * optional participant. The participant is only meaningful in groups when
 * the user did not author the message.
 *
 * <p>Boolean values are encoded as {@code "1"}/{@code "0"} and a missing
 * participant is encoded as the literal {@code "0"}. The encoded index is
 * {@code ["deleteMessageForMe", remote, id, fromMe, participant]}.
 *
 * @param remote      the remote chat {@link Jid}
 * @param id          the message identifier
 * @param fromMe      {@code true} when the message was sent by the current user
 * @param participant the participant {@link Jid} in group chats, or
 *                    {@code null} when not applicable
 */
public record DeleteMessageForMeActionArgs(Jid remote, String id, boolean fromMe, Jid participant) implements SyncActionArgs {
    /**
     * Converts this record into the tail portion of the sync index array.
     *
     * <p>The participant slot is filled with {@code "0"} when the message
     * was sent by the current user or when no participant is available.
     *
     * @return a four-element array encoding the message key
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
