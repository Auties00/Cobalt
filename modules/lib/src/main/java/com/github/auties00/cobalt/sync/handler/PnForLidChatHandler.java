package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.PnForLidChatAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Applies the {@code pnForLidChat} app-state action that registers a
 * bidirectional phone number / LID mapping for a chat.
 *
 * <p>Drives the phone-number-hiding (PNH) migration: each mutation carries a
 * {@code (lidJid, pnJid)} pair so all linked devices learn the phone-number
 * JID associated with a previously-LID-only chat. The action is gated on
 * {@link ABProp#PNH_PN_FOR_LID_CHAT_SYNC}; non-opted-in accounts surface every
 * mutation as {@link MutationApplicationResult#unsupported()}. The mutation
 * index keys each entry by the LID, formatted as {@snippet :
 *     ["pnForLidChat", lidJid]
 * }
 *
 * <p>Only {@link SyncdOperation#SET} is accepted and on success the
 * bidirectional pair is registered on the store. An unparseable or non-LID
 * index is reported as
 * {@link SyncdIndexUtils#malformedActionIndex(String, String)} and a missing
 * {@link PnForLidChatAction#pnJid()} as
 * {@link SyncdIndexUtils#malformedActionValue(String)}.
 *
 * @implNote
 * This implementation registers each mapping eagerly via
 * {@code LinkedWhatsAppStore.registerLidMapping(pnJid, lidJid)} as the mutations
 * arrive, rather than batching them and calling
 * {@code WAWebDBCreateLidPnMappings.createLidPnMappings} once at the end of the
 * batch as WA Web does; the typed Cobalt store is the sole source of truth so
 * the per-mutation register call is semantically equivalent.
 */
@WhatsAppWebModule(moduleName = "WAWebPnForLidChatSync")
public final class PnForLidChatHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link PnForLidChatHandler}.
     */
    private static final System.Logger LOGGER = Log.get(PnForLidChatHandler.class);

    /**
     * Holds the AB-props service consulted before applying any mutation.
     */
    private final ABPropsService abPropsService;

    /**
     * Constructs the PN-for-LID-chat sync handler bound to the given AB-props
     * service.
     *
     * @param abPropsService the AB-props service consulted on every mutation
     */
    @WhatsAppWebExport(moduleName = "WAWebPnForLidChatSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public PnForLidChatHandler(ABPropsService abPropsService) {
        this.abPropsService = abPropsService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPnForLidChatSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return PnForLidChatAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPnForLidChatSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return PnForLidChatAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPnForLidChatSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return PnForLidChatAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation walks the per-mutation arms of WA Web's
     * {@code WAWebPnForLidChatSync.applyMutations} in this order: the
     * {@link ABProp#PNH_PN_FOR_LID_CHAT_SYNC} gate; then
     * {@link SyncdOperation#SET}; then a non-empty {@code indexParts[1]}; then
     * a {@link PnForLidChatAction} payload with a non-{@code null} {@code pnJid}
     * (mirroring WA Web's {@code isWidlike} guards); then the index JID must be
     * a LID ({@link Jid#hasLidServer()}, mirroring WA Web's
     * {@code createUserLidOrThrow}). On success the bidirectional pair is
     * registered eagerly via {@code LinkedWhatsAppStore.registerLidMapping}; the WA
     * Web batch flush, the per-batch {@code WARN} counters, and the
     * frontend-only {@code bulkUpdatePhoneNumberJids} dispatch are not
     * replicated.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPnForLidChatSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (!abPropsService.getBool(ABProp.PNH_PN_FOR_LID_CHAT_SYNC)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "pn for lid chat: unsupported, ab-prop gate closed");
            return MutationApplicationResult.unsupported();
        }

        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        var indexArray = JSON.parseArray(mutation.index());
        var lidJidString = indexArray != null && indexArray.size() > 1 ? indexArray.getString(1) : null;
        if (lidJidString == null || lidJidString.isEmpty()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "pn for lid chat mutation malformed: missing lid index");
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof PnForLidChatAction action)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "pn for lid chat mutation malformed: missing action value");
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        var pnJid = action.pnJid().orElse(null);
        if (pnJid == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "pn for lid chat mutation malformed: missing pn jid");
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        var lidJid = Jid.of(lidJidString);
        if (!lidJid.hasLidServer()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "pn for lid chat mutation malformed: index is not a lid");
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }

        client.store().contactStore().registerLidMapping(pnJid, lidJid);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "pn for lid chat: registered mapping pn={0} lid={1}", pnJid, lidJid);
        return MutationApplicationResult.success();
    }
}
