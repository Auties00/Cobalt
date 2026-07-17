package com.github.auties00.cobalt.wam.synthetic;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientType;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.PttDailyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PttEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PttMessageUserJourneyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PttPlaybackEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.ChatbarInitialState;
import com.github.auties00.cobalt.wire.wam.type.PttMessageUserJourneyAction;
import com.github.auties00.cobalt.wire.wam.type.PttPlaybackSpeedType;
import com.github.auties00.cobalt.wire.wam.type.PttResultType;
import com.github.auties00.cobalt.wire.wam.type.PttSourceType;
import com.github.auties00.cobalt.wire.wam.type.PttStreamType;
import com.github.auties00.cobalt.wire.wam.type.PttTriggerType;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;
import com.github.auties00.cobalt.wire.wam.type.UserJourneyChatType;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Emits the synthetic voice-note (push-to-talk) WAM telemetry that a genuine
 * WhatsApp Web session produces around recording, playing back and aggregating
 * voice messages, so a Cobalt session's metrics stream carries the same
 * voice-note fingerprint a real browser client would.
 *
 * <p>WhatsApp Web instruments four voice-note surfaces that have no headless
 * counterpart. The composer recording session logs a {@code Ptt} event
 * ({@link com.github.auties00.cobalt.wire.wam.event.PttEvent}, id 458) when a
 * recording completes, carrying the result, source, duration, encoded size and
 * the draft-review interaction counters. The playback logger logs a
 * {@code PttPlayback} event
 * ({@link com.github.auties00.cobalt.wire.wam.event.PttPlaybackEvent}, id 2044) when a
 * note finishes playing, carrying the stream type, trigger, playback speed,
 * duration, played fraction and mini-player interaction counters. The daily
 * stats task flushes a {@code PttDaily} event
 * ({@link com.github.auties00.cobalt.wire.wam.event.PttDailyEvent}, id 2938) that
 * aggregates the day's record, send, cancel, lock, draft-review, playback,
 * fast-playback, paused-record, out-of-chat and stop-tap counts partitioned by
 * conversation kind. The chatbar funnel logs a {@code PttMessageUserJourney}
 * event ({@link com.github.auties00.cobalt.wire.wam.event.PttMessageUserJourneyEvent},
 * id 5402) at each stage of recording, previewing and sending a note.
 *
 * <p>None of these map to a Cobalt feature: Cobalt is a headless JVM client with
 * no microphone capture, no {@code MediaRecorder} opus pipeline, no audio
 * playback controller or mini-player, and no daily-stats aggregation task; voice
 * messages are sent from pre-encoded bytes and stored or forwarded, never
 * recorded or rendered. Their absence is nonetheless a fingerprint, so this
 * service fabricates one coherent, host-and-store-derived snapshot per
 * connection. Counts that partition by conversation kind are grounded in the
 * live store's chat composition (so a session with no group chats reports no
 * group voice-note activity), and only the fields the corresponding WA Web
 * reporter actually populates are set, leaving the native-only recorder-timing,
 * loudness and audio-route fields unset exactly as a real browser client leaves
 * them.
 *
 * @implNote
 * This implementation fires only for a {@link LinkedWhatsAppClientType#WEB}
 * session and returns without emitting anything for a
 * {@link LinkedWhatsAppClientType#MOBILE} session: every event here is committed
 * by a WA Web reporter module ({@code WAWebPttComposerRecordingSession},
 * {@code WAWebPttPlaybackLogger}, {@code WAWebTasksDailyStatsTask},
 * {@code WAWebPttMessageUserJourneyLogger}), and a primary phone-registered
 * device instead emits the native app's own voice-note event identifiers, which
 * are not modeled here. Each commit sets exactly the subset of fields its WA Web
 * reporter populates; the recorder-timing buckets, loudness figures, waveform
 * result and per-route audio timers are native-only fields that the browser
 * reporters never set, so they are deliberately left unset.
 *
 * @see WamService
 */
@WhatsAppWebModule(moduleName = "WAWebPttComposerRecordingSession")
@WhatsAppWebModule(moduleName = "WAWebPttPlaybackLogger")
@WhatsAppWebModule(moduleName = "WAWebTasksDailyStatsTask")
@WhatsAppWebModule(moduleName = "WAWebPttMessageUserJourneyLogger")
public final class SyntheticVoiceNoteTelemetry {
    /**
     * The bound WhatsApp client whose store supplies the live chat composition
     * that grounds the fabricated per-conversation-kind voice-note counters.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The WAM service through which every fabricated voice-note event is
     * committed for batched upload.
     */
    private final WamService wamService;

    /**
     * The stable per-session shared-session identifier reported in the
     * user-journey event's {@code appSessionId} field.
     *
     * <p>WhatsApp Web derives this from its shared-session module and keeps it
     * constant for the lifetime of the app session, so a single value is minted
     * once per {@code SyntheticVoiceNoteTelemetry} instance and reused.
     */
    private final String appSessionId;

    /**
     * The stable per-session unified-session identifier reported in the
     * user-journey event's {@code unifiedSessionId} field.
     *
     * <p>Like {@link #appSessionId} this is session-scoped on WA Web, so a single
     * value is minted once per instance and reused across journeys.
     */
    private final String unifiedSessionId;

    /**
     * Constructs a new {@code SyntheticVoiceNoteTelemetry} bound to the given
     * client and WAM service.
     *
     * @param client     the WhatsApp client whose store is sampled, must not be
     *                   {@code null}
     * @param wamService the WAM service used to commit the emitted events, must
     *                   not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SyntheticVoiceNoteTelemetry(LinkedWhatsAppClient client, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
        this.appSessionId = UUID.randomUUID().toString();
        this.unifiedSessionId = UUID.randomUUID().toString();
    }

    /**
     * Emits the once-per-connection voice-note telemetry snapshot on a dedicated
     * virtual thread.
     *
     * <p>This is the single entry point the client drives after the socket opens.
     * It fabricates and commits one coherent voice-note snapshot: a completed
     * recording ({@code Ptt}), a completed playback ({@code PttPlayback}), the
     * day's aggregated interaction counters ({@code PttDaily}) partitioned by the
     * live store's chat composition, and one chatbar send funnel
     * ({@code PttMessageUserJourney}).
     *
     * <p>The work runs off the socket-open thread because the daily aggregation
     * walks every chat to classify it by conversation kind; the WAM commits
     * themselves are cheap enqueues.
     *
     * @apiNote
     * The {@code PttDaily} aggregate is daily by nature on WA Web (its
     * daily-stats task flushes the accumulated counters once per day), whereas
     * the recording, playback and funnel events are per-interaction. This method
     * folds one representative sample of each into the per-connection snapshot; a
     * caller that wants the genuine daily cadence for the aggregate may invoke
     * this method again on a twenty-four-hour timer.
     *
     * @implNote
     * This implementation fires only for a {@link LinkedWhatsAppClientType#WEB}
     * session and returns immediately for a {@link LinkedWhatsAppClientType#MOBILE}
     * session, because the emitted events all belong to WA Web reporter modules
     * that a primary phone-registered device never drives.
     */
    public void emitSessionTelemetry() {
        if (client.store().accountStore().clientType() != LinkedWhatsAppClientType.WEB) {
            return;
        }
        Thread.ofVirtual()
                .name("voice-note-telemetry")
                .start(this::runBurst);
    }

    /**
     * Builds and commits the full voice-note snapshot.
     *
     * <p>The store's chats are classified once by conversation kind so the daily
     * counters and the funnel's chat-type reflect the real store composition, and
     * the recording, playback, daily and funnel events are then committed in
     * turn.
     */
    private void runBurst() {
        var store = client.store();
        var individual = 0;
        var group = 0;
        var broadcast = 0;
        var newsletter = 0;
        for (var chat : store.chatStore().chats()) {
            switch (chat.jid().server().type()) {
                case GROUP_OR_COMMUNITY -> group++;
                case BROADCAST -> broadcast++;
                case NEWSLETTER -> newsletter++;
                default -> individual++;
            }
        }

        commitPttRecording();
        commitPttPlayback();
        commitPttDaily(individual, group, broadcast, newsletter);
        commitPttMessageUserJourney(dominantChatType(individual, group, broadcast, newsletter));
    }

    /**
     * Fabricates and commits the {@code Ptt} (id 458) recording-completed event.
     *
     * <p>The snapshot describes a voice note recorded in a conversation and sent:
     * the source is the conversation composer, the result is a successful send,
     * and the duration and encoded size are a coherent pair for a short opus
     * recording (the duration is whole seconds and the size is rounded to
     * kilobytes, matching how the WA Web recording session rounds them). The
     * draft-review counters describe whether the user previewed the draft before
     * sending; the Meta-AI-thread flag is false for an ordinary conversation.
     *
     * @implNote
     * This implementation sets exactly the fields
     * {@code WAWebPttComposerRecordingSession} populates when it commits the event
     * on send (source, result, duration, size, stop/draft-preview flag, draft
     * play and seek counts, pause count, Meta-AI-thread flag). The recorder
     * callback and encode timing buckets, the loudness figures, the ogg and opus
     * write buckets, the waveform result and the audio-engine identifier are
     * native-app-only fields that the browser recording session never sets, so
     * they are left unset.
     */
    @WhatsAppWebExport(moduleName = "WAWebPttWamEvent", exports = "PttWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebPttComposerRecordingSession", exports = "_sendPttWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitPttRecording() {
        var seconds = SyntheticTelemetryUtils.jitter(6, 12);
        var previewSeen = ThreadLocalRandom.current().nextInt(100) < 35;
        wamService.commit(new PttEventBuilder()
                .pttSource(PttSourceType.FROM_CONVERSATION)
                .pttResult(PttResultType.SENT)
                .pttDuration(SyntheticTelemetryUtils.timer(seconds * 1000))
                .pttSize(Math.round(seconds * 2.4) * 1000.0)
                .pttStop(previewSeen)
                .pttDraftPlayCnt(previewSeen ? SyntheticTelemetryUtils.jitter(1, 2) : 0)
                .pttDraftSeekCnt(previewSeen ? SyntheticTelemetryUtils.jitter(0, 2) : 0)
                .pttPauseCnt(SyntheticTelemetryUtils.jitter(0, 1))
                .isMetaAiThread(false)
                .build());
    }

    /**
     * Fabricates and commits the {@code PttPlayback} (id 2044)
     * playback-completed event.
     *
     * <p>The snapshot describes a received voice note played to the end in the
     * chat view: the stream is opus (the format WA Web serves voice notes in),
     * the playback is a manual single play at normal speed with no seeks or speed
     * changes, the played fraction is close to one because the note reached its
     * end, and the mini-player counters are zero because the note was played
     * inside the chat rather than in the out-of-chat mini player.
     *
     * @implNote
     * This implementation sets exactly the fields {@code WAWebPttPlaybackLogger}
     * populates in its {@code commit} path (trigger, stream type, playback speed
     * and speed-change count, seek count, failure flag, duration, played
     * fraction, out-of-chat flag and the three mini-player counters). The player
     * type, per-route audio timers, audio-stream type, playback-overall and
     * player-init timers, main-thread-block flag and volume-after-max count are
     * native-app-only fields the browser logger never sets, so they are left
     * unset.
     */
    @WhatsAppWebExport(moduleName = "WAWebPttPlaybackWamEvent", exports = "PttPlaybackWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebPttPlaybackLogger", exports = "commit", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitPttPlayback() {
        var seconds = SyntheticTelemetryUtils.jitter(5, 14);
        wamService.commit(new PttPlaybackEventBuilder()
                .pttTrigger(PttTriggerType.MANUAL)
                .pttType(PttStreamType.OPUS)
                .pttPlaybackSpeed(PttPlaybackSpeedType.SPEED_1)
                .pttPlaybackSpeedCnt(0)
                .pttSeekCnt(0)
                .pttPlaybackFailed(false)
                .pttDuration(SyntheticTelemetryUtils.timer(seconds * 1000))
                .pttPlayedPct(jitterFraction(0.92, 0.08))
                .pttPlayedOutOfChat(false)
                .pttMiniPlayerPauseCnt(0)
                .pttMiniPlayerClose(false)
                .pttMiniPlayerClick(0)
                .build());
    }

    /**
     * Fabricates and commits the {@code PttDaily} (id 2938) daily-aggregate
     * event.
     *
     * <p>Each of the ten interaction kinds (record, send, cancel, lock,
     * draft-review, playback, fast-playback, paused-record, out-of-chat, stop-tap)
     * is reported for each of the five conversation kinds (individual, group,
     * broadcast, newsletter, interop). The counts are grounded in the live store:
     * a conversation kind with no chats reports no voice-note activity, and the
     * interop bucket is always empty because a linked-web store holds no interop
     * threads. Within each populated kind the counts form a plausible day of
     * activity in which records slightly exceed sends, every record ends with a
     * stop tap, and playbacks scale above the send volume because received notes
     * are also played.
     *
     * @param individual the number of individual chats in the store
     * @param group      the number of group and community chats in the store
     * @param broadcast  the number of broadcast and status chats in the store
     * @param newsletter the number of newsletter and channel chats in the store
     */
    @WhatsAppWebExport(moduleName = "WAWebPttDailyWamEvent", exports = "PttDailyWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebTasksDailyStatsTask", exports = "flushPttDailyStats", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitPttDaily(int individual, int group, int broadcast, int newsletter) {
        var ind = dailyKinds(individual);
        var grp = dailyKinds(group);
        var bcast = dailyKinds(broadcast);
        var news = dailyKinds(newsletter);
        var interop = new long[10];
        wamService.commit(new PttDailyEventBuilder()
                .pttRecordIndividual(ind[0])
                .pttSendIndividual(ind[1])
                .pttCancelIndividual(ind[2])
                .pttLockIndividual(ind[3])
                .pttDraftReviewIndividual(ind[4])
                .pttPlaybackIndividual(ind[5])
                .pttFastplaybackIndividual(ind[6])
                .pttPausedRecordIndividual(ind[7])
                .pttOutOfChatIndividual(ind[8])
                .pttStopTapIndividual(ind[9])
                .pttRecordGroup(grp[0])
                .pttSendGroup(grp[1])
                .pttCancelGroup(grp[2])
                .pttLockGroup(grp[3])
                .pttDraftReviewGroup(grp[4])
                .pttPlaybackGroup(grp[5])
                .pttFastplaybackGroup(grp[6])
                .pttPausedRecordGroup(grp[7])
                .pttOutOfChatGroup(grp[8])
                .pttStopTapGroup(grp[9])
                .pttRecordBroadcast(bcast[0])
                .pttSendBroadcast(bcast[1])
                .pttCancelBroadcast(bcast[2])
                .pttLockBroadcast(bcast[3])
                .pttDraftReviewBroadcast(bcast[4])
                .pttPlaybackBroadcast(bcast[5])
                .pttFastplaybackBroadcast(bcast[6])
                .pttPausedRecordBroadcast(bcast[7])
                .pttOutOfChatBroadcast(bcast[8])
                .pttStopTapBroadcast(bcast[9])
                .pttRecordNewsletter(news[0])
                .pttSendNewsletter(news[1])
                .pttCancelNewsletter(news[2])
                .pttLockNewsletter(news[3])
                .pttDraftReviewNewsletter(news[4])
                .pttPlaybackNewsletter(news[5])
                .pttFastplaybackNewsletter(news[6])
                .pttPausedRecordNewsletter(news[7])
                .pttOutOfChatNewsletter(news[8])
                .pttStopTapNewsletter(news[9])
                .pttRecordInterop(interop[0])
                .pttSendInterop(interop[1])
                .pttCancelInterop(interop[2])
                .pttLockInterop(interop[3])
                .pttDraftReviewInterop(interop[4])
                .pttPlaybackInterop(interop[5])
                .pttFastplaybackInterop(interop[6])
                .pttPausedRecordInterop(interop[7])
                .pttOutOfChatInterop(interop[8])
                .pttStopTapInterop(interop[9])
                .build());
    }

    /**
     * Fabricates and commits the {@code PttMessageUserJourney} (id 5402) chatbar
     * send-funnel event.
     *
     * <p>The snapshot describes a completed send stage of the voice-note funnel:
     * the action is a send from the chat thread, the chatbar started empty, the
     * note carries no quoted reply, and the thread is not a Meta-AI thread. The
     * session identifiers reuse the stable per-session {@link #appSessionId} and
     * {@link #unifiedSessionId}, while the funnel identifier is minted fresh per
     * journey, matching how the WA Web logger sources them. The chat-type is the
     * dominant conversation kind in the live store, and the intensity aggregate
     * is a plausible waveform-energy figure.
     *
     * @param chatType the conversation kind reported for this journey, derived
     *                 from the live store composition
     */
    @WhatsAppWebExport(moduleName = "WAWebPttMessageUserJourneyWamEvent", exports = "PttMessageUserJourneyWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebPttMessageUserJourneyLogger", exports = "PttMessageUserJourneyLogger", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitPttMessageUserJourney(UserJourneyChatType chatType) {
        wamService.commit(new PttMessageUserJourneyEventBuilder()
                .appSessionId(appSessionId)
                .unifiedSessionId(unifiedSessionId)
                .userJourneyFunnelId(UUID.randomUUID().toString())
                .pttMessageUserJourneyAction(PttMessageUserJourneyAction.SEND)
                .uiSurface(TsSurface.CHAT_THREAD)
                .userJourneyChatType(chatType)
                .chatbarInitialState(ChatbarInitialState.EMPTY)
                .pttMessageUserJourneyContainsQuotedItem(false)
                .isMetaAiThread(false)
                .pttIntensityAggregateValue(jitterFraction(0.30, 0.40))
                .userJourneyEventMs(System.currentTimeMillis())
                .build());
    }

    /**
     * Computes a plausible day of voice-note interaction counts for one
     * conversation kind.
     *
     * <p>When the conversation kind holds no chats the returned counts are all
     * zero, so an empty kind reports no activity. Otherwise the counts are scaled
     * from the chat count with jitter: records slightly exceed sends, playbacks
     * scale above sends because received notes are also played, every record ends
     * with a stop tap, and the cancel, lock, draft-review, fast-playback,
     * paused-record and out-of-chat counts are small non-negative values.
     *
     * @param chats the number of chats of the conversation kind
     * @return a ten-element array of counts ordered record, send, cancel, lock,
     *         draft-review, playback, fast-playback, paused-record, out-of-chat,
     *         stop-tap
     */
    private static long[] dailyKinds(int chats) {
        if (chats <= 0) {
            return new long[10];
        }
        var record = Math.max(1, (long) (chats * 0.6)) + SyntheticTelemetryUtils.jitter(0, 3);
        var send = Math.max(1, (long) (chats * 0.5)) + SyntheticTelemetryUtils.jitter(0, 2);
        var cancel = SyntheticTelemetryUtils.jitter(0, 2);
        var lock = SyntheticTelemetryUtils.jitter(0, 1);
        var draftReview = SyntheticTelemetryUtils.jitter(0, 2);
        var playback = Math.max(1, (long) (chats * 1.2)) + SyntheticTelemetryUtils.jitter(0, 5);
        var fastPlayback = SyntheticTelemetryUtils.jitter(0, 2);
        var pausedRecord = SyntheticTelemetryUtils.jitter(0, 1);
        var outOfChat = SyntheticTelemetryUtils.jitter(0, 2);
        return new long[]{record, send, cancel, lock, draftReview, playback, fastPlayback, pausedRecord, outOfChat, record};
    }

    /**
     * Selects the dominant conversation kind from the store's chat composition
     * for the user-journey event's chat-type field.
     *
     * <p>The kind with the most chats wins; ties resolve in favour of individual,
     * then group, then broadcast, then newsletter. When the store holds no chats
     * the kind defaults to individual, the most common voice-note context.
     * Newsletter chats map to the channel journey kind.
     *
     * @param individual the number of individual chats
     * @param group      the number of group and community chats
     * @param broadcast  the number of broadcast and status chats
     * @param newsletter the number of newsletter and channel chats
     * @return the {@link UserJourneyChatType} for the dominant conversation kind
     */
    private static UserJourneyChatType dominantChatType(int individual, int group, int broadcast, int newsletter) {
        var best = UserJourneyChatType.INDIVIDUAL;
        var bestCount = individual;
        if (group > bestCount) {
            best = UserJourneyChatType.GROUP;
            bestCount = group;
        }
        if (broadcast > bestCount) {
            best = UserJourneyChatType.BROADCAST;
            bestCount = broadcast;
        }
        if (newsletter > bestCount) {
            best = UserJourneyChatType.CHANNEL;
        }
        return best;
    }



    /**
     * Returns the given base fraction increased by a random offset up to the
     * given spread.
     *
     * <p>Used to fabricate the played-fraction and waveform-intensity fields as
     * plausible non-constant values in a band around their base rather than a
     * fixed constant that would repeat across sessions.
     *
     * @param base   the lower bound of the returned fraction
     * @param spread the width of the random offset added to {@code base}
     * @return {@code base} plus a random offset in the range zero (inclusive) to
     *         {@code spread} (exclusive)
     */
    private static double jitterFraction(double base, double spread) {
        return base + ThreadLocalRandom.current().nextDouble() * spread;
    }
}
