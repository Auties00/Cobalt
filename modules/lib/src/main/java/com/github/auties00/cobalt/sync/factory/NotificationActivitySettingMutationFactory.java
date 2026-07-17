package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.NotificationActivitySettingAction;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.NotificationActivitySettingActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Builds outgoing notification-activity-setting sync mutations.
 *
 * <p>Backs the per-device notification activity preference on the Settings notifications surface.
 * A single call produces one {@link SyncPendingMutation} that is consumed on receiving devices by
 * {@link com.github.auties00.cobalt.sync.handler.NotificationActivitySettingHandler}.
 *
 * @implNote
 * This implementation has no dedicated WA Web counterpart module: the
 * {@code notificationActivitySetting} action is declared only as a protobuf shape in
 * {@code WAWebProtobufSyncAction.pb} with no {@code WAWebNotificationActivitySettingSync} module
 * in the current bundle. The shape follows {@code WAWebSyncdActionUtils.buildPendingMutation} as
 * used by every sibling {@code AccountSyncdActionBase} subclass.
 */
public final class NotificationActivitySettingMutationFactory {
    /**
     * The logger for {@link NotificationActivitySettingMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(NotificationActivitySettingMutationFactory.class);

    /**
     * Constructs a notification-activity-setting mutation factory.
     *
     * <p>The factory keeps no state, so a single instance is sufficient per client.
     */
    public NotificationActivitySettingMutationFactory() {

    }

    /**
     * Builds a pending {@code notificationActivitySetting} mutation carrying the given
     * notification activity preference.
     *
     * <p>Receiving devices store the
     * {@link NotificationActivitySettingAction.NotificationActivitySetting} value verbatim in
     * their local prefs. The index carries only the action name because the preference is a
     * singleton per account.
     *
     * @implNote
     * This implementation models the {@code SyncActionValue.notificationActivitySettingAction}
     * protobuf shape as used by {@code WAWebSyncdActionUtils.buildPendingMutation}; the mutation is
     * routed through the {@code Regular} collection alongside the other account-scoped settings.
     *
     * @param timestamp the mutation timestamp recorded on both the outer mutation and the inner
     *                  {@code SyncActionValue}
     * @param setting   the new {@link NotificationActivitySettingAction.NotificationActivitySetting}
     * @return a pending mutation carrying the {@code notificationActivitySetting} action
     * @throws NullPointerException if {@code timestamp} or {@code setting} is {@code null}
     */
    public SyncPendingMutation getNotificationActivityMutation(Instant timestamp, NotificationActivitySettingAction.NotificationActivitySetting setting) {
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        Objects.requireNonNull(setting, "setting cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building notification activity setting mutation setting={0}", setting);
        var action = new NotificationActivitySettingActionBuilder()
                .notificationActivitySetting(setting)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .notificationActivitySettingAction(action)
                .build();
        var index = JSON.toJSONString(List.of(NotificationActivitySettingAction.ACTION_NAME));
        var pending = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                NotificationActivitySettingAction.ACTION_VERSION
        );
        return new SyncPendingMutation(pending, 0);
    }
}
