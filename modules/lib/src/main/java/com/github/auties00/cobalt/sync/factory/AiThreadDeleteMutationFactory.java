package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.handler.AiThreadDeleteHandler;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing app-state mutations that delete an AI thread on a Meta-AI bot chat.
 *
 * <p>When the local user removes a thread from a bot chat, the resulting
 * {@link SyncPendingMutation} is enqueued through
 * {@link com.github.auties00.cobalt.sync.WebAppStateService#pushPatches} so
 * every linked device drops the same thread from its bulk thread-metadata
 * store. This factory builds the outgoing mutation; the inbound counterpart
 * is {@link AiThreadDeleteHandler}.
 *
 * @implNote
 * This implementation gates only on the bot/thread identifiers; WA Web
 * additionally checks {@code WAWebBotBaseGating.isBotEnabled()} and
 * {@code isAiChatThreadsInfraEnabled()} on the receive side, so Cobalt callers
 * are expected to skip the factory call when the surface is gated off.
 */
public final class AiThreadDeleteMutationFactory {
    /**
     * The logger for {@link AiThreadDeleteMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(AiThreadDeleteMutationFactory.class);

    /**
     * Creates a stateless factory with no collaborators.
     *
     * <p>A single instance may be shared across the lifetime of the client.
     */
    public AiThreadDeleteMutationFactory() {

    }

    /**
     * Returns a {@link SyncPendingMutation} that deletes the given AI thread from the bot chat.
     *
     * <p>Call this when the user removes an AI thread; the returned mutation
     * must be enqueued via
     * {@link com.github.auties00.cobalt.sync.WebAppStateService#pushPatches} to
     * fan it out to linked devices. The mutation index follows
     * {@snippet :
     *     ["ai_thread_delete", chatJid.toString(), threadId]
     * }
     * and the action carries no sub-message because the index alone identifies
     * the thread.
     *
     * @implNote
     * This implementation emits the {@link DecryptedMutation.Trusted} variant
     * with {@link SyncdOperation#SET}, pinning the version to
     * {@link AiThreadDeleteHandler#ACTION_VERSION}.
     *
     * @param chatJid  the bot {@link Jid} owning the thread
     * @param threadId the thread identifier as exposed by the bot
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebAiThreadDeleteSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getAiThreadDeleteMutation(Jid chatJid, String threadId) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building ai thread delete mutation chat={0} thread={1}", chatJid, threadId);
        var timestamp = Instant.now();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .build();
        var index = JSON.toJSONString(List.of(AiThreadDeleteHandler.ACTION_NAME, chatJid.toString(), threadId));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                AiThreadDeleteHandler.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
