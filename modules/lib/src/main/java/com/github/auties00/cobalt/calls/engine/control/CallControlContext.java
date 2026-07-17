package com.github.auties00.cobalt.calls.engine.control;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Objects;

/**
 * Carries the minimal per call identity an in call control operation needs to address its signaling.
 *
 * <p>Every in call control action a controller emits stamps the same universal call header onto its
 * {@code <call>} child element: the {@code call-id} that names the call and the {@code call-creator}
 * device JID that identifies its originating device. Controllers also need to know the local device's own
 * JID, so a controller can recognize an inbound action that names itself, and whether the call is a group
 * call, because several actions (peer mute request, raise hand, screen share, reactions) are only
 * meaningful or only fanned out in a group. This record carries exactly that identity, decoupling the
 * controllers from the heavier per call engine context that owns the full mutable call state.
 *
 * <p>The record is immutable and is rebuilt by the lifecycle layer whenever it hands a controller the
 * call to act on, so a controller never holds a stale call identity across calls; it is the lightweight
 * value the engine's per call context projects for the control surface rather than a reference into the
 * context itself.
 *
 * @param callId      the identifier of the call this control operation targets; never {@code null}
 * @param callCreator the call creator's device JID stamped as {@code call-creator}; never {@code null}
 * @param selfJid     the local device's own JID, used to recognize an action that names this device;
 *                    never {@code null}
 * @param group       {@code true} when the call is a group call, which gates the group only control
 *                    actions and selects fanout addressing
 */
public record CallControlContext(String callId, Jid callCreator, Jid selfJid, boolean group) {
    /**
     * Validates the record components.
     *
     * @throws NullPointerException if {@code callId}, {@code callCreator}, or {@code selfJid} is
     *                              {@code null}
     */
    public CallControlContext {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(callCreator, "callCreator cannot be null");
        Objects.requireNonNull(selfJid, "selfJid cannot be null");
    }

    /**
     * Returns a non group control context for the given call identity.
     *
     * <p>This is the convenience used for a call between two devices, where the group only actions do not
     * apply; it sets {@link #group()} to {@code false}.
     *
     * @param callId      the call identifier
     * @param callCreator the call creator's device JID
     * @param selfJid     the local device's own JID
     * @return a non group control context
     * @throws NullPointerException if any argument is {@code null}
     */
    public static CallControlContext oneToOne(String callId, Jid callCreator, Jid selfJid) {
        return new CallControlContext(callId, callCreator, selfJid, false);
    }

    /**
     * Returns a group control context for the given call identity.
     *
     * <p>This is the convenience used for a group call, where the group only actions (peer mute request,
     * raise hand, screen share, reactions) are meaningful; it sets {@link #group()} to {@code true}.
     *
     * @param callId      the call identifier
     * @param callCreator the call creator's device JID
     * @param selfJid     the local device's own JID
     * @return a group control context
     * @throws NullPointerException if any argument is {@code null}
     */
    public static CallControlContext group(String callId, Jid callCreator, Jid selfJid) {
        return new CallControlContext(callId, callCreator, selfJid, true);
    }
}
