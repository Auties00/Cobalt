package com.github.auties00.cobalt.wire.cloud.analytics;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Pricing analytics of a WhatsApp Business Account.
 *
 * <p>Pricing analytics report billed message volume and cost over a time window, sliced into data
 * points at the requested granularity and broken down along the requested dimensions (phone number,
 * country, pricing type, pricing category, tier). The model flattens the buckets into a single
 * {@link DataPoint} list. Each data point carries its bucket boundaries, the volume and cost, and
 * whichever dimension fields the requested breakdown populated.
 *
 * <p>The breakdown dimensions are additive facets, not mutually exclusive variants: a single data
 * point may carry several dimension fields at once when the query requested more than one breakdown.
 * The query parameters are described by {@link Granularity}, {@link MetricType}, {@link Dimension},
 * {@link PricingType} and {@link PricingCategory}.
 */
public final class CloudPricingAnalytics {
    /**
     * The flattened per-bucket data points.
     */
    private final List<DataPoint> dataPoints;

    /**
     * Constructs new pricing analytics.
     *
     * @param dataPoints the flattened data points, or {@code null} for none
     */
    public CloudPricingAnalytics(List<DataPoint> dataPoints) {
        this.dataPoints = dataPoints == null ? List.of() : List.copyOf(dataPoints);
    }

    /**
     * Returns the flattened per-bucket data points.
     *
     * @return an unmodifiable list of data points, empty when none were returned
     */
    public List<DataPoint> dataPoints() {
        return dataPoints;
    }

    /**
     * The granularity at which pricing analytics are bucketed.
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
         * Daily buckets.
         */
        DAILY(2),
        /**
         * Monthly buckets.
         */
        MONTHLY(3);

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
         * @param input the wire token, for example {@code "DAILY"}, or {@code null}
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
     * The kind of metric a pricing-analytics query requests.
     *
     * <p>The {@link #UNKNOWN} constant guards against tokens this client does not yet model.
     */
    @ProtobufEnum
    public enum MetricType {
        /**
         * A metric type that this client does not recognise. Resolved for any token outside the modelled
         * set so that an unexpected value never fails decoding.
         */
        UNKNOWN(0),
        /**
         * The billed message volume metric.
         */
        VOLUME(1),
        /**
         * The cost metric.
         */
        COST(2);

        /**
         * The protobuf-assigned numeric index for this metric type.
         */
        final int index;

        /**
         * Constructs a {@code MetricType} with the specified protobuf index.
         *
         * @param index the protobuf enum index
         */
        MetricType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Returns the {@code MetricType} matching the given wire token.
         *
         * <p>The lookup matches the constant name case-insensitively against {@code input}; any
         * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
         * an unexpected value.
         *
         * @param input the wire token, for example {@code "VOLUME"}, or {@code null}
         * @return the matching metric type, or {@link #UNKNOWN} when {@code input} matches no constant
         */
        public static MetricType of(String input) {
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
         * Returns the protobuf-assigned numeric index for this metric type.
         *
         * @return the protobuf enum index
         */
        public int index() {
            return index;
        }
    }

    /**
     * A breakdown dimension a pricing-analytics query can group by.
     *
     * <p>The {@link #UNKNOWN} constant guards against tokens this client does not yet model.
     */
    @ProtobufEnum
    public enum Dimension {
        /**
         * A dimension that this client does not recognise. Resolved for any token outside the modelled set
         * so that an unexpected value never fails decoding.
         */
        UNKNOWN(0),
        /**
         * Break down by phone number.
         */
        PHONE(1),
        /**
         * Break down by country.
         */
        COUNTRY(2),
        /**
         * Break down by pricing type.
         */
        PRICING_TYPE(3),
        /**
         * Break down by pricing category.
         */
        PRICING_CATEGORY(4),
        /**
         * Break down by tier.
         */
        TIER(5);

        /**
         * The protobuf-assigned numeric index for this dimension.
         */
        final int index;

        /**
         * Constructs a {@code Dimension} with the specified protobuf index.
         *
         * @param index the protobuf enum index
         */
        Dimension(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Returns the {@code Dimension} matching the given wire token.
         *
         * <p>The lookup matches the constant name case-insensitively against {@code input}; any
         * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
         * an unexpected value.
         *
         * @param input the wire token, for example {@code "PHONE"}, or {@code null}
         * @return the matching dimension, or {@link #UNKNOWN} when {@code input} matches no constant
         */
        public static Dimension of(String input) {
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
         * Returns the protobuf-assigned numeric index for this dimension.
         *
         * @return the protobuf enum index
         */
        public int index() {
            return index;
        }
    }

    /**
     * The pricing type of a billed message.
     *
     * <p>The {@link #UNKNOWN} constant guards against tokens this client does not yet model.
     */
    @ProtobufEnum
    public enum PricingType {
        /**
         * A pricing type that this client does not recognise. Resolved for any token outside the modelled
         * set so that an unexpected value never fails decoding.
         */
        UNKNOWN(0),
        /**
         * A regular, billed message.
         */
        REGULAR(1),
        /**
         * A free entry-point message.
         */
        FREE_ENTRY_POINT(2),
        /**
         * A free customer-service message.
         */
        FREE_CUSTOMER_SERVICE(3),
        /**
         * A free group customer-service message.
         */
        FREE_GROUP_CUSTOMER_SERVICE(4);

        /**
         * The protobuf-assigned numeric index for this pricing type.
         */
        final int index;

        /**
         * Constructs a {@code PricingType} with the specified protobuf index.
         *
         * @param index the protobuf enum index
         */
        PricingType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Returns the {@code PricingType} matching the given wire token.
         *
         * <p>The lookup matches the constant name case-insensitively against {@code input}; any
         * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
         * an unexpected value.
         *
         * @param input the wire token, for example {@code "REGULAR"}, or {@code null}
         * @return the matching pricing type, or {@link #UNKNOWN} when {@code input} matches no constant
         */
        public static PricingType of(String input) {
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
         * Returns the protobuf-assigned numeric index for this pricing type.
         *
         * @return the protobuf enum index
         */
        public int index() {
            return index;
        }
    }

    /**
     * The pricing category of a billed message.
     *
     * <p>The {@link #UNKNOWN} constant guards against tokens this client does not yet model.
     */
    @ProtobufEnum
    public enum PricingCategory {
        /**
         * A pricing category that this client does not recognise. Resolved for any token outside the
         * modelled set so that an unexpected value never fails decoding.
         */
        UNKNOWN(0),
        /**
         * A marketing message.
         */
        MARKETING(1),
        /**
         * A utility message.
         */
        UTILITY(2),
        /**
         * An authentication message.
         */
        AUTHENTICATION(3),
        /**
         * A service message.
         */
        SERVICE(4),
        /**
         * An international authentication message.
         */
        AUTHENTICATION_INTERNATIONAL(5),
        /**
         * A group marketing message.
         */
        GROUP_MARKETING(6),
        /**
         * A group utility message.
         */
        GROUP_UTILITY(7),
        /**
         * A group service message.
         */
        GROUP_SERVICE(8),
        /**
         * A marketing-lite message.
         */
        MARKETING_LITE(9),
        /**
         * A dynamic marketing-lite message.
         */
        MARKETING_LITE_DYNAMIC(10),
        /**
         * A group marketing-lite message.
         */
        GROUP_MARKETING_LITE(11),
        /**
         * An AI-bot message.
         */
        AI_BOT(12);

        /**
         * The protobuf-assigned numeric index for this pricing category.
         */
        final int index;

        /**
         * Constructs a {@code PricingCategory} with the specified protobuf index.
         *
         * @param index the protobuf enum index
         */
        PricingCategory(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Returns the {@code PricingCategory} matching the given wire token.
         *
         * <p>The lookup matches the constant name case-insensitively against {@code input}; any
         * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
         * an unexpected value.
         *
         * @param input the wire token, for example {@code "MARKETING"}, or {@code null}
         * @return the matching pricing category, or {@link #UNKNOWN} when {@code input} matches no constant
         */
        public static PricingCategory of(String input) {
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
         * Returns the protobuf-assigned numeric index for this pricing category.
         *
         * @return the protobuf enum index
         */
        public int index() {
            return index;
        }
    }

    /**
     * A single pricing-analytics bucket.
     *
     * <p>Each bucket spans the half-open interval {@code [start, end)} and carries the billed volume and
     * cost for that interval. The dimension fields (pricing type, pricing category, phone number,
     * country, tier) are present only when the corresponding breakdown dimension was requested, so they
     * are exposed as {@link Optional}.
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
         * The billed message volume in the bucket.
         */
        private final long volume;

        /**
         * The cost in the bucket.
         */
        private final double cost;

        /**
         * The pricing type this bucket is scoped to, or {@code null} when not broken down by type.
         */
        private final String pricingType;

        /**
         * The pricing category this bucket is scoped to, or {@code null} when not broken down by category.
         */
        private final String pricingCategory;

        /**
         * The phone number this bucket is scoped to, or {@code null} when not broken down by phone.
         */
        private final String phoneNumber;

        /**
         * The country this bucket is scoped to, or {@code null} when not broken down by country.
         */
        private final String country;

        /**
         * The tier this bucket is scoped to, or {@code null} when not broken down by tier.
         */
        private final String tier;

        /**
         * Constructs a new data point.
         *
         * @param start           the inclusive start of the bucket
         * @param end             the exclusive end of the bucket
         * @param volume          the billed message volume
         * @param cost            the cost
         * @param pricingType     the scoped pricing type, or {@code null} when absent
         * @param pricingCategory the scoped pricing category, or {@code null} when absent
         * @param phoneNumber     the scoped phone number, or {@code null} when absent
         * @param country         the scoped country, or {@code null} when absent
         * @param tier            the scoped tier, or {@code null} when absent
         * @throws NullPointerException if {@code start} or {@code end} is {@code null}
         */
        public DataPoint(Instant start, Instant end, long volume, double cost, String pricingType,
                         String pricingCategory, String phoneNumber, String country, String tier) {
            this.start = Objects.requireNonNull(start, "start must not be null");
            this.end = Objects.requireNonNull(end, "end must not be null");
            this.volume = volume;
            this.cost = cost;
            this.pricingType = pricingType;
            this.pricingCategory = pricingCategory;
            this.phoneNumber = phoneNumber;
            this.country = country;
            this.tier = tier;
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
         * Returns the billed message volume in the bucket.
         *
         * @return the volume
         */
        public long volume() {
            return volume;
        }

        /**
         * Returns the cost in the bucket.
         *
         * @return the cost
         */
        public double cost() {
            return cost;
        }

        /**
         * Returns the pricing type this bucket is scoped to.
         *
         * @return an {@link Optional} carrying the pricing type, or empty when not broken down by type
         */
        public Optional<String> pricingType() {
            return Optional.ofNullable(pricingType);
        }

        /**
         * Returns the pricing category this bucket is scoped to.
         *
         * @return an {@link Optional} carrying the pricing category, or empty when not broken down by category
         */
        public Optional<String> pricingCategory() {
            return Optional.ofNullable(pricingCategory);
        }

        /**
         * Returns the phone number this bucket is scoped to.
         *
         * @return an {@link Optional} carrying the phone number, or empty when not broken down by phone
         */
        public Optional<String> phoneNumber() {
            return Optional.ofNullable(phoneNumber);
        }

        /**
         * Returns the country this bucket is scoped to.
         *
         * @return an {@link Optional} carrying the country, or empty when not broken down by country
         */
        public Optional<String> country() {
            return Optional.ofNullable(country);
        }

        /**
         * Returns the tier this bucket is scoped to.
         *
         * @return an {@link Optional} carrying the tier, or empty when not broken down by tier
         */
        public Optional<String> tier() {
            return Optional.ofNullable(tier);
        }
    }
}
