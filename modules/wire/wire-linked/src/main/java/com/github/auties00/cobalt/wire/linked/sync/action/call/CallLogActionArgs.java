package com.github.auties00.cobalt.wire.linked.sync.action.call;


import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments that uniquely identify a {@link CallLogAction} mutation
 * within the app state sync stream.
 *
 * <p>WhatsApp keys every call log replication mutation by the other party's
 * JID, the call identifier assigned by the signalling layer, and the
 * direction of the call. Two mutations sharing the same triple refer to
 * the same call entry and are deduplicated or updated in place when the
 * sync protocol resolves conflicts across devices.
 *
 * @param callerJid the JID of the remote participant the call is associated
 *                  with
 * @param callId    the unique call identifier assigned when the call was
 *                  placed or received
 * @param fromMe    {@code true} if the call was initiated by the current
 *                  user, {@code false} if it was received
 */
public record CallLogActionArgs(Jid callerJid, String callId, boolean fromMe) implements SyncActionArgs {
    /**
     * Returns the trailing index arguments encoded as strings in the order
     * expected by the call log action key.
     *
     * <p>The resulting array encodes the direction flag as {@code "1"} for
     * outgoing calls and {@code "0"} for incoming calls to match the
     * canonical wire representation.
     *
     * @return a three-element array of the form
     *         {@code [callerJid, callId, direction]}
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{callerJid.toString(), callId, fromMe ? "1" : "0"};
    }
}
