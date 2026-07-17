package com.github.auties00.cobalt.calls.transport.congestion.bwe.combine;

/**
 * Names the two phases of the remote picture in picture second stage combine that the
 * {@link BitrateCombiner} runs while {@link CombineMode#REMOTE_PIP} is active.
 *
 * <p>A new combine begins in {@link #ENTER}, where the combined estimate ramps toward the remote
 * receiver estimate and the packet pair link capacity. It converts to {@link #THROTTLE} once enough
 * time has elapsed or the remote estimate has grown large enough, after which the second stage applies
 * the steadier throttled fusion. The transition is one way for the life of the combine: a combine never
 * returns from {@link #THROTTLE} to {@link #ENTER}.
 *
 * @implNote This implementation converts from {@link #ENTER} to {@link #THROTTLE} once 7.5 seconds have
 * elapsed since the combine began or the remote receiver estimate reaches 200 kbps, whichever comes first.
 */
public enum RemotePipPhase {
    /**
     * The entering phase, in which the combined estimate ramps toward the remote receiver estimate and
     * the packet pair link capacity.
     */
    ENTER,

    /**
     * The throttled phase, in which the second stage applies the steadier throttled fusion after the
     * entering phase ends.
     */
    THROTTLE
}
