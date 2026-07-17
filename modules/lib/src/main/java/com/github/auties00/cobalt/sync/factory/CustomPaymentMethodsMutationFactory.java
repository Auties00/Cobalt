package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.CustomPaymentMethodsAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing app-state mutations that overwrite the SMB user's custom payment methods.
 *
 * Drives the Brazil PIX phase-1 seller-sync feature: SMB users curate the payment methods they
 * want to offer their customers, and the mutation replaces the whole list on every linked device.
 * This factory is the outgoing-mutation counterpart of
 * {@link com.github.auties00.cobalt.sync.handler.CustomPaymentMethodsHandler}.
 *
 * @implNote
 * This implementation does not gate on {@code WAWebMobilePlatforms.isSMB()} or the
 * {@code payments_br_pix_phase_1_seller_sync_enabled} AB-prop; WA Web checks both flags only when
 * applying inbound mutations, so the outgoing factory remains callable from any embedder.
 */
public final class CustomPaymentMethodsMutationFactory {
    /**
     * The logger for {@link CustomPaymentMethodsMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(CustomPaymentMethodsMutationFactory.class);

    /**
     * Creates an instance with no collaborators.
     *
     * The factory is stateless, so a single instance may be shared across the lifetime of the
     * client.
     */
    public CustomPaymentMethodsMutationFactory() {

    }

    /**
     * Returns a SET mutation that overwrites the SMB user's custom payment methods with the given snapshot.
     *
     * The mutation index follows
     * {@snippet :
     *     ["customPaymentMethods"]
     * }
     * with no per-row segment; the action carries the complete list so the receive side replaces
     * the prior value wholesale rather than appending to it.
     *
     * @implNote
     * This implementation captures the timestamp via {@link Instant#now()}; WA Web's
     * {@code getCustomPaymentMethodSetMutation} uses {@code WATimeUtils.unixTimeMs()} for the same
     * purpose.
     *
     * @param action the new snapshot of custom payment methods to broadcast
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebCustomPaymentMethodsSync", exports = "getCustomPaymentMethodSetMutation", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPendingMutation getCustomPaymentMethodSetMutation(CustomPaymentMethodsAction action) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building custom payment methods mutation count={0}", action.customPaymentMethods().size());
        var timestamp = Instant.now();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .customPaymentMethodsAction(action)
                .build();
        var index = JSON.toJSONString(List.of(CustomPaymentMethodsAction.ACTION_NAME));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                CustomPaymentMethodsAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
