package com.github.auties00.cobalt.stream.notification.group;

import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.groups.SmaxGroupsGroupsDirtyNotificationResponse;
import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.github.auties00.cobalt.ack.AckClass;
import com.github.auties00.cobalt.ack.AckSender;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.chat.Chat;
import com.github.auties00.cobalt.wire.linked.chat.ChatEphemeralTimer;
import com.github.auties00.cobalt.wire.linked.chat.ChatMetadata;
import com.github.auties00.cobalt.wire.linked.chat.ChatPolicy;
import com.github.auties00.cobalt.wire.linked.chat.community.CommunityMetadata;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadata;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupParticipant;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupParticipantBuilder;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupPartipantRole;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stream.NodeStreamService;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.GroupJoinCEventBuilder;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Handles {@code type="w:gp2"} group and community notifications.
 *
 * <p>Each notification targets exactly one group, named by the {@code from}
 * attribute, and carries one or more action children ({@code create},
 * {@code add}, {@code remove}, {@code promote}, {@code demote},
 * {@code subject}, {@code description}, {@code ephemeral}, {@code locked},
 * {@code announcement}, link/unlink, membership approval, and others). For
 * each notification this handler resolves or creates the target chat, applies
 * every action child inline against the local {@link Chat} and
 * {@link ChatMetadata}, then fires a full metadata refresh for every group
 * JID the notification references (including linked groups and subgroup
 * suggestions). A {@code groups_dirty} child short-circuits to a metadata
 * refresh of every group named in its {@code <group jid>} list and skips
 * inline mutation.
 *
 * @implNote
 * This implementation follows a two-phase approach: every action child is
 * applied inline against the local chat and metadata, then a full metadata
 * refresh is fired for every group JID referenced by the notification.
 * WA Web routes each action through
 * {@link Stanza} handlers that additionally
 * synthesise system messages in the chat thread; Cobalt relies on the
 * post-loop refresh to reach the same final state and lets the chat-message
 * stream produce its own system messages.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleGroupNotification")
public final class NotificationGroupStreamHandler extends SocketStreamHandler.Concurrent {

    /**
     * The logger for {@link NotificationGroupStreamHandler}.
     */
    private static final System.Logger LOGGER = Log.get(NotificationGroupStreamHandler.class);

    /**
     * Provides store reads and group metadata queries.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * Commits the {@code GroupJoinC} event when the user is added to a group
     * they did not create.
     */
    private final WamService wamService;

    /**
     * Ships the post-processing {@code <ack class="notification" type="w:gp2"/>}
     * stanza, echoing the original {@code participant} attribute back to the
     * server.
     */
    private final AckSender ackSender;

    /**
     * Constructs the handler with its shared dependencies.
     *
     * <p>Invoked once during {@link NodeStreamService} setup; the dependencies are
     * retained as fields for the lifetime of the handler.
     *
     * @param whatsapp   the client providing store and metadata access
     * @param wamService the service that commits the {@code GroupJoinC} event
     * @param ackSender  the sender used to acknowledge processed notifications
     */
    public NotificationGroupStreamHandler(LinkedWhatsAppClient whatsapp, WamService wamService, AckSender ackSender) {
        this.whatsapp = whatsapp;
        this.wamService = wamService;
        this.ackSender = ackSender;
    }

    /**
     * Validates the stanza shape, processes the notification, and always sends
     * the protocol-level acknowledgement.
     *
     * <p>Stanzas whose description is not {@code notification} or whose type is
     * not {@code w:gp2} are dropped without further work. For matching stanzas
     * the notification is processed by {@link #handleNotification(Stanza)};
     * any throwable raised during processing is logged and swallowed, and the
     * acknowledgement is sent in all cases.
     *
     * @param stanza the incoming stanza
     */
    @Override
    public void handle(Stanza stanza) {
        if (!stanza.hasDescription("notification") || !stanza.hasAttribute("type", "w:gp2")) {
            return;
        }

        try {
            handleNotification(stanza);
        } catch (Throwable throwable) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING,
                        "cannot handle w:gp2 notification " + stanza.getAttributeAsString("id", "<missing>"),
                        throwable);
            }
        } finally {
            sendNotificationAck(stanza);
        }
    }

    /**
     * Resolves or creates the target chat, applies every action child inline,
     * then refreshes metadata for every related group JID.
     *
     * <p>Processing aborts when the {@code from} attribute is absent or does
     * not name a group or community. A {@code groups_dirty} child
     * short-circuits to a metadata refresh of every group named in its
     * {@code <group jid>} list (falling back to the envelope group when the
     * list is empty) and skips the action loop. Otherwise the chat is resolved
     * (creating a new one when absent), the action children are applied in
     * order, and every group JID accumulated across the actions is refreshed.
     *
     * @implNote
     * This implementation bumps the chat's {@code conversationTimestamp} and
     * {@code lastMsgTimestamp} from the stanza's top-level {@code t} attribute
     * when present, matching the WA Web side effect for actions that
     * synthesise system messages.
     *
     * @param stanza the {@code <notification>} stanza
     */
    private void handleNotification(Stanza stanza) {
        var groupJid = stanza.getAttributeAsJid("from").orElse(null);
        if (groupJid == null || !groupJid.hasGroupOrCommunityServer()) {
            return;
        }

        if (stanza.hasChild("groups_dirty")) {
            var dirtyGroups = SmaxGroupsGroupsDirtyNotificationResponse.of(stanza)
                    .map(SmaxGroupsGroupsDirtyNotificationResponse::dirtyGroups)
                    .filter(groups -> !groups.isEmpty())
                    .orElseGet(() -> List.of(groupJid));
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "w:gp2 groups_dirty notification, refreshing {0} group(s)", dirtyGroups.size());
            for (var dirtyGroup : dirtyGroups) {
                refreshGroup(dirtyGroup);
            }
            return;
        }

        var chat = whatsapp.store().chatStore().findChatByJid(groupJid)
                .orElseGet(() -> whatsapp.store().chatStore().addNewChat(groupJid));
        var notificationTimestamp = resolveInstant(stanza, "t");
        if (notificationTimestamp != null) {
            chat.setConversationTimestamp(notificationTimestamp);
            chat.setLastMsgTimestamp(notificationTimestamp);
        }
        var relatedGroups = new LinkedHashSet<Jid>();
        relatedGroups.add(groupJid);

        for (var action : stanza.children()) {
            handleAction(stanza, chat, groupJid, action, relatedGroups);
        }

        for (var relatedGroup : relatedGroups) {
            refreshGroup(relatedGroup);
        }
    }

    /**
     * Selects the per-action branch based on the action stanza's description.
     *
     * <p>The action's group and community JID references are first collected
     * into {@code relatedGroups}, then the action is dispatched on its
     * description to the matching mutation. Actions that WA Web handles by
     * synthesising system messages ({@code invite}, {@code revoke}, membership
     * requests, subgroup suggestions, {@code change_number}, missing
     * participant) are not mutated inline here; they are logged and reconciled
     * by the post-loop metadata refresh because Cobalt's message generation
     * runs from a different stream. Unrecognised descriptions are logged and
     * ignored.
     *
     * @param notification  the parent {@code <notification>} stanza
     * @param chat          the local chat for the group
     * @param groupJid      the group JID
     * @param action        the action child being applied
     * @param relatedGroups the accumulator of related group JIDs to refresh post-loop
     */
    private void handleAction(Stanza notification, Chat chat, Jid groupJid, Stanza action, LinkedHashSet<Jid> relatedGroups) {
        collectRelatedGroups(action, relatedGroups);

        switch (action.description()) {
            case "create" -> applyCreate(notification, chat, groupJid, action);
            case "add" -> applyParticipants(groupJid, action, GroupParticipantMutation.ADD);
            case "remove" -> applyParticipants(groupJid, action, GroupParticipantMutation.REMOVE);
            case "promote", "linked_group_promote" -> applyParticipants(groupJid, action, GroupParticipantMutation.PROMOTE);
            case "demote", "linked_group_demote" -> applyParticipants(groupJid, action, GroupParticipantMutation.DEMOTE);
            case "modify" -> applyParticipants(groupJid, action, GroupParticipantMutation.MODIFY);
            case "subject" -> applySubject(notification, chat, groupJid, action);
            case "description" -> applyDescription(notification, chat, groupJid, action);
            case "locked" -> applyRestrict(groupJid, true);
            case "unlocked" -> applyRestrict(groupJid, false);
            case "announcement" -> applyAnnounce(groupJid, true);
            case "not_announcement" -> applyAnnounce(groupJid, false);
            case "no_frequently_forwarded" -> applyNoFrequentlyForwarded(groupJid, true);
            case "frequently_forwarded_ok" -> applyNoFrequentlyForwarded(groupJid, false);
            case "ephemeral" -> applyEphemeral(notification, chat, groupJid, action);
            case "not_ephemeral" -> clearEphemeral(chat, groupJid);
            case "growth_locked" -> applyGrowthLock(groupJid, action);
            case "growth_unlocked" -> clearGrowthLock(groupJid);
            case "link" -> applyLink(groupJid, action);
            case "unlink" -> applyUnlink(groupJid, action);
            case "membership_approval_mode" -> applyMembershipApproval(action, groupJid);
            case "allow_admin_reports" -> applyReportToAdmin(groupJid, true);
            case "not_allow_admin_reports" -> applyReportToAdmin(groupJid, false);
            case "allow_non_admin_sub_group_creation" -> applyNonAdminSubgroupCreation(groupJid, true);
            case "not_allow_non_admin_sub_group_creation" -> applyNonAdminSubgroupCreation(groupJid, false);
            case "member_add_mode" -> applyMemberAddMode(groupJid, action);
            case "auto_add_disabled" -> applyGeneralChatAutoAddDisabled(groupJid, true);
            case "group_safety_check" -> applyGroupSafetyCheck(groupJid, true);
            case "suspended" -> applySuspended(chat, groupJid, true);
            case "unsuspended" -> applySuspended(chat, groupJid, false);
            case "delete" -> {
                whatsapp.store().chatStore().removeChatMetadata(groupJid);
                chat.setTerminated(true);
            }
            case "invite",
                 "revoke",
                 "membership_approval_request",
                 "reports",
                 "created_membership_requests",
                 "revoked_membership_requests",
                 "created_sub_group_suggestion",
                 "revoked_sub_group_suggestions",
                 "change_number",
                 "missing_participant_identification"
                 -> {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG,
                            "handling w:gp2 action {0} conservatively via metadata refresh",
                            action.description());
                }
            }
            default -> {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ignoring unsupported w:gp2 action {0}", action.description());
            }
        }
    }

    /**
     * Applies a {@code <create>} action by extracting every field from the
     * nested {@code <group>} element and writing it to both the chat and the
     * metadata.
     *
     * <p>The full group record is rebuilt from the {@code <group>} child:
     * subject, creation time, creator, description, support and subgroup
     * flags, archive and suspension state, ephemeral configuration, and the
     * initial participant list. The {@code GroupJoinC} WAM event is committed
     * when the local account did not author the create, that is, when the user
     * was added to a group they did not create. Community-only fields
     * ({@code allow_non_admin_sub_group_creation},
     * {@code default_membership_approval_mode}) are applied only on the
     * community variant.
     *
     * @implNote
     * This implementation handles both {@link GroupMetadata} and
     * {@link CommunityMetadata} variants because Cobalt models groups and
     * communities as distinct metadata types whereas WA Web shares one schema
     * with discriminator fields.
     *
     * @param notification the parent {@code <notification>} stanza
     * @param chat         the local chat for the created group
     * @param groupJid     the group JID
     * @param action       the {@code <create>} action stanza
     */
    private void applyCreate(Stanza notification, Chat chat, Jid groupJid, Stanza action) {
        var groupNode = action.getChild("group").orElse(action);
        var notificationAuthor = notification.getAttributeAsJid("participant").orElse(null);
        var mePnUser = whatsapp.store().accountStore().jid().orElse(null);
        if (notificationAuthor == null
                || mePnUser == null
                || !notificationAuthor.toUserJid().equals(mePnUser.toUserJid())) {
            wamService.commit(new GroupJoinCEventBuilder().build());
        }

        var subject = groupNode.getAttributeAsString("subject", null);
        if (subject != null) {
            chat.setName(subject);
        }

        var creation = groupNode.getAttributeAsLong("creation", (Long) null);
        chat.setCreatedAt(creation == null ? null : Instant.ofEpochSecond(creation));
        chat.setCreatedBy(groupNode.getAttributeAsString("creator", null));
        chat.setDescription(resolveCreateDescriptionBody(groupNode));
        chat.setSupport(groupNode.hasChild("support"));
        chat.setDefaultSubgroup(groupNode.hasChild("default_sub_group"));
        chat.setArchived(false);
        chat.setSuspended(groupNode.hasChild("suspended"));
        chat.setTerminated(false);

        applyEphemeral(notification, chat, groupJid, groupNode);

        var metadata = currentMetadata(groupJid);
        if (metadata != null) {
            metadata.clearParticipants();
            metadata.addAllParticipants(parseParticipants(groupNode, GroupParticipantMutation.ADD));

            setSubject(metadata, subject);

            var createDescBody = resolveCreateDescriptionBody(groupNode);
            var createDescId = resolveCreateDescriptionId(groupNode);
            setDescription(metadata, createDescBody, createDescId,
                    resolveInstant(groupNode, "t"),
                    groupNode.getAttributeAsJid("participant").orElse(null));

            applyRestrict(metadata, groupNode.hasChild("locked"));
            applyAnnounce(metadata, groupNode.hasChild("announcement"));
            applyNoFrequentlyForwarded(metadata, groupNode.hasChild("no_frequently_forwarded"));

            var approvalState = groupNode.getChild("membership_approval_mode")
                    .flatMap(mam -> mam.getChild("group_join"))
                    .flatMap(gj -> gj.getAttributeAsString("state"))
                    .orElse(null);
            applyMembershipApproval(metadata, "on".equals(approvalState));

            var memberAddModeStr = groupNode.getChild("member_add_mode")
                    .flatMap(Stanza::toContentString)
                    .orElse(null);
            applyMemberAddMode(metadata, "admin_add".equals(memberAddModeStr));

            var memberLinkModeContent = groupNode.getChild("member_link_mode")
                    .flatMap(Stanza::toContentString)
                    .orElse(null);
            applyMemberLinkMode(metadata, memberLinkModeContent);

            applyLimitSharingEnabled(metadata, groupNode.hasChild("limit_sharing_enabled"));

            applyGeneralChatAutoAddDisabled(metadata, action.hasChild("auto_add_disabled"));

            if (metadata instanceof GroupMetadata groupMetadata) {
                groupMetadata.setSupport(groupNode.hasChild("support"));
                groupMetadata.setDefaultSubgroup(groupNode.hasChild("default_sub_group"));
                groupMetadata.setGeneralSubgroup(groupNode.hasChild("general_chat"));
                groupMetadata.setHiddenSubgroup(groupNode.hasChild("hidden_group"));
                groupMetadata.setGroupSafetyCheck(groupNode.hasChild("group_safety_check"));
                groupMetadata.setHasCapi(groupNode.hasChild("capi"));
                var size = groupNode.getAttributeAsInt("size", (Integer) null);
                if (size != null) {
                    groupMetadata.setSize(size);
                }

                groupNode.getChild("linked_parent")
                        .flatMap(lp -> lp.getAttributeAsJid("jid"))
                        .ifPresent(groupMetadata::setParentCommunityJid);

                var groupAdder = notification.getAttributeAsJid("participant").orElse(null);
                if (groupAdder != null) {
                    groupMetadata.setGroupAdder(groupAdder.toUserJid());
                }
            } else if (metadata instanceof CommunityMetadata communityMetadata) {
                communityMetadata.setSupport(groupNode.hasChild("support"));
                communityMetadata.setDefaultSubgroup(groupNode.hasChild("default_sub_group"));
                communityMetadata.setGeneralSubgroup(groupNode.hasChild("general_chat"));
                communityMetadata.setHiddenSubgroup(groupNode.hasChild("hidden_group"));
                communityMetadata.setGroupSafetyCheck(groupNode.hasChild("group_safety_check"));
                communityMetadata.setHasCapi(groupNode.hasChild("capi"));
                var size = groupNode.getAttributeAsInt("size", (Integer) null);
                if (size != null) {
                    communityMetadata.setSize(size);
                }

                communityMetadata.setAllowNonAdminSubGroupCreation(
                        groupNode.hasChild("allow_non_admin_sub_group_creation"));

                var parentClosed = groupNode.getChild("parent")
                        .flatMap(parent -> parent.getAttributeAsString("default_membership_approval_mode"))
                        .map("request_required"::equals)
                        .orElse(false);
                communityMetadata.setParentGroupClosed(parentClosed);
            }
        }
    }

    /**
     * Applies a {@code member_link_mode} content string to the metadata.
     *
     * <p>Maps {@code "admin_link"} to an admins-only link policy and
     * {@code "all_member_link"} to an any-member link policy, choosing the
     * field appropriate to the {@link GroupMetadata} or {@link CommunityMetadata}
     * variant. A {@code null} content and any unrecognised value leave the
     * field unchanged.
     *
     * @param metadata the metadata being updated
     * @param content  the {@code <member_link_mode>} text content, or {@code null}
     */
    private void applyMemberLinkMode(ChatMetadata metadata, String content) {
        if (content == null) {
            return;
        }
        if (metadata instanceof GroupMetadata groupMetadata) {
            if ("admin_link".equals(content)) {
                groupMetadata.setMemberLinkMode(ChatPolicy.ADMINS);
            } else if ("all_member_link".equals(content)) {
                groupMetadata.setMemberLinkMode(ChatPolicy.ANYONE);
            }
        } else if (metadata instanceof CommunityMetadata communityMetadata) {
            if ("admin_link".equals(content)) {
                communityMetadata.setMemberLinkModeAdminOnly(true);
            } else if ("all_member_link".equals(content)) {
                communityMetadata.setMemberLinkModeAdminOnly(false);
            }
        }
    }

    /**
     * Writes the {@code limit_sharing_enabled} flag onto the metadata.
     *
     * <p>Sets whether link and media sharing is restricted to admins on either
     * the {@link GroupMetadata} or {@link CommunityMetadata} variant.
     *
     * @implNote
     * This implementation does not gate the read on the Opus AB-prop that WA
     * Web consults because Cobalt has no equivalent client-side kill switch.
     *
     * @param metadata the metadata being updated
     * @param value    whether link and media sharing is restricted to admins
     */
    private void applyLimitSharingEnabled(ChatMetadata metadata, boolean value) {
        if (metadata instanceof GroupMetadata groupMetadata) {
            groupMetadata.setLimitSharingEnabled(value);
        } else if (metadata instanceof CommunityMetadata communityMetadata) {
            communityMetadata.setLimitSharingEnabled(value);
        }
    }

    /**
     * Writes the {@code generalChatAutoAddDisabled} flag directly onto the
     * metadata.
     *
     * <p>Shared between the top-level {@code auto_add_disabled} action and the
     * create flow, which sources the same value from the notification rather
     * than the nested {@code <group>} element.
     *
     * @param metadata the metadata being updated
     * @param value    whether auto-add to the general chat is disabled
     */
    private void applyGeneralChatAutoAddDisabled(ChatMetadata metadata, boolean value) {
        if (metadata instanceof GroupMetadata groupMetadata) {
            groupMetadata.setGeneralChatAutoAddDisabled(value);
        } else if (metadata instanceof CommunityMetadata communityMetadata) {
            communityMetadata.setGeneralChatAutoAddDisabled(value);
        }
    }

    /**
     * Applies a {@code <subject>} action by writing the new name to both the
     * chat and the metadata along with the change timestamp and author.
     *
     * <p>Does nothing when the {@code subject} attribute is absent. The change
     * time is read from {@code s_t} (falling back to {@code t}) and the author
     * from {@code s_o}, falling back to the notification's {@code participant}
     * attribute when {@code s_o} is absent.
     *
     * @param notification the parent {@code <notification>} stanza
     * @param chat         the local chat
     * @param groupJid     the group JID
     * @param action       the {@code <subject>} action stanza
     */
    private void applySubject(Stanza notification, Chat chat, Jid groupJid, Stanza action) {
        var subject = action.getAttributeAsString("subject", null);
        if (subject == null) {
            return;
        }

        chat.setName(subject);
        var metadata = currentMetadata(groupJid);
        if (metadata != null) {
            setSubject(metadata, subject);
            setSubjectTimestamp(metadata, resolveInstant(action, "s_t", "t"), action.getAttributeAsJid("s_o").orElse(notification.getAttributeAsJid("participant").orElse(null)));
        }
    }

    /**
     * Applies a {@code <description>} action by writing the new body and
     * identifier to both the chat and the metadata.
     *
     * <p>A nested {@code <delete/>} clears the body while preserving the id.
     * The change time is read from the action's {@code t} attribute, falling
     * back to the notification's {@code t}. The author is taken from the
     * action's {@code participant} attribute first, then the notification's
     * {@code participant} attribute.
     *
     * @param notification the parent {@code <notification>} stanza
     * @param chat         the local chat
     * @param groupJid     the group JID
     * @param action       the {@code <description>} action stanza
     */
    private void applyDescription(Stanza notification, Chat chat, Jid groupJid, Stanza action) {
        var deleted = action.hasChild("delete");
        var description = deleted ? null : resolveDescriptionBody(action);
        var descriptionId = resolveDescriptionId(action);
        var timestamp = resolveInstant(action, "t");
        if (timestamp == null) {
            timestamp = resolveInstant(notification, "t");
        }
        var author = action.getAttributeAsJid("participant").orElse(notification.getAttributeAsJid("participant").orElse(null));

        chat.setDescription(description);
        var metadata = currentMetadata(groupJid);
        if (metadata != null) {
            setDescription(metadata, description, descriptionId, timestamp, author);
        }
    }

    /**
     * Applies the {@code <locked>} or {@code <unlocked>} toggle to the
     * metadata, driving whether editing group info is restricted to admins.
     *
     * <p>Resolves the current metadata and, when present, delegates to
     * {@link #applyRestrict(ChatMetadata, boolean)}.
     *
     * @param groupJid   the group JID
     * @param restricted {@code true} for {@code <locked>}, {@code false} for {@code <unlocked>}
     */
    private void applyRestrict(Jid groupJid, boolean restricted) {
        var metadata = currentMetadata(groupJid);
        if (metadata != null) {
            applyRestrict(metadata, restricted);
        }
    }

    /**
     * Applies the {@code restrict} value directly to a metadata instance.
     *
     * <p>Shared by the per-JID {@link #applyRestrict(Jid, boolean)} entry point
     * and the create flow, which reads the value from the nested
     * {@code <group locked=.../>} child. Maps the flag through
     * {@link ChatPolicy#of(boolean)} for the {@link GroupMetadata} variant and
     * writes the raw boolean for {@link CommunityMetadata}.
     *
     * @param metadata   the metadata being updated
     * @param restricted whether metadata editing is restricted to admins
     */
    private void applyRestrict(ChatMetadata metadata, boolean restricted) {
        if (metadata instanceof GroupMetadata groupMetadata) {
            groupMetadata.setRestrict(ChatPolicy.of(restricted));
        } else if (metadata instanceof CommunityMetadata communityMetadata) {
            communityMetadata.setRestrict(restricted);
        }
    }

    /**
     * Applies the {@code <announcement>} or {@code <not_announcement>} toggle
     * to the metadata, driving whether only admins can send messages.
     *
     * <p>Resolves the current metadata and, when present, delegates to
     * {@link #applyAnnounce(ChatMetadata, boolean)}.
     *
     * @param groupJid         the group JID
     * @param announcementOnly {@code true} for {@code <announcement>}, {@code false} for {@code <not_announcement>}
     */
    private void applyAnnounce(Jid groupJid, boolean announcementOnly) {
        var metadata = currentMetadata(groupJid);
        if (metadata != null) {
            applyAnnounce(metadata, announcementOnly);
        }
    }

    /**
     * Applies the {@code announce} value directly to a metadata instance.
     *
     * <p>Shared by the per-JID {@link #applyAnnounce(Jid, boolean)} entry point
     * and the create flow. Maps the flag through {@link ChatPolicy#of(boolean)}
     * for the {@link GroupMetadata} variant and writes the raw boolean for
     * {@link CommunityMetadata}.
     *
     * @param metadata         the metadata being updated
     * @param announcementOnly whether only admins can send messages
     */
    private void applyAnnounce(ChatMetadata metadata, boolean announcementOnly) {
        if (metadata instanceof GroupMetadata groupMetadata) {
            groupMetadata.setAnnounce(ChatPolicy.of(announcementOnly));
        } else if (metadata instanceof CommunityMetadata communityMetadata) {
            communityMetadata.setAnnounce(announcementOnly);
        }
    }

    /**
     * Applies the {@code <no_frequently_forwarded>} or
     * {@code <frequently_forwarded_ok>} toggle to the metadata, driving whether
     * frequently forwarded messages are blocked.
     *
     * <p>Resolves the current metadata and, when present, delegates to
     * {@link #applyNoFrequentlyForwarded(ChatMetadata, boolean)}.
     *
     * @param groupJid the group JID
     * @param value    whether frequently forwarded messages are blocked
     */
    private void applyNoFrequentlyForwarded(Jid groupJid, boolean value) {
        var metadata = currentMetadata(groupJid);
        if (metadata != null) {
            applyNoFrequentlyForwarded(metadata, value);
        }
    }

    /**
     * Applies the {@code noFrequentlyForwarded} value directly to a metadata
     * instance.
     *
     * <p>Shared by the per-JID {@link #applyNoFrequentlyForwarded(Jid, boolean)}
     * entry point and the create flow, writing the flag onto either the
     * {@link GroupMetadata} or {@link CommunityMetadata} variant.
     *
     * @param metadata the metadata being updated
     * @param value    whether frequently forwarded messages are blocked
     */
    private void applyNoFrequentlyForwarded(ChatMetadata metadata, boolean value) {
        if (metadata instanceof GroupMetadata groupMetadata) {
            groupMetadata.setNoFrequentlyForwarded(value);
        } else if (metadata instanceof CommunityMetadata communityMetadata) {
            communityMetadata.setNoFrequentlyForwarded(value);
        }
    }

    /**
     * Applies an {@code <ephemeral>} action by writing the expiration duration
     * and setting timestamp to both the chat and the metadata.
     *
     * <p>Reads the {@code expiration} attribute from either the action stanza or
     * its nested {@code <ephemeral>} child (the create flow uses the nested
     * form) and converts it through {@link ChatEphemeralTimer#of(Integer)}.
     * Does nothing when no expiration is present. The setting time is read from
     * the action's {@code t} attribute, falling back to the notification's
     * {@code t}, and is applied to the chat only when present.
     *
     * @param notification the parent {@code <notification>} stanza or {@code <group>} container
     * @param chat         the local chat
     * @param groupJid     the group JID
     * @param action       the {@code <ephemeral>} action stanza or {@code <group>} container
     */
    private void applyEphemeral(Stanza notification, Chat chat, Jid groupJid, Stanza action) {
        var ephemeralNode = action.getChild("ephemeral").orElse(null);
        Stanza expirationSource;
        if (ephemeralNode != null) {
            expirationSource = ephemeralNode;
        } else {
            expirationSource = action;
        }

        var expiration = expirationSource.getAttributeAsInt("expiration", (Integer) null);
        if (expiration == null) {
            return;
        }

        var timer = ChatEphemeralTimer.of(expiration);
        var timestamp = resolveInstant(action, "t");
        if (timestamp == null) {
            timestamp = resolveInstant(notification, "t");
        }

        chat.setEphemeralExpiration(timer);
        if (timestamp != null) {
            chat.setEphemeralSettingTimestamp(timestamp);
        }

        var metadata = currentMetadata(groupJid);
        if (metadata != null) {
            metadata.setEphemeralExpiration(timer);
        }
    }

    /**
     * Applies a {@code <not_ephemeral>} action by clearing the ephemeral timer
     * on both the chat and the metadata.
     *
     * @param chat     the local chat
     * @param groupJid the group JID
     */
    private void clearEphemeral(Chat chat, Jid groupJid) {
        chat.setEphemeralExpiration(null);
        var metadata = currentMetadata(groupJid);
        if (metadata != null) {
            metadata.setEphemeralExpiration(null);
        }
    }

    /**
     * Applies a {@code <growth_locked>} action by writing the expiration
     * timestamp and lock type onto the metadata.
     *
     * <p>The growth lock gates new members joining the group until the lock
     * expires. The expiration is read from the action's {@code expiration}
     * attribute and the lock type from its {@code type} attribute, written onto
     * either the {@link GroupMetadata} or {@link CommunityMetadata} variant.
     *
     * @param groupJid the group JID
     * @param action   the {@code <growth_locked>} action stanza
     */
    private void applyGrowthLock(Jid groupJid, Stanza action) {
        var metadata = currentMetadata(groupJid);
        var expiration = resolveInstant(action, "expiration");
        var type = action.getAttributeAsString("type", null);
        if (metadata instanceof GroupMetadata groupMetadata) {
            groupMetadata.setGrowthLockExpiration(expiration);
            groupMetadata.setGrowthLockType(type);
        } else if (metadata instanceof CommunityMetadata communityMetadata) {
            communityMetadata.setGrowthLockExpiration(expiration);
            communityMetadata.setGrowthLockType(type);
        }
    }

    /**
     * Applies a {@code <growth_unlocked>} action by clearing the growth lock
     * fields on the metadata.
     *
     * @param groupJid the group JID
     */
    private void clearGrowthLock(Jid groupJid) {
        var metadata = currentMetadata(groupJid);
        if (metadata instanceof GroupMetadata groupMetadata) {
            groupMetadata.setGrowthLockExpiration(null);
            groupMetadata.setGrowthLockType(null);
        } else if (metadata instanceof CommunityMetadata communityMetadata) {
            communityMetadata.setGrowthLockExpiration(null);
            communityMetadata.setGrowthLockType(null);
        }
    }

    /**
     * Applies a {@code <link link_type="parent_group">} action by writing the
     * parent community JID onto the group metadata.
     *
     * <p>Only the {@code parent_group} link type maps to a metadata field; the
     * first nested {@code <group>} child whose {@code jid} names a group or
     * community is used. Other link types ({@code sub_group},
     * {@code sibling_group}) have no direct field on Cobalt's metadata and are
     * reconciled by the post-loop metadata refresh.
     *
     * @param groupJid the group JID
     * @param action   the {@code <link>} action stanza
     */
    private void applyLink(Jid groupJid, Stanza action) {
        var linkType = action.getAttributeAsString("link_type", null);
        var metadata = currentMetadata(groupJid);
        if (metadata instanceof GroupMetadata groupMetadata && "parent_group".equals(linkType)) {
            action.getChildren("group").stream()
                    .map(child -> child.getAttributeAsJid("jid").orElse(null))
                    .filter(related -> related != null && related.hasGroupOrCommunityServer())
                    .findFirst()
                    .ifPresent(groupMetadata::setParentCommunityJid);
        }
    }

    /**
     * Applies an {@code <unlink unlink_type="parent_group">} action by clearing
     * the parent community JID on the group metadata.
     *
     * <p>Only the {@code parent_group} unlink type is acted upon; other unlink
     * types leave the metadata unchanged.
     *
     * @param groupJid the group JID
     * @param action   the {@code <unlink>} action stanza
     */
    private void applyUnlink(Jid groupJid, Stanza action) {
        var unlinkType = action.getAttributeAsString("unlink_type", null);
        var metadata = currentMetadata(groupJid);
        if (metadata instanceof GroupMetadata groupMetadata && "parent_group".equals(unlinkType)) {
            groupMetadata.setParentCommunityJid(null);
        }
    }

    /**
     * Applies a {@code <membership_approval_mode>} action by reading the
     * {@code state} attribute of the nested {@code <group_join>} child.
     *
     * <p>Treats a {@code state} of {@code "on"} as approval-required and
     * delegates to {@link #applyMembershipApproval(ChatMetadata, boolean)} when
     * the metadata is present.
     *
     * @param action   the {@code <membership_approval_mode>} action stanza
     * @param groupJid the group JID
     */
    private void applyMembershipApproval(Stanza action, Jid groupJid) {
        var state = action.getChild("group_join")
                .flatMap(child -> child.getAttributeAsString("state"))
                .orElse(null);
        var enabled = "on".equals(state);
        var metadata = currentMetadata(groupJid);
        if (metadata != null) {
            applyMembershipApproval(metadata, enabled);
        }
    }

    /**
     * Applies the membership-approval-mode value directly to a metadata
     * instance.
     *
     * <p>Shared by the per-JID {@link #applyMembershipApproval(Stanza, Jid)}
     * entry point and the create flow. Maps the flag through
     * {@link ChatPolicy#of(boolean)} for the {@link GroupMetadata} variant and
     * writes the raw boolean for {@link CommunityMetadata}.
     *
     * @param metadata the metadata being updated
     * @param enabled  whether membership approval is required
     */
    private void applyMembershipApproval(ChatMetadata metadata, boolean enabled) {
        if (metadata instanceof GroupMetadata groupMetadata) {
            groupMetadata.setMembershipApprovalMode(ChatPolicy.of(enabled));
        } else if (metadata instanceof CommunityMetadata communityMetadata) {
            communityMetadata.setMembershipApprovalMode(enabled);
        }
    }

    /**
     * Applies the {@code <allow_admin_reports>} or
     * {@code <not_allow_admin_reports>} toggle to the metadata, driving the
     * report-to-admin feature.
     *
     * <p>Writes the flag onto either the {@link GroupMetadata} or
     * {@link CommunityMetadata} variant.
     *
     * @param groupJid the group JID
     * @param value    whether reporting to admin is enabled
     */
    private void applyReportToAdmin(Jid groupJid, boolean value) {
        var metadata = currentMetadata(groupJid);
        if (metadata instanceof GroupMetadata groupMetadata) {
            groupMetadata.setReportToAdminMode(value);
        } else if (metadata instanceof CommunityMetadata communityMetadata) {
            communityMetadata.setReportToAdminMode(value);
        }
    }

    /**
     * Applies the {@code <allow_non_admin_sub_group_creation>} or
     * {@code <not_allow_non_admin_sub_group_creation>} toggle to a community
     * metadata.
     *
     * <p>This is a community-only action; plain {@link GroupMetadata} has no
     * sub-group concept and is left unchanged.
     *
     * @param groupJid the community JID
     * @param value    whether non-admin sub-group creation is allowed
     */
    private void applyNonAdminSubgroupCreation(Jid groupJid, boolean value) {
        var metadata = currentMetadata(groupJid);
        if (metadata instanceof CommunityMetadata communityMetadata) {
            communityMetadata.setAllowNonAdminSubGroupCreation(value);
        }
    }

    /**
     * Applies a {@code <member_add_mode>} action by reading the text content
     * and checking for {@code "admin_add"}.
     *
     * <p>Treats {@code "admin_add"} as admins-only and delegates to
     * {@link #applyMemberAddMode(ChatMetadata, boolean)} when the metadata is
     * present.
     *
     * @param groupJid the group JID
     * @param action   the {@code <member_add_mode>} action stanza
     */
    private void applyMemberAddMode(Jid groupJid, Stanza action) {
        var adminOnly = "admin_add".equals(action.toContentString().orElse(null));
        var metadata = currentMetadata(groupJid);
        if (metadata != null) {
            applyMemberAddMode(metadata, adminOnly);
        }
    }

    /**
     * Applies the member-add-mode value directly to a metadata instance.
     *
     * <p>Shared by the per-JID {@link #applyMemberAddMode(Jid, Stanza)} entry
     * point and the create flow. Maps the flag through
     * {@link ChatPolicy#of(boolean)} for the {@link GroupMetadata} variant and
     * writes the raw boolean for {@link CommunityMetadata}.
     *
     * @param metadata  the metadata being updated
     * @param adminOnly whether only admins can add members
     */
    private void applyMemberAddMode(ChatMetadata metadata, boolean adminOnly) {
        if (metadata instanceof GroupMetadata groupMetadata) {
            groupMetadata.setMemberAddMode(ChatPolicy.of(adminOnly));
        } else if (metadata instanceof CommunityMetadata communityMetadata) {
            communityMetadata.setMemberAddModeAdminOnly(adminOnly);
        }
    }

    /**
     * Applies the {@code <auto_add_disabled>} action by writing the
     * {@code generalChatAutoAddDisabled} flag.
     *
     * <p>Resolves the current metadata and, when present, delegates to
     * {@link #applyGeneralChatAutoAddDisabled(ChatMetadata, boolean)}.
     *
     * @param groupJid the group JID
     * @param value    whether auto-add to the general chat is disabled
     */
    private void applyGeneralChatAutoAddDisabled(Jid groupJid, boolean value) {
        var metadata = currentMetadata(groupJid);
        if (metadata != null) {
            applyGeneralChatAutoAddDisabled(metadata, value);
        }
    }

    /**
     * Applies the {@code <group_safety_check>} action by writing the
     * safety-check flag on the metadata.
     *
     * <p>Writes the flag onto either the {@link GroupMetadata} or
     * {@link CommunityMetadata} variant.
     *
     * @param groupJid the group JID
     * @param value    whether the group safety check is set
     */
    private void applyGroupSafetyCheck(Jid groupJid, boolean value) {
        var metadata = currentMetadata(groupJid);
        if (metadata instanceof GroupMetadata groupMetadata) {
            groupMetadata.setGroupSafetyCheck(value);
        } else if (metadata instanceof CommunityMetadata communityMetadata) {
            communityMetadata.setGroupSafetyCheck(value);
        }
    }

    /**
     * Applies the {@code <suspended>} or {@code <unsuspended>} toggle by
     * updating both the chat's suspended flag and the metadata.
     *
     * <p>Writes the flag onto the chat and onto either the {@link GroupMetadata}
     * or {@link CommunityMetadata} variant.
     *
     * @param chat     the local chat
     * @param groupJid the group JID
     * @param value    whether the group is suspended
     */
    private void applySuspended(Chat chat, Jid groupJid, boolean value) {
        chat.setSuspended(value);
        var metadata = currentMetadata(groupJid);
        if (metadata instanceof GroupMetadata groupMetadata) {
            groupMetadata.setSuspended(value);
        } else if (metadata instanceof CommunityMetadata communityMetadata) {
            communityMetadata.setSuspended(value);
        }
    }

    /**
     * Parses {@code <participant>} children and applies the requested mutation
     * to the metadata's participant set.
     *
     * <p>Does nothing when no metadata is present. For {@code ADD} and
     * {@code MODIFY} the participant is first removed to clear any prior role,
     * then re-added. For {@code REMOVE} the participant is removed by JID. For
     * {@code PROMOTE} and {@code DEMOTE} the participant is removed and re-added
     * with the forced role produced by {@link #parseParticipants(Stanza, GroupParticipantMutation)}.
     *
     * @param groupJid the group JID
     * @param action   the action stanza carrying {@code <participant>} children
     * @param mutation the mutation type
     */
    private void applyParticipants(Jid groupJid, Stanza action, GroupParticipantMutation mutation) {
        var metadata = currentMetadata(groupJid);
        if (metadata == null) {
            return;
        }

        for (var participant : parseParticipants(action, mutation)) {
            switch (mutation) {
                case ADD, MODIFY -> {
                    metadata.removeParticipant(participant.userJid());
                    metadata.addParticipant(participant);
                }
                case REMOVE -> metadata.removeParticipant(participant.userJid());
                case PROMOTE, DEMOTE -> {
                    metadata.removeParticipant(participant.userJid());
                    metadata.addParticipant(participant);
                }
            }
        }
    }

    /**
     * Parses {@code <participant>} children into a set of
     * {@link GroupParticipant} records with roles derived from the mutation and
     * the {@code type} attribute.
     *
     * <p>Participant nodes without a {@code jid} attribute are skipped.
     * {@code PROMOTE} always yields {@link GroupPartipantRole#ADMIN} and
     * {@code DEMOTE} always yields {@link GroupPartipantRole#MEMBER}; every other
     * mutation derives the role from the participant's {@code type} attribute.
     * Each JID is normalised through its user form before being stored.
     *
     * @param action   the action stanza carrying participant children
     * @param mutation the mutation type
     * @return an insertion-ordered set of parsed participants
     */
    private LinkedHashSet<GroupParticipant> parseParticipants(Stanza action, GroupParticipantMutation mutation) {
        var participants = new LinkedHashSet<GroupParticipant>();
        for (var participantNode : action.getChildren("participant")) {
            var jid = participantNode.getAttributeAsJid("jid").orElse(null);
            if (jid == null) {
                continue;
            }

            var role = switch (mutation) {
                case PROMOTE -> GroupPartipantRole.ADMIN;
                case DEMOTE -> GroupPartipantRole.MEMBER;
                default -> GroupPartipantRole.of(participantNode.getAttributeAsString("type", null))
                        .orElse(GroupPartipantRole.MEMBER);
            };

            participants.add(new GroupParticipantBuilder()
                    .userJid(jid.toUserJid())
                    .rank(role)
                    .build());
        }
        return participants;
    }

    /**
     * Collects every group or community JID referenced by the action into the
     * related-groups accumulator.
     *
     * <p>Scans the action's inline {@code <group>} and
     * {@code <sub_group_suggestion>} children as well as its
     * {@code context_group_jid} and {@code parent_group_jid} attributes,
     * adding each JID that names a group or community. The accumulated set
     * drives the post-loop metadata refresh so cross-group actions (links,
     * subgroup suggestions, parent-group changes) reach a consistent final
     * state.
     *
     * @param action        the action stanza being scanned
     * @param relatedGroups the accumulator set
     */
    private void collectRelatedGroups(Stanza action, LinkedHashSet<Jid> relatedGroups) {
        action.streamChildren("group")
                .map(child -> child.getAttributeAsJid("jid").orElse(null))
                .filter(jid -> jid != null && jid.hasGroupOrCommunityServer())
                .forEach(relatedGroups::add);

        action.streamChildren("sub_group_suggestion")
                .map(child -> child.getAttributeAsJid("jid").orElse(null))
                .filter(jid -> jid != null && jid.hasGroupOrCommunityServer())
                .forEach(relatedGroups::add);

        action.getAttributeAsJid("context_group_jid")
                .filter(Jid::hasGroupOrCommunityServer)
                .ifPresent(relatedGroups::add);
        action.getAttributeAsJid("parent_group_jid")
                .filter(Jid::hasGroupOrCommunityServer)
                .ifPresent(relatedGroups::add);
    }

    /**
     * Returns the current metadata record for the group, or {@code null} when
     * it is not in the store.
     *
     * <p>Used by every action branch. Cobalt populates metadata lazily, so the
     * caller may receive {@code null} on the first reference to a freshly
     * observed group; the post-loop refresh repopulates it.
     *
     * @param groupJid the group JID
     * @return the matching metadata, or {@code null}
     */
    private ChatMetadata currentMetadata(Jid groupJid) {
        return whatsapp.store().chatStore().findChatMetadata(groupJid).orElse(null);
    }

    /**
     * Writes a new subject onto the metadata when the value is non-null.
     *
     * <p>Shared by {@link #applySubject(Stanza, Chat, Jid, Stanza)} and the create
     * flow, writing onto either the {@link GroupMetadata} or
     * {@link CommunityMetadata} variant. A {@code null} subject is ignored.
     *
     * @param metadata the metadata being updated
     * @param subject  the new subject, or {@code null} to skip
     */
    private void setSubject(ChatMetadata metadata, String subject) {
        if (subject == null) {
            return;
        }

        if (metadata instanceof GroupMetadata groupMetadata) {
            groupMetadata.setSubject(subject);
        } else if (metadata instanceof CommunityMetadata communityMetadata) {
            communityMetadata.setSubject(subject);
        }
    }

    /**
     * Writes the subject change timestamp and author onto the metadata.
     *
     * <p>Shared by {@link #applySubject(Stanza, Chat, Jid, Stanza)} with the
     * resolved {@code s_t} and {@code s_o} pair, writing onto either the
     * {@link GroupMetadata} or {@link CommunityMetadata} variant.
     *
     * @param metadata  the metadata being updated
     * @param timestamp the subject change time, or {@code null}
     * @param author    the subject author JID, or {@code null}
     */
    private void setSubjectTimestamp(ChatMetadata metadata, Instant timestamp, Jid author) {
        if (metadata instanceof GroupMetadata groupMetadata) {
            groupMetadata.setSubjectTimestamp(timestamp);
            groupMetadata.setSubjectAuthorJid(author);
        } else if (metadata instanceof CommunityMetadata communityMetadata) {
            communityMetadata.setSubjectTimestamp(timestamp);
            communityMetadata.setSubjectAuthorJid(author);
        }
    }

    /**
     * Writes the description, description id, timestamp, and author onto the
     * metadata.
     *
     * <p>Shared by {@link #applyDescription(Stanza, Chat, Jid, Stanza)} and the
     * create flow, writing all four fields onto either the
     * {@link GroupMetadata} or {@link CommunityMetadata} variant.
     *
     * @param metadata    the metadata being updated
     * @param description the description body, or {@code null} when cleared
     * @param id          the server-issued description revision id
     * @param timestamp   the description change time
     * @param author      the description author JID
     */
    private void setDescription(ChatMetadata metadata, String description, String id, Instant timestamp, Jid author) {
        if (metadata instanceof GroupMetadata groupMetadata) {
            groupMetadata.setDescription(description);
            groupMetadata.setDescriptionId(id);
            groupMetadata.setDescriptionTimestamp(timestamp);
            groupMetadata.setDescriptionAuthorJid(author);
        } else if (metadata instanceof CommunityMetadata communityMetadata) {
            communityMetadata.setDescription(description);
            communityMetadata.setDescriptionId(id);
            communityMetadata.setDescriptionTimestamp(timestamp);
            communityMetadata.setDescriptionAuthorJid(author);
        }
    }

    /**
     * Extracts the description body from the create-flow nested path
     * {@code description > body}.
     *
     * <p>Used by {@link #applyCreate(Stanza, Chat, Jid, Stanza)} to read the
     * initial group description.
     *
     * @param groupStanza the {@code <group>} element from the create stanza
     * @return the description body, or {@code null}
     */
    private String resolveCreateDescriptionBody(Stanza groupStanza) {
        return groupStanza.getChild("description")
                .flatMap(desc -> desc.getChild("body"))
                .flatMap(Stanza::toContentString)
                .orElse(null);
    }

    /**
     * Extracts the description id from the create-flow
     * {@code <description id=.../>} attribute.
     *
     * <p>Used by {@link #applyCreate(Stanza, Chat, Jid, Stanza)} to read the
     * initial group description revision id.
     *
     * @param groupStanza the {@code <group>} element from the create stanza
     * @return the description id, or {@code null}
     */
    private String resolveCreateDescriptionId(Stanza groupStanza) {
        return groupStanza.getChild("description")
                .flatMap(desc -> desc.getAttributeAsString("id"))
                .orElse(null);
    }

    /**
     * Extracts the description body from a non-create
     * {@code <description><body>...</body></description>} action stanza.
     *
     * <p>Used by {@link #applyDescription(Stanza, Chat, Jid, Stanza)}.
     *
     * @param stanza the description action stanza
     * @return the body text, or {@code null}
     */
    private String resolveDescriptionBody(Stanza stanza) {
        return stanza.getChild("body")
                .flatMap(Stanza::toContentString)
                .orElse(null);
    }

    /**
     * Extracts the description id from a {@code <description id=.../>} action
     * stanza.
     *
     * <p>Used by {@link #applyDescription(Stanza, Chat, Jid, Stanza)}.
     *
     * @param stanza the description action stanza
     * @return the description id, or {@code null}
     */
    private String resolveDescriptionId(Stanza stanza) {
        return stanza.getAttributeAsString("id", null);
    }

    /**
     * Returns the first positive epoch-seconds attribute as an {@link Instant},
     * walking the supplied attribute keys in order.
     *
     * <p>Used by every branch that needs a timestamp. Some actions name the
     * timestamp {@code t} and others {@code s_t} or {@code expiration}, so
     * callers pass the candidate keys in priority order. Non-positive values
     * are treated as absent.
     *
     * @param stanza the stanza to read from
     * @param keys the attribute keys to try in order
     * @return the resolved {@link Instant}, or {@code null}
     */
    private Instant resolveInstant(Stanza stanza, String... keys) {
        for (var key : keys) {
            var epoch = stanza.getAttributeAsLong(key, (Long) null);
            if (epoch != null && epoch > 0) {
                return Instant.ofEpochSecond(epoch);
            }
        }
        return null;
    }

    /**
     * Re-queries metadata for the given group from the server.
     *
     * <p>Acts as the post-loop reconciler for every related group JID. Any
     * throwable raised by the query is debug-logged and suppressed so a
     * transient network error does not abort the whole notification.
     *
     * @param groupJid the group JID to refresh
     */
    private void refreshGroup(Jid groupJid) {
        try {
            whatsapp.queryChatMetadata(groupJid);
        } catch (Throwable throwable) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG,
                        "cannot refresh group metadata for " + new LogRedactable.User(String.valueOf(groupJid)),
                        throwable);
            }
        }
    }

    /**
     * Enumerates the participant mutation kinds applied by
     * {@link #applyParticipants(Jid, Stanza, GroupParticipantMutation)}.
     *
     * <p>The values correspond one-to-one with the WA Web action tags
     * {@code add}, {@code remove}, {@code promote}, {@code demote}, and
     * {@code modify}.
     */
    private enum GroupParticipantMutation {
        /**
         * Adds the participant to the group.
         *
         * <p>The participant's role is derived from the
         * {@code <participant type="..."/>} attribute.
         */
        ADD,
        /**
         * Removes the participant from the group.
         *
         * <p>Only the JID is read; the role is ignored because the record is
         * being deleted.
         */
        REMOVE,
        /**
         * Promotes the participant to admin.
         *
         * <p>Forces the role to {@link GroupPartipantRole#ADMIN} regardless of
         * the {@code type} attribute.
         */
        PROMOTE,
        /**
         * Demotes the participant from admin to plain member.
         *
         * <p>Forces the role to {@link GroupPartipantRole#MEMBER} regardless of
         * the {@code type} attribute.
         */
        DEMOTE,
        /**
         * Updates the participant's metadata without changing the role.
         *
         * <p>The role is derived from the {@code type} attribute just like
         * {@link #ADD}.
         */
        MODIFY
    }

    /**
     * Sends the {@code <ack class="notification" type="w:gp2"/>} stanza for the
     * processed notification.
     *
     * <p>Fire-and-forget. The original stanza's {@code participant} attribute
     * is mirrored back so the server can attribute the acknowledgement to the
     * correct action author.
     *
     * @param stanza the original {@code <notification>} stanza
     */
    private void sendNotificationAck(Stanza stanza) {
        ackSender.ack(AckClass.NOTIFICATION, stanza)
                .type("w:gp2")
                .participant(stanza.getAttributeAsJid("participant").orElse(null))
                .send();
    }
}
