package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.bot.AiThreadRenameAction;
import com.github.auties00.cobalt.wire.linked.sync.action.bot.AiThreadRenameActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.AiThreadsUserJourneyEvent;
import com.github.auties00.cobalt.wire.wam.event.AiThreadsUserJourneyEventBuilder;
import com.github.auties00.cobalt.wam.threadlogging.LiveThreadLoggingService;
import com.github.auties00.cobalt.wire.wam.type.ThreadActionTypes;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Builds outgoing app-state mutations that rename an AI thread on a Meta-AI bot chat.
 *
 * <p>When the user changes a thread title, the returned mutation is enqueued
 * through {@link com.github.auties00.cobalt.sync.WebAppStateService#pushPatches}
 * so every linked device updates the same thread's metadata. This factory
 * builds the outgoing mutation; the inbound counterpart is
 * {@link com.github.auties00.cobalt.sync.handler.AiThreadRenameHandler}.
 *
 * <p>Building a rename mutation additionally records an
 * {@link AiThreadsUserJourneyEvent} through {@link WamService#commit} so the
 * rename appears in the Meta-AI thread user-journey telemetry, matching WA
 * Web's {@code WAWebThreadJourneyLogger.logThreadRename} which fires when the
 * rename action is issued.
 *
 * @implNote
 * This implementation skips the WA Web pre-emit
 * {@code isStringNotNullAndNotWhitespaceOnly(newTitle)} guard and forwards
 * whatever title the caller supplies; the receive-side handler still validates
 * the title via {@link AiThreadRenameAction#newTitle()}.
 */
public final class AiThreadRenameMutationFactory {
    /**
     * The logger for {@link AiThreadRenameMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(AiThreadRenameMutationFactory.class);

    /**
     * Holds the bound client, used to resolve the provisioned chat-thread
     * logging secret that hashes the AI thread id into the
     * {@link AiThreadsUserJourneyEvent#conversationThreadId()} field.
     */
    private final LinkedWhatsAppClient client;

    /**
     * Holds the WAM telemetry sink that receives the
     * {@link AiThreadsUserJourneyEvent} committed on every rename.
     */
    private final WamService wamService;

    /**
     * Holds the per-instance shared application session id reported as the
     * {@link AiThreadsUserJourneyEvent#appSessionId()} of every rename.
     *
     * <p>A single value is minted when the factory is constructed and reused
     * for the lifetime of the client, mirroring WA Web's
     * {@code WAWebGetSharedSessionId.getSharedSessionId} which is stable for a
     * single app load rather than per action.
     */
    private final String appSessionId;

    /**
     * Creates a factory bound to the given client and WAM telemetry sink.
     *
     * <p>A single instance is shared across the lifetime of the client. The
     * application session id reported on every rename journey event is minted
     * once here so that consecutive renames from the same client instance carry
     * a stable {@link AiThreadsUserJourneyEvent#appSessionId()}.
     *
     * @param client     the bound WhatsApp client, must not be {@code null}
     * @param wamService the WAM telemetry sink, must not be {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public AiThreadRenameMutationFactory(LinkedWhatsAppClient client, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
        this.appSessionId = UUID.randomUUID().toString();
    }

    /**
     * Returns a {@link SyncPendingMutation} that renames the given AI thread.
     *
     * <p>Call this when the user edits an AI thread title; the returned mutation
     * must be enqueued via
     * {@link com.github.auties00.cobalt.sync.WebAppStateService#pushPatches} to
     * fan it out to linked devices. The mutation index follows
     * {@snippet :
     *     ["ai_thread_rename", chatJid.toString(), threadId]
     * }
     * and the {@link AiThreadRenameAction} sub-message carries the new title.
     * The receive-side handler rejects a blank {@code newTitle}. Producing the
     * mutation also commits an {@link AiThreadsUserJourneyEvent} with
     * {@link ThreadActionTypes#RENAME} through {@link #emitRenameJourney(String)}.
     *
     * @implNote
     * This implementation emits the {@link DecryptedMutation.Trusted} variant
     * with {@link SyncdOperation#SET}, pinning the version to
     * {@link AiThreadRenameAction#ACTION_VERSION}.
     *
     * @param chatJid  the bot {@link Jid} owning the thread
     * @param threadId the thread identifier as exposed by the bot
     * @param newTitle the new title for the thread
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebAiThreadRenameSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getAiThreadRenameMutation(Jid chatJid, String threadId, String newTitle) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building ai thread rename mutation chat={0} thread={1}", chatJid, threadId);
        var timestamp = Instant.now();
        var action = new AiThreadRenameActionBuilder()
                .newTitle(newTitle)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .aiThreadRenameAction(action)
                .build();
        var index = JSON.toJSONString(List.of(AiThreadRenameAction.ACTION_NAME, chatJid.toString(), threadId));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                AiThreadRenameAction.ACTION_VERSION
        );
        emitRenameJourney(threadId);
        return new SyncPendingMutation(mutation, 0);
    }

    /**
     * Commits the Meta-AI thread user-journey event that records a rename.
     *
     * <p>The event carries a freshly minted per-action {@code aiSessionId}, the
     * stable {@link #appSessionId}, the current wall-clock time in milliseconds,
     * and {@link ThreadActionTypes#RENAME}. The thread id is hashed into
     * {@link AiThreadsUserJourneyEvent#conversationThreadId()} through
     * {@link LiveThreadLoggingService#chatThreadIdHmac(LinkedWhatsAppClient, String)},
     * matching WA Web's {@code WAWebChatThreadLogging.getThreadIDHMAC(threadId)};
     * the field is omitted when no chat-thread logging secret has been
     * provisioned. The entry-point and thread-creation-timestamp fields are left
     * unset because Cobalt's headless rename send path has no surrounding UI
     * context to source them from, and WA Web likewise omits them when
     * unavailable.
     *
     * @implNote
     * This implementation mints a random {@code aiSessionId} per rename rather
     * than threading WA Web's {@code WAWebThreadJourneyLogger} per-conversation
     * {@code aiSessionId} across actions; Cobalt has no AI-session lifecycle
     * object at the mutation-factory layer to reuse.
     *
     * @param threadId the thread identifier hashed into the journey event
     */
    @WhatsAppWebExport(moduleName = "WAWebThreadJourneyLogger", exports = "logThreadRename", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitRenameJourney(String threadId) {
        var conversationThreadId = LiveThreadLoggingService.chatThreadIdHmac(client, threadId);
        var builder = new AiThreadsUserJourneyEventBuilder()
                .aiSessionId(UUID.randomUUID().toString())
                .appSessionId(appSessionId)
                .eventTsMs(System.currentTimeMillis())
                .threadActionType(ThreadActionTypes.RENAME);
        if (!conversationThreadId.isBlank()) {
            builder.conversationThreadId(conversationThreadId);
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "committing ai thread rename journey event thread={0}", threadId);
        wamService.commit(builder.build());
    }
}
