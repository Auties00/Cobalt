package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivacySettingRelayAllCalls;
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivacySettingRelayAllCallsBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing VoIP relay-all-calls sync mutations.
 *
 * <p>This factory backs the "always relay calls" privacy toggle; relayed calls go through WhatsApp
 * infrastructure rather than peer-to-peer, masking the caller's network identifiers from the
 * callee. Mutations produced here are consumed on receiving devices by
 * {@link com.github.auties00.cobalt.sync.handler.VoipRelayAllCallsHandler}.
 *
 * @implNote
 * The wrapping send step that WA Web exposes goes through a sync lock which Cobalt does not run at
 * this layer.
 */
public final class VoipRelayAllCallsMutationFactory {
    /**
     * The logger for {@link VoipRelayAllCallsMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(VoipRelayAllCallsMutationFactory.class);

    /**
     * Constructs a VoIP relay-all-calls mutation factory.
     *
     * <p>The factory keeps no state, so a single instance is sufficient per client.
     */
    public VoipRelayAllCallsMutationFactory() {

    }

    /**
     * Builds a pending SET mutation for the VoIP relay-all-calls setting.
     *
     * <p>Receiving devices interpret the {@code isEnabled} flag as "disallow all peer-to-peer call
     * routing" and route subsequent calls exclusively through WhatsApp relays. The index carries
     * only the action name because the preference is a singleton per account.
     *
     * @implNote
     * This implementation models the {@code SyncActionValue.privacySettingRelayAllCalls} protobuf
     * shape as used by WA Web's generic pending-mutation builder; the mutation is routed through the
     * {@code Regular} collection with version {@code 1}.
     *
     * @param timestamp the mutation timestamp recorded on both the outer mutation and the inner
     *                  {@code SyncActionValue}
     * @param isEnabled {@code true} to disallow peer-to-peer call routing (force relay),
     *                  {@code false} to allow peer-to-peer routing
     * @return the pending mutation ready for sync upload
     */
    @WhatsAppWebExport(moduleName = "WAWebVoipRelayAllCallsSettingSync", exports = "getMutation", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPendingMutation getVoipRelayAllCallsMutation(Instant timestamp, boolean isEnabled) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building voip relay-all-calls mutation, isEnabled={0}", isEnabled);
        var action = new PrivacySettingRelayAllCallsBuilder()
                .isEnabled(isEnabled)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .privacySettingRelayAllCalls(action)
                .build();
        var index = JSON.toJSONString(List.of(PrivacySettingRelayAllCalls.ACTION_NAME));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                PrivacySettingRelayAllCalls.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
