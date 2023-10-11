package it.auties.whatsapp.model.business;

import it.auties.whatsapp.model.node.Node;

/**
 * A business hours entry that represents the hours of operation for a single day of the week.
 *
 * @param day       The day of the week that this entry represents.
 * @param mode      The mode of operation for this day.
 * @param openTime  The time in seconds since midnight that the business opens.
 * @param closeTime The time in seconds since midnight that the business closes.
 */
public record BusinessHoursEntry(String day, String mode, long openTime, long closeTime) {
    /**
     * Creates a {@link BusinessHoursEntry} from a {@link Node}.
     *
     * @param node The node to extract the business hours entry information from.
     * @return A {@link BusinessHoursEntry} extracted from the provided node.
     */
    public static BusinessHoursEntry of(Node node) {
        return new BusinessHoursEntry(
                node.attributes().getString("day_of_week"),
                node.attributes().getString("mode"),
                node.attributes().getLong("open_time"),
                node.attributes().getLong("close_time")
        );
    }

    /**
     * Returns whether the business is always open.
     *
     * @return whether the business is always open
     */
    public boolean isAlwaysOpen() {
        return openTime == 0 && closeTime == 0;
    }
}