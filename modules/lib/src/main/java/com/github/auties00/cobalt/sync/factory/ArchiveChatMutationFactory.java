package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessageRange;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds outgoing app-state mutations that archive or unarchive a chat.
 *
 * <p>When the user archives or unarchives a chat through
 * {@link com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient}, the resulting
 * {@link SyncPendingMutation} values are queued for outbound app-state sync so
 * linked devices reflect the same archive state. This factory builds the
 * outgoing mutations; the inbound counterpart is
 * {@link com.github.auties00.cobalt.sync.handler.ArchiveChatHandler}.
 *
 * @implNote
 * This implementation accepts a pre-built {@link SyncActionMessageRange}
 * because Cobalt does not run the WA Web
 * {@code WAWebMessageRangeUtils.constructMessageRange} pipeline, which is
 * tied to the browser's active-message-range IndexedDB tables; callers
 * resolve the range upstream.
 */
public final class ArchiveChatMutationFactory {
    /**
     * The logger for {@link ArchiveChatMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(ArchiveChatMutationFactory.class);

    /**
     * The pin-chat factory consulted to emit the companion unpin mutation when archiving.
     *
     * @implNote
     * This implementation mirrors WA Web's
     * {@code WAWebArchiveChatSync.getMutationsForArchive}, which unpins the
     * chat as a side effect of archiving it; Cobalt routes the unpin through
     * {@link PinChatMutationFactory#getPinMutation} from
     * {@link #getMutationsForArchive(Instant, boolean, Jid, SyncActionMessageRange)}.
     */
    private final PinChatMutationFactory pinChatMutationFactory;

    /**
     * Creates a factory bound to the given pin-chat factory.
     *
     * <p>The injected factory is the wiring seam for the archive-implies-unpin
     * behaviour of
     * {@link #getMutationsForArchive(Instant, boolean, Jid, SyncActionMessageRange)};
     * a single instance is shared across the client.
     *
     * @param pinChatMutationFactory the companion factory used to build the
     *                               unpin mutation that ships alongside an
     *                               archive mutation
     */
    public ArchiveChatMutationFactory(PinChatMutationFactory pinChatMutationFactory) {
        this.pinChatMutationFactory = pinChatMutationFactory;
    }

    /**
     * Returns a {@link SyncPendingMutation} that archives or unarchives the given chat.
     *
     * <p>Call this when archiving or unarchiving a single chat. The mutation
     * index follows
     * {@snippet :
     *     ["archive", chatJid.toString()]
     * }
     * and the {@link ArchiveChatAction} sub-message carries the
     * {@code archived} flag plus the supplied {@link SyncActionMessageRange} so
     * receive-side conflict resolution can compare ranges. Callers that want
     * the archive-also-unpins behaviour should prefer
     * {@link #getMutationsForArchive(Instant, boolean, Jid, SyncActionMessageRange)}.
     *
     * @implNote
     * This implementation skips WA Web's chat-jid mutation-index resolver
     * ({@code WAWebSyncdGetChat.getChatJidMutationIndexForChat}) because
     * Cobalt's {@link Jid} is already in canonical form; it also skips the
     * pending-mutation merge step
     * ({@code WAWebSyncdDb.getPendingMutationsRowsByIndex}) since Cobalt
     * coalesces app-state batches at a higher layer.
     *
     * @param timestamp    the mutation timestamp
     * @param archived     {@code true} to archive the chat, {@code false} to unarchive
     * @param chatJid      the chat {@link Jid} being toggled
     * @param messageRange the pre-built range covering the messages visible at the time of the action
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebArchiveChatSync", exports = "getArchiveChatMutation", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getArchiveChatMutation(
            Instant timestamp,
            boolean archived,
            Jid chatJid,
            SyncActionMessageRange messageRange
    ) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building archive chat mutation chat={0} archived={1}", chatJid, archived);
        var action = new ArchiveChatActionBuilder()
                .archived(archived)
                .messageRange(messageRange)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .archiveChatAction(action)
                .build();
        var index = JSON.toJSONString(List.of(ArchiveChatAction.ACTION_NAME, chatJid.toString()));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                ArchiveChatAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }

    /**
     * Returns the full set of {@link SyncPendingMutation} values that archive or unarchive a chat.
     *
     * <p>Archiving a chat in WhatsApp Web always unpins it as a side effect, so
     * this method emits both mutations in canonical order so linked devices see
     * them in one batch. Prefer this entry point over
     * {@link #getArchiveChatMutation(Instant, boolean, Jid, SyncActionMessageRange)}
     * when triggering the archive UI. The unpin mutation is appended only when
     * {@code archived} is {@code true}.
     *
     * @implNote
     * This implementation mirrors the {@code [archiveMutation, unpinMutation]}
     * shape of WA Web's {@code WAWebArchiveChatSync.getMutationsForArchive}; the
     * unpin is built via {@link PinChatMutationFactory#getPinMutation} with the
     * shared {@code timestamp} so the two mutations are timestamp-paired.
     *
     * @param timestamp    the mutation timestamp shared by both mutations
     * @param archived     {@code true} to archive (and unpin) the chat, {@code false} to unarchive
     * @param chatJid      the chat {@link Jid} being toggled
     * @param messageRange the pre-built range covering the messages visible at the time of the action
     * @return the ordered list of pending mutations to enqueue
     */
    @WhatsAppWebExport(moduleName = "WAWebArchiveChatSync", exports = "getMutationsForArchive", adaptation = WhatsAppAdaptation.DIRECT)
    public List<SyncPendingMutation> getMutationsForArchive(
            Instant timestamp,
            boolean archived,
            Jid chatJid,
            SyncActionMessageRange messageRange
    ) {
        var mutations = new ArrayList<SyncPendingMutation>();
        mutations.add(getArchiveChatMutation(timestamp, archived, chatJid, messageRange));
        if (archived) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "archiving chat={0} also unpins it", chatJid);
            mutations.add(pinChatMutationFactory.getPinMutation(timestamp, false, chatJid));
        }
        return mutations;
    }
}
