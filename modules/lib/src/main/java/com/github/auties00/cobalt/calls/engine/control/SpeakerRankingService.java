package com.github.auties00.cobalt.calls.engine.control;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.lang.System.Logger.Level;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;
import com.github.auties00.cobalt.calls.engine.control.event.CallGridRankingChanged;

/**
 * Maintains the display ordering of group call participants and emits a ranking event when it changes.
 *
 * <p>This service ranks participants for the call grid by a fixed comparator: participants with a raised
 * hand first, then active speakers, then by descending engine rank, then by ascending stable index as a
 * tie break. It holds one {@link RankingInputs} per participant, updated as the engine observes hand
 * raises, dominant speaker changes, and rank or index assignments, and recomputes the order on each
 * update. When the resulting order differs from the last emitted order it emits a
 * {@link CallGridRankingChanged} carrying the new ordering, the highest ranked participant first.
 *
 * <p>The inputs of every participant and the last emitted order are kept behind a lock so a concurrent
 * update never emits a torn ranking. The service is bound to its event sink at construction; it owns no
 * timers.
 *
 * @implNote This implementation serializes the recompute and the comparison against the last emitted order
 * behind a single {@link ReentrantLock}, so a concurrent update never observes or emits a partially updated
 * ordering.
 */
public final class SpeakerRankingService {
    /**
     * The logger for {@link SpeakerRankingService}.
     */
    private static final System.Logger LOGGER = Log.get(SpeakerRankingService.class);

    /**
     * Orders participants by the grid ranking rule: raised hand first, then active speakers, then higher
     * rank, then lower index.
     *
     * <p>A {@code true} hand raised flag and a {@code true} active speaker flag sort before their
     * {@code false} counterparts, a larger rank sorts before a smaller one, and a smaller index sorts
     * before a larger one as the final stable tie break. These four keys are the whole ordering; there is
     * no screen share or video dimension.
     */
    private static final Comparator<RankingInputs> GRID_ORDER = Comparator
            .comparingInt((RankingInputs input) -> input.handRaised() ? 0 : 1)
            .thenComparingInt(input -> input.activeSpeaker() ? 0 : 1)
            .thenComparing(Comparator.comparingInt(RankingInputs::rank).reversed())
            .thenComparingInt(RankingInputs::index);

    /**
     * The event sink the ranking changed event is emitted into.
     */
    private final CallEventSink events;

    /**
     * The current ranking inputs of each participant, keyed by participant device JID.
     */
    private final Map<Jid, RankingInputs> inputs = new HashMap<>();

    /**
     * Guards the recompute and the comparison against the last emitted order.
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * The participant order last emitted, used to suppress an unchanged emission.
     */
    private List<Jid> lastOrder = List.of();

    /**
     * Constructs a speaker ranking service bound to its event sink.
     *
     * @param events the event sink to emit the ranking changed event into; never {@code null}
     * @throws NullPointerException if {@code events} is {@code null}
     */
    public SpeakerRankingService(CallEventSink events) {
        this.events = Objects.requireNonNull(events, "events cannot be null");
    }

    /**
     * Sets the hand raised input of a participant and recomputes the ranking.
     *
     * @param participant the device JID whose hand state changed; never {@code null}
     * @param raised      {@code true} when the hand is raised
     * @throws NullPointerException if {@code participant} is {@code null}
     */
    public void setHandRaised(Jid participant, boolean raised) {
        update(participant, current -> current.withHandRaised(raised));
    }

    /**
     * Sets the active speaker input of a participant and recomputes the ranking.
     *
     * @param participant the device JID whose dominant speaker status changed; never {@code null}
     * @param speaking    {@code true} when the participant is the dominant speaker
     * @throws NullPointerException if {@code participant} is {@code null}
     */
    public void setActiveSpeaker(Jid participant, boolean speaking) {
        update(participant, current -> current.withActiveSpeaker(speaking));
    }

    /**
     * Sets the rank and stable index inputs of a participant and recomputes the ranking.
     *
     * @param participant the device JID whose rank or index changed; never {@code null}
     * @param rank        the engine rank, higher sorting first
     * @param index       the stable index, lower sorting first as the final tie break
     * @throws NullPointerException if {@code participant} is {@code null}
     */
    public void setRank(Jid participant, int rank, int index) {
        update(participant, current -> current.withRank(rank, index));
    }

    /**
     * Removes a participant from the ranking and recomputes it.
     *
     * <p>Used when a participant leaves the call; a participant that was not ranked leaves the ranking
     * unchanged.
     *
     * @param participant the device JID to remove; never {@code null}
     * @throws NullPointerException if {@code participant} is {@code null}
     */
    public void remove(Jid participant) {
        Objects.requireNonNull(participant, "participant cannot be null");
        lock.lock();
        try {
            if (inputs.remove(participant) != null) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "removed participant {0} from grid ranking", participant);
                recomputeAndEmit();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the current ranking, the highest ranked participant first.
     *
     * @return the current participant order; never {@code null}, unmodifiable
     */
    public List<Jid> ranking() {
        lock.lock();
        try {
            return lastOrder;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Applies a mutation to the inputs of a participant under the lock and recomputes the ranking.
     *
     * <p>The mutation is computed from the current inputs of the participant, or from a default valued entry
     * when the participant is new, then stored; the ranking is recomputed and a change emitted.
     *
     * @param participant the device JID whose inputs change
     * @param mutation    the mutation from the current inputs to the updated inputs
     */
    private void update(Jid participant, UnaryOperator<RankingInputs> mutation) {
        Objects.requireNonNull(participant, "participant cannot be null");
        lock.lock();
        try {
            var current = inputs.get(participant);
            if (current == null) {
                current = RankingInputs.initial(participant);
            }
            inputs.put(participant, mutation.apply(current));
            recomputeAndEmit();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Recomputes the participant order and emits a ranking changed event when it differs.
     *
     * <p>Sorts the current inputs by the grid order, compares the resulting JID order against the last
     * emitted order, and emits a {@link CallGridRankingChanged} and records the new order only when they
     * differ. Called under the lock. The recompute runs unconditionally, with no gate, matching the engine
     * path this service models.
     */
    private void recomputeAndEmit() {
        var order = inputs.values().stream()
                .sorted(GRID_ORDER)
                .map(RankingInputs::participant)
                .toList();
        if (!order.equals(lastOrder)) {
            lastOrder = order;
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "grid ranking changed, {0} participants", order.size());
            events.emit(new CallGridRankingChanged(order));
        }
    }

    /**
     * The comparator inputs held for one participant: identity, hand raised, active speaker, rank, and
     * stable index.
     *
     * @param participant   the participant device JID
     * @param handRaised    whether the participant has a hand raised
     * @param activeSpeaker whether the participant is the dominant speaker
     * @param rank          the engine rank, higher sorting first
     * @param index         the stable index, lower sorting first as the final tie break
     */
    public record RankingInputs(Jid participant, boolean handRaised, boolean activeSpeaker, int rank, int index) {
        /**
         * Validates the record components.
         *
         * @throws NullPointerException if {@code participant} is {@code null}
         */
        public RankingInputs {
            Objects.requireNonNull(participant, "participant cannot be null");
        }

        /**
         * Returns the default valued inputs for a newly ranked participant.
         *
         * <p>A new participant starts with no hand raised, not speaking, rank {@code 0}, and index
         * {@link Integer#MAX_VALUE} so it sorts last until a real index is assigned.
         *
         * @param participant the participant device JID
         * @return the initial inputs for the participant
         */
        static RankingInputs initial(Jid participant) {
            return new RankingInputs(participant, false, false, 0, Integer.MAX_VALUE);
        }

        /**
         * Returns a copy with the hand raised flag set to the given value.
         *
         * @param raised the new hand raised flag
         * @return the updated inputs
         */
        RankingInputs withHandRaised(boolean raised) {
            return new RankingInputs(participant, raised, activeSpeaker, rank, index);
        }

        /**
         * Returns a copy with the active speaker flag set to the given value.
         *
         * @param speaking the new active speaker flag
         * @return the updated inputs
         */
        RankingInputs withActiveSpeaker(boolean speaking) {
            return new RankingInputs(participant, handRaised, speaking, rank, index);
        }

        /**
         * Returns a copy with the rank and stable index set to the given values.
         *
         * @param rank  the new engine rank
         * @param index the new stable index
         * @return the updated inputs
         */
        RankingInputs withRank(int rank, int index) {
            return new RankingInputs(participant, handRaised, activeSpeaker, rank, index);
        }
    }
}
