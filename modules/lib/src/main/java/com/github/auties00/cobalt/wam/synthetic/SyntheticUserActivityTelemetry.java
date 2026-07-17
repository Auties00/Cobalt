package com.github.auties00.cobalt.wam.synthetic;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientType;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.Chat;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.TestAnonymousDailyIdEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.TestAnonymousMonthlyIdEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.TsBitArrayEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.TsExternalEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.TsNavigationEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.UserActivityEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.ThreadType;
import com.github.auties00.cobalt.wire.wam.type.TsExternalEventSource;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;
import com.github.auties00.cobalt.wire.wam.type.TypeOfGroupEnum;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Emits the synthetic WhatsApp Web user-activity and time-spent WAM telemetry
 * that a genuine WA Web session produces over its lifetime, so a Cobalt
 * session's metrics stream is indistinguishable from a real browser's on the
 * engagement surfaces.
 *
 * <p>WhatsApp Web runs a foreground engagement tracker that samples DOM focus,
 * pointer and keyboard activity every second and periodically flushes what it
 * has seen. That tracker feeds a small family of events: {@code UserActivity}
 * (id 1384) reports a per-second activity bitmap for the raw input listeners;
 * {@code TsBitArray} (id 4332) reports the same style of bitmap for the
 * time-spent session state machine; {@code TsNavigation} (id 4334) records each
 * move between UI surfaces (chat list to a thread, thread to background) keyed to
 * the time-spent session; and {@code TsExternal} (id 4574) records dwell time in
 * external UI sessions such as voice-note recording or the in-call UI. The
 * private-stats self-test canaries {@code TestAnonymousDailyId} (id 2958) and
 * {@code TestAnonymousMonthlyId} (id 2960) are committed field-less once per day
 * by the anonymous-id private-stats validator to exercise the daily and monthly
 * de-identification pipelines. None of these map to a Cobalt feature: Cobalt is a
 * headless JVM client with no DOM, no focus events, no view surfaces and no
 * navigation. Their absence is nonetheless a fingerprint, so this service
 * fabricates one plausible, host-derived snapshot per connection.
 *
 * <p>Every field WA sets is populated with either a real store-sourced value
 * (the chat a navigation lands on, its thread type and group size) or a
 * coherent fabricated value (a random per-session time-spent id, a Web-style
 * unified session id, a per-second activity bitmap with a realistic active-ratio,
 * relative and absolute session timestamps). The two private-stats canaries are
 * committed exactly as WA commits them, with no fields, because they exist only
 * to validate the anonymous-id hashing and carry no payload.
 *
 * @implNote
 * This implementation fires the whole burst from a single
 * {@link #emitSessionTelemetry()} entry point on a dedicated virtual thread,
 * mirroring the once-per-load reporter set plus the daily private-stats canary
 * pair. It mints one time-spent session (a random 31-bit id, a Web-style unified
 * session id, and a relative offset describing a session that has been active for
 * a few minutes) and threads that session through the {@code TsBitArray},
 * {@code TsNavigation} and {@code TsExternal} events so they correlate as they
 * would on a real client. The per-second activity bitmaps are regenerated on each
 * call so their values differ across sessions rather than being a frozen
 * constant.
 *
 * @see WamService
 */
@WhatsAppWebModule(moduleName = "WAWebWamTimeSpentLogger")
@WhatsAppWebModule(moduleName = "WAWebTimeSpentLoggingBitArray")
@WhatsAppWebModule(moduleName = "WAWebTimeSpentLoggingNavigation")
@WhatsAppWebModule(moduleName = "WAWebTimeSpentLoggingExternal")
@WhatsAppWebModule(moduleName = "WAWebTimeSpentLoggingSession")
@WhatsAppWebModule(moduleName = "WAWebWamPrivateStatsUtils")
public final class SyntheticUserActivityTelemetry {
    /**
     * The number of milliseconds in a day, used to derive the Web-style unified
     * session id, which is a decimal string of a value taken modulo a seven-day
     * window.
     */
    private static final long DAY_MS = 86_400_000L;

    /**
     * The seven-day rolling window, in milliseconds, that WhatsApp Web reduces the
     * clock into when it mints a unified session id.
     *
     * <p>WA Web computes the id as {@code (now + 3 days) % 7 days} and stringifies
     * the result, so the id is a bounded rolling value rather than a monotonic
     * timestamp; this constant is the modulus of that reduction.
     */
    private static final long UNIFIED_SESSION_WINDOW_MS = 7L * DAY_MS;

    /**
     * The three-day phase offset WhatsApp Web adds to the clock before reducing it
     * into {@link #UNIFIED_SESSION_WINDOW_MS} when minting a unified session id.
     */
    private static final long UNIFIED_SESSION_OFFSET_MS = 3L * DAY_MS;

    /**
     * The bound WhatsApp client whose store supplies the live chat sampled into the
     * navigation event and whose client type gates the WEB-only burst.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The WAM service through which every fabricated user-activity, time-spent and
     * private-stats canary event is committed for batched upload.
     */
    private final WamService wamService;

    /**
     * Constructs a new {@code SyntheticUserActivityTelemetry} bound to the given
     * client and WAM service.
     *
     * @param client     the WhatsApp client whose store is sampled, must not be
     *                   {@code null}
     * @param wamService the WAM service used to commit the emitted events, must
     *                   not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SyntheticUserActivityTelemetry(LinkedWhatsAppClient client, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
    }

    /**
     * Emits the user-activity and time-spent telemetry burst on a dedicated
     * virtual thread.
     *
     * <p>This is the single entry point the client drives after the socket opens.
     * It fabricates and commits one coherent snapshot of every engagement surface a
     * genuine WA Web session reports: the raw-input activity bitmap, the time-spent
     * session bit array, one surface navigation, one external-UI dwell, and the two
     * field-less anonymous-id private-stats canaries.
     *
     * <p>The work runs off the socket-open thread because the navigation event
     * walks every chat to select a plausible destination; the WAM commits
     * themselves are cheap enqueues.
     *
     * @apiNote
     * The events in this burst are periodic by nature on WA Web: the activity and
     * time-spent bit arrays are flushed on page-hide and on a session-duration
     * timer, and the two anonymous-id canaries are emitted once per day by the
     * private-stats validator. Cobalt has no equivalent scheduler in this service,
     * so it folds one such sample into the per-connection burst; a caller that
     * wants the periodic cadence may invoke this method again on a timer (roughly
     * daily for the canaries, and on each foreground-to-background transition for
     * the time-spent events).
     *
     * @implNote
     * This implementation fires only for a {@link LinkedWhatsAppClientType#WEB}
     * session and returns without emitting anything for a
     * {@link LinkedWhatsAppClientType#MOBILE} (primary phone-registration)
     * session, because the whole time-spent and anonymous-id family is WhatsApp Web
     * browser telemetry that a primary device, which runs no browser, never emits.
     */
    public void emitSessionTelemetry() {
        if (client.store().accountStore().clientType() != LinkedWhatsAppClientType.WEB) {
            return;
        }
        Thread.ofVirtual()
                .name("user-activity-telemetry")
                .start(this::runBurst);
    }

    /**
     * Builds and commits the full user-activity and time-spent burst.
     *
     * <p>A single time-spent session is minted (a random 31-bit id, a Web-style
     * unified session id, and a relative offset describing a session that has been
     * active for a few minutes) and threaded through the bit-array, navigation and
     * external events so they correlate. The busiest chat is selected once as the
     * navigation destination so the reported thread type and group size describe a
     * real conversation.
     */
    private void runBurst() {
        var now = System.currentTimeMillis();
        var relativeMs = SyntheticTelemetryUtils.jitter(60_000, 240_000);
        var tsSessionId = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        var unifiedSessionId = unifiedSessionId(now);

        Chat busiest = null;
        for (var chat : client.store().chatStore().chats()) {
            if (busiest == null || chat.messageCount() > busiest.messageCount()) {
                busiest = chat;
            }
        }

        commitUserActivity(now, relativeMs);
        commitTsBitArray(tsSessionId, unifiedSessionId, relativeMs, now);
        commitTsNavigation(busiest, tsSessionId, unifiedSessionId, relativeMs, now);
        commitTsExternal(tsSessionId, unifiedSessionId, relativeMs, now);
        commitTestAnonymousDailyId();
        commitTestAnonymousMonthlyId();
    }

    /**
     * Fabricates and commits the {@code UserActivity} (id 1384) raw-input activity
     * bitmap.
     *
     * <p>WhatsApp Web attaches passive focus, wheel, keydown, mouseover, mousemove,
     * click and scroll listeners and records, for each elapsed second, whether any
     * of them fired. The result is a bitmap split into a low word (the first
     * thirty-two seconds), an optional high word (seconds beyond thirty-two) and a
     * bit length; the cumulative-active count and a monotonic flush sequence
     * accompany it. This method fabricates one such window: the session id is the
     * array-creation time as a decimal string, the start time is that same instant,
     * and the bitmap describes a session that was active in a realistic majority of
     * its elapsed seconds.
     *
     * @param now        the current epoch-millisecond clock reading
     * @param relativeMs the number of milliseconds the session has been active,
     *                   used to place the array start time before {@code now}
     */
    @WhatsAppWebExport(moduleName = "WAWebUserActivityWamEvent", exports = "UserActivityWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebWamTimeSpentLogger", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitUserActivity(long now, long relativeMs) {
        var startTime = now - relativeMs;
        var length = (int) SyntheticTelemetryUtils.jitter(40, 23);
        var bitmap = activityBitmap(length);
        var sequence = SyntheticTelemetryUtils.jitter(0, 4);
        var builder = new UserActivityEventBuilder()
                .userActivitySessionId(Long.toString(startTime))
                .userActivityStartTime(startTime)
                .userActivityBitmapLen(length)
                .userActivityBitmapLow(bitmap.low())
                .userActivitySessionSeq(sequence)
                .userActivitySessionCum(bitmap.cumulative() + sequence * SyntheticTelemetryUtils.jitter(18, 12));
        if (length > 32) {
            builder.userActivityBitmapHigh(bitmap.high());
        }
        wamService.commit(builder.build());
    }

    /**
     * Fabricates and commits the {@code TsBitArray} (id 4332) time-spent session
     * bit array.
     *
     * <p>This is the session-state-machine counterpart of the raw-input activity
     * bitmap: the same low word, optional high word and bit length describe which
     * seconds the time-spent session considered the surface foregrounded, and the
     * cumulative-bits and flush-sequence counters carry the session's running
     * totals. The event is keyed to the shared time-spent session id and unified
     * session id and stamped with the relative and absolute session timestamps.
     *
     * @param tsSessionId      the shared time-spent session id for this burst
     * @param unifiedSessionId the shared Web-style unified session id for this
     *                         burst
     * @param relativeMs       the number of milliseconds since the time-spent
     *                         session started
     * @param now              the current epoch-millisecond clock reading, reported
     *                         as the absolute session timestamp
     */
    @WhatsAppWebExport(moduleName = "WAWebTsBitArrayWamEvent", exports = "TsBitArrayWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebTimeSpentLoggingBitArray", exports = "postTsBitArrayEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitTsBitArray(long tsSessionId, String unifiedSessionId, long relativeMs, long now) {
        var length = (int) SyntheticTelemetryUtils.jitter(40, 23);
        var bitmap = activityBitmap(length);
        var sequence = SyntheticTelemetryUtils.jitter(0, 4);
        var builder = new TsBitArrayEventBuilder()
                .tsSessionId(tsSessionId)
                .bitarrayLength(length)
                .bitarrayLow(bitmap.low())
                .cumulativeBits(bitmap.cumulative() + sequence * SyntheticTelemetryUtils.jitter(18, 12))
                .relativeTimestampMs(relativeMs)
                .sessionSeq(sequence)
                .tsTimestampMs(now)
                .unifiedSessionId(unifiedSessionId);
        if (length > 32) {
            builder.bitarrayHigh(bitmap.high());
        }
        wamService.commit(builder.build());
    }

    /**
     * Fabricates and commits the {@code TsNavigation} (id 4334) surface-navigation
     * event describing a move from the chat list into a conversation.
     *
     * <p>The navigation source is the chat list and the destination is the chat
     * thread. When a chat is available its real jid decides the reported thread
     * type and, for a group, the destination is described as a group thread with
     * the live participant count; otherwise a plausible individual thread is
     * reported. The canonical-enterprise-presence flag is reported absent, the
     * common state for a session not under enterprise recovery, and the event is
     * keyed to the shared time-spent and unified session ids.
     *
     * @param chat             the busiest chat to describe as the destination, or
     *                         {@code null} when the store holds no chats
     * @param tsSessionId      the shared time-spent session id for this burst
     * @param unifiedSessionId the shared Web-style unified session id for this
     *                         burst
     * @param relativeMs       the number of milliseconds since the time-spent
     *                         session started
     * @param now              the current epoch-millisecond clock reading, reported
     *                         as the absolute session timestamp
     */
    @WhatsAppWebExport(moduleName = "WAWebTsNavigationWamEvent", exports = "TsNavigationWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebTimeSpentLoggingNavigation", exports = "logTsForegroundNavigation", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitTsNavigation(Chat chat, long tsSessionId, String unifiedSessionId, long relativeMs, long now) {
        var isGroup = chat != null && chat.jid().hasGroupOrCommunityServer();
        var builder = new TsNavigationEventBuilder()
                .tsSessionId(tsSessionId)
                .relativeTimestampMs(relativeMs)
                .navigationSource(TsSurface.CHAT_LIST)
                .navigationDestination(TsSurface.CHAT_THREAD)
                .navigationDestinationViewName("conversation")
                .threadType(isGroup ? ThreadType.GROUP : ThreadType.INDIVIDUAL)
                .isCanonicalEntPresent(false)
                .tsTimestampMs(now)
                .unifiedSessionId(unifiedSessionId);
        if (isGroup) {
            builder.typeOfGroup(TypeOfGroupEnum.GROUP)
                    .groupSize(groupSizeOf(chat));
        }
        wamService.commit(builder.build());
    }

    /**
     * Fabricates and commits the {@code TsExternal} (id 4574) external-UI dwell
     * event describing time spent recording a voice note.
     *
     * <p>WhatsApp Web brackets certain non-navigation UI sessions, voice-note
     * recording and playback, the in-call UI and background message send, with a
     * begin and end marker and reports the elapsed seconds when the dwell crosses a
     * one-second floor. This method reports one such completed voice-note recording
     * dwell keyed to the shared time-spent and unified session ids.
     *
     * @param tsSessionId      the shared time-spent session id for this burst
     * @param unifiedSessionId the shared Web-style unified session id for this
     *                         burst
     * @param relativeMs       the number of milliseconds since the time-spent
     *                         session started, reported as the dwell's relative
     *                         start offset
     * @param now              the current epoch-millisecond clock reading, reported
     *                         as the absolute session timestamp
     */
    @WhatsAppWebExport(moduleName = "WAWebTsExternalWamEvent", exports = "TsExternalWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebTimeSpentLoggingExternal", exports = "beginTsExternalEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitTsExternal(long tsSessionId, String unifiedSessionId, long relativeMs, long now) {
        wamService.commit(new TsExternalEventBuilder()
                .tsSessionId(tsSessionId)
                .tsDuration(SyntheticTelemetryUtils.jitter(2, 24))
                .tsExternalEventSource(TsExternalEventSource.PTT_RECORD)
                .relativeTimestampMs(relativeMs)
                .tsTimestampMs(now)
                .unifiedSessionId(unifiedSessionId)
                .build());
    }

    /**
     * Commits the {@code TestAnonymousDailyId} (id 2958) private-stats self-test
     * canary with no fields.
     *
     * <p>WhatsApp Web commits this event field-less once per day from its
     * anonymous-id private-stats validator; it carries no payload and exists only
     * to exercise the daily de-identification pipeline that hashes the reporter's
     * anonymous daily id. The empty commit here is therefore the faithful signal,
     * not a placeholder, and the test enum and float properties are deliberately
     * left unset exactly as WA leaves them.
     */
    @WhatsAppWebExport(moduleName = "WAWebTestAnonymousDailyIdWamEvent", exports = "TestAnonymousDailyIdWamEvent", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebWamPrivateStatsUtils", exports = "logDailyPrivateStats", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitTestAnonymousDailyId() {
        wamService.commit(new TestAnonymousDailyIdEventBuilder().build());
    }

    /**
     * Commits the {@code TestAnonymousMonthlyId} (id 2960) private-stats self-test
     * canary with no fields.
     *
     * <p>WhatsApp Web commits this event field-less once per day from its
     * anonymous-id private-stats validator to exercise the monthly de-identification
     * pipeline that hashes the reporter's anonymous monthly id. The event defines no
     * properties, so the empty commit is its full and faithful shape.
     */
    @WhatsAppWebExport(moduleName = "WAWebTestAnonymousMonthlyIdWamEvent", exports = "TestAnonymousMonthlyIdWamEvent", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebWamPrivateStatsUtils", exports = "logDailyPrivateStats", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitTestAnonymousMonthlyId() {
        wamService.commit(new TestAnonymousMonthlyIdEventBuilder().build());
    }

    /**
     * Returns the number of participants to report for a group chat.
     *
     * <p>When the chat's participant list has been loaded its size is the real
     * group size; when it is empty (a companion device may not yet have pulled the
     * roster) a plausible small-to-medium group size is fabricated so the reported
     * value never collapses to zero.
     *
     * @param chat the group chat whose size is reported
     * @return the real participant count, or a fabricated plausible size when the
     *         roster is not loaded
     */
    private static long groupSizeOf(Chat chat) {
        var size = chat.participant().size();
        return size > 0 ? size : SyntheticTelemetryUtils.jitter(4, 60);
    }

    /**
     * Builds the Web-style unified session id for the given clock reading.
     *
     * <p>WhatsApp Web mints the id as {@code (now + 3 days) % 7 days} and
     * stringifies it, so the value is a bounded rolling number rather than a raw
     * timestamp; this reproduces that reduction so the emitted id has the same
     * shape and range as a genuine one.
     *
     * @param now the current epoch-millisecond clock reading
     * @return the decimal-string unified session id
     */
    private static String unifiedSessionId(long now) {
        return Long.toString((now + UNIFIED_SESSION_OFFSET_MS) % UNIFIED_SESSION_WINDOW_MS);
    }

    /**
     * Fabricates a per-second activity bitmap of the given bit length.
     *
     * <p>Each of the {@code length} elapsed seconds is marked active with a
     * realistic majority probability, matching a session in which the user touched
     * the surface in most but not all of its seconds. The set bits are packed into
     * a low word (seconds zero through thirty-one) and, when the length exceeds
     * thirty-two, a high word (the remaining seconds), and the total set-bit count
     * is returned as the cumulative-active seconds. The words are kept within
     * thirty-two bits to match the signed {@code Int32Array} representation WA Web
     * flushes.
     *
     * @param length the number of second-buckets to sample, capped by the caller to
     *               keep the high word within thirty-two bits
     * @return the fabricated bitmap words and cumulative-active count
     */
    private static Bitmap activityBitmap(int length) {
        var random = ThreadLocalRandom.current();
        var low = 0L;
        var high = 0L;
        var cumulative = 0L;
        for (var i = 0; i < length; i++) {
            if (random.nextInt(100) < 62) {
                if (i < 32) {
                    low |= 1L << i;
                } else {
                    high |= 1L << (i - 32);
                }
                cumulative++;
            }
        }
        return new Bitmap(low, high, cumulative);
    }


    /**
     * The packed result of {@link #activityBitmap(int)}: the low and high bitmap
     * words and the cumulative count of active seconds.
     *
     * @param low        the low bitmap word covering seconds zero through
     *                   thirty-one
     * @param high       the high bitmap word covering seconds thirty-two and beyond,
     *                   or zero when the sampled length did not reach the high word
     * @param cumulative the total number of active seconds across both words
     */
    private record Bitmap(long low, long high, long cumulative) {
    }
}
