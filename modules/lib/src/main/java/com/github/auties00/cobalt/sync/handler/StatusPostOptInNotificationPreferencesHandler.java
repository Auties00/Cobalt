package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.device.StatusPostOptInNotificationPreferencesAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Mirrors the user's opt-in choice for status-post notifications across linked
 * devices.
 *
 * <p>The sync dispatcher would route incoming
 * {@code "status_post_opt_in_notification_preferences_action"} mutations here if
 * the server ever emits one. The handler persists the boolean opt-in flag on
 * {@link LinkedWhatsAppStore} so notification delivery
 * for status posts respects the user's last choice.
 *
 * @implNote
 * This implementation is forward-looking. The
 * {@code SyncActionValue.StatusPostOptInNotificationPreferencesAction} protobuf
 * is declared in {@code WAWebProtobufSyncAction.pb} (field 71, collection
 * {@code REGULAR_HIGH}, single optional {@code enabled: bool} at index 1), but
 * the current WA Web snapshot does not ship a
 * {@code WAWebStatusPostOptInNotificationPreferencesSync} module and the action
 * is absent from {@code WAWebCollectionHandlerActions.ActionHandlers}, so WA
 * Web's dispatcher would currently skip any incoming mutation.
 */
public final class StatusPostOptInNotificationPreferencesHandler implements WebAppStateActionHandler {
    /**
     * The singleton instance held by the sync registry.
     */
    public static final StatusPostOptInNotificationPreferencesHandler INSTANCE =
            new StatusPostOptInNotificationPreferencesHandler();

    /**
     * The logger for {@link StatusPostOptInNotificationPreferencesHandler}.
     */
    private static final System.Logger LOGGER = Log.get(StatusPostOptInNotificationPreferencesHandler.class);

    /**
     * Constructs the singleton.
     *
     * <p>Callers obtain the handler via {@link #INSTANCE}; the constructor is
     * private so the registry cannot accidentally instantiate it twice.
     */
    private StatusPostOptInNotificationPreferencesHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String actionName() {
        return StatusPostOptInNotificationPreferencesAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns {@link SyncPatchType#REGULAR_HIGH} as inferred
     * from {@code WAWebProtobufSyncAction.pb}'s
     * {@code STATUS_POST_OPT_IN_NOTIFICATION_PREFERENCES_ACTION -> REGULAR_HIGH}
     * collection-router branch.
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
        return StatusPostOptInNotificationPreferencesAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A non-{@link SyncdOperation#SET} operation is reported as
     * {@link MutationApplicationResult#unsupported()}, a value that does not decode
     * to a {@link StatusPostOptInNotificationPreferencesAction} is reported as
     * malformed, and the {@code enabled} field is otherwise written to
     * {@link LinkedWhatsAppSettingsStore#setStatusPostOptInNotificationPreferencesEnabled(Boolean)}.
     *
     * @implNote
     * This implementation follows the canonical single-boolean-payload shape used
     * by sibling handlers because WA Web ships no concrete sync module for this
     * action; the shape is inferred from the protobuf schema and a sibling handler.
     */
    @Override
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof StatusPostOptInNotificationPreferencesAction action)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "status post opt-in notification preferences: malformed mutation value");
            return MutationApplicationResult.malformed();
        }

        client.store().settingsStore().setStatusPostOptInNotificationPreferencesEnabled(action.enabled());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "status post opt-in notification preferences: set enabled={0}", action.enabled());
        return MutationApplicationResult.success();
    }
}
