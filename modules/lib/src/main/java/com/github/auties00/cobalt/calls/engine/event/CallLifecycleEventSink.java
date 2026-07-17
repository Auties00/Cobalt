package com.github.auties00.cobalt.calls.engine.event;

import com.github.auties00.cobalt.calls.engine.LifecycleController;

/**
 * Receives the typed events a call raises, after the engine's emit gate has decided they are observable.
 *
 * <p>The voip engine funnels every state change the host may care about through one generic dispatcher,
 * which selects the event by its {@link CallEventType}. Cobalt routes that single egress through this
 * seam: the {@link LifecycleController} reports each event by its {@link CallEventType} and an opaque
 * serialized payload, and the implementer applies the emit gate that suppresses internal only ids and
 * fans the surviving events out to the registered listeners, one fresh virtual thread per matching
 * listener, so a listener may block freely without stalling the socket reader.
 *
 * <p>This is the contract the fan out unit implements; the {@link LifecycleController} depends only on
 * this interface, never on the concrete fan out, so the two compose without the controller knowing how an
 * event reaches a listener. The payload byte layout depends on the {@link CallEventType} and is opaque to
 * this seam: the controller passes the bytes through verbatim and the implementer decodes only the events
 * it surfaces.
 *
 * @apiNote This is an internal engine collaborator, not a public surface; embedders never call it. The
 * application observes call events through the {@code WhatsAppListener} bus the implementer fans out to,
 * not through this seam.
 */
@FunctionalInterface
public interface CallLifecycleEventSink {
    /**
     * Emits one typed event with its serialized payload.
     *
     * <p>The {@link LifecycleController} calls this for each event it raises (a call state transition, an
     * offer, accept or terminate leg, a fatal error, and so on). The implementer applies the emit gate for
     * the event's id and, when the event survives the gate, fans it out to the matching listeners; an event
     * the gate suppresses is dropped. The call returns to the controller once the event has been handed to
     * the bus, and any listener dispatch runs on the bus's own threads rather than on the calling thread.
     *
     * @param eventType the kind of event being emitted; never {@code null}
     * @param payload   the serialized event payload whose layout is determined by {@code eventType}; never
     *                  {@code null}, possibly empty when the event carries no payload
     */
    void emit(CallEventType eventType, byte[] payload);
}
