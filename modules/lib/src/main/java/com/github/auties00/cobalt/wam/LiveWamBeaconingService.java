package com.github.auties00.cobalt.wam;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.core.util.DataUtils;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Provides the production {@link WamBeaconingService} that performs a one-percent
 * activation roll at the start of each UTC calendar day, then hands out an
 * incrementing sequence number per call while the day stays active.
 *
 * <p>This class is wired into {@link LiveWamService} via its constructor and
 * consumed by {@link WamService} when sealing each event. Selecting an
 * alternative implementation is done by subclassing {@link WamService} and
 * passing a different {@link WamBeaconingService} to its base constructor.
 *
 * @implNote
 * This implementation diverges from WhatsApp Web on two fronts. First, WA Web
 * persists each buffer key's last roll in
 * {@code WAWebUserPrefsGeneral.getWamBeaconingSettings} (an IndexedDB row keyed
 * by the buffer key), so the activation decision survives a page reload mid-day;
 * Cobalt holds state in memory only and re-rolls on every process start. Second,
 * the counter is a {@code long} (not the {@code int} WA Web's JavaScript
 * {@code Number} naturally provides) so Cobalt does not silently sign-flip past
 * the 2^31 mark.
 */
@WhatsAppWebModule(moduleName = "WAWebWamBeaconing")
final class LiveWamBeaconingService implements WamBeaconingService {
    /**
     * The logger for {@link LiveWamBeaconingService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveWamBeaconingService.class);

    /**
     * Holds the one-percent cutoff applied to each fresh
     * {@link DataUtils#randomDouble} roll; matches the literal {@code .01} in
     * {@code WAWebWamBeaconing}.
     */
    private static final double ACTIVATION_PROBABILITY = 0.01;

    /**
     * Holds the per-buffer-key activation and counter state, keyed by the
     * buffer key string passed to {@link #nextSequenceNumber(String)}.
     */
    private final ConcurrentMap<String, ChannelState> states;

    /**
     * Constructs a beaconing source with no buffer keys yet known; state for
     * each key is created lazily on its first
     * {@link #nextSequenceNumber(String)} call.
     */
    LiveWamBeaconingService() {
        this.states = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation truncates {@link Instant#now} to
     * {@link ChronoUnit#DAYS} to derive the activation day; whenever that day
     * differs from the cached one for {@code bufferKey} the roll is redone with
     * a fresh {@link DataUtils#randomDouble} draw and the counter resets to
     * {@code 0} so the first post-activation pre-increment yields {@code 1}.
     */
    @Override
    public OptionalLong nextSequenceNumber(String bufferKey) {
        var state = states.computeIfAbsent(bufferKey, _ -> new ChannelState());
        var currentDayEpoch = Instant.now().truncatedTo(ChronoUnit.DAYS).getEpochSecond();
        if (currentDayEpoch != state.activationDayEpoch) {
            state.activationDayEpoch = currentDayEpoch;
            state.active = DataUtils.randomDouble() <= ACTIVATION_PROBABILITY;
            state.sequenceNumber = 0L;
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "wam beaconing roll for {0}: active={1}", bufferKey, state.active);
        }

        if (!state.active) {
            return OptionalLong.empty();
        }

        var sequence = ++state.sequenceNumber;
        if (Log.TRACE) LOGGER.log(Level.TRACE, "wam beaconing sequence for {0}: {1}", bufferKey, sequence);
        return OptionalLong.of(sequence);
    }

    /**
     * Holds the per-buffer-key activation and counter state, recreated lazily
     * the first time a key is observed by
     * {@link LiveWamBeaconingService#nextSequenceNumber(String)}.
     *
     * @implNote
     * This implementation stores {@code sequenceNumber} as a {@code long} so
     * the counter cannot wrap past {@link Integer#MAX_VALUE} into negative
     * territory; the JavaScript reference cannot hit that boundary because its
     * counter is a {@code Number}.
     */
    private static final class ChannelState {
        /**
         * Holds the UTC epoch second of the calendar day for which
         * {@link #active} was last decided; initialised to {@code -1} so the
         * first call observes a day change and re-rolls.
         */
        long activationDayEpoch = -1;

        /**
         * Holds whether beaconing is active for the current activation day;
         * {@code true} means the most recent roll fell at or below
         * {@link LiveWamBeaconingService#ACTIVATION_PROBABILITY}.
         */
        boolean active = false;

        /**
         * Holds the running monotonic counter that pre-increments on each
         * non-empty call; reset to {@code 0} at every activation-day change so
         * the first post-reset call returns {@code 1}.
         */
        long sequenceNumber = 0L;
    }
}
