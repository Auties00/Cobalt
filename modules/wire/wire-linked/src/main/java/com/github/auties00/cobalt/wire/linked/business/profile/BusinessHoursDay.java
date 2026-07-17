package com.github.auties00.cobalt.wire.linked.business.profile;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

/**
 * Represents the day of the week for a {@link BusinessHoursEntry}, indicating which day
 * the entry's operating schedule applies to.
 *
 * <p>All seven days of the week are defined using three-letter abbreviations that match
 * the wire format used by the WhatsApp protocol: {@code "sun"}, {@code "mon"},
 * {@code "tue"}, {@code "wed"}, {@code "thu"}, {@code "fri"}, and {@code "sat"}.
 *
 * <p>An {@link Unknown} variant is provided for forward compatibility with values that
 * may be introduced by the server in the future.
 *
 * <p>The wire format is a plain {@code String} (e.g. {@code "mon"}).
 *
 * @see BusinessHoursEntry#day()
 */
public sealed interface BusinessHoursDay {
    /**
     * The singleton instance for Sunday.
     */
    BusinessHoursDay SUNDAY = new Sunday();

    /**
     * The singleton instance for Monday.
     */
    BusinessHoursDay MONDAY = new Monday();

    /**
     * The singleton instance for Tuesday.
     */
    BusinessHoursDay TUESDAY = new Tuesday();

    /**
     * The singleton instance for Wednesday.
     */
    BusinessHoursDay WEDNESDAY = new Wednesday();

    /**
     * The singleton instance for Thursday.
     */
    BusinessHoursDay THURSDAY = new Thursday();

    /**
     * The singleton instance for Friday.
     */
    BusinessHoursDay FRIDAY = new Friday();

    /**
     * The singleton instance for Saturday.
     */
    BusinessHoursDay SATURDAY = new Saturday();

    /**
     * Returns the {@code BusinessHoursDay} corresponding to the given wire value.
     *
     * <p>Recognized values are {@code "sun"}, {@code "mon"}, {@code "tue"},
     * {@code "wed"}, {@code "thu"}, {@code "fri"}, and {@code "sat"}
     * (case-sensitive). Any other non-{@code null} value yields an
     * {@link Unknown} instance that preserves the original string.
     *
     * @param value the wire-format string, or {@code null}
     * @return the corresponding day, or {@code null} if {@code value} is {@code null}
     */
    @ProtobufDeserializer
    static BusinessHoursDay of(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "sun" -> SUNDAY;
            case "mon" -> MONDAY;
            case "tue" -> TUESDAY;
            case "wed" -> WEDNESDAY;
            case "thu" -> THURSDAY;
            case "fri" -> FRIDAY;
            case "sat" -> SATURDAY;
            default -> new Unknown(value);
        };
    }

    /**
     * Returns the wire-format string representation of this day.
     *
     * @return the day value as sent over the wire (e.g. {@code "mon"})
     */
    @ProtobufSerializer
    String value();

    /**
     * Sunday.
     */
    record Sunday() implements BusinessHoursDay {
        /**
         * {@inheritDoc}
         *
         * @return {@code "sun"}
         */
        @Override
        public String value() {
            return "sun";
        }
    }

    /**
     * Monday.
     */
    record Monday() implements BusinessHoursDay {
        /**
         * {@inheritDoc}
         *
         * @return {@code "mon"}
         */
        @Override
        public String value() {
            return "mon";
        }
    }

    /**
     * Tuesday.
     */
    record Tuesday() implements BusinessHoursDay {
        /**
         * {@inheritDoc}
         *
         * @return {@code "tue"}
         */
        @Override
        public String value() {
            return "tue";
        }
    }

    /**
     * Wednesday.
     */
    record Wednesday() implements BusinessHoursDay {
        /**
         * {@inheritDoc}
         *
         * @return {@code "wed"}
         */
        @Override
        public String value() {
            return "wed";
        }
    }

    /**
     * Thursday.
     */
    record Thursday() implements BusinessHoursDay {
        /**
         * {@inheritDoc}
         *
         * @return {@code "thu"}
         */
        @Override
        public String value() {
            return "thu";
        }
    }

    /**
     * Friday.
     */
    record Friday() implements BusinessHoursDay {
        /**
         * {@inheritDoc}
         *
         * @return {@code "fri"}
         */
        @Override
        public String value() {
            return "fri";
        }
    }

    /**
     * Saturday.
     */
    record Saturday() implements BusinessHoursDay {
        /**
         * {@inheritDoc}
         *
         * @return {@code "sat"}
         */
        @Override
        public String value() {
            return "sat";
        }
    }

    /**
     * An unrecognized day value, provided for forward compatibility.
     *
     * @param value the raw wire-format string
     */
    record Unknown(String value) implements BusinessHoursDay {
    }
}
