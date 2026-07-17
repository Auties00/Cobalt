package com.github.auties00.cobalt.calls.engine.control;

import com.github.auties00.cobalt.calls.transport.datachannel.AppDataController;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.call.datachannel.ReactionInfo;
import com.github.auties00.cobalt.wire.linked.call.datachannel.ReactionInfoBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.engine.control.event.ReactionStateChanged;

/**
 * Drives the in call reaction control by sending emoji reactions and surfacing inbound ones as events.
 *
 * <p>Reactions are transient UI overlays that travel over the call's application data side channel rather
 * than over signaling. {@link #sendReaction(String)} broadcasts the local user's reaction through the
 * attached {@link AppDataController#sendReaction(String)}, which retains it for retransmission and clears it
 * after a short window, and emits a {@link ReactionStateChanged} for the local user so its own overlay
 * shows. Inbound reactions, and their expiry, arrive on the {@link #onReaction(Jid, Optional)} callback this
 * controller registers with the {@link AppDataController}: an arrival emits a {@link ReactionStateChanged}
 * carrying the reaction and an expiry emits one carrying an empty reaction so the host takes the overlay
 * down. A reaction never serializes a {@code CallInteraction}; the wire layer does not carry that facade.
 *
 * <p>This controller is the inbound reaction observer the {@link AppDataController} is constructed with, so
 * it must exist before that controller; the outbound send seam is therefore attached after the application
 * data controller is built, through {@link #attach(AppDataController)}, to break the construction cycle. The
 * controller owns no timers of its own (the reaction lifetime and retransmission timers belong to the
 * {@link AppDataController}) and holds no mutable state beyond the single send seam binding.
 */
public final class ReactionController {
    /**
     * The logger for {@link ReactionController}.
     */
    private static final System.Logger LOGGER = Log.get(ReactionController.class);

    /**
     * The local device JID stamped as the participant on the local user's own reaction events.
     */
    private final Jid selfJid;

    /**
     * The event sink reaction events are emitted into.
     */
    private final CallEventSink events;

    /**
     * The application data controller carrying the outbound reaction send, attached after it is built.
     *
     * <p>Declared {@code volatile} because {@link #attach(AppDataController)} runs on the wiring thread while
     * {@link #sendReaction(String)} may run on a control thread; the field is written once.
     */
    private volatile AppDataController appData;

    /**
     * Constructs a reaction controller bound to the local identity and the event sink.
     *
     * <p>The outbound send seam is attached separately through {@link #attach(AppDataController)} once the
     * application data controller this controller observes has been built.
     *
     * @param selfJid the local device JID for the local user's own reaction events; never {@code null}
     * @param events  the event sink to emit reaction events into; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public ReactionController(Jid selfJid, CallEventSink events) {
        this.selfJid = Objects.requireNonNull(selfJid, "selfJid cannot be null");
        this.events = Objects.requireNonNull(events, "events cannot be null");
    }

    /**
     * Attaches the outbound reaction send seam.
     *
     * <p>Called once, after the application data controller this controller observes is built, to wire the
     * outbound path (the application data controller's reaction send). Breaking the binding into a separate
     * step resolves the construction cycle between this observer and the application data controller it is
     * passed to.
     *
     * @param appData the application data controller carrying the reaction send; never {@code null}
     * @throws NullPointerException if {@code appData} is {@code null}
     */
    public void attach(AppDataController appData) {
        this.appData = Objects.requireNonNull(appData, "appData cannot be null");
    }

    /**
     * Broadcasts the local user's emoji reaction and emits its local state change.
     *
     * <p>Sends the reaction through the attached {@link AppDataController#sendReaction(String)}, which assigns
     * it a transaction id and retains it for retransmission. Only when the send is accepted (a transaction id
     * is assigned) does it emit a {@link ReactionStateChanged} for the local user carrying the sent reaction
     * so its own overlay appears; a dropped send emits nothing. Returns the assigned transaction id, or
     * {@code -1} when no send seam is attached or the send was dropped.
     *
     * @param emoji the reaction emoji string to broadcast; never {@code null}
     * @return the transaction id assigned to the reaction, or {@code -1} when unsent
     * @throws NullPointerException if {@code emoji} is {@code null}
     */
    public long sendReaction(String emoji) {
        Objects.requireNonNull(emoji, "emoji cannot be null");
        var current = appData;
        if (current == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "reaction send dropped, no app data channel attached");
            return -1;
        }
        var transactionId = current.sendReaction(emoji);
        // WA emits the local reaction state event only for an accepted send: a precondition or teardown abort
        // (the analog of a closed controller here) surfaces no overlay, while a transport write failure is
        // retained and retransmitted rather than dropped. AppDataController.sendReaction returns -1 only when
        // closed, so suppress the optimistic emit on that dropped send.
        if (transactionId == -1) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "reaction send dropped by app data channel");
            return -1;
        }
        var reaction = new ReactionInfoBuilder()
                .transactionId(transactionId)
                .reaction(emoji)
                .build();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "reaction sent, transactionId={0}", transactionId);
        events.emit(new ReactionStateChanged(selfJid, Optional.of(reaction), true));
        return transactionId;
    }

    /**
     * Surfaces an inbound reaction arrival or expiry from the application data layer as an event.
     *
     * <p>Invoked by the {@link AppDataController} with a present reaction when one arrives from a participant
     * and an empty reaction when that participant's reaction is expired by the sweep. Emits a
     * {@link ReactionStateChanged} carrying the participant and the reaction; the {@code self} flag is set
     * only when the participant is the local device.
     *
     * @param participant the device JID whose reaction changed; never {@code null}
     * @param reaction    the arrived reaction, or empty when the reaction expired
     */
    public void onReaction(Jid participant, Optional<ReactionInfo> reaction) {
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "reaction update from {0}, present={1}", participant, reaction.isPresent());
        }
        events.emit(new ReactionStateChanged(participant, reaction, participant.equals(selfJid)));
    }
}
