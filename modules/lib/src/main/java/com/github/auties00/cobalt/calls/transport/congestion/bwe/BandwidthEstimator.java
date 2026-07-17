package com.github.auties00.cobalt.calls.transport.congestion.bwe;

import com.github.auties00.cobalt.calls.transport.congestion.bwe.combine.BitrateCombiner;
import com.github.auties00.cobalt.calls.transport.congestion.bwe.delay.GccDelayBasedEstimator;

/**
 * Abstracts the bandwidth estimation seam: a component that consumes per packet or per feedback
 * network observations and reports one target send bitrate.
 *
 * <p>A call runs two estimators behind this interface. {@link GccDelayBasedEstimator} is the GoogCC
 * delay based core (inter arrival grouping, trendline over use detection, and AIMD rate control) that
 * watches one way delay growth on received media. {@link LiveSenderBandwidthEstimator} is WhatsApp's
 * sender side controller, an AIMD loss and round trip time estimator whose output is fused with the
 * delay based estimate, the remote receiver estimate, the packet pair link capacity, and optional
 * machine learning estimates by the {@link BitrateCombiner}. Both expose the same surface: feed an
 * observation, read the current target, and reset for a new connection. This single seam lets the
 * combiner and the rate control treat the two producers uniformly.
 *
 * <p>Implementations are not required to be thread safe; the call session drives one estimator from
 * the single transport thread.
 */
public sealed interface BandwidthEstimator permits GccDelayBasedEstimator, LiveSenderBandwidthEstimator {
    /**
     * Returns the estimator's current target send bitrate.
     *
     * <p>Before any observation is fed, an implementation returns its configured initial estimate.
     *
     * @return the current target bitrate, in bits per second
     */
    long currentTargetBps();

    /**
     * Resets the estimator to its just constructed state.
     *
     * <p>Clears all accumulated history and restores the initial estimate so the same instance can be
     * reused for a new connection without carrying stale congestion state.
     */
    void reset();
}
