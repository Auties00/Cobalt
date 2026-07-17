package com.github.auties00.cobalt.wire.linked.message.system.history;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Selects the slice of history requested by a full on-demand history sync.
 *
 * <p>The window is expressed in one of two interchangeable ways: an absolute
 * lower bound through {@link #historyFromTimestamp()}, or a relative span ending
 * at the present through {@link #historyDurationDays()}. A request that sets
 * neither leaves the bound to the primary device's own per-device policy.
 */
@ProtobufMessage(name = "Message.FullHistorySyncOnDemandConfig")
public final class FullHistorySyncOnDemandConfig {
    /**
     * The absolute lower bound of the requested window; messages at or after
     * this instant are eligible for delivery.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant historyFromTimestamp;

    /**
     * The length of the requested window in days, counting back from the
     * present; an alternative to {@link #historyFromTimestamp}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    Integer historyDurationDays;


    /**
     * Constructs a new on-demand window configuration. This constructor is
     * package private and is reserved for the protobuf deserialiser and the
     * generated builder.
     *
     * @param historyFromTimestamp the absolute lower bound, or {@code null}
     * @param historyDurationDays  the relative span in days, or {@code null}
     */
    FullHistorySyncOnDemandConfig(Instant historyFromTimestamp, Integer historyDurationDays) {
        this.historyFromTimestamp = historyFromTimestamp;
        this.historyDurationDays = historyDurationDays;
    }

    /**
     * Returns the absolute lower bound of the requested window.
     *
     * @return an {@link Optional} containing the lower bound, or
     *         {@link Optional#empty()} when the window is unbounded below or
     *         expressed as a duration
     */
    public Optional<Instant> historyFromTimestamp() {
        return Optional.ofNullable(historyFromTimestamp);
    }

    /**
     * Returns the length of the requested window in days.
     *
     * @return an {@link OptionalInt} containing the day span, or
     *         {@link OptionalInt#empty()} when the window is unbounded or
     *         expressed as an absolute timestamp
     */
    public OptionalInt historyDurationDays() {
        return historyDurationDays == null ? OptionalInt.empty() : OptionalInt.of(historyDurationDays);
    }

    /**
     * Sets the absolute lower bound of the requested window.
     *
     * @param historyFromTimestamp the new lower bound, or {@code null}
     */
    public void setHistoryFromTimestamp(Instant historyFromTimestamp) {
        this.historyFromTimestamp = historyFromTimestamp;
    }

    /**
     * Sets the length of the requested window in days.
     *
     * @param historyDurationDays the new day span, or {@code null}
     */
    public void setHistoryDurationDays(Integer historyDurationDays) {
        this.historyDurationDays = historyDurationDays;
    }
}
