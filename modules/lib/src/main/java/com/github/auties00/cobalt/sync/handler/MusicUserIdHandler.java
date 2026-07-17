package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.media.MusicUserIdAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Applies {@link MusicUserIdAction} sync mutations carrying the user's
 * per-provider music account identifiers.
 *
 * <p>This handler persists the user's resolved music identifiers on
 * {@link LinkedWhatsAppStore} so per-provider music
 * accounts are visible across linked devices. Only {@link SyncdOperation#SET}
 * is accepted; non-{@code SET} operations and payloads where both
 * {@link MusicUserIdAction#musicUserId()} and
 * {@link MusicUserIdAction#musicUserIdMap()} are empty are reported as
 * {@link MutationApplicationResult#unsupported()} and
 * {@link MutationApplicationResult#malformed()} respectively.
 *
 * @implNote
 * This implementation has no WA Web counterpart. The
 * {@code SyncActionValue.MusicUserIdAction} protobuf is declared in the
 * sync-action protobuf module, but no {@code WAWebMusicUserIdSync} module
 * ships in the current snapshot and the action is absent from the WA Web
 * action-handler table, so WA Web silently drops any incoming mutation. The
 * handler exists in Cobalt as a forward-looking implementation that follows
 * the same singleton plus {@code applyMutation} contract as every other
 * registered handler; the {@link SyncPatchType#REGULAR} collection and the
 * empty-payload-rejects-as-malformed shape are inferred from sibling
 * identifier-style handlers and from the protobuf shape itself.
 */
public final class MusicUserIdHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link MusicUserIdHandler}.
     */
    private static final System.Logger LOGGER = Log.get(MusicUserIdHandler.class);

    /**
     * Constructs a stateless {@link MusicUserIdHandler} for registration in
     * the sync handler registry.
     */
    public MusicUserIdHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String actionName() {
        return MusicUserIdAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns {@link SyncPatchType#REGULAR} as a
     * forward-looking default, because no WA Web handler module exists to
     * consult; sibling identifier-style handlers use the same collection.
     */
    @Override
    public SyncPatchType collectionName() {
        return SyncPatchType.REGULAR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int version() {
        return MusicUserIdAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Only {@link SyncdOperation#SET} is accepted. A mutation whose
     * resolved {@link MusicUserIdAction} has both
     * {@link MusicUserIdAction#musicUserId()} empty AND
     * {@link MusicUserIdAction#musicUserIdMap()} empty is rejected as
     * {@link MutationApplicationResult#malformed()}; on success the resolved
     * action is written via
     * {@link LinkedWhatsAppBusinessStore#setMusicUserIdState(MusicUserIdAction)}.
     *
     * @implNote
     * This implementation rejects the all-empty payload so a
     * default-constructed protobuf does not silently overwrite the stored
     * identifiers; the action carries a single string and/or a string-keyed
     * map and there is no semantic for {@code REMOVE}.
     */
    @Override
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            if (Log.DEBUG)
                LOGGER.log(Level.DEBUG, "music user id: unsupported operation {0}", mutation.operation());
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof MusicUserIdAction action)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "music user id mutation malformed: missing action value");
            return MutationApplicationResult.malformed();
        }

        if (action.musicUserId().isEmpty() && action.musicUserIdMap().isEmpty()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "music user id mutation malformed: empty identifiers");
            return MutationApplicationResult.malformed();
        }

        client.store().businessStore().setMusicUserIdState(action);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "music user id: state updated");
        return MutationApplicationResult.success();
    }
}
