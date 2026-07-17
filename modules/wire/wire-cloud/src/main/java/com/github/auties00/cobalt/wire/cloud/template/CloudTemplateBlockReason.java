package com.github.auties00.cobalt.wire.cloud.template;

/**
 * The reason a recipient most commonly cited when blocking a WhatsApp Cloud API message template.
 *
 * <p>The template-comparison endpoint reports, per compared template, the dominant block reason in its
 * {@code TOP_BLOCK_REASON} metric. This enumeration projects the values that metric carries.
 */
public enum CloudTemplateBlockReason {
    /**
     * The recipient no longer needs the messages.
     */
    NO_LONGER_NEEDED,

    /**
     * The recipient gave no reason.
     */
    NO_REASON,

    /**
     * The recipient gave no reason (an alternative wording the server may report).
     */
    NO_REASON_GIVEN,

    /**
     * The recipient did not sign up for the messages.
     */
    NO_SIGN_UP,

    /**
     * The recipient found the messages offensive.
     */
    OFFENSIVE_MESSAGES,

    /**
     * The recipient gave a reason that does not map to another constant.
     */
    OTHER,

    /**
     * The recipient did not request the one-time password.
     */
    OTP_DID_NOT_REQUEST,

    /**
     * The recipient marked the messages as spam.
     */
    SPAM,

    /**
     * The server reported an unknown block reason.
     */
    UNKNOWN_BLOCK_REASON,

    /**
     * The block reason could not be mapped to any other constant; used as the parse fallback.
     */
    UNKNOWN;

    /**
     * Returns the constant matching the given wire value, falling back to {@link #UNKNOWN}.
     *
     * @param value the wire value, for example {@code "SPAM"}, or {@code null}
     * @return the matching constant, or {@link #UNKNOWN} when the value is {@code null} or unrecognized
     */
    public static CloudTemplateBlockReason of(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException exception) {
            return UNKNOWN;
        }
    }
}
