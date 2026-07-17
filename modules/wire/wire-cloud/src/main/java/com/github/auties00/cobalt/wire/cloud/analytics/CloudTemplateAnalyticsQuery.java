package com.github.auties00.cobalt.wire.cloud.analytics;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The request describing a per-template analytics query over a time window.
 *
 * <p>Template analytics report the sent, delivered, read, and per-button click counts of the named
 * templates, bucketed daily over the inclusive {@link #start() start} to exclusive {@link #end() end}
 * window. The {@link #templateIds() template ids} select which templates are reported and must be
 * non-empty. The {@link #metricTypes() metric types} narrow which metrics are returned, defaulting to the
 * server's set when empty; the {@link #productType() product type} scopes the report to a messaging
 * product; and {@link #useBusinessAccountTimezone() useBusinessAccountTimezone} requests bucketing in the
 * WhatsApp Business Account timezone. This model is the input to
 * {@code CloudWhatsAppClient.queryTemplateAnalytics}; the window bounds and the non-empty template-id list
 * are required.
 */
@ProtobufMessage
public final class CloudTemplateAnalyticsQuery {
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
     * The ids of the templates to report on, never empty.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final List<String> templateIds;

    /**
     * The metric types to request, empty for the server default.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    final List<CloudTemplateAnalytics.MetricType> metricTypes;

    /**
     * The messaging product to scope to, or {@code null} for the server default.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
    final CloudTemplateAnalytics.ProductType productType;

    /**
     * Whether to bucket in the WhatsApp Business Account timezone.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    final boolean useBusinessAccountTimezone;

    /**
     * Constructs a new template-analytics query.
     *
     * @param start                      the inclusive start of the window
     * @param end                        the exclusive end of the window
     * @param templateIds                the ids of the templates to report on
     * @param metricTypes                the metric-type filter, or {@code null} for the server default
     * @param productType                the messaging product to scope to, or {@code null} for the default
     * @param useBusinessAccountTimezone whether to bucket in the account timezone
     * @throws NullPointerException     if {@code start}, {@code end}, or {@code templateIds} is
     *                                  {@code null}
     * @throws IllegalArgumentException if {@code templateIds} is empty
     */
    CloudTemplateAnalyticsQuery(Instant start, Instant end, List<String> templateIds,
                                List<CloudTemplateAnalytics.MetricType> metricTypes,
                                CloudTemplateAnalytics.ProductType productType,
                                boolean useBusinessAccountTimezone) {
        this.start = Objects.requireNonNull(start, "start must not be null");
        this.end = Objects.requireNonNull(end, "end must not be null");
        Objects.requireNonNull(templateIds, "templateIds must not be null");
        if (templateIds.isEmpty()) {
            throw new IllegalArgumentException("templateIds must not be empty");
        }
        this.templateIds = List.copyOf(templateIds);
        this.metricTypes = metricTypes == null ? List.of() : List.copyOf(metricTypes);
        this.productType = productType;
        this.useBusinessAccountTimezone = useBusinessAccountTimezone;
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
     * Returns the ids of the templates to report on.
     *
     * @return an unmodifiable, non-empty list of template ids
     */
    public List<String> templateIds() {
        return templateIds;
    }

    /**
     * Returns the metric types to request.
     *
     * @return an unmodifiable list of metric types, empty for the server default
     */
    public List<CloudTemplateAnalytics.MetricType> metricTypes() {
        return metricTypes;
    }

    /**
     * Returns the messaging product to scope to.
     *
     * @return an {@link Optional} carrying the product type, or empty for the server default
     */
    public Optional<CloudTemplateAnalytics.ProductType> productType() {
        return Optional.ofNullable(productType);
    }

    /**
     * Returns whether to bucket in the WhatsApp Business Account timezone.
     *
     * @return the timezone flag
     */
    public boolean useBusinessAccountTimezone() {
        return useBusinessAccountTimezone;
    }
}
