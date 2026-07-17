package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.preference.QuickReplyBuilder;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.QuickReplyAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Applies the {@code quick_reply} app-state action that creates, updates, or
 * deletes WhatsApp Business quick reply templates.
 *
 * <p>Each mutation upserts or deletes a
 * {@code (shortcut, message, keywords, count)} record keyed by quick reply id.
 * The mutation index keys each entry by the quick reply id, formatted as
 * {@snippet :
 *     ["quick_reply", quickReplyId]
 * }
 *
 * @implNote
 * This implementation persists each entry on
 * {@link LinkedWhatsAppStore} via
 * {@code addQuickReply}/{@code removeQuickReply} keyed by id; WA Web stores the
 * same shape in the {@code quick-reply} IndexedDB table. The
 * {@code WAWebBackendApi.frontendFireAndForget} dispatches are dropped because
 * Cobalt has no UI consumer; the per-batch {@code WARN} counters are also
 * dropped.
 */
@WhatsAppWebModule(moduleName = "WAWebQuickRepliesSync")
public final class QuickReplyHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link QuickReplyHandler}.
     */
    private static final System.Logger LOGGER = Log.get(QuickReplyHandler.class);

    /**
     * Constructs the singleton quick reply sync handler.
     *
     * @implNote
     * This implementation is stateless; no AB-prop, store, or WAM dependency is
     * held.
     */
    @WhatsAppWebExport(moduleName = "WAWebQuickRepliesSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public QuickReplyHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebQuickRepliesSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return QuickReplyAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebQuickRepliesSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return QuickReplyAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebQuickRepliesSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return QuickReplyAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation walks the per-mutation arms of WA Web's
     * {@code WAWebQuickRepliesSync.applyMutations}: only
     * {@link SyncdOperation#SET} is accepted; a missing quick reply id surfaces
     * as {@link SyncdIndexUtils#malformedActionIndex(String, String)}; a
     * missing {@link QuickReplyAction} payload as
     * {@link SyncdIndexUtils#malformedActionValue(String)};
     * {@link QuickReplyAction#deleted()} {@code == true} removes the entry by
     * id; otherwise the {@link QuickReplyAction#shortcut()} and
     * {@link QuickReplyAction#message()} fields must both be non-empty;
     * {@link QuickReplyAction#keywords()} defaults to an empty list and
     * {@link QuickReplyAction#count()} to {@code 0}. Per-mutation exceptions
     * surface as {@link MutationApplicationResult#failed()}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebQuickRepliesSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        try {
            var indexArray = JSON.parseArray(mutation.index());
            var quickReplyId = indexArray.size() > 1 ? indexArray.getString(1) : null;
            if (quickReplyId == null || quickReplyId.isEmpty()) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "quick reply mutation malformed: missing id");
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof QuickReplyAction action)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "quick reply mutation malformed: missing action value");
                return SyncdIndexUtils.malformedActionValue(collectionName().name());
            }

            if (action.deleted()) {
                client.store().settingsStore().removeQuickReply(quickReplyId);
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "quick reply: removed id={0}", quickReplyId);
                return MutationApplicationResult.success();
            }

            var message = action.message().orElse(null);
            var shortcut = action.shortcut().orElse(null);
            if (shortcut == null || shortcut.isEmpty() || message == null || message.isEmpty()) {
                if (Log.WARNING)
                    LOGGER.log(Level.WARNING, "quick reply mutation malformed: missing shortcut or message");
                return SyncdIndexUtils.malformedActionValue(collectionName().name());
            }

            var keywords = action.keywords();
            var count = action.count().orElse(0);
            var quickReply = new QuickReplyBuilder()
                    .id(quickReplyId)
                    .shortcut(shortcut)
                    .message(message)
                    .keywords(keywords)
                    .count(count)
                    .build();
            client.store().settingsStore().addQuickReply(quickReply);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "quick reply: upserted id={0}, keywords={1}", quickReplyId, keywords.size());
            return MutationApplicationResult.success();
        } catch (Exception e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "quick reply mutation failed", e);
            return MutationApplicationResult.failed();
        }
    }

}
