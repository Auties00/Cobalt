package com.github.auties00.cobalt.wire.linked.reporting;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Enumerates the {@code context_flow} discriminator values WhatsApp emits when submitting a support
 * contact form, naming the in-app surface the form was opened from.
 *
 * <p>WhatsApp Web only ever emits {@link #GENERAL}, from the generic support entry point. The
 * server-side flow type may define further members, but they are not declared in the client bundle
 * and so are not modelled here. The value is carried on both the GraphQL submit mutation (its
 * {@code context_flow} input field) and the smax contact-form IQ (its
 * {@code <additional_attributes context_flow="...">} attribute); it routes the resulting support
 * ticket but does not change the submission outcome.
 *
 * <p>{@link #wireValue()} returns the literal sent on either wire; {@link #index()} returns the
 * protobuf-assigned numeric index used when this value is carried inside {@link SupportContactForm}.
 */
@ProtobufEnum
public enum SupportContactFormContextFlow {
    /**
     * Generic in-app support entry point; the only value WhatsApp Web emits for this discriminator.
     */
    GENERAL(0, "GENERAL");

    /**
     * The protobuf-assigned numeric index for this value.
     */
    final int index;

    /**
     * The literal value emitted on the wire {@code context_flow} field.
     */
    private final String wireValue;

    /**
     * Constructs a value bound to its protobuf index and wire literal.
     *
     * @param index     the protobuf enum index
     * @param wireValue the literal emitted on the {@code context_flow} field
     */
    SupportContactFormContextFlow(@ProtobufEnumIndex int index, String wireValue) {
        this.index = index;
        this.wireValue = wireValue;
    }

    /**
     * Returns the protobuf-assigned numeric index for this value.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }

    /**
     * Returns the literal value emitted on the wire {@code context_flow} field.
     *
     * @return the wire literal; never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }
}
