package com.github.auties00.cobalt.calls.engine.control;

import com.github.auties00.cobalt.calls.signaling.incall.RaiseHandStanza;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.lang.System.Logger.Level;
import java.util.Objects;
import com.github.auties00.cobalt.calls.engine.control.event.RaiseHandStateChanged;

/**
 * Drives the in call raise hand control, raising or lowering the local user's hand and surfacing peers'.
 *
 * <p>In a group call a participant can raise a hand to ask to speak. {@link #setHandRaised(boolean)} records
 * the new state, sends a {@code raise_hand} action carrying it, emits a {@link RaiseHandStateChanged} for the
 * local user, and feeds the local hand state into the {@link SpeakerRankingService} so a raised hand sorts
 * first in the grid. Inbound peer reports are delivered through {@link #onPeerHandRaised(Jid, boolean)}, which
 * emits a {@link RaiseHandStateChanged} for the reporting peer and feeds its hand state into the ranking too.
 *
 * <p>Unlike reactions, the raise hand signal rides the signaling plane rather than the application data
 * channel, so it dispatches through the {@link CallSignalingSender} as a {@link RaiseHandStanza}. The
 * controller is bound at construction to one call's identity, its signaling and event seams, and the
 * {@link SpeakerRankingService} it updates; it owns no timers and holds only the local hand state in a
 * volatile field.
 *
 * <p>The wire action is the {@code <raise_hand>} element carrying a {@code raise-hand-state} attribute of
 * {@code 1} when raised or {@code 0} when lowered.
 */
public final class RaiseHandController {
    /**
     * The logger for {@link RaiseHandController}.
     */
    private static final System.Logger LOGGER = Log.get(RaiseHandController.class);

    /**
     * The call identity this controller stamps onto its raise hand actions.
     */
    private final CallControlContext context;

    /**
     * The signaling egress raise hand actions are sent through.
     */
    private final CallSignalingSender sender;

    /**
     * The event sink raise hand events are emitted into.
     */
    private final CallEventSink events;

    /**
     * The speaker ranking service the hand state is fed into.
     */
    private final SpeakerRankingService ranking;

    /**
     * Whether the local user currently has a hand raised.
     *
     * <p>Declared {@code volatile} so {@link #setHandRaised(boolean)} can store it and {@link #isHandRaised()}
     * can read it without a lock; the field is a lone flag with no compound update.
     */
    private volatile boolean handRaised;

    /**
     * Constructs a raise hand controller bound to a call, its seams, and the ranking service.
     *
     * @param context the call identity to stamp onto raise hand actions; never {@code null}
     * @param sender  the signaling egress to send raise hand actions through; never {@code null}
     * @param events  the event sink to emit raise hand events into; never {@code null}
     * @param ranking the speaker ranking service to feed hand state into; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public RaiseHandController(CallControlContext context, CallSignalingSender sender, CallEventSink events,
                               SpeakerRankingService ranking) {
        this.context = Objects.requireNonNull(context, "context cannot be null");
        this.sender = Objects.requireNonNull(sender, "sender cannot be null");
        this.events = Objects.requireNonNull(events, "events cannot be null");
        this.ranking = Objects.requireNonNull(ranking, "ranking cannot be null");
    }

    /**
     * Raises or lowers the local user's hand, broadcasting the change.
     *
     * <p>Records the new hand state, sends a {@code raise_hand} action carrying it, emits a
     * {@link RaiseHandStateChanged} for the local user, and feeds the state into the
     * {@link SpeakerRankingService}.
     *
     * @param raised {@code true} to raise the hand, {@code false} to lower it
     */
    public void setHandRaised(boolean raised) {
        this.handRaised = raised;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "self hand raised state changed to {0}", raised);
        sender.send(new RaiseHandStanza(context.callId(), context.callCreator(), raised, context.group()));
        events.emit(new RaiseHandStateChanged(context.selfJid(), raised, true));
        ranking.setHandRaised(context.selfJid(), raised);
    }

    /**
     * Returns whether the local user currently has a hand raised.
     *
     * @return {@code true} when the local user's hand is raised
     */
    public boolean isHandRaised() {
        return handRaised;
    }

    /**
     * Handles an inbound peer raise hand report, emitting the peer's change and updating the ranking.
     *
     * <p>Emits a {@link RaiseHandStateChanged} for the reporting peer and feeds the peer's hand state into
     * the {@link SpeakerRankingService}; it does not change the local hand state.
     *
     * @param peer   the device JID of the reporting peer; never {@code null}
     * @param raised {@code true} when the peer raised a hand, {@code false} when lowered
     * @throws NullPointerException if {@code peer} is {@code null}
     */
    public void onPeerHandRaised(Jid peer, boolean raised) {
        Objects.requireNonNull(peer, "peer cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "peer {0} hand raised state changed to {1}", peer, raised);
        events.emit(new RaiseHandStateChanged(peer, raised, false));
        ranking.setHandRaised(peer, raised);
    }
}
