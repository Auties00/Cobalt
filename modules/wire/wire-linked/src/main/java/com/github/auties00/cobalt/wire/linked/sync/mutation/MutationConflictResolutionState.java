package com.github.auties00.cobalt.wire.linked.sync.mutation;

/**
 * Represents the outcome selected by a sync action handler when a local
 * pending mutation conflicts with an incoming remote mutation addressing
 * the same index.
 *
 * <p>App state sync mutations are identified by a deterministic index (such
 * as a chat JID for a mute or archive action). When a locally queued mutation
 * and a server mutation target the same index, the conflict must be resolved
 * deterministically so that the local view eventually converges with the
 * server view. Each handler inspects the two mutations and returns one of
 * these states to drive the resolution outcome.
 */
public enum MutationConflictResolutionState {
    /**
     * Applies the remote mutation and drops the local pending mutation.
     *
     * <p>Selected when the server copy is authoritative and the pending local
     * change should be discarded.
     */
    APPLY_REMOTE_DROP_LOCAL,

    /**
     * Skips the remote mutation and keeps the local pending mutation queued
     * for a later push to the server.
     *
     * <p>Selected when the pending local change is newer or takes precedence
     * over the incoming remote copy.
     */
    SKIP_REMOTE,

    /**
     * Skips the remote mutation and also drops the local pending mutation.
     *
     * <p>Selected when the handler has already produced a merged outcome
     * during conflict resolution (for example, for message range based
     * actions) and neither the remote nor the local mutation needs to be
     * applied separately.
     */
    SKIP_REMOTE_DROP_LOCAL
}
