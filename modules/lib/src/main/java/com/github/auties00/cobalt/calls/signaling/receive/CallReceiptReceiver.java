package com.github.auties00.cobalt.calls.signaling.receive;

import com.github.auties00.cobalt.ack.AckClass;
import com.github.auties00.cobalt.ack.AckSender;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stream.SocketStreamHandler;

import java.lang.System.Logger.Level;

/**
 * Acknowledges an inbound VoIP {@code <receipt>} stanza back to the server.
 *
 * <p>The server delivers a {@code <receipt>} carrying an {@code <offer>}, {@code <accept>}, or
 * {@code <reject>} child to confirm that one of the caller's outbound signaling legs reached its
 * destination. This handler reads the sender and stanza metadata and ships an {@code <ack>} back so the
 * server stops retransmitting: the acknowledgement echoes the inbound {@code id}, targets the original
 * sender, stamps the local user's phone number JID (with the device suffix stripped) as its {@code from}
 * attribute, sets {@code class} to {@code "receipt"}, and preserves the inbound {@code type} when present.
 *
 * <p>This is the {@code <receipt>} stream counterpart to {@link CallReceiver}, which acknowledges the
 * offer, accept, reject, and rekey legs that arrive inside a {@code <call>} envelope. The two paths share
 * the same {@code <receipt>} acknowledgement shape but are reached from different stream tags: the receipt
 * stream dispatcher routes the server's signaling confirmations here, while the call stream routes inbound
 * call actions to {@link CallReceiver}.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleVoipCallReceipt")
public final class CallReceiptReceiver extends SocketStreamHandler.Concurrent {
    /**
     * The logger for {@link CallReceiptReceiver}.
     */
    private static final System.Logger LOGGER = Log.get(CallReceiptReceiver.class);

    /**
     * Holds the WhatsApp client whose store supplies the local user's device JID for the outbound
     * acknowledgement.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * Holds the {@link AckSender} that emits the outbound {@code <ack class="receipt">} stanza, with the
     * local user's phone number echoed back as the {@code from} attribute.
     */
    private final AckSender ackSender;

    /**
     * Constructs a call receipt receiver bound to its client and acknowledgement sender.
     *
     * @param whatsapp  the WhatsApp client whose store supplies the local user's device JID
     * @param ackSender the {@link AckSender} that emits the outbound {@code <ack class="receipt">} stanza
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleVoipCallReceipt", exports = "handleCallReceipt",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public CallReceiptReceiver(LinkedWhatsAppClient whatsapp, AckSender ackSender) {
        this.whatsapp = whatsapp;
        this.ackSender = ackSender;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads the sender JID from the {@code from} attribute, the stanza identifier from the {@code id}
     * attribute, and the optional receipt type from the {@code type} attribute, then sends an {@code <ack>}
     * carrying those values plus the local user's phone number JID (with the device suffix stripped) as its
     * {@code from} attribute. The stanza is ignored, with a warning logged, when it carries no recognized
     * {@code <offer>}, {@code <accept>}, or {@code <reject>} child, when the {@code from} attribute is
     * missing, or when the {@code id} attribute is missing. The acknowledgement is also dropped without
     * logging when the local JID is not yet available, which occurs before the connection is ready.
     *
     * @param stanza the inbound {@code <receipt>} stanza expected to carry an {@code <offer>},
     *             {@code <accept>}, or {@code <reject>} child
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleVoipCallReceipt", exports = "handleCallReceipt",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void handle(Stanza stanza) {
        if (!stanza.hasChild("offer", "accept", "reject")) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "unrecognized call receipt stanza: {0}", stanza);
            return;
        }

        var from = stanza.getAttributeAsJid("from", null);
        if (from == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "call receipt missing from attribute: {0}", stanza);
            return;
        }

        var stanzaId = stanza.getAttributeAsString("id", null);
        if (stanzaId == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "call receipt missing id attribute: {0}", stanza);
            return;
        }

        var meDevicePn = whatsapp.store().accountStore().jid().orElse(null);
        if (meDevicePn == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "dropping call receipt ack: local jid not yet available");
            return;
        }

        ackSender.ack(AckClass.RECEIPT, stanza)
                .from(meDevicePn.toUserJid())
                .send();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sent call receipt ack id={0}", stanzaId);
    }
}
