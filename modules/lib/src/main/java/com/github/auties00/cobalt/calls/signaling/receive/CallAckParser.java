package com.github.auties00.cobalt.calls.signaling.receive;

import com.github.auties00.cobalt.ack.AckClass;
import com.github.auties00.cobalt.ack.AckParser;
import com.github.auties00.cobalt.ack.CallAck;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.relay.RelayInfo;

/**
 * Parses the synchronous {@code <ack class="call">} a call offer or accept returns into a
 * {@link CallAckOutcome}.
 *
 * <p>An outbound offer is shipped through {@link LinkedWhatsAppClient#sendNode(StanzaBuilder)}, which
 * blocks the calling virtual thread and returns the matching {@code <ack class="call" type="offer">}
 * as its value, carrying inside the {@code <relay>} block the media plane needs. The outbound accept
 * follows the same contract and is rejected through an {@code error} code the same way.
 * {@link #parse(Stanza)} turns that return stanza into the typed outcome the engine branches on, so
 * neither the offer sender nor the accept sender walks the ack envelope by hand or computes the
 * accept and reject decision on its own.
 *
 * <p>The envelope ({@code id}, {@code type}, {@code from}, {@code error}) is decoded by reusing the
 * shared {@link AckParser}, whose {@link AckClass#CALL} branch produces a {@link CallAck} whose
 * {@code <relay>} child is already parsed into the {@link RelayInfo} model through
 * {@link RelayInfo#of(Stanza)}. That relay is read back off {@link CallAck#relay()} and folded into the
 * outcome the transport layer consumes. A Cobalt {@link Stanza} is already a parsed tree, so this layer
 * is plain Java glue over the shared ack model.
 *
 * @implNote This implementation reads the {@code <relay>} child once, off {@link CallAck#relay()} as
 * produced by the shared {@link AckParser} through {@link RelayInfo#of(Stanza)}, rather than parsing
 * the child a second time.
 */
public final class CallAckParser {
    /**
     * The logger for {@link CallAckParser}.
     */
    private static final System.Logger LOGGER = Log.get(CallAckParser.class);

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private CallAckParser() {
        throw new AssertionError();
    }

    /**
     * Parses the {@code <ack class="call">} returned for an outbound offer or accept into a typed
     * outcome.
     *
     * <p>The input must be the {@code <ack>} element itself, as returned by
     * {@link LinkedWhatsAppClient#sendNode(StanzaBuilder)} for the offer or accept envelope. The envelope
     * attributes are read through the shared {@link AckParser}; when that parser yields a
     * {@link CallAck} its already parsed {@link RelayInfo} relay child is folded into the result. A
     * return stanza that the shared parser does not classify as a call ack (for example a server error
     * envelope under a different {@code class}) yields an empty result so the caller treats it as a
     * setup failure rather than a usable ack.
     *
     * @param ack the {@code <ack>} stanza returned by the server for the offer or accept
     * @return the parsed {@link CallAckOutcome}, or {@link Optional#empty()} when the stanza is not a
     *         call class ack
     * @throws NullPointerException     if {@code ack} is {@code null}
     * @throws IllegalArgumentException if the stanza tag is not {@code "ack"}
     */
    public static Optional<CallAckOutcome> parse(Stanza ack) {
        Objects.requireNonNull(ack, "ack cannot be null");
        if (!(AckParser.parse(ack) instanceof CallAck callAck)) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "ack is not a call class ack");
            return Optional.empty();
        }
        var outcome = CallAckOutcome.of(callAck, callAck.relay().orElse(null));
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "parsed call ack id={0} nack={1} relay={2}",
                    outcome.id(), outcome.isNack(), outcome.relay().isPresent());
        }
        return Optional.of(outcome);
    }
}
