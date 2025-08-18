package it.auties.whatsapp.model.business;


import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;

/**
 * A business hours representation that contains the business' time zone and a list of business hour
 * entries.
 */
@ProtobufMessage
public final class BusinessHours {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String timeZone;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final List<BusinessHoursEntry> entries;

    BusinessHours(String timeZone, List<BusinessHoursEntry> entries) {
        this.timeZone = Objects.requireNonNull(timeZone, "timeZone cannot be null");
        this.entries = Objects.requireNonNullElse(entries, List.of());
    }

    public String timeZone() {
        return timeZone;
    }

    public List<BusinessHoursEntry> entries() {
        return entries;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BusinessHours that
                && Objects.equals(timeZone, that.timeZone)
                && Objects.equals(entries, that.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeZone, entries);
    }

    @Override
    public String toString() {
        return "BusinessHours[" +
                "timeZone=" + timeZone + ", " +
                "entries=" + entries + ']';
    }
}