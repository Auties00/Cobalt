package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivateProcessingSettingAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Applies the {@code private_processing_setting} app-state action that
 * distributes the user's private-processing preference across linked devices.
 *
 * <p>Persists a single
 * {@link PrivateProcessingSettingAction.PrivateProcessingStatus} value (one of
 * {@link PrivateProcessingSettingAction.PrivateProcessingStatus#UNDEFINED},
 * {@link PrivateProcessingSettingAction.PrivateProcessingStatus#ENABLED},
 * {@link PrivateProcessingSettingAction.PrivateProcessingStatus#DISABLED}) on
 * {@link LinkedWhatsAppStore} so the
 * private-processing toggle stays consistent across paired devices. Only
 * {@link SyncdOperation#SET} is accepted; any other operation is reported as
 * {@link MutationApplicationResult#unsupported()} and a missing or unparseable
 * enum as {@link MutationApplicationResult#malformed()}.
 *
 * @implNote
 * This implementation has no WA Web counterpart: the
 * {@code SyncActionValue.PrivateProcessingSettingAction} protobuf (action
 * index 74) is declared in {@code WAWebProtobufSyncAction.pb} but no
 * {@code WAWebPrivateProcessingSettingSync} module ships and the action is
 * absent from {@code WAWebCollectionHandlerActions.ActionHandlers}, so WA Web
 * silently drops any incoming mutation. The {@link SyncPatchType#REGULAR_HIGH}
 * collection is inferred from sibling preference-style handlers; the version
 * and apply path are inferred from the protobuf shape (single mandatory enum).
 */
public final class PrivateProcessingSettingHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link PrivateProcessingSettingHandler}.
     */
    private static final System.Logger LOGGER = Log.get(PrivateProcessingSettingHandler.class);

    /**
     * Constructs the private processing setting sync handler.
     *
     * @implNote
     * This implementation is stateless; the handler holds no AB-prop / store /
     * WAM dependency.
     */
    public PrivateProcessingSettingHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String actionName() {
        return PrivateProcessingSettingAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns {@link SyncPatchType#REGULAR_HIGH} as a
     * forward-looking default; sibling preference-style handlers use the same
     * collection and no WA Web handler module exists to consult.
     */
    @Override
    public SyncPatchType collectionName() {
        return SyncPatchType.REGULAR_HIGH;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int version() {
        return PrivateProcessingSettingAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation accepts only {@link SyncdOperation#SET}: the action
     * carries a single mandatory enum and there is no semantic for
     * {@code REMOVE}. A missing or empty
     * {@link PrivateProcessingSettingAction#privateProcessingStatus()} is
     * rejected as {@link MutationApplicationResult#malformed()}; on success the
     * resolved enum is written via
     * {@code LinkedWhatsAppStore.setPrivateProcessingStatus}.
     */
    @Override
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof PrivateProcessingSettingAction action)
                || action.privateProcessingStatus().isEmpty()) {
            if (Log.WARNING)
                LOGGER.log(Level.WARNING, "private processing setting mutation malformed: missing status");
            return MutationApplicationResult.malformed();
        }

        client.store().settingsStore().setPrivateProcessingStatus(action.privateProcessingStatus().get());
        if (Log.DEBUG)
            LOGGER.log(Level.DEBUG, "private processing setting: status set to {0}", action.privateProcessingStatus().get());
        return MutationApplicationResult.success();
    }
}
