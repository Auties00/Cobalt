package com.github.auties00.cobalt.wire.cloud.template;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The request to compare the delivery performance of one message template against others.
 *
 * <p>A comparison reports how a base template performs relative to a set of peer templates over a time
 * window, so a business can see which copy lands better before standardising on one. This model carries
 * the base {@link #templateId() template id}, the {@link #comparisonTemplateIds() peers} to compare it
 * against, and the inclusive {@link #start() start} and exclusive {@link #end() end} of the window. This
 * model is the input to {@code CloudWhatsAppClient.compareMessageTemplates}; the template id and both
 * window bounds are required, while an empty comparison list means no peers were named.
 */
@ProtobufMessage
public final class CloudMessageTemplateComparisonRequest {
    /**
     * The base template id whose performance is compared.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String templateId;

    /**
     * The peer template ids to compare the base template against, empty when none were named.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final List<String> comparisonTemplateIds;

    /**
     * The inclusive start of the comparison window.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    final Instant start;

    /**
     * The exclusive end of the comparison window.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    final Instant end;

    /**
     * Constructs a new template-comparison request.
     *
     * @param templateId            the base template id whose performance is compared
     * @param comparisonTemplateIds the peer template ids, or {@code null} when none were named
     * @param start                 the inclusive start of the comparison window
     * @param end                   the exclusive end of the comparison window
     * @throws NullPointerException if {@code templateId}, {@code start}, or {@code end} is {@code null}
     */
    CloudMessageTemplateComparisonRequest(String templateId, List<String> comparisonTemplateIds, Instant start,
                                          Instant end) {
        this.templateId = Objects.requireNonNull(templateId, "templateId must not be null");
        this.comparisonTemplateIds = comparisonTemplateIds == null ? List.of() : List.copyOf(comparisonTemplateIds);
        this.start = Objects.requireNonNull(start, "start must not be null");
        this.end = Objects.requireNonNull(end, "end must not be null");
    }

    /**
     * Returns the base template id whose performance is compared.
     *
     * @return the template id
     */
    public String templateId() {
        return templateId;
    }

    /**
     * Returns the peer template ids to compare the base template against.
     *
     * @return an unmodifiable list of peer template ids, empty when none were named
     */
    public List<String> comparisonTemplateIds() {
        return comparisonTemplateIds;
    }

    /**
     * Returns the inclusive start of the comparison window.
     *
     * @return the window start
     */
    public Instant start() {
        return start;
    }

    /**
     * Returns the exclusive end of the comparison window.
     *
     * @return the window end
     */
    public Instant end() {
        return end;
    }
}
