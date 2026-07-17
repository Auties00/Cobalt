package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.MarketingMessageBuilder;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.business.MarketingMessageAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.time.Instant;

/**
 * Applies the {@code marketingMessage} app-state sync action that creates,
 * edits or soft-deletes a premium message template.
 *
 * <p>This handler backs the SMB premium-message template surface: when the
 * primary device authors, edits or deletes a marketing template the resulting
 * row fans out across the {@link SyncPatchType#REGULAR} collection so
 * companion devices can render the same template list. The mutation index
 * keys each entry by the stable template id, formatted as
 * {@snippet :
 *     ["marketingMessage", messageId]
 * }
 *
 * @implNote
 * This implementation persists each template eagerly through
 * {@link LinkedWhatsAppBusinessStore#putMarketingMessage(com.github.auties00.cobalt.wire.linked.business.MarketingMessage)}
 * keyed by the template id, collapsing WA Web's two-stage bulk-create-or-merge
 * plus collection-add flow into a single map write because Cobalt's storage is
 * a flat key/value map. The {@link MarketingMessageAction#isDeleted()} flag is
 * preserved on the stored row so the rest of the pipeline can filter the
 * live-template view; per Cobalt's pluggable error model, exceptions propagate
 * to the orchestrator instead of being mapped to
 * {@link MutationApplicationResult#failed()} inline.
 */
@WhatsAppWebModule(moduleName = "WAWebPremiumMessageSync")
public final class MarketingMessageHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link MarketingMessageHandler}.
     */
    private static final System.Logger LOGGER = Log.get(MarketingMessageHandler.class);

    /**
     * Constructs a new {@link MarketingMessageHandler} for registration in
     * the sync handler registry.
     */
    @WhatsAppWebExport(moduleName = "WAWebPremiumMessageSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public MarketingMessageHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPremiumMessageSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return MarketingMessageAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPremiumMessageSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return MarketingMessageAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPremiumMessageSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return MarketingMessageAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The index must carry the template id and only
     * {@link SyncdOperation#SET} is accepted. A missing
     * {@link MarketingMessageAction#type()} is rejected as
     * {@link MutationApplicationResult#malformed()};
     * {@link MarketingMessageAction#createdAt()} and
     * {@link MarketingMessageAction#lastSentAt()} are converted from the
     * wire's epoch milliseconds via {@link Instant#ofEpochMilli(long)}, and
     * the {@link MarketingMessageAction#isDeleted()} flag is persisted
     * verbatim so later readers can filter live templates.
     *
     * @implNote
     * This implementation classifies a missing index slot or missing
     * {@link MarketingMessageAction#type()} as
     * {@link MutationApplicationResult#malformed()} so the orchestrator does
     * not silently overwrite a template with a default-typed row.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPremiumMessageSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var indexArray = JSON.parseArray(mutation.index());
        if (indexArray.size() <= 1) {
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }
        var messageId = indexArray.getString(1);
        if (messageId == null || messageId.isEmpty()) {
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }

        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof MarketingMessageAction action)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "marketing message mutation has malformed action value, template={0}", messageId);
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        if (action.type().isEmpty()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "marketing message mutation missing type, template={0}", messageId);
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        client.store().businessStore().putMarketingMessage(new MarketingMessageBuilder()
                .templateId(messageId)
                .name(action.name().orElse(null))
                .message(action.message().orElse(null))
                .type(action.type().orElse(null))
                .createdAt(action.createdAt().isPresent() ? Instant.ofEpochMilli(action.createdAt().getAsLong()) : null)
                .lastSentAt(action.lastSentAt().isPresent() ? Instant.ofEpochMilli(action.lastSentAt().getAsLong()) : null)
                .deleted(action.isDeleted())
                .mediaId(action.mediaId().orElse(null))
                .build());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "marketing message template applied, template={0} type={1} deleted={2}", messageId, action.type().orElse(null), action.isDeleted());
        return MutationApplicationResult.success();
    }
}
