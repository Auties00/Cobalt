package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Input model for the daily time window during which the WhatsApp Business
 * AI agent's auto-reply bot is active.
 *
 * <p>The schedule has three pieces: whether the timed window is enabled,
 * the time zone the window is expressed in, and the daily start and end
 * times within that zone. When the window is disabled the bot replies at
 * all hours; when it is enabled the bot only replies between the start
 * and end times in the configured zone.
 *
 * <p>The wire-level shape stores the start and end as seconds elapsed
 * since the start of the day and the zone as its tz-database identifier,
 * which the WhatsApp Business app then re-interprets. This model exposes
 * the typed Java values directly: build it with a {@link LocalTime}
 * pair and a {@link ZoneId}, and the persisted shape is computed on
 * demand.
 */
@ProtobufMessage(name = "BusinessAiReplyBotSchedule")
public final class BusinessAiReplyBotSchedule {
    /**
     * Whether the timed window is active. When {@code false} the bot
     * replies at all hours regardless of {@link #start()} and
     * {@link #end()}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean enabled;

    /**
     * The {@code tz} database identifier of the zone the window is
     * expressed in (for example {@code "America/Los_Angeles"}).
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String zoneId;

    /**
     * Window start as seconds elapsed since the start of the day in
     * {@link #zoneId}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    final int startSecondOfDay;

    /**
     * Window end as seconds elapsed since the start of the day in
     * {@link #zoneId}.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT32)
    final int endSecondOfDay;

    /**
     * Constructs a new {@code BusinessAiReplyBotSchedule} from the
     * persisted wire-level shape.
     *
     * @param enabled          whether the timed window is active
     * @param zoneId           the tz-database zone identifier; required
     * @param startSecondOfDay the window start, in seconds since midnight
     * @param endSecondOfDay   the window end, in seconds since midnight
     * @throws NullPointerException if {@code zoneId} is {@code null}
     */
    public BusinessAiReplyBotSchedule(boolean enabled, String zoneId, int startSecondOfDay,
                                      int endSecondOfDay) {
        this.enabled = enabled;
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId cannot be null");
        this.startSecondOfDay = startSecondOfDay;
        this.endSecondOfDay = endSecondOfDay;
    }

    /**
     * Convenience constructor that takes typed {@link ZoneId} and
     * {@link LocalTime} values and converts them to the wire-level shape.
     *
     * @param enabled whether the timed window is active
     * @param zone    the zone the window is expressed in; required
     * @param start   the daily start time; required
     * @param end     the daily end time; required
     * @throws NullPointerException if {@code zone}, {@code start}, or
     *                              {@code end} is {@code null}
     */
    public BusinessAiReplyBotSchedule(boolean enabled, ZoneId zone, LocalTime start, LocalTime end) {
        this(enabled,
                Objects.requireNonNull(zone, "zone cannot be null").getId(),
                Objects.requireNonNull(start, "start cannot be null").toSecondOfDay(),
                Objects.requireNonNull(end, "end cannot be null").toSecondOfDay());
    }

    /**
     * Returns whether the timed window is active.
     *
     * @return the enabled flag
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * Returns the zone the window is expressed in.
     *
     * @return the {@link ZoneId}, never {@code null}
     */
    public ZoneId zone() {
        return ZoneId.of(zoneId);
    }

    /**
     * Returns the daily start time.
     *
     * @return the start {@link LocalTime}, never {@code null}
     */
    public LocalTime start() {
        return LocalTime.ofSecondOfDay(startSecondOfDay);
    }

    /**
     * Returns the daily end time.
     *
     * @return the end {@link LocalTime}, never {@code null}
     */
    public LocalTime end() {
        return LocalTime.ofSecondOfDay(endSecondOfDay);
    }

    /**
     * Returns the tz-database zone identifier.
     *
     * @return the zone id string, never {@code null}
     */
    public String zoneIdString() {
        return zoneId;
    }

    /**
     * Returns the window start as seconds since midnight.
     *
     * @return the start second-of-day
     */
    public int startSecondOfDay() {
        return startSecondOfDay;
    }

    /**
     * Returns the window end as seconds since midnight.
     *
     * @return the end second-of-day
     */
    public int endSecondOfDay() {
        return endSecondOfDay;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessAiReplyBotSchedule) obj;
        return enabled == that.enabled
                && startSecondOfDay == that.startSecondOfDay
                && endSecondOfDay == that.endSecondOfDay
                && Objects.equals(zoneId, that.zoneId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, zoneId, startSecondOfDay, endSecondOfDay);
    }

    @Override
    public String toString() {
        return "BusinessAiReplyBotSchedule[" +
                "enabled=" + enabled + ", " +
                "zoneId=" + zoneId + ", " +
                "startSecondOfDay=" + startSecondOfDay + ", " +
                "endSecondOfDay=" + endSecondOfDay + ']';
    }
}
