package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.CustomPaymentMethodsAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Persists the SMB-seller-configured custom payment methods (Brazil PIX phase 1) from {@code custom_payment_methods} sync mutations.
 *
 * <p>This handler drives the SMB Brazil PIX seller surface where the merchant
 * can configure custom payment-method codes that are advertised to customers
 * in chat. When the merchant edits the methods on another SMB device, the
 * server replays the resulting {@link CustomPaymentMethodsAction} here, and the
 * methods become readable through
 * {@link LinkedWhatsAppBusinessStore#customPaymentMethods()}.
 *
 * @implNote
 * This implementation is gated on the device platform being SMB
 * ({@link ClientPlatformType#IOS_BUSINESS} or
 * {@link ClientPlatformType#ANDROID_BUSINESS}) and on
 * {@link ABProp#PAYMENTS_BR_PIX_PHASE_1_SELLER_SYNC_ENABLED} being
 * set; both gates short-circuit to
 * {@link MutationApplicationResult#unsupported()}. The WA Web
 * {@code setCustomPaymentMethods} fire-and-forget frontend event is
 * collapsed into a direct
 * {@link LinkedWhatsAppBusinessStore#setCustomPaymentMethods}
 * write because Cobalt has no browser frontend bridge.
 */
@WhatsAppWebModule(moduleName = "WAWebCustomPaymentMethodsSync")
public final class CustomPaymentMethodsHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link CustomPaymentMethodsHandler}.
     */
    private static final System.Logger LOGGER = Log.get(CustomPaymentMethodsHandler.class);

    /**
     * The {@link ABPropsService} consulted before applying any mutation.
     *
     * <p>Reads the {@link ABProp#PAYMENTS_BR_PIX_PHASE_1_SELLER_SYNC_ENABLED}
     * gate; when off every mutation in the batch resolves to
     * {@link MutationApplicationResult#unsupported()}.
     */
    private final ABPropsService abPropsService;

    /**
     * Constructs the custom-payment-methods handler with its AB-props dependency.
     *
     * <p>The sync handler registry instantiates this with the shared
     * {@link ABPropsService}.
     *
     * @param abPropsService the {@link ABPropsService} consulted on every mutation
     */
    @WhatsAppWebExport(moduleName = "WAWebCustomPaymentMethodsSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public CustomPaymentMethodsHandler(ABPropsService abPropsService) {
        this.abPropsService = abPropsService;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebCustomPaymentMethodsSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return CustomPaymentMethodsAction.ACTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebCustomPaymentMethodsSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return CustomPaymentMethodsAction.COLLECTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebCustomPaymentMethodsSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return CustomPaymentMethodsAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Validates SMB platform and AB-prop gating, then for SET mutations
     * writes the {@link CustomPaymentMethodsAction#customPaymentMethods()} list
     * into the store. Returns {@link MutationApplicationResult#unsupported()}
     * for non-SMB platforms, when the AB-prop is off, or for non-{@code SET}
     * operations; returns
     * {@link SyncdIndexUtils#malformedActionValue(String)} when the action is
     * missing or mistyped.
     *
     * @implNote
     * This implementation maps WA Web's
     * {@code WAWebMobilePlatforms.isSMB()} to a direct
     * {@link ClientPlatformType} comparison against the two business
     * platform values; WA Web uses string sentinel values
     * {@code "smba"} and {@code "smbi"}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebCustomPaymentMethodsSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.DIRECT)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var platform = client.store().accountStore().device().platform();
        if (platform != ClientPlatformType.IOS_BUSINESS && platform != ClientPlatformType.ANDROID_BUSINESS) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "custom payment methods: unsupported platform {0}", platform);
            return MutationApplicationResult.unsupported();
        }

        if (!abPropsService.getBool(ABProp.PAYMENTS_BR_PIX_PHASE_1_SELLER_SYNC_ENABLED)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "custom payment methods: ab-prop disabled");
            return MutationApplicationResult.unsupported();
        }

        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof CustomPaymentMethodsAction action)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "custom payment methods: mutation value is not a CustomPaymentMethodsAction");
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        client.store().businessStore().setCustomPaymentMethods(action.customPaymentMethods());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "custom payment methods: set {0} methods", action.customPaymentMethods().size());
        return MutationApplicationResult.success();
    }

}
