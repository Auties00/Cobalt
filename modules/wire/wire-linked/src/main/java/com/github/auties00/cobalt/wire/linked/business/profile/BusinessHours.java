package com.github.auties00.cobalt.wire.linked.business.profile;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * Represents the business hours schedule for a WhatsApp Business account, containing the
 * time zone and the list of daily operating hour entries.
 *
 * <p>The time zone is an IANA time zone identifier (such as {@code "America/New_York"} or
 * {@code "Europe/London"}) used to interpret the opening and closing times in each
 * {@link BusinessHoursEntry} relative to the business's local time. The entries define the
 * operating mode and time ranges for individual days of the week.
 *
 * <p>A day of the week that is absent from the entries list is considered closed. A single
 * day may appear in multiple entries to represent split operating hours, such as a morning
 * and afternoon session separated by a break.
 *
 * @see BusinessProfile#hours()
 */
@ProtobufMessage
public final class BusinessHours {
    /**
     * The IANA time zone identifier for this schedule, such as {@code "America/New_York"} or
     * {@code "Europe/London"}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String timeZone;

    /**
     * The list of business hours configuration entries for individual days of the week.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<BusinessHoursEntry> entries;

    /**
     * Constructs a new business hours schedule with the specified time zone and entries.
     *
     * @param timeZone the IANA time zone identifier
     * @param entries  the list of daily operating hour entries, or {@code null} for an empty list
     */
    BusinessHours(String timeZone, List<BusinessHoursEntry> entries) {
        this.timeZone = timeZone;
        this.entries = entries;
    }

    /**
     * Returns the IANA time zone identifier for this schedule.
     *
     * <p>This value is used to determine the business's local time when evaluating whether
     * the business is currently open or closed based on the configured entries.
     *
     * @return the time zone identifier
     */
    public String timeZone() {
        return timeZone;
    }

    /**
     * Sets the IANA time zone identifier for this schedule.
     *
     * @param timeZone the time zone identifier
     */
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * Returns an unmodifiable view of the business hours configuration entries.
     *
     * <p>Each entry represents the operating schedule for a single day of the week. A day
     * that does not appear in the returned list is considered closed.
     *
     * @return a non-{@code null}, unmodifiable list of entries
     */
    public List<BusinessHoursEntry> entries() {
        return entries == null ? List.of() : Collections.unmodifiableList(entries);
    }

    /**
     * Sets the list of business hours configuration entries.
     *
     * @param entries the list of daily operating hour entries, or {@code null} for an empty list
     */
    public void setEntries(List<BusinessHoursEntry> entries) {
        this.entries = entries;
    }
}
