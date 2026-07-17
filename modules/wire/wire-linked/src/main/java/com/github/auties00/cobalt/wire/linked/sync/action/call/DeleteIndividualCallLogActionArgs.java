package com.github.auties00.cobalt.wire.linked.sync.action.call;


import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments that uniquely identify a
 * {@link DeleteIndividualCallLogAction} mutation within the app state sync
 * stream.
 *
 * <p>Every individual call log deletion is keyed by the same triple that
 * identifies the original call entry, so that the deletion replicates to
 * exactly the same record on every linked device. The triple pairs the
 * remote participant's JID with the call identifier and the direction of
 * the call.
 *
 * @param callerJid the JID of the remote participant the call is associated
 *                  with
 * @param callId    the unique call identifier assigned when the call was
 *                  placed or received
 * @param fromMe    {@code true} if the call was initiated by the current
 *                  user, {@code false} if it was received
 */
public record DeleteIndividualCallLogActionArgs(Jid callerJid, String callId, boolean fromMe) implements SyncActionArgs {
    /**
     * Returns the trailing index arguments encoded as strings in the order
     * expected by the individual call log deletion key.
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
