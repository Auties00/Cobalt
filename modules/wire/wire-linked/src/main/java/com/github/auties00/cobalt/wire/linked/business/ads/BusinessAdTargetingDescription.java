package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * One human-readable line describing who a WhatsApp Business advertisement will
 * reach.
 *
 * <p>While a merchant builds the audience for a "Click-to-WhatsApp" ad (a paid
 * promotion that opens a chat with the business when tapped), the server renders
 * the chosen targeting into plain-language sentences so the merchant can read
 * back who the ad will be shown to without inspecting the raw specification.
 * This model is one such sentence: a category and the rendered values for it
 * (for example the category "Location" with the values "United States, Canada").
 *
 * <p>{@link #category()} names the targeting facet the sentence describes;
 * {@link #values()} is the rendered description of the chosen values for that
 * facet; and {@link #meta()} carries an opaque server token the audience editor
 * uses to drive interaction with the sentence.
 */
@ProtobufMessage(name = "BusinessAdTargetingDescription")
public final class BusinessAdTargetingDescription {
    /**
     * Name of the targeting facet the sentence describes (for example location,
     * age, or interests). Empty when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String category;

    /**
     * Rendered description of the chosen values for the facet. Empty when the
     * server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String values;

    /**
     * Opaque server token the audience editor uses to drive interaction with the
     * sentence. Exposed as the raw server-defined string. Empty when the server
     * omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String meta;

    /**
     * Constructs a new {@code BusinessAdTargetingDescription}. The reference
     * arguments may be {@code null} when the server omitted them.
     *
     * @param category the targeting facet name, or {@code null}
     * @param values   the rendered values description, or {@code null}
     * @param meta     the opaque interaction token, or {@code null}
     */
    BusinessAdTargetingDescription(String category, String values, String meta) {
        this.category = category;
        this.values = values;
        this.meta = meta;
    }

    /**
     * Returns the name of the targeting facet the sentence describes.
     *
     * @return the targeting facet name, or empty when the server omitted it
     */
    public Optional<String> category() {
        return Optional.ofNullable(category);
    }

    /**
     * Returns the rendered description of the chosen values for the facet.
     *
     * @return the rendered values description, or empty when the server omitted
     *         it
     */
    public Optional<String> values() {
        return Optional.ofNullable(values);
    }

    /**
     * Returns the opaque server token the audience editor uses to drive
     * interaction with the sentence.
     *
     * @return the opaque interaction token, or empty when the server omitted it
     */
    public Optional<String> meta() {
        return Optional.ofNullable(meta);
    }
}
