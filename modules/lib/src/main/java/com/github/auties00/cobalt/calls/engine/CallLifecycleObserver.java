package com.github.auties00.cobalt.calls.engine;

import com.github.auties00.cobalt.calls.engine.context.CallContext;
import com.github.auties00.cobalt.calls.telemetry.CallResult;
import com.github.auties00.cobalt.wire.linked.call.CallEndReason;

/**
 * Observes the three end of call outcomes the lifecycle controller reports back to the call service.
 *
 * <p>The service sits above the controller (it holds the controller and delegates the public call
 * methods to it), so these are the controller's upward notifications: a call's media plane first going
 * active, an engine resolved call result the terminal end reason cannot recover, and a call ending. The
 * controller takes this observer as a constructor argument and the service satisfies it, so the upward
 * notification is a declared, typed dependency rather than a mutable callback the service pokes in after
 * construction; a bare engine build (the test harnesses) passes {@link #NONE}.
 *
 * @apiNote This is an internal engine seam, not a public surface; the call service is its only
 * implementer and passes {@code this} when it builds the controller through {@link LifecycleController#create}.
 */
public interface CallLifecycleObserver {
    /**
     * A no op observer for a bare engine build with no service above it.
     */
    CallLifecycleObserver NONE = new CallLifecycleObserver() {
        @Override
        public void onConnected(String callId) {
        }

        @Override
        public void onResult(String callId, CallResult result) {
        }

        @Override
        public void onEnded(String callId, CallContext context, CallEndReason reason) {
        }
    };

    /**
     * Reports that a call's media plane first reached its active bidirectional state.
     *
     * <p>Fired once per call so the service can stamp the call's connected instant on its telemetry
     * accumulator, symmetric with the ended instant stamped at {@link #onEnded}.
     *
     * @param callId the identifier of the connected call
     */
    void onConnected(String callId);

    /**
     * Reports an engine resolved call result the terminal end reason cannot recover.
     *
     * <p>Some outcomes carry a distinct result (an offer NACK is {@link CallResult#SERVER_NACK}, not the
     * generic teardown reason); the controller reports those here, keyed by call id, so the service records
     * the result on the call's telemetry before the call is torn down.
     *
     * @param callId the identifier of the call whose result resolved
     * @param result the resolved engine call result
     */
    void onResult(String callId, CallResult result);

    /**
     * Reports that a call has ended, at its single ending transition.
     *
     * <p>Fired once per call from the controller's teardown for every end path (local hangup, peer
     * terminate, reject, timeout, offer NACK, or setup failure), carrying the finished engine
     * {@link CallContext} (with its durations closed out) and the terminal {@link CallEndReason}. The
     * service frees the call's service level state, drains its end of call WAM telemetry, and pushes the
     * call log; the context is {@code null} when the registry allocated none (a test registry with no
     * manager), in which case the service records no call log but still frees the call.
     *
     * @param callId  the identifier of the ended call
     * @param context the finished engine call context, or {@code null} when none was allocated
     * @param reason  the terminal end reason
     */
    void onEnded(String callId, CallContext context, CallEndReason reason);
}
