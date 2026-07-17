package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorHandler;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.NotificationActivitySettingAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Applies the {@code notificationActivitySetting} app-state action that
 * carries the user's per-account notification activity preference.
 *
 * <p>This handler persists one of
 * {@link NotificationActivitySettingAction.NotificationActivitySetting}'s
 * values on {@link LinkedWhatsAppStore} so the
 * notification activity preference fans out across linked devices. Only
 * {@link SyncdOperation#SET} is accepted; any other operation is reported as
 * {@link MutationApplicationResult#unsupported()} and a missing or wrong-typed
 * value as {@link MutationApplicationResult#malformed()}.
 *
 * @implNote
 * This implementation has no WA Web counterpart: the
 * {@code SyncActionValue.NotificationActivitySettingAction} protobuf is
 * declared in the sync-action protobuf module but no
 * {@code WAWebNotificationActivitySettingSync} module ships, and the action is
 * absent from the WA Web action-handler table, so WA Web silently drops any
 * incoming mutation. The handler exists in Cobalt as a forward-looking
 * implementation; the {@link SyncPatchType#REGULAR} collection is taken
 * directly from the inline router in the WA Web sync-action protobuf module,
 * and the version and apply path are inferred from sibling settings handlers.
 */
public final class NotificationActivitySettingHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link NotificationActivitySettingHandler}.
     */
    private static final System.Logger LOGGER = Log.get(NotificationActivitySettingHandler.class);

    /**
     * Constructs a stateless {@link NotificationActivitySettingHandler} for
     * registration in the sync handler registry.
     */
    public NotificationActivitySettingHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String actionName() {
        return NotificationActivitySettingAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns {@link SyncPatchType#REGULAR} as declared by
     * the inline router in the WA Web sync-action protobuf module.
     */
    @Override
    public SyncPatchType collectionName() {
        return SyncPatchType.REGULAR;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns
     * {@link NotificationActivitySettingAction#ACTION_VERSION}; WA Web has no
     * concrete handler so there is no {@code getVersion()} to mirror, and the
     * declared value is the forward-looking default for a single-field
     * protobuf.
     */
    @Override
    public int version() {
        return NotificationActivitySettingAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Only {@link SyncdOperation#SET} is accepted. A missing or empty
     * {@link NotificationActivitySettingAction#notificationActivitySetting()}
     * is rejected as {@link MutationApplicationResult#malformed()}; on success
     * the resolved enum is written via
     * {@link LinkedWhatsAppSettingsStore#setNotificationActivitySetting(NotificationActivitySettingAction.NotificationActivitySetting)}.
     *
     * @implNote
     * This implementation lets exceptions propagate so the configured
     * {@link WhatsAppLinkedClientErrorHandler}
     * decides recovery, where WA Web sibling handlers wrap the body in a
     * try/catch returning a failed sentinel.
     */
    @Override
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            if (Log.DEBUG)
                LOGGER.log(Level.DEBUG, "notification activity setting: unsupported operation {0}", mutation.operation());
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof NotificationActivitySettingAction action)
                || action.notificationActivitySetting().isEmpty()) {
            if (Log.WARNING)
                LOGGER.log(Level.WARNING, "notification activity setting mutation malformed: missing value");
            return MutationApplicationResult.malformed();
        }

        client.store().settingsStore().setNotificationActivitySetting(action.notificationActivitySetting().get());
        if (Log.DEBUG)
            LOGGER.log(Level.DEBUG, "notification activity setting: updated to {0}", action.notificationActivitySetting().get());

        return MutationApplicationResult.success();
    }

}
