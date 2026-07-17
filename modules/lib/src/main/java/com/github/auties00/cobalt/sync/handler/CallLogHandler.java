package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.call.CallLogAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppChatStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Maintains the cross-device VoIP call history from inbound {@code call_log} app-state mutations.
 *
 * <p>This is the calls inbound counterpart of the end-of-call output produced by
 * {@link com.github.auties00.cobalt.calls.telemetry.CallLogSync}: when a call ends, is rejected, or is
 * missed on any device of the account,
 * the originating device pushes a {@code call_log} mutation, the server replays it onto every other
 * device, and the dispatcher routes the decoded mutation here. A {@link SyncdOperation#SET} carries a
 * {@link CallLogAction} whose {@link CallLogAction#log()} record is mirrored into the runtime call-history
 * table through
 * {@link LinkedWhatsAppChatStore#addCallLog(com.github.auties00.cobalt.wire.linked.call.CallLog)},
 * keyed by the record's own {@link com.github.auties00.cobalt.wire.linked.call.CallLog#callId()}; a
 * {@link SyncdOperation#REMOVE} drops the record named by the {@code callId} in index slot two through
 * {@link LinkedWhatsAppChatStore#removeCallLog(String)}.
 *
 * <p>The four-element mutation index is {@code ["call_log", callerJid, callId, fromMe]}; this handler
 * revalidates that the index carries at least the four segments and that the {@link CallLogAction#log()}
 * record carries a non-empty call id before storing, matching the build side
 * {@link com.github.auties00.cobalt.calls.telemetry.CallLogSync} writes.
 *
 * @implNote This implementation reproduces the inbound half of the engine call-log host-event seam (the
 * native {@code send_1to1_call_log_update_event} host event {@code 0x8a} in {@code events.cc}) as a plain
 * app-state handler. It keys the runtime table by the
 * record-internal {@link com.github.auties00.cobalt.wire.linked.call.CallLog#callId()} rather than by WA Web's
 * composite {@code callerJid|callId|fromMe} index, and rejects a SET whose record carries no call id as
 * {@link MutationApplicationResult#malformed()}, because the store has no composite key to fall back to.
 * The WA Web pairing-timestamp filter and the one-minute {@code shouldHideInConversation} window are
 * dropped because they only govern whether the call renders inline in a conversation view, which Cobalt
 * does not model. The store-side {@code callLogStates} table is runtime-only and is rebuilt
 * deterministically from these mutations plus the history-sync bootstrap, so it stays consistent across
 * restarts without a local snapshot.
 */
@WhatsAppWebModule(moduleName = "WAWebCallLogSync")
public final class CallLogHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link CallLogHandler}.
     */
    private static final System.Logger LOGGER = Log.get(CallLogHandler.class);

    /**
     * The slot in the parsed {@code call_log} index that carries the call id.
     *
     * <p>The index is {@code ["call_log", callerJid, callId, fromMe]}, so the call id is the third
     * element; the REMOVE path reads the call id from this slot to drop the matching record.
     */
    private static final int CALL_ID_INDEX = 2;

    /**
     * The minimum number of elements a well-formed {@code call_log} index carries.
     *
     * <p>A valid index is the four-tuple {@code ["call_log", callerJid, callId, fromMe]}; an index with
     * fewer elements is rejected as malformed.
     */
    private static final int MINIMUM_INDEX_LENGTH = 4;

    /**
     * Constructs the call-log app-state handler.
     *
     * <p>The handler holds no per-call state and reaches the store through the {@code client} argument of
     * {@link #applyMutation(LinkedWhatsAppClient, DecryptedMutation.Trusted)}, so a single instance is
     * shared for the lifetime of the client; the sync handler registry instantiates it exactly once.
     */
    @WhatsAppWebExport(moduleName = "WAWebCallLogSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public CallLogHandler() {

    }

    /**
     * {@inheritDoc}
     *
     * @return the {@code call_log} action name {@link CallLogAction#ACTION_NAME}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebCallLogSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return CallLogAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link SyncPatchType#REGULAR} collection the {@code call_log} action rides
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebCallLogSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return CallLogAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@code call_log} action version {@link CallLogAction#ACTION_VERSION}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebCallLogSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return CallLogAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For a {@link SyncdOperation#SET} mutation, validates that the action is a {@link CallLogAction}
     * carrying a {@link CallLogAction#log()} record with a non-empty
     * {@link com.github.auties00.cobalt.wire.linked.call.CallLog#callId()}, validates that the index carries at
     * least the four segments {@code ["call_log", callerJid, callId, fromMe]}, and mirrors the record into
     * the runtime call-history table through
     * {@link LinkedWhatsAppChatStore#addCallLog(com.github.auties00.cobalt.wire.linked.call.CallLog)}.
     * For a {@link SyncdOperation#REMOVE} mutation, reads the call id from index slot
     * {@value #CALL_ID_INDEX} and drops the matching record through
     * {@link LinkedWhatsAppChatStore#removeCallLog(String)}. Any other operation
     * reports {@link MutationApplicationResult#unsupported()}, a structurally invalid SET reports
     * {@link MutationApplicationResult#malformed()}, and a thrown exception is contained as
     * {@link MutationApplicationResult#failed()} so a single bad call-log mutation never aborts the patch.
     *
     * @implNote
     * This implementation keys the store by the record-internal call id and treats a SET with a missing
     * or empty call id as {@link MutationApplicationResult#malformed()}, because the store has no composite
     * {@code callerJid|callId|fromMe} key to fall back to as WA Web does.
     *
     * @param client   the {@link LinkedWhatsAppClient} whose store the mutation is applied to
     * @param mutation the decoded, trusted mutation to apply
     * @return the per-mutation application outcome
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebCallLogSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        try {
            if (mutation.operation() == SyncdOperation.SET) {
                if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof CallLogAction action)) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "call log mutation malformed: missing action value");
                    return MutationApplicationResult.malformed();
                }

                var log = action.log().orElse(null);
                if (log == null) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "call log mutation malformed: missing log record");
                    return MutationApplicationResult.malformed();
                }

                var indexArray = JSON.parseArray(mutation.index());
                if (indexArray == null || indexArray.size() < MINIMUM_INDEX_LENGTH) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "call log mutation malformed: index too short");
                    return MutationApplicationResult.malformed();
                }

                if (log.callId().isEmpty()) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "call log mutation malformed: missing call id");
                    return MutationApplicationResult.malformed();
                }

                client.store().chatStore().addCallLog(log);
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call log added: id={0}", log.callId().orElse(null));
                return MutationApplicationResult.success();
            }

            if (mutation.operation() == SyncdOperation.REMOVE) {
                var indexArray = JSON.parseArray(mutation.index());
                if (indexArray != null && indexArray.size() >= MINIMUM_INDEX_LENGTH) {
                    var callId = indexArray.getString(CALL_ID_INDEX);
                    if (callId != null) {
                        client.store().chatStore().removeCallLog(callId);
                        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call log removed: id={0}", callId);
                    }
                }
                return MutationApplicationResult.success();
            }

            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call log mutation unsupported: operation={0}", mutation.operation());
            return MutationApplicationResult.unsupported();
        } catch (Exception exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "call log mutation failed", exception);
            return MutationApplicationResult.failed();
        }
    }
}
