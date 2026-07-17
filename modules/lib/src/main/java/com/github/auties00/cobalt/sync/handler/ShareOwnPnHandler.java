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
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Records that a LID-identified contact may now see the local user's phone
 * number.
 *
 * <p>The sync dispatcher routes incoming {@code shareOwnPn} mutations here
 * whenever the user promotes a LID-only contact to phone-number-visible. The
 * handler flips
 * {@link com.github.auties00.cobalt.wire.linked.contact.Contact#isPhoneNumberShared()}
 * on the matching contact, upserting the contact when it is not yet in the
 * local store.
 */
@WhatsAppWebModule(moduleName = "WAWebShareOwnPnSync")
public final class ShareOwnPnHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link ShareOwnPnHandler}.
     */
    private static final System.Logger LOGGER = Log.get(ShareOwnPnHandler.class);

    /**
     * The AB-props service consulted before applying any mutation.
     */
    private final ABPropsService abPropsService;

    /**
     * Constructs the handler.
     *
     * <p>The handler is stateful only through the injected {@link ABPropsService};
     * Cobalt's sync registry holds a single instance per client.
     *
     * @param abPropsService the AB-props service consulted on every mutation
     */
    @WhatsAppWebExport(moduleName = "WAWebShareOwnPnSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public ShareOwnPnHandler(ABPropsService abPropsService) {
        this.abPropsService = abPropsService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebShareOwnPnSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return "shareOwnPn";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebShareOwnPnSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return SyncPatchType.REGULAR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebShareOwnPnSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return 8;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The mutation is gated on {@link ABProp#SHARE_OWN_PN_SYNC} and requires a
     * {@link SyncdOperation#SET}; either gate failing reports
     * {@link MutationApplicationResult#unsupported()}. Index slot {@code [1]} is
     * read as a wid-like string and parsed into a {@link Jid} that must carry the
     * LID server, reporting malformed otherwise. The matching LID contact is then
     * upserted with {@code phoneNumberShared = true}.
     *
     * @implNote
     * WA Web accumulates valid entries across the whole batch and flushes them
     * once through {@code WAWebUpdateLidMetadataJob}; Cobalt collapses that
     * pipeline into a direct per-mutation contact update because the unified
     * {@link LinkedWhatsAppStore} is itself the source
     * of truth and there is no IDB layer to batch.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebShareOwnPnSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (!abPropsService.getBool(ABProp.SHARE_OWN_PN_SYNC)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "share own pn: unsupported, ab-prop gate closed");
            return MutationApplicationResult.unsupported();
        }

        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        var indexArray = JSON.parseArray(mutation.index());
        var lidJidString = indexArray != null && indexArray.size() > 1 ? indexArray.getString(1) : null;
        if (lidJidString == null || lidJidString.isEmpty()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "share own pn mutation malformed: missing lid index");
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }

        var lidJid = Jid.of(lidJidString);
        if (!lidJid.hasLidServer()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "share own pn mutation malformed: index is not a lid");
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }

        var contact = client.store().contactStore().findContactByJid(lidJid)
                .orElseGet(() -> client.store().contactStore().addNewContact(lidJid));
        contact.setPhoneNumberShared(true);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "share own pn: marked phone number shared for lid={0}", lidJid);
        return MutationApplicationResult.success();
    }
}
