package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivateProcessingSettingAction;
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivateProcessingSettingActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing app-state mutations that toggle the Meta AI Private Processing preference.
 *
 * <p>Backs the Private Processing privacy switch exposed under chat settings: when enabled,
 * sensitive on-device AI computations may run for the linked account. Mutations produced here are
 * persisted into the user's prefs through the standard settings-bridge consumer pipeline so every
 * linked device converges on the same preference.
 *
 * @implNote
 * This implementation always emits an explicit
 * {@link PrivateProcessingSettingAction.PrivateProcessingStatus#ENABLED} or
 * {@link PrivateProcessingSettingAction.PrivateProcessingStatus#DISABLED} value; the
 * {@link PrivateProcessingSettingAction.PrivateProcessingStatus#UNDEFINED} sentinel that the
 * protobuf schema exposes is reserved for the "user has not yet expressed a preference" case and
 * is never produced by a client gesture.
 */
public final class PrivateProcessingSettingMutationFactory {
    /**
     * The logger for {@link PrivateProcessingSettingMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(PrivateProcessingSettingMutationFactory.class);

    /**
     * Constructs a private-processing mutation factory.
     *
     * <p>The factory is stateless; a single instance may be shared across the lifetime of the
     * client.
     */
    public PrivateProcessingSettingMutationFactory() {

    }

    /**
     * Builds a pending SET mutation for the Private Processing preference.
     *
     * <p>The index carries only the action name because the preference is a singleton per account.
     *
     * @implNote
     * This implementation maps the input boolean to the
     * {@link PrivateProcessingSettingAction.PrivateProcessingStatus} enum so the wire matches WA
     * Web's protobuf schema (field 74 in {@code WAWebProtobufSyncAction.pb},
     * {@code REGULAR_HIGH} priority).
     *
     * @param enabled {@code true} to enable Private Processing, {@code false} to disable it
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebSettingsBridgeApi", exports = "private_processing_setting", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getPrivateProcessingMutation(boolean enabled) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building private processing setting mutation enabled={0}", enabled);
        var timestamp = Instant.now();
        var status = enabled
                ? PrivateProcessingSettingAction.PrivateProcessingStatus.ENABLED
                : PrivateProcessingSettingAction.PrivateProcessingStatus.DISABLED;
        var action = new PrivateProcessingSettingActionBuilder()
                .privateProcessingStatus(status)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .privateProcessingSettingAction(action)
                .build();
        var index = JSON.toJSONString(List.of(PrivateProcessingSettingAction.ACTION_NAME));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                PrivateProcessingSettingAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
