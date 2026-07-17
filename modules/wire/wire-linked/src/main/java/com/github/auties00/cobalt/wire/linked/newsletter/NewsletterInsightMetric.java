package com.github.auties00.cobalt.wire.linked.newsletter;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a single analytics metric returned by the newsletter
 * insights query.
 *
 * <p>Newsletter admins use the insights surface to inspect engagement
 * statistics for their channels: total views, reactions, follower
 * growth, and so on. The relay reports each metric as an identifier
 * paired with a list of values; values are buckets that may be sliced
 * by country, role and timestamp depending on the metric. This type
 * exposes the metric identifier and its values as-is, leaving the
 * caller free to render them however is appropriate.
 */
@ProtobufMessage
public final class NewsletterInsightMetric {
    /**
     * The metric identifier (for example {@code "FOLLOWERS_TOTAL"}).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String identifier;

    /**
     * The reported values for this metric.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<Value> values;

    /**
     * The instant the insights aggregation that produced this metric was
     * last refreshed; older instants indicate the figures may be stale.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant lastUpdateTime;

    /**
     * The freshness status label reported alongside this metric (for
     * example a token signalling whether the figures are up to date or
     * still being computed).
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String metricsStatus;

    /**
     * Constructs a new {@code NewsletterInsightMetric}. Invoked by the
     * generated protobuf deserializer and by the converters that adapt
     * wire responses into the domain model.
     *
     * @param identifier     the metric identifier, may be {@code null}
     * @param values         the values; defaulted to an empty list when
     *                       {@code null}
     * @param lastUpdateTime the last-refresh instant, may be {@code null}
     * @param metricsStatus  the freshness status label, may be
     *                       {@code null}
     */
    NewsletterInsightMetric(String identifier, List<Value> values, Instant lastUpdateTime, String metricsStatus) {
        this.identifier = identifier;
        this.values = values == null ? List.of() : List.copyOf(values);
        this.lastUpdateTime = lastUpdateTime;
        this.metricsStatus = metricsStatus;
    }

    /**
     * Returns the metric identifier.
     *
     * @return an {@link Optional} carrying the identifier, or empty when
     *         not reported
     */
    public Optional<String> identifier() {
        return Optional.ofNullable(identifier);
    }

    /**
     * Returns the values reported for this metric.
     *
     * @return an unmodifiable list of values, never {@code null}
     */
    public List<Value> values() {
        return Collections.unmodifiableList(values);
    }

    /**
     * Returns the instant the insights aggregation behind this metric was
     * last refreshed.
     *
     * @return an {@link Optional} carrying the last-refresh instant, or
     *         empty when not reported
     */
    public Optional<Instant> lastUpdateTime() {
        return Optional.ofNullable(lastUpdateTime);
    }

    /**
     * Returns the freshness status label reported alongside this metric.
     *
     * @return an {@link Optional} carrying the status label, or empty when
     *         not reported
     */
    public Optional<String> metricsStatus() {
        return Optional.ofNullable(metricsStatus);
    }

    /**
     * Returns whether this metric equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a
     *         {@code NewsletterInsightMetric} carrying equal fields
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterInsightMetric that
                && Objects.equals(identifier, that.identifier)
                && Objects.equals(values, that.values)
                && Objects.equals(lastUpdateTime, that.lastUpdateTime)
                && Objects.equals(metricsStatus, that.metricsStatus);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(identifier, values, lastUpdateTime, metricsStatus);
    }

    /**
     * Returns a debug-oriented string representation.
     *
     * @return a human-readable string
     */
    @Override
    public String toString() {
        return "NewsletterInsightMetric[identifier=" + identifier +
                ", values=" + values.size() +
                ", lastUpdateTime=" + lastUpdateTime +
                ", metricsStatus=" + metricsStatus + ']';
    }

    /**
     * Represents a single value bucket of an insight metric.
     *
     * <p>Values may be sliced by country, role and timestamp; the
     * relay reports as many slices as it is willing to expose to the
     * caller.
     */
    @ProtobufMessage
    public static final class Value {
        /**
         * The numeric value of this slice, encoded as a string by the
         * relay so it can carry both integers and floating point counters
         * without loss of precision.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String value;

        /**
         * The ISO country code dimension this slice belongs to, when the
         * metric is sliced by country.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String country;

        /**
         * The viewer role dimension this slice belongs to, when the
         * metric is sliced by role.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String role;

        /**
         * The instant this slice corresponds to, when the metric is a
         * time series.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
        Instant timestamp;

        /**
         * Constructs a new {@code Value}. Invoked by the generated
         * protobuf deserializer and by the converters that adapt wire
         * responses into the domain model.
         *
         * @param value     the numeric value; may be {@code null}
         * @param country   the country dimension; may be {@code null}
         * @param role      the role dimension; may be {@code null}
         * @param timestamp the time-series instant; may be {@code null}
         */
        Value(String value, String country, String role, Instant timestamp) {
            this.value = value;
            this.country = country;
            this.role = role;
            this.timestamp = timestamp;
        }

        /**
         * Returns the numeric value of this slice.
         *
         * @return an {@link Optional} carrying the value, or empty when
         *         not reported
         */
        public Optional<String> value() {
            return Optional.ofNullable(value);
        }

        /**
         * Returns the ISO country code dimension this slice belongs to.
         *
         * @return an {@link Optional} carrying the country code, or empty
         *         when the metric is not sliced by country
         */
        public Optional<String> country() {
            return Optional.ofNullable(country);
        }

        /**
         * Returns the viewer role dimension this slice belongs to.
         *
         * @return an {@link Optional} carrying the role, or empty when
         *         the metric is not sliced by role
         */
        public Optional<String> role() {
            return Optional.ofNullable(role);
        }

        /**
         * Returns the instant this slice corresponds to.
         *
         * @return an {@link Optional} carrying the instant, or empty when
         *         the metric is not a time series
         */
        public Optional<Instant> timestamp() {
            return Optional.ofNullable(timestamp);
        }

        /**
         * Returns whether this value equals the supplied object.
         *
         * @param o the object to compare against
         * @return {@code true} if {@code o} is a {@code Value} carrying
         *         equal fields
         */
        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof Value that
                    && Objects.equals(value, that.value)
                    && Objects.equals(country, that.country)
                    && Objects.equals(role, that.role)
                    && Objects.equals(timestamp, that.timestamp);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(value, country, role, timestamp);
        }

        /**
         * Returns a debug-oriented string representation.
         *
         * @return a human-readable string
         */
        @Override
        public String toString() {
            return "Value[value=" + value +
                    ", country=" + country +
                    ", role=" + role +
                    ", timestamp=" + timestamp + ']';
        }
    }
}
