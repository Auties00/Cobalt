package com.github.auties00.cobalt.wire.cloud.analytics;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The request describing a messaging-analytics query over a time window.
 *
 * <p>Messaging analytics report the number of messages sent and delivered, bucketed at the requested
 * {@link #granularity() granularity} over the inclusive {@link #start() start} to exclusive
 * {@link #end() end} window. The window can optionally be narrowed by phone number, product type, and
 * country; an empty filter list means no narrowing along that dimension. This model is the input to
 * {@code CloudWhatsAppClient.queryMessagingAnalytics}; the window bounds and the granularity are required.
 */
@ProtobufMessage
public final class CloudMessagingAnalyticsQuery {
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
    final CloudMessagingAnalytics.Granularity granularity;

    /**
     * The phone numbers to filter to, empty when not narrowed by phone.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final List<String> phoneNumbers;

    /**
     * The product types to filter to, empty when not narrowed by product type.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final List<String> productTypes;

    /**
     * The country codes to filter to, empty when not narrowed by country.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final List<String> countryCodes;

    /**
     * Constructs a new messaging-analytics query.
     *
     * @param start        the inclusive start of the window
     * @param end          the exclusive end of the window
     * @param granularity  the bucket granularity
     * @param phoneNumbers the phone-number filter, or {@code null} for none
     * @param productTypes the product-type filter, or {@code null} for none
     * @param countryCodes the country-code filter, or {@code null} for none
     * @throws NullPointerException if {@code start}, {@code end}, or {@code granularity} is {@code null}
     */
    CloudMessagingAnalyticsQuery(Instant start, Instant end, CloudMessagingAnalytics.Granularity granularity,
                                 List<String> phoneNumbers, List<String> productTypes, List<String> countryCodes) {
        this.start = Objects.requireNonNull(start, "start must not be null");
        this.end = Objects.requireNonNull(end, "end must not be null");
        this.granularity = Objects.requireNonNull(granularity, "granularity must not be null");
        this.phoneNumbers = phoneNumbers == null ? List.of() : List.copyOf(phoneNumbers);
        this.productTypes = productTypes == null ? List.of() : List.copyOf(productTypes);
        this.countryCodes = countryCodes == null ? List.of() : List.copyOf(countryCodes);
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
    public CloudMessagingAnalytics.Granularity granularity() {
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
     * Returns the product types to filter to.
     *
     * @return an unmodifiable list of product types, empty when not narrowed by product type
     */
    public List<String> productTypes() {
        return productTypes;
    }

    /**
     * Returns the country codes to filter to.
     *
     * @return an unmodifiable list of country codes, empty when not narrowed by country
     */
    public List<String> countryCodes() {
        return countryCodes;
    }
}
