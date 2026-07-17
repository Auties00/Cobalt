package com.github.auties00.cobalt.wam.synthetic;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.newsletter.Newsletter;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterViewerMetadata;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterViewerRole;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.ChannelAdminEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ChannelDiscoveryVisibilityTrackingEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ChannelDyiEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ChannelLinkShareEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ChannelMessageVisibilityTrackingEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ChannelOpenEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ChannelOpenFromInviteEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ChannelProducerInsightsNavigationEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ChannelSimilarChannelsEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.NewsletterEnforcementEventsEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PsChannelsSnaplEventEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.ActionTarget;
import com.github.auties00.cobalt.wire.wam.type.AdminFlowType;
import com.github.auties00.cobalt.wire.wam.type.BannerStatus;
import com.github.auties00.cobalt.wire.wam.type.BannerStatusReason;
import com.github.auties00.cobalt.wire.wam.type.ChannelAdminAction;
import com.github.auties00.cobalt.wire.wam.type.ChannelDirectoryPillSelected;
import com.github.auties00.cobalt.wire.wam.type.ChannelDyiEventType;
import com.github.auties00.cobalt.wire.wam.type.ChannelEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.ChannelEventSurface;
import com.github.auties00.cobalt.wire.wam.type.ChannelEventUnit;
import com.github.auties00.cobalt.wire.wam.type.ChannelLinkShareDirection;
import com.github.auties00.cobalt.wire.wam.type.ChannelLinkShareEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.ChannelLinkShareScreen;
import com.github.auties00.cobalt.wire.wam.type.ChannelProducerInsightsActionType;
import com.github.auties00.cobalt.wire.wam.type.ChannelProducerInsightsEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.ChannelProducerInsightsSurface;
import com.github.auties00.cobalt.wire.wam.type.ChannelUserType;
import com.github.auties00.cobalt.wire.wam.type.EnforcementInteractionEventType;
import com.github.auties00.cobalt.wire.wam.type.EnforcementType;
import com.github.auties00.cobalt.wire.wam.type.InteractionEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.InteractionSurface;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Emits the synthetic channel (newsletter) WAM telemetry burst that a genuine
 * WhatsApp Web session produces while browsing, opening, administering and
 * moderating Channels, so a Cobalt session's metrics stream carries the same
 * channel-surface fingerprint a real browser leaves behind.
 *
 * <p>WhatsApp Web instruments every Channels surface with a family of
 * UI-driven telemetry events that Cobalt, being a headless client with no
 * rendered directory, no viewport, no producer-insights dashboard and no
 * moderation sheets, never fires from a real feature:
 * <ul>
 *   <li>opening a channel from the Updates tab ({@code ChannelOpen}) or from
 *       an invite link ({@code ChannelOpenFromInvite});</li>
 *   <li>viewport impression tracking of directory cards
 *       ({@code ChannelDiscoveryVisibilityTracking}) and of individual channel
 *       posts ({@code ChannelMessageVisibilityTracking}), driven on web by the
 *       Merlin impression manager;</li>
 *   <li>the similar-channels recommendation banner
 *       ({@code ChannelSimilarChannels});</li>
 *   <li>the channel creation/edit admin funnel ({@code ChannelAdmin}), the
 *       producer-insights analytics navigation
 *       ({@code ChannelProducerInsightsNavigation}), the Download-Your-Info
 *       flow ({@code ChannelDyi}) and the invite-link share
 *       ({@code ChannelLinkShare});</li>
 *   <li>the owner-side enforcement/appeal sheet interactions
 *       ({@code NewsletterEnforcementEvents});</li>
 *   <li>the private-channel video-playback SNAPL flush
 *       ({@code PsChannelsSnaplEvent}).</li>
 * </ul>
 *
 * <p>Every field WhatsApp Web sets is populated with either a real value read
 * from the bound session's channel store (the channel id, the viewer's role
 * mapped to the WAM user-type, the unread counter) or a plausible, host-derived
 * or captured constant (session ids minted the way each WA logger mints them,
 * the host country as the directory country selector, fabricated post and
 * playback metrics). No obviously-fake sentinel values are used; the zero
 * premium-unread counter and the {@code false} premium flag reflect a genuine
 * non-premium channel rather than a placeholder.
 *
 * @implNote
 * This implementation matches, field for field, the WA loggers whose source is
 * available ({@code NewsletterAdminFunnelLogger},
 * {@code NewsletterLinkShareLogging}, {@code SimilarNewsletterLogging},
 * {@code NewsletterProducerInsightsLogger}, {@code EnforcementActionLogging}),
 * setting only the properties those loggers populate. For the open, DYI,
 * invite-open and impression events, which are mobile-native or
 * viewport-driven on web and have no reachable web logger, it fabricates a
 * coherent full property set. The admin-side events fire only when the bound
 * account actually owns or administers a channel; the follower-side events
 * fire for any session with (or fabricating) a followed channel, and the
 * cross-surface actions (link share, invite open, video SNAPL) fire at a
 * realistic low per-session rate through {@link SyntheticTelemetryUtils#chance(int)} so a fleet of
 * sessions surfaces them at a believable frequency rather than on every
 * connection.
 *
 * @see WamService
 */
@WhatsAppWebModule(moduleName = "WAWebNewsletterAdminFunnelLogging")
@WhatsAppWebModule(moduleName = "WAWebNewsletterLinkShareLogging")
@WhatsAppWebModule(moduleName = "WAWebSimilarNewsletterLogging")
@WhatsAppWebModule(moduleName = "WAWebNewsletterProducerInsightsLogger")
@WhatsAppWebModule(moduleName = "WAWebEnforcementActionLogging")
@WhatsAppWebModule(moduleName = "WAWebMerlinImpressionManager")
@WhatsAppWebModule(moduleName = "WAWebMediaPlaybackLogFlusher")
public final class SyntheticChannelTelemetry {
    /**
     * The directory category names cycled into the
     * {@code channelCategoryName} discovery-impression field, captured from the
     * real WhatsApp Web channel directory so the reported category reads like a
     * genuine browsable section rather than a placeholder.
     */
    private static final String[] DIRECTORY_CATEGORIES = {
            "News", "Sports", "Entertainment", "Technology",
            "Lifestyle", "Gaming", "Music", "Business"
    };

    /**
     * The video resolutions cycled into the fabricated SNAPL playback metrics,
     * the common adaptive-bitrate ladder rungs a channel video player reports.
     */
    private static final String[] VIDEO_RESOLUTIONS = {"360p", "480p", "720p", "1080p"};

    /**
     * The largest integer that survives a JavaScript {@code number} without
     * precision loss.
     *
     * <p>WhatsApp Web mints the enforcement and media-event session ids with
     * {@code Math.floor(Number.MAX_SAFE_INTEGER * Math.random())}; bounding the
     * fabricated large ids by the same ceiling keeps them in the exact range a
     * genuine web logger produces.
     */
    private static final long MAX_SAFE_INTEGER = 9_007_199_254_740_991L;

    /**
     * The Meta application id reported in the {@code appId} field of the
     * private-channel SNAPL playback event.
     *
     * <p>Cobalt has no video player and therefore no genuine media-logging app
     * id to surface; this is a plausibly-shaped captured constant standing in
     * for the WhatsApp media-playback logging application id so the private
     * SNAPL upload carries a believable, stable owner.
     */
    private static final long MEDIA_PLAYBACK_APP_ID = 1889557694797632L;

    /**
     * The bound WhatsApp client whose channel store supplies the live
     * newsletters sampled into the fabricated channel-telemetry burst.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The WAM service through which every fabricated channel event is committed
     * for batched upload.
     */
    private final WamService wamService;

    /**
     * Constructs a new {@code SyntheticChannelTelemetry} bound to the given
     * client and WAM service.
     *
     * @param client     the WhatsApp client whose channel store is sampled,
     *                   must not be {@code null}
     * @param wamService the WAM service used to commit the emitted events, must
     *                   not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SyntheticChannelTelemetry(LinkedWhatsAppClient client, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
    }

    /**
     * Emits the once-per-connection channel-telemetry burst on a dedicated
     * virtual thread.
     *
     * <p>This is the single entry point the client drives after the socket
     * opens. It samples the bound session's channels, describes a primary and a
     * similar channel (falling back to a fabricated one when the session
     * follows none), and commits one coherent snapshot of the Channels
     * telemetry a genuine WA Web session leaves: the open beacon, the directory
     * and post viewport impressions, the similar-channels banner and, when the
     * account administers a channel, the admin funnel, producer-insights
     * navigation and, at a low rate, the Download-Your-Info and enforcement
     * interactions. The cross-surface actions (invite-link share, invite open
     * and the private video SNAPL flush) fire at a realistic low per-session
     * probability.
     *
     * @apiNote
     * The impression and SNAPL surfaces are continuous by nature on WA Web (the
     * Merlin manager fires an impression per card that scrolls into view and the
     * playback flusher fires per video watched). Cobalt has no scroll or
     * playback loop, so this method folds one representative sample of each into
     * the per-connection burst; a caller that wants a higher cadence may invoke
     * it again on a timer.
     */
    public void emitSessionTelemetry() {
        Thread.ofVirtual()
                .name("channel-telemetry")
                .start(this::runBurst);
    }

    /**
     * Builds and commits the full channel-telemetry burst.
     *
     * <p>The channel store is sampled once into a list of descriptors; the
     * first two drive the primary and similar-channel surfaces, and the first
     * descriptor whose viewer role is owner or admin drives the admin-side
     * surfaces. When the session follows no channels, two fabricated
     * descriptors stand in so the follower-side fingerprint still exists. The
     * unified-session id and updates-tab session id are minted once and shared
     * by the events that correlate through them, as a real Updates-tab session
     * would.
     */
    private void runBurst() {
        var channels = new ArrayList<ChannelDescriptor>();
        for (var newsletter : client.store().chatStore().newsletters()) {
            channels.add(describe(newsletter));
        }
        if (channels.isEmpty()) {
            channels.add(fabricateChannel(SyntheticTelemetryUtils.chance(25)));
            channels.add(fabricateChannel(false));
        }

        var primary = channels.getFirst();
        var similar = channels.size() > 1 ? channels.get(1) : fabricateChannel(false);
        var adminChannel = channels.stream()
                .filter(ChannelDescriptor::admin)
                .findFirst()
                .orElse(null);

        var unifiedSessionId = SyntheticTelemetryUtils.randomHexLower(12);
        var updatesTabSessionId = DataUtils.randomLong(1, 1_000_000_000);

        commitChannelOpen(primary, unifiedSessionId, updatesTabSessionId);
        commitDiscoveryVisibility(primary, unifiedSessionId, updatesTabSessionId);
        commitSimilarChannels(primary, similar, unifiedSessionId, updatesTabSessionId);
        commitMessageVisibility(primary, unifiedSessionId);

        if (adminChannel != null) {
            commitChannelAdmin(adminChannel);
            commitProducerInsights(adminChannel);
            if (SyntheticTelemetryUtils.chance(30)) {
                commitChannelDyi(unifiedSessionId, updatesTabSessionId);
            }
            if (SyntheticTelemetryUtils.chance(15)) {
                commitEnforcement(adminChannel);
            }
        }

        if (SyntheticTelemetryUtils.chance(20)) {
            commitLinkShare(primary);
        }
        if (SyntheticTelemetryUtils.chance(10)) {
            commitChannelOpenFromInvite(similar);
        }
        if (SyntheticTelemetryUtils.chance(25)) {
            commitChannelsSnapl();
        }
    }

    /**
     * Fabricates and commits the {@code ChannelOpen} (id 4316) channel-open
     * beacon for the primary channel.
     *
     * <p>The open is described as an Updates-tab open of a live channel: the
     * channel id, the viewer's real user type and unread counter are taken from
     * the store, the premium counters report the non-premium steady state, and
     * the network flag, discovery surface and session ids describe a healthy
     * foreground open.
     *
     * @param channel             the primary channel being opened
     * @param unifiedSessionId    the shared updates-tab unified session id
     * @param updatesTabSessionId the shared updates-tab session id
     */
    @WhatsAppWebExport(moduleName = "WAWebChannelOpenWamEvent", exports = "ChannelOpenWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitChannelOpen(ChannelDescriptor channel, String unifiedSessionId, long updatesTabSessionId) {
        wamService.commit(new ChannelOpenEventBuilder()
                .channelEntryPoint(ChannelEntryPoint.UPDATES_TAB)
                .channelSessionId(DataUtils.randomLong(1, 1_000_000_000))
                .channelUserType(channel.userType())
                .cid(channel.cid())
                .discoverySurface(TsSurface.CHANNEL_UPDATES_HOME)
                .hasNetworkConnection(true)
                .isPremium(false)
                .unreadMessages(channel.unreadMessages())
                .unreadPremiumMessages(0)
                .updatesTabSessionId(updatesTabSessionId)
                .unifiedSessionId(unifiedSessionId)
                .traceIdInt(DataUtils.randomLong(1, Integer.MAX_VALUE))
                .build());
    }

    /**
     * Fabricates and commits the {@code ChannelDiscoveryVisibilityTracking}
     * (id 5766) directory-card viewport impression.
     *
     * <p>The impression describes a recommended-channels card entering the
     * viewport in the channel directory: the category name and index, the card
     * index, the selected pill and the directory surface reproduce a genuine
     * Merlin impression, and the country selector is the host locale's country
     * so the reported directory filter is host-specific.
     *
     * @param channel             the channel whose directory card is impressed
     * @param unifiedSessionId    the shared updates-tab unified session id
     * @param updatesTabSessionId the shared updates-tab session id
     */
    @WhatsAppWebExport(moduleName = "WAWebChannelDiscoveryVisibilityTrackingWamEvent", exports = "ChannelDiscoveryVisibilityTrackingWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebMerlinImpressionManager", exports = "recordImpression", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitDiscoveryVisibility(ChannelDescriptor channel, String unifiedSessionId, long updatesTabSessionId) {
        var categoryIndex = ThreadLocalRandom.current().nextInt(DIRECTORY_CATEGORIES.length);
        wamService.commit(new ChannelDiscoveryVisibilityTrackingEventBuilder()
                .channelCategoryIndex(categoryIndex)
                .channelCategoryName(DIRECTORY_CATEGORIES[categoryIndex])
                .channelDirectorySessionId(DataUtils.randomLong(1, 1_000_000_000))
                .channelDiscoveryQueryId(SyntheticTelemetryUtils.randomHexLower(6))
                .channelDiscoverySearchId(SyntheticTelemetryUtils.randomHexLower(6))
                .channelEventUnit(ChannelEventUnit.RECOMMENDED_CHANNELS)
                .channelIndex(SyntheticTelemetryUtils.jitter(0, 10))
                .cid(channel.cid())
                .countrySelector(countrySelector())
                .discoverySurface(TsSurface.CHANNEL_DIRECTORY)
                .isSubImpression(false)
                .pillSelected(ChannelDirectoryPillSelected.RECOMMENDED)
                .similarChannelsSessionId(DataUtils.randomLong(1, Integer.MAX_VALUE))
                .traceIdInt(DataUtils.randomLong(1, Integer.MAX_VALUE))
                .unifiedSessionId(unifiedSessionId)
                .updatesTabSessionId(updatesTabSessionId)
                .build());
    }

    /**
     * Fabricates and commits the {@code ChannelSimilarChannels} (id 5202)
     * recommendation-banner impression.
     *
     * <p>This mirrors WA Web's {@code logSimilarNewsletterImpression}: the
     * origin channel id, the similar channel's id and user type, the banner
     * status and its found-reason, and the display and absolute ranks describe a
     * populated similar-channels banner in the Updates home.
     *
     * @param origin              the origin channel the banner is shown under
     * @param similar             the recommended similar channel
     * @param unifiedSessionId    the shared updates-tab unified session id
     * @param updatesTabSessionId the shared updates-tab session id
     */
    @WhatsAppWebExport(moduleName = "WAWebChannelSimilarChannelsWamEvent", exports = "ChannelSimilarChannelsWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSimilarNewsletterLogging", exports = "logSimilarNewsletterImpression", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitSimilarChannels(ChannelDescriptor origin, ChannelDescriptor similar, String unifiedSessionId, long updatesTabSessionId) {
        var rank = SyntheticTelemetryUtils.jitter(0, 4);
        wamService.commit(new ChannelSimilarChannelsEventBuilder()
                .cid(origin.cid())
                .bannerStatus(BannerStatus.DISPLAYED)
                .bannerStatusReason(BannerStatusReason.SIMILAR_CHANNELS_FOUND)
                .similarChannelRank(rank)
                .similarChannelDisplayRank(rank)
                .similarChannelId(similar.cid())
                .similarChannelEventSurface(ChannelEventSurface.CHANNEL_UPDATES_HOME)
                .similarChannelUserType(similar.userType())
                .similarChannelsSessionId(DataUtils.randomLong(1, Integer.MAX_VALUE))
                .unifiedSessionId(unifiedSessionId)
                .updatesTabSessionId(updatesTabSessionId)
                .build());
    }

    /**
     * Fabricates and commits the {@code ChannelMessageVisibilityTracking}
     * (id 5998) channel-post view-per-view impression.
     *
     * <p>The impression describes a single channel post scrolling into the
     * viewport: the channel id and the viewer's user type are real, the post id
     * is a fabricated server id, and the music, starred and original-author
     * flags reflect a plausible ordinary post (original-author true only when
     * the viewer administers the channel).
     *
     * @param channel          the channel whose post is impressed
     * @param unifiedSessionId the shared updates-tab unified session id
     */
    @WhatsAppWebExport(moduleName = "WAWebChannelMessageVisibilityTrackingWamEvent", exports = "ChannelMessageVisibilityTrackingWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebMerlinImpressionManager", exports = "recordImpression", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitMessageVisibility(ChannelDescriptor channel, String unifiedSessionId) {
        wamService.commit(new ChannelMessageVisibilityTrackingEventBuilder()
                .cid(channel.cid())
                .postId(Long.toString(SyntheticTelemetryUtils.jitter(100, 9_000)))
                .channelUserType(channel.userType())
                .containsMusic(SyntheticTelemetryUtils.chance(10))
                .isOriginalAuthor(channel.admin())
                .isStarredPost(SyntheticTelemetryUtils.chance(5))
                .isVpvImpression(true)
                .unifiedSessionId(unifiedSessionId)
                .traceIdInt(DataUtils.randomLong(1, Integer.MAX_VALUE))
                .build());
    }

    /**
     * Fabricates and commits the {@code ChannelAdmin} (id 4556) creation/edit
     * funnel step for an administered channel.
     *
     * <p>This mirrors WA Web's {@code NewsletterAdminFunnelLogger}: a single
     * edit-flow step that sets the channel icon from the gallery, carrying the
     * per-flow session id and the action-sequence position the funnel logger
     * increments across steps.
     *
     * @param channel the channel the viewer owns or administers
     */
    @WhatsAppWebExport(moduleName = "WAWebChannelAdminWamEvent", exports = "ChannelAdminWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebNewsletterAdminFunnelLogging", exports = "NewsletterAdminFunnelLogger", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitChannelAdmin(ChannelDescriptor channel) {
        wamService.commit(new ChannelAdminEventBuilder()
                .adminFlowType(AdminFlowType.EDIT)
                .channelAdminAction(ChannelAdminAction.CHANNEL_ICON_SET_GALLERY)
                .channelAdminSessionId(DataUtils.randomLong(1, 1_000_000_000))
                .adminFlowActionSequenceNumber(SyntheticTelemetryUtils.jitter(0, 3))
                .build());
    }

    /**
     * Fabricates and commits the {@code ChannelProducerInsightsNavigation}
     * (id 5626) analytics-dashboard navigation event.
     *
     * <p>This mirrors WA Web's {@code NewsletterProducerInsightsLogger.logOpen}:
     * the channel id, an open action targeting the reach tab from the
     * profile-see-all entry point on the channel-info surface, the per-session
     * media-event id and the zero-based sequence number the logger increments.
     *
     * @param channel the channel the viewer administers
     */
    @WhatsAppWebExport(moduleName = "WAWebChannelProducerInsightsNavigationWamEvent", exports = "ChannelProducerInsightsNavigationWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebNewsletterProducerInsightsLogger", exports = "NewsletterProducerInsightsLogger", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitProducerInsights(ChannelDescriptor channel) {
        wamService.commit(new ChannelProducerInsightsNavigationEventBuilder()
                .cid(channel.cid())
                .channelProducerInsightsActionType(ChannelProducerInsightsActionType.OPEN)
                .channelProducerInsightsActionTarget(ActionTarget.REACH_TAB)
                .channelProducerInsightsEntryPoint(ChannelProducerInsightsEntryPoint.PROFILE_SEE_ALL)
                .channelProducerInsightsSurface(ChannelProducerInsightsSurface.REACH_TAB)
                .channelProducerInsightsSequenceNumber(0)
                .producerInsightsSessionId(DataUtils.randomLong(1, MAX_SAFE_INTEGER))
                .build());
    }

    /**
     * Fabricates and commits the {@code ChannelDyi} (id 4726)
     * Download-Your-Information request event.
     *
     * <p>The event reports a report-request DYI step, carrying the shared
     * unified-session and updates-tab session ids.
     *
     * @param unifiedSessionId    the shared updates-tab unified session id
     * @param updatesTabSessionId the shared updates-tab session id
     */
    @WhatsAppWebExport(moduleName = "WAWebChannelDyiWamEvent", exports = "ChannelDyiWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitChannelDyi(String unifiedSessionId, long updatesTabSessionId) {
        wamService.commit(new ChannelDyiEventBuilder()
                .channelDyiEventType(ChannelDyiEventType.CHANNEL_REPORT_REQUEST)
                .unifiedSessionId(unifiedSessionId)
                .updatesTabSessionId(updatesTabSessionId)
                .build());
    }

    /**
     * Fabricates and commits the {@code ChannelLinkShare} (id 4728)
     * invite-link share event.
     *
     * <p>This mirrors WA Web's {@code logNewsletterLinkShare}: a WhatsApp-target
     * share of the channel invite link initiated from the channel info page,
     * carrying only the direction, entry point, screen and channel id the web
     * logger populates.
     *
     * @param channel the channel whose invite link is shared
     */
    @WhatsAppWebExport(moduleName = "WAWebChannelLinkShareWamEvent", exports = "ChannelLinkShareWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebNewsletterLinkShareLogging", exports = "logNewsletterLinkShare", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitLinkShare(ChannelDescriptor channel) {
        wamService.commit(new ChannelLinkShareEventBuilder()
                .channelLinkShareDirection(ChannelLinkShareDirection.WHATSAPP)
                .channelLinkShareEntryPoint(ChannelLinkShareEntryPoint.CHANNEL_INFO_PAGE)
                .channelLinkShareScreen(ChannelLinkShareScreen.CHANNEL_INFO)
                .cid(channel.cid())
                .build());
    }

    /**
     * Fabricates and commits the {@code ChannelOpenFromInvite} (id 7134)
     * private-channel invite-open beacon.
     *
     * <p>The open is described as a guest opening the channel from a shared
     * link and landing on the channel thread; it carries the link entry point,
     * the guest user type, the discovery surface and the channel id, and is
     * routed on the private telemetry channel like its schema declares.
     *
     * @param channel the channel opened from the invite link
     */
    @WhatsAppWebExport(moduleName = "WAWebChannelOpenFromInviteWamEvent", exports = "ChannelOpenFromInviteWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitChannelOpenFromInvite(ChannelDescriptor channel) {
        wamService.commit(new ChannelOpenFromInviteEventBuilder()
                .channelEntryPoint(ChannelEntryPoint.LINK)
                .channelUserType(ChannelUserType.GUEST)
                .discoverySurface(TsSurface.CHANNEL_THREAD)
                .cid(channel.cid())
                .build());
    }

    /**
     * Fabricates and commits the {@code NewsletterEnforcementEvents} (id 7112)
     * owner-side enforcement interaction.
     *
     * <p>This mirrors WA Web's {@code EnforcementActionLogging}: an
     * enforcement-detail click from the info-drawer alert option on the
     * enforcement-detail screen, carrying the per-session enforcement id and the
     * strike context (a suspend enforcement type and a violation category) the
     * logger attaches when a session context is present.
     *
     * @param channel the administered channel under enforcement
     */
    @WhatsAppWebExport(moduleName = "WAWebNewsletterEnforcementEventsWamEvent", exports = "NewsletterEnforcementEventsWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebEnforcementActionLogging", exports = "EnforcementCoreEventLogger", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitEnforcement(ChannelDescriptor channel) {
        wamService.commit(new NewsletterEnforcementEventsEventBuilder()
                .cid(channel.cid())
                .enforcementInteractionEvent(EnforcementInteractionEventType.CLICK_ENFORCEMENT_DETAIL)
                .interactionEntryPoint(InteractionEntryPoint.INFO_DRAWER_ALERT_OPTION)
                .interactionSurface(InteractionSurface.ENFORCEMENT_DETAIL_SCREEN)
                .newsletterEnforcementSessionId(DataUtils.randomLong(1, MAX_SAFE_INTEGER))
                .enforcementType(EnforcementType.CH_S)
                .violationCategory("spam")
                .build());
    }

    /**
     * Fabricates and commits the {@code PsChannelsSnaplEvent} (id 6254)
     * private-channel video-playback SNAPL flush.
     *
     * <p>The event carries a JSON blob of fabricated playback metrics (duration,
     * watch time, rebuffering, startup latency, bitrate and resolution) and the
     * media-logging application id, routed on the private SNAPL channel as its
     * schema declares.
     */
    @WhatsAppWebExport(moduleName = "WAWebPsChannelsSnaplEventWamEvent", exports = "PsChannelsSnaplEventWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebMediaPlaybackLogFlusher", exports = "flush", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitChannelsSnapl() {
        wamService.commit(new PsChannelsSnaplEventEventBuilder()
                .videoEventJson(fabricatePlaybackMetrics())
                .appId(MEDIA_PLAYBACK_APP_ID)
                .build());
    }

    /**
     * Describes a stored newsletter as a channel descriptor for the burst.
     *
     * <p>The channel id is the newsletter JID's user component (the numeric
     * channel id WA reports as {@code cid}); the viewer's role is mapped to the
     * WAM {@link ChannelUserType}, and the unread counter is read directly from
     * the store.
     *
     * @param newsletter the stored newsletter to describe
     * @return the channel descriptor for {@code newsletter}
     */
    private static ChannelDescriptor describe(Newsletter newsletter) {
        var role = newsletter.viewerMetadata()
                .map(NewsletterViewerMetadata::role)
                .orElse(NewsletterViewerRole.UNKNOWN);
        var userType = channelUserType(role);
        return new ChannelDescriptor(
                newsletter.jid().user(),
                userType,
                newsletter.unreadMessagesCount(),
                userType == ChannelUserType.OWNER || userType == ChannelUserType.ADMIN);
    }

    /**
     * Fabricates a channel descriptor for a session that follows no real
     * channels.
     *
     * <p>The channel id has the {@code 120363} prefix a genuine WhatsApp channel
     * JID carries, followed by random digits; an admin descriptor reports the
     * owner or admin user type, a non-admin descriptor reports a follower with a
     * small plausible unread counter.
     *
     * @param admin whether the fabricated channel is administered by the viewer
     * @return the fabricated channel descriptor
     */
    private static ChannelDescriptor fabricateChannel(boolean admin) {
        var userType = admin
                ? (SyntheticTelemetryUtils.chance(50) ? ChannelUserType.OWNER : ChannelUserType.ADMIN)
                : ChannelUserType.FOLLOWER;
        return new ChannelDescriptor(fabricatedChannelId(), userType, SyntheticTelemetryUtils.jitter(0, 25), admin);
    }

    /**
     * Maps a newsletter viewer role to the WAM channel user type.
     *
     * <p>Owner and admin map to their WAM counterparts, guest maps to guest, and
     * both the subscriber and unknown roles map to the follower user type WA
     * reports for an ordinary channel reader.
     *
     * @param role the newsletter viewer role
     * @return the matching {@link ChannelUserType}
     */
    private static ChannelUserType channelUserType(NewsletterViewerRole role) {
        return switch (role) {
            case OWNER -> ChannelUserType.OWNER;
            case ADMIN -> ChannelUserType.ADMIN;
            case GUEST -> ChannelUserType.GUEST;
            case SUBSCRIBER, UNKNOWN -> ChannelUserType.FOLLOWER;
        };
    }

    /**
     * Builds the JSON playback-metrics blob carried by the SNAPL event.
     *
     * <p>The watch time is bounded by the fabricated video duration so the two
     * are internally consistent, and the rebuffer, startup, bitrate and
     * resolution figures describe a plausible ordinary channel-video playback.
     *
     * @return the fabricated playback-metrics JSON string
     */
    private static String fabricatePlaybackMetrics() {
        var durationMs = SyntheticTelemetryUtils.jitter(15_000, 120_000);
        var watchedMs = 3_000 + ThreadLocalRandom.current().nextLong(Math.max(1, durationMs - 3_000));
        return "{\"video_duration_ms\":" + durationMs
                + ",\"watch_time_ms\":" + watchedMs
                + ",\"rebuffer_count\":" + SyntheticTelemetryUtils.jitter(0, 3)
                + ",\"rebuffer_time_ms\":" + SyntheticTelemetryUtils.jitter(0, 800)
                + ",\"startup_time_ms\":" + SyntheticTelemetryUtils.jitter(120, 600)
                + ",\"average_bitrate_bps\":" + SyntheticTelemetryUtils.jitter(400_000, 1_200_000)
                + ",\"resolution\":\"" + VIDEO_RESOLUTIONS[DataUtils.randomInt(VIDEO_RESOLUTIONS.length)] + "\""
                + ",\"is_autoplay\":" + SyntheticTelemetryUtils.chance(60)
                + "}";
    }

    /**
     * Returns the host locale's ISO 3166-1 alpha-2 country code for the
     * directory country selector, or {@code "US"} when the host locale reports
     * no country.
     *
     * <p>Deriving the selector from the real host locale keeps the reported
     * directory filter host-specific rather than a frozen constant.
     *
     * @return the two-letter country selector
     */
    private static String countrySelector() {
        var country = Locale.getDefault().getCountry();
        return country == null || country.isEmpty() ? "US" : country;
    }




    /**
     * Returns a fabricated WhatsApp channel id: the {@code 120363} prefix a
     * genuine channel JID carries, followed by twelve random digits.
     *
     * @return the fabricated numeric channel id
     */
    private static String fabricatedChannelId() {
        var builder = new StringBuilder("120363");
        for (var i = 0; i < 12; i++) {
            builder.append(ThreadLocalRandom.current().nextInt(10));
        }
        return builder.toString();
    }





    /**
     * Describes a channel sampled for the telemetry burst.
     *
     * @param cid            the numeric channel id reported as {@code cid}
     * @param userType       the viewer's WAM channel user type
     * @param unreadMessages the viewer's unread message count for the channel
     * @param admin          whether the viewer owns or administers the channel
     */
    private record ChannelDescriptor(String cid, ChannelUserType userType, long unreadMessages, boolean admin) {
    }
}
