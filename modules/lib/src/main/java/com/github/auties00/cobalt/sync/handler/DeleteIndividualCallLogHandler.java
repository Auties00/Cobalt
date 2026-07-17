package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.call.CallLog;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.call.CallLogAction;
import com.github.auties00.cobalt.wire.linked.sync.action.call.DeleteIndividualCallLogAction;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppChatStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.util.List;

/**
 * Removes every cross-device call-history entry for one peer and direction on a
 * {@code delete_individual_call_log} app-state mutation.
 *
 * <p>When the user clears a contact's call entries from the call tab on one device, the originating
 * device pushes a {@link DeleteIndividualCallLogAction} carrying the remote
 * {@linkplain DeleteIndividualCallLogAction#peerJid() peer JID} and the call
 * {@linkplain DeleteIndividualCallLogAction#isIncoming() direction}; the server replays it to every other
 * device, and this handler bulk-removes the matching entries from the runtime call-history table. It is
 * distinct from the per-call-id {@link com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation#REMOVE}
 * path of {@link CallLogHandler}, which drops one record by call id: this action targets a whole
 * peer-plus-direction group at once.
 *
 * <p>Because the runtime table {@link LinkedWhatsAppChatStore#callLogStates()} is keyed
 * by call id rather than by peer, the handler resolves the target set by scanning the table and removing
 * every {@link CallLog} whose {@linkplain CallLog#isIncoming() direction} equals the action's direction and
 * whose remote party (the {@linkplain CallLog#callCreatorJid() creator} for an incoming call, or any
 * {@linkplain CallLog#participants() participant} otherwise) resolves to the action's peer JID. JID
 * comparison bridges the hosted-domain boundaries through {@link Jid#isSameAccount(Jid)} so a record
 * written under a hosted domain still matches a deletion keyed under the underlying user domain.
 *
 * @implNote This implementation is the inbound handler for the {@code delete_individual_call_log} action,
 * which the legacy Cobalt sync handler set never wired (only the per-call-id {@code call_log} action was
 * handled); the calls package adds it for cross-device parity, matching the WASM call-log table semantics where a
 * by-peer purge removes the engine's whole linked-list run for that peer. The action carries no call id of
 * its own, so the by-call-id store key cannot be used directly; the table scan is the deliberate Cobalt
 * adaptation of WA Web's per-peer deletion. The {@code callLogStates} table is runtime-only and rebuilt
 * from app-state plus history-sync, so removing entries here keeps every device's call tab consistent
 * without touching a persisted snapshot.
 */
@WhatsAppWebModule(moduleName = "WAWebCallLogSync")
public final class DeleteIndividualCallLogHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link DeleteIndividualCallLogHandler}.
     */
    private static final System.Logger LOGGER = Log.get(DeleteIndividualCallLogHandler.class);

    /**
     * Constructs the individual-call-log-deletion app-state handler.
     *
     * <p>The handler holds no per-call state and reaches the store through the {@code client} argument of
     * {@link #applyMutation(LinkedWhatsAppClient, DecryptedMutation.Trusted)}, so a single instance is
     * shared for the lifetime of the client; the sync handler registry instantiates it exactly once.
     */
    public DeleteIndividualCallLogHandler() {

    }

    /**
     * {@inheritDoc}
     *
     * @return the {@code delete_individual_call_log} action name {@link DeleteIndividualCallLogAction#ACTION_NAME}
     */
    @Override
    public String actionName() {
        return DeleteIndividualCallLogAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The individual-call-log-deletion action shares the {@link SyncPatchType#REGULAR} collection with
     * the {@code call_log} action it deletes from.
     *
     * @return the {@link SyncPatchType#REGULAR} collection
     */
    @Override
    public SyncPatchType collectionName() {
        return CallLogAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@code delete_individual_call_log} action version
     *         {@link DeleteIndividualCallLogAction#ACTION_VERSION}
     */
    @Override
    public int version() {
        return DeleteIndividualCallLogAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Applies the deletion regardless of the wire operation, because the action itself denotes a
     * removal: it validates that the action is a {@link DeleteIndividualCallLogAction} carrying a present
     * {@linkplain DeleteIndividualCallLogAction#peerJid() peer JID}, then bulk-removes every matching
     * {@link CallLog} through {@link #removeMatching(LinkedWhatsAppClient, Jid, boolean)}. A missing peer
     * JID reports {@link MutationApplicationResult#malformed()}; an action of any other type reports
     * {@link MutationApplicationResult#malformed()} as well; a thrown exception is contained as
     * {@link MutationApplicationResult#failed()} so a single bad deletion never aborts the patch.
     *
     * @implNote
     * This implementation applies on both {@link com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation#SET}
     * and {@link com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation#REMOVE} because the action body
     * is self-describing: the {@code delete_individual_call_log} action denotes a removal regardless of the
     * wire operation, so it is not gated on the operation as the {@code call_log} handler is.
     *
     * @param client   the {@link LinkedWhatsAppClient} whose store the deletion is applied to
     * @param mutation the decoded, trusted mutation to apply
     * @return the per-mutation application outcome
     */
    @Override
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        try {
            if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof DeleteIndividualCallLogAction action)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "delete individual call log: mutation value is not a DeleteIndividualCallLogAction");
                return MutationApplicationResult.malformed();
            }

            var peer = action.peerJid().orElse(null);
            if (peer == null) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "delete individual call log: missing peer jid");
                return MutationApplicationResult.malformed();
            }

            removeMatching(client, peer, action.isIncoming());
            return MutationApplicationResult.success();
        } catch (Exception exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "delete individual call log: failed to apply mutation", exception);
            return MutationApplicationResult.failed();
        }
    }

    /**
     * Removes every call-history entry whose direction and remote party match the deletion target.
     *
     * <p>Snapshots the current {@link LinkedWhatsAppChatStore#callLogStates()} into a
     * defensive copy, then for every entry whose {@linkplain CallLog#isIncoming() direction} equals
     * {@code isIncoming} and whose remote party matches {@code peer} (through
     * {@link #matchesPeer(CallLog, Jid)}) it drops the entry by its
     * {@linkplain CallLog#callId() call id} through
     * {@link LinkedWhatsAppChatStore#removeCallLog(String)}. Iterating the copy keeps
     * the scan independent of the concurrent removals against the live table.
     *
     * @param client     the {@link LinkedWhatsAppClient} whose store the deletion is applied to
     * @param peer       the remote peer JID whose entries are being removed
     * @param isIncoming the call direction to match, {@code true} for incoming calls
     */
    private void removeMatching(LinkedWhatsAppClient client, Jid peer, boolean isIncoming) {
        var chatStore = client.store().chatStore();
        var removedCount = 0;
        for (var log : List.copyOf(chatStore.callLogStates())) {
            var callId = log.callId().orElse(null);
            if (callId == null) {
                continue;
            }
            if (log.isIncoming() == isIncoming && matchesPeer(log, peer)) {
                chatStore.removeCallLog(callId);
                removedCount++;
            }
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "delete individual call log: removed {0} entries for peer={1} incoming={2}", removedCount, peer, isIncoming);
    }

    /**
     * Returns whether the remote party of a call-history entry resolves to the deletion target peer.
     *
     * <p>The remote party of an incoming one-to-one call is the {@linkplain CallLog#callCreatorJid() call
     * creator}; for an outgoing or group call it appears in the {@linkplain CallLog#participants()
     * participant} list. The entry matches when its creator equals {@code peer}, or when any participant's
     * {@linkplain CallLog.ParticipantInfo#userJid() user JID} equals {@code peer}, with every comparison
     * performed through {@link Jid#isSameAccount(Jid)} so a record written under a hosted domain still
     * matches a deletion keyed under the underlying user domain.
     *
     * @param log  the call-history entry under test
     * @param peer the deletion target peer JID
     * @return {@code true} when the entry's remote party resolves to {@code peer}
     */
    private static boolean matchesPeer(CallLog log, Jid peer) {
        var creator = log.callCreatorJid().orElse(null);
        if (creator != null && creator.isSameAccount(peer)) {
            return true;
        }
        for (var participant : log.participants()) {
            var userJid = participant.userJid().orElse(null);
            if (userJid != null && userJid.isSameAccount(peer)) {
                return true;
            }
        }
        return false;
    }
}
