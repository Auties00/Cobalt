package com.github.auties00.cobalt.wire.stanza.iq.biz;

import java.util.Objects;
import java.util.Optional;

/**
 * One per-day configuration row inside an {@link IqEditBusinessProfileBusinessHours} payload.
 *
 * <p>Each row expresses one open or closed segment for a given day of week. For an open window the open and close
 * times are minutes since midnight; for a closed day the matching {@code mode} string (for example {@code "closed"})
 * is supplied with both endpoints absent.
 *
 * @implNote
 * This implementation models the row as {@code (dayOfWeek, mode, openTime, closeTime)} where the times are minutes
 * since midnight; multiple rows on the same day produce split open windows (for example morning and evening). The
 * {@code mode} string is routed verbatim into the {@code mode} attribute of the {@code <business_hours_config/>} child.
 */
public final class IqEditBusinessProfileBusinessHoursConfig {
    /**
     * The day-of-week identifier routed verbatim into the {@code day_of_week} attribute.
     */
    private final String dayOfWeek;

    /**
     * The open/closed mode identifier (for example {@code "open_specific_hours"}, {@code "closed"}) routed verbatim
     * into the {@code mode} attribute.
     */
    private final String mode;

    /**
     * The open time in minutes since midnight, when supplied.
     */
    private final Integer openTime;

    /**
     * The close time in minutes since midnight, when supplied.
     */
    private final Integer closeTime;

    /**
     * Constructs a typed row from a day and a mode.
     *
     * <p>Pass {@code null} for {@code openTime} and {@code closeTime} when the row models a closed day or any mode
     * where the per-row times do not apply.
     *
     * @param dayOfWeek the day-of-week identifier; never {@code null}
     * @param mode      the mode identifier; never {@code null}
     * @param openTime  the open time in minutes since midnight; may be {@code null}
     * @param closeTime the close time in minutes since midnight; may be {@code null}
     * @throws NullPointerException if {@code dayOfWeek} or {@code mode} is {@code null}
     */
    public IqEditBusinessProfileBusinessHoursConfig(String dayOfWeek, String mode, Integer openTime, Integer closeTime) {
        this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "dayOfWeek cannot be null");
        this.mode = Objects.requireNonNull(mode, "mode cannot be null");
        this.openTime = openTime;
        this.closeTime = closeTime;
    }

    /**
     * Returns the day-of-week identifier that the row targets.
     *
     * @return the day; never {@code null}
     */
    public String dayOfWeek() {
        return dayOfWeek;
    }

    /**
     * Returns the open/closed mode identifier, typically {@code "open_specific_hours"} for an open window or
     * {@code "closed"} for a closed day.
     *
     * @return the mode; never {@code null}
     */
    public String mode() {
        return mode;
    }

    /**
     * Returns the open time in minutes since midnight.
     *
     * <p>The value is absent when the mode does not carry a per-row open time.
     *
     * @return an {@link Optional} carrying the time
     */
    public Optional<Integer> openTime() {
        return Optional.ofNullable(openTime);
    }

    /**
     * Returns the close time in minutes since midnight.
     *
     * <p>The value is absent when the mode does not carry a per-row close time.
     *
     * @return an {@link Optional} carrying the time
     */
    public Optional<Integer> closeTime() {
        return Optional.ofNullable(closeTime);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqEditBusinessProfileBusinessHoursConfig) obj;
        return Objects.equals(this.dayOfWeek, that.dayOfWeek)
                && Objects.equals(this.mode, that.mode)
                && Objects.equals(this.openTime, that.openTime)
                && Objects.equals(this.closeTime, that.closeTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dayOfWeek, mode, openTime, closeTime);
    }

    @Override
    public String toString() {
        return "IqEditBusinessProfileBusinessHoursConfig[dayOfWeek=" + dayOfWeek
                + ", mode=" + mode + ']';
    }
}
