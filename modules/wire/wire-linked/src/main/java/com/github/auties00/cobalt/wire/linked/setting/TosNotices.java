package com.github.auties00.cobalt.wire.linked.setting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Duration;
import java.util.List;

/**
 * Aggregated queue of pending Terms-of-Service updates the user must
 * acknowledge.
 *
 * <p>The server returns the full set of currently-tracked notices for the
 * user along with a recommended re-poll interval that tells the client how
 * long it can cache the response before refreshing. The interval is clamped
 * server-side to a sane range (typically between two hours and three days);
 * when the value would fall outside that range the parser falls back to a
 * one-day default.
 *
 * <p>Each entry in {@link #notices()} is a {@link TosNotice} carrying a
 * notice identifier and the user's per-notice acceptance flag.
 */
@ProtobufMessage
public final class TosNotices {
    /**
     * The recommended re-poll interval expressed in seconds. Stored as raw
     * seconds matching the wire encoding; the {@link #refresh()} accessor
     * wraps the value into a {@link Duration} for caller convenience.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT64)
    long refreshSeconds;

    /**
     * The pending acknowledgement entries, one per outstanding notice.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<TosNotice> notices;

    /**
     * Constructs a new {@code TosNotices} with the supplied refresh
     * interval (in seconds) and per-notice entries.
     *
     * @param refreshSeconds the refresh interval in seconds
     * @param notices        the per-notice entries; {@code null} is
     *                       treated as an empty list
     */
    TosNotices(long refreshSeconds, List<TosNotice> notices) {
        this.refreshSeconds = refreshSeconds;
        this.notices = notices == null ? List.of() : notices;
    }

    /**
     * Returns the recommended re-poll interval.
     *
     * <p>The underlying field stores the interval as raw seconds matching
     * the wire encoding; this accessor wraps the value into a
     * {@link Duration} for caller convenience.
     *
     * @return the refresh interval; never {@code null}
     */
    public Duration refresh() {
        return Duration.ofSeconds(refreshSeconds);
    }

    /**
     * Returns the per-notice acknowledgement entries.
     *
     * @return an unmodifiable list of notices; never {@code null}, possibly
     *         empty
     */
    public List<TosNotice> notices() {
        return notices;
    }

    /**
     * Sets the recommended re-poll interval in seconds.
     *
     * @param refreshSeconds the interval in seconds
     */
    public void setRefreshSeconds(long refreshSeconds) {
        this.refreshSeconds = refreshSeconds;
    }

    /**
     * Sets the recommended re-poll interval.
     *
     * @param refresh the interval; {@code null} is treated as
     *                {@link Duration#ZERO}
     */
    public void setRefresh(Duration refresh) {
        this.refreshSeconds = refresh == null ? 0L : refresh.getSeconds();
    }

    /**
     * Sets the per-notice acknowledgement entries.
     *
     * @param notices the entries to set; {@code null} is treated as an
     *                empty list
     */
    public void setNotices(List<TosNotice> notices) {
        this.notices = notices == null ? List.of() : notices;
    }
}
