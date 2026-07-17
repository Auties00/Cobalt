package com.github.auties00.cobalt.calls.engine.info;

import com.github.auties00.cobalt.calls.engine.context.CallManager;
import com.github.auties00.cobalt.calls.telemetry.CallResult;
import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.Objects;

/**
 * Refreshes a call's info snapshot for a lifecycle event from the resolved context's running state.
 *
 * <p>Resolves the call context for the reported identifier through the {@link CallManager}, and when the
 * manager holds it, reads the context's accumulated active and lonely durations, its current state, and
 * its current result under the context lock, then hands them to the single {@link CallInfoManager} to
 * rebuild and publish the snapshot. A reported identifier the manager does not hold produces no update.
 *
 * @param manager     the call manager whose context supplies the running state for the reported call
 * @param infoManager the call info manager whose snapshot is rebuilt from that running state
 */
public record LiveCallInfoUpdater(CallManager manager, CallInfoManager infoManager)
        implements CallInfoUpdater {
    /**
     * The logger for {@link LiveCallInfoUpdater}.
     */
    private static final System.Logger LOGGER = Log.get(LiveCallInfoUpdater.class);

    /**
     * Rejects a null manager or info manager.
     *
     * @throws NullPointerException if {@code manager} or {@code infoManager} is {@code null}
     */
    public LiveCallInfoUpdater {
        Objects.requireNonNull(manager, "manager cannot be null");
        Objects.requireNonNull(infoManager, "infoManager cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation resolves the call context for {@code callId} through the
     * {@link CallManager} and returns without touching the snapshot when the manager holds no such call.
     * When it does, it reads the context's accumulated active and lonely durations, current state, and
     * current result under the context lock, then refreshes the {@link CallInfoManager} snapshot from
     * them. An as yet unresolved result reads as the in progress
     * {@link CallResult#CALL_OFFER_ACK_NOT_RECEIVED}.
     */
    @Override
    public void updateForEvent(String callId, CallEventType eventType) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(eventType, "eventType cannot be null");
        var context = manager.getByCallId(callId).orElse(null);
        if (context == null) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "call info update skipped, unknown call {0} for event {1}",
                        callId, eventType);
            }
            return;
        }
        context.lock().lock();
        try {
            var active = Duration.ofMillis(context.activeDurationMillis());
            var lonely = Duration.ofMillis(context.lonelyDurationMillis());
            var result = context.result().orElse(CallResult.CALL_OFFER_ACK_NOT_RECEIVED);
            // TODO: report the real setup duration once connected state timestamp accounting is threaded through the context
            infoManager.updateForEvent(eventType, context.state(), result, active, lonely,
                    Duration.ZERO, null);
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "call info refreshed for call {0}, event {1}, state {2}, result {3}",
                        callId, eventType, context.state(), result);
            }
        } finally {
            context.lock().unlock();
        }
    }
}
