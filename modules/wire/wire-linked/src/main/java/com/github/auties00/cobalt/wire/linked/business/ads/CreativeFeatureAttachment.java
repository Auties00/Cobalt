package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Product-extension attachment of a creative-features spec.
 *
 * <p>The creative-features spec of an automatic-creative-optimisation ad group carries one product
 * extension describing the automated creative enhancements the platform may apply. This model holds
 * that extension: its {@link #actionMetadata() action metadata} and its {@link #enrollStatus() enrolment
 * status}.
 */
@ProtobufMessage(name = "CreativeFeatureAttachment")
public final class CreativeFeatureAttachment {
    /**
     * Action-metadata block of the product extension. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final CreativeFeatureActionMetadata actionMetadata;

    /**
     * Enrolment status of the product extension (for example {@code "OPT_OUT"}). Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String enrollStatus;

    /**
     * Constructs a new {@code CreativeFeatureAttachment}. Every argument may be {@code null} to leave the
     * corresponding field unset.
     *
     * @param actionMetadata the action-metadata block, or {@code null}
     * @param enrollStatus   the enrolment status, or {@code null}
     */
    CreativeFeatureAttachment(CreativeFeatureActionMetadata actionMetadata, String enrollStatus) {
        this.actionMetadata = actionMetadata;
        this.enrollStatus = enrollStatus;
    }

    /**
     * Returns the action-metadata block of the product extension.
     *
     * @return an {@link Optional} carrying the action-metadata block, or empty when unset
     */
    public Optional<CreativeFeatureActionMetadata> actionMetadata() {
        return Optional.ofNullable(actionMetadata);
    }

    /**
     * Returns the enrolment status of the product extension.
     *
     * @return an {@link Optional} carrying the enrolment status, or empty when unset
     */
    public Optional<String> enrollStatus() {
        return Optional.ofNullable(enrollStatus);
    }
}
