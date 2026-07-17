package com.github.auties00.cobalt.wire.linked.sync.action.device;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Represents a sync action that toggles whether the account receives
 * notifications for status updates from contacts the user has explicitly opted
 * into.
 *
 * <p>When enabled, each linked device surfaces push notifications whenever a
 * followed contact posts a new status. The preference is replicated across
 * every device through the app state sync protocol so the user has a
 * consistent experience. The mutation is singleton, so the sync index is
 * composed solely of {@link #ACTION_NAME} with no trailing arguments.
 */
@ProtobufMessage(name = "SyncActionValue.StatusPostOptInNotificationPreferencesAction")
public final class StatusPostOptInNotificationPreferencesAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name used as the sole component of the singleton mutation
     * index for this action type.
     */
    public static final String ACTION_NAME = "status_post_opt_in_notification_preferences_action";

    /**
     * Schema version advertised by this action, used by sync handlers to gate
     * deserialisation and handling of newer payload shapes.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * Returns the canonical action name for every {@code StatusPostOptInNotificationPreferencesAction}.
     *
     * @return the constant {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version for every {@code StatusPostOptInNotificationPreferencesAction}.
     *
     * @return the constant {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Flag that enables push notifications for status updates from opted in
     * contacts.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean enabled;


    /**
     * Constructs a new {@code StatusPostOptInNotificationPreferencesAction} from
     * raw protobuf field values.
     *
     * @param enabled whether status post opt in notifications are enabled,
     *                possibly {@code null}
     */
    StatusPostOptInNotificationPreferencesAction(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns whether status post opt in notifications are enabled.
     *
     * @return {@code true} if notifications are enabled, {@code false} otherwise
     *         (including when the field was unset on the wire)
     */
    public boolean enabled() {
        return enabled != null && enabled;
    }

    /**
     * Sets the enabled flag for status post opt in notifications.
     *
     * @param enabled {@code true} to enable, {@code false} to disable, or
     *                {@code null} to clear
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
