package com.github.auties00.cobalt.message.receive;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfo;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfoBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.message.MessageStatus;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;

import java.lang.System.Logger.Level;
import java.time.Instant;

/**
 * Inbound receiver that turns a plaintext newsletter {@code <message>} stanza into a
 * fully populated {@link NewsletterMessageInfo}.
 *
 * <p>Selected by {@link MessageReceivingService#process(Stanza)} whenever the {@code from}
 * JID belongs to the {@code @newsletter} server; the resulting posts back the Channels
 * feature. Newsletter messages are not Signal-encrypted, so this receiver skips the
 * entire decryption pipeline used by {@link ChatMessageReceiver}.
 *
 * @implNote
 * This implementation collapses WhatsApp Web's
 * {@code WAWebHandleNewsletterMsg.default} processor and the
 * {@code WAWebNewsletterMsgParser} parser into a single member-by-member parse against
 * the {@link Stanza} attributes; the WA Web path also runs the message through
 * {@code WAWebNewsletterMsgProcessor.preprocessNewsletterMsg} for add-on votes and
 * orphan detection, neither of which Cobalt currently models.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleNewsletterMsg")
final class NewsletterMessageReceiver extends MessageReceiver<NewsletterMessageInfo> {
    /**
     * The logger for {@link NewsletterMessageReceiver}.
     */
    private static final System.Logger LOGGER = Log.get(NewsletterMessageReceiver.class);

    /**
     * Constructs a newsletter receiver bound to the given store.
     *
     * @param store the session store used by the parent {@link MessageReceiver}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleNewsletterMsg", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    NewsletterMessageReceiver(LinkedWhatsAppStore store) {
        super(store);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads {@code id}, {@code t} (seconds since epoch), {@code server_id}, and the
     * optional {@code is_sender} attribute from the stanza, then decodes the protobuf
     * carried inside the {@code <plaintext>} child. Returns {@code null} when the
     * {@code <plaintext>} child is missing or empty; the orchestrator treats this as a
     * silent drop.
     *
     * @implNote
     * This implementation always stamps the resulting
     * {@link NewsletterMessageInfo#status()} as
     * {@link MessageStatus#DELIVERED}; newsletter posts do not carry an
     * end-to-end-ack contract so the status is fixed at receive time.
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleNewsletterMsg", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    NewsletterMessageInfo receive(Stanza stanza, Jid fromJid) {
        var id = stanza.getRequiredAttributeAsString("id");
        var timestampSeconds = stanza.getRequiredAttributeAsLong("t");
        var timestamp = Instant.ofEpochSecond(timestampSeconds);
        var serverId = stanza.getRequiredAttributeAsInt("server_id");
        var isSender = "true".equals(stanza.getAttributeAsString("is_sender", null));

        var plaintext = stanza.getChild("plaintext")
                .flatMap(Stanza::toContentBytes)
                .orElse(null);
        if (plaintext == null || plaintext.length == 0) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                    "newsletter message {0} has no plaintext content", id);
            return null;
        }

        var container = decodeProtobuf(id, plaintext);
        if (container == null) {
            return null;
        }

        var key = new MessageKeyBuilder()
                .id(id)
                .parentJid(fromJid)
                .fromMe(isSender)
                .build();

        var info = new NewsletterMessageInfoBuilder()
                .key(key)
                .serverId(serverId)
                .timestamp(timestamp)
                .message(container)
                .status(MessageStatus.DELIVERED)
                .build();

        if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                "processed newsletter message {0} from {1}", id, fromJid);
        return info;
    }
}
