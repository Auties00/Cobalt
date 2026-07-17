package com.github.auties00.cobalt.wam.synthetic;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.ChannelsVideoPlayEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.GifFromProviderSentEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.GifSearchCancelledEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.GifSearchNoResultsEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.GifSearchResultTappedEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.GifSearchSessionStartedEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.InlineVideoPlaybackClosedEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MediaHubUserJourneyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MediaPickerEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MediaStreamPlaybackEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.NonMessagePeerDataMediaUploadEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.StickerCommonQueryToStaticServerEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.StickerPickerOpenedEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.StickerStoreOpenedEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebHdMediaAwarenessInteractionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebcEmojiOpenEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebcMediaAnalyzedEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebcMediaEditorSendEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebcMediaLoadEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebcStickerMakerEventsEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.ActionCode;
import com.github.auties00.cobalt.wire.wam.type.EntryPointType;
import com.github.auties00.cobalt.wire.wam.type.GifSearchProvider;
import com.github.auties00.cobalt.wire.wam.type.InlineVideoType;
import com.github.auties00.cobalt.wire.wam.type.MediaPickerOriginType;
import com.github.auties00.cobalt.wire.wam.type.MediaQuality;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageType;
import com.github.auties00.cobalt.wire.wam.type.PeerDataRequestType;
import com.github.auties00.cobalt.wire.wam.type.PeerDataResponseResultType;
import com.github.auties00.cobalt.wire.wam.type.PlaybackOriginType;
import com.github.auties00.cobalt.wire.wam.type.PlaybackStateType;
import com.github.auties00.cobalt.wire.wam.type.QueryType;
import com.github.auties00.cobalt.wire.wam.type.StickerStoreOpenedOriginType;
import com.github.auties00.cobalt.wire.wam.type.SurfaceCode;
import com.github.auties00.cobalt.wire.wam.type.VideoPlayOrigin;
import com.github.auties00.cobalt.wire.wam.type.VideoPlayResult;
import com.github.auties00.cobalt.wire.wam.type.VideoPlayType;
import com.github.auties00.cobalt.wire.wam.type.WebcMediaLoadResultCode;
import com.github.auties00.cobalt.wire.wam.type.WebcStickerMakerEventNameType;

import java.util.Locale;
import java.util.Objects;

/**
 * Synthesises the block of media-surface WhatsApp Metrics (WAM) events that a
 * genuine WhatsApp Web session emits from its rich in-browser media UI but that
 * a headless Cobalt session has no feature to trigger.
 *
 * <p>WhatsApp Web logs a dense stream of media-interaction beacons as the user
 * drives the browser client: the pre-send transcode/repair analysis pass
 * ({@code WebcMediaAnalyzed}), the media-picker/editor aggregate
 * ({@code MediaPicker}, {@code WebcMediaEditorSend}), the GIF-search funnel
 * ({@code GifSearch*}, {@code GifFromProviderSent}), the emoji/sticker trays
 * ({@code WebcEmojiOpen}, {@code StickerPickerOpened}, {@code StickerStoreOpened},
 * {@code WebcStickerMakerEvents}, {@code StickerCommonQueryToStaticServer}),
 * DOM media element decode/render ({@code WebcMediaLoad}), streaming and inline
 * video playback ({@code MediaStreamPlayback}, {@code InlineVideoPlaybackClosed},
 * {@code ChannelsVideoPlay}), the HD-media awareness nudge
 * ({@code WebHdMediaAwarenessInteraction}), the sticker-reupload peer-data
 * responder ({@code NonMessagePeerDataMediaUpload}), and the Media Hub gallery
 * journey ({@code MediaHubUserJourney}). None of these have a Cobalt counterpart:
 * Cobalt uploads and downloads media bytes through native pipelines with no
 * browser DOM, no media worker, no picker/editor UI, and no gallery surface.
 * A telemetry stream that never carried any of these events would be trivially
 * distinguishable from a real Web client.
 *
 * <p>This service closes that gap. Each per-event {@code commit} helper fabricates
 * one plausible, real-looking occurrence of its event: enum choices, counts, byte
 * sizes, and timing figures are drawn from realistic ranges (jittered per call so
 * successive sessions do not fingerprint identically), session identifiers are
 * freshly minted UUIDs, and language codes are sourced from the bound account's
 * live locale. The single public entry point {@link #emitSessionTelemetry()} fires
 * the whole block once per connected session, mirroring the once-per-connect
 * cadence of a real user's first media interactions.
 *
 * @implNote
 * This implementation fires every media beacon exactly once from
 * {@link #emitSessionTelemetry()} rather than reacting to real UI actions,
 * because Cobalt is headless and has no media UI to observe. The fabricated
 * figures are deliberately jittered through {@link SyntheticTelemetryUtils#between(long, long)} so the
 * emitted values vary across sessions the way an organic interaction stream
 * would, instead of shipping fixed constants that would fingerprint every Cobalt
 * session identically.
 *
 * @see WamService
 */
@WhatsAppWebModule(moduleName = "WAWebWebcMediaAnalyzedWamEvent")
@WhatsAppWebModule(moduleName = "WAWebMediaPickerWamEvent")
@WhatsAppWebModule(moduleName = "WAWebGifSearchResultTappedWamEvent")
@WhatsAppWebModule(moduleName = "WAWebGifFromProviderSentWamEvent")
@WhatsAppWebModule(moduleName = "WAWebGifSearchCancelledWamEvent")
@WhatsAppWebModule(moduleName = "WAWebGifSearchNoResultsWamEvent")
@WhatsAppWebModule(moduleName = "WAWebGifSearchSessionStartedWamEvent")
@WhatsAppWebModule(moduleName = "WAWebWebcEmojiOpenWamEvent")
@WhatsAppWebModule(moduleName = "WAWebWebcMediaLoadWamEvent")
@WhatsAppWebModule(moduleName = "WAWebMediaStreamPlaybackWamEvent")
@WhatsAppWebModule(moduleName = "WAWebStickerPickerOpenedWamEvent")
@WhatsAppWebModule(moduleName = "WAWebInlineVideoPlaybackClosedWamEvent")
@WhatsAppWebModule(moduleName = "WAWebStickerCommonQueryToStaticServerWamEvent")
@WhatsAppWebModule(moduleName = "WAWebWebcMediaEditorSendWamEvent")
@WhatsAppWebModule(moduleName = "WAWebStickerStoreOpenedWamEvent")
@WhatsAppWebModule(moduleName = "WAWebWebcStickerMakerEventsWamEvent")
@WhatsAppWebModule(moduleName = "WAWebNonMessagePeerDataMediaUploadWamEvent")
@WhatsAppWebModule(moduleName = "WAWebWebHdMediaAwarenessInteractionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebChannelsVideoPlayWamEvent")
@WhatsAppWebModule(moduleName = "WAWebMediaHubUserJourneyWamEvent")
public final class SyntheticMediaTelemetry {
    /**
     * The bound WhatsApp client whose account store supplies the live locale
     * used to source realistic language codes for the fabricated GIF-search and
     * sticker-query events.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The WAM service through which every fabricated media event is committed
     * for batched upload.
     */
    private final WamService wamService;

    /**
     * Constructs a new {@code SyntheticMediaTelemetry} bound to the given client
     * and WAM service.
     *
     * @param client     the WhatsApp client whose store supplies the live locale,
     *                   must not be {@code null}
     * @param wamService the WAM service used to commit the fabricated events, must
     *                   not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SyntheticMediaTelemetry(LinkedWhatsAppClient client, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
    }

    /**
     * Emits the full block of synthetic media-surface telemetry for the current
     * session.
     *
     * <p>This fires every media beacon exactly once, in the rough order a real
     * user's first media interactions would surface them: media analysis and
     * loading, the picker/editor aggregate, the GIF and emoji and sticker trays,
     * streaming and inline and channel video playback, the HD-media nudge, the
     * sticker-reupload responder, and the Media Hub journey. It is intended to be
     * called once per connected session from the client's socket-open callback;
     * none of these events are periodic by nature, so there is no recurring
     * cadence to schedule.
     */
    public void emitSessionTelemetry() {
        commitWebcMediaAnalyzed();
        commitWebcMediaLoad();
        commitMediaPicker();
        commitWebcMediaEditorSend();
        commitWebcEmojiOpen();
        commitGifSearchSessionStarted();
        commitGifSearchResultTapped();
        commitGifSearchNoResults();
        commitGifSearchCancelled();
        commitGifFromProviderSent();
        commitStickerPickerOpened();
        commitStickerStoreOpened();
        commitStickerCommonQueryToStaticServer();
        commitWebcStickerMakerEvents();
        commitNonMessagePeerDataMediaUpload();
        commitMediaStreamPlayback();
        commitInlineVideoPlaybackClosed();
        commitChannelsVideoPlay();
        commitWebHdMediaAwarenessInteraction();
        commitMediaHubUserJourney();
    }

    /**
     * Fabricates and commits one {@code WebcMediaAnalyzed} event.
     *
     * <p>Models the browser media-worker pre-send analysis pass reporting a
     * supported MP4 file that was validated in a few hundred milliseconds.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcMediaAnalyzedWamEvent", exports = "WebcMediaAnalyzedWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitWebcMediaAnalyzed() {
        wamService.commit(new WebcMediaAnalyzedEventBuilder()
                .webcMediaSupported(true)
                .webcMediaExtensions("mp4")
                .webcMediaAnalyzeT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.between(90, 380)))
                .build());
    }

    /**
     * Fabricates and commits one {@code WebcMediaLoad} event.
     *
     * <p>Models a media blob successfully decoded and rendered into a DOM element
     * in a few tens of milliseconds.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcMediaLoadWamEvent", exports = "WebcMediaLoadWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitWebcMediaLoad() {
        wamService.commit(new WebcMediaLoadEventBuilder()
                .webcMediaLoadResult(WebcMediaLoadResultCode.SUCCESS)
                .webcMediaLoadT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.between(4, 55)))
                .build());
    }

    /**
     * Fabricates and commits one {@code MediaPicker} aggregate event.
     *
     * <p>Models a single unedited photo sent to one recipient from the chat photo
     * library at automatic quality, with the picker session spanning a few seconds
     * and no crossposting or view-once toggles engaged.
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaPickerWamEvent", exports = "MediaPickerWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitMediaPicker() {
        wamService.commit(new MediaPickerEventBuilder()
                .mediaType(MediaType.PHOTO)
                .mediaPickerOrigin(MediaPickerOriginType.CHAT_PHOTO_LIBRARY)
                .mediaPickerSent(1)
                .mediaPickerSentUnchanged(1)
                .mediaPickerChanged(0)
                .mediaPickerDeleted(0)
                .chatRecipients(1)
                .statusRecipients(0)
                .isViewOnce(false)
                .hasCollectionCaption(false)
                .itemCaptionCount(1)
                .hdToggleEligible(true)
                .hdToggleState(MediaQuality.AUTO)
                .hdToggleChange(0)
                .photoQualitySetting(MediaQuality.AUTO)
                .videoQualitySetting(MediaQuality.AUTO)
                .audienceSelectorClicked(false)
                .audienceSelectorUpdated(false)
                .isFbCrosspostingEnabled(false)
                .isIgCrosspostingEnabled(false)
                .mediaPickerSessionId(SyntheticTelemetryUtils.newSessionId())
                .pickerSessionId(SyntheticTelemetryUtils.between(1_000_000L, 9_999_999L))
                .mediaPickerT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.between(1_500, 9_000)))
                .build());
    }

    /**
     * Fabricates and commits one {@code WebcMediaEditorSend} event.
     *
     * <p>Models a single image sent from the in-app editor after one text layer
     * and one emoji layer were applied.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcMediaEditorSendWamEvent", exports = "WebcMediaEditorSendWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitWebcMediaEditorSend() {
        wamService.commit(new WebcMediaEditorSendEventBuilder()
                .imageCount(1)
                .editedImageCount(1)
                .paintedImageCount(0)
                .textLayerCount(1)
                .emojiLayerCount(1)
                .stickerLayerCount(0)
                .blurImageCount(0)
                .build());
    }

    /**
     * Fabricates and commits one {@code WebcEmojiOpen} event.
     *
     * <p>Models the user opening the emoji tab of the expression tray.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcEmojiOpenWamEvent", exports = "WebcEmojiOpenWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitWebcEmojiOpen() {
        wamService.commit(new WebcEmojiOpenEventBuilder()
                .webcEmojiOpenTab("emoji")
                .build());
    }

    /**
     * Fabricates and commits one {@code GifSearchSessionStarted} event.
     *
     * <p>Models the user opening the GIF search surface backed by the Tenor
     * provider.
     */
    @WhatsAppWebExport(moduleName = "WAWebGifSearchSessionStartedWamEvent", exports = "GifSearchSessionStartedWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitGifSearchSessionStarted() {
        wamService.commit(new GifSearchSessionStartedEventBuilder()
                .gifSearchProvider(GifSearchProvider.TENOR)
                .build());
    }

    /**
     * Fabricates and commits one {@code GifSearchResultTapped} event.
     *
     * <p>Models the user tapping a result at a plausible rank within the Tenor
     * result grid.
     */
    @WhatsAppWebExport(moduleName = "WAWebGifSearchResultTappedWamEvent", exports = "GifSearchResultTappedWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitGifSearchResultTapped() {
        wamService.commit(new GifSearchResultTappedEventBuilder()
                .gifSearchProvider(GifSearchProvider.TENOR)
                .rank(SyntheticTelemetryUtils.between(0, 24))
                .build());
    }

    /**
     * Fabricates and commits one {@code GifSearchNoResults} event.
     *
     * <p>Models a Tenor search that returned nothing, tagged with the account's
     * live language code as both the UI and input language.
     */
    @WhatsAppWebExport(moduleName = "WAWebGifSearchNoResultsWamEvent", exports = "GifSearchNoResultsWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitGifSearchNoResults() {
        var language = languageCode();
        wamService.commit(new GifSearchNoResultsEventBuilder()
                .gifSearchProvider(GifSearchProvider.TENOR)
                .languageCode(language)
                .inputLanguageCode(language)
                .build());
    }

    /**
     * Fabricates and commits one {@code GifSearchCancelled} event.
     *
     * <p>Models the user dismissing the Tenor-backed GIF search surface.
     */
    @WhatsAppWebExport(moduleName = "WAWebGifSearchCancelledWamEvent", exports = "GifSearchCancelledWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitGifSearchCancelled() {
        wamService.commit(new GifSearchCancelledEventBuilder()
                .gifSearchProvider(GifSearchProvider.TENOR)
                .build());
    }

    /**
     * Fabricates and commits one {@code GifFromProviderSent} event.
     *
     * <p>Models the user sending a GIF sourced from the Tenor provider.
     */
    @WhatsAppWebExport(moduleName = "WAWebGifFromProviderSentWamEvent", exports = "GifFromProviderSentWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitGifFromProviderSent() {
        wamService.commit(new GifFromProviderSentEventBuilder()
                .gifSearchProvider(GifSearchProvider.TENOR)
                .build());
    }

    /**
     * Fabricates and commits one {@code StickerPickerOpened} event.
     *
     * <p>Models the user opening the sticker tray; the event carries no fields.
     */
    @WhatsAppWebExport(moduleName = "WAWebStickerPickerOpenedWamEvent", exports = "StickerPickerOpenedWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitStickerPickerOpened() {
        wamService.commit(new StickerPickerOpenedEventBuilder()
                .build());
    }

    /**
     * Fabricates and commits one {@code StickerStoreOpened} event.
     *
     * <p>Models the user opening the sticker store from the media editor, the sole
     * defined origin.
     */
    @WhatsAppWebExport(moduleName = "WAWebStickerStoreOpenedWamEvent", exports = "StickerStoreOpenedWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitStickerStoreOpened() {
        wamService.commit(new StickerStoreOpenedEventBuilder()
                .stickerStoreOpenedOrigin(StickerStoreOpenedOriginType.MEDIA_EDITOR)
                .build());
    }

    /**
     * Fabricates and commits one {@code StickerCommonQueryToStaticServer} event.
     *
     * <p>Models a successful sticker-store catalog fetch from the static server,
     * carrying a realistic query string tagged with the account's language and a
     * latency of a few hundred milliseconds.
     */
    @WhatsAppWebExport(moduleName = "WAWebStickerCommonQueryToStaticServerWamEvent", exports = "StickerCommonQueryToStaticServerWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitStickerCommonQueryToStaticServer() {
        wamService.commit(new StickerCommonQueryToStaticServerEventBuilder()
                .queryType(QueryType.STICKER_STORE_DATA)
                .httpResponseCode(200)
                .params("count=30&lg=" + languageCode())
                .queryLatencyMs(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.between(45, 480)))
                .build());
    }

    /**
     * Fabricates and commits one {@code WebcStickerMakerEvents} event.
     *
     * <p>Models the entry action of the sticker-maker tool, the button tap that
     * opens it.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcStickerMakerEventsWamEvent", exports = "WebcStickerMakerEventsWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitWebcStickerMakerEvents() {
        wamService.commit(new WebcStickerMakerEventsEventBuilder()
                .stickerMakerEventName(WebcStickerMakerEventNameType.STICKER_MAKER_BUTTON_TAP)
                .build());
    }

    /**
     * Fabricates and commits one {@code NonMessagePeerDataMediaUpload} event.
     *
     * <p>Models this device successfully responding to a single sticker-reupload
     * peer-data request from a sibling device.
     */
    @WhatsAppWebExport(moduleName = "WAWebNonMessagePeerDataMediaUploadWamEvent", exports = "NonMessagePeerDataMediaUploadWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitNonMessagePeerDataMediaUpload() {
        wamService.commit(new NonMessagePeerDataMediaUploadEventBuilder()
                .peerDataRequestType(PeerDataRequestType.UPLOAD_STICKER)
                .peerDataRequestCount(1)
                .peerDataSuccessUploadCount(1)
                .peerDataSuccessInlineNoUploadCount(0)
                .peerDataExistingDataNoUploadCount(0)
                .peerDataErrorCount(0)
                .peerDataNotFoundCount(0)
                .peerDataResponseResult(PeerDataResponseResultType.SUCCESS)
                .peerDataRequestSessionId(SyntheticTelemetryUtils.newSessionId())
                .build());
    }

    /**
     * Fabricates and commits one {@code MediaStreamPlayback} event.
     *
     * <p>Models a received conversation video that was streamed to completion:
     * a multi-megabyte file played once from start to finish with a short initial
     * buffering pause and no seeks, rebuffering, or errors.
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaStreamPlaybackWamEvent", exports = "MediaStreamPlaybackWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitMediaStreamPlayback() {
        var durationSeconds = SyntheticTelemetryUtils.between(12, 180);
        var sizeBytes = SyntheticTelemetryUtils.between(400_000L, 12_000_000L);
        wamService.commit(new MediaStreamPlaybackEventBuilder()
                .mediaType(MediaType.VIDEO)
                .mediaSize((double) sizeBytes)
                .bytesTransferred((double) sizeBytes)
                .bytesDownloadedStart((double) SyntheticTelemetryUtils.between(16_000L, 64_000L))
                .didPlay(true)
                .playbackCount(1)
                .playbackOrigin(PlaybackOriginType.CONVERSATION)
                .playbackState(PlaybackStateType.ENDED)
                .playbackError(0)
                .videoDuration(durationSeconds)
                .overallT(SyntheticTelemetryUtils.timer(durationSeconds * 1000 + SyntheticTelemetryUtils.between(200, 1_500)))
                .overallPlayT(SyntheticTelemetryUtils.timer(durationSeconds * 1000))
                .initialBufferingT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.between(80, 600)))
                .totalRebufferingT(SyntheticTelemetryUtils.timer(0))
                .totalRebufferingCount(0)
                .seekCount(0)
                .forcedPlayCount(0)
                .build());
    }

    /**
     * Fabricates and commits one {@code InlineVideoPlaybackClosed} event.
     *
     * <p>Models a received one-to-one YouTube inline preview that was played for
     * part of its length before the viewer closed it, with no stall and no
     * click-to-action.
     */
    @WhatsAppWebExport(moduleName = "WAWebInlineVideoPlaybackClosedWamEvent", exports = "InlineVideoPlaybackClosedWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitInlineVideoPlaybackClosed() {
        var watchMillis = SyntheticTelemetryUtils.between(3_000, 45_000);
        wamService.commit(new InlineVideoPlaybackClosedEventBuilder()
                .inlineVideoType(InlineVideoType.YOUTUBE)
                .messageType(MessageType.INDIVIDUAL)
                .inlineVideoPlayed(true)
                .inlineVideoComplete(false)
                .inlineVideoCompletionRate(SyntheticTelemetryUtils.between(20, 95))
                .inlineVideoWatchT(SyntheticTelemetryUtils.timer(watchMillis))
                .inlineVideoDurationT(SyntheticTelemetryUtils.timer(watchMillis + SyntheticTelemetryUtils.between(5_000, 30_000)))
                .inlineVideoPlayStartT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.between(200, 1_500)))
                .inlineVideoStallT(SyntheticTelemetryUtils.timer(0))
                .inlineVideoHasRcat(false)
                .isSentByMe(false)
                .chatSize(1)
                .build());
    }

    /**
     * Fabricates and commits one {@code ChannelsVideoPlay} event.
     *
     * <p>Models a 720p channel (newsletter) post video streamed to completion
     * once, with a fabricated channel id and post id, a short initial buffering
     * pause, and a plausible multi-megabyte size.
     */
    @WhatsAppWebExport(moduleName = "WAWebChannelsVideoPlayWamEvent", exports = "ChannelsVideoPlayWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitChannelsVideoPlay() {
        var durationSeconds = SyntheticTelemetryUtils.between(15, 240);
        var sizeBytes = SyntheticTelemetryUtils.between(800_000L, 30_000_000L);
        wamService.commit(new ChannelsVideoPlayEventBuilder()
                .cid("120363" + SyntheticTelemetryUtils.between(100_000_000_000L, 999_999_999_999L) + "@newsletter")
                .postId(String.valueOf(SyntheticTelemetryUtils.between(1, 9_999)))
                .videoPlayOrigin(VideoPlayOrigin.CHANNELS)
                .videoPlayResult(VideoPlayResult.OK)
                .videoPlayType(VideoPlayType.STREAM)
                .videoDuration(durationSeconds)
                .videoSize((double) sizeBytes)
                .width(1_280)
                .height(720)
                .autoPlayT(SyntheticTelemetryUtils.between(50, 400))
                .videoPlayT(durationSeconds * 1000)
                .videoInitialBufferingT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.between(80, 700)))
                .finishCount(1)
                .watchingModule("channel")
                .build());
    }

    /**
     * Fabricates and commits one {@code WebHdMediaAwarenessInteraction} event.
     *
     * <p>Models the user accepting the HD-media awareness nudge by selecting HD.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebHdMediaAwarenessInteractionWamEvent", exports = "WebHdMediaAwarenessInteractionWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitWebHdMediaAwarenessInteraction() {
        wamService.commit(new WebHdMediaAwarenessInteractionEventBuilder()
                .hdMediaSelected(true)
                .build());
    }

    /**
     * Fabricates and commits one {@code MediaHubUserJourney} event.
     *
     * <p>Models the first action of a Media Hub session opened from a contact's
     * info screen onto the media surface, carrying fresh session identifiers.
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHubUserJourneyWamEvent", exports = "MediaHubUserJourneyWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitMediaHubUserJourney() {
        wamService.commit(new MediaHubUserJourneyEventBuilder()
                .mediaHubAction(ActionCode.OPEN_MEDIA_HUB)
                .mediaHubEntryPoint(EntryPointType.CONTACT_INFO)
                .mediaHubSurface(SurfaceCode.MEDIA)
                .mediaHubSequenceNumber(1)
                .mediaHubSessionId(SyntheticTelemetryUtils.newSessionId())
                .unifiedSessionId(SyntheticTelemetryUtils.newSessionId())
                .customFields("{\"source\":\"contact_info\"}")
                .build());
    }

    /**
     * Resolves the lowercase language subtag reported in the fabricated
     * GIF-search and sticker-query events from the bound account's live locale.
     *
     * <p>For an IETF tag such as {@code "it-IT"} this returns {@code "it"}; when
     * the account carries no locale it falls back to {@code "en"}.
     *
     * @return the lowercase language subtag, never {@code null}
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
                .orElse("en");
    }



}
