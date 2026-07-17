package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.bot.BotWelcomeRequestStateBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.bot.BotWelcomeRequestAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Tracks whether the per-bot welcome message has been requested for a given bot chat.
 *
 * <p>The bot-chat surface suppresses the on-open welcome prompt after it has
 * been delivered once. When the welcome request lands on another device, the
 * server replays it here as a {@link BotWelcomeRequestAction}, and the result
 * is read back via
 * {@link LinkedWhatsAppBusinessStore#findBotWelcomeRequestState(Jid)}.
 *
 * @implNote
 * This implementation stores the requested flag in a typed quintet
 * keyed by bot JID rather than as the {@code hasRequestedWelcomeMsg}
 * column on the chat row that WA Web mutates; the WA Web frontend
 * {@code chatCollectionUpdate} fire-and-forget event is intentionally
 * omitted because Cobalt has no browser frontend bridge.
 */
@WhatsAppWebModule(moduleName = "WAWebBotWelcomeRequestSync")
public final class BotWelcomeRequestHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link BotWelcomeRequestHandler}.
     */
    private static final System.Logger LOGGER = Log.get(BotWelcomeRequestHandler.class);

    /**
     * Constructs the singleton bot-welcome-request handler.
     *
     * <p>The sync handler registry instantiates this type exactly once.
     */
    @WhatsAppWebExport(moduleName = "WAWebBotWelcomeRequestSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public BotWelcomeRequestHandler() {

    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebBotWelcomeRequestSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return BotWelcomeRequestAction.ACTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebBotWelcomeRequestSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return BotWelcomeRequestAction.COLLECTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebBotWelcomeRequestSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return BotWelcomeRequestAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads {@link BotWelcomeRequestAction#isSent()} from the mutation value,
     * locates the target bot {@link com.github.auties00.cobalt.wire.linked.chat.Chat}
     * by JID, and upserts the welcome-request state. Returns
     * {@link MutationApplicationResult#unsupported()} for {@link SyncdOperation#REMOVE}
     * operations (WA Web treats {@link SyncdOperation#REMOVE} as not supported),
     * an orphan result keyed by chat JID and model type {@code "Chat"} when the
     * chat is not in the store, and {@link MutationApplicationResult#failed()}
     * for any unknown operation or thrown exception.
     *
     * @implNote
     * This implementation derives the {@code requested} boolean via
     * {@link BotWelcomeRequestAction#isSent()} which coalesces a null
     * wire field to {@code false}; WA Web treats a null
     * {@code isSent} as malformed and emits
     * {@link SyncdIndexUtils#malformedActionValue(String)}. The Cobalt
     * model accessor is lossy on the boolean wire field so the
     * malformed branch is unreachable here.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebBotWelcomeRequestSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() == SyncdOperation.REMOVE) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "bot welcome request mutation unsupported: REMOVE operation");
            return MutationApplicationResult.unsupported();
        }

        if (mutation.operation() != SyncdOperation.SET) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "bot welcome request mutation failed: unexpected operation={0}", mutation.operation());
            return MutationApplicationResult.failed();
        }

        try {
            var indexArray = JSON.parseArray(mutation.index());
            var chatJidString = indexArray.getString(1);
            if (chatJidString == null || chatJidString.isEmpty()) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "bot welcome request mutation malformed: missing chat jid in index");
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof BotWelcomeRequestAction action)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "bot welcome request mutation malformed: missing action value");
                return SyncdIndexUtils.malformedActionValue(collectionName().name());
            }

            var chatJid = Jid.of(chatJidString);
            var chat = client.store().chatStore().findChatByJid(chatJid);
            if (chat.isEmpty()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "bot welcome request mutation orphaned: chat not found {0}", chatJid);
                return MutationApplicationResult.orphan(chatJidString, "Chat");
            }

            var resolvedJid = chat.get().toJid();
            client.store().businessStore().putBotWelcomeRequestState(new BotWelcomeRequestStateBuilder().botJid(resolvedJid).requested(action.isSent()).build());
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "bot welcome request state updated: bot={0} requested={1}", resolvedJid, action.isSent());

            return MutationApplicationResult.success();
        } catch (Exception e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "bot welcome request mutation failed", e);
            return MutationApplicationResult.failed();
        }
    }

}
