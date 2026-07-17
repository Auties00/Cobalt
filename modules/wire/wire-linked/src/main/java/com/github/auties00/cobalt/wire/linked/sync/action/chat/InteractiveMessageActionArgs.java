package com.github.auties00.cobalt.wire.linked.sync.action.chat;


import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments for an {@link InteractiveMessageAction}.
 *
 * <p>The index extends the standard message key (remote, id, from-me,
 * participant) with an extra element that identifies the specific
 * interactive element (for example, a CTA button) that was acted upon.
 * This allows independent interactions with multiple buttons of the same
 * message to coexist in the app state store without colliding.
 *
 * <p>Boolean values are encoded as {@code "1"}/{@code "0"} and a missing
 * participant is encoded as the literal {@code "0"}. The encoded index is
 * {@code ["interactive_message_action", remote, id, fromMe, participant,
 * interactionId]}.
 *
 * @param remote        the remote chat {@link Jid}
 * @param id            the message identifier
 * @param fromMe        {@code true} when the message was sent by the current user
 * @param participant   the participant {@link Jid} in group chats, or
 *                      {@code null} when not applicable
 * @param interactionId identifier of the interactive element (for example a
 *                      CTA button) within the message
 */
public record InteractiveMessageActionArgs(Jid remote, String id, boolean fromMe, Jid participant, String interactionId) implements SyncActionArgs {
    /**
     * Converts this record into the tail portion of the sync index array.
     *
     * @return a five-element array encoding the message key followed by the
     *         interaction identifier
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{
                remote.toString(),
                id,
                fromMe ? "1" : "0",
                participant != null && !fromMe ? participant.toString() : "0",
                interactionId
        };
    }
}
