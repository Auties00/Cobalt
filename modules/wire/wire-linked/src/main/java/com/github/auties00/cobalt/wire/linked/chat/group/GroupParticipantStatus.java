package com.github.auties00.cobalt.wire.linked.chat.group;

import java.util.Arrays;

/**
 * Represents the per-participant status code returned by WhatsApp when an
 * administrator attempts to add, remove, promote, or demote members of a
 * group or community.
 *
 * <p>Each constant carries the HTTP-style numeric code emitted in the
 * {@code error} attribute of the {@code <participant/>} stanza in responses to
 * {@code addGroupParticipants}, {@code removeGroupParticipants},
 * {@code promoteGroupParticipants} and {@code demoteGroupParticipants} IQs.
 * The code {@link #OK} (200) denotes success; every other constant signals
 * a per-participant failure. Unknown or unmapped numeric codes resolve to
 * {@link #UNKNOWN}.
 *
 * <p>These values mirror the {@code code} field of the participant entries
 * produced by WA Web's {@code WAWebGroupModifyParticipantsJob} response
 * mappers.
 *
 * @see GroupParticipant
 */
public enum GroupParticipantStatus {
    /**
     * Placeholder value used when the server-returned code does not match
     * any recognised constant.
     */
    UNKNOWN(0),

    /**
     * The operation succeeded for this participant. Corresponds to the
     * default {@code "200"} code emitted by WA Web's
     * {@code WAWebGroupModifyParticipantsJob} when no error is attached.
     */
    OK(200),

    /**
     * The operation could not be completed because the caller does not
     * have permission to modify the participant (for example, attempting
     * to demote the group founder).
     */
    NOT_AUTHORIZED(403),

    /**
     * The targeted participant is not a WhatsApp user, so no action can
     * be taken.
     */
    NOT_WHATSAPP_USER(404),

    /**
     * The participant could not be added because they are not allowed to
     * join this group (for example, the group is locked or the user has
     * opted out of group invitations).
     */
    NOT_ALLOWED(405),

    /**
     * The participant is already a member of the group and therefore
     * cannot be re-added.
     */
    ALREADY_IN_GROUP(409),

    /**
     * The participant cannot be added because they are on the phone
     * number provider's reject list, typically because of spam-prevention
     * signals.
     */
    NOT_ACCEPTABLE(406),

    /**
     * A rate limit has been reached and the participant operation was
     * refused by the server.
     */
    RATE_LIMITED(429);

    /**
     * The numeric status code emitted on the wire.
     */
    private final int code;

    /**
     * Constructs a {@code GroupParticipantStatus} with the given numeric
     * code.
     *
     * @param code the HTTP-style numeric code
     */
    GroupParticipantStatus(int code) {
        this.code = code;
    }

    /**
     * Returns the {@code GroupParticipantStatus} matching the given numeric
     * code, falling back to {@link #UNKNOWN} when no constant matches.
     *
     * @param code the numeric status code, typically the value of the
     *             {@code error} attribute of a {@code <participant/>}
     *             stanza in the IQ response
     * @return the matching status, or {@link #UNKNOWN} if none matches
     */
    public static GroupParticipantStatus of(int code) {
        return Arrays.stream(values())
                .filter(entry -> entry.code() == code)
                .findFirst()
                .orElse(UNKNOWN);
    }

    /**
     * Returns the numeric status code for this participant outcome.
     *
     * @return the HTTP-style numeric code
     */
    public int code() {
        return code;
    }
}
