package com.github.auties00.cobalt.calls.engine.context;

import com.github.auties00.cobalt.wire.linked.call.Call;
import com.github.auties00.cobalt.calls.engine.state.CallLifecycleState;
import com.github.auties00.cobalt.calls.engine.LifecycleController;

/**
 * Allocates and frees the engine call context for a call on behalf of the lifecycle controller.
 *
 * <p>The engine holds at most a primary and one optional secondary (dual) call context, each with its own
 * lock. A context is allocated when a call starts, is answered, or arrives as an offer, and is freed when
 * the call ends. The {@link LifecycleController} reaches that allocation through this seam, asking for a
 * context to be allocated for a new call and freed on teardown, keyed by call identifier. The controller
 * keeps its own light orchestration handle for wiring across units; the heavy context, its timer entries,
 * its durations, and its lock live behind this seam, and the state transition and timer seams then drive
 * that context by the same identifier.
 *
 * <p>The dual call ceiling is enforced here: an allocation that would exceed a primary and one secondary
 * call is refused. The controller applies its own admission check before allocating, so this seam's
 * refusal is the authoritative backstop rather than the primary gate.
 *
 * @apiNote This is an internal engine collaborator, not a public surface; embedders never call it.
 * @implNote This implementation guards each context with a {@link java.util.concurrent.locks.ReentrantLock}
 * following the Cobalt threading model. When both a primary and a secondary context are live, the secondary
 * is freed before the primary.
 */
public interface CallContextRegistry {
    /**
     * Allocates an engine call context for a new call and returns it.
     *
     * <p>The implementer lays out the context from the call's identity (its identifier, peer, creator,
     * direction, topology, and video flag, all carried by the {@link Call}) and its initial internal
     * state, registering it under the call's identifier so the state transition and timer seams can drive
     * it, and returns the allocated context so the caller can hand it to its teardown collaborators (the
     * outbound call log sink reads the finished context at teardown). Allocating a context for an identifier
     * that already has one refreshes that context rather than duplicating it. An allocation that would
     * exceed the dual call ceiling is refused with an {@link IllegalStateException}.
     *
     * @param call         the public view of the call whose context is being allocated
     * @param initialState the call's initial internal state
     * @return the allocated engine call context, or {@code null} when the implementer builds none (a test
     *         registry with no manager)
     * @throws NullPointerException  if {@code call} or {@code initialState} is {@code null}
     * @throws IllegalStateException if allocating would exceed the dual call ceiling
     */
    CallContext allocate(Call call, CallLifecycleState initialState);

    /**
     * Frees the engine call context for a call.
     *
     * <p>Called as a call tears down so the context, its timers, and its manager slot are released.
     * Freeing a context for an identifier that has none is a no op.
     *
     * @param callId the identifier of the call whose context is being freed
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    void release(String callId);
}
