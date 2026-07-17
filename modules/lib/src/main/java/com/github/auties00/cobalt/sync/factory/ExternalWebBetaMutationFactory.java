package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.device.ExternalWebBetaAction;
import com.github.auties00.cobalt.wire.linked.sync.action.device.ExternalWebBetaActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing app-state mutations that toggle enrolment in the WhatsApp Web and Desktop beta
 * program.
 *
 * Drives the beta opt-in switch exposed on every linked Web and Desktop installation; the same JS
 * bundle backs both surfaces so the mutation flips the flag for both at once. This factory is the
 * outgoing-mutation counterpart of
 * {@link com.github.auties00.cobalt.sync.handler.ExternalWebBetaHandler}, which updates the local
 * opt-in flag on receiving devices.
 *
 * @implNote
 * This implementation mirrors {@code WAWebExternalBetaOptInAction.setOptInBetaAction}, the
 * programmatic entry point WA Web exposes for the toggle. The mutation is a singleton
 * ({@code [external_web_beta]}); each new mutation overwrites the previous opt-in value rather than
 * appending a new one.
 */
public final class ExternalWebBetaMutationFactory {
    /**
     * The logger for {@link ExternalWebBetaMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(ExternalWebBetaMutationFactory.class);

    /**
     * Constructs an external-web-beta mutation factory.
     *
     * The factory is stateless, so a single instance may be shared across the lifetime of the
     * client.
     */
    public ExternalWebBetaMutationFactory() {

    }

    /**
     * Builds a pending SET mutation that toggles the beta opt-in flag.
     *
     * The index carries only the action name because the preference is a singleton per account.
     *
     * @implNote
     * This implementation captures the timestamp via {@link Instant#now()}; WA Web's
     * {@code setOptInBetaAction} uses {@code WATimeUtils.unixTimeMs()} for the same purpose.
     *
     * @param enrolled {@code true} to opt the account into the beta program, {@code false} to opt out
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebExternalBetaOptInAction", exports = "setOptInBetaAction", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getExternalWebBetaMutation(boolean enrolled) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building external web beta mutation enrolled={0}", enrolled);
        var timestamp = Instant.now();
        var action = new ExternalWebBetaActionBuilder()
                .isOptIn(enrolled)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .externalWebBetaAction(action)
                .build();
        var index = JSON.toJSONString(List.of(ExternalWebBetaAction.ACTION_NAME));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                ExternalWebBetaAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
