package com.github.auties00.cobalt.wire.linked.sync.mutation;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;

/**
 * Result returned by a sync action handler after attempting to apply a single
 * mutation to local state.
 *
 * <p>App state sync processes mutations one at a time. Each handler reports
 * back whether the mutation was applied, skipped, rejected, or deferred for
 * a later retry. The outcome drives downstream bookkeeping: orphan mutation
 * persistence, audit logging, and version reconciliation.
 *
 * <p>For orphan outcomes the {@code modelId} and {@code modelType} fields
 * record the identifier and kind of the referenced entity so that the orphan
 * can be replayed later when the entity becomes available (for example, when
 * the referenced chat arrives via history sync).
 *
 * @param actionState the outcome category produced by the handler
 * @param modelId the identifier of the missing entity for orphan outcomes,
 *                or {@code null} for non orphan outcomes
 * @param modelType the type of the missing entity for orphan outcomes,
 *                  or {@code null} for non orphan outcomes
 */
public record MutationApplicationResult(
        SyncActionState actionState,
        String modelId,
        String modelType
) {
    /**
     * Returns a result indicating that the mutation was successfully applied
     * to local state.
     *
     * @return a success result with no orphan metadata
     */
    public static MutationApplicationResult success() {
        return new MutationApplicationResult(SyncActionState.SUCCESS, null, null);
    }

    /**
     * Returns a result indicating that no handler is registered for the
     * mutation's action type.
     *
     * <p>Unsupported mutations are retained so that a future handler can
     * replay them when support is added.
     *
     * @return an unsupported result with no orphan metadata
     */
    public static MutationApplicationResult unsupported() {
        return new MutationApplicationResult(SyncActionState.UNSUPPORTED, null, null);
    }

    /**
     * Returns a result indicating that the mutation payload could not be
     * decoded or failed structural validation.
     *
     * @return a malformed result with no orphan metadata
     */
    public static MutationApplicationResult malformed() {
        return new MutationApplicationResult(SyncActionState.MALFORMED, null, null);
    }

    /**
     * Returns a result indicating that the mutation was intentionally skipped
     * by the handler (for example, because it is version gated or duplicated).
     *
     * @return a skipped result with no orphan metadata
     */
    public static MutationApplicationResult skipped() {
        return new MutationApplicationResult(SyncActionState.SKIPPED, null, null);
    }

    /**
     * Returns a result indicating that the handler encountered an error while
     * attempting to apply the mutation.
     *
     * @return a failed result with no orphan metadata
     */
    public static MutationApplicationResult failed() {
        return new MutationApplicationResult(SyncActionState.FAILED, null, null);
    }

    /**
     * Returns a result indicating that the mutation references an entity not
     * yet present locally, without specifying which entity is missing.
     *
     * @return an orphan result with no identifier information
     */
    public static MutationApplicationResult orphan() {
        return new MutationApplicationResult(SyncActionState.ORPHAN, null, null);
    }

    /**
     * Returns a result indicating that the mutation references an entity not
     * yet present locally, tagged with the referenced entity identifier and
     * type so that the mutation can be replayed on entity arrival.
     *
     * @param modelId the identifier of the missing entity
     * @param modelType the type of the missing entity
     * @return an orphan result carrying identifier and type metadata
     */
    public static MutationApplicationResult orphan(String modelId, String modelType) {
        return new MutationApplicationResult(SyncActionState.ORPHAN, modelId, modelType);
    }

    /**
     * Returns whether this result represents an orphan outcome.
     *
     * @return {@code true} if the action state is {@link SyncActionState#ORPHAN}
     */
    public boolean isOrphan() {
        return actionState == SyncActionState.ORPHAN;
    }
}
