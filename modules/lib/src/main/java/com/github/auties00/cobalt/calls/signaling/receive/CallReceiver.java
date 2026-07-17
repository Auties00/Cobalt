package com.github.auties00.cobalt.calls.signaling.receive;

import com.github.auties00.cobalt.ack.AckSender;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stream.SocketStreamHandler;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.CallStanza;
import com.github.auties00.cobalt.calls.signaling.session.OfferNoticeStanza;

/**
 * Receives inbound VoIP call signaling stanzas and routes each into classification, acknowledgement,
 * buffering, and engine dispatch.
 *
 * <p>This handler is registered under the {@code "call"} stream tag and is the entry point for every
 * signaling action the server delivers inside a {@code <call>} envelope. It is an
 * {@link SocketStreamHandler.Ordered ordered} handler keyed on the call identifier so all signals for
 * one call are processed in arrival order on a single chain, while signals for distinct calls still
 * run in parallel. Ordering matters because a group's join and the rekey that depends on it must not
 * race.
 *
 * <p>For each inbound stanza the handler reads the {@code <call>} envelope (the sender, the single
 * child payload, the optional {@code sender_lid}), classifies the payload through a
 * {@link CallSignalingRouter}, and acts on the verdict: a malformed or unroutable payload is dropped,
 * a payload whose call object does not yet exist is buffered through a {@link CallMessageBuffer} for
 * later replay, and a routable payload is decoded into a {@link CallMessage} and forwarded to the
 * engine sink. Independently of the verdict the handler emits the wire acknowledgement the payload
 * requires: a {@code <receipt>} for the offer, accept, reject, and rekey legs, and an
 * {@code <ack class="call" type="...">} echoing the payload tag for every other signal, so the server
 * stops retransmitting regardless of whether the local engine could act on the message.
 *
 * <p>The decoded {@link CallMessage} is handed to a sink supplied at construction rather than to a
 * fixed engine reference, so the signaling layer stays decoupled from the lifecycle controller that
 * consumes the messages; the integrator wires the sink to the engine. The sink receives the decoded
 * message together with the envelope {@code from}, the authoring device {@link Jid} the engine needs
 * as the decryption sender and as the companion device discriminator the terminate guards key on,
 * because that sender is the {@code <call>} envelope's attribute rather than a field of the decoded
 * action. The handler never throws out of its work method for a single bad stanza: parse failures are
 * dropped, and the ordered base absorbs any escaping throwable.
 *
 * @implNote This implementation reads the {@link Stanza} tree directly rather than translating the
 * envelope into an intermediate signaling node form, because a {@link Stanza} is already the fully
 * parsed {@code <call>} element. An unsupported or unroutable payload still receives the
 * {@code <ack class="call">} the server expects for its envelope, so the server stops retransmitting
 * even when the local engine cannot act on the message.
 */
public final class CallReceiver extends SocketStreamHandler.Ordered {
    /**
     * The logger for {@link CallReceiver}.
     */
    private static final System.Logger LOGGER = Log.get(CallReceiver.class);

    /**
     * The stream tag this handler is registered under.
     */
    public static final String STREAM_TAG = "call";

    /**
     * The wire attribute naming the sender on the {@code <call>} envelope.
     */
    private static final String FROM_ATTRIBUTE = "from";

    /**
     * The wire attribute naming the sender LID on the {@code <call>} envelope.
     */
    private static final String SENDER_LID_ATTRIBUTE = "sender_lid";

    /**
     * The wire attribute naming the call identifier on a {@code <call>} child element.
     */
    private static final String CALL_ID_ATTRIBUTE = "call-id";

    /**
     * The wire attribute naming the call creator's device JID on a {@code <call>} child element.
     */
    private static final String CALL_CREATOR_ATTRIBUTE = "call-creator";

    /**
     * The fallback {@code call-id} used as the ordering key when a payload carries none, so a malformed
     * payload is still serialised on one chain rather than spreading across the key space.
     */
    private static final String UNKEYED_ORDERING_KEY = "";

    /**
     * The set of payload tags that create a call, so a buffered one is drained and replayed at once.
     *
     * <p>An offer is the signal that creates a call: the call object does not exist when the offer
     * arrives, so unlike a transport or rekey message that must wait for its call, the offer is what
     * brings the call into being. The offer is therefore forwarded to the engine first, which creates
     * and rings the call, and only then are any messages other than the offer that raced ahead of it and
     * were buffered replayed against the now live call in arrival order; a racer replayed before the
     * offer created the call would reach no call object and be dropped, so the offer always precedes the
     * buffered racers.
     */
    private static final Set<String> CALL_CREATING_TAGS = Set.of("offer");

    /**
     * Emits the wire acknowledgement each inbound payload requires, choosing between a
     * {@code <receipt>} for the offer, accept, reject, and rekey legs and an
     * {@code <ack class="call">} for every other signal.
     */
    private final CallSignalingAcknowledger acknowledger;

    /**
     * Classifies each inbound payload into its signaling type and routing verdict.
     */
    private final CallSignalingRouter router;

    /**
     * Buffers payloads whose call object does not yet exist for later replay.
     */
    private final CallMessageBuffer buffer;

    /**
     * Reports whether a call object already exists for a given call identifier, so the router can
     * decide whether a payload is processed now or buffered.
     */
    private final Predicate<String> callExists;

    /**
     * Receives each decoded routable {@link CallMessage} together with the envelope {@code from} for
     * engine handling.
     */
    private final BiConsumer<CallMessage, Jid> sink;

    /**
     * Receives each decoded {@link OfferNoticeStanza} for handling outside the call engine.
     */
    private final Consumer<OfferNoticeStanza> offerNoticeSink;

    /**
     * Constructs a call receiver bound to its client, acknowledgement sender, router, buffer,
     * call existence predicate, and engine sink.
     *
     * @param whatsapp   the client used to send receipts and read the local account identity
     * @param ackSender  the {@link AckSender} used to emit the {@code <ack class="call">} for
     *                   signals that are not receipts
     * @param router     the {@link CallSignalingRouter} that classifies each payload
     * @param buffer     the {@link CallMessageBuffer} that holds payloads whose call object does not
     *                   yet exist
     * @param callExists a predicate reporting whether a call object already exists for a call
     *                   identifier
     * @param sink       the consumer that receives each decoded routable {@link CallMessage} together
     *                   with the envelope {@code from} that authored it
     * @param offerNoticeSink the consumer that receives each decoded {@link OfferNoticeStanza}, which is
     *                   handled outside the call engine
     * @throws NullPointerException if any argument is {@code null}
     */
    public CallReceiver(LinkedWhatsAppClient whatsapp, AckSender ackSender, CallSignalingRouter router,
                              CallMessageBuffer buffer, Predicate<String> callExists,
                              BiConsumer<CallMessage, Jid> sink, Consumer<OfferNoticeStanza> offerNoticeSink) {
        this.acknowledger = new CallSignalingAcknowledger(whatsapp, ackSender);
        this.router = Objects.requireNonNull(router, "router cannot be null");
        this.buffer = Objects.requireNonNull(buffer, "buffer cannot be null");
        this.callExists = Objects.requireNonNull(callExists, "callExists cannot be null");
        this.sink = Objects.requireNonNull(sink, "sink cannot be null");
        this.offerNoticeSink = Objects.requireNonNull(offerNoticeSink, "offerNoticeSink cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * Returns the payload's {@code call-id} so every signal for one call is serialised on one chain;
     * a malformed envelope with no payload, or a payload with no {@code call-id}, falls back to a
     * single shared key so it is still processed in arrival order rather than spread across the key
     * space.
     *
     * @param stanza the inbound {@code <call>} stanza
     * @return the ordering key, never {@code null}
     */
    @Override
    protected String orderingKey(Stanza stanza) {
        return stanza.getChild()
                .flatMap(payload -> payload.getAttributeAsString(CALL_ID_ATTRIBUTE))
                .orElse(UNKEYED_ORDERING_KEY);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads the {@code <call>} envelope, classifies the single child payload, emits the wire
     * acknowledgement the payload requires, and either drops, buffers, or decodes and forwards the
     * payload per the classification verdict. A stanza with no sender or no payload is dropped without
     * acknowledgement because there is nothing to acknowledge.
     *
     * @param stanza the inbound {@code <call>} stanza
     */
    @Override
    public void handle(Stanza stanza) {
        var from = stanza.getAttributeAsJid(FROM_ATTRIBUTE, null);
        var payload = stanza.getChild().orElse(null);
        if (from == null || payload == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ignoring malformed call stanza: {0}", stanza);
            return;
        }

        if (OfferNoticeStanza.ELEMENT.equals(payload.description())) {
            handleOfferNotice(stanza, payload, from);
            return;
        }

        var senderLid = stanza.getAttributeAsJid(SENDER_LID_ATTRIBUTE, null);
        var callId = payload.getAttributeAsString(CALL_ID_ATTRIBUTE, null);
        var callCreator = payload.getAttributeAsJid(CALL_CREATOR_ATTRIBUTE, null);
        var verdict = router.classify(payload.description(), callId, callCreator, senderLid,
                callId != null && callExists.test(callId));

        acknowledger.acknowledge(stanza, payload, verdict.callId().orElse(null), from,
                verdict.callCreator().orElse(null));

        switch (verdict.disposition()) {
            case DROP -> {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "dropping call payload {0}: {1}", payload.description(), stanza);
                }
            }
            case BUFFER -> verdict.callId().ifPresent(id -> bufferAndMaybeReplay(id, payload, from));
            case PROCESS -> forward(payload, from);
        }
    }

    /**
     * Acknowledges an {@code <offer_notice>} and hands the decoded notice to the sink outside the engine.
     *
     * <p>An offer notice is not a voip engine signaling action: it does not create, advance, or buffer a
     * call object. It is acknowledged with the same {@code <ack class="call" type="offer_notice">} the
     * server expects for any signal that is not a receipt, then decoded through
     * {@link OfferNoticeStanza#of(Stanza)} and forwarded to {@link #offerNoticeSink}, which surfaces it
     * to the application and records it in the call history. A notice that cannot be decoded is dropped
     * after acknowledgement so the server stops retransmitting.
     *
     * @param stanza  the inbound {@code <call>} envelope
     * @param payload the {@code <offer_notice>} child element
     * @param from    the envelope {@code from} sender, threaded from {@link #handle(Stanza)} so the
     *                acknowledgement does not parse it again
     */
    private void handleOfferNotice(Stanza stanza, Stanza payload, Jid from) {
        acknowledger.acknowledge(stanza, payload, payload.getAttributeAsString(CALL_ID_ATTRIBUTE).orElse(null),
                from, null);
        OfferNoticeStanza.of(stanza).ifPresentOrElse(offerNoticeSink, () -> {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "could not decode offer_notice: {0}", stanza);
        });
    }

    /**
     * Buffers a payload that is not yet routable, or forwards the offer that creates a call and replays
     * any payloads that raced ahead of it.
     *
     * <p>A payload other than an offer that races ahead of its call is buffered through
     * {@link CallMessageBuffer} and stays buffered until the offer arrives. An offer is the signal that
     * brings the call into being, so it is forwarded to the engine sink first, creating the call from the
     * offer and ringing it; only then, and only when
     * {@link CallMessageBuffer#hasBufferedMessages(String)} reports that messages raced ahead of it, is
     * the call's buffer drained and replayed against the now live call in arrival order with the offer
     * envelope's {@code from}. Forwarding the offer before the buffered racers is required: a racer
     * replayed while the call object still did not exist would be forwarded to a call the lifecycle
     * controller does not track and dropped. This is the production caller that makes a fresh inbound
     * offer reach the lifecycle controller and ring.
     *
     * <p>The {@link CallMessageBuffer} stores only the payload {@link Stanza}, not the sender of the
     * envelope each was carried in, so a drained payload that raced ahead of the offer is replayed under
     * the offer's envelope sender rather than its own. This matches the engine, which processes the offer
     * and the messages that raced ahead of it as one batch authored by the same peer device for the same
     * call.
     *
     * @param callId  the call identifier the payload belongs to
     * @param payload the {@code <call>} child element being buffered or, for the offer, forwarded
     * @param from    the envelope sender of the stanza that triggered this call, used as the attribution
     *                for the offer and for every drained payload when the offer creates the call
     */
    private void bufferAndMaybeReplay(String callId, Stanza payload, Jid from) {
        if (CALL_CREATING_TAGS.contains(payload.description())) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call {0} created by offer, replaying buffered messages", callId);
            forward(payload, from);
            if (buffer.hasBufferedMessages(callId)) {
                buffer.drainBufferedMessages(callId).forEach(drained -> forward(drained, from));
            }
            return;
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "buffering call payload {0} for call {1} pending creation",
                    payload.description(), callId);
        }
        buffer.buffer(callId, payload);
    }

    /**
     * Decodes a routable payload into a {@link CallMessage} and forwards it, with the envelope
     * {@code from}, to the engine sink.
     *
     * <p>Decoding goes through {@link CallStanza#parse(Stanza)}, the integrator owned parser that
     * maps a {@code <call>} child element to its typed message record. The decoded message is paired with
     * {@code from}, the envelope sender, so the engine receives the authoring device JID alongside the
     * action. A payload the parser cannot decode is dropped without forwarding; the acknowledgement has
     * already been sent, so the server does not retransmit.
     *
     * @param payload the {@code <call>} child element to decode and forward
     * @param from    the envelope sender forwarded alongside the decoded message
     */
    private void forward(Stanza payload, Jid from) {
        CallStanza.parse(payload).ifPresentOrElse(
                message -> {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "forwarding call message type={0}", message.type());
                    sink.accept(message, from);
                },
                () -> {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "could not decode call payload: {0}", payload);
                });
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation additionally clears the {@link CallMessageBuffer} so a new connection starts
     * with no recorded transaction ids, terminate reasons, or buffered messages from the previous
     * connection, then drops the ordered base's in flight per call chains.
     */
    @Override
    public void reset() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "resetting call receiver state");
        buffer.reset();
        super.reset();
    }
}
