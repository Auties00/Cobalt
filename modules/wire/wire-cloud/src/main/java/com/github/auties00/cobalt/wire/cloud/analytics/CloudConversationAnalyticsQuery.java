package com.github.auties00.cobalt.wire.cloud.analytics;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The request describing a conversation-analytics query over a time window.
 *
 * <p>Conversation analytics report conversation counts and their cost, bucketed at the requested
 * {@link #granularity() granularity} over the inclusive {@link #start() start} to exclusive
 * {@link #end() end} window and broken down along the requested {@link #dimensions() dimensions}. The
 * window can optionally be narrowed by phone number, country, metric type, conversation category,
 * conversation type, and conversation direction; an empty filter list means no narrowing along that
 * dimension. This model is the input to {@code CloudWhatsAppClient.queryConversationAnalytics}; the window
 * bounds and the granularity are required.
 */
@ProtobufMessage
public final class CloudConversationAnalyticsQuery {
    /**
     * The inclusive start of the window.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    final Instant start;

    /**
     * The exclusive end of the window.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    final Instant end;

    /**
     * The bucket granularity.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    final CloudConversationAnalytics.Granularity granularity;

    /**
     * The phone numbers to filter to, empty when not narrowed by phone.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final List<String> phoneNumbers;

    /**
     * The country codes to filter to, empty when not narrowed by country.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final List<String> countryCodes;

    /**
     * The metric types to request, empty for the server default.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.ENUM)
    final List<CloudConversationAnalytics.MetricType> metricTypes;

    /**
     * The conversation categories to filter to, empty when not narrowed by category.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.ENUM)
    final List<CloudConversationAnalytics.ConversationCategory> conversationCategories;

    /**
     * The conversation types to filter to, empty when not narrowed by type.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.ENUM)
    final List<CloudConversationAnalytics.ConversationType> conversationTypes;

    /**
     * The conversation directions to filter to, empty when not narrowed by direction.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.ENUM)
    final List<CloudConversationAnalytics.ConversationDirection> conversationDirections;

    /**
     * The breakdown dimensions, empty for none.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.ENUM)
    final List<CloudConversationAnalytics.Dimension> dimensions;

    /**
     * Constructs a new conversation-analytics query.
     *
     * @param start                  the inclusive start of the window
     * @param end                    the exclusive end of the window
     * @param granularity            the bucket granularity
     * @param phoneNumbers           the phone-number filter, or {@code null} for none
     * @param countryCodes           the country-code filter, or {@code null} for none
     * @param metricTypes            the metric-type filter, or {@code null} for the server default
     * @param conversationCategories the conversation-category filter, or {@code null} for none
     * @param conversationTypes      the conversation-type filter, or {@code null} for none
     * @param conversationDirections the conversation-direction filter, or {@code null} for none
     * @param dimensions             the breakdown dimensions, or {@code null} for none
     * @throws NullPointerException if {@code start}, {@code end}, or {@code granularity} is {@code null}
     */
    CloudConversationAnalyticsQuery(Instant start, Instant end, CloudConversationAnalytics.Granularity granularity,
                                    List<String> phoneNumbers, List<String> countryCodes,
                                    List<CloudConversationAnalytics.MetricType> metricTypes,
                                    List<CloudConversationAnalytics.ConversationCategory> conversationCategories,
                                    List<CloudConversationAnalytics.ConversationType> conversationTypes,
                                    List<CloudConversationAnalytics.ConversationDirection> conversationDirections,
                                    List<CloudConversationAnalytics.Dimension> dimensions) {
        this.start = Objects.requireNonNull(start, "start must not be null");
        this.end = Objects.requireNonNull(end, "end must not be null");
        this.granularity = Objects.requireNonNull(granularity, "granularity must not be null");
        this.phoneNumbers = phoneNumbers == null ? List.of() : List.copyOf(phoneNumbers);
        this.countryCodes = countryCodes == null ? List.of() : List.copyOf(countryCodes);
        this.metricTypes = metricTypes == null ? List.of() : List.copyOf(metricTypes);
        this.conversationCategories = conversationCategories == null ? List.of() : List.copyOf(conversationCategories);
        this.conversationTypes = conversationTypes == null ? List.of() : List.copyOf(conversationTypes);
        this.conversationDirections = conversationDirections == null ? List.of() : List.copyOf(conversationDirections);
        this.dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
    }

    /**
     * Returns the inclusive start of the window.
     *
     * @return the window start
     */
    public Instant start() {
        return start;
    }

    /**
     * Returns the exclusive end of the window.
     *
     * @return the window end
     */
    public Instant end() {
        return end;
    }

    /**
     * Returns the bucket granularity.
     *
     * @return the granularity
     */
    public CloudConversationAnalytics.Granularity granularity() {
        return granularity;
    }

    /**
     * Returns the phone numbers to filter to.
     *
     * @return an unmodifiable list of phone numbers, empty when not narrowed by phone
     */
    public List<String> phoneNumbers() {
        return phoneNumbers;
    }

    /**
     * Returns the country codes to filter to.
     *
     * @return an unmodifiable list of country codes, empty when not narrowed by country
     */
    public List<String> countryCodes() {
        return countryCodes;
    }

    /**
     * Returns the metric types to request.
     *
     * @return an unmodifiable list of metric types, empty for the server default
     */
    public List<CloudConversationAnalytics.MetricType> metricTypes() {
        return metricTypes;
    }

    /**
     * Returns the conversation categories to filter to.
     *
     * @return an unmodifiable list of conversation categories, empty when not narrowed by category
     */
    public List<CloudConversationAnalytics.ConversationCategory> conversationCategories() {
        return conversationCategories;
    }

    /**
     * Returns the conversation types to filter to.
     *
     * @return an unmodifiable list of conversation types, empty when not narrowed by type
     */
    public List<CloudConversationAnalytics.ConversationType> conversationTypes() {
        return conversationTypes;
    }

    /**
     * Returns the conversation directions to filter to.
     *
     * @return an unmodifiable list of conversation directions, empty when not narrowed by direction
     */
    public List<CloudConversationAnalytics.ConversationDirection> conversationDirections() {
        return conversationDirections;
    }

    /**
     * Returns the breakdown dimensions.
     *
     * @return an unmodifiable list of breakdown dimensions, empty for none
     */
    public List<CloudConversationAnalytics.Dimension> dimensions() {
        return dimensions;
    }
}
