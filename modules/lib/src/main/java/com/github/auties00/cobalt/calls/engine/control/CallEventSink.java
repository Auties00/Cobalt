package com.github.auties00.cobalt.calls.engine.control;

import com.github.auties00.cobalt.calls.engine.control.event.ControlCallEvent;

/**
 * The egress an in call controller emits a {@link ControlCallEvent} into for fan out to listeners.
 *
 * <p>When an in call control operation changes call visible state, the controller publishes a typed
 * {@link ControlCallEvent} describing the change. This seam hides the gate that decides whether an event
 * survives and the per listener dispatch that follows: an implementation passes the event through its gate
 * and, when the event survives, delivers it to each matching listener off the caller's thread. A controller
 * therefore depends only on this single method sink, never on a concrete bus, and a controller can be
 * exercised in isolation by capturing the events it emits.
 *
 * @implSpec An implementation MUST accept any {@link ControlCallEvent} without throwing and MUST NOT block
 * the caller: the emit happens on the controller's own thread (a timer callback or an operation call), and
 * the fan out to listeners is dispatched elsewhere so a slow listener never stalls the controller. An
 * implementation MAY suppress an event whose {@link ControlCallEvent#type()} is marked internal only.
 * @see ControlCallEvent
 */
@FunctionalInterface
public interface CallEventSink {
    /**
     * Emits a control event for gating and fan out to listeners.
     *
     * @implSpec An implementation MUST NOT block the caller and MUST NOT throw for a well formed event.
     * @param event the event to emit; never {@code null}
     */
    void emit(ControlCallEvent event);
}
