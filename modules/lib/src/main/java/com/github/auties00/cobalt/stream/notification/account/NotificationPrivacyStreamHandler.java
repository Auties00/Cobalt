package com.github.auties00.cobalt.stream.notification.account;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.github.auties00.cobalt.ack.AckClass;
import com.github.auties00.cobalt.ack.AckSender;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.migration.LidMigrationService;
import com.github.auties00.cobalt.listener.linked.internal.LinkedTrustedContactTokenListener;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.Arrays;

/**
 * Handles {@code type="privacy_token"} notifications carrying server-issued trusted-contact tokens (TC
 * tokens) for peers the user has interacted with.
 *
 * <p>A TC token is a server-rolled blob whose presence on a chat is the trust signal that gates the
 * verified-identity UI affordance for that contact. Receiving a fresh token persists it on the chat
 * record and triggers a presence re-subscription so the UI updates immediately.</p>
 *
 * @implNote This implementation persists the raw token content and its timestamp on the chat record
 * (preferring a LID-keyed chat over a PN-keyed one). WA Web records the same fields plus a server-issued
 * sequence number; Cobalt does not maintain that sequence number because the timestamp guard inside
 * {@link #updateChatTcToken(Jid, Jid, Instant, byte[])} already serialises updates.
 */
@WhatsAppWebModule(moduleName = "WAWebHandlePrivacyTokensNotification")
final class NotificationPrivacyStreamHandler extends SocketStreamHandler.Concurrent {

    /**
     * Logs warnings about malformed stanzas and debug messages about unknown token types.
     */
    private static final System.Logger LOGGER = Log.get(NotificationPrivacyStreamHandler.class);

    /**
     * Holds the client used for store reads and presence re-subscription.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * Holds the ack sender used to ship the post-processing
     * {@code <ack class="notification" type="privacy_token"/>} stanza.
     */
    private final AckSender ackSender;

    /**
     * Constructs the handler with the shared client and ack sender.
     *
     * @param whatsapp  the client
     * @param ackSender the ack sender
     */
    NotificationPrivacyStreamHandler(LinkedWhatsAppClient whatsapp, AckSender ackSender) {
        this.whatsapp = whatsapp;
        this.ackSender = ackSender;
    }

    /**
     * Validates the stanza shape, processes each token child, and always sends the protocol-level ACK.
     *
     * <p>Stanzas whose description is not {@code notification} or whose {@code type} is not
     * {@code privacy_token} are dropped without ACK; valid stanzas are always ACKed even when handling
     * throws.</p>
     *
     * @param stanza the incoming {@code <notification>} stanza
     */
    @Override
    public void handle(Stanza stanza) {
        if (!stanza.hasDescription("notification") || !stanza.hasAttribute("type", "privacy_token")) {
            return;
        }

        try {
            handleNotification(stanza);
        } catch (Throwable throwable) {
            if (Log.WARNING) LOGGER.log(Level.WARNING,
                    "cannot handle privacy_token notification " + stanza.getAttributeAsString("id", "<missing>"),
                    throwable);
        } finally {
            sendNotificationAck(stanza);
        }
    }

    /**
     * Reads the sender PN and optional sender LID, waits for the offline delivery window to end, then
     * iterates each {@code <token>} child by type.
     *
     * <p>The offline-delivery barrier ensures the per-chat record exists before a TC token is applied to
     * it. Each {@code <token>} child is dispatched on its {@code type} attribute; the only recognised
     * type is {@code trusted_contact}.</p>
     *
     * @implNote This implementation only knows the {@code trusted_contact} token type; any other type is
     * debug-logged and dropped, matching WA Web which logs and ignores unknown types.
     *
     * @param stanza the {@code <notification>} stanza
     */
    private void handleNotification(Stanza stanza) {
        var senderPn = getUserJid(stanza, "from");
        var senderLid = stanza.getAttributeAsJid("sender_lid")
                .map(Jid::toUserJid)
                .orElse(null);
        if (senderPn == null) {
            return;
        }

        var tokensNode = stanza.getChild("tokens").orElse(null);
        if (tokensNode == null) {
            return;
        }

        whatsapp.store().connectionStore().waitForOfflineDeliveryEnd();

        for (var tokenNode : tokensNode.getChildren("token")) {
            var type = tokenNode.getAttributeAsString("type", "");
            switch (type) {
                case "trusted_contact" -> handleTrustedContactToken(senderPn, senderLid, tokenNode);
                default -> {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                            "incomingPrivacyTokensParser - receiving an unknown type: {0}", type);
                }
            }
        }
    }

    /**
     * Applies a single {@code trusted_contact} token to the corresponding chat and re-subscribes to the
     * sender's presence.
     *
     * <p>Filters out non-user senders (PSAs, bots, server jids), which cannot carry a verified-identity
     * affordance, then writes the token via {@link #updateChatTcToken(Jid, Jid, Instant, byte[])} and
     * re-subscribes to the sender's presence so the chat thread shows the back-online update once the
     * token lands. Empty token content is ignored.</p>
     *
     * @implNote This implementation debug-logs and ignores presence-subscription failures; WA Web's
     * presence collection retries internally via the presence-queue worker.
     *
     * @param senderPn  the sender's phone-number JID
     * @param senderLid the sender's LID JID, or {@code null} when absent
     * @param tokenStanza the {@code <token type="trusted_contact"/>} child carrying the token bytes and timestamp
     */
    private void handleTrustedContactToken(Jid senderPn, Jid senderLid, Stanza tokenStanza) {
        if (!LidMigrationService.isRegularUser(senderPn)) {
            return;
        }

        var content = tokenStanza.toContentBytes().orElse(null);
        if (content == null || content.length == 0) {
            return;
        }

        var tokenTimestamp = getInstantAttribute(tokenStanza, "t");

        updateChatTcToken(senderPn, senderLid, tokenTimestamp, content);

        try {
            whatsapp.subscribeToPresence(senderPn);
        } catch (Throwable throwable) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                    "cannot resubscribe to presence for tc token sender {0}: {1}",
                    senderPn,
                    throwable.getMessage());
        }
    }

    /**
     * Writes the new TC token bytes and timestamp to the chat record, preferring a LID-keyed chat when
     * one exists and creating a PN-keyed chat record on demand otherwise.
     *
     * <p>Resolves the chat by LID first, then PN (auto-creating a PN-keyed record), and writes the token
     * only when it differs from the stored bytes and is not older than the stored timestamp; an identical
     * replay or a stale timestamp leaves the record untouched.</p>
     *
     * @param senderPn       the sender's phone-number JID
     * @param senderLid      the sender's LID JID, or {@code null}
     * @param tokenTimestamp the server-reported token timestamp, or {@code null}
     * @param tcTokenContent the raw trusted-contact token bytes
     */
    private void updateChatTcToken(Jid senderPn, Jid senderLid, Instant tokenTimestamp, byte[] tcTokenContent) {
        var chat = (senderLid != null
                ? whatsapp.store().chatStore().findChatByJid(senderLid).orElse(null)
                : null);
        if (chat == null) {
            chat = whatsapp.store().chatStore().findChatByJid(senderPn)
                    .orElseGet(() -> whatsapp.store().chatStore().addNewChat(senderPn));
        }

        var existingToken = chat.tcToken().orElse(null);
        var existingTimestamp = chat.tcTokenTimestamp().orElse(null);
        if (existingToken != null && Arrays.equals(existingToken, tcTokenContent)
                || existingTimestamp != null && tokenTimestamp != null && existingTimestamp.isAfter(tokenTimestamp)) {
            return;
        }

        chat.setTcToken(tcTokenContent);
        chat.setTcTokenTimestamp(tokenTimestamp);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "updated trusted-contact token for {0}, {1} bytes", chat.jid(), tcTokenContent.length);

        var peer = (senderLid != null ? senderLid : senderPn).toUserJid();
        for (var listener : whatsapp.store().listeners()) {
            if (listener instanceof LinkedTrustedContactTokenListener internal) {
                internal.onTrustedContactTokenReceived(peer, tcTokenContent);
            }
        }
    }

    /**
     * Reads a JID-valued attribute and reduces it to user form.
     *
     * <p>Used by {@link #handleNotification(Stanza)} to extract the sender's PN from the stanza's
     * {@code from} attribute.</p>
     *
     * @param stanza the stanza to read from
     * @param key  the attribute name
     * @return the parsed user JID, or {@code null} if absent
     */
    private Jid getUserJid(Stanza stanza, String key) {
        return stanza.getAttributeAsJid(key)
                .map(Jid::toUserJid)
                .orElse(null);
    }

    /**
     * Reads an epoch-seconds long attribute and converts it to an {@link Instant}, returning {@code null}
     * for absent or non-positive values.
     *
     * <p>Used by the {@code trusted_contact} branch to parse the token's {@code t} timestamp.</p>
     *
     * @param stanza the stanza to read from
     * @param key  the attribute name
     * @return the parsed instant, or {@code null}
     */
    private Instant getInstantAttribute(Stanza stanza, String key) {
        var seconds = stanza.getAttributeAsLong(key, (Long) null);
        return seconds == null || seconds <= 0 ? null : Instant.ofEpochSecond(seconds);
    }

    /**
     * Sends the {@code <ack class="notification" type="privacy_token"/>} stanza for the processed
     * notification.
     *
     * <p>The ack is fire-and-forget.</p>
     *
     * @param stanza the original {@code <notification>} stanza
     */
    private void sendNotificationAck(Stanza stanza) {
        ackSender.ack(AckClass.NOTIFICATION, stanza).type("privacy_token").send();
    }
}
