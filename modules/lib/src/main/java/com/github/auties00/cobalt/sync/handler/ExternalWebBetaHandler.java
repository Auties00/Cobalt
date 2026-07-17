package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPayload;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValue;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.device.ExternalWebBetaAction;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSyncStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Applies the {@code external_web_beta} app-state sync action that toggles the
 * user's enrolment in the WhatsApp Web external beta programme.
 *
 * <p>The action carries a single boolean fanned out across the
 * {@link SyncPatchType#REGULAR} collection so every linked surface stops or
 * starts pulling beta builds. The handler is gated by the
 * {@link ABProp#EXTERNAL_BETA_CAN_JOIN} A/B prop; while the prop is off every
 * mutation is reported as {@link MutationApplicationResult#unsupported()}
 * regardless of payload.
 *
 * @implNote
 * This implementation persists the bit through
 * {@link LinkedWhatsAppSyncStore#setExternalWebBeta(boolean)} and mirrors it
 * onto the account release channel so the next handshake advertises the beta
 * channel while enrolled. It performs none of the backend restart, A/B-prop
 * refresh or telemetry that the build-channel negotiation would otherwise
 * trigger, because a Cobalt embedder does not negotiate its build channel
 * through Meta's update service.
 */
@WhatsAppWebModule(moduleName = "WAWebExternalWebBetaSync")
public final class ExternalWebBetaHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link ExternalWebBetaHandler}.
     */
    private static final System.Logger LOGGER = Log.get(ExternalWebBetaHandler.class);

    /**
     * The {@link ABPropsService} consulted before every mutation to gate the
     * handler on {@link ABProp#EXTERNAL_BETA_CAN_JOIN}.
     */
    private final ABPropsService abPropsService;

    /**
     * Constructs an {@link ExternalWebBetaHandler} bound to the given A/B-props
     * service.
     *
     * @param abPropsService the A/B-props service consulted on every mutation
     */
    @WhatsAppWebExport(moduleName = "WAWebExternalWebBetaSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public ExternalWebBetaHandler(ABPropsService abPropsService) {
        this.abPropsService = abPropsService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebExternalWebBetaSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return ExternalWebBetaAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebExternalWebBetaSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return ExternalWebBetaAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebExternalWebBetaSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return ExternalWebBetaAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads {@link ABProp#EXTERNAL_BETA_CAN_JOIN} first and short-circuits
     * the mutation as {@link MutationApplicationResult#unsupported()} when the
     * flag is off; non-{@link SyncdOperation#SET} operations are also reported
     * as unsupported and a missing or mistyped action payload yields a
     * malformed result. When the flag is on the value is persisted through
     * {@link LinkedWhatsAppSyncStore#setExternalWebBeta(boolean)} and mirrored
     * onto the account release channel.
     *
     * @implNote
     * This implementation re-reads the A/B prop on every mutation rather than
     * caching it, so a server-side prop flip reaches the next incoming sync
     * without restarting the client.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebExternalWebBetaSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (!abPropsService.getBool(ABProp.EXTERNAL_BETA_CAN_JOIN)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "external web beta: ab-prop disabled");
            return MutationApplicationResult.unsupported();
        }

        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(SyncActionValue::action).orElse(null) instanceof ExternalWebBetaAction action)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "external web beta: mutation value is not an ExternalWebBetaAction");
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        client.store().syncStore().setExternalWebBeta(action.isOptIn());
        client.store().accountStore().setReleaseChannel(action.isOptIn() ? ClientPayload.ClientReleaseChannel.BETA : ClientPayload.ClientReleaseChannel.RELEASE);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "external web beta: opt-in={0}", action.isOptIn());
        return MutationApplicationResult.success();
    }
}
