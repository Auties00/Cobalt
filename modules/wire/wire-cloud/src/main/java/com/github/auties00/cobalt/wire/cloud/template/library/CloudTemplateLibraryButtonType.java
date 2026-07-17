package com.github.auties00.cobalt.wire.cloud.template.library;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The kind of button a WhatsApp Cloud API Template Library entry exposes for binding.
 *
 * <p>When a business adopts a template from the Template Library it supplies the caller-bindable values
 * for the entry's buttons; the button type selects which value is required. A {@link #URL} button carries
 * a link, a {@link #PHONE_NUMBER} button carries a dialable number, a {@link #QUICK_REPLY} button carries
 * only its label, and an {@link #OTP} button carries the one-time-passcode delivery type. The
 * {@link #UNKNOWN} constant guards against tokens this client does not yet model.
 */
@ProtobufEnum
public enum CloudTemplateLibraryButtonType {
    /**
     * A button type that this client does not recognise. Resolved for any token outside the modelled set
     * so that an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * A button that opens a URL.
     */
    URL(1),

    /**
     * A button that dials a phone number.
     */
    PHONE_NUMBER(2),

    /**
     * A button that sends a fixed quick-reply payload.
     */
    QUICK_REPLY(3),

    /**
     * A button that delivers a one-time passcode.
     */
    OTP(4);

    /**
     * The protobuf-assigned numeric index for this button type.
     */
    final int index;

    /**
     * Constructs a {@code CloudTemplateLibraryButtonType} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudTemplateLibraryButtonType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudTemplateLibraryButtonType} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any unrecognised
     * or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on an unexpected
     * value.
     *
     * @param input the wire token, for example {@code "URL"}, or {@code null}
     * @return the matching button type, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudTemplateLibraryButtonType of(String input) {
        if (input == null) {
            return UNKNOWN;
        }
        for (var value : values()) {
            if (value != UNKNOWN && value.name().equalsIgnoreCase(input)) {
                return value;
            }
        }
        return UNKNOWN;
    }

    /**
     * Returns the protobuf-assigned numeric index for this button type.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }
}
