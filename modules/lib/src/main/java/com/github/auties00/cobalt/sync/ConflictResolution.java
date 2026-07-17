package com.github.auties00.cobalt.sync;

import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

/**
 * The decision returned by a sync action handler when a remote mutation
 * arrives at an index that the local device already has a pending mutation
 * for.
 *
 * <p>The {@link MutationConflictResolutionState} verdict drives the
 * {@code remoteMutationsToApply} and {@code pendingSetMutationsToDrop}
 * buckets in the resolver: it decides whether the remote mutation is
 * applied while the local pending mutation is dropped, the remote mutation
 * is skipped outright, or both sides are discarded in favour of a merged
 * mutation. The {@link #mergedMutation()} payload is non-{@code null} only
 * for the merge case produced by {@link #merged(DecryptedMutation.Trusted)};
 * the enum-only verdicts produced by {@link #of(MutationConflictResolutionState)}
 * leave it {@code null}.
 *
 * @param state          the verdict that selects between applying the
 *                       remote mutation, skipping it, and replacing both
 *                       sides with {@code mergedMutation}
 * @param mergedMutation the mutation that supersedes both sides when the
 *                       handler combined two non-enclosing message ranges,
 *                       or {@code null} for the enum-only verdicts
 */
@WhatsAppWebModule(moduleName = "WAWebSyncActionStore")
public record ConflictResolution(
        MutationConflictResolutionState state,
        DecryptedMutation.Trusted mergedMutation
) {
    /**
     * Wraps an enum-only verdict that needs no merged mutation.
     *
     * <p>Returned by handlers whose verdict maps straight onto one of
     * {@link MutationConflictResolutionState#APPLY_REMOTE_DROP_LOCAL},
     * {@link MutationConflictResolutionState#SKIP_REMOTE}, or the no-merge form of
     * {@link MutationConflictResolutionState#SKIP_REMOTE_DROP_LOCAL}. Use
     * {@link #merged(DecryptedMutation.Trusted)} instead when the handler
     * substitutes a third mutation in place of the two losing ones.
     *
     * @param state the verdict to record
     * @return a resolution carrying {@code state} and a {@code null}
     *         {@link #mergedMutation()}
     * @see #merged(DecryptedMutation.Trusted)
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncActionStore", exports = "doConflictResolution", adaptation = WhatsAppAdaptation.ADAPTED)
    public static ConflictResolution of(MutationConflictResolutionState state) {
        return new ConflictResolution(state, null);
    }

    /**
     * Wraps a verdict that replaces both sides with a third mutation
     * computed by the handler.
     *
     * <p>Used by message-range handlers (archive, mark-as-read,
     * delete-chat, and similar) when neither the local pending mutation nor
     * the incoming remote mutation fully encloses the other. The handler
     * unions the two ranges into a single mutation, returns it here, and the
     * caller drops both originals before applying {@code merged} to local
     * state. The verdict is fixed at
     * {@link MutationConflictResolutionState#SKIP_REMOTE_DROP_LOCAL} because the
     * remote mutation is dropped without being applied as-is.
     *
     * @param merged the merged mutation to apply and add to the pending
     *               queue
     * @return a resolution carrying
     *         {@link MutationConflictResolutionState#SKIP_REMOTE_DROP_LOCAL} and the
     *         supplied {@code merged} payload
     * @see #of(MutationConflictResolutionState)
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncActionStore", exports = "doConflictResolution", adaptation = WhatsAppAdaptation.ADAPTED)
    public static ConflictResolution merged(DecryptedMutation.Trusted merged) {
        return new ConflictResolution(MutationConflictResolutionState.SKIP_REMOTE_DROP_LOCAL, merged);
    }
}
