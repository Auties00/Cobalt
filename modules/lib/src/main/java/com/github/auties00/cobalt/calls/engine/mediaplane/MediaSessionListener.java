package com.github.auties00.cobalt.calls.engine.mediaplane;

import com.github.auties00.cobalt.calls.engine.LifecycleController;

/**
 * The per call notifications a {@linkplain MediaPlane#bringUp brought up} media plane reports back to the
 * lifecycle controller.
 *
 * <p>The media plane is a downstream unit the controller drives; the two events it raises during a call
 * (the plane reaching its first bidirectional traffic, and the local platform camera capture being
 * revoked mid call) flow back through this listener, supplied per call at bring up rather than held by
 * the plane. Keeping the notification scoped to the one bring up call means the plane holds no reference
 * to the controller, so the controller owns the plane as a plain constructor dependency with no cycle.
 *
 * @apiNote This is an internal engine seam, not a public surface; the lifecycle controller is its only
 * implementer and passes it into {@link MediaPlane#bringUp bringUp} for each call.
 */
public interface MediaSessionListener {
    /**
     * Reports that the call's media plane has reached its first bidirectional traffic.
     *
     * <p>Fired once per call, on a fresh virtual thread, when the relay transport first carries traffic;
     * the controller advances the call to its active state through
     * {@link LifecycleController#onMediaConnected(String)}.
     *
     * @param callId the identifier of the connected call
     */
    void onConnected(String callId);

    /**
     * Reports that the call's local platform camera capture was revoked by the operating system mid call.
     *
     * <p>Fired on the capture driver's pump thread when the platform camera enters its interrupted state;
     * the controller drives the local video to paused and notifies the peer through
     * {@link LifecycleController#onLocalCaptureInterrupted(String)}.
     *
     * @param callId the identifier of the call whose camera capture was revoked
     */
    void onCaptureInterrupted(String callId);
}
