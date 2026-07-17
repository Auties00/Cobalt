package com.github.auties00.cobalt.stream.newsletter;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.listener.NewMessageListener;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerSpec;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.core.message.MessageStatus;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfo;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfoBuilder;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.lang.System.Logger.Level;
import java.time.Instant;

/**
 * Handles inbound {@code <status>} stanzas pushed from newsletter (channel) servers.
 *
 * <p>This handler powers the WhatsApp Channels delivery surface. Every text or media post published
 * on a channel the current account follows arrives as a {@code <status>} stanza, is decoded and
 * persisted into the owning {@code Newsletter} store entry, and is surfaced to registered listeners
 * through an {@code onNewMessage} callback. Reaction and reaction-revoke variants are acknowledged
 * but not materialised, and admin-revoke variants ({@code edit="8"}) are logged and skipped.
 *
 * @implNote
 * This implementation decodes the {@code <plaintext>} child via {@link LinkedMessageContainerSpec#decode(byte[])}
 * on the dispatch thread; decode failures are demoted to a debug log rather than thrown because a
 * single malformed channel post must not poison the stream. Admin-revoke ({@code edit="8"}) stanzas
 * are dropped rather than projected into synthetic protocol-revoke messages, leaving that mapping for
 * a future higher-layer pathway.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleNewsletterStatus")
@WhatsAppWebModule(moduleName = "WAWebNewsletterStatusUtils")
public final class NewsletterStatusStreamHandler extends SocketStreamHandler.Concurrent {
    /**
     * Logs debug-level diagnostics while parsing newsletter status stanzas.
     *
     * <p>Receives debug entries for skipped revoke stanzas, missing plaintext payloads, and
     * {@link LinkedMessageContainerSpec#decode(byte[])} failures. Downstream code never relies on these
     * messages.
     */
    private static final System.Logger LOGGER = Log.get(NewsletterStatusStreamHandler.class);

    /**
     * Holds the owning {@link LinkedWhatsAppClient} used to access the store and broadcast new-message
     * events to registered listeners.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * Constructs a handler bound to the given {@link LinkedWhatsAppClient}.
     *
     * <p>Invoked by the socket-stream wiring at client construction; application code does not
     * instantiate handlers directly.
     *
     * @param whatsapp the owning {@link LinkedWhatsAppClient} instance
     */
    public NewsletterStatusStreamHandler(LinkedWhatsAppClient whatsapp) {
        this.whatsapp = whatsapp;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Decodes the inbound newsletter post, persists it into the owning channel's store entry, and
     * dispatches an {@code onNewMessage} callback to every listener registered on the store. The
     * stanza is dropped without effect when its {@code from} attribute is absent or does not name a
     * newsletter server, when its {@code id} attribute is absent, when it carries a {@code reaction}
     * child, when its {@code edit} attribute equals {@code "8"} (admin revoke), or when its
     * {@code plaintext} child is absent or empty. A built message carries the stanza's
     * {@code server_id}, the {@code t} timestamp, and {@link MessageStatus#DELIVERED}; the message key
     * marks the post as outgoing when {@code is_sender} equals {@code "true"}.
     *
     * @implNote
     * This implementation drops admin-revoke ({@code edit="8"}) stanzas rather than synthesising a
     * protocol-revoke message; the revoke is left for a future higher-layer pathway. All other early
     * returns happen before {@link LinkedMessageContainerSpec#decode(byte[])} runs.
     */
    @Override
    public void handle(Stanza stanza) {
        var from = stanza.getAttributeAsJid("from", null);
        if (from == null || !from.hasNewsletterServer()) {
            return;
        }

        var id = stanza.getAttributeAsString("id", null);
        if (id == null) {
            return;
        }

        if (stanza.hasChild("reaction")) {
            return;
        }

        var edit = stanza.getAttributeAsString("edit", null);
        if ("8".equals(edit)) {
            // TODO: project admin revoke (edit="8") into a synthetic protocol
            //       message the way WAWebNewsletterStatusUtils.mapStatusRevokeToMsgData
            //       does, rather than dropping it here.
            if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                    "skipping newsletter status revoke {0} from {1}", id, from);
            return;
        }

        var plaintext = stanza.getChild("plaintext")
                .flatMap(Stanza::toContentBytes)
                .orElse(null);
        if (plaintext == null || plaintext.length == 0) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                    "ignoring newsletter status {0} with no plaintext content", id);
            return;
        }

        var message = decodeMessage(id, plaintext);
        if (message == null) {
            return;
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "storing newsletter message {0} from {1}", id, from);

        var serverId = stanza.getAttributeAsInt("server_id", 0);
        var timestamp = resolveTimestamp(stanza);
        var isSender = "true".equals(stanza.getAttributeAsString("is_sender", null));

        var key = new MessageKeyBuilder()
                .id(id)
                .parentJid(from)
                .fromMe(isSender)
                .build();

        var info = new NewsletterMessageInfoBuilder()
                .key(key)
                .serverId(serverId)
                .timestamp(timestamp)
                .message(message)
                .status(MessageStatus.DELIVERED)
                .build();

        storeMessage(from, info);
        notifyNewMessage(info);
    }

    /**
     * Decodes the raw plaintext payload carried by a newsletter status stanza into a typed
     * {@link LinkedMessageContainer}.
     *
     * <p>Returns {@code null} rather than throwing when the bytes cannot be parsed, so that a single
     * unparseable stanza cannot abort the dispatch loop.
     *
     * @implNote
     * This implementation logs the failure at {@code DEBUG} level and suppresses the exception; the
     * {@code <status>} stanza is treated as if it had never arrived.
     *
     * @param id        the stanza identifier, included in the debug log message for traceability
     * @param plaintext the raw protobuf bytes lifted from the {@code <plaintext>} child stanza
     * @return the decoded {@link LinkedMessageContainer}, or {@code null} when the bytes cannot be parsed
     */
    private LinkedMessageContainer decodeMessage(String id, byte[] plaintext) {
        try {
            return LinkedMessageContainerSpec.decode(plaintext);
        } catch (Exception exception) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                    "failed to decode newsletter status " + id, exception);
            return null;
        }
    }

    /**
     * Resolves the {@link Instant} represented by the stanza's {@code t} attribute.
     *
     * <p>Newsletter status stanzas carry their server-side publish time as an epoch-second long. This
     * method parses that long into an {@link Instant}, returning {@code null} when the attribute is
     * absent.
     *
     * @param stanza the {@code <status>} stanza stanza
     * @return the parsed {@link Instant}, or {@code null} when the attribute is missing
     */
    private Instant resolveTimestamp(Stanza stanza) {
        var timestamp = stanza.getAttributeAsLong("t", (Long) null);
        return timestamp == null ? null : Instant.ofEpochSecond(timestamp);
    }

    /**
     * Appends the decoded post to the newsletter's message collection and bumps the newsletter-level
     * metadata.
     *
     * <p>Looks up the {@link com.github.auties00.cobalt.wire.linked.newsletter.Newsletter} store entry for
     * the given channel, lazily creating it when the channel has never been seen on this device. The
     * channel timestamp is updated to the post's timestamp regardless of authorship, the unread
     * counter is incremented only for posts the current account did not author, and the post is then
     * added to the channel's message collection.
     *
     * @param newsletterJid the channel {@link Jid} that owns the post
     * @param info          the decoded {@link NewsletterMessageInfo} to persist
     */
    private void storeMessage(Jid newsletterJid, NewsletterMessageInfo info) {
        var newsletter = whatsapp.store().chatStore().findNewsletterByJid(newsletterJid)
                .orElseGet(() -> whatsapp.store().chatStore().addNewNewsletter(newsletterJid));
        newsletter.setTimestamp(info.timestamp().orElse(null));
        if (!info.key().fromMe()) {
            newsletter.setUnreadMessagesCount(newsletter.unreadMessagesCount() + 1);
        }
        newsletter.addMessage(info);
    }

    /**
     * Broadcasts an
     * {@link LinkedWhatsAppClientListener#onNewMessage(LinkedWhatsAppClient, LinkedMessageInfo)}
     * callback to every listener registered on the store, surfacing inbound channel posts on the
     * public listener surface.
     *
     * @implNote
     * This implementation dispatches each callback on its own virtual thread so that a slow listener
     * cannot block the socket-stream dispatch loop or starve other listeners.
     *
     * @param info the decoded {@link LinkedMessageInfo} to publish
     */
    private void notifyNewMessage(LinkedMessageInfo info) {
        for (var listener : whatsapp.store().listeners()) {
            if (listener instanceof NewMessageListener typed) {
                Thread.startVirtualThread(() -> typed.onNewMessage(whatsapp, info));
            }
        }
    }
}
