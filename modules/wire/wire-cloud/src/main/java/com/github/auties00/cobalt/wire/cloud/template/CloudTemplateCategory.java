package com.github.auties00.cobalt.wire.cloud.template;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The category a WhatsApp Cloud API message template is classified under.
 *
 * <p>Every message template belongs to exactly one category, which governs its review path, its pricing,
 * and the messaging windows it may be sent in. A business selects the category when it authors a template
 * or adopts one from the Template Library; the server may later reclassify a template, which surfaces as a
 * category-update webhook. The {@link #UNKNOWN} constant guards against tokens this client does not yet
 * model.
 */
@ProtobufEnum
public enum CloudTemplateCategory {
    /**
     * A category that this client does not recognise. Resolved for any token outside the modelled set so
     * that an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * A marketing template: promotions, offers, announcements, and other business-initiated outreach.
     */
    MARKETING(1),

    /**
     * A utility template: account updates, order confirmations, alerts, and other transactional follow-ups
     * to a user action.
     */
    UTILITY(2),

    /**
     * An authentication template: one-time passcodes and other identity-verification codes.
     */
    AUTHENTICATION(3);

    /**
     * The protobuf-assigned numeric index for this category.
     */
    final int index;

    /**
     * Constructs a {@code CloudTemplateCategory} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudTemplateCategory(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudTemplateCategory} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any unrecognised
     * or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on an unexpected
     * value.
     *
     * @param input the wire token, for example {@code "MARKETING"}, or {@code null}
     * @return the matching category, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudTemplateCategory of(String input) {
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
     * Returns the WhatsApp wire token for this category.
     *
     * @return the wire token, the constant name verbatim
     */
    public String token() {
        return name();
    }

    /**
     * Returns the protobuf-assigned numeric index for this category.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }
}
