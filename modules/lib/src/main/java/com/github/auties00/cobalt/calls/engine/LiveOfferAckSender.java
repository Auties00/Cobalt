package com.github.auties00.cobalt.calls.engine;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Ships the offer envelope over the owning client and returns the server's synchronous call ack.
 *
 * <p>The controller builds the {@code <call>} offer envelope and hands it here as a built {@link Stanza},
 * whereas the client's value returning send accepts a {@link StanzaBuilder} and stamps the correlation id
 * itself. This sender therefore reads the envelope's recipient and its single offer child, rebuilds them
 * into a fresh {@link StanzaBuilder}, and blocks the calling virtual thread on
 * {@link LinkedWhatsAppClient#sendNode(StanzaBuilder)} until the matching {@code <ack class="call">}
 * returns.
 *
 * @param whatsapp the owning client whose id correlated send carries the offer envelope and blocks for the
 *                 ack
 */
public record LiveOfferAckSender(LinkedWhatsAppClient whatsapp) implements OfferAckSender {
    /**
     * The logger for {@link LiveOfferAckSender}.
     */
    private static final System.Logger LOGGER = Log.get(LiveOfferAckSender.class);

    /**
     * Validates the client backing this sender.
     *
     * @throws NullPointerException if {@code whatsapp} is {@code null}
     */
    public LiveOfferAckSender {
        Objects.requireNonNull(whatsapp, "whatsapp cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation copies the {@code to} recipient and the single offer child off the built
     * envelope into a fresh {@link StanzaBuilder} so the client send can stamp the correlation id, then
     * blocks for the matching {@code <ack class="call">}. A missing recipient or missing offer child, and any
     * failure shipping the envelope, surface as a non fatal {@link WhatsAppCallException.DataChannel}.
     */
    @Override
    public Stanza sendOfferAndAwaitAck(Stanza offerEnvelope) {
        Objects.requireNonNull(offerEnvelope, "offerEnvelope cannot be null");
        var to = offerEnvelope.getAttributeAsJid("to")
                .orElseThrow(() -> new WhatsAppCallException.DataChannel("offer envelope has no recipient"));
        var child = offerEnvelope.getChild()
                .orElseThrow(() -> new WhatsAppCallException.DataChannel("offer envelope has no action"));
        var builder = new StanzaBuilder()
                .description(offerEnvelope.description())
                .attribute("to", to)
                .content(child);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sending call offer to {0}", to);
        try {
            var ack = whatsapp.sendNode(builder);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "received call offer ack from {0}", to);
            return ack;
        } catch (RuntimeException exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to send call offer", exception);
            throw new WhatsAppCallException.DataChannel("could not send call offer", exception);
        }
    }
}
