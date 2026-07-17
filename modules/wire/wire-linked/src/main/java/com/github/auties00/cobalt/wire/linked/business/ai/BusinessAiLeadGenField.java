package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * One capture field of a WhatsApp Business AI agent's lead-capture flow.
 *
 * <p>A lead-capture flow asks an interested customer for a small set of
 * details (such as their name, email, or phone number). Each detail the flow
 * may ask for is a capture field: it carries the {@link #label()} shown to the
 * customer and whether the merchant has {@link #enabled()} it for collection.
 */
@ProtobufMessage(name = "BusinessAiLeadGenField")
public final class BusinessAiLeadGenField {
    /**
     * Human-readable label of the detail this field captures, as shown to the
     * customer. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String label;

    /**
     * Whether the merchant has enabled this field for collection. A disabled
     * field is part of the flow's definition but is not asked of the customer.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    final boolean enabled;

    /**
     * Constructs a new {@code BusinessAiLeadGenField}. The {@code label} may
     * be {@code null} when the server omitted it.
     *
     * @param label   the field label, or {@code null}
     * @param enabled whether the field is enabled for collection
     */
    BusinessAiLeadGenField(String label, boolean enabled) {
        this.label = label;
        this.enabled = enabled;
    }

    /**
     * Returns the human-readable label of the detail this field captures.
     *
     * @return the field label, or empty when the server omitted it
     */
    public Optional<String> label() {
        return Optional.ofNullable(label);
    }

    /**
     * Returns whether the merchant has enabled this field for collection.
     *
     * @return {@code true} when the field is collected, {@code false} otherwise
     */
    public boolean enabled() {
        return enabled;
    }
}
