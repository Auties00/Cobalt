package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValue;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.PushNameSetting;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.PushNameSettingBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing push-name-setting sync mutations.
 *
 * <p>Backs the profile-name editor on the Settings profile surface. A single call produces one
 * {@link SyncPendingMutation} that propagates the new pushname to every linked device, where it
 * is consumed by {@link com.github.auties00.cobalt.sync.handler.PushNameSettingHandler}, which
 * applies the pushname locally and broadcasts a presence update.
 *
 * @implNote
 * This implementation mirrors {@code WAWebPushNameSync.getPushnameMutation} and routes the
 * mutation through the {@code CriticalBlock} collection with version {@code 1}, matching the WA
 * Web handler's behaviour where the pushname is part of the critical bootstrap data sync stage.
 */
public final class PushNameSettingMutationFactory {
    /**
     * The logger for {@link PushNameSettingMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(PushNameSettingMutationFactory.class);

    /**
     * Constructs a push-name-setting mutation factory.
     *
     * <p>The factory keeps no state, so a single instance is sufficient per client.
     */
    public PushNameSettingMutationFactory() {

    }

    /**
     * Builds a pending mutation that broadcasts a new pushname to other linked devices.
     *
     * <p>Receiving devices treat an empty pushname as the bootstrap-stage invalid marker and log
     * the corresponding invalid stage while still applying the change locally. The single WA Web
     * caller wraps this with a {@code lockForSync} transaction.
     *
     * @implNote
     * This implementation maps directly onto {@code WAWebPushNameSync.getPushnameMutation}; the
     * index carries only {@link PushNameSetting#ACTION_NAME} because the action is a singleton per
     * account and {@code indexArgs} is always empty.
     *
     * @param timestamp the mutation timestamp recorded on both the outer mutation and the inner
     *                  {@link SyncActionValue}
     * @param name      the new pushname to broadcast; may be {@code null} or empty to clear the
     *                  pushname (the receiver substitutes the empty string and logs the bootstrap
     *                  invalid stage in that case)
     * @return a pending mutation carrying the {@code setting_pushName} action
     */
    @WhatsAppWebExport(moduleName = "WAWebPushNameSync", exports = "getPushnameMutation", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPendingMutation getPushnameMutation(Instant timestamp, String name) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building pushname mutation nameLength={0}", name == null ? 0 : name.length());
        var setting = new PushNameSettingBuilder()
                .name(name)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .pushNameSetting(setting)
                .build();
        var index = JSON.toJSONString(List.of(PushNameSetting.ACTION_NAME));
        var pending = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                PushNameSetting.ACTION_VERSION
        );
        return new SyncPendingMutation(pending, 0);
    }
}
