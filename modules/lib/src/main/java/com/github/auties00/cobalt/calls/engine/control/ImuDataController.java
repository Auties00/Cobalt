package com.github.auties00.cobalt.calls.engine.control;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.lang.System.Logger.Level;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Drives the in call inertial measurement unit control by publishing the local device's motion samples and
 * tracking inbound ones.
 *
 * <p>Samples are uploaded on the call's application data stream, the same transport that carries reactions
 * and transcripts, packed into fixed frames of {@link ImuSample#FRAME_SIZE} bytes. {@link #publish(ImuSample)}
 * hands a local sample to the application data stream sender and retains it as the latest sample sent;
 * inbound peer samples are delivered through {@link #onSample(Jid, ImuSample)}, which records the latest
 * sample per participant and notifies the inbound observer. Only the most recent sample in each direction is
 * held, since IMU is a continuous best effort stream where stale samples have no value.
 *
 * <p>A sample is an opaque byte frame. Its internal accelerometer, gyroscope, and orientation field layout
 * is produced by the mobile host's sensor capture, and this controller only transports the frame verbatim; it
 * neither decodes nor rewrites the bytes. A distinct sample never carries an on wire sub type byte, so the
 * whole frame is the reading.
 *
 * <p>Unlike the other in call controls, IMU emits no host facing
 * {@link com.github.auties00.cobalt.calls.engine.event.CallEvent}; it bridges the local capture and the
 * application data stream and exposes the latest inbound sample per participant for a consumer such as a
 * video renderer. The controller is bound to its sample sender and the inbound observer at construction and
 * owns no timers.
 */
public final class ImuDataController {
    /**
     * The logger for {@link ImuDataController}.
     */
    private static final System.Logger LOGGER = Log.get(ImuDataController.class);

    /**
     * The application data stream sender local IMU samples are published through.
     */
    private final Consumer<ImuSample> streamSender;

    /**
     * The observer notified when an inbound peer IMU sample arrives.
     */
    private final BiConsumer<Jid, ImuSample> inboundObserver;

    /**
     * The latest inbound sample per participant, keyed by device JID.
     */
    private final Map<Jid, ImuSample> latestInbound = new ConcurrentHashMap<>();

    /**
     * The latest local sample published, or {@code null} when none has been published.
     *
     * <p>Declared {@code volatile} so {@link #publish(ImuSample)} can store it and {@link #latestOutbound()}
     * can read it without a lock: the field is a lone reference with no compound read then write.
     */
    private volatile ImuSample latestOutbound;

    /**
     * Constructs an IMU controller bound to the application data stream sender and the inbound observer.
     *
     * @param streamSender    the application data stream sender to publish local samples through; never
     *                        {@code null}
     * @param inboundObserver the observer for inbound peer IMU samples; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public ImuDataController(Consumer<ImuSample> streamSender, BiConsumer<Jid, ImuSample> inboundObserver) {
        this.streamSender = Objects.requireNonNull(streamSender, "streamSender cannot be null");
        this.inboundObserver = Objects.requireNonNull(inboundObserver, "inboundObserver cannot be null");
    }

    /**
     * Publishes the local device's IMU sample on the application data stream.
     *
     * <p>Hands the sample to the application data stream sender and retains it as the latest local sample.
     *
     * @param sample the local IMU sample to publish; never {@code null}
     * @throws NullPointerException if {@code sample} is {@code null}
     */
    public void publish(ImuSample sample) {
        Objects.requireNonNull(sample, "sample cannot be null");
        latestOutbound = sample;
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "publishing imu sample len={0} timestampMicros={1}",
                    sample.payload().length, sample.timestampMicros());
        }
        streamSender.accept(sample);
    }

    /**
     * Records an inbound peer IMU sample and notifies the inbound observer.
     *
     * <p>Replaces the participant's latest inbound sample and delivers the sample to the observer.
     *
     * @param participant the device JID the sample came from; never {@code null}
     * @param sample      the inbound IMU sample; never {@code null}
     * @throws NullPointerException if {@code participant} or {@code sample} is {@code null}
     */
    public void onSample(Jid participant, ImuSample sample) {
        Objects.requireNonNull(participant, "participant cannot be null");
        Objects.requireNonNull(sample, "sample cannot be null");
        latestInbound.put(participant, sample);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "received imu sample from {0} len={1}", participant, sample.payload().length);
        }
        inboundObserver.accept(participant, sample);
    }

    /**
     * Returns the latest local IMU sample published, if any.
     *
     * @return an {@link Optional} with the latest local sample, or empty when none has been published
     */
    public Optional<ImuSample> latestOutbound() {
        return Optional.ofNullable(latestOutbound);
    }

    /**
     * Returns the latest inbound IMU sample from a participant, if one is held.
     *
     * @param participant the device JID to look up; never {@code null}
     * @return an {@link Optional} with the participant's latest inbound sample, or empty
     * @throws NullPointerException if {@code participant} is {@code null}
     */
    public Optional<ImuSample> latestInbound(Jid participant) {
        Objects.requireNonNull(participant, "participant cannot be null");
        return Optional.ofNullable(latestInbound.get(participant));
    }
}
