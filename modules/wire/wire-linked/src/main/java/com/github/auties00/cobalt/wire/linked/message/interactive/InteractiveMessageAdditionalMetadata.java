package com.github.auties00.cobalt.wire.linked.message.interactive;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Carries supplementary metadata attached to an interactive message.
 *
 * <p>This container currently exposes a single flag that tracks whether the user has fully
 * completed a "galaxy flow", the WhatsApp internal name for a business-driven multi-step
 * conversational flow. Additional fields may be added here as the protocol evolves without
 * touching the rest of the interactive message model.
 */
@ProtobufMessage(name = "InteractiveMessageAdditionalMetadata")
public final class InteractiveMessageAdditionalMetadata {
    /**
     * Raw flag indicating whether the associated galaxy flow has been fully completed.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean isGalaxyFlowCompleted;


    /**
     * Constructs a new metadata container with the supplied completion flag.
     *
     * @param isGalaxyFlowCompleted the raw completion flag, possibly {@code null}
     */
    InteractiveMessageAdditionalMetadata(Boolean isGalaxyFlowCompleted) {
        this.isGalaxyFlowCompleted = isGalaxyFlowCompleted;
    }

    /**
     * Returns whether the galaxy flow linked to this message has been completed.
     *
     * <p>A missing value is treated as {@code false}.
     *
     * @return {@code true} if the galaxy flow has been completed, {@code false} otherwise
     */
    public boolean isGalaxyFlowCompleted() {
        return isGalaxyFlowCompleted != null && isGalaxyFlowCompleted;
    }

    /**
     * Updates the galaxy flow completion flag.
     *
     * @param isGalaxyFlowCompleted the new flag value, or {@code null} to clear the field
     */
    public void setGalaxyFlowCompleted(Boolean isGalaxyFlowCompleted) {
        this.isGalaxyFlowCompleted = isGalaxyFlowCompleted;
    }
}
