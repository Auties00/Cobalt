package com.github.auties00.cobalt.wam.synthetic;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.AboutConsumptionDailyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.AboutConsumptionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.AboutCreationDailyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.AboutInteractionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.StatusItemViewEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.StatusPostImpressionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.StatusRowViewEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.StatusViewerActionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.UpdatesTabSearchEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebcStatusSessionEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.AboutConsumptionSurfaceType;
import com.github.auties00.cobalt.wire.wam.type.AboutEntrypointType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.PairedMediaType;
import com.github.auties00.cobalt.wire.wam.type.StatusCategory;
import com.github.auties00.cobalt.wire.wam.type.StatusContentType;
import com.github.auties00.cobalt.wire.wam.type.StatusItemViewResult;
import com.github.auties00.cobalt.wire.wam.type.StatusPairedMediaQuality;
import com.github.auties00.cobalt.wire.wam.type.StatusPosterContactType;
import com.github.auties00.cobalt.wire.wam.type.StatusRowEntryMethod;
import com.github.auties00.cobalt.wire.wam.type.StatusRowSection;
import com.github.auties00.cobalt.wire.wam.type.StatusType;
import com.github.auties00.cobalt.wire.wam.type.StatusViewActionType;
import com.github.auties00.cobalt.wire.wam.type.StatusViewEntryMethod;
import com.github.auties00.cobalt.wire.wam.type.StatusViewExitMethod;
import com.github.auties00.cobalt.wire.wam.type.TopBarAttributionType;
import com.github.auties00.cobalt.wire.wam.type.UpdateTabSearchEventType;
import com.github.auties00.cobalt.wire.wam.type.UpdatesTabSearchModeType;
import com.github.auties00.cobalt.wire.wam.type.UpdatesTabSearchResultType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;

/**
 * Emits the synthetic status-tab and profile-About WAM telemetry a genuine
 * WhatsApp Web session produces, so a Cobalt session's metrics stream carries
 * the same status-viewer and About-surface events a real browser reports.
 *
 * <p>WhatsApp Web instruments its Updates tab and profile About surfaces with a
 * family of UI-interaction beacons that a headless client never fires because it
 * has no status tray, no full-screen status viewer, no Updates-tab search box and
 * no About consumption bubble: the status-row impression
 * ({@code StatusRowView}), the per-item status-viewer telemetry
 * ({@code StatusItemView}), the status-tab session summary
 * ({@code WebcStatusSession}), the Updates-tab search beacon
 * ({@code UpdatesTabSearch}), the Merlin status/channel-post impression
 * ({@code StatusPostImpression}), the in-viewer action beacon
 * ({@code StatusViewerAction}), and the profile About consumption, interaction
 * and daily-funnel counters ({@code AboutConsumption}, {@code AboutInteraction},
 * {@code AboutConsumptionDaily}, {@code AboutCreationDaily}). None of these map
 * to a Cobalt feature; their total absence from a session is itself a
 * fingerprint, so this service fabricates one plausible, coherent status-viewing
 * session per connection and reports the About counters a browser would coarsen
 * into its daily-stats block.
 *
 * <p>Every field WA sets is populated with either a real store-sourced value
 * (the recent status item and row counts derived from the live status
 * collection, the distinct-poster row count, the followed-channel count from the
 * newsletter collection, the account locale) or a realistic constant or
 * host-jittered value captured from a genuine desktop WA Web status view (photo
 * status dimensions, load and view timers, view and impression counts, poster
 * contact type). No obviously-fake sentinel values are used; the PSA-campaign,
 * inline-video, URL-status and channel-only fields are left unset because a plain
 * contact photo status genuinely carries none of them, exactly as a real viewer
 * would report.
 *
 * @implNote
 * This implementation fires the whole burst from a single
 * {@link #emitSessionTelemetry()} entry point on a dedicated virtual thread,
 * minting one shared set of correlation ids (status session, viewer session,
 * Updates-tab session, unified session and Updates-tab search session) so the
 * row-view, item-view, post-impression and search events cross-reference as they
 * would in a real status-viewing session, exactly as the browser-perf burst
 * shares one page-load id across its correlated events.
 *
 * @see WamService
 * @see SyntheticBrowserPerfTelemetry
 */
@WhatsAppWebModule(moduleName = "WAWebStatusCollection")
@WhatsAppWebModule(moduleName = "WAWebMerlinImpressionManager")
@WhatsAppWebModule(moduleName = "WAWebNewsletterInboxSearchLogging")
@WhatsAppWebModule(moduleName = "WAWebAboutWamLogger")
@WhatsAppWebModule(moduleName = "WAWebTasksDailyStatsTask")
public final class SyntheticStatusTelemetry {
    /**
     * The bound WhatsApp client whose store supplies the live status collection,
     * newsletter collection and account locale sampled into the fabricated events.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The WAM service through which every fabricated status and About event is
     * committed for batched upload.
     */
    private final WamService wamService;

    /**
     * Constructs a new {@code SyntheticStatusTelemetry} bound to the given client
     * and WAM service.
     *
     * @param client     the WhatsApp client whose store is sampled, must not be
     *                   {@code null}
     * @param wamService the WAM service used to commit the emitted events, must
     *                   not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SyntheticStatusTelemetry(LinkedWhatsAppClient client, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
    }

    /**
     * Emits the once-per-connection status-tab and About telemetry burst on a
     * dedicated virtual thread.
     *
     * <p>This is the single entry point the client drives after the socket opens.
     * It fabricates and commits one coherent status-viewing session: the status
     * row-view impression, the per-item status-viewer telemetry, the status-tab
     * session summary, the Updates-tab search beacon, the Merlin post-impression,
     * the in-viewer action, and the profile About consumption, interaction and
     * daily-funnel counters, all sharing one set of correlation ids.
     *
     * <p>The work runs off the socket-open thread because deriving the recent item
     * and distinct-poster row counts walks the live status collection; the WAM
     * commits themselves are cheap enqueues subject to the usual
     * {@link WamService} sampling.
     *
     * @apiNote
     * The two About daily-aggregate events ({@code AboutConsumptionDaily} and
     * {@code AboutCreationDaily}) are periodic by nature on WA Web, where its
     * daily-stats task coarsens per-day About-bubble tap and About-creation funnel
     * counters and reports them at most once per rolling day. Cobalt has no such
     * scheduler in this service, so it folds one plausible daily snapshot into the
     * per-connection burst; a caller that wants the true daily cadence may invoke
     * this method again on a twenty-four hour timer.
     */
    public void emitSessionTelemetry() {
        Thread.ofVirtual()
                .name("status-telemetry")
                .start(this::runBurst);
    }

    /**
     * Builds and commits the full status-tab and About telemetry burst.
     *
     * <p>The store is sampled once for the recent status item count, the
     * distinct-poster row count and the followed-channel count, from which the
     * viewed and muted sub-counts are derived; one status-viewing session is then
     * minted and threaded through every correlated event so they share a
     * consistent set of session ids and a common row index.
     */
    private void runBurst() {
        var session = sampleSession();

        commitStatusRowView(session);
        commitStatusItemView(session);
        commitStatusSession(session);
        commitUpdatesTabSearch(session);
        commitStatusPostImpression(session);
        commitStatusViewerAction();

        var locale = aboutLocale();
        commitAboutConsumption();
        commitAboutInteraction(locale);
        commitAboutConsumptionDaily(locale);
        commitAboutCreationDaily(locale);
    }

    /**
     * Samples the live store and mints the shared correlation ids and counts for
     * one fabricated status-viewing session.
     *
     * <p>The recent item and row counts prefer the real store figures (the size of
     * the status collection and its distinct-poster count) and fall back to a
     * plausible floor when the store holds no status, so an empty client still
     * reports a realistic tray rather than zeros. The viewed sub-counts are derived
     * as roughly two thirds of the recent counts, the muted sub-counts as a small
     * jittered remainder, and the followed-channel count prefers the real
     * newsletter total over a plausible floor.
     *
     * @return the minted status-viewing session carrier
     */
    private StatusSession sampleSession() {
        var chatStore = client.store().chatStore();
        var statusItems = chatStore.status();

        var recentRowCount = Math.max(distinctPosters(statusItems), SyntheticTelemetryUtils.jitter(3, 6));
        var recentItemCount = Math.max(statusItems.size(), recentRowCount + SyntheticTelemetryUtils.jitter(2, 8));
        var viewedRowCount = Math.max(1, recentRowCount * 2 / 3);
        var viewedItemCount = Math.max(viewedRowCount, recentItemCount * 2 / 3);
        var mutedRowCount = SyntheticTelemetryUtils.jitter(0, 2);
        var mutedItemCount = mutedRowCount == 0 ? 0 : mutedRowCount + SyntheticTelemetryUtils.jitter(0, 3);
        var followedChannelCount = Math.max(chatStore.newsletters().size(), SyntheticTelemetryUtils.jitter(1, 5));

        return new StatusSession(
                DataUtils.randomLong(1, Long.MAX_VALUE),
                DataUtils.randomLong(1, Long.MAX_VALUE),
                DataUtils.randomLong(1, Long.MAX_VALUE),
                SyntheticTelemetryUtils.randomHexLower(16),
                SyntheticTelemetryUtils.randomHexLower(8),
                SyntheticTelemetryUtils.jitter(0, 4),
                recentItemCount,
                recentRowCount,
                viewedItemCount,
                viewedRowCount,
                mutedItemCount,
                mutedRowCount,
                followedChannelCount);
    }

    /**
     * Fabricates and commits the {@code StatusRowView} (id 1656) status-tray
     * row-view impression.
     *
     * <p>The event describes a direct tap on a recent-stories row: the entry
     * method, the shared row index, the recent-stories section, the row's unread
     * and total item counts derived from the session, and the shared status,
     * viewer, unified and Updates-tab session ids. The PSA-campaign field is left
     * unset because a plain contact status row carries no public-service-announcement
     * campaign, matching what a real non-PSA row reports.
     *
     * @param session the shared status-viewing session
     */
    @WhatsAppWebExport(moduleName = "WAWebStatusRowViewWamEvent", exports = "StatusRowViewWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitStatusRowView(StatusSession session) {
        wamService.commit(new StatusRowViewEventBuilder()
                .statusRowEntryMethod(StatusRowEntryMethod.DIRECT_ROW_TAP)
                .statusRowSection(StatusRowSection.RECENT_STORIES)
                .statusRowIndex(session.rowIndex())
                .statusRowViewCount(SyntheticTelemetryUtils.jitter(1, 6))
                .statusRowUnreadItemCount(SyntheticTelemetryUtils.jitter(1, 4))
                .statusSessionId(session.statusSessionId())
                .statusViewerSessionId(session.statusViewerSessionId())
                .updatesTabSessionId(session.updatesTabSessionId())
                .unifiedSessionId(session.unifiedSessionId())
                .build());
    }

    /**
     * Fabricates and commits the {@code StatusItemView} (id 1658) per-item
     * status-viewer telemetry.
     *
     * <p>The event describes viewing a single standard-definition photo status
     * from a contact in the address book: the media type and status type, a
     * successful view result, the load, view and item-length timers, the view and
     * impression counts, the media dimensions and file size, the download-progress
     * and estimated-bandwidth samples, the poster contact type, the reshare and
     * forward affordance flags, and the shared session ids plus a fresh trace id.
     * The PSA-link, inline-video, URL-status and reshare-source fields are left
     * unset because a plain photo status carries none of them.
     *
     * @param session the shared status-viewing session
     */
    @WhatsAppWebExport(moduleName = "WAWebStatusItemViewWamEvent", exports = "StatusItemViewWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitStatusItemView(StatusSession session) {
        wamService.commit(new StatusItemViewEventBuilder()
                .mediaType(MediaType.PHOTO)
                .statusType(StatusType.IMAGE)
                .statusCategory(StatusCategory.REGULAR_STATUS)
                .statusItemViewResult(StatusItemViewResult.OK)
                .statusRowSection(StatusRowSection.RECENT_STORIES)
                .statusRowIndex(session.rowIndex())
                .statusItemIndex(SyntheticTelemetryUtils.jitter(0, 4))
                .statusItemViewCount(SyntheticTelemetryUtils.jitter(5, 40))
                .statusItemImpressionCount(SyntheticTelemetryUtils.jitter(8, 50))
                .statusItem3sViewCount(SyntheticTelemetryUtils.jitter(3, 30))
                .statusItemLoadTime(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(120, 200)))
                .statusItemViewTime(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(3000, 3000)))
                .statusItemLength(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(5000, 5000)))
                .statusItemUnread(true)
                .isPosterBiz(false)
                .isPosterInAddressBook(true)
                .statusPosterContactType(StatusPosterContactType.CONTACT)
                .isResharable(true)
                .isReshare(false)
                .isForwardable(true)
                .isForwarded(false)
                .isSubscribed(false)
                .isAlreadyDownloaded(false)
                .isViewedInLandscape(false)
                .musicBlocked(false)
                .statusContainsMusic(false)
                .statusContainsQuestion(false)
                .statusContainsReactionSticker(false)
                .statusMediaWidth(1080)
                .statusMediaHeight(1920)
                .mediaFileSize(SyntheticTelemetryUtils.jitter(180_000, 420_000))
                .bytesDownloadedStartView((double) SyntheticTelemetryUtils.jitter(40_000, 80_000))
                .estimatedBandwidth((double) SyntheticTelemetryUtils.jitter(1_500_000, 3_000_000))
                .pairedMediaType(PairedMediaType.SD_PHOTO)
                .statusPairedMediaQuality(StatusPairedMediaQuality.SD)
                .statusId(SyntheticTelemetryUtils.randomHexLower(12))
                .statusViewerSessionId(session.statusViewerSessionId())
                .unifiedSessionId(session.unifiedSessionId())
                .updatesTabSessionId(session.updatesTabSessionId())
                .traceIdInt(DataUtils.randomLong(1, Integer.MAX_VALUE))
                .build());
    }

    /**
     * Fabricates and commits the {@code WebcStatusSession} (id 1880) status-tab
     * session summary.
     *
     * <p>The event summarizes the rendered status list for the shared status
     * session: the recent, viewed and muted item counts and their corresponding UI
     * row counts, all sourced from the session sample so they agree with the
     * live-derived recent totals.
     *
     * @param session the shared status-viewing session
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcStatusSessionWamEvent", exports = "WebcStatusSessionWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebStatusCollection", exports = "commitStatusSession", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitStatusSession(StatusSession session) {
        wamService.commit(new WebcStatusSessionEventBuilder()
                .webcStatusSessionId(session.statusSessionId())
                .webcStatusRecentItemCount(session.recentItemCount())
                .webcStatusRecentRowCount(session.recentRowCount())
                .webcStatusViewedItemCount(session.viewedItemCount())
                .webcStatusViewedRowCount(session.viewedRowCount())
                .webcStatusMutedItemCount(session.mutedItemCount())
                .webcStatusMutedRowCount(session.mutedRowCount())
                .build());
    }

    /**
     * Fabricates and commits the {@code UpdatesTabSearch} (id 4838) Updates-tab
     * search beacon.
     *
     * <p>The event describes a query-mode search that returns status results: the
     * followed, admin and premium channel counts, the recent and viewed status
     * item and row counts drawn from the session sample, the search event, mode and
     * result types, and the shared unified and Updates-tab session ids plus a fresh
     * search session id.
     *
     * @param session the shared status-viewing session
     */
    @WhatsAppWebExport(moduleName = "WAWebUpdatesTabSearchWamEvent", exports = "UpdatesTabSearchWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebNewsletterInboxSearchLogging", exports = "logNewsletterInboxSearchEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitUpdatesTabSearch(StatusSession session) {
        wamService.commit(new UpdatesTabSearchEventBuilder()
                .channelsFollowedCount(session.followedChannelCount())
                .channelsAdminCount(SyntheticTelemetryUtils.jitter(0, 2))
                .premiumChannelsFollowedCount(SyntheticTelemetryUtils.jitter(0, 1))
                .recentStatusItemCount(session.recentItemCount())
                .recentStatusRowCount(session.recentRowCount())
                .viewedStatusItemCount(session.viewedItemCount())
                .viewedStatusRowCount(session.viewedRowCount())
                .updateTabSearchEventType(UpdateTabSearchEventType.SEARCH)
                .updatesTabSearchModeType(UpdatesTabSearchModeType.QUERY)
                .updatesTabSearchResultType(UpdatesTabSearchResultType.STATUS)
                .updatesTabSearchSessionId(session.updatesTabSearchSessionId())
                .unifiedSessionId(session.unifiedSessionId())
                .updatesTabSessionId(session.updatesTabSessionId())
                .build());
    }

    /**
     * Fabricates and commits the {@code StatusPostImpression} (id 6364) Merlin
     * status-post impression beacon.
     *
     * <p>The event describes a successful first, full-screen impression of a
     * standard-definition photo status from a contact, entered by a direct
     * post-object tap from the recent-stories section and exited by a forward tap:
     * the content and media types, the entry and exit methods, the view result, the
     * load, view and playback timers, the post, pog and view sequence indices, the
     * poster contact type and hashed poster id, the reshare affordance flags, and
     * the shared viewer, unified and Updates-tab session ids plus a fresh trace id.
     * The channel-only, group-status, PSA-campaign, URL-status and attribution
     * fields are left unset because a plain contact photo status carries none of
     * them.
     *
     * @param session the shared status-viewing session
     */
    @WhatsAppWebExport(moduleName = "WAWebStatusPostImpressionWamEvent", exports = "StatusPostImpressionWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebMerlinImpressionManager", exports = "reportImpression", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitStatusPostImpression(StatusSession session) {
        wamService.commit(new StatusPostImpressionEventBuilder()
                .statusContentType(StatusContentType.PHOTO)
                .statusMediaType(MediaType.PHOTO)
                .statusCategory(StatusCategory.REGULAR_STATUS)
                .entryMethod(StatusViewEntryMethod.DIRECT_POG_TAP)
                .statusViewExitMethod(StatusViewExitMethod.FORWARD_TAP)
                .statusViewEntrypoint(StatusRowSection.RECENT_STORIES)
                .statusItemViewResult(StatusItemViewResult.OK)
                .isFirstView(true)
                .isSelfView(false)
                .isSubImpression(false)
                .isSuccessfulView(true)
                .isPosterBiz(false)
                .isViewedInLandscape(false)
                .isLastStatus(false)
                .isCloseSharingPost(false)
                .isEngagementCard(false)
                .isSubscribed(false)
                .isResharable(true)
                .isReshare(false)
                .musicBlocked(false)
                .statusContainsMusic(false)
                .statusContainsQuestion(false)
                .statusContainsReactionSticker(false)
                .statusPosterContactType(StatusPosterContactType.CONTACT)
                .pairedMediaType(PairedMediaType.SD_PHOTO)
                .statusLoadTime(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(120, 200)))
                .statusViewTime(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(3000, 4000)))
                .statusPostPlaybackDuration(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(4000, 4000)))
                .statusPogIndex(SyntheticTelemetryUtils.jitter(0, 4))
                .statusPostIndex(SyntheticTelemetryUtils.jitter(0, 4))
                .viewSequenceIndex(SyntheticTelemetryUtils.jitter(0, 10))
                .pogViewSequenceIndex(SyntheticTelemetryUtils.jitter(0, 10))
                .statusId(SyntheticTelemetryUtils.randomHexLower(12))
                .statusPosterHashId(SyntheticTelemetryUtils.randomHexLower(10))
                .statusViewerSessionId(session.statusViewerSessionId())
                .unifiedSessionId(session.unifiedSessionId())
                .updatesTabSessionId(session.updatesTabSessionId())
                .traceIdInt(DataUtils.randomLong(1, Integer.MAX_VALUE))
                .build());
    }

    /**
     * Fabricates and commits the {@code StatusViewerAction} (id 6692) in-viewer
     * action beacon.
     *
     * <p>The event describes tapping the reshared-status attribution chip in the
     * status viewer: the viewer action type, the top-bar attribution type and its
     * string label, and the regular-status category. The inline-video and
     * URL-status fields are left unset because the tapped attribution is not a link
     * or embedded video.
     */
    @WhatsAppWebExport(moduleName = "WAWebStatusViewerActionWamEvent", exports = "StatusViewerActionWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitStatusViewerAction() {
        wamService.commit(new StatusViewerActionEventBuilder()
                .viewerActionType(StatusViewActionType.ATTRIBUTION_TAPPED)
                .attributionType(TopBarAttributionType.RESHARED_STATUS)
                .attributionTypes("reshared_status")
                .statusCategory(StatusCategory.REGULAR_STATUS)
                .build());
    }

    /**
     * Fabricates and commits the {@code AboutConsumption} (id 6814) profile-About
     * consumption beacon.
     *
     * <p>The event records viewing another user's About text-status surface from a
     * one-on-one chat, the most common consumption surface.
     */
    @WhatsAppWebExport(moduleName = "WAWebAboutConsumptionWamEvent", exports = "AboutConsumptionWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebAboutWamLogger", exports = "logAboutConsumption", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitAboutConsumption() {
        wamService.commit(new AboutConsumptionEventBuilder()
                .aboutConsumptionSurface(AboutConsumptionSurfaceType.ONE_ON_ONE_CHAT)
                .build());
    }

    /**
     * Fabricates and commits the {@code AboutInteraction} (id 7084) profile-About
     * interaction beacon.
     *
     * <p>The event records an interaction with the About consumption surface on the
     * profile-info screen, tagged with the resolved account locale.
     *
     * @param locale the account locale reported in the event
     */
    @WhatsAppWebExport(moduleName = "WAWebAboutInteractionWamEvent", exports = "AboutInteractionWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebAboutWamLogger", exports = "logAboutInteraction", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitAboutInteraction(String locale) {
        wamService.commit(new AboutInteractionEventBuilder()
                .aboutConsumptionSurface(AboutConsumptionSurfaceType.PROFILE_INFO)
                .aboutLocale(locale)
                .build());
    }

    /**
     * Fabricates and commits the {@code AboutConsumptionDaily} (id 6816) daily
     * About-consumption aggregate.
     *
     * <p>The event reports the day's About chat-bubble tap count, About consumption
     * count, and About-surface message-send count, tagged with the resolved account
     * locale. Cobalt has no About-consumption UI, so the three counters are small
     * plausible per-day totals rather than tracked figures.
     *
     * @param locale the account locale reported in the event
     */
    @WhatsAppWebExport(moduleName = "WAWebAboutConsumptionDailyWamEvent", exports = "AboutConsumptionDailyWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebTasksDailyStatsTask", exports = "reportAboutConsumptionDaily", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitAboutConsumptionDaily(String locale) {
        wamService.commit(new AboutConsumptionDailyEventBuilder()
                .aboutChatBubbleTapCount(SyntheticTelemetryUtils.jitter(1, 5))
                .aboutChatConsumptionCount(SyntheticTelemetryUtils.jitter(1, 8))
                .aboutMessageSendCount(SyntheticTelemetryUtils.jitter(1, 4))
                .aboutLocale(locale)
                .build());
    }

    /**
     * Fabricates and commits the {@code AboutCreationDaily} (id 6820) daily
     * About-creation funnel aggregate.
     *
     * <p>The event reports the day's About-creation funnel entered from the profile
     * entrypoint: the visit and started counts, a success count, and the resolved
     * account locale. The started count is floored at one so the funnel is
     * internally consistent (a started creation implies a visit), and the failure
     * count is left unset because Cobalt observes no creation failures.
     *
     * @param locale the account locale reported in the event
     */
    @WhatsAppWebExport(moduleName = "WAWebAboutCreationDailyWamEvent", exports = "AboutCreationDailyWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebTasksDailyStatsTask", exports = "reportAboutCreationDaily", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitAboutCreationDaily(String locale) {
        wamService.commit(new AboutCreationDailyEventBuilder()
                .aboutCreationVisit(SyntheticTelemetryUtils.jitter(2, 4))
                .aboutCreationStarted(SyntheticTelemetryUtils.jitter(1, 2))
                .aboutSuccessCount(1)
                .aboutEntrypoint(AboutEntrypointType.PROFILE)
                .aboutLocale(locale)
                .build());
    }

    /**
     * Counts the distinct posters across a status collection.
     *
     * <p>Each poster contributes one status-tray row regardless of how many status
     * items they posted, so the distinct sender count is the faithful row count for
     * the session summary. Items with no resolvable sender are skipped.
     *
     * @param statusItems the live status collection to inspect
     * @return the number of distinct posters, as a row count
     */
    private static long distinctPosters(Collection<ChatMessageInfo> statusItems) {
        var posters = new HashSet<Jid>();
        for (var item : statusItems) {
            item.senderJid().ifPresent(posters::add);
        }
        return posters.size();
    }

    /**
     * Resolves the account locale reported in the About events.
     *
     * <p>The bound account's stored locale is preferred; when it is absent or
     * blank the host default locale's IETF language tag is used, so the reported
     * value is a plausible non-empty locale rather than a placeholder.
     *
     * @return the resolved locale as an IETF language tag
     */
    private String aboutLocale() {
        return client.store()
                .accountStore()
                .locale()
                .filter(tag -> !tag.isBlank())
                .orElseGet(() -> Locale.getDefault().toLanguageTag());
    }






    /**
     * The shared correlation ids and derived counts describing one fabricated
     * status-viewing session, threaded through every correlated status event so
     * they cross-reference consistently.
     *
     * @param statusSessionId          the status-collection session id shared by the
     *                                 row-view and session-summary events
     * @param statusViewerSessionId    the status-viewer session id shared by the
     *                                 row-view, item-view and post-impression events
     * @param updatesTabSessionId      the Updates-tab session id shared across every
     *                                 correlated event
     * @param unifiedSessionId         the unified session id shared across every
     *                                 correlated event
     * @param updatesTabSearchSessionId the per-search session id carried by the
     *                                 Updates-tab search event
     * @param rowIndex                 the status-tray row index shared by the
     *                                 row-view and item-view events
     * @param recentItemCount          the count of recent status items
     * @param recentRowCount           the count of recent status-tray rows
     * @param viewedItemCount          the count of viewed status items
     * @param viewedRowCount           the count of viewed status-tray rows
     * @param mutedItemCount           the count of muted status items
     * @param mutedRowCount            the count of muted status-tray rows
     * @param followedChannelCount     the count of followed channels
     */
    private record StatusSession(
            long statusSessionId,
            long statusViewerSessionId,
            long updatesTabSessionId,
            String unifiedSessionId,
            String updatesTabSearchSessionId,
            long rowIndex,
            long recentItemCount,
            long recentRowCount,
            long viewedItemCount,
            long viewedRowCount,
            long mutedItemCount,
            long mutedRowCount,
            long followedChannelCount) {
    }
}
