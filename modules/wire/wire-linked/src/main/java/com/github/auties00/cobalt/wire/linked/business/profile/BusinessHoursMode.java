package com.github.auties00.cobalt.wire.linked.business.profile;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

/**
 * Represents the operating mode for a {@link BusinessHoursEntry}, indicating how the
 * business's availability is determined for a given day.
 *
 * <p>Three modes are currently defined by the WhatsApp protocol:
 * <ul>
 *   <li>{@link SpecificHours} indicates that the business operates during specific time
 *       ranges defined by the entry's opening and closing times.
 *   <li>{@link Open24H} indicates that the business is open for the entire day.
 *   <li>{@link AppointmentOnly} indicates that the business is available only by
 *       appointment.
 * </ul>
 *
 * <p>An {@link Unknown} variant is provided for forward compatibility with values that
 * may be introduced by the server in the future.
 *
 * <p>The wire format is a plain {@code String} (e.g. {@code "specific_hours"}).
 *
 * @see BusinessHoursEntry#mode()
 */
public sealed interface BusinessHoursMode {
    /**
     * The singleton instance for all-day availability.
     */
    BusinessHoursMode OPEN_24H = new Open24H();

    /**
     * The singleton instance for specific operating hours.
     */
    BusinessHoursMode SPECIFIC_HOURS = new SpecificHours();

    /**
     * The singleton instance for appointment-only availability.
     */
    BusinessHoursMode APPOINTMENT_ONLY = new AppointmentOnly();

    /**
     * Returns the {@code BusinessHoursMode} corresponding to the given wire value.
     *
     * <p>Recognized values are {@code "open_24h"}, {@code "specific_hours"}, and
     * {@code "appointment_only"} (case-sensitive). Any other non-{@code null} value
     * yields an {@link Unknown} instance that preserves the original string.
     *
     * @param value the wire-format string, or {@code null}
     * @return the corresponding mode, or {@code null} if {@code value} is {@code null}
     */
    @ProtobufDeserializer
    static BusinessHoursMode of(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "open_24h" -> OPEN_24H;
            case "specific_hours" -> SPECIFIC_HOURS;
            case "appointment_only" -> APPOINTMENT_ONLY;
            default -> new Unknown(value);
        };
    }

    /**
     * Returns the wire-format string representation of this mode.
     *
     * @return the mode value as sent over the wire (e.g. {@code "specific_hours"})
     */
    @ProtobufSerializer
    String value();

    /**
     * The business is open for the entire day.
     */
    record Open24H() implements BusinessHoursMode {
        /**
         * {@inheritDoc}
         *
         * @return {@code "open_24h"}
         */
        @Override
        public String value() {
            return "open_24h";
        }
    }

    /**
     * The business operates during specific time ranges defined by the opening and
     * closing times of the {@link BusinessHoursEntry}.
     */
    record SpecificHours() implements BusinessHoursMode {
        /**
         * {@inheritDoc}
         *
         * @return {@code "specific_hours"}
         */
        @Override
        public String value() {
            return "specific_hours";
        }
    }

    /**
     * The business is available only by appointment.
     */
    record AppointmentOnly() implements BusinessHoursMode {
        /**
         * {@inheritDoc}
         *
         * @return {@code "appointment_only"}
         */
        @Override
        public String value() {
            return "appointment_only";
        }
    }

    /**
     * An unrecognized mode value, provided for forward compatibility.
     *
     * @param value the raw wire-format string
     */
    record Unknown(String value) implements BusinessHoursMode {
    }
}
