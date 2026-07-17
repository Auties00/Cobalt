package com.github.auties00.cobalt.wire.linked.chat.group;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Represents a configurable WhatsApp group or community setting whose value
 * can be toggled by an administrator.
 *
 * <p>Each constant mirrors an entry in WhatsApp Web's
 * {@code WAWebGroupConstants.GROUP_SETTING_TYPE} map and carries the same
 * protocol-level string identifier emitted into {@code w:g2} IQ stanzas and
 * MEX group-property updates.
 *
 * <p>Several settings are dispatched as direct {@code w:g2} IQs via
 * {@code WAWebGroupModifyInfoJob.setGroupProperty} (for example
 * {@link #ANNOUNCEMENT}, {@link #RESTRICT}, {@link #EPHEMERAL},
 * {@link #MEMBERSHIP_APPROVAL_MODE}, {@link #REPORT_TO_ADMIN_MODE} and
 * {@link #ALLOW_NON_ADMIN_SUB_GROUP_CREATION}). Others flow through the MEX
 * {@code mexUpdateGroupPropertyJob} GraphQL endpoint in WA Web.
 *
 * @see com.github.auties00.cobalt.wire.linked.chat.ChatPolicy
 */
public enum GroupSetting {
    /**
     * Whether only administrators can send messages. Mirrors the
     * {@code "announcement"} identifier in WA Web's
     * {@code GROUP_SETTING_TYPE}.
     */
    ANNOUNCEMENT("announcement"),

    /**
     * Whether only administrators can edit the group info (subject,
     * description, picture). Mirrors the {@code "restrict"} identifier in
     * WA Web's {@code GROUP_SETTING_TYPE}.
     */
    RESTRICT("restrict"),

    /**
     * Whether messages that have been forwarded many times are blocked.
     * Mirrors the {@code "no_frequently_forwarded"} identifier in WA Web's
     * {@code GROUP_SETTING_TYPE}.
     */
    NO_FREQUENTLY_FORWARDED("no_frequently_forwarded"),

    /**
     * Whether ephemeral (disappearing) messages are enabled. Mirrors the
     * {@code "ephemeral"} identifier in WA Web's {@code GROUP_SETTING_TYPE}.
     */
    EPHEMERAL("ephemeral"),

    /**
     * Whether admin approval is required for new members to join. Mirrors
     * the {@code "membership_approval_mode"} identifier in WA Web's
     * {@code GROUP_SETTING_TYPE}.
     */
    MEMBERSHIP_APPROVAL_MODE("membership_approval_mode"),

    /**
     * Whether members can report messages to administrators. Mirrors the
     * {@code "report_to_admin_mode"} identifier in WA Web's
     * {@code GROUP_SETTING_TYPE}.
     */
    REPORT_TO_ADMIN_MODE("report_to_admin_mode"),

    /**
     * Whether non-admin community members may create subgroups. Mirrors the
     * {@code "allow_non_admin_sub_group_creation"} identifier in WA Web's
     * {@code GROUP_SETTING_TYPE}.
     */
    ALLOW_NON_ADMIN_SUB_GROUP_CREATION("allow_non_admin_sub_group_creation"),

    /**
     * Whether only administrators can add new members. Mirrors the
     * {@code "member_add_mode"} identifier in WA Web's
     * {@code GROUP_SETTING_TYPE}.
     */
    MEMBER_ADD_MODE("member_add_mode"),

    /**
     * Whether only administrators can share invite links. Mirrors the
     * {@code "member_link_mode"} identifier in WA Web's
     * {@code GROUP_SETTING_TYPE}.
     */
    MEMBER_LINK_MODE("member_link_mode"),

    /**
     * Whether sharing is rate-limited to reduce spam. Mirrors the
     * {@code "limit_sharing"} identifier in WA Web's
     * {@code GROUP_SETTING_TYPE}.
     */
    LIMIT_SHARING("limit_sharing"),

    /**
     * Whether all members (or only admins) can send message history to
     * newly added members. Mirrors the
     * {@code "member_share_group_history_mode"} identifier in WA Web's
     * {@code GROUP_SETTING_TYPE}.
     */
    MEMBER_SHARE_GROUP_HISTORY_MODE("member_share_group_history_mode");

    /**
     * The protocol-level string identifier emitted on the wire.
     */
    private final String data;

    /**
     * Constructs a {@code GroupSetting} with the given protocol identifier.
     *
     * @param data the wire-level identifier
     */
    GroupSetting(String data) {
        this.data = data;
    }

    /**
     * Returns the {@code GroupSetting} matching the given protocol-level
     * identifier.
     *
     * @param input the wire-level identifier as defined by WA Web's
     *              {@code GROUP_SETTING_TYPE}
     * @return the matching setting constant
     * @throws NoSuchElementException if no constant matches the input
     */
    public static GroupSetting of(String input) {
        return Arrays.stream(values())
                .filter(entry -> Objects.equals(entry.data(), input))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Cannot find GroupSetting for %s".formatted(input)));
    }

    /**
     * Returns the protocol-level string identifier for this setting.
     *
     * @return the wire-level identifier
     */
    public String data() {
        return data;
    }
}
