package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.PaymentInfoAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Applies the {@code payment_info} app-state action that distributes the
 * order-details Customer Payment Instructions (CPI) string to linked SMB
 * devices.
 *
 * <p>The CPI string is the formatted block of payment instructions a business
 * attaches to outgoing order details so the buyer can complete payment out of
 * band. The action is gated on SMB platform AND
 * {@link ABProp#ORDER_DETAILS_PAYMENT_INSTRUCTIONS_SYNC_ENABLED}; non-SMB or
 * AB-prop-disabled accounts surface every mutation as
 * {@link MutationApplicationResult#unsupported()}. The mutation index is the
 * singleton {@snippet :
 *     ["payment_info"]
 * }
 *
 * <p>Only {@link SyncdOperation#SET} is accepted and the resolved CPI string
 * is written to the store; a missing CPI string surfaces as
 * {@link SyncdIndexUtils#malformedActionValue(String)}.
 *
 * @implNote
 * This implementation collapses WA Web's
 * {@code setCPIInfo} bridge chain (which diff-checks against the current value
 * and emits a change event) into a single
 * {@code LinkedWhatsAppStore.setPaymentInstructionCpi} call: there is no UI consumer
 * to dispatch the change event to, and the diff check is a UI-render
 * optimisation with no behavioural side effect. The per-batch {@code WARN}
 * counters are dropped.
 */
@WhatsAppWebModule(moduleName = "WAWebPaymentInfoSync")
public final class PaymentInfoHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link PaymentInfoHandler}.
     */
    private static final System.Logger LOGGER = Log.get(PaymentInfoHandler.class);

    /**
     * Holds the AB-props service consulted before applying any mutation.
     */
    private final ABPropsService abPropsService;

    /**
     * Constructs the payment-info sync handler bound to the given AB-props
     * service.
     *
     * @param abPropsService the AB-props service consulted on every mutation
     */
    @WhatsAppWebExport(moduleName = "WAWebPaymentInfoSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public PaymentInfoHandler(ABPropsService abPropsService) {
        this.abPropsService = abPropsService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPaymentInfoSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return PaymentInfoAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPaymentInfoSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return PaymentInfoAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPaymentInfoSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return PaymentInfoAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation gates each mutation on, in order:
     * {@link ClientPlatformType#IOS_BUSINESS} or
     * {@link ClientPlatformType#ANDROID_BUSINESS} (mirroring WA Web's
     * {@code WAWebMobilePlatforms.isSMB}); then
     * {@link ABProp#ORDER_DETAILS_PAYMENT_INSTRUCTIONS_SYNC_ENABLED}; then
     * {@link SyncdOperation#SET}; then a non-{@code null}
     * {@link PaymentInfoAction#cpi()} string. Failures at the first three
     * layers surface as {@link MutationApplicationResult#unsupported()}; a
     * missing CPI string surfaces as
     * {@link SyncdIndexUtils#malformedActionValue(String)}. On success the
     * resolved CPI string is written via
     * {@code LinkedWhatsAppStore.setPaymentInstructionCpi}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPaymentInfoSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var platform = client.store().accountStore().device().platform();
        if (platform != ClientPlatformType.IOS_BUSINESS && platform != ClientPlatformType.ANDROID_BUSINESS) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "payment info: unsupported, platform {0} is not smb", platform);
            return MutationApplicationResult.unsupported();
        }

        if (!abPropsService.getBool(ABProp.ORDER_DETAILS_PAYMENT_INSTRUCTIONS_SYNC_ENABLED)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "payment info: unsupported, cpi sync ab-prop disabled");
            return MutationApplicationResult.unsupported();
        }

        if (mutation.operation() != SyncdOperation.SET) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "payment info: unsupported operation {0}", mutation.operation());
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof PaymentInfoAction action)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "payment info mutation malformed: missing action value");
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        var cpi = action.cpi().orElse(null);
        if (cpi == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "payment info mutation malformed: missing cpi string");
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        client.store().businessStore().setPaymentInstructionCpi(cpi);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "payment info: cpi updated, length={0}", cpi.length());
        return MutationApplicationResult.success();
    }
}
