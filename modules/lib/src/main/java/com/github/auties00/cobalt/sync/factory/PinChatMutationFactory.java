package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessageRange;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinAction;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.PinnedChatsEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.PinnedChatsPremiumStatusType;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing pin-chat sync mutations.
 *
 * <p>Backs the chat-list pin gesture and the newsletter pin toggle in the Newsletters surface.
 * Mutations produced here are consumed on receiving devices by
 * {@link com.github.auties00.cobalt.sync.handler.PinChatHandler}, which applies the pin update to
 * the chat table and, when locked, runs the four-chat-limit eviction. This factory is also used
 * internally by {@link LockChatMutationFactory} and {@link ArchiveChatMutationFactory} to attach a
 * companion unpin when an archive or lock takes effect.
 *
 * @implNote
 * This implementation mirrors {@code WAWebPinChatSync.getPinMutation}. Receiver-side conflict
 * resolution and the {@code WAWebMdSyncdDogfoodingFeatureUsageWamEvent} telemetry emitted on
 * unpinning the fourth chat are handler-side concerns and do not surface here. The
 * {@code WAWebPinnedChatsWamEvent} pinned-chats beacon that WA Web commits inside
 * {@code WAWebSetPinChatAction.setPin} after a successful pin is emitted by
 * {@link #emitPinnedChats(LinkedWhatsAppClient, Jid)}, driven from the client's pin path.
 */
public final class PinChatMutationFactory {
    /**
     * The logger for {@link PinChatMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(PinChatMutationFactory.class);

    /**
     * Holds the WAM telemetry service used to commit the {@code WAWebPinnedChatsWamEvent}
     * pinned-chats beacon after a pin gesture completes.
     *
     * <p>The beacon is only committed on the pin path; unpins, and the companion unpins built for
     * {@link LockChatMutationFactory} and {@link ArchiveChatMutationFactory}, emit nothing, which
     * matches WA Web committing the event only when the pin flag is set.
     */
    private final WamService wamService;

    /**
     * Constructs a pin-chat mutation factory bound to the given WAM telemetry service.
     *
     * <p>The factory keeps no other state, so a single instance is sufficient per client. The WAM
     * service is consulted only by {@link #emitPinnedChats(LinkedWhatsAppClient, Jid)}; the
     * mutation-building methods do not touch it.
     *
     * @param wamService the WAM telemetry service used to commit the pinned-chats beacon
     */
    public PinChatMutationFactory(WamService wamService) {
        this.wamService = wamService;
    }

    /**
     * Builds a pending mutation that pins or unpins a chat or newsletter.
     *
     * <p>Called from the public pin setter and from
     * {@link LockChatMutationFactory#getMutationsForLock(Instant, boolean, Jid, SyncActionMessageRange)}.
     * Newsletters are supported on the same builder because WA Web routes them through the same
     * apply path after detecting a newsletter JID.
     *
     * @implNote
     * This implementation passes the supplied {@code chatJid} verbatim into the index. WA Web's
     * {@code getChatJidMutationIndexForChat} would swap a PN for its paired LID under LID1x1
     * migration, which Cobalt does not maintain at this layer. The mutation is routed through the
     * {@code RegularLow} collection alongside the other chat-scoped sync actions and uses
     * {@code SET} as the operation. This method does not commit the pinned-chats beacon; the beacon
     * is fired by {@link #emitPinnedChats(LinkedWhatsAppClient, Jid)} because WA Web commits it
     * post-pin from the persisted store, not while building the outgoing mutation.
     *
     * @param timestamp the mutation timestamp recorded on both the outer mutation and the inner
     *                  {@code SyncActionValue}
     * @param pinned    {@code true} to pin the chat, {@code false} to unpin
     * @param chatJid   the JID of the chat or newsletter
     * @return the pending mutation for the pin action
     */
    public SyncPendingMutation getPinMutation(Instant timestamp, boolean pinned, Jid chatJid) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building pin mutation chat={0} pinned={1}", chatJid, pinned);
        var pinAction = new PinActionBuilder()
                .pinned(pinned)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .pinAction(pinAction)
                .build();
        var index = JSON.toJSONString(List.of(PinAction.ACTION_NAME, chatJid.toString()));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                PinAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }

    /**
     * Commits the pinned-chats beacon after a chat or newsletter has been pinned.
     *
     * <p>Reports the number of conversations pinned after this pin took effect and the account's
     * pinned-chats premium status. For a chat JID the count is the number of chats carrying a pin
     * timestamp; for a newsletter JID it is the number of pinned newsletters. In both cases the
     * conversation being pinned is counted, so the value is correct whether the caller invokes this
     * method before or after the local model records the new pin. WA Web fires the beacon only on
     * the pin path; callers must not invoke it when unpinning.
     *
     * @implNote
     * This implementation mirrors the {@code WAWebPinnedChatsWamEvent} commit that WA Web performs
     * inside {@code WAWebSetPinChatAction.setPin}, deriving {@code pinnedChatNumber} the same way as
     * {@code WAWebChatPinBridge.getNumConversationsPinned} (newsletter pins for a newsletter JID,
     * chat pins otherwise). The {@code pinnedChatsPremiumStatus} field is reported as
     * {@link PinnedChatsPremiumStatusType#DISABLED} because Cobalt does not implement the
     * {@code WAWebAuraGating} premium-benefit gating that WA Web consults in
     * {@code WAWebPinnedChatsWamUtils.getPinnedChatsPremiumStatus} to escalate to
     * {@link PinnedChatsPremiumStatusType#ENABLED} or {@link PinnedChatsPremiumStatusType#ACTIVE};
     * a client without the WA Plus pinned-chats benefit always reports {@code DISABLED}.
     *
     * @param client  the client whose chat store supplies the pinned-conversation count
     * @param chatJid the JID of the chat or newsletter that was pinned
     */
    @WhatsAppWebExport(moduleName = "WAWebSetPinChatAction", exports = "setPin", adaptation = WhatsAppAdaptation.ADAPTED)
    public void emitPinnedChats(LinkedWhatsAppClient client, Jid chatJid) {
        var pinnedChatNumber = chatJid.hasNewsletterServer()
                ? client.store().chatStore().newsletterPinStates().stream()
                        .filter(pin -> !pin.newsletterJid().equals(chatJid))
                        .count() + 1L
                : client.store().chatStore().chats().stream()
                        .filter(chat -> chat.pinnedTimestamp().isPresent())
                        .filter(chat -> !chat.jid().equals(chatJid))
                        .count() + 1L;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "committing pinned chats beacon chat={0} pinnedCount={1}", chatJid, pinnedChatNumber);
        wamService.commit(new PinnedChatsEventBuilder()
                .pinnedChatNumber(pinnedChatNumber)
                .pinnedChatsPremiumStatus(PinnedChatsPremiumStatusType.DISABLED)
                .build());
    }
}
