package com.github.auties00.cobalt.wire.stanza.iq.biz;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The typed business-hours payload carried as the {@code <business_hours/>} child of an {@link IqEditBusinessProfileRequest}.
 *
 * <p>The payload expresses the merchant's open-hours schedule in a single edit: the optional timezone (IANA identifier)
 * frames the per-day times, the optional note renders above the schedule grid, and the per-day rows enumerate the open
 * and closed segments. An empty row list clears the schedule without removing the {@code <business_hours/>} envelope.
 *
 * @implNote
 * This implementation aggregates the day-of-week schedule as a flat list of {@link IqEditBusinessProfileBusinessHoursConfig}
 * entries rather than as a per-day map; multiple rows can target the same day to express split open windows (for example
 * morning and evening).
 */
public final class IqEditBusinessProfileBusinessHours {
    /**
     * The IANA timezone identifier framing the per-day times, when supplied.
     */
    private final String timezone;

    /**
     * The optional note text rendered above the schedule grid.
     */
    private final String note;

    /**
     * The per-day schedule rows.
     */
    private final List<IqEditBusinessProfileBusinessHoursConfig> config;

    /**
     * Constructs a typed payload from a schedule.
     *
     * <p>Pass {@code null} for {@code timezone} when the merchant has not set one and {@code null} for {@code note}
     * when there is nothing to render above the grid.
     *
     * @param timezone the IANA timezone identifier; may be {@code null}
     * @param note     the note text; may be {@code null}
     * @param config   the per-day schedule rows; never {@code null}
     * @throws NullPointerException if {@code config} is {@code null}
     */
    public IqEditBusinessProfileBusinessHours(String timezone, String note, List<IqEditBusinessProfileBusinessHoursConfig> config) {
        this.timezone = timezone;
        this.note = note;
        Objects.requireNonNull(config, "config cannot be null");
        this.config = List.copyOf(config);
    }

    /**
     * Returns the IANA timezone identifier framing the schedule rows.
     *
     * <p>The value is absent when the merchant has not configured one.
     *
     * @return an {@link Optional} carrying the timezone
     */
    public Optional<String> timezone() {
        return Optional.ofNullable(timezone);
    }

    /**
     * Returns the free-text note rendered above the schedule grid.
     *
     * <p>The value is absent when the merchant did not stamp one.
     *
     * @return an {@link Optional} carrying the note
     */
    public Optional<String> note() {
        return Optional.ofNullable(note);
    }

    /**
     * Returns the per-day schedule rows.
     *
     * <p>The list is empty when the merchant has not configured any open windows.
     *
     * @return an unmodifiable list; never {@code null}
     */
    public List<IqEditBusinessProfileBusinessHoursConfig> config() {
        return config;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqEditBusinessProfileBusinessHours) obj;
        return Objects.equals(this.timezone, that.timezone)
                && Objects.equals(this.note, that.note)
                && Objects.equals(this.config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timezone, note, config);
    }

    @Override
    public String toString() {
        return "IqEditBusinessProfileBusinessHours[timezone=" + timezone
                + ", note=" + note + ", config=" + config + ']';
    }
}
