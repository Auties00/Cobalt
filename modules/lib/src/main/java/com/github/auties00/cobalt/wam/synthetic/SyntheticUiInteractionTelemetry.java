package com.github.auties00.cobalt.wam.synthetic;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.AttachmentTrayActionsEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.BannerEventEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.BugReportSessionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ChatFilterEventEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ChatFolderOpenEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ContactUsSessionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.DialogEventEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.GuestUpsellInteractionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.SearchActionEventEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.SearchUserJourneyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.UiActionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebcButterbarEventEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebcMenuEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebcNativeUpsellCtaEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebcNavbarEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebcWhatsNewImpressionEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.*;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Emits the block of WhatsApp Web user-interface interaction WAM beacons that a
 * real Web or Desktop session produces from its chrome (navigation bar, header
 * dropdown menu, banners, dialogs, search bar, attachment tray, upsell CTAs,
 * bug-report flow, and the "What's New" panel) but that a headless Cobalt client
 * has no rendered surface to generate.
 *
 * <p>None of the sixteen events wired here map to a Cobalt feature: they measure
 * clicks, impressions, and render latencies on a graphical client. A Cobalt
 * session that never emitted any of them would carry a telemetry fingerprint
 * trivially distinguishable from a genuine WhatsApp Web session, so this service
 * synthesises one plausible, self-consistent occurrence of each and commits them
 * through {@link WamService#commit(com.github.auties00.cobalt.wam.model.WamEventSpec)}.
 * Every event is populated with real store-derived values wherever the datum
 * exists (locale-derived language code, LID/PN addressing mode, device count,
 * and chat, contact, group, and label counts sampled from the live store) and
 * with realistic fabricated constants for the purely presentational fields a
 * headless client cannot source (surface names, dialog names, banner ids,
 * render-latency timers, session identifiers).
 *
 * <p>The single public entrypoint is {@link #emitSessionTelemetry()}. Each event
 * is committed exactly once per invocation; the intended cadence is once per
 * successful connection (a real UI produces these throughout a session, but a
 * single representative sample per connect is sufficient to keep the stream
 * shaped like a genuine client without over-reporting).
 *
 * @see WamService
 * @see com.github.auties00.cobalt.wire.wam.event.UiActionEvent
 */
public final class SyntheticUiInteractionTelemetry {
    /**
     * The default language subtag reported when the bound account carries no
     * locale, matching the {@code en} fallback a fresh Web session would report.
     */
    private static final String DEFAULT_LANGUAGE_CODE = "en";

    /**
     * The upper bound applied to store-derived result counts before they are
     * reported in the search beacons, so a synthetic typeahead or result page
     * advertises a UI-plausible handful of rows rather than the account's entire
     * chat or contact total.
     */
    private static final int MAX_SEARCH_RESULTS = 6;

    /**
     * The bound WhatsApp client whose store supplies the live account, chat,
     * contact, and settings state sampled when populating the synthetic events.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The WAM service through which every synthesised UI-interaction event is
     * committed for batched upload.
     */
    private final WamService wamService;

    /**
     * The stable per-instance session identifier reused across the emitted
     * events as their string session/activity/unified-session fields.
     *
     * <p>It is minted once at construction and shared so the synthesised beacons
     * correlate to a single session the way a real client's UI events would,
     * mirroring WA Web's per-tab {@code appSessionId}.
     */
    private final UUID sessionUuid;

    /**
     * The stable per-instance device identifier reported as the
     * {@code deviceId} of the synthesised banner event.
     *
     * <p>It is minted once at construction so repeated emissions from the same
     * service instance carry a consistent device identity.
     */
    private final UUID deviceUuid;

    /**
     * Constructs a new {@code SyntheticUiInteractionTelemetry} bound to the given
     * client and WAM service.
     *
     * @param client     the WhatsApp client whose store is sampled, must not be
     *                   {@code null}
     * @param wamService the WAM service used to commit the emitted events, must
     *                   not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SyntheticUiInteractionTelemetry(LinkedWhatsAppClient client, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
        this.sessionUuid = UUID.randomUUID();
        this.deviceUuid = UUID.randomUUID();
    }

    /**
     * Emits one representative occurrence of every synthetic UI-interaction
     * beacon carried by this service.
     *
     * <p>This is the single entrypoint the client drives once per successful
     * connection. It commits the navigation, menu, banner, dialog, search,
     * attachment-tray, upsell, bug-report, folder, and "What's New" events in
     * turn; each helper fabricates one self-consistent occurrence and commits it
     * through {@link WamService}. There is no natural recurring cadence because a
     * headless client raises no UI events, so a single per-connect sample stands
     * in for the stream a rendered client would produce continuously.
     */
    public void emitSessionTelemetry() {
        emitContactUsSession();
        emitUiAction();
        emitBannerEvent();
        emitChatFilterEvent();
        emitWebcMenu();
        emitChatFolderOpen();
        emitBugReportSession();
        emitWebcButterbar();
        emitWebcNativeUpsellCta();
        emitAttachmentTrayActions();
        emitWebcNavbar();
        emitSearchAction();
        emitSearchUserJourney();
        emitDialogEvent();
        emitGuestUpsellInteraction();
        emitWebcWhatsNewImpression();
    }

    /**
     * Synthesises the end of a Contact Us help-center session (event id 470).
     *
     * <p>The fabricated session models a user who browsed the in-app FAQ, viewed
     * a suggested article, attached a diagnostic log, and closed the flow from
     * the FAQ screen. The language code is sourced from the live account locale.
     */
    @WhatsAppWebExport(moduleName = "WAWebContactUsSessionWamEvent", exports = "ContactUsSessionWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitContactUsSession() {
        wamService.commit(new ContactUsSessionEventBuilder()
                .contactUsExitState(ContactUsExitState.IN_APP_FAQ)
                .contactUsFaq(true)
                .contactUsMenuFaqT(Instant.ofEpochMilli(3_500))
                .contactUsAutomaticEmail(false)
                .contactUsLogs(true)
                .contactUsOutage(false)
                .contactUsOutageEmail(false)
                .contactUsScreenshotC(1.0d)
                .contactUsT(Instant.ofEpochMilli(18_000))
                .languageCode(languageCode())
                .build());
    }

    /**
     * Synthesises the chat-open render-latency UiAction beacon (event id 472).
     *
     * <p>The fabricated action models opening a one-to-one chat: the addressing
     * mode, LID flag, and device count are sourced from the live account, while
     * the render-latency timers, database counters, and surface names are
     * plausible constants a rendered client would report for a warm chat open.
     */
    @WhatsAppWebExport(moduleName = "WAWebUiActionWamEvent", exports = "UiActionWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitUiAction() {
        wamService.commit(new UiActionEventBuilder()
                .uiActionType(UiActionType.CHAT_OPEN)
                .uiActionChatType(UiActionChatType.INDIVIDUAL)
                .uiActionT(Instant.ofEpochMilli(142))
                .uiActionPreloaded(false)
                .isLowPowerMode(false)
                .sizeBucket(SizeBucket.LT128)
                .participantCount(2)
                .deviceCount(deviceCount())
                .isLid(hasLid())
                .localAddressingMode(addressingMode())
                .uiActionTtrcSurfaceName("chat")
                .uiActionPresentationSource("chatlist")
                .dbMainThreadCount(3)
                .dbReadsCount(11)
                .dbWritesCount(2)
                .dbMainThreadReadsDurationT(Instant.ofEpochMilli(6))
                .dbMainThreadWritesDurationT(Instant.ofEpochMilli(2))
                .dbBgThreadReadsDurationT(Instant.ofEpochMilli(9))
                .dbBgThreadWritesDurationT(Instant.ofEpochMilli(4))
                .traceIdInt(traceId())
                .appSessionId(sessionId())
                .unifiedSessionId(sessionId())
                .build());
    }

    /**
     * Synthesises a banner impression beacon (event id 1578).
     *
     * <p>The fabricated event models the privacy-checkup NUX banner being shown
     * on the chat list, carrying the stable device identifier and a fresh
     * per-emission notification log id.
     */
    @WhatsAppWebExport(moduleName = "WAWebBannerEventWamEvent", exports = "BannerEventWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitBannerEvent() {
        wamService.commit(new BannerEventEventBuilder()
                .bannerType(BannerTypes.PRIVACY_CHECKUP)
                .bannerOperation(BannerOperations.SHOWN)
                .bannerId("privacy_checkup")
                .deviceId(deviceUuid.toString())
                .notificationLogId(SyntheticTelemetryUtils.newSessionId())
                .build());
    }

    /**
     * Synthesises a chat-list filter selection beacon (event id 1616).
     *
     * <p>The fabricated event models the user tapping the predefined "Unread"
     * filter on the chat list, correlated to this instance's session id.
     */
    @WhatsAppWebExport(moduleName = "WAWebChatFilterEventWamEvent", exports = "ChatFilterEventWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitChatFilterEvent() {
        wamService.commit(new ChatFilterEventEventBuilder()
                .actionType(ChatFilterActionTypes.SELECT_FILTER)
                .filterType(ChatFilterTypes.UNREAD)
                .targetScreen(ChatFilterTargetScreen.CHAT_LIST)
                .listType(ListType.PREDEFINED)
                .predefinedId(1)
                .listId(1)
                .listIndex(0)
                .sessionId(numericSessionId())
                .activitySessionId(sessionId())
                .build());
    }

    /**
     * Synthesises a chat-list header dropdown menu selection beacon (event id 2504).
     *
     * <p>The fabricated event models opening the chat-list header dropdown and
     * choosing the "New group" item.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcMenuWamEvent", exports = "WebcMenuWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitWebcMenu() {
        wamService.commit(new WebcMenuEventBuilder()
                .webcMenuAction(WebcMenuType.THREADS_SCREEN_CLICK)
                .webcMenuItemLabel(WebcMenuItemLabel.NEW_GROUP)
                .build());
    }

    /**
     * Synthesises a chat-folder open beacon (event id 2808).
     *
     * <p>The fabricated event models opening the "Unread" folder tab; the
     * activity-indicator badge count is derived from the live chat total, capped
     * to a UI-plausible value.
     */
    @WhatsAppWebExport(moduleName = "WAWebChatFolderOpenWamEvent", exports = "ChatFolderOpenWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitChatFolderOpen() {
        wamService.commit(new ChatFolderOpenEventBuilder()
                .folderType("unread")
                .activityIndicatorCount(Math.min(chatCount(), 12))
                .hasImportantMessages(false)
                .build());
    }

    /**
     * Synthesises a bug-report flow session beacon (event id 3850).
     *
     * <p>The fabricated event models a successful bug-report submission opened
     * from settings with one screenshot attached and a short description.
     */
    @WhatsAppWebExport(moduleName = "WAWebBugReportSessionWamEvent", exports = "BugReportSessionWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitBugReportSession() {
        wamService.commit(new BugReportSessionEventBuilder()
                .bugReportFlowAction(BugReportFlowAction.SUBMISSION_SUCCESSFUL)
                .bugReportingEntryPoint(BugReportEntryPointName.BUG_REPORT_ENTRY_POINT_SETTINGS)
                .bugReportingEndpoint("wa_bug_report")
                .submitBugCategory("connectivity")
                .submitBugContainsTitle(true)
                .bugReportMediaCount(1)
                .bugReportImageCount(1)
                .bugReportVideoCount(0)
                .bugReportNumberOfChars(118)
                .bugReportNumberOfWords(21)
                .bugReportTaskId(SyntheticTelemetryUtils.newSessionId())
                .clientServerJoinKey(SyntheticTelemetryUtils.newSessionId())
                .build());
    }

    /**
     * Synthesises a butterbar notification banner beacon (event id 3932).
     *
     * <p>The fabricated event models an impression of the top notification
     * butterbar prompting the user to enable desktop notifications.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcButterbarEventWamEvent", exports = "WebcButterbarEventWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitWebcButterbar() {
        wamService.commit(new WebcButterbarEventEventBuilder()
                .webcButterbarAction(WebcButterbarActionType.IMPRESSION)
                .webcButterbarType(WebcButterbarBbType.NOTIFICATION)
                .build());
    }

    /**
     * Synthesises a native desktop-app upsell CTA beacon (event id 3934).
     *
     * <p>The fabricated event models an impression of the "download the desktop
     * app" upsell rendered in the intro panel on the production release channel.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcNativeUpsellCtaWamEvent", exports = "WebcNativeUpsellCtaWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitWebcNativeUpsellCta() {
        wamService.commit(new WebcNativeUpsellCtaEventBuilder()
                .webcNativeUpsellCtaSource(WebcNativeUpsellCtaSourceType.INTRO_PANEL)
                .webcNativeUpsellCtaEventType(WebcNativeUpsellCtaEventType.IMPRESSION)
                .webcNativeUpsellCtaQrScreenExperimentGroup(WebcNativeUpsellCtaQrScreenExperimentGroup.CONTROL)
                .webcNativeUpsellCtaReleaseChannel(WebcNativeUpsellCtaReleaseChannel.PRODUCTION)
                .webcNativeUpsellCtaIsBetaUser(false)
                .build());
    }

    /**
     * Synthesises a composer attachment-tray action beacon (event id 3980).
     *
     * <p>The fabricated event models a successful photo send from the
     * photo-and-video library within a one-to-one thread.
     */
    @WhatsAppWebExport(moduleName = "WAWebAttachmentTrayActionsWamEvent", exports = "AttachmentTrayActionsWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitAttachmentTrayActions() {
        wamService.commit(new AttachmentTrayActionsEventBuilder()
                .attachmentTrayAction(AttachmentTrayActionType.SEND)
                .attachmentTrayActionTarget(AttachmentTrayActionTargetType.PHOTO_AND_VIDEO_LIBRARY)
                .actionThreadType(ActionThreadTypeType.P2P_THREAD)
                .groupSizeBucket(ClientGroupSizeBucket.SMALL)
                .isAGroup(false)
                .isSuccessful(true)
                .sendMediaType(SendMediaTypeType.PHOTO)
                .actionDurationMs(820)
                .sendTime(310)
                .build());
    }

    /**
     * Synthesises a side navigation-bar tap beacon (event id 5258).
     *
     * <p>The fabricated event models the user tapping the "Chats" navbar item.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcNavbarWamEvent", exports = "WebcNavbarWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitWebcNavbar() {
        wamService.commit(new WebcNavbarEventBuilder()
                .webcNavbarItemLabel(WebcNavbarItemLabel.CHATS)
                .build());
    }

    /**
     * Synthesises a chat-list search action beacon (event id 5308).
     *
     * <p>The fabricated event models a typeahead result surfacing from the chat
     * list; the chat, contact, and group counts are sampled from the live store
     * and capped to a UI-plausible number of surfaced rows.
     */
    @WhatsAppWebExport(moduleName = "WAWebSearchActionEventWamEvent", exports = "SearchActionEventWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitSearchAction() {
        wamService.commit(new SearchActionEventEventBuilder()
                .searchAction(SearchActionType.TYPEAHEAD_SHOW)
                .searchActionEntryPoint(SearchActionEntryPointType.CHATS_LIST)
                .resultPageShown(true)
                .searchChatsCount(cappedResults(chatCount()))
                .searchContactsCount(cappedResults(contactCount()))
                .searchGroupsCount(cappedResults(groupCount()))
                .searchMessagesCount(4)
                .searchAiSuggestionCount(1)
                .searchFilterCount(cappedResults(labelCount()))
                .bizSearchCount(0)
                .selectedItemRank(2)
                .build());
    }

    /**
     * Synthesises a global-search user-journey beacon (event id 6358).
     *
     * <p>The fabricated event models a result page being shown for a global
     * search from the chat list; the chat, contact, group, and message result
     * counts are sampled from the live store and capped, and the journey is
     * correlated to this instance's session id.
     */
    @WhatsAppWebExport(moduleName = "WAWebSearchUserJourneyWamEvent", exports = "SearchUserJourneyWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitSearchUserJourney() {
        var chats = cappedResults(chatCount());
        var contacts = cappedResults(contactCount());
        var messages = 4L;
        wamService.commit(new SearchUserJourneyEventBuilder()
                .searchUserJourneyAction(SearchUserJourneyAction.RESULT_PAGE_SHOW)
                .searchUseCase(SearchUseCase.GLOBAL_SEARCH)
                .uiSurface(TsSurface.CHAT_LIST)
                .searchUjItemType(SearchUjItemType.CHAT)
                .searchUjCriteriaType(SearchUjCriteriaType.TEXT_MATCH_FILTER)
                .searchUjChatsCount(chats)
                .searchUjContactsCount(contacts)
                .searchUjGroupsInCommonCount(cappedResults(groupCount()))
                .searchUjMessagesCount(messages)
                .searchUjBizCount(0)
                .searchUjAiSuggestionCount(1)
                .searchUjFilterCount(0)
                .searchUjResultCount(chats + contacts + messages)
                .searchUjSelectedItemRank(1)
                .searchUjHasFuzzyResults(false)
                .searchHasSemanticSearchResults(false)
                .searchFtsMessagesCount(messages)
                .searchSemanticMessagesCount(0)
                .searchFtsAndSemanticMessagesCount(0)
                .searchUjPushnamesCount(0)
                .searchUjRecentSearchesIndividualCount(0)
                .searchUjRecentSearchesGroupCount(0)
                .userJourneyEventMs(Instant.now().toEpochMilli())
                .userJourneyFunnelId(SyntheticTelemetryUtils.newSessionId())
                .unifiedSessionId(sessionId())
                .searchUniqueSessionId(sessionId())
                .searchSessionQueryId(SyntheticTelemetryUtils.newSessionId())
                .build());
    }

    /**
     * Synthesises a generic UI dialog interaction beacon (event id 7068).
     *
     * <p>The fabricated event models the user clicking within the logout
     * confirmation dialog opened from settings.
     */
    @WhatsAppWebExport(moduleName = "WAWebDialogEventWamEvent", exports = "DialogEventWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitDialogEvent() {
        wamService.commit(new DialogEventEventBuilder()
                .dialogName(DialogNameType.LOGOUT)
                .dialogEventType(DialogEventType.CLICK)
                .dialogEventSource("settings")
                .build());
    }

    /**
     * Synthesises a guest-mode upsell interaction beacon (event id 7146).
     *
     * <p>The fabricated event models a guest user viewing the download-app
     * upsell prompt on the landing screen.
     */
    @WhatsAppWebExport(moduleName = "WAWebGuestUpsellInteractionWamEvent", exports = "GuestUpsellInteractionWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitGuestUpsellInteraction() {
        wamService.commit(new GuestUpsellInteractionEventBuilder()
                .guestUpsellAction(GuestUpsellActionType.VIEW)
                .guestUpsellEntryPoint(GuestUpsellEntryPointType.LANDING_SCREEN_DOWNLOAD_CTA)
                .build());
    }

    /**
     * Synthesises a "What's New" panel impression beacon (event id 8200).
     *
     * <p>The fabricated event models the modal "What's New" announcement panel
     * being shown, with a plausible dwell time and variant id.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcWhatsNewImpressionWamEvent", exports = "WebcWhatsNewImpressionWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitWebcWhatsNewImpression() {
        wamService.commit(new WebcWhatsNewImpressionEventBuilder()
                .webcWhatsNewAction(WebcWhatsNewActionType.IMPRESSION)
                .webcWhatsNewSurface(WebcWhatsNewSurfaceType.MODAL)
                .webcWhatsNewTimeSpent(Instant.ofEpochMilli(4_200))
                .webcWhatsNewVariant(3)
                .build());
    }

    /**
     * Returns the shared per-instance session identifier as a string.
     *
     * <p>It is reused across the emitted events as their string session,
     * activity-session, and unified-session fields so the synthesised beacons
     * correlate to one session.
     *
     * @return the session UUID rendered as a string
     */
    private String sessionId() {
        return sessionUuid.toString();
    }

    /**
     * Returns a positive numeric session identifier derived from the shared
     * per-instance session UUID.
     *
     * <p>It masks off the sign bit of the UUID's most-significant longword so the
     * integer-typed session fields receive a stable, non-negative value.
     *
     * @return the non-negative numeric session identifier
     */
    private long numericSessionId() {
        return sessionUuid.getMostSignificantBits() & Long.MAX_VALUE;
    }

    /**
     * Returns a positive trace identifier for the UiAction beacon.
     *
     * <p>It masks off the sign bit of a fresh random longword so each emission
     * carries a distinct, non-negative trace id the way a rendered client's
     * per-action trace would.
     *
     * @return the non-negative trace identifier
     */
    private long traceId() {
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
    }


    /**
     * Returns the lowercase language subtag of the bound account's locale.
     *
     * <p>For a locale tag such as {@code "it-IT"} this returns {@code "it"};
     * when the account carries no locale it returns {@link #DEFAULT_LANGUAGE_CODE}.
     *
     * @return the reported language code
     */
    private String languageCode() {
        return client.store()
                .accountStore()
                .locale()
                .map(tag -> {
                    var dash = tag.indexOf('-');
                    var language = dash >= 0 ? tag.substring(0, dash) : tag;
                    return language.toLowerCase(Locale.ROOT);
                })
                .orElse(DEFAULT_LANGUAGE_CODE);
    }

    /**
     * Returns whether the bound account has a resolved LID identity.
     *
     * @return {@code true} when the account carries a LID, {@code false} otherwise
     */
    private boolean hasLid() {
        return client.store().accountStore().lid().isPresent();
    }

    /**
     * Returns the addressing mode the bound account currently uses.
     *
     * @return {@link AddressingMode#LID} when the account has a LID, otherwise
     *         {@link AddressingMode#PN}
     */
    private AddressingMode addressingMode() {
        return hasLid() ? AddressingMode.LID : AddressingMode.PN;
    }

    /**
     * Returns the number of devices attached to the bound account, including the
     * account's own device.
     *
     * @return the linked-device count plus one for the primary device
     */
    private long deviceCount() {
        return client.store().accountStore().linkedDevices().size() + 1L;
    }

    /**
     * Returns the number of chats held in the live store.
     *
     * @return the chat count
     */
    private int chatCount() {
        return client.store().chatStore().chats().size();
    }

    /**
     * Returns the number of group and community chats held in the live store.
     *
     * @return the count of chats whose JID targets the group-or-community server
     */
    private long groupCount() {
        return client.store()
                .chatStore()
                .chats()
                .stream()
                .filter(chat -> chat.jid().hasGroupOrCommunityServer())
                .count();
    }

    /**
     * Returns the number of contacts held in the live store.
     *
     * @return the contact count
     */
    private int contactCount() {
        return client.store().contactStore().contacts().size();
    }

    /**
     * Returns the number of labels configured in the live store.
     *
     * @return the label count
     */
    private int labelCount() {
        return client.store().settingsStore().labels().size();
    }

    /**
     * Clamps a live store total to the maximum number of rows a synthetic search
     * beacon reports as surfaced results.
     *
     * @param value the raw store total
     * @return {@code value} clamped to {@link #MAX_SEARCH_RESULTS}
     */
    private long cappedResults(long value) {
        return Math.min(value, MAX_SEARCH_RESULTS);
    }
}
