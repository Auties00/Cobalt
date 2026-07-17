package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorHandler;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.sync.ConflictResolution;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Routes a single app-state-sync action class through Cobalt's sync pipeline.
 *
 * <p>Each implementation adapts one WhatsApp Web sync module which describes how mutations of a
 * single action type are validated, applied to the local store, conflict-resolved against pending
 * outgoing mutations, and (for the message-oriented actions) re-keyed into the
 * {@link com.github.auties00.cobalt.wire.core.message.MessageKey} space. The dispatcher selects the
 * right handler via {@link #actionName()} and reads {@link #collectionName()} / {@link #version()}
 * as routing metadata for patch decoding and version gating. The framework selects the right
 * implementation when an incoming sync patch reaches the dispatch loop; this surface is documented
 * for contributors adding a new handler for an action that the server ships but Cobalt has not yet
 * wired up.
 *
 * <p>This interface flattens WA Web's five-class category hierarchy and the shared
 * {@code SyncdAction} root that all five extend. The {@code is*SyncdAction()} /
 * {@code as*SyncdActionHandler()} discrimination pattern is intentionally dropped because Cobalt's
 * dispatcher reaches each handler through the {@link #actionName()} key directly and never needs to
 * downcast.
 */
@WhatsAppWebModule(moduleName = "WAWebSyncdAction")
public interface WebAppStateActionHandler {
    /**
     * Returns the action identifier this handler claims.
     *
     * <p>The dispatch loop reads this value when routing each decoded mutation; the returned string
     * is the canonical WA Web action name (for example {@code "archive"}, {@code "pin_v1"}, or
     * {@code "userStatusMute"}).
     *
     * @implSpec
     * Implementations must return the same value on every call and the value must match the WA Web
     * action constant; otherwise the dispatcher will route incoming mutations to the wrong handler.
     *
     * @return the canonical action identifier
     */
    String actionName();

    /**
     * Returns the {@link SyncPatchType} collection this handler reads from.
     *
     * <p>Each WA Web sync action lives in exactly one collection ({@code CRITICAL_BLOCK},
     * {@code CRITICAL_UNBLOCK_LOW}, {@code REGULAR_HIGH}, {@code REGULAR}, {@code REGULAR_LOW}); this
     * accessor exposes that constant for the patch decoder and the conflict lookup table.
     *
     * @implSpec
     * Implementations must return the same constant on every call and the value must match the
     * collection assignment in the corresponding WA Web sync module.
     *
     * @return the patch type the underlying action is stored under
     */
    SyncPatchType collectionName();

    /**
     * Returns the mutation format version this handler accepts.
     *
     * <p>The dispatcher consults this value before invoking
     * {@link #applyMutation(LinkedWhatsAppClient, DecryptedMutation.Trusted)}; mutations whose on-wire
     * version is greater than the handler's declared version are skipped so that an older client
     * cannot mis-interpret a newer payload shape.
     *
     * @implSpec
     * Implementations must return the same integer on every call and the value must match the
     * version of the corresponding WA Web module.
     *
     * @return the maximum mutation format version this handler can apply
     */
    int version();

    /**
     * Applies a single decoded mutation and reports the outcome.
     *
     * <p>This is the per-mutation entry point used by the default batch loop in
     * {@link #applyMutationBatch(LinkedWhatsAppClient, List)}; handlers that need batch-level
     * deduplication or single-mutation semantics override the batch entry point instead and may
     * delegate here.
     *
     * @implSpec
     * Implementations must classify the outcome as {@link MutationApplicationResult#unsupported()}
     * for non-{@code SET} operations they do not handle,
     * {@link MutationApplicationResult#malformed()} for missing or unparseable index/value
     * components, {@link MutationApplicationResult#orphan()} for mutations referencing an unknown
     * entity, and {@link MutationApplicationResult#success()} after the local store has been
     * updated. Exceptions thrown from this method propagate to the configured
     * {@link WhatsAppLinkedClientErrorHandler} rather than being mapped
     * to a sentinel {@link MutationApplicationResult} as WA Web does.
     *
     * @param client   the {@link LinkedWhatsAppClient} whose store the mutation should be applied to
     * @param mutation the decoded, trusted mutation to apply
     * @return the detailed application outcome
     */
    MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation);

    /**
     * Applies a batch of decoded mutations and returns one result per input.
     *
     * <p>Handlers that need cross-mutation logic (last-write-wins deduplication in
     * {@link SettingsSyncHandler}, "exactly one mutation" enforcement in
     * {@link StatusPrivacyHandler}, last-mutation-only application in
     * {@link UnarchiveChatsSettingHandler}, batch-wide latest-timestamp tracking in
     * {@link WaffleAccountLinkStateHandler}) override this method. Handlers with purely per-mutation
     * semantics inherit the default loop.
     *
     * @implSpec
     * The returned list must have the same size as the input and the result at index {@code i} must
     * correspond to the mutation at index {@code i} of the input; overriders that skip earlier
     * mutations must still emit a placeholder ({@link MutationApplicationResult#skipped()} or
     * equivalent) for every input entry so the dispatcher can correlate outcomes with mutations by
     * position.
     *
     * @implNote
     * This implementation walks the input list sequentially on the calling virtual thread,
     * delegating each mutation to {@link #applyMutation(LinkedWhatsAppClient, DecryptedMutation.Trusted)}.
     * WA Web's {@code Promise.all} parallelism collapses to sequential blocking calls because
     * Cobalt's sync pipeline already runs on a virtual thread per patch.
     *
     * @param client    the {@link LinkedWhatsAppClient} whose store the mutations should be applied to
     * @param mutations the decoded, trusted mutations to apply
     * @return the per-mutation outcomes, in the same order as {@code mutations}
     */
    default List<MutationApplicationResult> applyMutationBatch(LinkedWhatsAppClient client, List<DecryptedMutation.Trusted> mutations) {
        var results = new ArrayList<MutationApplicationResult>(mutations.size());
        for (var mutation : mutations) {
            results.add(applyMutation(client, mutation));
        }
        return results;
    }

    /**
     * Resolves the collision between a local pending mutation and an incoming remote mutation
     * sharing the same index.
     *
     * <p>Invoked during sync patch processing whenever an incoming mutation collides with an
     * outgoing one that has not yet been acknowledged by the server; handlers may override to merge
     * the two (for example for mark-as-read message-range actions) but the default timestamp
     * tiebreaker is correct for the vast majority of actions.
     *
     * @implSpec
     * Implementations must return a {@link ConflictResolution} whose
     * {@link ConflictResolution#state()} is one of
     * {@link MutationConflictResolutionState#APPLY_REMOTE_DROP_LOCAL} (remote wins),
     * {@link MutationConflictResolutionState#SKIP_REMOTE} (local wins), or
     * {@link MutationConflictResolutionState#SKIP_REMOTE_DROP_LOCAL} (merge); the optional merged mutation
     * in the resolution is consulted by the dispatcher when the state indicates a merge.
     *
     * @implNote
     * This implementation lets the incoming remote mutation win when its timestamp is greater than
     * or equal to the local timestamp; otherwise the remote is dropped.
     *
     * @param localMutation  the local pending mutation
     * @param remoteMutation the incoming remote mutation
     * @return the conflict resolution indicating which mutation to keep
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdAction", exports = "AccountSyncdActionBase", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebSyncdAction", exports = "ChatSyncdActionBase", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebSyncdAction", exports = "ChatOrContactSyncdActionBase", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebSyncdAction", exports = "MessageSyncdActionBase", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebSyncdAction", exports = "ChatMessageRangeSyncdActionBase", adaptation = WhatsAppAdaptation.DIRECT)
    default ConflictResolution resolveConflicts(DecryptedMutation.Trusted localMutation, DecryptedMutation.Trusted remoteMutation) {
        if (remoteMutation.timestamp().compareTo(localMutation.timestamp()) >= 0) {
            return ConflictResolution.of(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL);
        } else {
            return ConflictResolution.of(MutationConflictResolutionState.SKIP_REMOTE);
        }
    }

    /**
     * Decides whether a remote mutation should be dropped because a different pending local mutation
     * at a different index already supersedes it.
     *
     * <p>This hook handles cross-index dependencies (for example an outgoing delete-chat for chat A
     * pending while an incoming archive for chat A arrives); handlers that need such logic override
     * this method, while handlers with no cross-index coupling accept the default of "never drop".
     *
     * @implSpec
     * Implementations must return {@code true} only when there is a pending local mutation whose
     * presence makes applying the remote mutation incorrect or wasteful; the {@code pendingByIndex}
     * map is keyed by mutation index so handlers can probe specific cousins by computing the
     * relevant index strings.
     *
     * @implNote
     * This implementation always returns {@code false}, matching WA Web's default behaviour.
     *
     * @param remoteMutation the candidate remote mutation
     * @param pendingByIndex  all pending local mutations keyed by their index
     * @return {@code true} if the remote mutation should be dropped, {@code false} otherwise
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdAction", exports = "AccountSyncdActionBase", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebSyncdAction", exports = "ChatSyncdActionBase", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebSyncdAction", exports = "ChatOrContactSyncdActionBase", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebSyncdAction", exports = "MessageSyncdActionBase", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebSyncdAction", exports = "ChatMessageRangeSyncdActionBase", adaptation = WhatsAppAdaptation.DIRECT)
    default boolean dropMutationDueToCrossIndexConflict(
            DecryptedMutation.Trusted remoteMutation,
            Map<String, DecryptedMutation.Trusted> pendingByIndex
    ) {
        return false;
    }
}
