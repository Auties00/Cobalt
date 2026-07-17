package com.github.auties00.cobalt.calls.signaling.receive;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stream.SocketStreamHandler;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.function.BiConsumer;
import com.github.auties00.cobalt.calls.signaling.session.TerminateStanza;

/**
 * Receives top level bare {@code <terminate>} stanzas that end a call without a {@code <call>}
 * envelope.
 *
 * <p>The call end signal does not always arrive wrapped in a {@code <call>} stanza. The server emits a
 * bare {@code <terminate call-id call-creator reason .../>} at the top level when another device of the
 * same account answered or declined the call, when the server times out the offer, or when the peer
 * infrastructure forcibly ends the call in a way that does not fit a wrapped {@code <call><terminate>}
 * payload. This handler is registered under the {@code "terminate"} stream tag and routes the bare
 * terminate through the same decode and forward path the {@link CallReceiver} uses for an enveloped
 * terminate, so the engine sees one terminate model regardless of envelope shape.
 *
 * <p>The handler is {@link SocketStreamHandler.Ordered ordered} keyed on the call identifier so a bare
 * terminate is serialised on the same per call chain as any enveloped signal for that call, and the
 * terminate is never reordered relative to an enveloped signal that races it. A bare terminate with no
 * {@code call-id} is dropped because it cannot be associated with a call. The handler forwards the
 * decoded {@link TerminateStanza}, together with the stanza {@code from}, to a sink supplied at
 * construction rather than to a fixed engine reference, keeping the signaling layer decoupled from the
 * lifecycle controller; the integrator wires the sink to the engine. The stanza {@code from} is the
 * authoring device {@link Jid} the engine needs as the companion device discriminator the terminate
 * guards key on, which the bare {@code <terminate>} carries on its top level {@code from} attribute
 * rather than inside the decoded action.
 *
 * @implNote This implementation decodes the top level {@code <terminate>} element directly through
 * {@link TerminateStanza#of(Stanza)} rather than unwrapping a {@code <call>} child, and the reason
 * literal resolves to {@link com.github.auties00.cobalt.wire.linked.call.CallEndReason} inside that record.
 * The ordered base's per call chain provides the terminate serialisation on a virtual thread.
 */
public final class TerminateReceiver extends SocketStreamHandler.Ordered {
    /**
     * The logger for {@link TerminateReceiver}.
     */
    private static final System.Logger LOGGER = Log.get(TerminateReceiver.class);

    /**
     * The stream tag this handler is registered under.
     */
    public static final String STREAM_TAG = "terminate";

    /**
     * The wire attribute naming the call identifier on a bare {@code <terminate>} stanza.
     */
    private static final String CALL_ID_ATTRIBUTE = "call-id";

    /**
     * The wire attribute naming the sender on a bare {@code <terminate>} stanza.
     */
    private static final String FROM_ATTRIBUTE = "from";

    /**
     * The fallback ordering key used when a stanza carries no {@code call-id}, so a malformed terminate
     * is still serialised on one chain rather than spread across the key space.
     */
    private static final String UNKEYED_ORDERING_KEY = "";

    /**
     * Receives the decoded {@link TerminateStanza} together with the stanza {@code from} for engine
     * handling.
     */
    private final BiConsumer<TerminateStanza, Jid> sink;

    /**
     * Constructs a bare terminate receiver bound to its engine sink.
     *
     * @param sink the consumer that receives each decoded {@link TerminateStanza} together with the
     *             stanza {@code from} that authored it
     * @throws NullPointerException if {@code sink} is {@code null}
     */
    public TerminateReceiver(BiConsumer<TerminateStanza, Jid> sink) {
        this.sink = Objects.requireNonNull(sink, "sink cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * Returns the stanza's {@code call-id} so a bare terminate is serialised on the same per call
     * chain as any enveloped signal for that call; a stanza with no {@code call-id} falls back to a
     * single shared key so it is still processed in arrival order.
     *
     * @param stanza the inbound bare {@code <terminate>} stanza
     * @return the ordering key, never {@code null}
     */
    @Override
    protected String orderingKey(Stanza stanza) {
        return stanza.getAttributeAsString(CALL_ID_ATTRIBUTE, UNKEYED_ORDERING_KEY);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Drops a stanza with no {@code call-id}; otherwise decodes the bare {@code <terminate>} into a
     * {@link TerminateStanza} and forwards it, with the stanza {@code from}, to the engine sink. A stanza
     * that carries a {@code call-id} but no {@code call-creator} fails decode and is dropped without
     * forwarding, because both attributes are required to address the ended call. A stanza with no
     * {@code from} forwards a {@code null} sender, which the engine treats as a non companion device.
     *
     * @param stanza the inbound bare {@code <terminate>} stanza
     */
    @Override
    public void handle(Stanza stanza) {
        if (!stanza.hasAttribute(CALL_ID_ATTRIBUTE)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ignoring bare terminate without call-id: {0}", stanza);
            return;
        }
        TerminateStanza terminate;
        try {
            terminate = TerminateStanza.of(stanza);
        } catch (RuntimeException exception) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ignoring malformed bare terminate stanza", exception);
            return;
        }
        var from = stanza.getAttributeAsJid(FROM_ATTRIBUTE, null);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "forwarding bare terminate for call {0}",
                    stanza.getAttributeAsString(CALL_ID_ATTRIBUTE, UNKEYED_ORDERING_KEY));
        }
        sink.accept(terminate, from);
    }
}
