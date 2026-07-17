package com.github.auties00.cobalt.wam.synthetic;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.chat.Chat;
import com.github.auties00.cobalt.wire.linked.chat.ChatMute;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupParticipant;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupPartipantRole;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.CommunityFeatureUsageEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.CommunityHomeActionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.CommunityTabActionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.GroupCatchUpEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.GroupMemberAddingUserJourneyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.GroupMemberUpdatesEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.GroupProfilePictureEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MentionPickerActionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PsGroupExitExperienceExitDeleteConfirmationDialogUiInteractionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PsGroupExitExperienceExitDialogInteractionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PsGroupExitExperienceGroupActionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PsGroupSafetyCheckEnabledEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PsGroupSafetyCheckExitDialogEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PsGroupSafetyCheckSheetSeenEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PsGroupSafetyCheckUiInteractionsEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.SuspendedGroupDeleteEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.AddMembersEntrypointType;
import com.github.auties00.cobalt.wire.wam.type.CommunityFeatureUiActionTakenType;
import com.github.auties00.cobalt.wire.wam.type.CommunityUiFeatureType;
import com.github.auties00.cobalt.wire.wam.type.DeleteSuspendedGroupBtn;
import com.github.auties00.cobalt.wire.wam.type.GroupCreateEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.GroupExitExperienceOrigin;
import com.github.auties00.cobalt.wire.wam.type.GroupMemberAddingActionType;
import com.github.auties00.cobalt.wire.wam.type.GroupMemberAddingMemberType;
import com.github.auties00.cobalt.wire.wam.type.GroupMemberUpdatesActionName;
import com.github.auties00.cobalt.wire.wam.type.GroupMemberUpdatesCurrentScreen;
import com.github.auties00.cobalt.wire.wam.type.GroupProfileActionType;
import com.github.auties00.cobalt.wire.wam.type.GroupTypeClient;
import com.github.auties00.cobalt.wire.wam.type.MentionType;
import com.github.auties00.cobalt.wire.wam.type.PreciseSizeBucket;
import com.github.auties00.cobalt.wire.wam.type.ProfilePictureType;
import com.github.auties00.cobalt.wire.wam.type.PsGroupExitExperienceActions;
import com.github.auties00.cobalt.wire.wam.type.PsGroupExitExperienceDeleteConfirmationDialogActions;
import com.github.auties00.cobalt.wire.wam.type.PsGroupExitExperienceExitDialogActions;
import com.github.auties00.cobalt.wire.wam.type.PsGroupSafetyCheckExitDialogActions;
import com.github.auties00.cobalt.wire.wam.type.PsGroupSafetyCheckUiInteractions;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Emits the group-and-community family of WhatsApp Metrics events that a genuine
 * WhatsApp Web session logs from group management, community, and group-safety
 * surfaces Cobalt does not render.
 *
 * <p>WhatsApp Web instruments a broad set of group and community screens: the
 * community-home and community-tab engagement counters, the group profile-picture
 * editor, community feature usage, the suspended-group delete flow, the
 * add-members group-history user journey, the group safety-check modal (enabled,
 * sheet-seen, UI interactions, exit dialog), the group exit-experience
 * spam-mitigation dialogs (exit dialog, delete-confirmation dialog, group action),
 * the composer {@code @}mention picker, the group-member-updates screen, and the
 * group catch-up mention summary. Every event in this class is UI-impression or
 * interaction telemetry: the WhatsApp Web feature that would fire it (a rendered
 * modal, a tab-navigation counter, an autocomplete plugin) has no counterpart in a
 * headless client, so Cobalt has no natural trigger for any of them. A Cobalt
 * session that never emitted these events would be trivially separable from a real
 * Web client whose telemetry stream always carries them.
 *
 * <p>This service therefore synthesises a plausible per-connect snapshot of that
 * surface. It sources real store state wherever a faithful value exists (a real
 * group or parent-community JID, the participant-count size bucket, the caller's
 * administrator status, the group creation timestamp, the muted state, the pending
 * mention ratio, the group message count) and fabricates the remaining UI-only
 * fields (dialog impression counters, click targets, funnel and session
 * identifiers, hashed integrity ids) with fresh per-connect randomness so
 * successive reconnects do not report a byte-identical, frozen figure that would
 * itself fingerprint the client. The values mirror what a lightly used real account
 * that belongs to groups and communities would report.
 *
 * <p>The single public entrypoint {@link #emitGroupTelemetry()} is intended to run
 * once per successful connect. It always emits the daily-aggregate community
 * engagement snapshots (the ones WhatsApp Web batches from its scheduled
 * daily-stats task) and emits the interaction and dialog events under probability
 * gates so any given session reports an organic subset rather than an identical
 * every-connect burst; across sessions every event fires. Emission cadence is
 * best-effort synthetic: the community-action events model the once-per-day
 * WhatsApp Web daily-stats task, the interaction and dialog events model sporadic
 * user actions.
 *
 * @implNote
 * This implementation draws every fabricated figure, probability gate, and
 * identifier from {@link ThreadLocalRandom} (directly or through
 * {@link SyntheticTelemetryUtils}) rather than from a host-stable seed, so no value
 * is a per-installation constant replayed byte-for-byte on every reconnect: each
 * connect mints fresh counters, session ids, and fabricated group JIDs. The hashed
 * integrity and thread identifiers are computed with {@code SHA-256} over the
 * caller identity and the group JID via
 * {@link SyntheticTelemetryUtils#sha256HexLower(String)}, matching the shape of
 * WhatsApp Web's HMAC-derived thread ids and integrity user hashes without
 * depending on their server-issued keys.
 *
 * @see WamService
 * @see com.github.auties00.cobalt.wam.LiveDailyStatsService
 */
@WhatsAppWebModule(moduleName = "WAWebGroupCatchUpWamEvent")
@WhatsAppWebModule(moduleName = "WAWebCommunityHomeActionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebCommunityTabActionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebGroupProfilePictureWamEvent")
@WhatsAppWebModule(moduleName = "WAWebCommunityFeatureUsageWamEvent")
@WhatsAppWebModule(moduleName = "WAWebSuspendedGroupDeleteWamEvent")
@WhatsAppWebModule(moduleName = "WAWebGroupMemberAddingUserJourneyWamEvent")
@WhatsAppWebModule(moduleName = "WAWebPsGroupSafetyCheckEnabledWamEvent")
@WhatsAppWebModule(moduleName = "WAWebPsGroupSafetyCheckSheetSeenWamEvent")
@WhatsAppWebModule(moduleName = "WAWebPsGroupSafetyCheckUiInteractionsWamEvent")
@WhatsAppWebModule(moduleName = "WAWebPsGroupSafetyCheckExitDialogWamEvent")
@WhatsAppWebModule(moduleName = "WAWebPsGroupExitExperienceExitDeleteConfirmationDialogUiInteractionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebPsGroupExitExperienceExitDialogInteractionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebPsGroupExitExperienceGroupActionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebMentionPickerActionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebGroupMemberUpdatesWamEvent")
public final class SyntheticGroupTelemetry {
    /**
     * The bound WhatsApp client whose store supplies the live group, community, and
     * account state sampled when populating the synthesised events.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The WAM service through which every synthesised group-and-community event is
     * committed for batched upload.
     */
    private final WamService wamService;

    /**
     * Constructs a new {@code SyntheticGroupTelemetry} bound to the given client and
     * WAM service.
     *
     * @param client     the WhatsApp client whose store is sampled, must not be
     *                   {@code null}
     * @param wamService the WAM service used to commit the emitted events, must not
     *                   be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SyntheticGroupTelemetry(LinkedWhatsAppClient client, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
    }

    /**
     * Emits the synthetic group-and-community snapshot for the current connect.
     *
     * <p>The community daily-aggregate events ({@code CommunityHomeAction},
     * {@code CommunityTabAction}) are committed unconditionally, modelling the
     * once-per-day WhatsApp Web daily-stats task as a per-connect heartbeat. The
     * remaining interaction, dialog, and profile events are committed under
     * probability gates so a single session reports a plausible organic subset
     * rather than an identical every-connect burst; across sessions every event in
     * the group fires.
     *
     * @apiNote intended to be invoked once from the client's connect callback; it is
     *          cheap and non-blocking, reading only in-memory store state
     */
    public void emitGroupTelemetry() {
        commitCommunityHomeAction();
        commitCommunityTabAction();

        if (SyntheticTelemetryUtils.chance(33)) {
            commitGroupCatchUp();
        }
        if (SyntheticTelemetryUtils.chance(20)) {
            commitGroupProfilePicture();
        }
        if (SyntheticTelemetryUtils.chance(16)) {
            commitCommunityFeatureUsage();
        }
        if (SyntheticTelemetryUtils.chance(25)) {
            commitMentionPickerAction();
        }
        if (SyntheticTelemetryUtils.chance(20)) {
            commitGroupMemberUpdates();
        }
        if (SyntheticTelemetryUtils.chance(16)) {
            commitGroupMemberAddingUserJourney();
        }
        if (SyntheticTelemetryUtils.chance(12)) {
            commitPsGroupSafetyCheckEnabled();
        }
        if (SyntheticTelemetryUtils.chance(12)) {
            commitPsGroupSafetyCheckSheetSeen();
        }
        if (SyntheticTelemetryUtils.chance(10)) {
            commitPsGroupSafetyCheckUiInteractions();
        }
        if (SyntheticTelemetryUtils.chance(8)) {
            commitPsGroupSafetyCheckExitDialog();
        }
        if (SyntheticTelemetryUtils.chance(8)) {
            commitPsGroupExitExperienceExitDialogInteraction();
        }
        if (SyntheticTelemetryUtils.chance(7)) {
            commitPsGroupExitExperienceExitDeleteConfirmationDialogUiInteraction();
        }
        if (SyntheticTelemetryUtils.chance(5)) {
            commitPsGroupExitExperienceGroupAction();
        }
        if (SyntheticTelemetryUtils.chance(3)) {
            commitSuspendedGroupDelete();
        }
    }

    /**
     * Builds and commits one {@code CommunityHomeAction} event carrying the daily
     * aggregated community-home engagement counters.
     *
     * <p>WhatsApp Web flushes this from its daily-stats task with the discovery,
     * join, navigation, and view tallies its community-home screen accumulated. The
     * community-home identity is taken from a real parent-community JID when the
     * store holds one; the engagement counters, which track UI events Cobalt does
     * not surface, are fabricated as small daily tallies with the view count kept at
     * least one so a flushed snapshot is never empty.
     */
    private void commitCommunityHomeAction() {
        wamService.commit(new CommunityHomeActionEventBuilder()
                .communityHomeGroupDiscoveries(SyntheticTelemetryUtils.count(0, 6))
                .communityHomeGroupJoins(SyntheticTelemetryUtils.count(0, 3))
                .communityHomeGroupNavigations(SyntheticTelemetryUtils.count(0, 12))
                .communityHomeId(anyCommunityJid())
                .communityHomeViews(SyntheticTelemetryUtils.count(1, 8))
                .build());
    }

    /**
     * Builds and commits one {@code CommunityTabAction} event carrying the daily
     * aggregated community-tab navigation counters.
     *
     * <p>WhatsApp Web flushes this from its daily-stats task with the tab-view,
     * group-navigation, to-home, no-action, and context-menu tallies its
     * community-tab UI accumulated. All counters track tab-navigation events Cobalt
     * does not surface and are fabricated as small daily tallies, with the primary
     * tab-view count kept at least one.
     */
    private void commitCommunityTabAction() {
        wamService.commit(new CommunityTabActionEventBuilder()
                .communityTabViews(SyntheticTelemetryUtils.count(1, 10))
                .communityTabGroupNavigations(SyntheticTelemetryUtils.count(0, 8))
                .communityTabToHomeViews(SyntheticTelemetryUtils.count(0, 4))
                .communityNoActionTabViews(SyntheticTelemetryUtils.count(0, 3))
                .communityTabViewsViaContextMenu(SyntheticTelemetryUtils.count(0, 2))
                .build());
    }

    /**
     * Builds and commits one {@code GroupCatchUp} event reporting the percentage of
     * pending mentions surfaced during a group catch-up.
     *
     * <p>The percentage is computed from live chat-store state for a real group
     * whose unread and unread-mention counts are both known; otherwise a plausible
     * low pending-mention percentage is fabricated.
     */
    private void commitGroupCatchUp() {
        wamService.commit(new GroupCatchUpEventBuilder()
                .mentionsCountPendingPercentage(pendingMentionsPercentage())
                .build());
    }

    /**
     * Builds and commits one {@code GroupProfilePicture} event describing an action
     * on a group's profile picture.
     *
     * <p>The group creation timestamp, participant-count size bucket, and the
     * caller's administrator status are taken from a real group chat when the store
     * holds one; the profile action, picture presence, and picture type, which track
     * an editor UI Cobalt does not render, are fabricated as a plausible
     * picture-update interaction.
     */
    private void commitGroupProfilePicture() {
        var group = firstGroupChat();
        wamService.commit(new GroupProfilePictureEventBuilder()
                .groupProfileAction(oneOf(new GroupProfileActionType[]{
                        GroupProfileActionType.CHANGE_PROFILE_PHOTO,
                        GroupProfileActionType.TAP_ACTION_ITEM_VIEW_PHOTO,
                        GroupProfileActionType.TAP_ACTION_ITEM_UPLOAD_PHOTO,
                        GroupProfileActionType.PROFILE_PIC_UPDATED
                }))
                .hasProfilePicture(true)
                .profilePictureType(oneOf(new ProfilePictureType[]{
                        ProfilePictureType.PHOTO_CAMERA,
                        ProfilePictureType.PHOTO_UPLOAD,
                        ProfilePictureType.EMOJI,
                        ProfilePictureType.STICKER
                }))
                .isAdmin(group.map(this::isSelfAdmin).orElseGet(() -> SyntheticTelemetryUtils.chance(50)))
                .preciseGroupSizeBucket(sizeBucket(group.map(this::groupSize).orElseGet(() -> (long) SyntheticTelemetryUtils.count(3, 120))))
                .groupCreationDs(group.flatMap(Chat::createdAt)
                        .map(instant -> String.valueOf(instant.getEpochSecond()))
                        .orElseGet(() -> String.valueOf(fabricatedCreationSeconds())))
                .build());
    }

    /**
     * Builds and commits one {@code CommunityFeatureUsage} event describing use of a
     * community UI feature.
     *
     * <p>The community identity is taken from a real parent-community JID when the
     * store holds one; the UI feature is reported as the sole enumerated
     * subgroup-switch feature and the action taken is fabricated from the community
     * navigation and add flows.
     */
    private void commitCommunityFeatureUsage() {
        wamService.commit(new CommunityFeatureUsageEventBuilder()
                .communityId(anyCommunityJid())
                .communityUiFeature(CommunityUiFeatureType.SUBGROUP_SWITCH)
                .communityUiAction(oneOf(new CommunityFeatureUiActionTakenType[]{
                        CommunityFeatureUiActionTakenType.ENTRY,
                        CommunityFeatureUiActionTakenType.GROUP_NAV,
                        CommunityFeatureUiActionTakenType.GROUP_ADD,
                        CommunityFeatureUiActionTakenType.COMMUNITY_NAV
                }))
                .build());
    }

    /**
     * Builds and commits one {@code SuspendedGroupDelete} private-stats event
     * describing the deletion of a suspended group.
     *
     * <p>The event carries only the UI button source from which the delete was
     * triggered, fabricated as either the bottom-sheet or the blocked-composer
     * button that a real suspended-group delete flow offers.
     */
    private void commitSuspendedGroupDelete() {
        wamService.commit(new SuspendedGroupDeleteEventBuilder()
                .deleteBtnSource(oneOf(new DeleteSuspendedGroupBtn[]{
                        DeleteSuspendedGroupBtn.BOTTOM_SHEET_BTN,
                        DeleteSuspendedGroupBtn.BLOCKED_COMPOSER_BTN
                }))
                .build());
    }

    /**
     * Builds and commits one {@code GroupMemberAddingUserJourney} event describing a
     * step of the add-members group-history-sharing funnel.
     *
     * <p>The target group id, its message count, and the caller's administrator
     * status are taken from a real group chat when the store holds one; the modelled
     * step is the history-footer-displayed stage. The user-journey timestamp is the
     * real wall clock, the unified and app session identifiers are fresh UUIDs, and
     * the funnel identifier and the selection, suggestion, and toggle fields are
     * fabricated to mirror a member picker opened on an existing group.
     */
    private void commitGroupMemberAddingUserJourney() {
        var group = firstGroupChat();
        var groupJid = group.map(chat -> chat.jid().toString()).orElseGet(this::fabricatedGroupJid);
        var historyCount = group.map(Chat::messageCount)
                .map(Integer::longValue)
                .orElseGet(() -> (long) SyntheticTelemetryUtils.count(5, 200));
        wamService.commit(new GroupMemberAddingUserJourneyEventBuilder()
                .groupMemberAddingActionType(GroupMemberAddingActionType.GROUP_HISTORY_FOOTER_DISPLAYED)
                .groupMemberAddingMemberType(GroupMemberAddingMemberType.WA_USER)
                .uiSurface(oneOf(new TsSurface[]{
                        TsSurface.GROUP_MEMBER_ADD_EXISTING_GROUP,
                        TsSurface.GROUP_MEMBER_ADD_GROUP_CREATION,
                        TsSurface.GROUP_CREATION
                }))
                .groupCreateEntryPoint(oneOf(new GroupCreateEntryPoint[]{
                        GroupCreateEntryPoint.CHATS,
                        GroupCreateEntryPoint.MENU,
                        GroupCreateEntryPoint.CONTACT_INFO_GROUPS_IN_COMMON,
                        GroupCreateEntryPoint.COMMUNITY_HOME
                }))
                .groupAddMemberEntryPoint(oneOf(new AddMembersEntrypointType[]{
                        AddMembersEntrypointType.GROUP_INFO_ACTION_BUTTON,
                        AddMembersEntrypointType.GROUP_INFO_CONTEXT_MENU,
                        AddMembersEntrypointType.GROUP_MEMBERS_LIST_ADD_BUTTON
                }))
                .groupCreationGroupId(groupJid)
                .groupHistoryMessagesCount(historyCount)
                .isGroupHistoryToggledOn(true)
                .selectedMemberCnt(SyntheticTelemetryUtils.count(1, 12))
                .addSelectedContactsCount(SyntheticTelemetryUtils.count(1, 12))
                .suggestedContactsCount(SyntheticTelemetryUtils.count(0, 20))
                .potentialTotalSuggestionCount(SyntheticTelemetryUtils.count(5, 40))
                .hasGroupName(true)
                .hasProfilePicture(SyntheticTelemetryUtils.chance(50))
                .isAdmin(group.map(this::isSelfAdmin).orElseGet(() -> SyntheticTelemetryUtils.chance(50)))
                .userJourneyEventMs(System.currentTimeMillis())
                .unifiedSessionId(SyntheticTelemetryUtils.newSessionId())
                .appSessionId(SyntheticTelemetryUtils.newSessionId())
                .userJourneyFunnelId(SyntheticTelemetryUtils.sha256HexLower("funnel|" + groupJid).substring(0, 16))
                .build());
    }

    /**
     * Builds and commits one {@code PsGroupSafetyCheckEnabled} private-stats event
     * describing the group safety-check feature being enabled for a joined group.
     *
     * <p>The safety-check group JID is taken from a real group chat when the store
     * holds one and the initially-muted flag is read from that chat's live mute
     * state; the hashed integrity user id is derived from the caller identity, and
     * the join-by-invite-link, added-by-contact, and will-be-seen flags are
     * fabricated to mirror a user who was added to an unfamiliar group.
     */
    private void commitPsGroupSafetyCheckEnabled() {
        var group = firstGroupChat();
        var groupJid = group.map(chat -> chat.jid().toString()).orElseGet(this::fabricatedGroupJid);
        wamService.commit(new PsGroupSafetyCheckEnabledEventBuilder()
                .psSafetyCheckGroupJid(groupJid)
                .psWasSafetyCheckGroupInitiallyMuted(group.flatMap(Chat::mute)
                        .map(ChatMute::isMuted)
                        .orElseGet(() -> SyntheticTelemetryUtils.chance(50)))
                .didJoinByGil(SyntheticTelemetryUtils.chance(50))
                .wasAddedByContact(SyntheticTelemetryUtils.chance(50))
                .willSafetyCheckBeSeen(true)
                .integrityGroupUserHashedId(integrityGroupUserHashedId(groupJid))
                .build());
    }

    /**
     * Builds and commits one {@code PsGroupSafetyCheckSheetSeen} private-stats event
     * recording that the group safety-check bottom sheet was seen.
     *
     * <p>The safety-check group JID is taken from a real group chat when the store
     * holds one and the hashed integrity user id is derived from the caller
     * identity.
     */
    private void commitPsGroupSafetyCheckSheetSeen() {
        var groupJid = firstGroupChat().map(chat -> chat.jid().toString()).orElseGet(this::fabricatedGroupJid);
        wamService.commit(new PsGroupSafetyCheckSheetSeenEventBuilder()
                .psSafetyCheckGroupJid(groupJid)
                .integrityGroupUserHashedId(integrityGroupUserHashedId(groupJid))
                .build());
    }

    /**
     * Builds and commits one {@code PsGroupSafetyCheckUiInteractions} private-stats
     * event describing an interaction with the group safety-check modal.
     *
     * <p>The safety-check group JID is taken from a real group chat when the store
     * holds one and the hashed integrity user id is derived from the caller
     * identity; the interaction is drawn from the sheet's tappable options.
     */
    private void commitPsGroupSafetyCheckUiInteractions() {
        var groupJid = firstGroupChat().map(chat -> chat.jid().toString()).orElseGet(this::fabricatedGroupJid);
        wamService.commit(new PsGroupSafetyCheckUiInteractionsEventBuilder()
                .psSafetyCheckGroupJid(groupJid)
                .integrityGroupUserHashedId(integrityGroupUserHashedId(groupJid))
                .psSafetyCheckInteraction(oneOf(new PsGroupSafetyCheckUiInteractions[]{
                        PsGroupSafetyCheckUiInteractions.SEE_CHAT,
                        PsGroupSafetyCheckUiInteractions.SEE_SCAM_EXAMPLES,
                        PsGroupSafetyCheckUiInteractions.PRIVACY_SETTINGS,
                        PsGroupSafetyCheckUiInteractions.HOW_TO_REPORT,
                        PsGroupSafetyCheckUiInteractions.DISMISS,
                        PsGroupSafetyCheckUiInteractions.X_BUTTON
                }))
                .build());
    }

    /**
     * Builds and commits one {@code PsGroupSafetyCheckExitDialog} private-stats event
     * describing an action taken on the group safety-check exit dialog.
     *
     * <p>The safety-check group JID is taken from a real group chat when the store
     * holds one and the hashed integrity user id is derived from the caller
     * identity; the dialog action is drawn from the group-exit options.
     */
    private void commitPsGroupSafetyCheckExitDialog() {
        var groupJid = firstGroupChat().map(chat -> chat.jid().toString()).orElseGet(this::fabricatedGroupJid);
        wamService.commit(new PsGroupSafetyCheckExitDialogEventBuilder()
                .psSafetyCheckGroupJid(groupJid)
                .integrityGroupUserHashedId(integrityGroupUserHashedId(groupJid))
                .psGroupSafetyCheckExitDialogAction(oneOf(new PsGroupSafetyCheckExitDialogActions[]{
                        PsGroupSafetyCheckExitDialogActions.GROUP_EXIT_DIALOG_DISMISS,
                        PsGroupSafetyCheckExitDialogActions.GROUP_EXIT_DIALOG_EXIT,
                        PsGroupSafetyCheckExitDialogActions.GROUP_EXIT_DIALOG_EXIT_AND_REPORT
                }))
                .build());
    }

    /**
     * Builds and commits one {@code PsGroupExitExperienceExitDialogInteraction}
     * private-stats event describing an interaction with the group exit-experience
     * exit dialog.
     *
     * <p>The group JID is taken from a real group chat when the store holds one; the
     * experience-enabled flag is reported as {@code true} (WhatsApp Web only logs
     * this dialog when the experience is enabled), the reporting-enabled flag is
     * fabricated, and the dialog action and touch-point origin are drawn from the
     * spam-mitigation exit flow.
     */
    private void commitPsGroupExitExperienceExitDialogInteraction() {
        wamService.commit(new PsGroupExitExperienceExitDialogInteractionEventBuilder()
                .psExitExperienceReportingEnabled(SyntheticTelemetryUtils.chance(50))
                .psGroupExitExperienceEnabled(true)
                .psGroupExitExperienceExitDialogAction(oneOf(new PsGroupExitExperienceExitDialogActions[]{
                        PsGroupExitExperienceExitDialogActions.GROUP_EXIT_EXPERIENCE_DIALOG_OPEN,
                        PsGroupExitExperienceExitDialogActions.GROUP_EXIT_EXPERIENCE_DIALOG_NEW_EXIT_TAPPED,
                        PsGroupExitExperienceExitDialogActions.GROUP_EXIT_EXPERIENCE_DIALOG_NEW_EXIT_AND_DELETE_TAPPED,
                        PsGroupExitExperienceExitDialogActions.GROUP_EXIT_EXPERIENCE_DIALOG_CANCELLED
                }))
                .psGroupExitExperienceGroupJid(anyGroupJid())
                .psGroupExitExperienceTouchPoint(exitExperienceOrigin())
                .build());
    }

    /**
     * Builds and commits one
     * {@code PsGroupExitExperienceExitDeleteConfirmationDialogUiInteraction}
     * private-stats event describing an interaction with the exit-and-delete
     * confirmation dialog.
     *
     * <p>The group JID is taken from a real group chat when the store holds one; the
     * dialog action is fabricated as either the delete-confirmed or the cancelled
     * button and the touch-point origin is drawn from the spam-mitigation exit flow.
     */
    private void commitPsGroupExitExperienceExitDeleteConfirmationDialogUiInteraction() {
        wamService.commit(new PsGroupExitExperienceExitDeleteConfirmationDialogUiInteractionEventBuilder()
                .psGroupExitExperienceDeleteConfirmationDialogAction(oneOf(
                        new PsGroupExitExperienceDeleteConfirmationDialogActions[]{
                                PsGroupExitExperienceDeleteConfirmationDialogActions.GROUP_EXIT_EXPERIENCE_DELETE_CONFIRMATION_DIALOG_DELETE_TAPPED,
                                PsGroupExitExperienceDeleteConfirmationDialogActions.GROUP_EXIT_EXPERIENCE_DELETE_CONFIRMATION_DIALOG_CANCELLED
                        }))
                .psGroupExitExperienceGroupJid(anyGroupJid())
                .psGroupExitExperienceTouchPoint(exitExperienceOrigin())
                .build());
    }

    /**
     * Builds and commits one {@code PsGroupExitExperienceGroupAction} private-stats
     * event recording a group action taken in the exit-experience flow.
     *
     * <p>The group JID is taken from a real group chat when the store holds one; the
     * action is fabricated as a group deletion or exit and the experience-enabled
     * flag is reported as {@code true}, matching WhatsApp Web's
     * {@code logGroupDeleteFromDeletePopup} which commits only the action, the
     * enabled flag, and the group JID.
     */
    private void commitPsGroupExitExperienceGroupAction() {
        wamService.commit(new PsGroupExitExperienceGroupActionEventBuilder()
                .psGroupExitExperienceAction(oneOf(new PsGroupExitExperienceActions[]{
                        PsGroupExitExperienceActions.GROUP_DELETED,
                        PsGroupExitExperienceActions.GROUP_EXITED
                }))
                .psGroupExitExperienceEnabled(true)
                .psGroupExitExperienceGroupJid(anyGroupJid())
                .build());
    }

    /**
     * Builds and commits one {@code MentionPickerAction} event describing an
     * interaction with the composer {@code @}mention picker.
     *
     * <p>The group size, group-type classification, and group JID are taken from a
     * real group chat when the store holds one; the thread identifier is a hashed
     * derivation of the group JID matching WhatsApp Web's chat-thread-id shape, and
     * the mention type is drawn from the picker's candidate kinds.
     */
    private void commitMentionPickerAction() {
        var group = firstGroupChat();
        var groupJid = group.map(chat -> chat.jid().toString()).orElseGet(this::fabricatedGroupJid);
        wamService.commit(new MentionPickerActionEventBuilder()
                .groupSize(group.map(this::groupSize).orElseGet(() -> (long) SyntheticTelemetryUtils.count(3, 80)))
                .groupTypeClient(group.map(this::groupTypeClient).orElse(GroupTypeClient.REGULAR_GROUP))
                .isAGroup(true)
                .mentionGroupId(groupJid)
                .mentionType(oneOf(new MentionType[]{
                        MentionType.REGULAR_USER,
                        MentionType.GROUP,
                        MentionType.EVERYONE,
                        MentionType.META_AI_BOT
                }))
                .threadId(threadId(groupJid))
                .build());
    }

    /**
     * Builds and commits one {@code GroupMemberUpdates} event describing a fetch on
     * the group member-updates screen.
     *
     * <p>The modelled step is a successful member-updates fetch on the updates
     * screen; the fetched-message count and latency and the session identifier are
     * fabricated to mirror a lightly populated screen loaded in well under a second.
     */
    private void commitGroupMemberUpdates() {
        wamService.commit(new GroupMemberUpdatesEventBuilder()
                .groupMemberUpdatesActionName(GroupMemberUpdatesActionName.FETCH_MEMBER_UPDATES_SUCCESS)
                .groupMemberUpdatesCurrentScreen(GroupMemberUpdatesCurrentScreen.GROUP_MEMBER_UPDATES_SCREEN)
                .fetchedMessageCount(SyntheticTelemetryUtils.count(0, 25))
                .fetchedMessageLatency(SyntheticTelemetryUtils.count(40, 800))
                .groupMemberUpdatesSessionId(SyntheticTelemetryUtils.newSessionId())
                .build());
    }

    /**
     * Returns the percentage of pending mentions for a real group, or a fabricated
     * low percentage when no such group state exists.
     *
     * <p>When a group chat reports both an unread count and an unread-mention count,
     * the value is the mention share of the unread backlog, clamped to
     * {@code [0, 100]}; otherwise a plausible low pending-mention percentage is
     * fabricated.
     *
     * @return a pending-mentions percentage in {@code [0, 100]}
     */
    private long pendingMentionsPercentage() {
        for (var chat : client.store().chatStore().chats()) {
            if (!chat.jid().hasGroupOrCommunityServer()) {
                continue;
            }
            var unread = chat.unreadCount();
            var mentions = chat.unreadMentionCount();
            if (unread.isPresent() && unread.getAsInt() > 0 && mentions.isPresent()) {
                var pct = (long) mentions.getAsInt() * 100L / unread.getAsInt();
                return Math.min(100L, Math.max(0L, pct));
            }
        }
        return SyntheticTelemetryUtils.count(0, 40);
    }

    /**
     * Returns any one group or community chat from the store.
     *
     * @return the first chat whose JID targets the group-or-community domain, or
     *         empty when the store holds none
     */
    private Optional<Chat> firstGroupChat() {
        return client.store()
                .chatStore()
                .chats()
                .stream()
                .filter(chat -> chat.jid().hasGroupOrCommunityServer())
                .findFirst();
    }

    /**
     * Returns any one parent-community chat from the store.
     *
     * @return the first chat flagged as a parent group, or empty when the store
     *         holds none
     */
    private Optional<Chat> firstParentGroup() {
        return client.store()
                .chatStore()
                .chats()
                .stream()
                .filter(Chat::isParentGroup)
                .findFirst();
    }

    /**
     * Returns a group JID string, preferring a real group chat over a fabrication.
     *
     * @return a real group JID when the store holds one, otherwise a fabricated
     *         group JID
     */
    private String anyGroupJid() {
        return firstGroupChat()
                .map(chat -> chat.jid().toString())
                .orElseGet(this::fabricatedGroupJid);
    }

    /**
     * Returns a community JID string, preferring a real parent community, then any
     * real group, over a fabrication.
     *
     * @return a real parent-community or group JID when the store holds one,
     *         otherwise a fabricated group JID
     */
    private String anyCommunityJid() {
        return firstParentGroup()
                .or(this::firstGroupChat)
                .map(chat -> chat.jid().toString())
                .orElseGet(this::fabricatedGroupJid);
    }

    /**
     * Returns the number of participants in the given group chat, or a fabricated
     * plausible size when the participant list is empty.
     *
     * @param chat the group chat to size
     * @return the participant count, or a fabricated size when none are known
     */
    private long groupSize(Chat chat) {
        var size = chat.participant().size();
        return size > 0 ? size : SyntheticTelemetryUtils.count(3, 120);
    }

    /**
     * Classifies the given group chat for the {@code groupTypeClient} field.
     *
     * <p>A parent group maps to {@link GroupTypeClient#PARENT_GROUP}, a default
     * subgroup to {@link GroupTypeClient#DEFAULT_SUB_GROUP}, any other linked
     * subgroup to {@link GroupTypeClient#SUB_GROUP}, and a standalone group to
     * {@link GroupTypeClient#REGULAR_GROUP}.
     *
     * @param chat the group chat to classify
     * @return the matching client-side group type
     */
    private GroupTypeClient groupTypeClient(Chat chat) {
        if (chat.isParentGroup()) {
            return GroupTypeClient.PARENT_GROUP;
        }
        if (chat.isDefaultSubgroup()) {
            return GroupTypeClient.DEFAULT_SUB_GROUP;
        }
        if (chat.parentGroupId().isPresent()) {
            return GroupTypeClient.SUB_GROUP;
        }
        return GroupTypeClient.REGULAR_GROUP;
    }

    /**
     * Returns whether the bound account is an administrator or founder of the given
     * group chat.
     *
     * <p>The caller's phone-number JID and LID are matched against the group's
     * participant list; a match with the {@link GroupPartipantRole#ADMIN} or
     * {@link GroupPartipantRole#FOUNDER} rank yields {@code true}.
     *
     * @param chat the group chat to inspect
     * @return {@code true} when the account participates with an administrator or
     *         founder rank
     */
    private boolean isSelfAdmin(Chat chat) {
        var account = client.store().accountStore();
        var self = account.jid();
        var selfLid = account.lid();
        for (var participant : chat.participant()) {
            var participantJid = participant.userJid();
            var isSelf = self.filter(participantJid::equals).isPresent()
                    || selfLid.filter(participantJid::equals).isPresent();
            if (!isSelf) {
                continue;
            }
            var rank = participant.rank();
            if (rank.filter(role -> role == GroupPartipantRole.ADMIN || role == GroupPartipantRole.FOUNDER).isPresent()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Maps a participant count to the corresponding {@link PreciseSizeBucket}.
     *
     * <p>The buckets mirror the fixed less-than thresholds WhatsApp reports, from
     * {@link PreciseSizeBucket#LT4} up to {@link PreciseSizeBucket#LT5000}, with
     * counts of five thousand or more landing in
     * {@link PreciseSizeBucket#LARGEST_BUCKET}.
     *
     * @param size the participant count to bucket
     * @return the matching size bucket
     */
    private static PreciseSizeBucket sizeBucket(long size) {
        if (size < 4) {
            return PreciseSizeBucket.LT4;
        }
        if (size < 8) {
            return PreciseSizeBucket.LT8;
        }
        if (size < 16) {
            return PreciseSizeBucket.LT16;
        }
        if (size < 32) {
            return PreciseSizeBucket.LT32;
        }
        if (size < 64) {
            return PreciseSizeBucket.LT64;
        }
        if (size < 128) {
            return PreciseSizeBucket.LT128;
        }
        if (size < 256) {
            return PreciseSizeBucket.LT256;
        }
        if (size < 512) {
            return PreciseSizeBucket.LT512;
        }
        if (size < 1000) {
            return PreciseSizeBucket.LT1000;
        }
        if (size < 1500) {
            return PreciseSizeBucket.LT1500;
        }
        if (size < 2000) {
            return PreciseSizeBucket.LT2000;
        }
        if (size < 2500) {
            return PreciseSizeBucket.LT2500;
        }
        if (size < 3000) {
            return PreciseSizeBucket.LT3000;
        }
        if (size < 3500) {
            return PreciseSizeBucket.LT3500;
        }
        if (size < 4000) {
            return PreciseSizeBucket.LT4000;
        }
        if (size < 4500) {
            return PreciseSizeBucket.LT4500;
        }
        if (size < 5000) {
            return PreciseSizeBucket.LT5000;
        }
        return PreciseSizeBucket.LARGEST_BUCKET;
    }

    /**
     * Returns a touch-point origin drawn from the group exit-experience
     * spam-mitigation entry flows.
     *
     * @return one of the exit-experience origins a real spam flow reports
     */
    private GroupExitExperienceOrigin exitExperienceOrigin() {
        return oneOf(new GroupExitExperienceOrigin[]{
                GroupExitExperienceOrigin.FGX_CARD,
                GroupExitExperienceOrigin.SUSPICIOUS_CHAT_BANNER,
                GroupExitExperienceOrigin.CHAT_OVERFLOW_MENU,
                GroupExitExperienceOrigin.WEB_CONTEXT_MENU,
                GroupExitExperienceOrigin.GROUP_INFO
        });
    }

    /**
     * Derives the hashed integrity user id for the given group.
     *
     * <p>The value is the {@code SHA-256} hash of the caller's LID (falling back to
     * its phone-number JID) combined with the group JID, matching the opaque
     * fixed-width hex shape of WhatsApp Web's integrity user hash without needing its
     * server-issued salt.
     *
     * @param groupJid the group JID the hash is scoped to
     * @return a lowercase hexadecimal hashed user id
     */
    private String integrityGroupUserHashedId(String groupJid) {
        var account = client.store().accountStore();
        var self = account.lid()
                .or(account::jid)
                .map(Jid::toString)
                .orElse("");
        return SyntheticTelemetryUtils.sha256HexLower(self + '|' + groupJid);
    }

    /**
     * Derives the hashed thread id for the given group JID.
     *
     * <p>The value is the {@code SHA-256} hash of the JID under a thread-scoped
     * prefix, matching the shape of WhatsApp Web's HMAC-derived chat-thread id.
     *
     * @param jid the chat JID the thread id is scoped to
     * @return a lowercase hexadecimal thread id
     */
    private String threadId(String jid) {
        return SyntheticTelemetryUtils.sha256HexLower("thread|" + jid);
    }

    /**
     * Fabricates a plausible WhatsApp group JID string.
     *
     * <p>The value carries the {@code 120363} community-era prefix real group JIDs
     * use, followed by freshly sampled digits and the group-or-community domain, so
     * it is well-formed and re-minted on every call rather than replayed identically
     * across reconnects. This is a fallback only: a real group JID from the store is
     * always preferred over this fabrication.
     *
     * @return a fabricated group JID of the form {@code 120363XXXXXXXXXXXX@g.us}
     */
    private String fabricatedGroupJid() {
        var random = ThreadLocalRandom.current();
        var builder = new StringBuilder("120363");
        for (var i = 0; i < 12; i++) {
            builder.append(random.nextInt(10));
        }
        return builder.append("@g.us").toString();
    }

    /**
     * Fabricates a plausible group creation timestamp in epoch seconds.
     *
     * <p>The value lands in the multi-year window real WhatsApp groups were created
     * in, drawn freshly from {@link ThreadLocalRandom} so it is not a frozen
     * per-installation constant.
     *
     * @return a fabricated creation instant, in seconds since the epoch
     */
    private long fabricatedCreationSeconds() {
        return 1_500_000_000L + ThreadLocalRandom.current().nextInt(250_000_000);
    }

    /**
     * Returns a pseudo-random element of the given array.
     *
     * <p>The element is drawn from {@link ThreadLocalRandom} so successive draws are
     * fresh rather than replayed from a host-stable seed.
     *
     * @param options the candidate values, must be non-empty
     * @param <T>     the element type
     * @return one element of {@code options} drawn uniformly at random
     */
    private <T> T oneOf(T[] options) {
        return options[ThreadLocalRandom.current().nextInt(options.length)];
    }
}
