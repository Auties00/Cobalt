package com.github.auties00.cobalt.wire.cloud.analytics;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Per-template, per-bucket analytics of a WhatsApp Business Account message template.
 *
 * <p>Template analytics report the number of times a template was sent, delivered, and read over a time
 * window, sliced into daily data points, optionally with the per-button click breakdown. Each data
 * point becomes one instance of this model, carrying the template id, the bucket boundaries, the sent,
 * delivered, and read counts, and the button-click breakdown.
 *
 * <p>The {@code sent}, {@code delivered}, and {@code read} counts are present only when the
 * corresponding metric was requested, so they are exposed as {@link Optional}. The button-click
 * breakdown is empty when the template carries no buttons or the click metric was not requested. The
 * query parameters are described by {@link MetricType} and {@link ProductType}.
 */
public final class CloudTemplateAnalytics {
    /**
     * The template id this data point belongs to.
     */
    private final String templateId;

    /**
     * The inclusive start of the bucket.
     */
    private final Instant start;

    /**
     * The exclusive end of the bucket.
     */
    private final Instant end;

    /**
     * The number of times the template was sent, or {@code null} when not requested.
     */
    private final Long sent;

    /**
     * The number of times the template was delivered, or {@code null} when not requested.
     */
    private final Long delivered;

    /**
     * The number of times the template was read, or {@code null} when not requested.
     */
    private final Long read;

    /**
     * The per-button click breakdown.
     */
    private final List<ButtonClick> clicked;

    /**
     * Constructs new template analytics.
     *
     * @param templateId the template id, or {@code null} when absent
     * @param start      the inclusive start of the bucket
     * @param end        the exclusive end of the bucket
     * @param sent       the sent count, or {@code null} when not requested
     * @param delivered  the delivered count, or {@code null} when not requested
     * @param read       the read count, or {@code null} when not requested
     * @param clicked    the per-button click breakdown, or {@code null} for none
     * @throws NullPointerException if {@code start} or {@code end} is {@code null}
     */
    public CloudTemplateAnalytics(String templateId, Instant start, Instant end, Long sent, Long delivered,
                                  Long read, List<ButtonClick> clicked) {
        this.templateId = templateId;
        this.start = Objects.requireNonNull(start, "start must not be null");
        this.end = Objects.requireNonNull(end, "end must not be null");
        this.sent = sent;
        this.delivered = delivered;
        this.read = read;
        this.clicked = clicked == null ? List.of() : List.copyOf(clicked);
    }

    /**
     * Returns the template id this data point belongs to.
     *
     * @return the template id, or {@code null} when the server returned none
     */
    public String templateId() {
        return templateId;
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
     * Returns the number of times the template was sent.
     *
     * @return an {@link Optional} carrying the sent count, or empty when the metric was not requested
     */
    public Optional<Long> sent() {
        return Optional.ofNullable(sent);
    }

    /**
     * Returns the number of times the template was delivered.
     *
     * @return an {@link Optional} carrying the delivered count, or empty when the metric was not requested
     */
    public Optional<Long> delivered() {
        return Optional.ofNullable(delivered);
    }

    /**
     * Returns the number of times the template was read.
     *
     * @return an {@link Optional} carrying the read count, or empty when the metric was not requested
     */
    public Optional<Long> read() {
        return Optional.ofNullable(read);
    }

    /**
     * Returns the per-button click breakdown.
     *
     * @return an unmodifiable list of button clicks, empty when none were returned
     */
    public List<ButtonClick> clicked() {
        return clicked;
    }

    /**
     * The metric a template-analytics query requests.
     *
     * <p>The enum name is the verbatim token sent in the {@code metric_types(...)} field expansion. The
     * core {@code SENT}/{@code DELIVERED}/{@code READ}/{@code CLICKED} set is corroborated by multiple
     * sources; the conversion-related metrics are sourced from a documentation mirror, not a primary Meta
     * capture, and may change. The {@link #UNKNOWN} constant guards against tokens this client does not yet
     * model.
     */
    @ProtobufEnum
    public enum MetricType {
        /**
         * A metric that this client does not recognise. Resolved for any token outside the modelled set so
         * that an unexpected value never fails decoding.
         */
        UNKNOWN(0),
        /**
         * The number of times the template was sent.
         */
        SENT(1),
        /**
         * The number of times the template was delivered.
         */
        DELIVERED(2),
        /**
         * The number of times the template was read.
         */
        READ(3),
        /**
         * The per-button click breakdown.
         */
        CLICKED(4),
        /**
         * The cost metric.
         */
        COST(5),
        /**
         * App-activation conversions.
         */
        APP_ACTIVATIONS(6),
        /**
         * App add-to-cart conversions.
         */
        APP_ADD_TO_CART(7),
        /**
         * App checkout-initiated conversions.
         */
        APP_CHECKOUTS_INITIATED(8),
        /**
         * App purchase conversions.
         */
        APP_PURCHASES(9),
        /**
         * App purchase conversion value.
         */
        APP_PURCHASES_CONVERSION_VALUE(10),
        /**
         * Website add-to-cart conversions.
         */
        WEBSITE_ADD_TO_CART(11),
        /**
         * Website checkout-initiated conversions.
         */
        WEBSITE_CHECKOUTS_INITIATED(12),
        /**
         * Website purchase conversions.
         */
        WEBSITE_PURCHASES(13),
        /**
         * Website purchase conversion value.
         */
        WEBSITE_PURCHASES_CONVERSION_VALUE(14);

        /**
         * The protobuf-assigned numeric index for this metric.
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
         * @param input the wire token, for example {@code "SENT"}, or {@code null}
         * @return the matching metric, or {@link #UNKNOWN} when {@code input} matches no constant
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
         * Returns the protobuf-assigned numeric index for this metric.
         *
         * @return the protobuf enum index
         */
        public int index() {
            return index;
        }
    }

    /**
     * The product type a template-analytics query scopes to.
     *
     * <p>The enum name is the verbatim token sent in the {@code product_type(...)} field expansion. It
     * selects which messaging product the reported templates belong to; {@link #CLOUD_API} is the
     * default for templates sent through the Cloud API. The {@link #UNKNOWN} constant guards against tokens
     * this client does not yet model.
     */
    @ProtobufEnum
    public enum ProductType {
        /**
         * A product type that this client does not recognise. Resolved for any token outside the modelled
         * set so that an unexpected value never fails decoding.
         */
        UNKNOWN(0),
        /**
         * Templates sent through the Cloud API.
         */
        CLOUD_API(1),
        /**
         * Templates reported through campaign insights.
         */
        CAMPAIGN_INSIGHTS(2),
        /**
         * Templates sent through the Marketing Messages Lite API.
         */
        MARKETING_MESSAGES_LITE_API(3);

        /**
         * The protobuf-assigned numeric index for this product type.
         */
        final int index;

        /**
         * Constructs a {@code ProductType} with the specified protobuf index.
         *
         * @param index the protobuf enum index
         */
        ProductType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Returns the {@code ProductType} matching the given wire token.
         *
         * <p>The lookup matches the constant name case-insensitively against {@code input}; any
         * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
         * an unexpected value.
         *
         * @param input the wire token, for example {@code "CLOUD_API"}, or {@code null}
         * @return the matching product type, or {@link #UNKNOWN} when {@code input} matches no constant
         */
        public static ProductType of(String input) {
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
         * Returns the protobuf-assigned numeric index for this product type.
         *
         * @return the protobuf enum index
         */
        public int index() {
            return index;
        }
    }

    /**
     * The click count of a single template button within a bucket.
     *
     * @param type          the button type, for example {@code quick_reply_button} or {@code url_button}
     * @param buttonContent the displayed button content
     * @param count         the number of clicks
     */
    public record ButtonClick(String type, String buttonContent, long count) {
    }
}
