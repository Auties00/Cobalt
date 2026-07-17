package com.github.auties00.cobalt.calls.engine.control;

import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Dispatches a call link or waiting room request reply IQ over the client's id correlated socket and
 * returns the echoed action stanza of the reply.
 *
 * <p>This is the live {@link CallLinkIqSender} the call link and waiting room control units depend on. It
 * wraps the typed request's {@linkplain CallMessage#toStanza() action stanza} in a {@code <call>} envelope
 * addressed to the {@code call} service and blocks on
 * {@link LinkedWhatsAppClient#sendNode(StanzaBuilder)} for the matching {@code <ack class="call">} reply,
 * then unwraps the reply's echoed action child so the controller's ack parser receives the
 * {@code <link_query>}, {@code <link_join>}, or {@code <waiting_room>} stanza it expects rather than the
 * {@code <ack>} envelope.
 *
 * <p>The request carries no {@code type} attribute on the {@code <call>} envelope: the service routes the
 * operation on the single child element tag the request record renders, so the child tag alone selects
 * query, admit, or deny. The reply is the {@code <ack class="call">} envelope whose single echoed action
 * child each ack parser
 * ({@link com.github.auties00.cobalt.calls.signaling.link.LinkQueryAck#of(Stanza)},
 * {@link com.github.auties00.cobalt.calls.signaling.link.LinkJoinAck#of(Stanza)},
 * {@link com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomAdmitAck#of(Stanza)}) parses, so
 * this sender returns that child; an error reply with no echoed child returns the envelope itself so the
 * parser surfaces the missing required attribute.
 *
 * @param whatsapp the owning client whose id correlated send carries the call link IQ
 */
public record LiveCallLinkIqSender(LinkedWhatsAppClient whatsapp) implements CallLinkIqSender {
    /**
     * The logger for {@link LiveCallLinkIqSender}.
     */
    private static final System.Logger LOGGER = Log.get(LiveCallLinkIqSender.class);

    /**
     * The wire element tag of the call signaling envelope and of its acknowledgement.
     */
    private static final String CALL_ELEMENT = "call";

    /**
     * The wire attribute naming the recipient on the {@code <call>} envelope.
     */
    private static final String TO_ATTRIBUTE = "to";

    /**
     * Canonicalizes the sender over its client.
     *
     * @throws NullPointerException if {@code whatsapp} is {@code null}
     */
    public LiveCallLinkIqSender {
        Objects.requireNonNull(whatsapp, "whatsapp cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation wraps the request's action stanza in a {@code <call to="call">}
     * envelope, blocks on the client's id correlated send for the {@code <ack class="call">} reply, and
     * returns the reply's echoed action child (preferring the child whose tag matches the request, then
     * the single child, then the envelope itself) so the controller's ack parser receives the echoed
     * action stanza rather than the {@code <ack>} wrapper. A send failure surfaces as a non fatal
     * {@link WhatsAppCallException.DataChannel} the controller treats as the operation failing.
     */
    @Override
    public Stanza sendForReply(CallMessage request) {
        Objects.requireNonNull(request, "request cannot be null");
        var action = request.toStanza();
        var builder = new StanzaBuilder()
                .description(CALL_ELEMENT)
                .attribute(TO_ATTRIBUTE, JidServer.call())
                .content(action);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sending call-link iq action={0}", action.description());
        Stanza reply;
        try {
            reply = whatsapp.sendNode(builder);
        } catch (RuntimeException exception) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "call-link iq send failed action=" + action.description(), exception);
            }
            throw new WhatsAppCallException.DataChannel("could not send call-link IQ", exception);
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call-link iq reply received action={0}", action.description());
        return reply.getChild(action.description())
                .or(reply::getChild)
                .orElse(reply);
    }
}
