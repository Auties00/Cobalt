package com.github.auties00.cobalt.wire.linked.business.promotion;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Primary call-to-action carried by a WhatsApp quick-promotion creative.
 *
 * <p>Each quick-promotion banner exposes one primary tap target: a piece
 * of label text and the destination URL the client opens when the user
 * taps it.
 *
 * <p>This model is that call-to-action pair.
 */
@ProtobufMessage(name = "QuickPromotionAction")
public final class QuickPromotionAction {
    /**
     * Rendered call-to-action label, or {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String label;

    /**
     * Destination URL the client opens when the user taps the
     * call-to-action, or {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String destinationUrl;

    /**
     * Constructs a new {@code QuickPromotionAction}. Either reference
     * argument may be {@code null} when the server omitted the
     * corresponding field.
     *
     * @param label          the rendered call-to-action label, or
     *                       {@code null}
     * @param destinationUrl the destination URL, or {@code null}
     */
    QuickPromotionAction(String label, String destinationUrl) {
        this.label = label;
        this.destinationUrl = destinationUrl;
    }

    /**
     * Returns the rendered call-to-action label.
     *
     * @return an {@code Optional} carrying the label, or empty when the
     *         server omitted it
     */
    public Optional<String> label() {
        return Optional.ofNullable(label);
    }

    /**
     * Returns the destination URL the client opens when the user taps
     * the call-to-action.
     *
     * @return an {@code Optional} carrying the URL, or empty when the
     *         server omitted it
     */
    public Optional<String> destinationUrl() {
        return Optional.ofNullable(destinationUrl);
    }
}
