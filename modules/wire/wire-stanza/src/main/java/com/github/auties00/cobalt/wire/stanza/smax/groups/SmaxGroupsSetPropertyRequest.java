package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the outbound {@code <iq type="set" xmlns="w:g2">} stanza that toggles one or more group-property flags.
 *
 * <p>This request backs the group-settings switches (admin-only edits, announcement mode, shared history,
 * ephemeral messages, membership approval, community sub-group creation, frequently-forwarded label,
 * admin-reports, and others). Each toggle corresponds to a distinct child element under the IQ envelope. The
 * relay treats the children as orthogonal; opposing pairs (such as {@link #locked()} versus {@link #unlocked()},
 * {@link #announcement()} versus {@link #notAnnouncement()}) are not validated client-side, and the relay rejects
 * conflicting requests with a {@link SmaxGroupsSetPropertyResponse.ClientError}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsSetPropertyRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseSetGroupMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseIQSetRequestMixin")
public final class SmaxGroupsSetPropertyRequest implements SmaxStanza.Request {
    /**
     * The group {@link Jid} whose properties are being toggled.
     */
    private final Jid groupJid;

    /**
     * Whether to emit a {@code <locked/>} child (chat-info edits become admin-only).
     */
    private final boolean locked;

    /**
     * Whether to emit an {@code <announcement/>} child (only admins may post messages).
     */
    private final boolean announcement;

    /**
     * Whether to emit a {@code <no_frequently_forwarded/>} child.
     */
    private final boolean noFrequentlyForwarded;

    /**
     * The optional ephemeral-message expiration in seconds.
     */
    private final Integer ephemeralExpiration;

    /**
     * The optional ephemeral-message trigger value paired with {@link #ephemeralExpiration}.
     */
    private final Integer ephemeralTrigger;

    /**
     * Whether to emit an {@code <unlocked/>} child (chat-info edits return to non-admin-allowed).
     */
    private final boolean unlocked;

    /**
     * Whether to emit a {@code <not_announcement/>} child (any participant may post messages).
     */
    private final boolean notAnnouncement;

    /**
     * Whether to emit a {@code <frequently_forwarded_ok/>} child.
     */
    private final boolean frequentlyForwardedOk;

    /**
     * Whether to emit a {@code <not_ephemeral/>} child (turn off ephemeral messages).
     */
    private final boolean notEphemeral;

    /**
     * The optional membership-approval mode value.
     */
    private final String membershipApprovalGroupJoinMode;

    /**
     * Whether to emit an {@code <allow_admin_reports/>} child.
     */
    private final boolean allowAdminReports;

    /**
     * Whether to emit a {@code <not_allow_admin_reports/>} child.
     */
    private final boolean notAllowAdminReports;

    /**
     * Whether to emit an {@code <allow_non_admin_sub_group_creation/>} child (community parent groups only).
     */
    private final boolean allowNonAdminSubGroupCreation;

    /**
     * Whether to emit a {@code <not_allow_non_admin_sub_group_creation/>} child.
     */
    private final boolean notAllowNonAdminSubGroupCreation;

    /**
     * Whether to emit a {@code <group_history/>} child (enable shared history for new joiners).
     */
    private final boolean groupHistory;

    /**
     * Whether to emit a {@code <no_group_history/>} child.
     */
    private final boolean noGroupHistory;

    /**
     * Constructs a set-property request.
     *
     * <p>Each boolean parameter governs whether the matching child element is emitted; the {@code Integer} and
     * {@code String} parameters carry optional attribute values. Opposing pairs are not validated client-side;
     * the relay rejects conflicting requests with a {@link SmaxGroupsSetPropertyResponse.ClientError}.
     *
     * @param groupJid                          the group {@link Jid}
     * @param locked                            whether to flip the group to locked
     * @param announcement                      whether to flip the group to announcement-only
     * @param noFrequentlyForwarded             whether to disable the frequently-forwarded label
     * @param ephemeralExpiration               optional ephemeral expiration in seconds; {@code null} skips the
     *                                          {@code <ephemeral/>} child
     * @param ephemeralTrigger                  optional ephemeral trigger value; ignored when
     *                                          {@code ephemeralExpiration} is {@code null}
     * @param unlocked                          whether to flip the group to unlocked
     * @param notAnnouncement                   whether to revert announcement-only mode
     * @param frequentlyForwardedOk             whether to re-enable the frequently-forwarded label
     * @param notEphemeral                      whether to turn off ephemeral messages
     * @param membershipApprovalGroupJoinMode   optional membership-approval mode value ({@code "on"} or
     *                                          {@code "off"}); {@code null} skips the
     *                                          {@code <membership_approval_mode/>} child
     * @param allowAdminReports                 whether to enable admin reports
     * @param notAllowAdminReports              whether to disable admin reports
     * @param allowNonAdminSubGroupCreation     whether to permit non-admin sub-group creation
     * @param notAllowNonAdminSubGroupCreation  whether to forbid non-admin sub-group creation
     * @param groupHistory                      whether to enable shared group history
     * @param noGroupHistory                    whether to disable shared group history
     * @throws NullPointerException if {@code groupJid} is {@code null}
     */
    public SmaxGroupsSetPropertyRequest(Jid groupJid,
                   boolean locked,
                   boolean announcement,
                   boolean noFrequentlyForwarded,
                   Integer ephemeralExpiration,
                   Integer ephemeralTrigger,
                   boolean unlocked,
                   boolean notAnnouncement,
                   boolean frequentlyForwardedOk,
                   boolean notEphemeral,
                   String membershipApprovalGroupJoinMode,
                   boolean allowAdminReports,
                   boolean notAllowAdminReports,
                   boolean allowNonAdminSubGroupCreation,
                   boolean notAllowNonAdminSubGroupCreation,
                   boolean groupHistory,
                   boolean noGroupHistory) {
        this.groupJid = Objects.requireNonNull(groupJid, "groupJid cannot be null");
        this.locked = locked;
        this.announcement = announcement;
        this.noFrequentlyForwarded = noFrequentlyForwarded;
        this.ephemeralExpiration = ephemeralExpiration;
        this.ephemeralTrigger = ephemeralTrigger;
        this.unlocked = unlocked;
        this.notAnnouncement = notAnnouncement;
        this.frequentlyForwardedOk = frequentlyForwardedOk;
        this.notEphemeral = notEphemeral;
        this.membershipApprovalGroupJoinMode = membershipApprovalGroupJoinMode;
        this.allowAdminReports = allowAdminReports;
        this.notAllowAdminReports = notAllowAdminReports;
        this.allowNonAdminSubGroupCreation = allowNonAdminSubGroupCreation;
        this.notAllowNonAdminSubGroupCreation = notAllowNonAdminSubGroupCreation;
        this.groupHistory = groupHistory;
        this.noGroupHistory = noGroupHistory;
    }

    /**
     * Returns the target group {@link Jid}.
     *
     * <p>The value routes verbatim into the IQ's {@code to} attribute.
     *
     * @return the group {@link Jid}; never {@code null}
     */
    public Jid groupJid() {
        return groupJid;
    }

    /**
     * Returns whether the request flips the group to locked.
     *
     * @return {@code true} when the {@code <locked/>} child is emitted
     */
    public boolean locked() {
        return locked;
    }

    /**
     * Returns whether the request flips the group to announcement-only.
     *
     * @return {@code true} when the {@code <announcement/>} child is emitted
     */
    public boolean announcement() {
        return announcement;
    }

    /**
     * Returns whether the request disables the frequently-forwarded label.
     *
     * @return {@code true} when the {@code <no_frequently_forwarded/>} child is emitted
     */
    public boolean noFrequentlyForwarded() {
        return noFrequentlyForwarded;
    }

    /**
     * Returns the optional ephemeral-message expiration value.
     *
     * <p>The result is empty when the request does not toggle ephemerality; present values are the expiration in
     * seconds.
     *
     * @return an {@link Optional} carrying the expiration in seconds, or empty
     */
    public Optional<Integer> ephemeralExpiration() {
        return Optional.ofNullable(ephemeralExpiration);
    }

    /**
     * Returns the optional ephemeral-message trigger value.
     *
     * @return an {@link Optional} carrying the trigger, or empty when the request omits it
     */
    public Optional<Integer> ephemeralTrigger() {
        return Optional.ofNullable(ephemeralTrigger);
    }

    /**
     * Returns whether the request flips the group to unlocked.
     *
     * @return {@code true} when the {@code <unlocked/>} child is emitted
     */
    public boolean unlocked() {
        return unlocked;
    }

    /**
     * Returns whether the request reverts announcement-only mode.
     *
     * @return {@code true} when the {@code <not_announcement/>} child is emitted
     */
    public boolean notAnnouncement() {
        return notAnnouncement;
    }

    /**
     * Returns whether the request re-enables the frequently-forwarded label.
     *
     * @return {@code true} when the {@code <frequently_forwarded_ok/>} child is emitted
     */
    public boolean frequentlyForwardedOk() {
        return frequentlyForwardedOk;
    }

    /**
     * Returns whether the request turns off ephemeral messages.
     *
     * @return {@code true} when the {@code <not_ephemeral/>} child is emitted
     */
    public boolean notEphemeral() {
        return notEphemeral;
    }

    /**
     * Returns the optional membership-approval mode value.
     *
     * <p>Present values are typically {@code "on"} or {@code "off"}; the result is empty when the request omits
     * the {@code <membership_approval_mode/>} child.
     *
     * @return an {@link Optional} carrying the join-mode value, or empty
     */
    public Optional<String> membershipApprovalGroupJoinMode() {
        return Optional.ofNullable(membershipApprovalGroupJoinMode);
    }

    /**
     * Returns whether the request enables admin reports.
     *
     * @return {@code true} when the {@code <allow_admin_reports/>} child is emitted
     */
    public boolean allowAdminReports() {
        return allowAdminReports;
    }

    /**
     * Returns whether the request disables admin reports.
     *
     * @return {@code true} when the {@code <not_allow_admin_reports/>} child is emitted
     */
    public boolean notAllowAdminReports() {
        return notAllowAdminReports;
    }

    /**
     * Returns whether the request allows non-admin sub-group creation.
     *
     * <p>The relay only honours this toggle on community parent groups.
     *
     * @return {@code true} when the {@code <allow_non_admin_sub_group_creation/>} child is emitted
     */
    public boolean allowNonAdminSubGroupCreation() {
        return allowNonAdminSubGroupCreation;
    }

    /**
     * Returns whether the request forbids non-admin sub-group creation.
     *
     * @return {@code true} when the {@code <not_allow_non_admin_sub_group_creation/>} child is emitted
     */
    public boolean notAllowNonAdminSubGroupCreation() {
        return notAllowNonAdminSubGroupCreation;
    }

    /**
     * Returns whether the request enables shared group history.
     *
     * @return {@code true} when the {@code <group_history/>} child is emitted
     */
    public boolean groupHistory() {
        return groupHistory;
    }

    /**
     * Returns whether the request disables shared group history.
     *
     * @return {@code true} when the {@code <no_group_history/>} child is emitted
     */
    public boolean noGroupHistory() {
        return noGroupHistory;
    }

    /**
     * Materialises the outbound IQ stanza ready for dispatch.
     *
     * <p>The resulting envelope nests each enabled toggle as a sibling under the IQ envelope, for example
     * {@snippet :
     *     <iq xmlns="w:g2" to="<groupJid>" type="set">
     *         <locked/>
     *         <announcement/>
     *         <ephemeral expiration="86400" trigger="0"/>
     *         <membership_approval_mode group_join_mode="on"/>
     *         ...
     *     </iq>
     * }
     * Children whose corresponding flag is {@code false} or whose attribute is {@code null} are omitted.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the selected toggle children
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsSetPropertyRequest",
            exports = "makeSetPropertyRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var children = new ArrayList<Stanza>();
        if (locked) {
            children.add(new StanzaBuilder().description("locked").build());
        }
        if (announcement) {
            children.add(new StanzaBuilder().description("announcement").build());
        }
        if (noFrequentlyForwarded) {
            children.add(new StanzaBuilder().description("no_frequently_forwarded").build());
        }
        if (ephemeralExpiration != null) {
            var ephemeralBuilder = new StanzaBuilder()
                    .description("ephemeral")
                    .attribute("expiration", ephemeralExpiration);
            if (ephemeralTrigger != null) {
                ephemeralBuilder.attribute("trigger", ephemeralTrigger);
            }
            children.add(ephemeralBuilder.build());
        }
        if (unlocked) {
            children.add(new StanzaBuilder().description("unlocked").build());
        }
        if (notAnnouncement) {
            children.add(new StanzaBuilder().description("not_announcement").build());
        }
        if (frequentlyForwardedOk) {
            children.add(new StanzaBuilder().description("frequently_forwarded_ok").build());
        }
        if (notEphemeral) {
            children.add(new StanzaBuilder().description("not_ephemeral").build());
        }
        if (membershipApprovalGroupJoinMode != null) {
            var membershipNode = new StanzaBuilder()
                    .description("membership_approval_mode")
                    .attribute("group_join_mode", membershipApprovalGroupJoinMode)
                    .build();
            children.add(membershipNode);
        }
        if (allowAdminReports) {
            children.add(new StanzaBuilder().description("allow_admin_reports").build());
        }
        if (notAllowAdminReports) {
            children.add(new StanzaBuilder().description("not_allow_admin_reports").build());
        }
        if (allowNonAdminSubGroupCreation) {
            children.add(new StanzaBuilder().description("allow_non_admin_sub_group_creation").build());
        }
        if (notAllowNonAdminSubGroupCreation) {
            children.add(new StanzaBuilder().description("not_allow_non_admin_sub_group_creation").build());
        }
        if (groupHistory) {
            children.add(new StanzaBuilder().description("group_history").build());
        }
        if (noGroupHistory) {
            children.add(new StanzaBuilder().description("no_group_history").build());
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", groupJid)
                .attribute("type", "set")
                .content(children);
    }

    /**
     * Compares this request to {@code obj} for value equality across every field.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsSetPropertyRequest} with identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGroupsSetPropertyRequest) obj;
        return this.locked == that.locked
                && this.announcement == that.announcement
                && this.noFrequentlyForwarded == that.noFrequentlyForwarded
                && this.unlocked == that.unlocked
                && this.notAnnouncement == that.notAnnouncement
                && this.frequentlyForwardedOk == that.frequentlyForwardedOk
                && this.notEphemeral == that.notEphemeral
                && this.allowAdminReports == that.allowAdminReports
                && this.notAllowAdminReports == that.notAllowAdminReports
                && this.allowNonAdminSubGroupCreation == that.allowNonAdminSubGroupCreation
                && this.notAllowNonAdminSubGroupCreation == that.notAllowNonAdminSubGroupCreation
                && this.groupHistory == that.groupHistory
                && this.noGroupHistory == that.noGroupHistory
                && Objects.equals(this.groupJid, that.groupJid)
                && Objects.equals(this.ephemeralExpiration, that.ephemeralExpiration)
                && Objects.equals(this.ephemeralTrigger, that.ephemeralTrigger)
                && Objects.equals(this.membershipApprovalGroupJoinMode, that.membershipApprovalGroupJoinMode);
    }

    /**
     * Returns a hash composed of every field.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(groupJid, locked, announcement, noFrequentlyForwarded, ephemeralExpiration,
                ephemeralTrigger, unlocked, notAnnouncement, frequentlyForwardedOk, notEphemeral,
                membershipApprovalGroupJoinMode, allowAdminReports, notAllowAdminReports,
                allowNonAdminSubGroupCreation, notAllowNonAdminSubGroupCreation, groupHistory, noGroupHistory);
    }

    /**
     * Returns a debug string carrying every field.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsSetPropertyRequest[groupJid=" + groupJid
                + ", locked=" + locked
                + ", announcement=" + announcement
                + ", noFrequentlyForwarded=" + noFrequentlyForwarded
                + ", ephemeralExpiration=" + ephemeralExpiration
                + ", ephemeralTrigger=" + ephemeralTrigger
                + ", unlocked=" + unlocked
                + ", notAnnouncement=" + notAnnouncement
                + ", frequentlyForwardedOk=" + frequentlyForwardedOk
                + ", notEphemeral=" + notEphemeral
                + ", membershipApprovalGroupJoinMode=" + membershipApprovalGroupJoinMode
                + ", allowAdminReports=" + allowAdminReports
                + ", notAllowAdminReports=" + notAllowAdminReports
                + ", allowNonAdminSubGroupCreation=" + allowNonAdminSubGroupCreation
                + ", notAllowNonAdminSubGroupCreation=" + notAllowNonAdminSubGroupCreation
                + ", groupHistory=" + groupHistory
                + ", noGroupHistory=" + noGroupHistory
                + ']';
    }
}
