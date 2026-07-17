package com.github.auties00.cobalt.wire.cloud.analytics;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Messaging analytics of a WhatsApp Business Account.
 *
 * <p>Messaging analytics report the number of messages sent and delivered over a time window, sliced
 * into data points at the requested granularity. The window can optionally be filtered by phone number,
 * product type, and country. This model carries the echoed phone-number and country filters, the
 * granularity token the server reports, and the per-bucket data points; each
 * {@link DataPoint data point} carries its bucket boundaries and the sent and delivered counts.
 */
public final class CloudMessagingAnalytics {
    /**
     * The phone numbers the analytics were filtered to, echoed by the server.
     */
    private final List<String> phoneNumbers;

    /**
     * The country codes the analytics were filtered to, echoed by the server.
     */
    private final List<String> countryCodes;

    /**
     * The granularity token the server reports, for example {@code DAILY}, or {@code null} when absent.
     */
    private final String granularity;

    /**
     * The per-bucket data points.
     */
    private final List<DataPoint> dataPoints;

    /**
     * Constructs new messaging analytics.
     *
     * @param phoneNumbers the echoed phone-number filter, or {@code null} for none
     * @param countryCodes the echoed country-code filter, or {@code null} for none
     * @param granularity  the reported granularity token, or {@code null} when absent
     * @param dataPoints   the per-bucket data points, or {@code null} for none
     */
    public CloudMessagingAnalytics(List<String> phoneNumbers, List<String> countryCodes, String granularity,
                                   List<DataPoint> dataPoints) {
        this.phoneNumbers = phoneNumbers == null ? List.of() : List.copyOf(phoneNumbers);
        this.countryCodes = countryCodes == null ? List.of() : List.copyOf(countryCodes);
        this.granularity = granularity;
        this.dataPoints = dataPoints == null ? List.of() : List.copyOf(dataPoints);
    }

    /**
     * Returns the phone numbers the analytics were filtered to.
     *
     * @return an unmodifiable list of phone numbers, empty when none were applied
     */
    public List<String> phoneNumbers() {
        return phoneNumbers;
    }

    /**
     * Returns the country codes the analytics were filtered to.
     *
     * @return an unmodifiable list of country codes, empty when none were applied
     */
    public List<String> countryCodes() {
        return countryCodes;
    }

    /**
     * Returns the granularity token the server reports.
     *
     * @return the granularity token, or {@code null} when the server reported none
     */
    public String granularity() {
        return granularity;
    }

    /**
     * Returns the per-bucket data points.
     *
     * @return an unmodifiable list of data points, empty when none were returned
     */
    public List<DataPoint> dataPoints() {
        return dataPoints;
    }

    /**
     * The granularity at which messaging analytics are bucketed.
     *
     * <p>The enum name is the verbatim token sent in the {@code granularity(...)} field expansion. The
     * {@link #UNKNOWN} constant guards against tokens this client does not yet model.
     */
    @ProtobufEnum
    public enum Granularity {
        /**
         * A granularity that this client does not recognise. Resolved for any token outside the modelled
         * set so that an unexpected value never fails decoding.
         */
        UNKNOWN(0),
        /**
         * Half-hour buckets.
         */
        HALF_HOUR(1),
        /**
         * Daily buckets; the server default when no granularity is requested.
         */
        DAY(2),
        /**
         * Monthly buckets.
         */
        MONTH(3);

        /**
         * The protobuf-assigned numeric index for this granularity.
         */
        final int index;

        /**
         * Constructs a {@code Granularity} with the specified protobuf index.
         *
         * @param index the protobuf enum index
         */
        Granularity(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Returns the {@code Granularity} matching the given wire token.
         *
         * <p>The lookup matches the constant name case-insensitively against {@code input}; any
         * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
         * an unexpected value.
         *
         * @param input the wire token, for example {@code "DAY"}, or {@code null}
         * @return the matching granularity, or {@link #UNKNOWN} when {@code input} matches no constant
         */
        public static Granularity of(String input) {
            if (input == null) {
                return UNKNOWN;
            }
            for (var value : values()) {
                if (value != UNKNOWN && value.name().equalsIgnoreCase(input)) {
                    return value;
                }
            }
            return UNKNOWN;
        }

        /**
         * Returns the protobuf-assigned numeric index for this granularity.
         *
         * @return the protobuf enum index
         */
        public int index() {
            return index;
        }
    }

    /**
     * A single messaging-analytics bucket.
     *
     * <p>Each bucket spans the half-open interval {@code [start, end)} and carries the count of messages
     * sent and delivered in that interval.
     */
    public static final class DataPoint {
        /**
         * The inclusive start of the bucket.
         */
        private final Instant start;

        /**
         * The exclusive end of the bucket.
         */
        private final Instant end;

        /**
         * The number of messages sent in the bucket.
         */
        private final int sent;

        /**
         * The number of messages delivered in the bucket.
         */
        private final int delivered;

        /**
         * Constructs a new data point.
         *
         * @param start     the inclusive start of the bucket
         * @param end       the exclusive end of the bucket
         * @param sent      the number of messages sent
         * @param delivered the number of messages delivered
         * @throws NullPointerException if {@code start} or {@code end} is {@code null}
         */
        public DataPoint(Instant start, Instant end, int sent, int delivered) {
            this.start = Objects.requireNonNull(start, "start must not be null");
            this.end = Objects.requireNonNull(end, "end must not be null");
            this.sent = sent;
            this.delivered = delivered;
        }

        /**
         * Returns the inclusive start of the bucket.
         *
         * @return the bucket start
         */
        public Instant start() {
            return start;
        }

        /**
         * Returns the exclusive end of the bucket.
         *
         * @return the bucket end
         */
        public Instant end() {
            return end;
        }

        /**
         * Returns the number of messages sent in the bucket.
         *
         * @return the sent count
         */
        public int sent() {
            return sent;
        }

        /**
         * Returns the number of messages delivered in the bucket.
         *
         * @return the delivered count
         */
        public int delivered() {
            return delivered;
        }
    }
}
