package com.github.auties00.cobalt.wam.synthetic;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.chat.Chat;
import com.github.auties00.cobalt.wire.linked.contact.Contact;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.ChatLockDailyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.DangerousFileOpenStatsV2EventBuilder;
import com.github.auties00.cobalt.wire.wam.event.DefenseModeClickEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.DyiReportDownloadEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.DyiReportRequestEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.GatedChatOpenedEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.LockFolderUnlockEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PnhDailyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PrivacyHighlightDailyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PrivacySettingsClickEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PrivacyTipActionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PsFmxActionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ScreenLockSettingsDataEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.UserNoticeErrorEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.UserNoticeEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.UsernameExposedEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ViewOnceScreenshotActionsEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.ChatGatedReason;
import com.github.auties00.cobalt.wire.wam.type.DefenseModeClickControlName;
import com.github.auties00.cobalt.wire.wam.type.DefenseModeClickDesiredState;
import com.github.auties00.cobalt.wire.wam.type.DyiReportTypeCode;
import com.github.auties00.cobalt.wire.wam.type.DyiTriggerTypeCode;
import com.github.auties00.cobalt.wire.wam.type.FmxEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.FmxEventType;
import com.github.auties00.cobalt.wire.wam.type.HarmfulFileWarningClickthroughAction;
import com.github.auties00.cobalt.wire.wam.type.HarmfulFileWarningSenderRelationship;
import com.github.auties00.cobalt.wire.wam.type.HighlightGroupType;
import com.github.auties00.cobalt.wire.wam.type.LandingSurface;
import com.github.auties00.cobalt.wire.wam.type.NoticeTriggeredBy;
import com.github.auties00.cobalt.wire.wam.type.NoticeType;
import com.github.auties00.cobalt.wire.wam.type.PrivacyControlEntryPointType;
import com.github.auties00.cobalt.wire.wam.type.PrivacyControlItemType;
import com.github.auties00.cobalt.wire.wam.type.PrivacyHighlightCategoryEnum;
import com.github.auties00.cobalt.wire.wam.type.PrivacyHighlightSurfaceEnum;
import com.github.auties00.cobalt.wire.wam.type.PrivacyTipActionType;
import com.github.auties00.cobalt.wire.wam.type.TypeOfGroupEnum;
import com.github.auties00.cobalt.wire.wam.type.UnlockEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.UserNoticeErrorEventType;
import com.github.auties00.cobalt.wire.wam.type.UserNoticeEventType;
import com.github.auties00.cobalt.wire.wam.type.VoMessageType;
import com.github.auties00.cobalt.wire.wam.type.VoSsAction;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Emits the privacy-and-safety family of WhatsApp Metrics events that a genuine
 * WhatsApp Web session logs from user-interface surfaces Cobalt does not render.
 *
 * <p>WhatsApp Web instruments a broad set of privacy, safety, and account-control
 * screens: persistent-disclosure (PDFN) user notices, the E2EE privacy-highlight
 * golden box, the privacy-settings and privacy-tip surfaces, chat-lock and
 * locked-folder unlocks, screen-lock configuration, the phone-number-hiding (PNH)
 * indicators, the first-message-experience (FMX) safety card, the dangerous-file
 * open warning, defense-mode controls, download-your-information (DYI) reports,
 * gated-chat opens, view-once screenshot detection, and username exposure. Every
 * event in this class is UI-impression or interaction telemetry: the WhatsApp Web
 * feature that would fire it has no counterpart in a headless client, so Cobalt
 * has no natural trigger for any of them. A Cobalt session that never emitted
 * these events would be trivially separable from a real Web client whose telemetry
 * stream always carries them.
 *
 * <p>This service therefore synthesises a plausible per-connect snapshot of that
 * surface. It sources real store state wherever a faithful value exists (contact
 * and locked-chat counts, missing phone-to-LID mappings, a real thread identifier,
 * the presence of a reserved username) and fabricates the remaining UI-only fields
 * (dialog impression counters, click targets, auto-lock durations) as fresh
 * per-connect draws from {@link SyntheticTelemetryUtils}, so successive connects
 * report an organic, non-identical snapshot rather than a byte-for-byte constant.
 * The values mirror what a lightly used real account would report.
 *
 * <p>The single public entrypoint {@link #emitPrivacySafetyTelemetry()} is intended
 * to run once per successful connect. It always emits the daily-aggregate snapshot
 * events (the ones WhatsApp Web batches from a scheduled daily-stats task) and emits
 * the interaction and error events under per-connect probability gates so any given
 * session reports an organic subset rather than an identical every-connect burst;
 * across sessions every event fires. Emission cadence is best-effort synthetic: the
 * daily events model the once-per-day WhatsApp Web task, the interaction events model
 * sporadic user actions.
 *
 * @implNote
 * This implementation draws every fabricated figure fresh on each connect through
 * the shared {@link SyntheticTelemetryUtils} helpers (which sample
 * {@link ThreadLocalRandom}) and mints every server-assigned identifier fresh per
 * interaction, so no value is a cross-connect constant that could itself fingerprint
 * the client. There is deliberately no session-stable seed: none of the events in
 * this group carries a fabricated setting that must recur identically across the
 * burst, so each value is an independent once-per-connect draw.
 *
 * @see WamService
 * @see com.github.auties00.cobalt.wam.LiveDailyStatsService
 */
@WhatsAppWebModule(moduleName = "WAWebUserNoticeWamEvent")
@WhatsAppWebModule(moduleName = "WAWebUserNoticeErrorWamEvent")
@WhatsAppWebModule(moduleName = "WAWebGatedChatOpenedWamEvent")
@WhatsAppWebModule(moduleName = "WAWebPrivacyHighlightDailyWamEvent")
@WhatsAppWebModule(moduleName = "WAWebViewOnceScreenshotActionsWamEvent")
@WhatsAppWebModule(moduleName = "WAWebPrivacySettingsClickWamEvent")
@WhatsAppWebModule(moduleName = "WAWebPnhDailyWamEvent")
@WhatsAppWebModule(moduleName = "WAWebChatLockDailyWamEvent")
@WhatsAppWebModule(moduleName = "WAWebLockFolderUnlockWamEvent")
@WhatsAppWebModule(moduleName = "WAWebPrivacyTipActionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebScreenLockSettingsDataWamEvent")
@WhatsAppWebModule(moduleName = "WAWebDangerousFileOpenStatsV2WamEvent")
@WhatsAppWebModule(moduleName = "WAWebPsFmxActionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebDefenseModeClickWamEvent")
@WhatsAppWebModule(moduleName = "WAWebDyiReportDownloadWamEvent")
@WhatsAppWebModule(moduleName = "WAWebDyiReportRequestWamEvent")
@WhatsAppWebModule(moduleName = "WAWebUsernameExposedWamEvent")
public final class SyntheticPrivacySafetyTelemetry {
    /**
     * The candidate app screen-lock auto-lock durations, in seconds, reported in
     * the {@code ScreenLockSettingsData} event.
     *
     * <p>The set mirrors the fixed options a real WhatsApp screen-lock offers:
     * immediately ({@code 0}), one minute ({@code 60}), fifteen minutes
     * ({@code 900}), and one hour ({@code 3600}).
     */
    private static final long[] SCREEN_LOCK_DURATIONS = {0L, 60L, 900L, 3600L};

    /**
     * The candidate exposure-context tokens reported in the {@code UsernameExposed}
     * event, naming the surface on which a username became visible.
     */
    private static final String[] USERNAME_EXPOSURE_CONTEXTS = {
            "profile", "contact_info", "message_info", "group_participant_list"
    };

    /**
     * The bound WhatsApp client whose store supplies the live contact, chat, and
     * account state sampled when populating the synthesised events.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The WAM service through which every synthesised privacy-and-safety event is
     * committed for batched upload.
     */
    private final WamService wamService;

    /**
     * Constructs a new {@code SyntheticPrivacySafetyTelemetry} bound to the given
     * client and WAM service.
     *
     * @param client     the WhatsApp client whose store is sampled, must not be
     *                   {@code null}
     * @param wamService the WAM service used to commit the emitted events, must not
     *                   be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SyntheticPrivacySafetyTelemetry(LinkedWhatsAppClient client, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
    }

    /**
     * Emits the synthetic privacy-and-safety snapshot for the current connect.
     *
     * <p>The daily-aggregate events ({@code PrivacyHighlightDaily}, {@code PnhDaily},
     * {@code ChatLockDaily}, {@code ScreenLockSettingsData}) are committed
     * unconditionally, modelling the once-per-day WhatsApp Web daily-stats task as a
     * per-connect heartbeat. The remaining interaction and error events are committed
     * under per-connect probability gates so a single session reports a plausible
     * organic subset rather than an identical every-connect burst; the
     * {@code UsernameExposed} event is additionally gated on the account actually
     * holding a reserved username. Across sessions every event in the group fires.
     *
     * @apiNote intended to be invoked once from the client's connect callback; it is
     *          cheap and non-blocking, reading only in-memory store state
     */
    public void emitPrivacySafetyTelemetry() {
        commitPrivacyHighlightDaily();
        commitPnhDaily();
        commitChatLockDaily();
        commitScreenLockSettingsData();

        if (SyntheticTelemetryUtils.chance(33)) {
            commitPrivacySettingsClick();
        }
        if (SyntheticTelemetryUtils.chance(25)) {
            commitPrivacyTipAction();
        }
        if (SyntheticTelemetryUtils.chance(16)) {
            commitDefenseModeClick();
        }
        if (SyntheticTelemetryUtils.chance(20)) {
            commitGatedChatOpened();
        }
        if (SyntheticTelemetryUtils.chance(25)) {
            commitViewOnceScreenshotActions();
        }
        if (SyntheticTelemetryUtils.chance(20)) {
            commitLockFolderUnlock();
        }
        if (SyntheticTelemetryUtils.chance(33)) {
            commitUserNotice();
        }
        if (SyntheticTelemetryUtils.chance(5)) {
            commitUserNoticeError();
        }
        if (SyntheticTelemetryUtils.chance(12)) {
            commitDangerousFileOpenStatsV2();
        }
        if (SyntheticTelemetryUtils.chance(25)) {
            commitPsFmxAction();
        }
        if (SyntheticTelemetryUtils.chance(8)) {
            commitDyiReportRequest();
        }
        if (SyntheticTelemetryUtils.chance(8)) {
            commitDyiReportDownload();
        }
        commitUsernameExposed();
    }

    /**
     * Builds and commits one {@code UserNotice} event describing a persistent
     * marketing-message disclosure (PDFN) notice lifecycle step.
     *
     * <p>WhatsApp Web logs this when a server-pushed disclosure notice is shown to
     * and progressed by the user. Cobalt has only the disclosure read RPCs and no
     * notice-rendering surface, so the notice identity, content version, and stage
     * are fabricated as a plausible shown-disclosure step with a sub-second display
     * latency recorded in {@code tsMs}.
     */
    private void commitUserNotice() {
        wamService.commit(new UserNoticeEventBuilder()
                .userNoticeId(noticeId())
                .userNoticeContentVersion(1)
                .noticeType(NoticeType.PDFN_DISCLOSURE)
                .noticeTriggeredBy(NoticeTriggeredBy.AUTO_START)
                .userNoticeEvent(oneOf(new UserNoticeEventType[]{
                        UserNoticeEventType.PDFN_SHOWN_0,
                        UserNoticeEventType.PDFN_SOFT_OPT_IN,
                        UserNoticeEventType.PDFN_OK
                }))
                .tsMs(Instant.ofEpochMilli(SyntheticTelemetryUtils.count(300, 4000)))
                .build());
    }

    /**
     * Builds and commits one {@code UserNoticeError} event describing a transient
     * failure fetching or parsing a disclosure notice's display content.
     *
     * <p>Emitted rarely by {@link #emitPrivacySafetyTelemetry()} because a genuine
     * client only logs this on an occasional content-fetch fault; the notice identity
     * and error kind are fabricated to mirror a benign transient fetch error.
     */
    private void commitUserNoticeError() {
        wamService.commit(new UserNoticeErrorEventBuilder()
                .userNoticeId(noticeId())
                .userNoticeContentVersion(1)
                .noticeType(NoticeType.PDFN_DISCLOSURE)
                .userNoticeErrorEvent(oneOf(new UserNoticeErrorEventType[]{
                        UserNoticeErrorEventType.JSON_FETCH,
                        UserNoticeErrorEventType.IMAGE_FETCH,
                        UserNoticeErrorEventType.JSON_FETCH_REDIRECT
                }))
                .build());
    }

    /**
     * Builds and commits one {@code GatedChatOpened} event describing the opening of
     * a chat that sits behind a gating interstitial.
     *
     * <p>The gating reason is fabricated as the terms-of-service acceptance gate and
     * the open is marked self-initiated, the common case for a user tapping into a
     * gated thread.
     */
    private void commitGatedChatOpened() {
        wamService.commit(new GatedChatOpenedEventBuilder()
                .chatGatedReason(ChatGatedReason.TOS3)
                .selfInitiated(true)
                .build());
    }

    /**
     * Builds and commits one {@code PrivacyHighlightDaily} event carrying the daily
     * aggregated appearance and selection counts for the E2EE privacy-highlight
     * surface.
     *
     * <p>WhatsApp Web commits this from its daily-stats task only when a golden-box
     * or info-screen narrative or dialog was shown; the fabricated appearance counts
     * are therefore kept at least one, and the selection count never exceeds the
     * dialog-appearance count.
     */
    private void commitPrivacyHighlightDaily() {
        var dialogAppear = SyntheticTelemetryUtils.count(1, 5);
        wamService.commit(new PrivacyHighlightDailyEventBuilder()
                .dialogAppearCount(dialogAppear)
                .dialogSelectCount(SyntheticTelemetryUtils.count(0, dialogAppear))
                .narrativeAppearCount(SyntheticTelemetryUtils.count(1, 8))
                .privacyHighlightCategory(PrivacyHighlightCategoryEnum.E2EE)
                .privacyHighlightSurface(oneOf(new PrivacyHighlightSurfaceEnum[]{
                        PrivacyHighlightSurfaceEnum.INFO_SCREEN_CONTACT,
                        PrivacyHighlightSurfaceEnum.INFO_SCREEN_GROUP,
                        PrivacyHighlightSurfaceEnum.CHATS_LIST,
                        PrivacyHighlightSurfaceEnum.CALLS_LIST
                }))
                .build());
    }

    /**
     * Builds and commits one {@code ViewOnceScreenshotActions} event describing a
     * screenshot action on a view-once message.
     *
     * <p>The thread identity and group flag are taken from a real chat when the store
     * holds one, so the reported {@code threadId} and {@code isAGroup} are genuine;
     * the view-once media type and screenshot action are fabricated as a photo whose
     * screenshot was taken.
     */
    private void commitViewOnceScreenshotActions() {
        var builder = new ViewOnceScreenshotActionsEventBuilder()
                .voMessageType(oneOf(new VoMessageType[]{
                        VoMessageType.PHOTO, VoMessageType.VIDEO, VoMessageType.PTT
                }))
                .voSsAction(oneOf(new VoSsAction[]{
                        VoSsAction.SCREENSHOT_TAKEN, VoSsAction.SCREENSHOT_BLOCKED
                }));
        firstChat().ifPresent(chat -> builder
                .threadId(chat.jid().toString())
                .isAGroup(chat.jid().hasGroupOrCommunityServer()));
        wamService.commit(builder.build());
    }

    /**
     * Builds and commits one {@code PrivacySettingsClick} event describing a tap on
     * the privacy-settings screen.
     *
     * <p>The entry point is fabricated as the privacy-settings surface and the tapped
     * control is drawn from the common privacy items a user browses.
     */
    private void commitPrivacySettingsClick() {
        wamService.commit(new PrivacySettingsClickEventBuilder()
                .privacyControlEntryPoint(PrivacyControlEntryPointType.PRIVACY_SETTINGS)
                .privacyControlItem(oneOf(new PrivacyControlItemType[]{
                        PrivacyControlItemType.LAST_SEEN_AND_ONLINE,
                        PrivacyControlItemType.PROFILE_PHOTO,
                        PrivacyControlItemType.ABOUT,
                        PrivacyControlItemType.GROUPS,
                        PrivacyControlItemType.READ_RECEIPT,
                        PrivacyControlItemType.DISAPPEARING_MESSAGES
                }))
                .build());
    }

    /**
     * Builds and commits one {@code PnhDaily} event carrying the daily
     * phone-number-hiding counters.
     *
     * <p>The total-contacts count and the missing phone-to-LID mapping count are
     * computed from live contact-store state; the indicator-click and reaction-tray
     * counters, which track UI interactions Cobalt does not surface, are fabricated
     * as small daily tallies. The group type is reported as a standard group.
     */
    private void commitPnhDaily() {
        var contactStore = client.store().contactStore();
        var contacts = contactStore.contacts();
        var mappingMissing = contacts.stream()
                .map(Contact::jid)
                .filter(Jid::hasUserServer)
                .filter(jid -> contactStore.findLidByPhone(jid).isEmpty())
                .count();
        wamService.commit(new PnhDailyEventBuilder()
                .totalContacts(contacts.size())
                .mappingMissing(mappingMissing)
                .pnhIndicatorClicksChat(SyntheticTelemetryUtils.count(0, 4))
                .pnhIndicatorClicksInfoScreen(SyntheticTelemetryUtils.count(0, 2))
                .reactionDeleteCount(SyntheticTelemetryUtils.count(0, 3))
                .reactionOpenTrayCount(SyntheticTelemetryUtils.count(0, 6))
                .typeOfGroup(TypeOfGroupEnum.GROUP)
                .build());
    }

    /**
     * Builds and commits one {@code ChatLockDaily} event carrying the daily
     * locked-chats folder aggregates.
     *
     * <p>The locked-folder chat count is computed from live chat-store state; the
     * folder-open, add, and remove counters and the folder-hidden and
     * secret-code-active flags, which track UI actions absent from a headless client,
     * are fabricated as a small daily snapshot.
     */
    private void commitChatLockDaily() {
        wamService.commit(new ChatLockDailyEventBuilder()
                .folderChatsCount(lockedChatCount())
                .folderOpenCount(SyntheticTelemetryUtils.count(0, 8))
                .newAddChatCount(SyntheticTelemetryUtils.count(0, 2))
                .newRemoveChatCount(SyntheticTelemetryUtils.count(0, 1))
                .lockFolderHidden(SyntheticTelemetryUtils.chance(50))
                .secretCodeActive(SyntheticTelemetryUtils.chance(50))
                .build());
    }

    /**
     * Builds and commits one {@code LockFolderUnlock} event describing an
     * authentication that opened the locked-chats folder.
     *
     * <p>The total locked-chat count is computed from live chat-store state; the
     * landing surface and unlock entry point are fabricated as an unlock that landed
     * on the folder from the chat list.
     */
    private void commitLockFolderUnlock() {
        wamService.commit(new LockFolderUnlockEventBuilder()
                .landingSurface(LandingSurface.FOLDER)
                .totalChatCount(lockedChatCount())
                .unlockEntryPoint(oneOf(new UnlockEntryPoint[]{
                        UnlockEntryPoint.CHAT_LIST,
                        UnlockEntryPoint.CHAT_INFO,
                        UnlockEntryPoint.SEARCH
                }))
                .build());
    }

    /**
     * Builds and commits one {@code PrivacyTipAction} event describing an action
     * taken on a privacy tip.
     *
     * <p>The action is drawn from the tip lifecycle a user walks through: viewing the
     * tip, tapping it, or dismissing it.
     */
    private void commitPrivacyTipAction() {
        wamService.commit(new PrivacyTipActionEventBuilder()
                .privacyTipActionType(oneOf(new PrivacyTipActionType[]{
                        PrivacyTipActionType.VIEW,
                        PrivacyTipActionType.CLICK_PRIVACY_TIP,
                        PrivacyTipActionType.CLICK_OK
                }))
                .build());
    }

    /**
     * Builds and commits one {@code ScreenLockSettingsData} event reporting the
     * configured app screen-lock auto-lock duration.
     *
     * <p>The duration is drawn from the fixed set of options a real screen-lock
     * offers ({@link #SCREEN_LOCK_DURATIONS}).
     */
    private void commitScreenLockSettingsData() {
        wamService.commit(new ScreenLockSettingsDataEventBuilder()
                .screenAutoLockDuration(SCREEN_LOCK_DURATIONS[ThreadLocalRandom.current().nextInt(SCREEN_LOCK_DURATIONS.length)])
                .build());
    }

    /**
     * Builds and commits one {@code DangerousFileOpenStatsV2} private-stats event
     * describing an interaction with the harmful-file open warning dialog.
     *
     * <p>The clickthrough action and the sender relationship are fabricated to mirror
     * a user deciding whether to open a flagged attachment from a contact or a
     * non-contact.
     */
    private void commitDangerousFileOpenStatsV2() {
        wamService.commit(new DangerousFileOpenStatsV2EventBuilder()
                .harmfulFileWarningClickthroughAction(oneOf(new HarmfulFileWarningClickthroughAction[]{
                        HarmfulFileWarningClickthroughAction.CANCEL,
                        HarmfulFileWarningClickthroughAction.OPEN,
                        HarmfulFileWarningClickthroughAction.LEARN_MORE
                }))
                .harmfulFileWarningSenderRelationship(oneOf(new HarmfulFileWarningSenderRelationship[]{
                        HarmfulFileWarningSenderRelationship.CONTACT,
                        HarmfulFileWarningSenderRelationship.NON_CONTACT
                }))
                .build());
    }

    /**
     * Builds and commits one {@code PsFmxAction} private-stats event describing the
     * display of a first-message-experience safety card when opening a chat.
     *
     * <p>The common-group count is fabricated as a small tally and the entry point,
     * event, and highlight-group type are fabricated to mirror a safety card being
     * viewed for a single non-contact sender; the sender-classification flags carry
     * plausible values for a first-contact scenario.
     */
    private void commitPsFmxAction() {
        wamService.commit(new PsFmxActionEventBuilder()
                .commonGroupNum(SyntheticTelemetryUtils.count(0, 5))
                .fmxEntryPoint(FmxEntryPoint.FMX_CARD)
                .fmxEvent(oneOf(new FmxEventType[]{
                        FmxEventType.FMX_CARD_INSERTED, FmxEventType.FMX_CARD_VIEWED
                }))
                .highlightGroupType(HighlightGroupType.SINGLE)
                .countryShown(SyntheticTelemetryUtils.chance(50))
                .isSenderSmb(SyntheticTelemetryUtils.chance(25))
                .isSuspiciousFmx(false)
                .newAccountShown(SyntheticTelemetryUtils.chance(33))
                .notAContactShown(true)
                .build());
    }

    /**
     * Builds and commits one {@code DefenseModeClick} event describing a tap on a
     * defense-mode account-protection control.
     *
     * <p>The control, accepted flag, and desired state are fabricated to mirror a
     * user accepting the main defense-mode toggle at its standard tier.
     */
    private void commitDefenseModeClick() {
        wamService.commit(new DefenseModeClickEventBuilder()
                .controlName(DefenseModeClickControlName.MAIN_CONTROL)
                .defenseModeClickAccepted(true)
                .desiredState(DefenseModeClickDesiredState.ON_STANDARD)
                .build());
    }

    /**
     * Builds and commits one {@code DyiReportDownload} event describing the outcome
     * of downloading a download-your-information report.
     *
     * <p>The outcome is fabricated as a successful account-report download; the error
     * message is left unset because success carries none.
     */
    private void commitDyiReportDownload() {
        wamService.commit(new DyiReportDownloadEventBuilder()
                .dyiDownloadSucceeded(true)
                .dyiReportType(DyiReportTypeCode.ACCOUNT)
                .build());
    }

    /**
     * Builds and commits one {@code DyiReportRequest} event describing a
     * download-your-information report request.
     *
     * <p>The report type and trigger are fabricated as an ad-hoc account-report
     * request, the shape a user-initiated request takes.
     */
    private void commitDyiReportRequest() {
        wamService.commit(new DyiReportRequestEventBuilder()
                .dyiReportType(DyiReportTypeCode.ACCOUNT)
                .dyiTriggerType(DyiTriggerTypeCode.ADHOC)
                .build());
    }

    /**
     * Builds and commits one {@code UsernameExposed} event naming the surface on
     * which the account's username became visible.
     *
     * <p>This is a no-op unless the account actually holds a reserved username: the
     * event only makes sense once a username exists, so it is gated on live
     * account-store state rather than fabricated unconditionally. The exposure
     * context is drawn from {@link #USERNAME_EXPOSURE_CONTEXTS}.
     */
    private void commitUsernameExposed() {
        if (client.store().accountStore().username().isEmpty()) {
            return;
        }
        wamService.commit(new UsernameExposedEventBuilder()
                .usernameExposureContext(oneOf(USERNAME_EXPOSURE_CONTEXTS))
                .build());
    }

    /**
     * Counts the chats currently held behind the Chat Lock feature.
     *
     * @return the number of locked chats in the store
     */
    private long lockedChatCount() {
        return client.store()
                .chatStore()
                .chats()
                .stream()
                .filter(Chat::locked)
                .count();
    }

    /**
     * Returns any one chat from the store, preferring a stable arbitrary element.
     *
     * @return the first chat the store yields, or empty when no chats exist
     */
    private Optional<Chat> firstChat() {
        return client.store()
                .chatStore()
                .chats()
                .stream()
                .findFirst();
    }

    /**
     * Fabricates a plausible server-assigned user-notice identifier.
     *
     * <p>The value lands in the eight-digit range real disclosure notice ids occupy
     * and is minted fresh on every call through
     * {@link SyntheticTelemetryUtils#between(long, long)}, mirroring how WhatsApp
     * re-mints a notice identifier for each notice rather than reusing a
     * cross-connect constant that would fingerprint the client.
     *
     * @return a freshly fabricated user-notice identifier
     */
    private static long noticeId() {
        return SyntheticTelemetryUtils.between(10_000_000L, 99_999_999L);
    }

    /**
     * Returns a pseudo-random element of the given array.
     *
     * @param options the candidate values, must be non-empty
     * @param <T>     the element type
     * @return one element of {@code options} drawn from
     *         {@link ThreadLocalRandom#current()}
     */
    private static <T> T oneOf(T[] options) {
        return options[ThreadLocalRandom.current().nextInt(options.length)];
    }
}
