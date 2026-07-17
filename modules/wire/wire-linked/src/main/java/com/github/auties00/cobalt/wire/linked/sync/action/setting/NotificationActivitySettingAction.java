package com.github.auties00.cobalt.wire.linked.sync.action.setting;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents the user's notification activity preference, synchronised
 * across linked devices.
 *
 * <p>This setting controls which conversations trigger notifications:
 * either all messages or only highlighted (for example mentioned)
 * messages. Two additional {@code DEFAULT_*} values are used by clients
 * to indicate that the selection was not explicitly set and the
 * platform-specific default should be applied.
 */
@ProtobufMessage(name = "SyncActionValue.NotificationActivitySettingAction")
public final class NotificationActivitySettingAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name used when this setting is encoded in an app
     * state sync mutation.
     */
    public static final String ACTION_NAME = "notificationActivitySetting";

    /**
     * Canonical action version emitted alongside this setting when it is
     * published to the app state sync collection.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * Returns the canonical action name for this setting.
     *
     * @return the constant {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the canonical action version for this setting.
     *
     * @return the constant {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }

    /**
     * The selected notification activity preference.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    NotificationActivitySetting notificationActivitySetting;


    /**
     * Constructs a new notification activity setting action.
     *
     * @param notificationActivitySetting the selected preference, or
     *                                    {@code null} if not set
     */
    NotificationActivitySettingAction(NotificationActivitySetting notificationActivitySetting) {
        this.notificationActivitySetting = notificationActivitySetting;
    }

    /**
     * Returns the currently selected notification activity preference.
     *
     * @return an {@link Optional} containing the selected
     *         {@link NotificationActivitySetting}, or empty if the setting
     *         has never been written
     */
    public Optional<NotificationActivitySetting> notificationActivitySetting() {
        return Optional.ofNullable(notificationActivitySetting);
    }

    /**
     * Updates the notification activity preference.
     *
     * @param notificationActivitySetting the new preference, or {@code null}
     *                                    to clear the value
     */
    public void setNotificationActivitySetting(NotificationActivitySetting notificationActivitySetting) {
        this.notificationActivitySetting = notificationActivitySetting;
    }

    /**
     * Enumeration of the possible notification activity modes.
     *
     * <p>The {@code DEFAULT_*} variants indicate that the user has not made
     * an explicit choice and the platform default should apply; the plain
     * variants denote an explicit user selection.
     */
    @ProtobufEnum(name = "SyncActionValue.NotificationActivitySettingAction.NotificationActivitySetting")
    public static enum NotificationActivitySetting {
        /**
         * Default mode that notifies for all messages.
         *
         * <p>Emitted by clients when the user has not explicitly chosen a
         * value; applications should treat this as the "all messages"
         * platform default.
         */
        DEFAULT_ALL_MESSAGES(0),

        /**
         * Explicit user selection to notify for every message.
         */
        ALL_MESSAGES(1),

        /**
         * Explicit user selection to notify only for highlighted messages
         * such as direct mentions or replies.
         */
        HIGHLIGHTS(2),

        /**
         * Default mode that notifies only for highlighted messages.
         *
         * <p>Emitted by clients when the user has not explicitly chosen a
         * value and the platform default is the highlights-only mode.
         */
        DEFAULT_HIGHLIGHTS(3);

        /**
         * Constructs a mode with the given protobuf index.
         *
         * @param index the wire-level protobuf enum index
         */
        NotificationActivitySetting(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Wire-level protobuf enum index for this mode.
         */
        final int index;

        /**
         * Returns the wire-level protobuf enum index for this mode.
         *
         * @return the protobuf index associated with this enum constant
         */
        public int index() {
            return this.index;
        }
    }
}
