package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Action-metadata entry of a creative-features product extension.
 *
 * <p>When automatic creative optimisation enrols a Click-to-WhatsApp ad group, each product extension
 * carries an action-metadata block whose {@link #type() type} labels the automated action the platform
 * may take on the creative. The label is an opaque server token, carried verbatim.
 */
@ProtobufMessage(name = "CreativeFeatureActionMetadata")
public final class CreativeFeatureActionMetadata {
    /**
     * Type label of the automated action. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String type;

    /**
     * Constructs a new {@code CreativeFeatureActionMetadata}.
     *
     * @param type the action type label, or {@code null} to leave it unset
     */
    CreativeFeatureActionMetadata(String type) {
        this.type = type;
    }

    /**
     * Returns the type label of the automated action.
     *
     * @return an {@link Optional} carrying the action type label, or empty when unset
     */
    public Optional<String> type() {
        return Optional.ofNullable(type);
    }
}
