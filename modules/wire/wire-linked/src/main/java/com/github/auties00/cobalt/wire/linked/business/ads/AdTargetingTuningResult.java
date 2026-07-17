package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Server-adjusted WhatsApp Business ad targeting spec rewritten to comply
 * with one or more advertising-disclosure regulated categories.
 *
 * <p>When a merchant is composing a "Click-to-WhatsApp" ad (the paid
 * promotion that opens a chat with the business when tapped) and the
 * audience they have chosen falls under a regulated category (for example
 * housing, employment, credit, or one of the country-specific
 * special-ad-category rules), the server rewrites the targeting spec into
 * a compliant form. The WhatsApp Business client then applies the rewritten
 * spec back to its audience-targeting controls so the merchant sees the
 * adjusted audience before publishing.
 *
 * <p>This model is that adjusted spec: the JSON-encoded targeting spec the
 * server returned for the requested regulated category or categories. The
 * payload is exposed as a {@link String} because the spec's field set is
 * defined by the server and is not modelled as typed fields.
 */
@ProtobufMessage(name = "AdTargetingTuningResult")
public final class AdTargetingTuningResult {
    /**
     * JSON-encoded targeting spec the server rewrote to comply with the
     * requested regulated category or categories. {@code null} when the
     * server omitted the field.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String tunedTargetingSpec;

    /**
     * Constructs a new {@code AdTargetingTuningResult}.
     *
     * @param tunedTargetingSpec the JSON-encoded adjusted targeting spec,
     *                           or {@code null} when the server omitted it
     */
    AdTargetingTuningResult(String tunedTargetingSpec) {
        this.tunedTargetingSpec = tunedTargetingSpec;
    }

    /**
     * Returns the JSON-encoded adjusted targeting spec.
     *
     * @return an {@code Optional} carrying the JSON-encoded spec, or empty
     *         when the server omitted it
     */
    public Optional<String> tunedTargetingSpec() {
        return Optional.ofNullable(tunedTargetingSpec);
    }
}
