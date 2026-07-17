package com.github.auties00.cobalt.calls.transport.congestion.bwe.shaping;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Latches the conservative startup mode off once the initial bandwidth estimate leaves a configured
 * band, releasing the estimator to ramp normally.
 *
 * <p>At call start the sender estimator holds a conservative initial estimate. This controller watches
 * the initial estimate through {@link #onInitEstimate(long)}: while enabled and the estimate stays
 * within {@code [lowerBoundBps, upperBoundBps]} the conservative mode remains active and the estimator
 * keeps the cautious startup behaviour; the first time the estimate falls below the lower bound or
 * rises above the upper bound the mode latches off and stays off for the life of the call, so a brief
 * return into the band does not arm it again.
 *
 * <p>Instances are not thread safe; the owning sender estimator drives one controller from the single
 * transport thread.
 */
public final class ConservativeInitMode {
    /**
     * The logger for {@link ConservativeInitMode}.
     */
    private static final System.Logger LOGGER = Log.get(ConservativeInitMode.class);

    /**
     * Whether conservative init mode is enabled by configuration.
     *
     * <p>When disabled the controller is permanently stopped and never latches.
     */
    private final boolean enabled;

    /**
     * Lower bound, in bits per second, of the band the init estimate must stay within to keep the mode
     * active.
     */
    private final long lowerBoundBps;

    /**
     * Upper bound, in bits per second, of the band the init estimate must stay within to keep the mode
     * active.
     */
    private final long upperBoundBps;

    /**
     * Whether the mode has latched off.
     *
     * <p>Once set, the mode stays stopped regardless of later estimates.
     */
    private boolean stopped;

    /**
     * Constructs a conservative init mode with the given enable flag and band.
     *
     * @param enabled       whether the mode is enabled; when {@code false} it starts stopped
     * @param lowerBoundBps the lower bound of the band, in bits per second
     * @param upperBoundBps the upper bound of the band, in bits per second
     */
    public ConservativeInitMode(boolean enabled, long lowerBoundBps, long upperBoundBps) {
        this.enabled = enabled;
        this.lowerBoundBps = lowerBoundBps;
        this.upperBoundBps = upperBoundBps;
        this.stopped = !enabled;
    }

    /**
     * Feeds the current initial bandwidth estimate and latches the mode off if it leaves the band.
     *
     * <p>Has no effect once the mode has stopped. While enabled and still active, an estimate below
     * {@link #lowerBoundBps} or above {@link #upperBoundBps} latches the stop.
     *
     * @param initBweBps the current initial estimate, in bits per second
     * @return {@code true} when the mode is still active after this estimate, {@code false} once it has
     *         stopped
     */
    public boolean onInitEstimate(long initBweBps) {
        if (stopped) {
            return false;
        }
        if (initBweBps < lowerBoundBps || initBweBps > upperBoundBps) {
            stopped = true;
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "conservative init mode latched off: estimate={0}bps bounds=[{1},{2}]bps",
                        initBweBps, lowerBoundBps, upperBoundBps);
            }
            return false;
        }
        return true;
    }

    /**
     * Returns whether conservative init mode is still active.
     *
     * @return {@code true} while the mode is enabled and has not latched off
     */
    public boolean isActive() {
        return enabled && !stopped;
    }

    /**
     * Returns whether the mode has latched off.
     *
     * @return {@code true} once the mode has stopped
     */
    public boolean isStopped() {
        return stopped;
    }
}
