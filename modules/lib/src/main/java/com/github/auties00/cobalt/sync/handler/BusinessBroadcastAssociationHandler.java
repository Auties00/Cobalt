package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.BroadcastListParticipant;
import com.github.auties00.cobalt.wire.linked.business.BroadcastListParticipantBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastAssociationAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppContactStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

/**
 * Adds or removes a single recipient JID from a business broadcast list in response to a {@code broadcast_jid} sync mutation.
 *
 * <p>This handles the per-recipient diff of a Business broadcast list when one
 * recipient is added or removed on another device. The
 * {@link BusinessBroadcastAssociationAction#deleted()} flag inside the mutation
 * value distinguishes additions from removals; both arrive over the wire as
 * {@link SyncdOperation#SET}.
 *
 * @implNote
 * This implementation is forward-looking: WA Web reserves the
 * {@code broadcast_jid} action under
 * {@code WASyncdConst.Actions.BroadcastJid} and defines the
 * {@link BusinessBroadcastAssociationAction} protobuf in
 * {@code WAWebProtobufSyncAction.pb}, but the current snapshot does
 * not ship a {@code WAWebBroadcastJidSync} module nor register this
 * action in {@code WAWebCollectionHandlerActions.ActionHandlers}, so
 * WA Web does not currently dispatch incoming mutations of this
 * type. The Cobalt handler infers the per-recipient mutation from
 * the related {@code WAWebBroadcastListSync} (which manages the
 * parent broadcast list and its participant array). When recipient
 * JIDs are LID/HostedLID they are stored as {@code lidJid}
 * directly; phone-number recipients populate {@code pnJid} and
 * resolve {@code lidJid} via
 * {@link LinkedWhatsAppContactStore#findLidByPhone(Jid)},
 * falling back to the phone-number JID itself when no LID is
 * known.
 */
@WhatsAppWebModule(moduleName = "WAWebBroadcastListSync")
public final class BusinessBroadcastAssociationHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link BusinessBroadcastAssociationHandler}.
     */
    private static final System.Logger LOGGER = Log.get(BusinessBroadcastAssociationHandler.class);

    /**
     * Constructs the singleton broadcast-association handler.
     *
     * <p>The sync handler registry instantiates this type exactly once.
     */
    @WhatsAppWebExport(moduleName = "WAWebBroadcastListSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public BusinessBroadcastAssociationHandler() {

    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebBroadcastListSync", exports = "getAction", adaptation = WhatsAppAdaptation.ADAPTED)
    public String actionName() {
        return BusinessBroadcastAssociationAction.ACTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebBroadcastListSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPatchType collectionName() {
        return BusinessBroadcastAssociationAction.COLLECTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebBroadcastListSync", exports = "getVersion", adaptation = WhatsAppAdaptation.ADAPTED)
    public int version() {
        return BusinessBroadcastAssociationAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Validates the JSON index {@code ["broadcast_jid", listId, recipientJid]},
     * resolves the parent broadcast list from the store, removes any existing
     * entry for the recipient, and (when the action is not
     * {@link BusinessBroadcastAssociationAction#deleted()}) appends a fresh
     * {@link BroadcastListParticipant} entry. Returns
     * {@link MutationApplicationResult#unsupported()} for non-{@link SyncdOperation#SET}
     * operations, {@link MutationApplicationResult#malformed()} when the index
     * or value is shaped wrong, and an orphan result keyed by list id with model
     * type {@code "BroadcastList"} when the parent list is not in the store.
     *
     * @implNote
     * This implementation copies the existing participant array into a
     * mutable {@link ArrayList} before mutating because the model
     * exposes an unmodifiable {@link List}; the parent list is then
     * upserted via
     * {@link LinkedWhatsAppBusinessStore#putBusinessBroadcastList(com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastList)}.
     * Phone-number recipients resolve their LID via
     * {@link LinkedWhatsAppContactStore#findLidByPhone(Jid)},
     * falling back to the phone-number JID itself when no LID is known. An empty
     * participant array is normalized to {@code null} so the stored list shape
     * matches the wire shape.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebBroadcastListSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "broadcast association mutation unsupported: operation={0}", mutation.operation());
            return MutationApplicationResult.unsupported();
        }

        var indexArray = JSON.parseArray(mutation.index());
        if (indexArray.size() < 3) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "broadcast association mutation malformed: index size={0}", indexArray.size());
            return MutationApplicationResult.malformed();
        }

        var listId = indexArray.getString(1);
        var recipientJidString = indexArray.getString(2);
        if (listId == null || listId.isBlank() || recipientJidString == null || recipientJidString.isBlank()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "broadcast association mutation malformed: missing list id or recipient jid");
            return MutationApplicationResult.malformed();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof BusinessBroadcastAssociationAction action)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "broadcast association mutation malformed: missing action value");
            return MutationApplicationResult.malformed();
        }

        var existing = client.store().businessStore().findBusinessBroadcastList(listId).orElse(null);
        if (existing == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "broadcast association mutation orphaned: list not found id={0}", listId);
            return MutationApplicationResult.orphan(listId, "BroadcastList");
        }

        var recipientJid = Jid.of(recipientJidString);
        List<BroadcastListParticipant> participants = new ArrayList<>(existing.participants());
        participants.removeIf(participant ->
                recipientJid.equals(participant.lidJid())
                        || participant.pnJid().filter(recipientJid::equals).isPresent()
        );
        if (!action.deleted()) {
            BroadcastListParticipant participant;
            if (recipientJid.hasLidServer() || recipientJid.hasHostedLidServer()) {
                participant = new BroadcastListParticipantBuilder()
                        .lidJid(recipientJid)
                        .build();
            } else {
                participant = new BroadcastListParticipantBuilder()
                        .lidJid(client.store().contactStore().findLidByPhone(recipientJid).orElse(recipientJid))
                        .pnJid(recipientJid)
                        .build();
            }
            participants.add(participant);
        }

        existing.setParticipants(participants.isEmpty() ? null : participants);
        client.store().businessStore().putBusinessBroadcastList(existing);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "broadcast association applied: list={0} recipient={1} deleted={2}", listId, recipientJid, action.deleted());
        return MutationApplicationResult.success();
    }
}
