package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.MarketingMessageBroadcastBuilder;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.business.MarketingMessageBroadcastAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Applies the {@code marketingMessageBroadcast} app-state sync action that
 * tags an outgoing message as belonging to a premium message template.
 *
 * <p>This handler backs the SMB premium-message tracking surface: when a
 * marketing template is broadcast, each recipient send is tagged with the
 * template's premium message id and the resulting association fans out across
 * the {@link SyncPatchType#REGULAR} collection so companion devices can
 * attribute the send to the template. The mutation index encodes both ids,
 * formatted as
 * {@snippet :
 *     ["marketingMessageBroadcast", premiumMessageId, messageId]
 * }
 *
 * @implNote
 * This implementation persists each association eagerly through
 * {@link LinkedWhatsAppBusinessStore#putMarketingMessageBroadcast(com.github.auties00.cobalt.wire.linked.business.MarketingMessageBroadcast)}
 * keyed by the sent message id, with the premium template id stored as the
 * record's status field. WA Web batches the pairs and mutates the
 * {@code sentMessageIds} set on each premium template once at the end of the
 * batch; Cobalt's {@link MarketingMessageBroadcastAction} protobuf does not
 * carry {@code sentMessageIds}, so a side map is used instead. Per Cobalt's
 * pluggable error model, exceptions propagate to the orchestrator instead of
 * being mapped to {@link MutationApplicationResult#failed()} inline.
 */
@WhatsAppWebModule(moduleName = "WAWebPremiumMessageBroadcastSync")
public final class MarketingMessageBroadcastHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link MarketingMessageBroadcastHandler}.
     */
    private static final System.Logger LOGGER = Log.get(MarketingMessageBroadcastHandler.class);

    /**
     * Constructs a new {@link MarketingMessageBroadcastHandler} for
     * registration in the sync handler registry.
     */
    @WhatsAppWebExport(moduleName = "WAWebPremiumMessageBroadcastSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public MarketingMessageBroadcastHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPremiumMessageBroadcastSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return MarketingMessageBroadcastAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPremiumMessageBroadcastSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return MarketingMessageBroadcastAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPremiumMessageBroadcastSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return MarketingMessageBroadcastAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The index must carry both the premium message id and the sent
     * message id; only {@link SyncdOperation#SET} is accepted. When the
     * referenced premium template is unknown locally the mutation is reported
     * as {@link MutationApplicationResult#orphan()}; otherwise the
     * association is persisted via
     * {@link LinkedWhatsAppBusinessStore#putMarketingMessageBroadcast(com.github.auties00.cobalt.wire.linked.business.MarketingMessageBroadcast)}.
     *
     * @implNote
     * This implementation classifies a missing index slot as
     * {@link MutationApplicationResult#malformed()} explicitly so an
     * out-of-bounds {@code JSON.parseArray} access does not surface as
     * {@link MutationApplicationResult#failed()}. The association is stored
     * eagerly because Cobalt's underlying storage is a flat key/value map and
     * there is no per-entity set to mutate.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPremiumMessageBroadcastSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var indexArray = JSON.parseArray(mutation.index());
        if (indexArray.size() <= 2) {
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }
        var premiumMessageId = indexArray.getString(1);
        var messageId = indexArray.getString(2);
        if (premiumMessageId == null || premiumMessageId.isEmpty()
                || messageId == null || messageId.isEmpty()) {
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }

        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        if (client.store().businessStore().findMarketingMessage(premiumMessageId).isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "marketing message broadcast orphaned, template {0} not found", premiumMessageId);
            return MutationApplicationResult.orphan();
        }

        client.store().businessStore().putMarketingMessageBroadcast(new MarketingMessageBroadcastBuilder()
                .templateId(messageId)
                .status(premiumMessageId)
                .build());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "marketing message broadcast recorded, message={0} template={1}", messageId, premiumMessageId);
        return MutationApplicationResult.success();
    }
}
