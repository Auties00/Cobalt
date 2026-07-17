package com.github.auties00.cobalt.wire.cloud.analytics;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Conversation analytics of a WhatsApp Business Account.
 *
 * <p>Conversation analytics report conversation counts and their associated cost over a time window,
 * sliced into data points at the requested granularity and broken down along the requested dimensions
 * (phone number, country, conversation type, conversation direction, conversation category). The
 * server nests data points under a {@code data[]} envelope; this model flattens them into a single
 * {@link DataPoint} list. Each data point carries its bucket boundaries, the conversation count, the
 * cost, and whichever dimension fields the requested breakdown populated.
 */
public final class CloudConversationAnalytics {
    /**
     * The flattened per-bucket data points.
     */
    private final List<DataPoint> dataPoints;

    /**
     * Constructs new conversation analytics.
     *
     * @param dataPoints the flattened data points, or {@code null} for none
     */
    public CloudConversationAnalytics(List<DataPoint> dataPoints) {
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
     * The granularity at which conversation analytics are bucketed.
     *
     * <p>The enum name is the verbatim token sent in the {@code granularity(...)} field expansion. Note
     * that conversation analytics use {@code DAILY} and {@code MONTHLY}, not the {@code DAY} and
     * {@code MONTH} tokens of messaging analytics. The {@link #UNKNOWN} constant guards against tokens this
     * client does not yet model.
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
     * The kind of metric a conversation-analytics query requests.
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
         * The conversation count metric.
         */
        CONVERSATION(1),
        /**
         * The conversation cost metric.
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
         * @param input the wire token, for example {@code "CONVERSATION"}, or {@code null}
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
     * The pricing category of a conversation.
     *
     * <p>The {@link #UNKNOWN} constant guards against tokens this client does not yet model.
     */
    @ProtobufEnum
    public enum ConversationCategory {
        /**
         * A conversation category that this client does not recognise. Resolved for any token outside the
         * modelled set so that an unexpected value never fails decoding.
         */
        UNKNOWN(0),
        /**
         * A marketing conversation.
         */
        MARKETING(1),
        /**
         * A utility conversation.
         */
        UTILITY(2),
        /**
         * An authentication conversation.
         */
        AUTHENTICATION(3),
        /**
         * A service conversation.
         */
        SERVICE(4),
        /**
         * An international authentication conversation.
         */
        AUTHENTICATION_INTERNATIONAL(5),
        /**
         * A marketing-lite conversation.
         */
        MARKETING_LITE(6);

        /**
         * The protobuf-assigned numeric index for this conversation category.
         */
        final int index;

        /**
         * Constructs a {@code ConversationCategory} with the specified protobuf index.
         *
         * @param index the protobuf enum index
         */
        ConversationCategory(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Returns the {@code ConversationCategory} matching the given wire token.
         *
         * <p>The lookup matches the constant name case-insensitively against {@code input}; any
         * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
         * an unexpected value.
         *
         * @param input the wire token, for example {@code "MARKETING"}, or {@code null}
         * @return the matching conversation category, or {@link #UNKNOWN} when {@code input} matches no
         *         constant
         */
        public static ConversationCategory of(String input) {
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
         * Returns the protobuf-assigned numeric index for this conversation category.
         *
         * @return the protobuf enum index
         */
        public int index() {
            return index;
        }
    }

    /**
     * The pricing type of a conversation.
     *
     * <p>The {@link #UNKNOWN} constant guards against tokens this client does not yet model.
     */
    @ProtobufEnum
    public enum ConversationType {
        /**
         * A conversation type that this client does not recognise. Resolved for any token outside the
         * modelled set so that an unexpected value never fails decoding.
         */
        UNKNOWN(0),
        /**
         * A regular, billed conversation.
         */
        REGULAR(1),
        /**
         * A free entry-point conversation.
         */
        FREE_ENTRY_POINT(2),
        /**
         * A free-tier conversation.
         */
        FREE_TIER(3);

        /**
         * The protobuf-assigned numeric index for this conversation type.
         */
        final int index;

        /**
         * Constructs a {@code ConversationType} with the specified protobuf index.
         *
         * @param index the protobuf enum index
         */
        ConversationType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Returns the {@code ConversationType} matching the given wire token.
         *
         * <p>The lookup matches the constant name case-insensitively against {@code input}; any
         * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
         * an unexpected value.
         *
         * @param input the wire token, for example {@code "REGULAR"}, or {@code null}
         * @return the matching conversation type, or {@link #UNKNOWN} when {@code input} matches no
         *         constant
         */
        public static ConversationType of(String input) {
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
         * Returns the protobuf-assigned numeric index for this conversation type.
         *
         * @return the protobuf enum index
         */
        public int index() {
            return index;
        }
    }

    /**
     * The direction of a conversation.
     *
     * <p>The {@link #UNKNOWN} constant guards against tokens this client does not yet model.
     */
    @ProtobufEnum
    public enum ConversationDirection {
        /**
         * A direction that this client does not recognise. Resolved for any token outside the modelled set
         * so that an unexpected value never fails decoding.
         */
        UNKNOWN(0),
        /**
         * A conversation initiated by the business.
         */
        BUSINESS_INITIATED(1),
        /**
         * A conversation initiated by the user.
         */
        USER_INITIATED(2);

        /**
         * The protobuf-assigned numeric index for this conversation direction.
         */
        final int index;

        /**
         * Constructs a {@code ConversationDirection} with the specified protobuf index.
         *
         * @param index the protobuf enum index
         */
        ConversationDirection(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Returns the {@code ConversationDirection} matching the given wire token.
         *
         * <p>The lookup matches the constant name case-insensitively against {@code input}; any
         * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
         * an unexpected value.
         *
         * @param input the wire token, for example {@code "USER_INITIATED"}, or {@code null}
         * @return the matching conversation direction, or {@link #UNKNOWN} when {@code input} matches no
         *         constant
         */
        public static ConversationDirection of(String input) {
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
         * Returns the protobuf-assigned numeric index for this conversation direction.
         *
         * @return the protobuf enum index
         */
        public int index() {
            return index;
        }
    }

    /**
     * A breakdown dimension a conversation-analytics query can group by.
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
         * Break down by conversation type.
         */
        CONVERSATION_TYPE(3),
        /**
         * Break down by conversation direction.
         */
        CONVERSATION_DIRECTION(4),
        /**
         * Break down by conversation category.
         */
        CONVERSATION_CATEGORY(5);

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
     * A single conversation-analytics bucket.
     *
     * <p>Each bucket spans the half-open interval {@code [start, end)} and carries the conversation count
     * and cost for that interval. The dimension fields (phone number, country, conversation type,
     * conversation direction, conversation category) are present only when the corresponding breakdown
     * dimension was requested, so they are exposed as {@link Optional}.
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
         * The conversation count in the bucket.
         */
        private final long conversation;

        /**
         * The conversation cost in the bucket.
         */
        private final double cost;

        /**
         * The phone number this bucket is scoped to, or {@code null} when not broken down by phone.
         */
        private final String phoneNumber;

        /**
         * The country this bucket is scoped to, or {@code null} when not broken down by country.
         */
        private final String country;

        /**
         * The conversation type this bucket is scoped to, or {@code null} when not broken down by type.
         */
        private final String conversationType;

        /**
         * The conversation direction this bucket is scoped to, or {@code null} when not broken down by
         * direction.
         */
        private final String conversationDirection;

        /**
         * The conversation category this bucket is scoped to, or {@code null} when not broken down by
         * category.
         */
        private final String conversationCategory;

        /**
         * Constructs a new data point.
         *
         * @param start                 the inclusive start of the bucket
         * @param end                   the exclusive end of the bucket
         * @param conversation          the conversation count
         * @param cost                  the conversation cost
         * @param phoneNumber           the scoped phone number, or {@code null} when absent
         * @param country               the scoped country, or {@code null} when absent
         * @param conversationType      the scoped conversation type, or {@code null} when absent
         * @param conversationDirection the scoped conversation direction, or {@code null} when absent
         * @param conversationCategory  the scoped conversation category, or {@code null} when absent
         * @throws NullPointerException if {@code start} or {@code end} is {@code null}
         */
        public DataPoint(Instant start, Instant end, long conversation, double cost, String phoneNumber,
                         String country, String conversationType, String conversationDirection,
                         String conversationCategory) {
            this.start = Objects.requireNonNull(start, "start must not be null");
            this.end = Objects.requireNonNull(end, "end must not be null");
            this.conversation = conversation;
            this.cost = cost;
            this.phoneNumber = phoneNumber;
            this.country = country;
            this.conversationType = conversationType;
            this.conversationDirection = conversationDirection;
            this.conversationCategory = conversationCategory;
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
         * Returns the conversation count in the bucket.
         *
         * @return the conversation count
         */
        public long conversation() {
            return conversation;
        }

        /**
         * Returns the conversation cost in the bucket.
         *
         * @return the conversation cost
         */
        public double cost() {
            return cost;
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
         * Returns the conversation type this bucket is scoped to.
         *
         * @return an {@link Optional} carrying the conversation type, or empty when not broken down by type
         */
        public Optional<String> conversationType() {
            return Optional.ofNullable(conversationType);
        }

        /**
         * Returns the conversation direction this bucket is scoped to.
         *
         * @return an {@link Optional} carrying the direction, or empty when not broken down by direction
         */
        public Optional<String> conversationDirection() {
            return Optional.ofNullable(conversationDirection);
        }

        /**
         * Returns the conversation category this bucket is scoped to.
         *
         * @return an {@link Optional} carrying the category, or empty when not broken down by category
         */
        public Optional<String> conversationCategory() {
            return Optional.ofNullable(conversationCategory);
        }
    }
}
