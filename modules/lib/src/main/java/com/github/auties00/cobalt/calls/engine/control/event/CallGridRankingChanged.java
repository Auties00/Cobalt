package com.github.auties00.cobalt.calls.engine.control.event;

import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.List;
import java.util.Objects;

/**
 * A {@link ControlCallEvent} reporting a new participant ordering for the call grid.
 *
 * <p>The engine ranks group call participants for display by a comparator that orders hand raised
 * participants first, then active speakers, then by rank, then by stable index. When that ordering
 * changes, the engine emits this event carrying the new {@link #ranking() ranking}: the participant
 * device {@link Jid}s in display order, the top ranked participant first.
 *
 * @param ranking the participant device {@link Jid}s in display order, the highest ranked first; never
 *                {@code null}, defensively copied and unmodifiable
 */
public record CallGridRankingChanged(List<Jid> ranking) implements ControlCallEvent {
    /**
     * Validates the record components and defensively copies the ranking list into an unmodifiable
     * {@link List}.
     *
     * @throws NullPointerException if {@code ranking} is {@code null} or contains a {@code null} element
     */
    public CallGridRankingChanged {
        Objects.requireNonNull(ranking, "ranking cannot be null");
        ranking = List.copyOf(ranking);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link CallEventType#CALL_GRID_RANKING_CHANGED}
     */
    @Override
    public CallEventType type() {
        return CallEventType.CALL_GRID_RANKING_CHANGED;
    }
}
