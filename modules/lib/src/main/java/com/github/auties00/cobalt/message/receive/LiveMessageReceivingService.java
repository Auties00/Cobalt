package com.github.auties00.cobalt.message.receive;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.message.dedup.MessageDedup;
import com.github.auties00.cobalt.message.receive.crypto.MessageDecryption;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.wam.WamService;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Live implementation of {@link MessageReceivingService} that routes each inbound
 * {@code <message>} stanza to the receiver matching the stanza's address class.
 *
 * <p>The router picks {@link NewsletterMessageReceiver} when the {@code from} JID lives
 * on the {@code @newsletter} server (Channels) and {@link ChatMessageReceiver} for
 * every other address class (1:1, group, broadcast, status, peer).
 *
 * @implNote
 * This implementation mirrors WhatsApp Web's
 * {@code WAWebCommsHandleMessagingStanza.handleMessagingStanza} dispatch but inlines
 * the dedup cache that WA Web gates behind the {@code web_pending_message_cache_enabled}
 * AB prop via {@code WAWebMessageDedupUtils.isPengingMessageCacheEnabled}; Cobalt
 * always dedups so duplicate fanout deliveries are dropped without the AB-prop
 * branch.
 */
@WhatsAppWebModule(moduleName = "WAWebCommsHandleMessagingStanza")
@WhatsAppWebModule(moduleName = "WAWebHandleMsg")
public final class LiveMessageReceivingService implements MessageReceivingService {
    /**
     * The logger for {@link LiveMessageReceivingService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveMessageReceivingService.class);

    /**
     * Receiver invoked for every non-newsletter inbound stanza.
     *
     * <p>Holds the full Signal-protocol decryption pipeline.
     */
    private final ChatMessageReceiver chatReceiver;

    /**
     * Receiver invoked for every {@code @newsletter}-server inbound stanza.
     *
     * <p>Reads the {@code <plaintext>} child directly; no Signal decryption is involved.
     */
    private final NewsletterMessageReceiver newsletterReceiver;

    /**
     * In-flight dedup cache keyed by {@code fromJid:id}.
     *
     * <p>Guards against the same E2E message being processed twice when the server
     * fanout duplicates a delivery during an offline-to-online transition; entries are
     * short-lived and removed in the {@code finally} block of {@link #process(Stanza)}.
     */
    private final MessageDedup dedup;

    /**
     * Constructs the receiving service and assembles the internal receiver graph.
     *
     * <p>The same {@link LinkedWhatsAppStore} used by the rest of the client must be passed so
     * the receivers see consistent self-JID and Signal-session state.
     *
     * @param store      the central session store, shared with the rest of the client
     * @param decryption the Signal-protocol decryption service (PKMSG/MSG/SKMSG) plus
     *                   the MSMSG bot-message scheme used by {@link ChatMessageReceiver}
     * @param wamService
     * @implNote This implementation injects both receivers through the constructor and exposes no
     * service-locator accessor for them; both are package-private and owned solely by
     * this service.
     */
    @WhatsAppWebExport(moduleName = "WAWebCommsHandleMessagingStanza", exports = "handleMessagingStanza",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public LiveMessageReceivingService(
            LinkedWhatsAppStore store,
            MessageDecryption decryption,
            WamService wamService) {
        this.chatReceiver = new ChatMessageReceiver(store, decryption, wamService);
        this.newsletterReceiver = new NewsletterMessageReceiver(store);
        this.dedup = new MessageDedup();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation builds the dedup key as {@code fromJid + ":" + id} and
     * releases it in a {@code finally} so a follow-up message from the same peer is
     * not falsely flagged when the first finishes; WhatsApp Web's
     * {@code WAWebHandleMsg} delegates the same bookkeeping to
     * {@code WAWebMessageDedupUtils.addPendingMessage}, which is also released after
     * the message is fully processed.
     */
    @WhatsAppWebExport(moduleName = "WAWebCommsHandleMessagingStanza", exports = "handleMessagingStanza",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebHandleMsg", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public LinkedMessageInfo process(Stanza stanza) {
        Objects.requireNonNull(stanza, "node");

        var fromJid = stanza.getRequiredAttributeAsJid("from");
        if (fromJid.hasNewsletterServer()) {
            return newsletterReceiver.receive(stanza, fromJid);
        }

        var id = stanza.getRequiredAttributeAsString("id");
        var dedupKey = fromJid + ":" + id;
        if (dedup.isPending(dedupKey)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                    "duplicate message {0}, skipping", id);
            return null;
        }
        dedup.add(dedupKey);

        try {
            return chatReceiver.receive(stanza, fromJid);
        } finally {
            dedup.remove(dedupKey);
        }
    }

    @WhatsAppWebExport(moduleName = "WAWebMessageDedupUtils", exports = "maybeClearPendingMessages",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void clearPendingMessages() {
        dedup.clear();
    }
}
