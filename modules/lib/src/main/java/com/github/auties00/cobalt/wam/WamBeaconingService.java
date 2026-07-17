package com.github.auties00.cobalt.wam;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.util.OptionalLong;

/**
 * Supplies the per-buffer-key beaconing sequence numbers consulted by
 * {@link WamService} when sealing each WAM event into its outbound buffer.
 *
 * <p>This interface backs the once-a-day one-percent activation roll that
 * sub-samples the WAM upload stream. When the roll for the day succeeds for a
 * buffer key, every subsequent event written to that key gets an incrementing
 * sequence number attached as field {@code 3433}
 * ({@link com.github.auties00.cobalt.wam.binary.WamGlobalEncoder#writeBeaconSessionId});
 * when the roll fails, the sequence number is absent and no per-event beacon
 * global is emitted. A non-empty {@link #nextSequenceNumber(String)} result is
 * the only path through which beaconing globals enter the wire.
 *
 * <p>The production implementation is {@link LiveWamBeaconingService}; tests
 * substitute their own controllable variant.
 *
 * @implSpec
 * Implementations must keep activation state and the running counter
 * partitioned by {@code bufferKey} so that activation of {@code "regular"}
 * does not influence {@code "realtime"} or any of the private-stats buffer
 * keys. Implementations need not be thread-safe; every call originates from
 * {@link WamService}'s single flush thread.
 *
 * @implNote
 * This implementation recognises buffer keys that are the union of the two
 * channel-derived names ({@code "regular"}, {@code "realtime"}) and the eight
 * private-stats id keys declared by {@code WAWebWamGlobals.PrivateStatsAllIds}
 * (such as {@code "DefaultPsId"} or {@code "IdTtlDaily"}).
 */
@WhatsAppWebModule(moduleName = "WAWebWamBeaconing")
interface WamBeaconingService {
    /**
     * Returns the next beaconing sequence number for the given buffer key when
     * beaconing is active for the current UTC day, otherwise an empty value.
     *
     * <p>Called by {@link WamService} once per event when sealing it into the
     * outbound buffer; the returned value, when present, becomes the
     * {@code beaconSessionId} global written into that event's frame.
     *
     * @implSpec
     * On the first call of a new UTC calendar day for {@code bufferKey},
     * implementations must re-decide activation; subsequent calls within the
     * same day must return monotonically increasing values when activation
     * succeeded, and an empty value when it failed. A counter reset to a fresh
     * sequence start at every new active day is part of the contract; values
     * must remain positive across the {@link Integer#MAX_VALUE} boundary so the
     * counter never sign-flips.
     *
     * @param bufferKey the buffer key identifying the beaconing track (for
     *                  example {@code "regular"}, {@code "realtime"}, or one of
     *                  the eight {@code WAWebWamGlobals.PrivateStatsAllIds} key
     *                  names)
     * @return the next sequence number when beaconing is active, otherwise
     *         empty
     */
    @WhatsAppWebExport(moduleName = "WAWebWamBeaconing", exports = "maybeGetEventSequenceNumber", adaptation = WhatsAppAdaptation.ADAPTED)
    OptionalLong nextSequenceNumber(String bufferKey);
}
