package com.github.auties00.cobalt.wire.cloud.analytics;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The request describing a pricing-analytics query over a time window.
 *
 * <p>Pricing analytics report billed message volume and cost, bucketed at the requested
 * {@link #granularity() granularity} over the inclusive {@link #start() start} to exclusive
 * {@link #end() end} window and broken down along the requested {@link #dimensions() dimensions}. The
 * window can optionally be narrowed by phone number, country, metric type, pricing type, pricing category,
 * and tier; an empty filter list means no narrowing along that dimension. This model is the input to
 * {@code CloudWhatsAppClient.queryPricingAnalytics}; the window bounds and the granularity are required.
 */
@ProtobufMessage
public final class CloudPricingAnalyticsQuery {
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
    final CloudPricingAnalytics.Granularity granularity;

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
    final List<CloudPricingAnalytics.MetricType> metricTypes;

    /**
     * The breakdown dimensions, empty for none.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.ENUM)
    final List<CloudPricingAnalytics.Dimension> dimensions;

    /**
     * The pricing types to filter to, empty when not narrowed by type.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.ENUM)
    final List<CloudPricingAnalytics.PricingType> pricingTypes;

    /**
     * The pricing categories to filter to, empty when not narrowed by category.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.ENUM)
    final List<CloudPricingAnalytics.PricingCategory> pricingCategories;

    /**
     * The tiers to filter to, empty when not narrowed by tier.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    final List<String> tiers;

    /**
     * Constructs a new pricing-analytics query.
     *
     * @param start             the inclusive start of the window
     * @param end               the exclusive end of the window
     * @param granularity       the bucket granularity
     * @param phoneNumbers      the phone-number filter, or {@code null} for none
     * @param countryCodes      the country-code filter, or {@code null} for none
     * @param metricTypes       the metric-type filter, or {@code null} for the server default
     * @param dimensions        the breakdown dimensions, or {@code null} for none
     * @param pricingTypes      the pricing-type filter, or {@code null} for none
     * @param pricingCategories the pricing-category filter, or {@code null} for none
     * @param tiers             the tier filter, or {@code null} for none
     * @throws NullPointerException if {@code start}, {@code end}, or {@code granularity} is {@code null}
     */
    CloudPricingAnalyticsQuery(Instant start, Instant end, CloudPricingAnalytics.Granularity granularity,
                               List<String> phoneNumbers, List<String> countryCodes,
                               List<CloudPricingAnalytics.MetricType> metricTypes,
                               List<CloudPricingAnalytics.Dimension> dimensions,
                               List<CloudPricingAnalytics.PricingType> pricingTypes,
                               List<CloudPricingAnalytics.PricingCategory> pricingCategories, List<String> tiers) {
        this.start = Objects.requireNonNull(start, "start must not be null");
        this.end = Objects.requireNonNull(end, "end must not be null");
        this.granularity = Objects.requireNonNull(granularity, "granularity must not be null");
        this.phoneNumbers = phoneNumbers == null ? List.of() : List.copyOf(phoneNumbers);
        this.countryCodes = countryCodes == null ? List.of() : List.copyOf(countryCodes);
        this.metricTypes = metricTypes == null ? List.of() : List.copyOf(metricTypes);
        this.dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        this.pricingTypes = pricingTypes == null ? List.of() : List.copyOf(pricingTypes);
        this.pricingCategories = pricingCategories == null ? List.of() : List.copyOf(pricingCategories);
        this.tiers = tiers == null ? List.of() : List.copyOf(tiers);
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
    public CloudPricingAnalytics.Granularity granularity() {
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
    public List<CloudPricingAnalytics.MetricType> metricTypes() {
        return metricTypes;
    }

    /**
     * Returns the breakdown dimensions.
     *
     * @return an unmodifiable list of breakdown dimensions, empty for none
     */
    public List<CloudPricingAnalytics.Dimension> dimensions() {
        return dimensions;
    }

    /**
     * Returns the pricing types to filter to.
     *
     * @return an unmodifiable list of pricing types, empty when not narrowed by type
     */
    public List<CloudPricingAnalytics.PricingType> pricingTypes() {
        return pricingTypes;
    }

    /**
     * Returns the pricing categories to filter to.
     *
     * @return an unmodifiable list of pricing categories, empty when not narrowed by category
     */
    public List<CloudPricingAnalytics.PricingCategory> pricingCategories() {
        return pricingCategories;
    }

    /**
     * Returns the tiers to filter to.
     *
     * @return an unmodifiable list of tiers, empty when not narrowed by tier
     */
    public List<String> tiers() {
        return tiers;
    }
}
