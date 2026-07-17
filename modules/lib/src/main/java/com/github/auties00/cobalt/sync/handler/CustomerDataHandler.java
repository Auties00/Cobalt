package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.business.CustomerDataAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Acknowledges Business CRM customer-data mutations from {@code customer_data} sync mutations without persisting them.
 *
 * <p>This handler drives the SMB Business CRM customer-record surface where
 * the merchant attaches CRM fields (contact type, email, alternate phone
 * numbers, birthday, address, acquisition source, lead stage, last order) to a
 * chat JID. When the merchant edits a record on another device, the server
 * replays the resulting {@link CustomerDataAction} here.
 *
 * @implNote
 * This implementation validates the index and value but does not
 * persist anything: Cobalt has no dedicated customer-data store
 * mirroring WA Web's {@code customerData2} IDB table or the
 * {@code addOrEditCustomerData} / {@code removeCustomerDataByChatJid}
 * pipeline. The mutation is acknowledged so the sync engine sees
 * {@link MutationApplicationResult#success()} and does not retry; the
 * payload is dropped.
 */
@WhatsAppWebModule(moduleName = "WAWebCustomerDataSync")
public final class CustomerDataHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link CustomerDataHandler}.
     */
    private static final System.Logger LOGGER = Log.get(CustomerDataHandler.class);

    /**
     * Constructs the singleton customer-data handler.
     *
     * <p>The sync handler registry instantiates this once during client
     * bootstrap.
     */
    @WhatsAppWebExport(moduleName = "WAWebCustomerDataSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public CustomerDataHandler() {
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebCustomerDataSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return CustomerDataAction.ACTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebCustomerDataSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return CustomerDataAction.COLLECTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebCustomerDataSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return CustomerDataAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For SET mutations, validates {@code indexParts[1]} as a chat JID and
     * the value as a {@link CustomerDataAction}. For REMOVE mutations,
     * validates the chat JID when present. Returns
     * {@link MutationApplicationResult#unsupported()} for other operations and
     * {@link SyncdIndexUtils#malformedActionValue(String)} when the index slot
     * is empty, the JID fails to parse, or the value is mistyped.
     *
     * @implNote
     * This implementation acknowledges valid mutations with
     * {@link MutationApplicationResult#success()} without persisting:
     * Cobalt has no
     * {@code addOrEditCustomerData} / {@code removeCustomerDataByChatJid}
     * equivalent. The {@link Jid#of(String)} call replaces WA Web's
     * {@code WAJids.validateChatJid} and is more lenient about
     * accepted forms; this is acceptable here because the payload is
     * dropped either way.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebCustomerDataSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var indexArray = JSON.parseArray(mutation.index());
        var chatJidString = indexArray.size() >= 2 ? indexArray.getString(1) : null;

        if (mutation.operation() == SyncdOperation.SET) {
            if (chatJidString == null || chatJidString.isBlank()) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "customer data: missing chat jid in mutation index");
                return SyncdIndexUtils.malformedActionValue(collectionName().name());
            }

            var chatJid = Jid.of(chatJidString);
            if (chatJid == null) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "customer data: chat jid failed to parse, jid={0}", new LogRedactable.User(chatJidString));
                return SyncdIndexUtils.malformedActionValue(collectionName().name());
            }

            if (mutation.value().isEmpty()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "customer data: acknowledging empty value for {0}, not persisted", chatJid);
                return MutationApplicationResult.success();
            }

            if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof CustomerDataAction)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "customer data: mutation value is not a CustomerDataAction");
                return SyncdIndexUtils.malformedActionValue(collectionName().name());
            }

            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "customer data: acknowledging set for {0}, not persisted", chatJid);
            return MutationApplicationResult.success();
        } else if (mutation.operation() == SyncdOperation.REMOVE) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "customer data: acknowledging remove, not persisted");
            return MutationApplicationResult.success();
        } else {
            return MutationApplicationResult.unsupported();
        }
    }
}
