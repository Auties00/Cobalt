package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.DetectedOutcomesStatusAction;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.DetectedOutcomesStatusActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing app-state mutations that toggle whether automated message and contact detections
 * (spam, flagged-account warnings) are mirrored across linked devices.
 *
 * Drives the Click-To-WhatsApp detected-outcomes onboarding switch on the settings surface; the
 * receive side forwards the boolean to {@code WAWebUserPrefsDetectedOutcomes} so subsequent
 * classifications surface (or stop surfacing) on every companion. This factory is the
 * outgoing-mutation counterpart of
 * {@link com.github.auties00.cobalt.sync.handler.DetectedOutcomesStatusHandler}.
 *
 * @implNote
 * This implementation mirrors {@code WAWebCTWADetectedOutcomeOnboardingStatusUpdateAction}, the
 * dispatcher WA Web invokes from {@code WAWebCTWABridgeApi.ctwaDetectedOutcomeOnboardingStatusUpdate}.
 */
public final class DetectedOutcomesStatusMutationFactory {
    /**
     * The logger for {@link DetectedOutcomesStatusMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(DetectedOutcomesStatusMutationFactory.class);

    /**
     * Constructs a detected-outcomes-status mutation factory.
     *
     * The factory is stateless, so a single instance may be shared across the lifetime of the
     * client.
     */
    public DetectedOutcomesStatusMutationFactory() {

    }

    /**
     * Builds a pending SET mutation that toggles the detected-outcomes status flag.
     *
     * The index carries only the action name because the preference is a singleton per account.
     *
     * @implNote
     * This implementation captures the timestamp via {@link Instant#now()}; WA Web's
     * {@code ctwaDetectedOutcomeOnboardingStatusUpdateAction} uses {@code WATimeUtils.unixTimeMs()}
     * for the same purpose.
     *
     * @param enabled {@code true} to enable cross-device sync of automated detections, {@code false} to disable it
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebCTWADetectedOutcomeOnboardingStatusUpdateAction", exports = "ctwaDetectedOutcomeOnboardingStatusUpdateAction", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getDetectedOutcomesStatusMutation(boolean enabled) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building detected outcomes status mutation enabled={0}", enabled);
        var timestamp = Instant.now();
        var action = new DetectedOutcomesStatusActionBuilder()
                .isEnabled(enabled)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .detectedOutcomesStatusAction(action)
                .build();
        var index = JSON.toJSONString(List.of(DetectedOutcomesStatusAction.ACTION_NAME));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                DetectedOutcomesStatusAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
