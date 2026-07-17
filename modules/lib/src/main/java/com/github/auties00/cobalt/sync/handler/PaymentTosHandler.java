package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.PaymentTosAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Applies the {@code payment_tos} app-state action that records acceptance of
 * the Brazil Pix Terms of Service across linked SMB devices.
 *
 * <p>When the business owner accepts the Pix payment ToS on one SMB device the
 * acceptance fans out so the other paired SMB devices can render the Pix flow
 * without prompting again. The action is gated on SMB platform AND
 * {@link ABProp#PAYMENTS_BR_PIX_ON_WEB}; non-SMB or AB-prop-disabled accounts
 * surface every mutation as {@link MutationApplicationResult#unsupported()}.
 * The mutation index is the singleton {@snippet :
 *     ["payment_tos"]
 * }
 *
 * <p>Only {@link SyncdOperation#SET} is accepted and the resolved action is
 * written to the store; a missing {@link PaymentTosAction} payload surfaces as
 * {@link SyncdIndexUtils#malformedActionValue(String)}.
 *
 * @implNote
 * This implementation collapses WA Web's
 * {@code WAWebUserPrefsPaymentTos.setPaymentTos} call into a single
 * {@code LinkedWhatsAppStore.setPaymentTos} write; the per-batch {@code WARN}
 * counters are dropped.
 */
@WhatsAppWebModule(moduleName = "WAWebPaymentTosSync")
public final class PaymentTosHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link PaymentTosHandler}.
     */
    private static final System.Logger LOGGER = Log.get(PaymentTosHandler.class);

    /**
     * Holds the AB-props service consulted before applying any mutation.
     */
    private final ABPropsService abPropsService;

    /**
     * Constructs the payment-ToS sync handler bound to the given AB-props
     * service.
     *
     * @param abPropsService the AB-props service consulted on every mutation
     */
    @WhatsAppWebExport(moduleName = "WAWebPaymentTosSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public PaymentTosHandler(ABPropsService abPropsService) {
        this.abPropsService = abPropsService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPaymentTosSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return PaymentTosAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPaymentTosSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return PaymentTosAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPaymentTosSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return PaymentTosAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation gates each mutation on, in order:
     * {@link ClientPlatformType#IOS_BUSINESS} or
     * {@link ClientPlatformType#ANDROID_BUSINESS} (mirroring WA Web's
     * {@code WAWebMobilePlatforms.isSMB}); then
     * {@link ABProp#PAYMENTS_BR_PIX_ON_WEB}; then {@link SyncdOperation#SET};
     * then a non-{@code null} {@link PaymentTosAction} payload. Failures at the
     * first three layers surface as
     * {@link MutationApplicationResult#unsupported()}; a missing payload
     * surfaces as {@link SyncdIndexUtils#malformedActionValue(String)}. On
     * success the resolved action is written via
     * {@code LinkedWhatsAppStore.setPaymentTos}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPaymentTosSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var platform = client.store().accountStore().device().platform();
        if (platform != ClientPlatformType.IOS_BUSINESS && platform != ClientPlatformType.ANDROID_BUSINESS) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "payment tos: unsupported, platform {0} is not smb", platform);
            return MutationApplicationResult.unsupported();
        }

        if (!abPropsService.getBool(ABProp.PAYMENTS_BR_PIX_ON_WEB)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "payment tos: unsupported, pix ab-prop disabled");
            return MutationApplicationResult.unsupported();
        }

        if (mutation.operation() != SyncdOperation.SET) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "payment tos: unsupported operation {0}", mutation.operation());
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof PaymentTosAction action)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "payment tos mutation malformed: missing action value");
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        client.store().businessStore().setPaymentTos(action);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "payment tos: acceptance recorded");
        return MutationApplicationResult.success();
    }

}
