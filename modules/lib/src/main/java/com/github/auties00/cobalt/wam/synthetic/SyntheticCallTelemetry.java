package com.github.auties00.cobalt.wam.synthetic;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.CallInfoUserJourneyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PreCallUserJourneyCallsTabEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.RingtoneScreenEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.CallSizeBucket;
import com.github.auties00.cobalt.wire.wam.type.CallSizeType;
import com.github.auties00.cobalt.wire.wam.type.CallType;
import com.github.auties00.cobalt.wire.wam.type.CallsTabSource;
import com.github.auties00.cobalt.wire.wam.type.ParticipantActionSource;
import com.github.auties00.cobalt.wire.wam.type.PreCallActionType;
import com.github.auties00.cobalt.wire.wam.type.RingtoneEntryType;
import com.github.auties00.cobalt.wire.wam.type.SubSurface;

import java.util.Objects;

/**
 * Emits the block of WhatsApp Web call-experience WAM beacons that a real Web or
 * Desktop session produces from its Calls tab, call-info surface, and ringtone
 * settings screen but that a headless Cobalt client has no rendered surface to
 * generate.
 *
 * <p>None of the three events wired here map to a Cobalt feature. Cobalt embeds a
 * headless VoIP engine (the {@code calls} package) that can place and answer
 * calls, but it renders no Calls tab, no pre-call funnel, no call-info
 * participant list, and no ringtone picker, so it never raises the
 * user-journey and settings-screen telemetry a genuine client emits while the
 * user browses call history, opens a call-info sheet, or changes their ringtone.
 * A Cobalt session that never emitted any of these would therefore carry a
 * telemetry fingerprint trivially distinguishable from a real WhatsApp Web
 * session. This service synthesises one plausible, self-consistent occurrence of
 * each and commits them through
 * {@link WamService#commit(com.github.auties00.cobalt.wam.model.WamEventSpec)}.
 *
 * <p>The session-identity and timing fields are shaped exactly as WhatsApp Web
 * shapes them: {@code WAWebPreCallUserJourneyLogger} and
 * {@code WAWebCallInfoUserJourneyLogger} both stamp a process-wide
 * {@code appSessionId} minted once as {@code WARandomHex.randomHex(16)} (sixteen
 * random bytes rendered as a thirty-two character lowercase hex string) and a
 * per-funnel {@code userJourneyFunnelId} (and, for the call-info logger, a
 * per-session {@code surfaceSessionId}) minted the same way, with
 * {@code userJourneyEventMs} taken from the wall clock. This service reuses one
 * {@code appSessionId} across both journey events, mints a fresh funnel and
 * surface id per event, and derives the participant count of the synthesised
 * group call from the live store's largest group chat where one exists.
 *
 * <p>The single public entrypoint is {@link #emitSessionTelemetry()}. Each event
 * is committed exactly once per invocation; the intended cadence is once per
 * successful connection (a rendered client produces these throughout a session
 * as the user works through call history and settings, but a single
 * representative sample per connect is enough to keep the stream shaped like a
 * genuine client without over-reporting).
 *
 * @see WamService
 * @see com.github.auties00.cobalt.wire.wam.event.PreCallUserJourneyCallsTabEvent
 * @see com.github.auties00.cobalt.wire.wam.event.CallInfoUserJourneyEvent
 * @see com.github.auties00.cobalt.wire.wam.event.RingtoneScreenEvent
 */
@WhatsAppWebModule(moduleName = "WAWebPreCallUserJourneyLogger")
@WhatsAppWebModule(moduleName = "WAWebPreCallUserJourneyCallsTabWamEvent")
@WhatsAppWebModule(moduleName = "WAWebCallInfoUserJourneyLogger")
@WhatsAppWebModule(moduleName = "WAWebCallInfoUserJourneyWamEvent")
@WhatsAppWebModule(moduleName = "WAWebRingtoneScreenWamEvent")
public final class SyntheticCallTelemetry {
    /**
     * The number of bytes hashed into every session identifier, matching the
     * {@code WARandomHex.randomHex(16)} call the WhatsApp Web call loggers use to
     * mint {@code appSessionId}, {@code surfaceSessionId}, and
     * {@code userJourneyFunnelId}.
     *
     * <p>Sixteen random bytes render to a thirty-two character lowercase hex
     * string, the exact wire shape of the real client's identifiers.
     */
    private static final int SESSION_ID_BYTES = 16;

    /**
     * The participant count at or below which a group call is bucketed
     * {@link CallSizeBucket#SMALL}.
     */
    private static final long SMALL_BUCKET_MAX = 8L;

    /**
     * The participant count at or below which a group call is bucketed
     * {@link CallSizeBucket#MEDIUM}.
     */
    private static final long MEDIUM_BUCKET_MAX = 32L;

    /**
     * The participant count at or below which a group call is bucketed
     * {@link CallSizeBucket#LARGE}; larger calls bucket
     * {@link CallSizeBucket#XLARGE}.
     */
    private static final long LARGE_BUCKET_MAX = 64L;

    /**
     * The upper clamp applied to a live-sampled group participant count so the
     * synthesised {@code numParticipantsShown} stays inside the range a real
     * call-info sheet would render at once.
     */
    private static final int MAX_PARTICIPANTS_SHOWN = 32;

    /**
     * The bound WhatsApp client whose store supplies the live group participant
     * count sampled when populating the call-info event.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The WAM service through which every synthesised call event is committed for
     * batched upload.
     */
    private final WamService wamService;

    /**
     * The process-wide application session identifier reused across the two
     * user-journey events.
     *
     * <p>It is minted once at construction as a thirty-two character lowercase hex
     * string through {@link SyntheticTelemetryUtils#randomHexLower(int)}, mirroring
     * the static {@code appSessionId} that WhatsApp Web's
     * {@code WAWebPreCallUserJourneyLogger} and
     * {@code WAWebCallInfoUserJourneyLogger} compute once at module load via
     * {@code WARandomHex.randomHex(16)} and share across every call-journey beacon
     * of the session.
     *
     * <p>Drawing the sixteen bytes from
     * {@link java.util.concurrent.ThreadLocalRandom} through that helper keeps this
     * value stable across both journey beacons of a single session yet freshly
     * minted on every new session, so it never freezes into a byte-identical
     * cross-session fingerprint the way a host-seeded draw would.
     */
    private final String appSessionId;

    /**
     * Constructs a new {@code SyntheticCallTelemetry} bound to the given client
     * and WAM service.
     *
     * @param client     the WhatsApp client whose store is sampled, must not be
     *                   {@code null}
     * @param wamService the WAM service used to commit the emitted events, must
     *                   not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SyntheticCallTelemetry(LinkedWhatsAppClient client, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
        this.appSessionId = SyntheticTelemetryUtils.randomHexLower(SESSION_ID_BYTES);
    }

    /**
     * Emits one representative occurrence of every synthetic call beacon carried
     * by this service.
     *
     * <p>This is the single entrypoint the client drives once per successful
     * connection. It commits the Calls-tab pre-call funnel action, the call-info
     * surface user-journey milestone, and the ringtone-settings screen change in
     * turn; each helper fabricates one self-consistent occurrence and commits it
     * through {@link WamService}. There is no natural recurring cadence because a
     * headless client raises no call-UI events, so a single per-connect sample
     * stands in for the stream a rendered client would produce as the user
     * browses call history and settings.
     */
    public void emitSessionTelemetry() {
        emitPreCallUserJourneyCallsTab();
        emitCallInfoUserJourney();
        emitRingtoneScreen();
    }

    /**
     * Synthesises a Calls-tab pre-call funnel action (event id 5680).
     *
     * <p>The fabricated event models the user tapping the audio-call affordance on
     * an outgoing-call log row in the Calls tab after switching to it, mirroring
     * WA Web's {@code WAWebPreCallUserJourneyLogger} which stamps the shared
     * {@code appSessionId} and a per-funnel {@code userJourneyFunnelId} and
     * commits the tapped {@link PreCallActionType} against its {@link SubSurface}.
     * The descriptive fields a full calls-tab row-tap beacon also carries (the
     * tab source, the resulting call's size type, the tapped row's item position,
     * a per-surface session id, and the event wall-clock time) are populated to
     * complete the shape. The {@code genaiBots} field is left unset because the
     * synthesised call has no AI-bot participants, matching the real client which
     * omits it outside AI-bot calls.
     */
    @WhatsAppWebExport(moduleName = "WAWebPreCallUserJourneyLogger", exports = "PreCallUserJourneyLogger", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitPreCallUserJourneyCallsTab() {
        wamService.commit(new PreCallUserJourneyCallsTabEventBuilder()
                .appSessionId(appSessionId)
                .callsTabSource(CallsTabSource.SWITCH)
                .callSizeType(CallSizeType.ONE_TO_ONE)
                .preCallActionType(PreCallActionType.CLICK_AUDIO_CALL)
                .subSurface(SubSurface.CALL_LOG_OUTGOING_ROW)
                .itemPosition(SyntheticTelemetryUtils.count(0, 8))
                .surfaceSessionId(SyntheticTelemetryUtils.randomHexLower(SESSION_ID_BYTES))
                .userJourneyEventMs(System.currentTimeMillis())
                .userJourneyFunnelId(SyntheticTelemetryUtils.randomHexLower(SESSION_ID_BYTES))
                .build());
    }

    /**
     * Synthesises a call-info surface user-journey milestone (event id 6034).
     *
     * <p>The fabricated event models the user opening the call-info sheet of a
     * small ad-hoc group voice call and viewing its participants from the call
     * header, mirroring WA Web's {@code WAWebCallInfoUserJourneyLogger.logEvent}
     * which stamps the shared {@code appSessionId}, the per-session
     * {@code surfaceSessionId} and {@code userJourneyFunnelId}, the wall-clock
     * {@code userJourneyEventMs}, and the {@link PreCallActionType} milestone, then
     * conditionally attaches the call's size type, group-size bucket, call type,
     * participant action source, and the number of participants shown. The
     * participant count is sampled from the live store's largest group chat when
     * one exists and the group-size bucket is derived from it so the two fields
     * stay self-consistent; the {@code genaiBots} field is omitted because the
     * synthesised call has no AI-bot participants.
     */
    @WhatsAppWebExport(moduleName = "WAWebCallInfoUserJourneyLogger", exports = "CallInfoUserJourneyLogger", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitCallInfoUserJourney() {
        var participantsShown = participantsShown();
        wamService.commit(new CallInfoUserJourneyEventBuilder()
                .appSessionId(appSessionId)
                .surfaceSessionId(SyntheticTelemetryUtils.randomHexLower(SESSION_ID_BYTES))
                .userJourneyFunnelId(SyntheticTelemetryUtils.randomHexLower(SESSION_ID_BYTES))
                .preCallActionType(PreCallActionType.CALL_INFO_OPEN)
                .userJourneyEventMs(System.currentTimeMillis())
                .callSizeType(CallSizeType.ADHOC)
                .callGroupSizeBucket(bucketFor(participantsShown))
                .callType(CallType.VOICE)
                .participantActionSource(ParticipantActionSource.HEADER_AUDIO)
                .numParticipantsShown(participantsShown)
                .build());
    }

    /**
     * Synthesises a ringtone-settings screen change (event id 7608).
     *
     * <p>The fabricated event models the user opening the ringtone settings screen
     * and applying a new app-wide ringtone selection without cancelling or
     * resetting it, populating the ringtone id, the app-wide
     * {@link RingtoneEntryType} source, and a plausible count of premium ringtones
     * already downloaded. WhatsApp Web ships no call site for this event (it is a
     * mobile-surface screen), so there is no logger to mirror; the fields are
     * populated from the event definition to keep the beacon well-shaped.
     */
    @WhatsAppWebExport(moduleName = "WAWebRingtoneScreenWamEvent", exports = "RingtoneScreenWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitRingtoneScreen() {
        wamService.commit(new RingtoneScreenEventBuilder()
                .premiumRingtonesDownloadedCount(SyntheticTelemetryUtils.count(0, 4))
                .ringtoneChangeApplied(Boolean.TRUE)
                .ringtoneId(randomRingtoneId())
                .ringtoneReset(Boolean.FALSE)
                .ringtoneSelectionCancelled(Boolean.FALSE)
                .ringtoneSource(RingtoneEntryType.APP_WIDE)
                .ringtoneSubscribeSelected(Boolean.FALSE)
                .build());
    }

    /**
     * Returns the number of participants to report as shown on the synthesised
     * call-info sheet.
     *
     * <p>When the live store holds at least one group or community chat with a
     * non-empty participant list, the value is the largest such chat's participant
     * count clamped to {@link #MAX_PARTICIPANTS_SHOWN} so it stays inside the range
     * a real call-info sheet renders at once; otherwise a plausible small count is
     * fabricated.
     *
     * @return the participant count to report, always at least one
     */
    private long participantsShown() {
        var largest = client.store()
                .chatStore()
                .chats()
                .stream()
                .filter(chat -> chat.jid().hasGroupOrCommunityServer())
                .mapToInt(chat -> chat.participant().size())
                .filter(size -> size > 0)
                .max();
        if (largest.isPresent()) {
            return Math.min(largest.getAsInt(), MAX_PARTICIPANTS_SHOWN);
        }
        return SyntheticTelemetryUtils.count(2, 8);
    }

    /**
     * Returns the group-size bucket that a call with the given participant count
     * falls into.
     *
     * <p>The thresholds mirror the coarse buckets a real client reports alongside
     * the exact participant count, keeping the two fields self-consistent on the
     * synthesised call-info beacon.
     *
     * @param participants the participant count of the synthesised call
     * @return the matching {@link CallSizeBucket}
     */
    private CallSizeBucket bucketFor(long participants) {
        if (participants <= SMALL_BUCKET_MAX) {
            return CallSizeBucket.SMALL;
        }
        if (participants <= MEDIUM_BUCKET_MAX) {
            return CallSizeBucket.MEDIUM;
        }
        if (participants <= LARGE_BUCKET_MAX) {
            return CallSizeBucket.LARGE;
        }
        return CallSizeBucket.XLARGE;
    }

    /**
     * Fabricates a plausible opaque ringtone asset identifier.
     *
     * <p>The value is a sixteen-character lowercase hex string standing in for the
     * content id a real client stores when the user selects a downloaded ringtone.
     * WhatsApp re-mints this id every time the setting is applied, so it is drawn
     * fresh on every call from {@link SyntheticTelemetryUtils#randomHexLower(int)}
     * rather than derived from a stable seed; a value repeated verbatim across
     * sessions would itself be a distinguishing fingerprint.
     *
     * @return a freshly minted ringtone id
     */
    private String randomRingtoneId() {
        return SyntheticTelemetryUtils.randomHexLower(8);
    }
}
