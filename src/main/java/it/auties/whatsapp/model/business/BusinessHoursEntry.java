package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.node.Node;

import java.util.Objects;

/**
 * A business hours entry that represents the hours of operation for a single day of the week.
 */
@ProtobufMessage
public final class BusinessHoursEntry {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String day;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String mode;

    @ProtobufProperty(index = 3, type = ProtobufType.INT64)
    final long openTime;

    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    final long closeTime;

    BusinessHoursEntry(String day, String mode, long openTime, long closeTime) {
        this.day = day;
        this.mode = mode;
        this.openTime = openTime;
        this.closeTime = closeTime;
    }

    public static BusinessHoursEntry of(Node node) {
        return new BusinessHoursEntry(
                node.attributes().getString("day_of_week"),
                node.attributes().getString("mode"),
                node.attributes().getLong("open_time"),
                node.attributes().getLong("close_time")
        );
    }

    public String day() {
        return day;
    }

    public String mode() {
        return mode;
    }

    public long openTime() {
        return openTime;
    }

    public long closeTime() {
        return closeTime;
    }

    public boolean alwaysOpen() {
        return openTime == 0 && closeTime == 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BusinessHoursEntry that
                && openTime == that.openTime
                && closeTime == that.closeTime
                && Objects.equals(day, that.day)
                && Objects.equals(mode, that.mode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(day, mode, openTime, closeTime);
    }

    @Override
    public String toString() {
        return "BusinessHoursEntry[" +
                "day=" + day + ", " +
                "mode=" + mode + ", " +
                "openTime=" + openTime + ", " +
                "closeTime=" + closeTime + ']';
    }
}