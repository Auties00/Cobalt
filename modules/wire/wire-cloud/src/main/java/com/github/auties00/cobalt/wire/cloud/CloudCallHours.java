package com.github.auties00.cobalt.wire.cloud;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The business-hours configuration of the WhatsApp Cloud API Calling feature.
 *
 * <p>A business may restrict when consumers can call it. This model carries the enable status, the
 * timezone the hours are interpreted in, the recurring weekly operating-hour slots, and one-off holiday
 * overrides.
 */
public final class CloudCallHours {
    /**
     * The call-hours enable status, for example {@code "ENABLED"} or {@code "DISABLED"}, or
     * {@code null} when unset.
     */
    private final String status;

    /**
     * The IANA timezone id the hours are interpreted in, for example {@code "America/Manaus"}, or
     * {@code null} when unset.
     */
    private final String timezoneId;

    /**
     * The recurring weekly operating-hour slots.
     */
    private final List<WeeklyOperatingHours> weeklyOperatingHours;

    /**
     * The one-off holiday overrides.
     */
    private final List<HolidaySchedule> holidaySchedule;

    /**
     * Constructs a new call-hours configuration.
     *
     * @param status               the enable status, or {@code null} when unset
     * @param timezoneId           the timezone id, or {@code null} when unset
     * @param weeklyOperatingHours the recurring weekly slots, or {@code null} for none
     * @param holidaySchedule      the holiday overrides, or {@code null} for none
     */
    public CloudCallHours(String status, String timezoneId, List<WeeklyOperatingHours> weeklyOperatingHours,
                          List<HolidaySchedule> holidaySchedule) {
        this.status = status;
        this.timezoneId = timezoneId;
        this.weeklyOperatingHours = weeklyOperatingHours == null ? List.of() : List.copyOf(weeklyOperatingHours);
        this.holidaySchedule = holidaySchedule == null ? List.of() : List.copyOf(holidaySchedule);
    }

    /**
     * Returns the call-hours enable status.
     *
     * @return an {@link Optional} carrying the status, or empty when unset
     */
    public Optional<String> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns the timezone id the hours are interpreted in.
     *
     * @return an {@link Optional} carrying the timezone id, or empty when unset
     */
    public Optional<String> timezoneId() {
        return Optional.ofNullable(timezoneId);
    }

    /**
     * Returns the recurring weekly operating-hour slots.
     *
     * @return an unmodifiable list of weekly slots, empty when none were configured
     */
    public List<WeeklyOperatingHours> weeklyOperatingHours() {
        return weeklyOperatingHours;
    }

    /**
     * Returns the one-off holiday overrides.
     *
     * @return an unmodifiable list of holiday overrides, empty when none were configured
     */
    public List<HolidaySchedule> holidaySchedule() {
        return holidaySchedule;
    }

    /**
     * A recurring weekly operating-hour slot for a single day of the week.
     */
    public static final class WeeklyOperatingHours {
        /**
         * The day of the week the slot applies to, for example {@code "MONDAY"}.
         */
        private final String dayOfWeek;

        /**
         * The open time in {@code HHmm} 24-hour form, for example {@code "0400"}.
         */
        private final String openTime;

        /**
         * The close time in {@code HHmm} 24-hour form, for example {@code "1020"}.
         */
        private final String closeTime;

        /**
         * Constructs a new weekly operating-hour slot.
         *
         * @param dayOfWeek the day of the week
         * @param openTime  the open time in {@code HHmm} form
         * @param closeTime the close time in {@code HHmm} form
         * @throws NullPointerException if any argument is {@code null}
         */
        public WeeklyOperatingHours(String dayOfWeek, String openTime, String closeTime) {
            this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null");
            this.openTime = Objects.requireNonNull(openTime, "openTime must not be null");
            this.closeTime = Objects.requireNonNull(closeTime, "closeTime must not be null");
        }

        /**
         * Returns the day of the week the slot applies to.
         *
         * @return the day of the week
         */
        public String dayOfWeek() {
            return dayOfWeek;
        }

        /**
         * Returns the open time.
         *
         * @return the open time in {@code HHmm} form
         */
        public String openTime() {
            return openTime;
        }

        /**
         * Returns the close time.
         *
         * @return the close time in {@code HHmm} form
         */
        public String closeTime() {
            return closeTime;
        }
    }

    /**
     * A one-off holiday override for a single date.
     */
    public static final class HolidaySchedule {
        /**
         * The date the override applies to, in {@code yyyy-MM-dd} form, for example {@code "2026-01-01"}.
         */
        private final String date;

        /**
         * The start time in {@code HHmm} 24-hour form, for example {@code "0000"}.
         */
        private final String startTime;

        /**
         * The end time in {@code HHmm} 24-hour form, for example {@code "2359"}.
         */
        private final String endTime;

        /**
         * Constructs a new holiday override.
         *
         * @param date      the date in {@code yyyy-MM-dd} form
         * @param startTime the start time in {@code HHmm} form
         * @param endTime   the end time in {@code HHmm} form
         * @throws NullPointerException if any argument is {@code null}
         */
        public HolidaySchedule(String date, String startTime, String endTime) {
            this.date = Objects.requireNonNull(date, "date must not be null");
            this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
            this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        }

        /**
         * Returns the date the override applies to.
         *
         * @return the date in {@code yyyy-MM-dd} form
         */
        public String date() {
            return date;
        }

        /**
         * Returns the start time.
         *
         * @return the start time in {@code HHmm} form
         */
        public String startTime() {
            return startTime;
        }

        /**
         * Returns the end time.
         *
         * @return the end time in {@code HHmm} form
         */
        public String endTime() {
            return endTime;
        }
    }
}
