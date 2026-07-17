package com.github.auties00.cobalt.wire.linked.sync.action.call;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * App state sync action that removes a single call log entry from the
 * history of every device linked to the current user.
 *
 * <p>When the user deletes a specific call entry from the call log on one
 * device, WhatsApp replicates the deletion to its other devices by
 * emitting this action. The action carries the identity of the remote
 * participant and the direction of the call so that the matching entry
 * can be located and removed from every local call log.
 *
 * <p>The action is identified on the wire by {@link #ACTION_NAME} at
 * {@link #ACTION_VERSION}. Its mutation index is produced by
 * {@link DeleteIndividualCallLogActionArgs}.
 */
@ProtobufMessage(name = "SyncActionValue.DeleteIndividualCallLogAction")
public final class DeleteIndividualCallLogAction implements SyncAction<DeleteIndividualCallLogActionArgs> {
    /**
     * Canonical WhatsApp action name for individual call log deletions,
     * matching the first element of the encoded mutation index used on
     * the wire.
     */
    public static final String ACTION_NAME = "delete_individual_call_log";

    /**
     * Schema version declared by this action, used by handlers to gate
     * deserialisation of newer payload shapes.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * Returns the canonical action name used as the first element of the
     * encoded index array for this action.
     *
     * @return the canonical action name {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version declared by this action.
     *
     * @return the action schema version {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * JID of the remote participant whose call log entry is being deleted.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid peerJid;

    /**
     * Direction flag of the call being deleted, where {@code true} marks
     * an incoming call and {@code false} marks an outgoing one.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    Boolean isIncoming;


    /**
     * Constructs a new individual call log deletion action for the given
     * participant and direction.
     *
     * <p>This constructor is package-private and used by the generated
     * protobuf builder. External callers should use
     * {@code DeleteIndividualCallLogActionBuilder} to create instances.
     *
     * @param peerJid    the JID of the remote participant, or {@code null}
     *                   if absent
     * @param isIncoming the direction flag, or {@code null} if absent
     */
    DeleteIndividualCallLogAction(Jid peerJid, Boolean isIncoming) {
        this.peerJid = peerJid;
        this.isIncoming = isIncoming;
    }

    /**
     * Returns the JID of the remote participant whose call log entry is
     * being removed, if any.
     *
     * @return an {@link Optional} wrapping the peer JID, or
     *         {@code Optional.empty()} if no participant is set
     */
    public Optional<Jid> peerJid() {
        return Optional.ofNullable(peerJid);
    }

    /**
     * Returns whether the call being deleted was an incoming call.
     *
     * <p>A missing direction flag is interpreted as outgoing, consistent
     * with the default representation on the wire.
     *
     * @return {@code true} if the call being deleted was incoming,
     *         {@code false} if it was outgoing or unspecified
     */
    public boolean isIncoming() {
        return isIncoming != null && isIncoming;
    }

    /**
     * Replaces the JID of the remote participant whose call log entry is
     * being removed.
     *
     * @param peerJid the new peer JID, or {@code null} to clear the
     *                existing value
     */
    public void setPeerJid(Jid peerJid) {
        this.peerJid = peerJid;
    }

    /**
     * Replaces the direction flag of the call being deleted.
     *
     * @param isIncoming the new direction flag, or {@code null} to clear
     *                   the existing value
     */
    public void setIncoming(Boolean isIncoming) {
        this.isIncoming = isIncoming;
    }


}
