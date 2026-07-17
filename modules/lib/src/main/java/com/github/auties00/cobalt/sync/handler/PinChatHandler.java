package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterPin;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterPinBuilder;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValue;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinAction;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.wire.wam.event.MdSyncdDogfoodingFeatureUsageEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PinnedChatsMaxAlertEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.MdFeatureCode;
import com.github.auties00.cobalt.wire.wam.type.PremiumStatusType;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Applies the {@code pin_v1} app-state action that pins or unpins chats and
 * newsletters across linked devices.
 *
 * <p>When the primary device pins or unpins a conversation the resulting
 * timestamp fans out across the {@link SyncPatchType#REGULAR_LOW} collection.
 * Pinning enforces the per-account caps {@value #MAX_PINNED_CHATS} for chats
 * and {@value #MAX_PINNED_NEWSLETTERS} for newsletters: when the cap would be
 * exceeded the oldest pin is evicted (when its timestamp is older than the
 * incoming pin) or the incoming pin itself is rejected (when the oldest pin is
 * newer); in either case a pending unpin is queued so the rejection propagates
 * back to other devices. The mutation index keys each entry by the chat or
 * newsletter JID, formatted as {@snippet :
 *     ["pin_v1", chatOrNewsletterJid]
 * }
 *
 * @implNote
 * This implementation splits WA Web's unified
 * {@code WAWebPinChatSync.applyMutation} into two arms based on the JID
 * server: chat pins live as {@code pinnedTimestamp} on the
 * {@link com.github.auties00.cobalt.wire.linked.chat.Chat} model directly;
 * newsletter pins live in a dedicated {@link NewsletterPin}-keyed map on the
 * store because Cobalt's
 * {@link com.github.auties00.cobalt.wire.linked.newsletter.Newsletter} model has no
 * {@code pinnedTimestamp} field. The
 * {@link MdFeatureCode#UNPIN_4TH_CHAT_MUTATION} dogfooding WAM event is
 * committed when the cap-eviction path is taken. The exception-propagation
 * boundary differs from WA Web: per-mutation exceptions surface as
 * {@link MutationApplicationResult#failed()} for callers, not as a silent
 * {@code Failed} result inside the batch.
 */
public final class PinChatHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link PinChatHandler}.
     */
    private static final System.Logger LOGGER = Log.get(PinChatHandler.class);

    /**
     * Holds the per-account cap on pinned chats.
     *
     * @implNote
     * This implementation hardcodes the WA Web
     * {@code WAWebPinChatLimits.MAX_PINNED_CHATS = 3}; the
     * {@code WAWebChatPinBridge.getPinLimit} helper also consults a
     * premium-benefit gating function which Cobalt does not implement, so the
     * literal value is used directly.
     */
    private static final int MAX_PINNED_CHATS = 3;

    /**
     * Holds the per-account cap on pinned newsletters.
     *
     * @implNote
     * This implementation hardcodes the WA Web
     * {@code WAWebPinChatLimits.MAX_PINNED_NEWSLETTERS = 2}.
     */
    private static final int MAX_PINNED_NEWSLETTERS = 2;

    /**
     * Holds the WAM telemetry service used to commit the dogfooding feature
     * usage event when cap-eviction occurs.
     */
    private final WamService wamService;

    /**
     * Constructs the pin-chat sync handler bound to the given WAM telemetry
     * service.
     *
     * @implNote
     * WA Web wires the WAM commit directly to the global
     * {@code WAWebMdSyncdDogfoodingFeatureUsageWamEvent} singleton; this
     * implementation injects {@link WamService} so the dogfooding emission is
     * testable.
     *
     * @param wamService the WAM telemetry service used by the cap-eviction
     *                   paths
     */
    public PinChatHandler(WamService wamService) {
        this.wamService = wamService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String actionName() {
        return PinAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SyncPatchType collectionName() {
        return PinAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int version() {
        return PinAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation walks the per-mutation arms of WA Web's
     * {@code WAWebPinChatSync.applyMutation}: only {@link SyncdOperation#SET}
     * is accepted; a chat JID without an {@code @} server is treated as
     * {@link SyncdIndexUtils#malformedActionIndex(String, String)}; newsletter
     * JIDs are routed to
     * {@link #applyNewsletterPinMutation(LinkedWhatsAppClient, Jid, PinAction, Instant)}
     * because Cobalt stores newsletters separately from chats; the unpin path
     * zeros the chat's {@code pinnedTimestamp}; the pin path early-outs on
     * already-pinned chats, accepts when the cap is not yet reached, and
     * otherwise picks the oldest pin to evict (when its timestamp is older than
     * the incoming pin) or rejects the incoming pin (queueing a pending unpin
     * in either case). Cobalt's {@link PinAction#pinned()} accessor coalesces a
     * {@code null} flag to {@code false} per the project nullable boolean rule,
     * so a missing {@code pinned} field is interpreted as "unpin" rather than
     * as {@link MutationApplicationResult#malformed()}.
     */
    @Override
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "pin chat: unsupported operation {0}", mutation.operation());
            return MutationApplicationResult.unsupported();
        }

        try {
            var indexArray = JSON.parseArray(mutation.index());
            var chatJidString = indexArray.size() > 1 ? indexArray.getString(1) : null;
            if (chatJidString == null || chatJidString.isEmpty()) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "pin chat mutation malformed: missing jid index");
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            if (chatJidString.indexOf('@') < 0) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "pin chat mutation malformed: jid missing server");
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            Jid chatJid;
            try {
                chatJid = Jid.of(chatJidString);
            } catch (Exception e) {
                if (Log.WARNING)
                    LOGGER.log(Level.WARNING, "pin chat mutation malformed: unparseable jid {0}", new LogRedactable.User(chatJidString));
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }
            if (chatJid == null) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "pin chat mutation malformed: null jid");
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof PinAction action)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "pin chat mutation malformed: missing action value");
                return SyncdIndexUtils.malformedActionValue(collectionName().name());
            }

            var currentTimestamp = mutation.timestamp();
            var isNewsletter = chatJid.hasNewsletterServer();

            if (isNewsletter) {
                return applyNewsletterPinMutation(client, chatJid, action, currentTimestamp);
            }

            var chat = client.store().chatStore().findChatByJid(chatJid);
            if (chat.isEmpty()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "pin chat: orphan chat {0}", chatJid);
                return MutationApplicationResult.orphan(chatJidString, "Chat");
            }

            if (!action.pinned()) {
                chat.get().setPinnedTimestamp(null);
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "pin chat: unpinned {0}", chatJid);
                return MutationApplicationResult.success();
            }

            var allPinnedChats = client.store().chatStore().chats().stream()
                    .filter(c -> c.pinnedTimestamp().isPresent())
                    .toList();

            var alreadyPinned = allPinnedChats.stream()
                    .anyMatch(c -> c.jid().equals(chatJid));
            if (alreadyPinned) {
                chat.get().setPinnedTimestamp(currentTimestamp);
                chat.get().setArchived(false);
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "pin chat: refreshed pin timestamp for {0}", chatJid);
                return MutationApplicationResult.success();
            }

            if (allPinnedChats.size() < MAX_PINNED_CHATS) {
                chat.get().setPinnedTimestamp(currentTimestamp);
                chat.get().setArchived(false);
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "pin chat: pinned {0}, count={1}", chatJid, allPinnedChats.size() + 1);
                return MutationApplicationResult.success();
            }

            if (Log.DEBUG)
                LOGGER.log(Level.DEBUG, "pin chat: cap of {0} reached, evaluating eviction for {1}", MAX_PINNED_CHATS, chatJid);
            this.wamService.commit(new MdSyncdDogfoodingFeatureUsageEventBuilder()
                    .mdSyncdDogfoodingFeature(MdFeatureCode.UNPIN_4TH_CHAT_MUTATION)
                    .build());
            emitPinnedChatsMaxAlert();

            var oldestPinned = allPinnedChats.stream()
                    .min(Comparator.comparing(c -> c.pinnedTimestamp().orElse(Instant.EPOCH)))
                    .orElseThrow();
            var oldestTimestamp = oldestPinned.pinnedTimestamp().orElse(Instant.EPOCH);

            if (oldestTimestamp.isBefore(currentTimestamp)) {
                oldestPinned.setPinnedTimestamp(null);
                queueUnpinMutation(client, oldestPinned.jid(), currentTimestamp);
                chat.get().setPinnedTimestamp(currentTimestamp);
                chat.get().setArchived(false);
                if (Log.DEBUG)
                    LOGGER.log(Level.DEBUG, "pin chat: evicted oldest pin {0}, pinned {1}", oldestPinned.jid(), chatJid);
                return MutationApplicationResult.success();
            }

            queueUnpinMutation(client, chatJid, currentTimestamp);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "pin chat: rejected pin for {0}, cap already newer", chatJid);
            return MutationApplicationResult.success();
        } catch (Exception e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "pin chat mutation failed", e);
            return MutationApplicationResult.failed();
        }
    }

    /**
     * Commits the max-pinned-chats alert telemetry beacon when an incoming
     * pin action is rejected because the {@value #MAX_PINNED_CHATS} chat cap
     * is already reached.
     *
     * <p>WA Web surfaces a premium-upsell alert at this exact point and reports
     * the user's premium standing together with whether the alert's "add to
     * list" or "subscribe" upsell option was selected. A headless client shows
     * no interactive alert, so the two selection flags are reported as
     * {@code false} (no upsell option is chosen) and the premium status
     * resolves to {@link PremiumStatusType#DISABLED}.
     *
     * @implNote
     * This implementation fixes the {@link PinnedChatsMaxAlertEvent#premiumStatus()}
     * field to {@link PremiumStatusType#DISABLED} because Cobalt implements no
     * Aura premium gating; WA Web derives the value from
     * {@code WAWebAuraGating.isPinnedChatsBenefitActive} /
     * {@code isPinnedChatsEnabled}, and the free-tier cap of
     * {@value #MAX_PINNED_CHATS} is only enforced precisely when that benefit
     * is inactive, so {@code DISABLED} is the sole reachable value at this
     * branch. The {@link PinnedChatsMaxAlertEvent#addToListSelected()} and
     * {@link PinnedChatsMaxAlertEvent#subscribeSelected()} selection flags,
     * which WA Web populates from later alert button taps, are reported as
     * {@code false} because a headless client never renders the alert.
     */
    @WhatsAppWebExport(moduleName = "WAWebPinnedChatsWamUtils", exports = "logPinnedChatsMaxAlert", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitPinnedChatsMaxAlert() {
        this.wamService.commit(new PinnedChatsMaxAlertEventBuilder()
                .premiumStatus(PremiumStatusType.DISABLED)
                .addToListSelected(false)
                .subscribeSelected(false)
                .build());
    }

    /**
     * Applies a pin mutation targeting a newsletter JID.
     *
     * <p>Mirrors the cap-eviction behaviour of the chat path
     * (already-pinned passthrough, cap check, oldest-vs-incoming eviction,
     * pending-unpin queue) but against the separate newsletter-pin store.
     *
     * @implNote
     * This implementation tracks newsletter pin state via a dedicated
     * {@code newsletterPinStates} map on
     * {@link LinkedWhatsAppStore} keyed by the
     * newsletter JID, because Cobalt's
     * {@link com.github.auties00.cobalt.wire.linked.newsletter.Newsletter} model has
     * no {@code pinnedTimestamp} field.
     *
     * @param client            the {@link LinkedWhatsAppClient} whose store is mutated
     * @param newsletterJid     the newsletter JID
     * @param action            the pin action carrying the {@code pinned} flag
     * @param currentTimestamp  the mutation timestamp
     * @return the detailed application result
     */
    private MutationApplicationResult applyNewsletterPinMutation(
            LinkedWhatsAppClient client,
            Jid newsletterJid,
            PinAction action,
            Instant currentTimestamp
    ) {
        var newsletter = client.store().chatStore().findNewsletterByJid(newsletterJid);
        if (newsletter.isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "pin chat: orphan newsletter {0}", newsletterJid);
            return MutationApplicationResult.orphan(newsletterJid.toString(), "Newsletter");
        }

        if (!action.pinned()) {
            client.store().chatStore().removeNewsletterPin(newsletterJid);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "pin chat: unpinned newsletter {0}", newsletterJid);
            return MutationApplicationResult.success();
        }

        var existing = client.store().chatStore().findNewsletterPin(newsletterJid).orElse(null);
        if (existing != null) {
            client.store().chatStore().putNewsletterPin(new NewsletterPinBuilder().newsletterJid(newsletterJid).pinnedAt(currentTimestamp).build());
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "pin chat: refreshed newsletter pin timestamp for {0}", newsletterJid);
            return MutationApplicationResult.success();
        }

        var pins = client.store().chatStore().newsletterPinStates();
        if (pins.size() < MAX_PINNED_NEWSLETTERS) {
            client.store().chatStore().putNewsletterPin(new NewsletterPinBuilder().newsletterJid(newsletterJid).pinnedAt(currentTimestamp).build());
            if (Log.DEBUG)
                LOGGER.log(Level.DEBUG, "pin chat: pinned newsletter {0}, count={1}", newsletterJid, pins.size() + 1);
            return MutationApplicationResult.success();
        }

        if (Log.DEBUG)
            LOGGER.log(Level.DEBUG, "pin chat: newsletter cap of {0} reached, evaluating eviction for {1}", MAX_PINNED_NEWSLETTERS, newsletterJid);
        this.wamService.commit(new MdSyncdDogfoodingFeatureUsageEventBuilder()
                .mdSyncdDogfoodingFeature(MdFeatureCode.UNPIN_4TH_CHAT_MUTATION)
                .build());

        var oldest = pins.stream()
                .min(Comparator.comparing(NewsletterPin::pinnedAt))
                .orElseThrow();

        if (oldest.pinnedAt().isBefore(currentTimestamp)) {
            client.store().chatStore().removeNewsletterPin(oldest.newsletterJid());
            client.store().chatStore().putNewsletterPin(new NewsletterPinBuilder().newsletterJid(newsletterJid).pinnedAt(currentTimestamp).build());
            queueUnpinMutation(client, oldest.newsletterJid(), currentTimestamp);
            if (Log.DEBUG)
                LOGGER.log(Level.DEBUG, "pin chat: evicted oldest newsletter pin {0}, pinned {1}", oldest.newsletterJid(), newsletterJid);
            return MutationApplicationResult.success();
        }

        queueUnpinMutation(client, newsletterJid, currentTimestamp);
        if (Log.DEBUG)
            LOGGER.log(Level.DEBUG, "pin chat: rejected newsletter pin for {0}, cap already newer", newsletterJid);
        return MutationApplicationResult.success();
    }

    /**
     * Queues a pending unpin mutation so the rejection propagates to other
     * linked devices.
     *
     * @implNote
     * This implementation mirrors the WA Web
     * {@code WAWebPinChatSync.createPendingUnpin}: it builds a pin mutation
     * with {@code pinned = false} via
     * {@link #getPinMutation(Instant, boolean, Jid)} and appends it to the
     * pending mutations table.
     *
     * @param client    the {@link LinkedWhatsAppClient} whose store receives the queued mutation
     * @param chatJid   the chat or newsletter JID to unpin
     * @param timestamp the mutation timestamp
     */
    private void queueUnpinMutation(LinkedWhatsAppClient client, Jid chatJid, Instant timestamp) {
        var pending = getPinMutation(timestamp, false, chatJid);
        client.store().syncStore().addPendingMutations(collectionName(), List.of(pending));
    }

    /**
     * Builds a single pin mutation for the given chat or newsletter JID.
     *
     * <p>The public outgoing-mutation factory equivalent of this method lives
     * on {@link com.github.auties00.cobalt.sync.factory.PinChatMutationFactory}.
     *
     * @implNote
     * This implementation mirrors the WA Web
     * {@code WAWebPinChatSync.getPinMutation}: it builds a {@link PinAction}
     * with the requested {@code pinned} flag, wraps it in a
     * {@link SyncActionValue}, and
     * serialises the mutation index as the JSON array
     * {@code ["pin_v1", chatJid]}. The WA Web LID 1:1 migration is not
     * implemented, so the chat-jid index resolves to the JID's canonical
     * string form.
     *
     * @param timestamp the mutation timestamp
     * @param pinned    whether the chat should be pinned ({@code true}) or unpinned ({@code false})
     * @param chatJid   the JID of the chat or newsletter
     * @return the pending mutation for the pin action
     */
    private static SyncPendingMutation getPinMutation(Instant timestamp, boolean pinned, Jid chatJid) {
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
     * Builds the set of pending mutations needed to pin or unpin a chat or
     * newsletter.
     *
     * <p>Returns a single-element list carrying just the pin (or unpin)
     * mutation. Callers that need WA Web's pin+unarchive sequence must
     * explicitly build and append an archive mutation after this method.
     *
     * @implNote
     * This implementation diverges from WA Web's
     * {@code WAWebPinChatSync.getMutationsForPin} in two ways: the
     * {@link MdFeatureCode#PIN_MUTATION} WAM emission is moved to the caller
     * (the static helper has no {@link WamService} handle), and the
     * pin+unarchive append is not bundled here because building an archive
     * mutation requires a message range that the caller must construct via the
     * higher-level archive sync infrastructure.
     *
     * @param timestamp the mutation timestamp
     * @param pinned    whether the chat should be pinned
     * @param chatJid   the JID of the chat or newsletter
     * @return the list of pending mutations for the pin operation
     */
    public static List<SyncPendingMutation> getMutationsForPin(Instant timestamp, boolean pinned, Jid chatJid) {
        var mutations = new ArrayList<SyncPendingMutation>();
        mutations.add(getPinMutation(timestamp, pinned, chatJid));
        return mutations;
    }
}
