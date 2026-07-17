package com.github.auties00.cobalt.wire.linked.business.promotion;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Server acknowledgement of a logged WhatsApp quick-promotion interaction.
 *
 * <p>WhatsApp shows quick-promotion banners (small in-app cards advertising
 * a feature or an action the user might want to take) across both
 * consumer and Business surfaces. The client logs every interaction (a
 * banner impression, a dismiss tap, or a primary call-to-action click)
 * back to the server so the pacing engine can decide whether to keep
 * showing the same banner.
 *
 * <p>This model is the server's acknowledgement of one logged
 * interaction: a non-empty acknowledgement carries the echoed client
 * mutation identifier and is treated by WhatsApp as the success signal
 * for the logged event.
 */
@ProtobufMessage(name = "QuickPromotionLogAcknowledgement")
public final class QuickPromotionLogAcknowledgement {
    /**
     * Client mutation identifier the server echoed back. {@code null}
     * when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String clientMutationId;

    /**
     * Constructs a new {@code QuickPromotionLogAcknowledgement}.
     *
     * @param clientMutationId the echoed client mutation identifier, or
     *                         {@code null} when the server omitted it
     */
    QuickPromotionLogAcknowledgement(String clientMutationId) {
        this.clientMutationId = clientMutationId;
    }

    /**
     * Returns the echoed client mutation identifier.
     *
     * @return an {@code Optional} carrying the identifier, or empty when
     *         the server omitted it
     */
    public Optional<String> clientMutationId() {
        return Optional.ofNullable(clientMutationId);
    }
}
