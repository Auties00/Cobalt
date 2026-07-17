package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.MerchantPaymentPartnerAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Applies the {@code merchant_payment_partner} app-state sync action that
 * persists the SMB merchant's payment service provider for Brazil.
 *
 * <p>This handler backs the SMB Brazil merchant onboarding flow: when the
 * primary device finishes the BR PSP onboarding the resulting partner record
 * fans out across the {@link SyncPatchType#REGULAR_LOW} collection so
 * companion devices can show the correct payment method. The handler is gated
 * by the SMB platform check
 * ({@link ClientPlatformType#IOS_BUSINESS} /
 * {@link ClientPlatformType#ANDROID_BUSINESS}) and the
 * {@link ABProp#PAYMENTS_BR_MERCHANT_PSP_ACCOUNT_STATUS_SYNC} A/B prop; while
 * either gate is closed every mutation is reported as
 * {@link MutationApplicationResult#unsupported()}. The mutation index has no
 * variable parts and is always
 * {@snippet :
 *     ["merchant_payment_partner"]
 * }
 *
 * @implNote
 * This implementation persists the action through
 * {@link LinkedWhatsAppBusinessStore#setMerchantPaymentPartner(MerchantPaymentPartnerAction)}
 * instead of WA Web's UserPrefs write, since Cobalt has no UserPrefs
 * key-value store. The SMB and A/B-prop gates are evaluated per mutation
 * rather than once for the batch, so a server-side prop flip reaches the next
 * incoming sync without restarting the client.
 */
public final class MerchantPaymentPartnerHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link MerchantPaymentPartnerHandler}.
     */
    private static final System.Logger LOGGER = Log.get(MerchantPaymentPartnerHandler.class);

    /**
     * The {@link ABPropsService} consulted before every mutation to gate the
     * handler on {@link ABProp#PAYMENTS_BR_MERCHANT_PSP_ACCOUNT_STATUS_SYNC}.
     */
    private final ABPropsService abPropsService;

    /**
     * Constructs a {@link MerchantPaymentPartnerHandler} bound to the given
     * A/B-props service for registration in the sync handler registry.
     *
     * @param abPropsService the A/B-props service consulted on every mutation
     */
    public MerchantPaymentPartnerHandler(ABPropsService abPropsService) {
        this.abPropsService = abPropsService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String actionName() {
        return MerchantPaymentPartnerAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SyncPatchType collectionName() {
        return MerchantPaymentPartnerAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int version() {
        return MerchantPaymentPartnerAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The SMB platform tag and the
     * {@link ABProp#PAYMENTS_BR_MERCHANT_PSP_ACCOUNT_STATUS_SYNC} flag are
     * read first; when either gate is closed the mutation is reported as
     * {@link MutationApplicationResult#unsupported()}. When both gates are
     * open and the operation is {@link SyncdOperation#SET}, the value is
     * persisted directly through
     * {@link LinkedWhatsAppBusinessStore#setMerchantPaymentPartner(MerchantPaymentPartnerAction)}.
     */
    @Override
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var platform = client.store().accountStore().device().platform();
        if (platform != ClientPlatformType.IOS_BUSINESS && platform != ClientPlatformType.ANDROID_BUSINESS) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "merchant payment partner mutation unsupported, platform={0}", platform);
            return MutationApplicationResult.unsupported();
        }

        if (!abPropsService.getBool(ABProp.PAYMENTS_BR_MERCHANT_PSP_ACCOUNT_STATUS_SYNC)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "merchant payment partner mutation unsupported, ab prop disabled");
            return MutationApplicationResult.unsupported();
        }

        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof MerchantPaymentPartnerAction action)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "merchant payment partner mutation has malformed action value");
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        client.store().businessStore().setMerchantPaymentPartner(action);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "merchant payment partner applied");
        return MutationApplicationResult.success();
    }
}
