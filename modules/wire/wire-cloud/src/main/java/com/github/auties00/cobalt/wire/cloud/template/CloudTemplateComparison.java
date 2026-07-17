package com.github.auties00.cobalt.wire.cloud.template;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The result of comparing the performance of two or more WhatsApp Cloud API message templates over a
 * time window.
 *
 * <p>The comparison endpoint returns a metric array; this model merges those metrics into two views:
 * <ul>
 * <li>{@link #blockRateOrder()} carries the compared template ids ordered by increasing block rate,
 * derived from the {@code BLOCK_RATE} metric.</li>
 * <li>{@link #perTemplate()} maps each template id to its per-template {@link Metrics}, joining the
 * {@code MESSAGE_SENDS} send count and the {@code TOP_BLOCK_REASON} block reason under one key.</li>
 * </ul>
 * Either view may be empty when the server omits the corresponding metric.
 */
public final class CloudTemplateComparison {
    /**
     * The compared template ids ordered by increasing block rate.
     */
    private final List<String> blockRateOrder;

    /**
     * The per-template metrics, keyed by template id.
     */
    private final Map<String, Metrics> perTemplate;

    /**
     * Constructs a new template comparison.
     *
     * @param blockRateOrder the template ids ordered by increasing block rate, or {@code null} for none
     * @param perTemplate    the per-template metrics keyed by template id, or {@code null} for none
     */
    public CloudTemplateComparison(List<String> blockRateOrder, Map<String, Metrics> perTemplate) {
        this.blockRateOrder = blockRateOrder == null ? List.of() : List.copyOf(blockRateOrder);
        this.perTemplate = perTemplate == null ? Map.of() : Map.copyOf(perTemplate);
    }

    /**
     * Returns the compared template ids ordered by increasing block rate.
     *
     * @return an unmodifiable list of template ids, empty when the {@code BLOCK_RATE} metric was absent
     */
    public List<String> blockRateOrder() {
        return blockRateOrder;
    }

    /**
     * Returns the per-template metrics.
     *
     * @return an unmodifiable map from template id to its {@link Metrics}, empty when neither the
     *         {@code MESSAGE_SENDS} nor the {@code TOP_BLOCK_REASON} metric was returned
     */
    public Map<String, Metrics> perTemplate() {
        return perTemplate;
    }

    /**
     * The merged metrics of a single compared template.
     *
     * <p>The two per-template metrics returned by the comparison endpoint are joined here: the
     * {@code MESSAGE_SENDS} send count and the dominant {@code TOP_BLOCK_REASON}. The block reason is
     * optional because the {@code TOP_BLOCK_REASON} metric may be absent for a template.
     */
    public static final class Metrics {
        /**
         * The number of times the template was sent.
         */
        private final long timesSent;

        /**
         * The dominant block reason of the template, or {@code null} when none was returned.
         */
        private final CloudTemplateBlockReason topBlockReason;

        /**
         * Constructs a new per-template metrics view.
         *
         * @param timesSent      the number of times the template was sent
         * @param topBlockReason the dominant block reason, or {@code null} when none was returned
         */
        public Metrics(long timesSent, CloudTemplateBlockReason topBlockReason) {
            this.timesSent = timesSent;
            this.topBlockReason = topBlockReason;
        }

        /**
         * Returns the number of times the template was sent.
         *
         * @return the send count, {@code 0} when the {@code MESSAGE_SENDS} metric was absent for the
         *         template
         */
        public long timesSent() {
            return timesSent;
        }

        /**
         * Returns the dominant block reason of the template.
         *
         * @return an {@link Optional} carrying the {@link CloudTemplateBlockReason}, or empty when the
         *         {@code TOP_BLOCK_REASON} metric was absent for the template
         */
        public Optional<CloudTemplateBlockReason> topBlockReason() {
            return Optional.ofNullable(topBlockReason);
        }
    }
}
