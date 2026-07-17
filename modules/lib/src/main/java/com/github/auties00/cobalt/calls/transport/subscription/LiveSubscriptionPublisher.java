package com.github.auties00.cobalt.calls.transport.subscription;

import com.github.auties00.cobalt.calls.engine.participant.ParticipantProvider;
import com.github.auties00.cobalt.calls.engine.participant.ParticipantView;
import com.github.auties00.cobalt.calls.util.TimerEntry;
import com.github.auties00.cobalt.calls.util.TimerHeap;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.call.datachannel.RxSubscriptions;
import com.github.auties00.cobalt.wire.linked.call.datachannel.RxSubscriptionsBuilder;
import com.github.auties00.cobalt.wire.linked.call.datachannel.RxSubscriptionsSpec;
import com.github.auties00.cobalt.wire.linked.call.datachannel.RxVidSubscriptionInfo;
import com.github.auties00.cobalt.wire.linked.call.datachannel.RxVidSubscriptionInfoBuilder;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SenderSubscriptions;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SenderSubscriptionsSpec;
import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamDescriptor;
import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamDescriptorBuilder;
import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamDescriptors;
import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamDescriptorsBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.LongSupplier;
import java.util.function.ToIntFunction;
import com.github.auties00.cobalt.calls.transport.LiveRelayTransport;

/**
 * Publishes a client's send layout and receive wishes to the selective forwarding unit.
 *
 * <p>A group call reaches the selective forwarding unit through the relay, and the unit
 * forwards only the streams and qualities each client asks for. A client expresses what
 * it sends and what it wants to receive by embedding serialized protobufs inside STUN
 * binding request attributes: a {@link SenderSubscriptions} in the sender attribute and
 * an {@link RxSubscriptions} in the receiver attribute. This class turns the call's
 * {@link StreamLayout} into the {@link StreamDescriptors} list, frames the sender and
 * receiver subscriptions as {@link SubscriptionStunAttribute} values for the STUN
 * message writer, and drives the periodic resend of the cached receive subscription so a
 * dropped binding does not strand the receiver. It also owns the hop by hop
 * {@link RtcpRxSubscriptionTable} that records which RTCP feedback the unit should
 * forward, exposed through {@link #rtcpRxTable()}.
 *
 * <p>The cached subscription and its change suppression live in a
 * {@link RxSubscriptionState}, and the hop by hop RTCP feedback subscriptions live in a
 * {@link RtcpRxSubscriptionTable}; both are owned by this publisher for the lifetime of
 * the call.
 *
 * <p>The resend timer is scheduled on the supplied {@link TimerHeap} against the supplied
 * nanosecond clock; each fire resends the most recently published subscription through
 * the resend callback and arms the next timer, so the cadence continues until
 * {@link #close()} cancels it. This class is not thread safe: the single call transport
 * thread that polls the timer heap also calls every method here.
 *
 * @implNote This implementation builds a media, FEC, and NACK descriptor triple per
 * active media layer and single descriptors for the application data, live transcription,
 * and hop by hop FEC SSRCs, omitting any triple whose SSRC is
 * {@link StreamLayout#ABSENT_SSRC}. The resend cadence is driven through the
 * {@link TimerHeap} the transport thread already polls, and each fire schedules the
 * following resend so the periodic callback continues on its own.
 */
public final class LiveSubscriptionPublisher {
    /**
     * The logger for {@link LiveSubscriptionPublisher}.
     */
    private static final System.Logger LOGGER = Log.get(LiveSubscriptionPublisher.class);

    /**
     * The default interval between resends of the cached receive subscription.
     *
     * <p>Applied as the period of the receive subscription resend timer and, with the
     * same value, as the minimum duration gate before a resubscription is issued.
     *
     * @implNote This implementation uses {@code 100ms}, the compiled default of the
     * voip parameter {@code rx_subscription_min_duration_ms}, which the transport reads
     * both as the resend timer period and as the minimum duration comparison. The server
     * may override it through the voip parameter key {@code rx_sub_min_dur_ms}, which is
     * absent from the observed settings, so the compiled default governs.
     */
    public static final Duration DEFAULT_RESEND_INTERVAL = Duration.ofMillis(100);

    /**
     * The exclusive upper bound on a valid receive subscription video quality index.
     *
     * <p>A quality entry whose {@link RxVidSubscriptionInfo.VideoQuality#index() index} is
     * at or above this is dropped from the computed subscription.
     *
     * @implNote This implementation uses {@code 5}, the exclusive bound the receive
     * subscription build enforces by rejecting a requested quality greater than
     * {@code 4}, which is exactly the five {@link RxVidSubscriptionInfo.VideoQuality}
     * levels {@code DEFAULT}(0) through {@code HD}(4).
     */
    private static final int VIDEO_QUALITY_BOUND = 5;

    /**
     * The timer heap the resend timer is scheduled on.
     *
     * <p>Driven by the call transport thread; this publisher schedules and cancels a
     * single resend entry on it. Never {@code null}.
     */
    private final TimerHeap timerHeap;

    /**
     * Source of the current time in the timer heap's nanosecond timebase.
     *
     * <p>Read when arming the resend timer so the deadline shares the clock the transport
     * thread polls the heap with. Never {@code null}.
     */
    private final LongSupplier clock;

    /**
     * Callback invoked to resend the cached receive subscription on each timer fire.
     *
     * <p>Receives the framed receiver {@link SubscriptionStunAttribute} carrying the most
     * recently published subscription; the transport attaches it to a STUN binding request
     * toward the relay. Never {@code null}.
     */
    private final Consumer<SubscriptionStunAttribute> rxResender;

    /**
     * The interval between successive resends of the cached subscription.
     *
     * <p>Applied when arming and rearming the resend timer. Never {@code null}.
     */
    private final Duration resendInterval;

    /**
     * The cached receive subscription and its change suppression state.
     *
     * <p>Holds the last published subscription so a redundant publish is suppressed and a
     * resend carries the current subscription. Never {@code null}.
     */
    private final RxSubscriptionState rxState;

    /**
     * The hop by hop RTCP feedback subscription table for this call.
     *
     * <p>Records which RTCP feedback the selective forwarding unit should forward per
     * media SSRC. Never {@code null}.
     */
    private final RtcpRxSubscriptionTable rtcpRxTable;

    /**
     * The participant read seam this publisher reads the call roster through, or
     * {@code null} on a one to one call that tracks no roster.
     *
     * <p>The receive subscription computation reads the participant provider for two facts
     * it needs: the first connected peer the one to one layout keys its single peer off,
     * and the per stream subscriber count the selective forwarding unit quality picker uses
     * to avoid subscribing to a video stream no participant wants. It is the roster the
     * receive subscription compute walks in
     * {@link #computeRxSubscription(ToIntFunction, IntFunction)}. It is {@code null} on a
     * one to one call, which has a single fixed peer and allocates no
     * {@link com.github.auties00.cobalt.calls.engine.participant.CallMembership}, and on a
     * publisher built without a roster.
     *
     * @implNote This implementation is threaded in at construction from
     * {@link com.github.auties00.cobalt.calls.engine.participant.CallMembership#participantProvider()}
     * rather than reached through an embedded call context, because Cobalt splits the
     * membership out of the call context.
     */
    private final ParticipantProvider participantProvider;

    /**
     * The armed resend timer entry, or {@code null} when no resend is scheduled.
     *
     * <p>Set when a changed subscription is published and rearmed on each fire; cancelled
     * by {@link #close()}. Holds {@code null} before the first publish and after close.
     */
    private TimerEntry resendTimer;

    /**
     * The framed receiver attribute for the last published subscription, or {@code null}
     * when nothing is cached.
     *
     * <p>Built once in {@link #publishRxSubscription(RxSubscriptions, long)} alongside the
     * {@link RxSubscriptionState} record and forwarded verbatim by {@link #onResendTimer()}
     * so the 10 Hz resend does not encode the protobuf again and clone it into a fresh
     * {@link SubscriptionStunAttribute} every tick. Kept in lockstep with the
     * {@link #rxState} cache: set when that cache is recorded, nulled when it is cleared in
     * {@link #close()}. {@link SubscriptionStunAttribute} is an immutable record (its
     * constructor and {@link SubscriptionStunAttribute#value() value()} both clone), so the
     * one instance is shared across the initial publish and every resend without aliasing.
     */
    private SubscriptionStunAttribute cachedReceiverAttribute;

    /**
     * Whether this publisher has been closed.
     *
     * <p>Once {@code true} the resend timer stays cancelled and {@link #close()} does
     * nothing; publishing after close still frames attributes but arms no timer.
     */
    private boolean closed;

    /**
     * Constructs a publisher with the default resend interval and no participant provider.
     *
     * <p>Equivalent to {@link #LiveSubscriptionPublisher(TimerHeap, LongSupplier, Consumer, Duration, ParticipantProvider)}
     * with {@link #DEFAULT_RESEND_INTERVAL} and a {@code null} provider. Suitable for a
     * publisher with no participant context (a one to one call, or a test exercising only
     * the framing methods).
     *
     * @param timerHeap  the heap the resend timer is scheduled on; must not be {@code null}
     * @param clock      the nanosecond clock the heap is polled with; must not be {@code null}
     * @param rxResender the callback that sends the resent receiver attribute; must not be {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public LiveSubscriptionPublisher(TimerHeap timerHeap,
                                     LongSupplier clock,
                                     Consumer<SubscriptionStunAttribute> rxResender) {
        this(timerHeap, clock, rxResender, DEFAULT_RESEND_INTERVAL, null);
    }

    /**
     * Constructs a publisher with an explicit resend interval and no participant provider.
     *
     * <p>Equivalent to {@link #LiveSubscriptionPublisher(TimerHeap, LongSupplier, Consumer, Duration, ParticipantProvider)}
     * with a {@code null} provider.
     *
     * @param timerHeap      the heap the resend timer is scheduled on; must not be {@code null}
     * @param clock          the nanosecond clock the heap is polled with; must not be {@code null}
     * @param rxResender     the callback that sends the resent receiver attribute; must not be {@code null}
     * @param resendInterval the interval between cached subscription resends; must not be {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public LiveSubscriptionPublisher(TimerHeap timerHeap,
                                     LongSupplier clock,
                                     Consumer<SubscriptionStunAttribute> rxResender,
                                     Duration resendInterval) {
        this(timerHeap, clock, rxResender, resendInterval, null);
    }

    /**
     * Constructs a publisher with an explicit resend interval and a participant read seam.
     *
     * <p>The publisher starts with no cached subscription, an empty feedback table, and no
     * armed timer; the first changed {@link #publishRxSubscription(RxSubscriptions, long)}
     * arms the resend. The {@code participantProvider} is the roster read seam the
     * receive subscription compute reads; pass {@code null} on a one to one call that tracks
     * no roster.
     *
     * @param timerHeap           the heap the resend timer is scheduled on; must not be {@code null}
     * @param clock               the nanosecond clock the heap is polled with; must not be {@code null}
     * @param rxResender          the callback that sends the resent receiver attribute; must not be
     *                            {@code null}
     * @param resendInterval      the interval between cached subscription resends; must not be {@code null}
     * @param participantProvider the call roster read seam, or {@code null} when no roster is tracked
     * @throws NullPointerException if {@code timerHeap}, {@code clock}, {@code rxResender}, or
     *                              {@code resendInterval} is {@code null}
     */
    public LiveSubscriptionPublisher(TimerHeap timerHeap,
                                     LongSupplier clock,
                                     Consumer<SubscriptionStunAttribute> rxResender,
                                     Duration resendInterval,
                                     ParticipantProvider participantProvider) {
        this.timerHeap = Objects.requireNonNull(timerHeap, "timerHeap cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.rxResender = Objects.requireNonNull(rxResender, "rxResender cannot be null");
        this.resendInterval = Objects.requireNonNull(resendInterval, "resendInterval cannot be null");
        this.participantProvider = participantProvider;
        this.rxState = new RxSubscriptionState();
        this.rtcpRxTable = new RtcpRxSubscriptionTable();
        this.resendTimer = null;
        this.closed = false;
    }

    /**
     * Returns the participant read seam this publisher reads the call roster through, if one was supplied.
     *
     * <p>Present on a group call, where it is the
     * {@link com.github.auties00.cobalt.calls.engine.participant.CallMembership#participantProvider() membership
     * provider}; empty on a one to one call that tracks no roster. It is the seam the
     * receive subscription compute reads for the first connected peer and the per stream subscriber count.
     *
     * @return an {@link Optional} holding the participant provider, or empty when none was supplied
     */
    public Optional<ParticipantProvider> participantProvider() {
        return Optional.ofNullable(participantProvider);
    }

    /**
     * Builds the stream descriptor list declaring every stream the layout publishes.
     *
     * <p>Emits one {@link StreamDescriptor} per active stream: the audio media plus its
     * forward error correction and negative acknowledgement descriptors, the same triple
     * for each present video simulcast layer, and the application data, live transcription,
     * and hop by hop forward error correction descriptors for whichever feature SSRCs the
     * layout allocates. Absent SSRCs yield no descriptors. The result is the
     * {@link StreamDescriptors} the selective forwarding unit reads to set up forwarding.
     *
     * @param layout the SSRC and feature layout this client publishes; must not be {@code null}
     * @return the stream descriptors for the layout
     * @throws NullPointerException if {@code layout} is {@code null}
     */
    public StreamDescriptors buildStreamDescriptors(StreamLayout layout) {
        Objects.requireNonNull(layout, "layout cannot be null");
        var descriptors = new ArrayList<StreamDescriptor>(StreamLayout.MAX_STREAM_DESCRIPTORS);
        appendMediaTriple(descriptors, StreamDescriptor.StreamLayer.AUDIO, layout.audioSsrc(), false);
        appendMediaTriple(descriptors, StreamDescriptor.StreamLayer.VIDEO_STREAM0,
                layout.videoStream0Ssrc(), layout.uplinkPrefetch());
        appendMediaTriple(descriptors, StreamDescriptor.StreamLayer.VIDEO_STREAM1,
                layout.videoStream1Ssrc(), layout.uplinkPrefetch());
        appendSingle(descriptors, StreamDescriptor.StreamLayer.APP_DATA_STREAM0,
                StreamDescriptor.PayloadType.APP_DATA, layout.appDataSsrc(), layout.uplinkPrefetch());
        appendSingle(descriptors, StreamDescriptor.StreamLayer.LIVE_TRANSCRIPTION_STREAM0,
                StreamDescriptor.PayloadType.MEDIA, layout.liveTranscriptionSsrc(), false);
        appendSingle(descriptors, StreamDescriptor.StreamLayer.HBH_FEC_CLIENT_TO_SERVER,
                StreamDescriptor.PayloadType.HBH_FEC, layout.hbhFecTxSsrc(), false);
        appendSingle(descriptors, StreamDescriptor.StreamLayer.HBH_FEC_SERVER_TO_CLIENT,
                StreamDescriptor.PayloadType.HBH_FEC, layout.hbhFecRxSsrc(), false);
        return new StreamDescriptorsBuilder()
                .streamDescriptors(descriptors)
                .build();
    }

    /**
     * Appends the media, FEC, and NACK descriptors for one media layer when its SSRC is
     * present.
     *
     * <p>A media layer is published as three descriptors sharing the layer's SSRC: the
     * media payload, the paired forward error correction stream, and the negative
     * acknowledgement stream. When the SSRC is {@link StreamLayout#ABSENT_SSRC} no
     * descriptor is appended, so an inactive layer contributes nothing. The uplink prefetch
     * flag is carried on the media descriptor only.
     *
     * @param into     the accumulator to append to
     * @param layer    the logical layer the triple describes
     * @param ssrc     the layer SSRC, or {@link StreamLayout#ABSENT_SSRC} to skip
     * @param prefetch whether uplink prefetch is engaged on the media descriptor
     */
    private void appendMediaTriple(List<StreamDescriptor> into,
                                   StreamDescriptor.StreamLayer layer,
                                   int ssrc,
                                   boolean prefetch) {
        if (ssrc == StreamLayout.ABSENT_SSRC) {
            return;
        }
        appendSingle(into, layer, StreamDescriptor.PayloadType.MEDIA, ssrc, prefetch);
        appendSingle(into, layer, StreamDescriptor.PayloadType.FEC, ssrc, false);
        appendSingle(into, layer, StreamDescriptor.PayloadType.NACK, ssrc, false);
    }

    /**
     * Appends a single descriptor for the given layer and payload type when the SSRC is
     * present.
     *
     * <p>Builds one {@link StreamDescriptor} binding the layer and payload type to the
     * SSRC, carrying the uplink prefetch flag, and appends it. When the SSRC is
     * {@link StreamLayout#ABSENT_SSRC} nothing is appended.
     *
     * @param into        the accumulator to append to
     * @param layer       the logical layer the descriptor describes
     * @param payloadType the payload type the descriptor describes
     * @param ssrc        the stream SSRC, or {@link StreamLayout#ABSENT_SSRC} to skip
     * @param prefetch    whether uplink prefetch is engaged
     */
    private void appendSingle(List<StreamDescriptor> into,
                              StreamDescriptor.StreamLayer layer,
                              StreamDescriptor.PayloadType payloadType,
                              int ssrc,
                              boolean prefetch) {
        if (ssrc == StreamLayout.ABSENT_SSRC) {
            return;
        }
        into.add(new StreamDescriptorBuilder()
                .streamLayer(layer)
                .payloadType(payloadType)
                .ssrc(ssrc)
                .isUplinkPrefetchEnabled(prefetch ? Boolean.TRUE : null)
                .build());
    }

    /**
     * Frames a sender subscription as the proprietary STUN sender subscription attribute.
     *
     * <p>Serializes the {@link SenderSubscriptions} protobuf and wraps the bytes in a
     * {@link SubscriptionStunAttribute} of type
     * {@link SubscriptionStunAttribute#SENDER_SUBSCRIPTIONS_TYPE} so the STUN message
     * writer can append it to a binding request.
     *
     * @param senderSubscriptions the sender subscription to frame; must not be {@code null}
     * @return the STUN attribute carrying the serialized sender subscription
     * @throws NullPointerException if {@code senderSubscriptions} is {@code null}
     */
    public SubscriptionStunAttribute buildSenderAttribute(SenderSubscriptions senderSubscriptions) {
        Objects.requireNonNull(senderSubscriptions, "senderSubscriptions cannot be null");
        var bytes = SenderSubscriptionsSpec.encode(senderSubscriptions);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "sender subscription framed, {0} bytes", bytes.length);
        }
        return new SubscriptionStunAttribute(SubscriptionStunAttribute.SENDER_SUBSCRIPTIONS_TYPE, bytes);
    }

    /**
     * Frames a receive subscription as the proprietary STUN receiver subscription attribute.
     *
     * <p>Serializes the {@link RxSubscriptions} protobuf and wraps the bytes in a
     * {@link SubscriptionStunAttribute} of type
     * {@link SubscriptionStunAttribute#RECEIVER_SUBSCRIPTION_TYPE}. Unlike
     * {@link #publishRxSubscription(RxSubscriptions, long)} this performs no suppression and
     * does not touch the cached state; it is the framing primitive the publish path builds on.
     *
     * @param rxSubscriptions the receive subscription to frame; must not be {@code null}
     * @return the STUN attribute carrying the serialized receive subscription
     * @throws NullPointerException if {@code rxSubscriptions} is {@code null}
     */
    public SubscriptionStunAttribute buildReceiverAttribute(RxSubscriptions rxSubscriptions) {
        Objects.requireNonNull(rxSubscriptions, "rxSubscriptions cannot be null");
        var bytes = RxSubscriptionsSpec.encode(rxSubscriptions);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "receiver subscription framed, {0} bytes", bytes.length);
        }
        return new SubscriptionStunAttribute(SubscriptionStunAttribute.RECEIVER_SUBSCRIPTION_TYPE, bytes);
    }

    /**
     * Computes this client's receive subscription by walking the connected peers in the call roster.
     *
     * <p>Every connected peer other than self and extension peers that the subscriber wants video from
     * contributes its relay PID to the {@link RxSubscriptions#vidRxPids() vid_rx_pids} list, and, when a
     * quality is chosen for that PID, an {@link RxVidSubscriptionInfo} entry pairing the PID with the
     * requested {@link RxVidSubscriptionInfo.VideoQuality quality} to
     * {@link RxSubscriptions#vidSubscriptions()}. The two roster facts the subscription build reads but the
     * participant snapshot does not carry are supplied as seams: {@code pidResolver} maps a peer's user JID
     * to its server assigned relay PID (the relay {@code <participant pid jid>} mapping), and
     * {@code qualityChooser} returns the requested receive quality for a PID (the per tile quality the
     * platform decides), or empty to omit a quality entry for a PID the subscriber wants any video from. A
     * peer whose JID resolves to no PID, and a quality whose ordinal is outside the valid range, are skipped.
     * The result is suitable for {@link #publishRxSubscription(RxSubscriptions, long)}.
     *
     * <p>When this publisher tracks no roster ({@link #participantProvider()} is empty) or the roster is not
     * valid, the computed subscription is empty, matching a call with no connected peers to subscribe to.
     *
     * @implNote This implementation walks the connected peers, validates each requested quality against the
     * {@link #VIDEO_QUALITY_BOUND} bound, and emits a {@code vid_rx_pids} entry per wanted peer plus a
     * {@code vid_subscriptions} entry per chosen quality. It takes the quality through the
     * {@code qualityChooser} seam rather than computing it, because the per tile quality is a UI decision
     * (which video tiles are visible and their on screen size) not recoverable here. The relay PID lookup is
     * a seam rather than a {@link ParticipantView} field because that view carries the peer JIDs but not the
     * relay assigned PID, which lives in the parsed relay {@code <participant>} list outside this publisher.
     * @param pidResolver    maps a connected peer's user JID to its relay PID, returning a negative value
     *                       when the JID has no PID; never {@code null}
     * @param qualityChooser returns the requested receive quality for a relay PID, or empty to request any
     *                       video without a quality entry; never {@code null}
     * @return the computed receive subscription over the connected peers
     * @throws NullPointerException if {@code pidResolver} or {@code qualityChooser} is {@code null}
     */
    public RxSubscriptions computeRxSubscription(ToIntFunction<Jid> pidResolver,
                                                 IntFunction<Optional<RxVidSubscriptionInfo.VideoQuality>> qualityChooser) {
        Objects.requireNonNull(pidResolver, "pidResolver cannot be null");
        Objects.requireNonNull(qualityChooser, "qualityChooser cannot be null");
        var pids = new ArrayList<Integer>();
        var subscriptions = new ArrayList<RxVidSubscriptionInfo>();
        if (participantProvider != null && participantProvider.isValid()) {
            for (var view : participantProvider.views()) {
                if (!view.isConnectedPeer()) {
                    continue;
                }
                var userJid = view.userJid();
                if (userJid == null) {
                    continue;
                }
                var pid = pidResolver.applyAsInt(userJid);
                if (pid < 0) {
                    continue;
                }
                pids.add(pid);
                var quality = qualityChooser.apply(pid).orElse(null);
                if (quality != null && quality.index() < VIDEO_QUALITY_BOUND) {
                    subscriptions.add(new RxVidSubscriptionInfoBuilder()
                            .pid(pid)
                            .vidQuality(quality)
                            .build());
                }
            }
        }
        var result = new RxSubscriptionsBuilder()
                .vidRxPids(pids)
                .vidSubscriptions(subscriptions)
                .build();
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "rx subscription computed, {0} pid(s), {1} quality entrie(s)",
                    pids.size(), subscriptions.size());
        }
        return result;
    }

    /**
     * Publishes a receive subscription, suppressing it when it has not changed.
     *
     * <p>When the subscription is identical to the last published one this returns an
     * empty result and changes nothing, so the caller sends no binding. When it differs
     * this records it as the new cached subscription, arms or rearms the resend timer
     * against the supplied clock, and returns the framed
     * {@link SubscriptionStunAttribute} for the caller to attach to a STUN binding
     * request. The subsequent resend the timer drives carries the most recently published
     * subscription.
     *
     * @param rxSubscriptions the receive subscription to publish; must not be {@code null}
     * @param nowNanos        the current time in the resend timer's nanosecond timebase
     * @return the STUN attribute to send, or an empty result when the subscription is a
     *         redundant resend
     * @throws NullPointerException if {@code rxSubscriptions} is {@code null}
     */
    public Optional<SubscriptionStunAttribute> publishRxSubscription(RxSubscriptions rxSubscriptions,
                                                                     long nowNanos) {
        Objects.requireNonNull(rxSubscriptions, "rxSubscriptions cannot be null");
        // TODO: emit the leading 0x4000 WARP rate control report the live capture carries ahead of the
        //  subscription; it is hop by hop SRTP sealed and the seal is not reproducible from captures yet.
        if (!rxState.shouldPublish(rxSubscriptions)) {
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "rx subscription unchanged, resend suppressed");
            }
            return Optional.empty();
        }
        rxState.record(rxSubscriptions);
        var attribute = buildReceiverAttribute(rxSubscriptions);
        cachedReceiverAttribute = attribute;
        armResendTimer(nowNanos);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "rx subscription published, {0} pid(s)", rxSubscriptions.vidRxPids().size());
        }
        return Optional.of(attribute);
    }

    /**
     * Cancels any armed resend timer and schedules a fresh one against the supplied clock.
     *
     * <p>Ensures a single resend entry is live at a time: an existing entry is cancelled
     * before a new one is scheduled {@link #resendInterval} ahead of {@code nowNanos}. When
     * the publisher is closed no timer is armed. The scheduled callback runs
     * {@link #onResendTimer()}.
     *
     * @param nowNanos the current time in the timer heap's nanosecond timebase
     */
    private void armResendTimer(long nowNanos) {
        if (closed) {
            return;
        }
        if (resendTimer != null) {
            resendTimer.cancel();
        }
        resendTimer = timerHeap.schedule(nowNanos, resendInterval, this::onResendTimer);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "rx subscription resend timer armed, interval={0}", resendInterval);
        }
    }

    /**
     * Resends the cached receive subscription and arms the next resend timer.
     *
     * <p>Invoked by the timer heap when the resend interval elapses. When a subscription
     * has been cached and the publisher is open, it hands the already built
     * {@link #cachedReceiverAttribute} to the resend callback, then schedules the next
     * resend; when nothing is cached or the publisher is closed it does neither, ending the
     * cadence. Reading the current time from the injected clock keeps the next deadline
     * on the heap's timebase.
     */
    private void onResendTimer() {
        if (closed) {
            return;
        }
        var cached = cachedReceiverAttribute;
        if (cached == null) {
            return;
        }
        rxResender.accept(cached);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "rx subscription resent");
        }
        armResendTimer(clock.getAsLong());
    }

    /**
     * Returns the hop by hop RTCP feedback subscription table for this call.
     *
     * <p>The caller registers and removes feedback subscriptions on the returned table to
     * tell the selective forwarding unit which RTCP feedback to forward for each media
     * SSRC.
     *
     * @return the RTCP feedback subscription table, never {@code null}
     */
    public RtcpRxSubscriptionTable rtcpRxTable() {
        return rtcpRxTable;
    }

    /**
     * Releases the publisher's timer and clears its cached subscription and feedback table.
     *
     * <p>Cancels the resend timer if it is armed, clears the cached receive subscription
     * so a later transport republishes from scratch, and empties the RTCP feedback table.
     * Idempotent: a second close does nothing.
     */
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (resendTimer != null) {
            resendTimer.cancel();
            resendTimer = null;
        }
        rxState.clear();
        cachedReceiverAttribute = null;
        rtcpRxTable.clear();
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "subscription publisher closed");
        }
    }
}
