package com.github.auties00.cobalt.wire.linked.business.crossposting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Per-account additional-feature-set eligibility for the cross-posting
 * services.
 *
 * <p>The server attaches a single eligibility marker that gates the
 * additional-feature-set of the cross-posting and account-linking surfaces.
 * The marker is exposed as a raw string because its server value set is not
 * recoverable from the WhatsApp client.
 */
@ProtobufMessage(name = "CrossPostingAdditionalFeatureSet")
public final class CrossPostingAdditionalFeatureSet {
    /**
     * Server-defined additional-feature-set eligibility marker. {@code null}
     * when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String eligibility;

    /**
     * Constructs a new {@code CrossPostingAdditionalFeatureSet}.
     *
     * @param eligibility the additional-feature-set eligibility marker, or
     *                    {@code null}
     */
    CrossPostingAdditionalFeatureSet(String eligibility) {
        this.eligibility = eligibility;
    }

    /**
     * Returns the additional-feature-set eligibility marker.
     *
     * @return the eligibility marker, or empty when the server omitted it
     */
    public Optional<String> eligibility() {
        return Optional.ofNullable(eligibility);
    }
}
