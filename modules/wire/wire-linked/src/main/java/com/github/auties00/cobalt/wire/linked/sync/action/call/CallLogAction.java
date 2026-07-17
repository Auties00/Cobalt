package com.github.auties00.cobalt.wire.linked.sync.action.call;

import com.github.auties00.cobalt.wire.linked.call.CallLog;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;

import java.util.Optional;

/**
 * App state sync action that records a call log entry across a user's linked
 * devices.
 *
 * <p>WhatsApp uses this action to replicate the history of voice and video
 * calls so that, when a call is placed or received on one device, every
 * other device linked to the same account sees the corresponding entry in
 * its call history. The payload carries a {@link CallLog} describing the
 * participants, direction, state, and timing of the call.
 *
 * <p>The action is identified on the wire by {@link #ACTION_NAME} at
 * {@link #ACTION_VERSION} and travels on the {@link #COLLECTION_NAME}
 * collection. Its mutation index is keyed by the caller JID, the call
 * identifier, and whether the call was outgoing, as encoded by
 * {@link CallLogActionArgs}.
 */
@ProtobufMessage(name = "SyncActionValue.CallLogAction")
public final class CallLogAction implements SyncAction<CallLogActionArgs> {
    /**
     * Canonical WhatsApp action name for call log entries, matching the
     * first element of the encoded mutation index used on the wire.
     */
    public static final String ACTION_NAME = "call_log";

    /**
     * Schema version declared by this action, used by handlers to gate
     * deserialisation of newer payload shapes.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * App state sync collection that carries this action, which is the
     * regular (non critical) collection used for ordinary user history.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR;

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
     * Structured call log payload describing the participants, direction,
     * state, and timing of the call being recorded.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    CallLog log;


    /**
     * Constructs a new call log action with the given payload.
     *
     * <p>This constructor is package-private and used by the generated
     * protobuf builder. External callers should use
     * {@code CallLogActionBuilder} to create instances.
     *
     * @param log the call log payload, or {@code null} if absent
     */
    CallLogAction(CallLog log) {
        this.log = log;
    }

    /**
     * Returns the call log payload carried by this action, if any.
     *
     * <p>The payload describes the call being replicated to every linked
     * device, including participants, direction, call state, and timing.
     *
     * @return an {@link Optional} wrapping the {@link CallLog}, or
     *         {@code Optional.empty()} if no payload is set
     */
    public Optional<CallLog> log() {
        return Optional.ofNullable(log);
    }

    /**
     * Replaces the call log payload carried by this action.
     *
     * @param log the new call log payload, or {@code null} to clear the
     *            existing payload
     */
    public void setLog(CallLog log) {
        this.log = log;
    }



}
