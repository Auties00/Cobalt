package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Outbound {@code <iq type="set" xmlns="w:g2" to="g.us">} stanza that creates a new group, community sub-group,
 * or community parent.
 *
 * This request backs every group-creation pipeline: regular group, breakout sub-group, community-parent, hidden
 * sub-group, and so on. The wide field surface fuses every optional child into a single envelope, so most fields
 * are opt-in markers. New call sites should prefer the fluent {@link Builder} over the all-args constructor.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsCreateRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseSetServerMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseIQSetRequestMixin")
public final class SmaxGroupsCreateRequest implements SmaxStanza.Request {
    /**
     * Holds the group's subject (display name) emitted as the {@code <create subject="...">} attribute.
     */
    private final String subject;

    /**
     * Holds the seed-participant rows emitted as {@code <participant>} children.
     */
    private final List<RequestParticipant> participants;

    /**
     * Holds the optional description body, emitted under {@code <description><body/></description>}.
     */
    private final String descriptionBody;

    /**
     * Holds the optional description id emitted as the {@code <description id="...">} attribute.
     */
    private final String descriptionId;

    /**
     * Indicates whether to attach a {@code <locked/>} child marking chat-info edits as admin-only.
     */
    private final boolean locked;

    /**
     * Indicates whether to attach an {@code <announcement/>} child restricting posting to admins.
     */
    private final boolean announcement;

    /**
     * Indicates whether to attach a {@code <parent default_membership_approval_mode="request_required"/>} child
     * marking this group as a community parent.
     */
    private final boolean parentDefaultMembershipApprovalMode;

    /**
     * Indicates whether to attach a {@code <no_frequently_forwarded/>} child.
     */
    private final boolean noFrequentlyForwarded;

    /**
     * Holds the optional ephemeral-message expiration in seconds; non-null emits an {@code <ephemeral/>} child.
     */
    private final Integer ephemeralExpiration;

    /**
     * Holds the optional ephemeral-message trigger value, paired with {@link #ephemeralExpiration}.
     */
    private final Integer ephemeralTrigger;

    /**
     * Holds the optional membership-approval join-mode value; non-null emits a {@code <membership_approval_mode/>}
     * child.
     */
    private final String membershipApprovalGroupJoinMode;

    /**
     * Indicates whether to attach a {@code <breakout/>} child marking the new group as a breakout sub-group.
     */
    private final boolean breakout;

    /**
     * Indicates whether to attach a {@code <created_as_lid/>} child.
     */
    private final boolean createdAsLid;

    /**
     * Holds the optional {@code addressing_mode_override mode="..."} value.
     */
    private final String addressingModeOverrideMode;

    /**
     * Holds the optional parent-community {@link Jid}; non-null emits a {@code <linked_parent jid="..."/>} child.
     */
    private final Jid linkedParentJid;

    /**
     * Indicates whether to attach a {@code <hidden_group/>} child hiding the new group from the community
     * directory.
     */
    private final boolean hiddenGroup;

    /**
     * Indicates whether to attach an {@code <allow_non_admin_sub_group_creation/>} child.
     */
    private final boolean allowNonAdminSubGroupCreation;

    /**
     * Indicates whether to attach a {@code <create_general_chat/>} child.
     */
    private final boolean createGeneralChat;

    /**
     * Indicates whether to attach a {@code <capi/>} child.
     */
    private final boolean capi;

    /**
     * Holds the optional {@code dedup} attribute attached to the {@code <create/>} root.
     */
    private final String dedupAttr;

    /**
     * Holds the optional {@code member_add_mode} attribute attached to the {@code <create/>} root.
     */
    private final String memberAddMode;

    /**
     * Holds the optional {@code member_link_mode} attribute attached to the {@code <create/>} root.
     */
    private final String memberLinkMode;

    /**
     * Holds the optional {@code member_share_group_history_mode} attribute attached to the {@code <create/>} root.
     */
    private final String memberShareGroupHistoryMode;

    /**
     * Constructs a fully-parametrised create-group request.
     *
     * New call sites should prefer {@link #builder()}; this all-args constructor is intentionally
     * package-friendly in spirit so the {@link Builder#build()} path stays a single call. The supplied
     * participant list is defensively copied.
     *
     * @param subject                             the group subject
     * @param participants                        the seed participants
     * @param descriptionBody                     the optional description body
     * @param descriptionId                       the optional description id
     * @param locked                              see {@link #locked()}
     * @param announcement                        see {@link #announcement()}
     * @param parentDefaultMembershipApprovalMode see {@link #parentDefaultMembershipApprovalMode()}
     * @param noFrequentlyForwarded               see {@link #noFrequentlyForwarded()}
     * @param ephemeralExpiration                 the optional ephemeral expiration in seconds
     * @param ephemeralTrigger                    the optional ephemeral trigger value
     * @param membershipApprovalGroupJoinMode     the optional membership-approval join mode
     * @param breakout                            see {@link #breakout()}
     * @param createdAsLid                        see {@link #createdAsLid()}
     * @param addressingModeOverrideMode          the optional addressing-mode override value
     * @param linkedParentJid                     the optional parent community {@link Jid}
     * @param hiddenGroup                         see {@link #hiddenGroup()}
     * @param allowNonAdminSubGroupCreation       see {@link #allowNonAdminSubGroupCreation()}
     * @param createGeneralChat                   see {@link #createGeneralChat()}
     * @param capi                                see {@link #capi()}
     * @param dedupAttr                           the optional dedup token
     * @param memberAddMode                       the optional member-add-mode value
     * @param memberLinkMode                      the optional member-link-mode value
     * @param memberShareGroupHistoryMode         the optional member-share-history-mode value
     * @throws NullPointerException if {@code subject} or {@code participants} is {@code null}
     */
    public SmaxGroupsCreateRequest(String subject,
                   List<RequestParticipant> participants,
                   String descriptionBody,
                   String descriptionId,
                   boolean locked,
                   boolean announcement,
                   boolean parentDefaultMembershipApprovalMode,
                   boolean noFrequentlyForwarded,
                   Integer ephemeralExpiration,
                   Integer ephemeralTrigger,
                   String membershipApprovalGroupJoinMode,
                   boolean breakout,
                   boolean createdAsLid,
                   String addressingModeOverrideMode,
                   Jid linkedParentJid,
                   boolean hiddenGroup,
                   boolean allowNonAdminSubGroupCreation,
                   boolean createGeneralChat,
                   boolean capi,
                   String dedupAttr,
                   String memberAddMode,
                   String memberLinkMode,
                   String memberShareGroupHistoryMode) {
        Objects.requireNonNull(subject, "subject cannot be null");
        Objects.requireNonNull(participants, "participants cannot be null");
        this.subject = subject;
        this.participants = List.copyOf(participants);
        this.descriptionBody = descriptionBody;
        this.descriptionId = descriptionId;
        this.locked = locked;
        this.announcement = announcement;
        this.parentDefaultMembershipApprovalMode = parentDefaultMembershipApprovalMode;
        this.noFrequentlyForwarded = noFrequentlyForwarded;
        this.ephemeralExpiration = ephemeralExpiration;
        this.ephemeralTrigger = ephemeralTrigger;
        this.membershipApprovalGroupJoinMode = membershipApprovalGroupJoinMode;
        this.breakout = breakout;
        this.createdAsLid = createdAsLid;
        this.addressingModeOverrideMode = addressingModeOverrideMode;
        this.linkedParentJid = linkedParentJid;
        this.hiddenGroup = hiddenGroup;
        this.allowNonAdminSubGroupCreation = allowNonAdminSubGroupCreation;
        this.createGeneralChat = createGeneralChat;
        this.capi = capi;
        this.dedupAttr = dedupAttr;
        this.memberAddMode = memberAddMode;
        this.memberLinkMode = memberLinkMode;
        this.memberShareGroupHistoryMode = memberShareGroupHistoryMode;
    }

    /**
     * Returns a fresh {@link Builder} for fluent construction.
     *
     * This is the canonical entry point for new call sites; the all-args constructor is provided primarily for
     * the builder's {@link Builder#build()} method.
     *
     * @return a new {@link Builder}; never {@code null}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the group subject.
     *
     * @return the subject; never {@code null}
     */
    public String subject() {
        return subject;
    }

    /**
     * Returns the seed participants.
     *
     * <p>The list is empty for a community-parent create, which seeds no members; regular group creates carry
     * one or more entries.
     *
     * @return an unmodifiable list of {@link RequestParticipant}s; possibly empty
     */
    public List<RequestParticipant> participants() {
        return participants;
    }

    /**
     * Returns the optional description body.
     *
     * @return an {@link Optional} carrying the body, or empty when omitted
     */
    public Optional<String> descriptionBody() {
        return Optional.ofNullable(descriptionBody);
    }

    /**
     * Returns the optional description id.
     *
     * @return an {@link Optional} carrying the id, or empty when omitted
     */
    public Optional<String> descriptionId() {
        return Optional.ofNullable(descriptionId);
    }

    /**
     * Returns whether the {@code <locked/>} child is attached.
     *
     * When {@code true} the relay marks chat-info edits (subject, picture, description) as admin-only.
     *
     * @return {@code true} when the marker is emitted
     */
    public boolean locked() {
        return locked;
    }

    /**
     * Returns whether the {@code <announcement/>} child is attached.
     *
     * When {@code true} the relay restricts posting to admins; the UI renders this as an announcement group.
     *
     * @return {@code true} when the marker is emitted
     */
    public boolean announcement() {
        return announcement;
    }

    /**
     * Returns whether the {@code <parent default_membership_approval_mode="request_required"/>} marker is
     * attached.
     *
     * When {@code true} the new group becomes a community parent whose sub-groups inherit the approval-required
     * default; {@code request_required} is the only value emitted.
     *
     * @return {@code true} when the marker is emitted
     */
    public boolean parentDefaultMembershipApprovalMode() {
        return parentDefaultMembershipApprovalMode;
    }

    /**
     * Returns whether the {@code <no_frequently_forwarded/>} child is attached.
     *
     * @return {@code true} when the marker is emitted
     */
    public boolean noFrequentlyForwarded() {
        return noFrequentlyForwarded;
    }

    /**
     * Returns the optional ephemeral-message expiration.
     *
     * @return an {@link Optional} carrying the expiration in seconds, or empty when omitted
     */
    public Optional<Integer> ephemeralExpiration() {
        return Optional.ofNullable(ephemeralExpiration);
    }

    /**
     * Returns the optional ephemeral-message trigger value.
     *
     * @return an {@link Optional} carrying the trigger, or empty when omitted
     */
    public Optional<Integer> ephemeralTrigger() {
        return Optional.ofNullable(ephemeralTrigger);
    }

    /**
     * Returns the optional membership-approval join-mode value.
     *
     * @return an {@link Optional} carrying the value, or empty when omitted
     */
    public Optional<String> membershipApprovalGroupJoinMode() {
        return Optional.ofNullable(membershipApprovalGroupJoinMode);
    }

    /**
     * Returns whether the {@code <breakout/>} child is attached.
     *
     * When {@code true} the new group is materialised as a breakout sub-group inside an existing community; the
     * {@link #linkedParentJid()} must point at the community parent.
     *
     * @return {@code true} when the marker is emitted
     */
    public boolean breakout() {
        return breakout;
    }

    /**
     * Returns whether the {@code <created_as_lid/>} child is attached.
     *
     * @return {@code true} when the marker is emitted
     */
    public boolean createdAsLid() {
        return createdAsLid;
    }

    /**
     * Returns the optional addressing-mode-override value.
     *
     * @return an {@link Optional} carrying the value, or empty when omitted
     */
    public Optional<String> addressingModeOverrideMode() {
        return Optional.ofNullable(addressingModeOverrideMode);
    }

    /**
     * Returns the optional parent-community {@link Jid}.
     *
     * @return an {@link Optional} carrying the {@link Jid}, or empty when omitted
     */
    public Optional<Jid> linkedParentJid() {
        return Optional.ofNullable(linkedParentJid);
    }

    /**
     * Returns whether the {@code <hidden_group/>} child is attached.
     *
     * @return {@code true} when the marker is emitted
     */
    public boolean hiddenGroup() {
        return hiddenGroup;
    }

    /**
     * Returns whether the {@code <allow_non_admin_sub_group_creation/>} child is attached.
     *
     * @return {@code true} when the marker is emitted
     */
    public boolean allowNonAdminSubGroupCreation() {
        return allowNonAdminSubGroupCreation;
    }

    /**
     * Returns whether the {@code <create_general_chat/>} child is attached.
     *
     * @return {@code true} when the marker is emitted
     */
    public boolean createGeneralChat() {
        return createGeneralChat;
    }

    /**
     * Returns whether the {@code <capi/>} child is attached.
     *
     * @return {@code true} when the marker is emitted
     */
    public boolean capi() {
        return capi;
    }

    /**
     * Returns the optional dedup token.
     *
     * When supplied, the relay uses the {@code (creator, dedup)} tuple to suppress duplicate creations and answers
     * with {@link SmaxGroupsCreateResponse.GroupAlreadyExists} carrying the original group's {@link Jid}.
     *
     * @return an {@link Optional} carrying the token, or empty when omitted
     */
    public Optional<String> dedupAttr() {
        return Optional.ofNullable(dedupAttr);
    }

    /**
     * Returns the optional member-add-mode value.
     *
     * @return an {@link Optional} carrying the value, or empty when omitted
     */
    public Optional<String> memberAddMode() {
        return Optional.ofNullable(memberAddMode);
    }

    /**
     * Returns the optional member-link-mode value.
     *
     * @return an {@link Optional} carrying the value, or empty when omitted
     */
    public Optional<String> memberLinkMode() {
        return Optional.ofNullable(memberLinkMode);
    }

    /**
     * Returns the optional member-share-group-history-mode value.
     *
     * @return an {@link Optional} carrying the value, or empty when omitted
     */
    public Optional<String> memberShareGroupHistoryMode() {
        return Optional.ofNullable(memberShareGroupHistoryMode);
    }

    /**
     * Materialises the outbound IQ stanza ready for dispatch.
     *
     * The resulting envelope wraps a {@code <create subject="..." [dedup="..." ...]/>} root carrying zero or more
     * {@code <participant>} children plus every opt-in feature flag as a marker child. The reply is parsed by
     * {@link SmaxGroupsCreateResponse#of(Stanza, Stanza)}.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <create/>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsCreateRequest",
            exports = "makeCreateRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var createBuilder = new StanzaBuilder()
                .description("create")
                .attribute("subject", subject);
        if (dedupAttr != null) {
            createBuilder.attribute("dedup", dedupAttr);
        }
        if (memberAddMode != null) {
            createBuilder.attribute("member_add_mode", memberAddMode);
        }
        if (memberLinkMode != null) {
            createBuilder.attribute("member_link_mode", memberLinkMode);
        }
        if (memberShareGroupHistoryMode != null) {
            createBuilder.attribute("member_share_group_history_mode", memberShareGroupHistoryMode);
        }
        var children = new ArrayList<Stanza>();
        for (var participant : participants) {
            children.add(participant.toStanza());
        }
        if (descriptionBody != null || descriptionId != null) {
            var descriptionBuilder = new StanzaBuilder()
                    .description("description");
            if (descriptionId != null) {
                descriptionBuilder.attribute("id", descriptionId);
            }
            if (descriptionBody != null) {
                var bodyNode = new StanzaBuilder()
                        .description("body")
                        .content(descriptionBody.getBytes(StandardCharsets.UTF_8))
                        .build();
                descriptionBuilder.content(bodyNode);
            }
            children.add(descriptionBuilder.build());
        }
        if (locked) {
            children.add(new StanzaBuilder().description("locked").build());
        }
        if (announcement) {
            children.add(new StanzaBuilder().description("announcement").build());
        }
        if (parentDefaultMembershipApprovalMode) {
            var parentNode = new StanzaBuilder()
                    .description("parent")
                    .attribute("default_membership_approval_mode", "request_required")
                    .build();
            children.add(parentNode);
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
        if (membershipApprovalGroupJoinMode != null) {
            var membershipNode = new StanzaBuilder()
                    .description("membership_approval_mode")
                    .attribute("group_join_mode", membershipApprovalGroupJoinMode)
                    .build();
            children.add(membershipNode);
        }
        if (breakout) {
            children.add(new StanzaBuilder().description("breakout").build());
        }
        if (createdAsLid) {
            children.add(new StanzaBuilder().description("created_as_lid").build());
        }
        if (addressingModeOverrideMode != null) {
            var addressingNode = new StanzaBuilder()
                    .description("addressing_mode_override")
                    .attribute("mode", addressingModeOverrideMode)
                    .build();
            children.add(addressingNode);
        }
        if (linkedParentJid != null) {
            var linkedParentNode = new StanzaBuilder()
                    .description("linked_parent")
                    .attribute("jid", linkedParentJid)
                    .build();
            children.add(linkedParentNode);
        }
        if (hiddenGroup) {
            children.add(new StanzaBuilder().description("hidden_group").build());
        }
        if (allowNonAdminSubGroupCreation) {
            children.add(new StanzaBuilder().description("allow_non_admin_sub_group_creation").build());
        }
        if (createGeneralChat) {
            children.add(new StanzaBuilder().description("create_general_chat").build());
        }
        if (capi) {
            children.add(new StanzaBuilder().description("capi").build());
        }
        createBuilder.content(children);
        var createNode = createBuilder.build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", JidServer.groupOrCommunity())
                .attribute("type", "set")
                .content(createNode);
    }

    /**
     * Compares this request to {@code obj} for value equality across every field.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsCreateRequest} with identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGroupsCreateRequest) obj;
        return this.locked == that.locked
                && this.announcement == that.announcement
                && this.noFrequentlyForwarded == that.noFrequentlyForwarded
                && this.breakout == that.breakout
                && this.createdAsLid == that.createdAsLid
                && this.hiddenGroup == that.hiddenGroup
                && this.allowNonAdminSubGroupCreation == that.allowNonAdminSubGroupCreation
                && this.createGeneralChat == that.createGeneralChat
                && this.capi == that.capi
                && this.parentDefaultMembershipApprovalMode == that.parentDefaultMembershipApprovalMode
                && Objects.equals(this.subject, that.subject)
                && Objects.equals(this.participants, that.participants)
                && Objects.equals(this.descriptionBody, that.descriptionBody)
                && Objects.equals(this.descriptionId, that.descriptionId)
                && Objects.equals(this.ephemeralExpiration, that.ephemeralExpiration)
                && Objects.equals(this.ephemeralTrigger, that.ephemeralTrigger)
                && Objects.equals(this.membershipApprovalGroupJoinMode, that.membershipApprovalGroupJoinMode)
                && Objects.equals(this.addressingModeOverrideMode, that.addressingModeOverrideMode)
                && Objects.equals(this.linkedParentJid, that.linkedParentJid)
                && Objects.equals(this.dedupAttr, that.dedupAttr)
                && Objects.equals(this.memberAddMode, that.memberAddMode)
                && Objects.equals(this.memberLinkMode, that.memberLinkMode)
                && Objects.equals(this.memberShareGroupHistoryMode, that.memberShareGroupHistoryMode);
    }

    /**
     * Returns a hash composed of every field.
     *
     * @implNote This implementation splits the hash into two {@code Objects.hash} batches because the all-args
     * field list exceeds the practical width of a single varargs call; the {@code primary * 31 + secondary} mix
     * preserves permutation-sensitive distribution across the batches.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        var primary = Objects.hash(subject, participants, descriptionBody, descriptionId, locked, announcement,
                parentDefaultMembershipApprovalMode, noFrequentlyForwarded, ephemeralExpiration,
                ephemeralTrigger, membershipApprovalGroupJoinMode, breakout, createdAsLid,
                addressingModeOverrideMode, linkedParentJid);
        var secondary = Objects.hash(hiddenGroup, allowNonAdminSubGroupCreation, createGeneralChat, capi,
                dedupAttr, memberAddMode, memberLinkMode, memberShareGroupHistoryMode);
        return primary * 31 + secondary;
    }

    /**
     * Returns a debug string carrying every field.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsCreateRequest[subject=" + subject
                + ", participants=" + participants
                + ", descriptionBody=" + descriptionBody
                + ", descriptionId=" + descriptionId
                + ", locked=" + locked
                + ", announcement=" + announcement
                + ", parentDefaultMembershipApprovalMode=" + parentDefaultMembershipApprovalMode
                + ", noFrequentlyForwarded=" + noFrequentlyForwarded
                + ", ephemeralExpiration=" + ephemeralExpiration
                + ", ephemeralTrigger=" + ephemeralTrigger
                + ", membershipApprovalGroupJoinMode=" + membershipApprovalGroupJoinMode
                + ", breakout=" + breakout
                + ", createdAsLid=" + createdAsLid
                + ", addressingModeOverrideMode=" + addressingModeOverrideMode
                + ", linkedParentJid=" + linkedParentJid
                + ", hiddenGroup=" + hiddenGroup
                + ", allowNonAdminSubGroupCreation=" + allowNonAdminSubGroupCreation
                + ", createGeneralChat=" + createGeneralChat
                + ", capi=" + capi
                + ", dedupAttr=" + dedupAttr
                + ", memberAddMode=" + memberAddMode
                + ", memberLinkMode=" + memberLinkMode
                + ", memberShareGroupHistoryMode=" + memberShareGroupHistoryMode + ']';
    }

    /**
     * Single seed-participant entry inside the outbound {@code <create/>} payload.
     *
     * Carries the participant's primary {@link Jid} plus optional secondary identity attributes (phone-number JID,
     * username, permission token).
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutGroupsCreateRequest")
    @WhatsAppWebModule(moduleName = "WASmaxOutGroupsPermissionTokenMixin")
    public static final class RequestParticipant {
        /**
         * Holds the participant {@link Jid}.
         */
        private final Jid jid;

        /**
         * Holds the optional phone-number {@link Jid}.
         */
        private final Jid phoneNumber;

        /**
         * Holds the optional username.
         */
        private final String username;

        /**
         * Holds the optional permission token attached as the {@code permission_token} attribute.
         */
        private final String permissionToken;

        /**
         * Constructs a {@link RequestParticipant} entry.
         *
         * @param jid             the participant {@link Jid}
         * @param phoneNumber     the optional phone-number {@link Jid}; may be {@code null}
         * @param username        the optional username; may be {@code null}
         * @param permissionToken the optional permission token; may be {@code null}
         * @throws NullPointerException if {@code jid} is {@code null}
         */
        public RequestParticipant(Jid jid, Jid phoneNumber, String username, String permissionToken) {
            this.jid = Objects.requireNonNull(jid, "jid cannot be null");
            this.phoneNumber = phoneNumber;
            this.username = username;
            this.permissionToken = permissionToken;
        }

        /**
         * Returns the participant {@link Jid}.
         *
         * @return the {@link Jid}; never {@code null}
         */
        public Jid jid() {
            return jid;
        }

        /**
         * Returns the optional phone-number {@link Jid}.
         *
         * @return an {@link Optional} carrying the phone JID, or empty when omitted
         */
        public Optional<Jid> phoneNumber() {
            return Optional.ofNullable(phoneNumber);
        }

        /**
         * Returns the optional username.
         *
         * @return an {@link Optional} carrying the username, or empty when omitted
         */
        public Optional<String> username() {
            return Optional.ofNullable(username);
        }

        /**
         * Returns the optional permission token.
         *
         * @return an {@link Optional} carrying the token, or empty when omitted
         */
        public Optional<String> permissionToken() {
            return Optional.ofNullable(permissionToken);
        }

        /**
         * Materialises the {@code <participant/>} child {@link Stanza} for this entry.
         *
         * The optional {@code phone_number}, {@code username} and {@code permission_token} attributes are only
         * emitted when their backing field is present.
         *
         * @return the materialised {@link Stanza}
         */
        public Stanza toStanza() {
            var builder = new StanzaBuilder()
                    .description("participant")
                    .attribute("jid", jid);
            if (phoneNumber != null) {
                builder.attribute("phone_number", phoneNumber);
            }
            if (username != null) {
                builder.attribute("username", username);
            }
            if (permissionToken != null) {
                builder.attribute("permission_token", permissionToken);
            }
            return builder.build();
        }

        /**
         * Compares this participant to {@code obj} for value equality across every field.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link RequestParticipant} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (RequestParticipant) obj;
            return Objects.equals(this.jid, that.jid)
                    && Objects.equals(this.phoneNumber, that.phoneNumber)
                    && Objects.equals(this.username, that.username)
                    && Objects.equals(this.permissionToken, that.permissionToken);
        }

        /**
         * Returns a hash composed of every field.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(jid, phoneNumber, username, permissionToken);
        }

        /**
         * Returns a debug string carrying every field.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsCreateRequest.RequestParticipant[jid=" + jid
                    + ", phoneNumber=" + phoneNumber
                    + ", username=" + username
                    + ", permissionToken=" + permissionToken + ']';
        }
    }

    /**
     * Fluent builder for {@link SmaxGroupsCreateRequest}.
     *
     * The only mandatory input is {@link #subject(String)}; participants (via
     * {@link #addParticipant(RequestParticipant)} or {@link #addParticipants(List)}) and every other setter are
     * optional, as a community-parent create seeds no members. {@link #build()} validates the subject.
     */
    public static final class Builder {
        /**
         * Accumulates the subject.
         */
        private String subject;

        /**
         * Accumulates the participants list.
         */
        private final List<RequestParticipant> participants = new ArrayList<>();

        /**
         * Accumulates the optional description body.
         */
        private String descriptionBody;

        /**
         * Accumulates the optional description id.
         */
        private String descriptionId;

        /**
         * Accumulates the locked toggle.
         */
        private boolean locked;

        /**
         * Accumulates the announcement toggle.
         */
        private boolean announcement;

        /**
         * Accumulates the parent-default membership-approval toggle.
         */
        private boolean parentDefaultMembershipApprovalMode;

        /**
         * Accumulates the no-frequently-forwarded toggle.
         */
        private boolean noFrequentlyForwarded;

        /**
         * Accumulates the optional ephemeral expiration in seconds.
         */
        private Integer ephemeralExpiration;

        /**
         * Accumulates the optional ephemeral trigger value.
         */
        private Integer ephemeralTrigger;

        /**
         * Accumulates the optional membership-approval join-mode value.
         */
        private String membershipApprovalGroupJoinMode;

        /**
         * Accumulates the breakout toggle.
         */
        private boolean breakout;

        /**
         * Accumulates the created-as-lid toggle.
         */
        private boolean createdAsLid;

        /**
         * Accumulates the optional addressing-mode-override value.
         */
        private String addressingModeOverrideMode;

        /**
         * Accumulates the optional linked parent community {@link Jid}.
         */
        private Jid linkedParentJid;

        /**
         * Accumulates the hidden-group toggle.
         */
        private boolean hiddenGroup;

        /**
         * Accumulates the allow-non-admin-sub-group-creation toggle.
         */
        private boolean allowNonAdminSubGroupCreation;

        /**
         * Accumulates the create-general-chat toggle.
         */
        private boolean createGeneralChat;

        /**
         * Accumulates the capi toggle.
         */
        private boolean capi;

        /**
         * Accumulates the optional dedup token.
         */
        private String dedupAttr;

        /**
         * Accumulates the optional member-add-mode value.
         */
        private String memberAddMode;

        /**
         * Accumulates the optional member-link-mode value.
         */
        private String memberLinkMode;

        /**
         * Accumulates the optional member-share-group-history-mode value.
         */
        private String memberShareGroupHistoryMode;

        /**
         * Constructs a fresh builder.
         *
         * New call sites should prefer {@link SmaxGroupsCreateRequest#builder()} as the canonical entry point.
         */
        public Builder() {
        }

        /**
         * Sets the group subject.
         *
         * @param subject the subject text
         * @return this {@link Builder}
         * @throws NullPointerException if {@code subject} is {@code null}
         */
        public Builder subject(String subject) {
            this.subject = Objects.requireNonNull(subject, "subject cannot be null");
            return this;
        }

        /**
         * Appends a single participant.
         *
         * @param participant the participant
         * @return this {@link Builder}
         * @throws NullPointerException if {@code participant} is {@code null}
         */
        public Builder addParticipant(RequestParticipant participant) {
            Objects.requireNonNull(participant, "participant cannot be null");
            this.participants.add(participant);
            return this;
        }

        /**
         * Appends every participant from the supplied collection.
         *
         * @param entries the participants to append
         * @return this {@link Builder}
         * @throws NullPointerException if {@code entries} or any entry is {@code null}
         */
        public Builder addParticipants(List<RequestParticipant> entries) {
            Objects.requireNonNull(entries, "entries cannot be null");
            for (var entry : entries) {
                addParticipant(entry);
            }
            return this;
        }

        /**
         * Sets the optional description body.
         *
         * @param descriptionBody the body text; may be {@code null}
         * @return this {@link Builder}
         */
        public Builder descriptionBody(String descriptionBody) {
            this.descriptionBody = descriptionBody;
            return this;
        }

        /**
         * Sets the optional description id attribute.
         *
         * @param descriptionId the description id; may be {@code null}
         * @return this {@link Builder}
         */
        public Builder descriptionId(String descriptionId) {
            this.descriptionId = descriptionId;
            return this;
        }

        /**
         * Sets the locked toggle.
         *
         * @param locked the desired flag value
         * @return this {@link Builder}
         */
        public Builder locked(boolean locked) {
            this.locked = locked;
            return this;
        }

        /**
         * Sets the announcement toggle.
         *
         * @param announcement the desired flag value
         * @return this {@link Builder}
         */
        public Builder announcement(boolean announcement) {
            this.announcement = announcement;
            return this;
        }

        /**
         * Sets the parent-default membership-approval toggle.
         *
         * @param flag the desired flag value
         * @return this {@link Builder}
         */
        public Builder parentDefaultMembershipApprovalMode(boolean flag) {
            this.parentDefaultMembershipApprovalMode = flag;
            return this;
        }

        /**
         * Sets the no-frequently-forwarded toggle.
         *
         * @param flag the desired flag value
         * @return this {@link Builder}
         */
        public Builder noFrequentlyForwarded(boolean flag) {
            this.noFrequentlyForwarded = flag;
            return this;
        }

        /**
         * Sets the optional ephemeral expiration in seconds.
         *
         * @param expiration the expiration value; may be {@code null} to omit the {@code <ephemeral/>} child
         * @return this {@link Builder}
         */
        public Builder ephemeralExpiration(Integer expiration) {
            this.ephemeralExpiration = expiration;
            return this;
        }

        /**
         * Sets the optional ephemeral trigger value.
         *
         * @param trigger the trigger value; may be {@code null}
         * @return this {@link Builder}
         */
        public Builder ephemeralTrigger(Integer trigger) {
            this.ephemeralTrigger = trigger;
            return this;
        }

        /**
         * Sets the optional membership-approval join-mode value.
         *
         * @param mode the mode value; may be {@code null}
         * @return this {@link Builder}
         */
        public Builder membershipApprovalGroupJoinMode(String mode) {
            this.membershipApprovalGroupJoinMode = mode;
            return this;
        }

        /**
         * Sets the breakout toggle.
         *
         * @param flag the desired flag value
         * @return this {@link Builder}
         */
        public Builder breakout(boolean flag) {
            this.breakout = flag;
            return this;
        }

        /**
         * Sets the created-as-lid toggle.
         *
         * @param flag the desired flag value
         * @return this {@link Builder}
         */
        public Builder createdAsLid(boolean flag) {
            this.createdAsLid = flag;
            return this;
        }

        /**
         * Sets the optional addressing-mode-override value.
         *
         * @param mode the mode value; may be {@code null}
         * @return this {@link Builder}
         */
        public Builder addressingModeOverrideMode(String mode) {
            this.addressingModeOverrideMode = mode;
            return this;
        }

        /**
         * Sets the optional parent community {@link Jid}.
         *
         * @param jid the parent {@link Jid}; may be {@code null}
         * @return this {@link Builder}
         */
        public Builder linkedParentJid(Jid jid) {
            this.linkedParentJid = jid;
            return this;
        }

        /**
         * Sets the hidden-group toggle.
         *
         * @param flag the desired flag value
         * @return this {@link Builder}
         */
        public Builder hiddenGroup(boolean flag) {
            this.hiddenGroup = flag;
            return this;
        }

        /**
         * Sets the allow-non-admin-sub-group-creation toggle.
         *
         * @param flag the desired flag value
         * @return this {@link Builder}
         */
        public Builder allowNonAdminSubGroupCreation(boolean flag) {
            this.allowNonAdminSubGroupCreation = flag;
            return this;
        }

        /**
         * Sets the create-general-chat toggle.
         *
         * @param flag the desired flag value
         * @return this {@link Builder}
         */
        public Builder createGeneralChat(boolean flag) {
            this.createGeneralChat = flag;
            return this;
        }

        /**
         * Sets the capi toggle.
         *
         * @param flag the desired flag value
         * @return this {@link Builder}
         */
        public Builder capi(boolean flag) {
            this.capi = flag;
            return this;
        }

        /**
         * Sets the optional dedup token.
         *
         * @param dedupAttr the dedup token; may be {@code null}
         * @return this {@link Builder}
         */
        public Builder dedupAttr(String dedupAttr) {
            this.dedupAttr = dedupAttr;
            return this;
        }

        /**
         * Sets the optional member-add-mode value.
         *
         * @param mode the mode value; may be {@code null}
         * @return this {@link Builder}
         */
        public Builder memberAddMode(String mode) {
            this.memberAddMode = mode;
            return this;
        }

        /**
         * Sets the optional member-link-mode value.
         *
         * @param mode the mode value; may be {@code null}
         * @return this {@link Builder}
         */
        public Builder memberLinkMode(String mode) {
            this.memberLinkMode = mode;
            return this;
        }

        /**
         * Sets the optional member-share-group-history-mode value.
         *
         * @param mode the mode value; may be {@code null}
         * @return this {@link Builder}
         */
        public Builder memberShareGroupHistoryMode(String mode) {
            this.memberShareGroupHistoryMode = mode;
            return this;
        }

        /**
         * Materialises a {@link SmaxGroupsCreateRequest} from the accumulated state.
         *
         * @return the constructed request; never {@code null}
         * @throws NullPointerException if {@link #subject(String)} was never called
         */
        public SmaxGroupsCreateRequest build() {
            Objects.requireNonNull(subject, "subject must be set before build()");
            return new SmaxGroupsCreateRequest(subject, participants, descriptionBody, descriptionId, locked, announcement,
                    parentDefaultMembershipApprovalMode, noFrequentlyForwarded, ephemeralExpiration,
                    ephemeralTrigger, membershipApprovalGroupJoinMode, breakout, createdAsLid,
                    addressingModeOverrideMode, linkedParentJid, hiddenGroup, allowNonAdminSubGroupCreation,
                    createGeneralChat, capi, dedupAttr, memberAddMode, memberLinkMode,
                    memberShareGroupHistoryMode);
        }
    }
}
