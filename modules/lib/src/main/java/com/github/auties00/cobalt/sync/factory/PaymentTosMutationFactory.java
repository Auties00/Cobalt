package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.PaymentTosAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing payment-TOS sync mutations.
 *
 * <p>Backs the SMB (WhatsApp Business) Brazil Pix payments terms-of-service flow, which is the
 * only WA Web surface that creates outgoing payment-TOS mutations. Mutations produced here are
 * consumed on receiving devices by
 * {@link com.github.auties00.cobalt.sync.handler.PaymentTosHandler}, which persists the action.
 *
 * @implNote
 * This implementation mirrors {@code WAWebPaymentTosSync.getPaymentTosSetMutation}, which is
 * invoked from {@code WAWebPaymentsTosJob}; the receiver-side branch additionally gates the
 * application on the {@code payments_br_pix_on_web} AB prop and on
 * {@code WAWebMobilePlatforms.isSMB()}.
 */
public final class PaymentTosMutationFactory {
    /**
     * The logger for {@link PaymentTosMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(PaymentTosMutationFactory.class);

    /**
     * Constructs a payment-TOS mutation factory.
     *
     * <p>The factory keeps no state, so a single instance is sufficient per client.
     */
    public PaymentTosMutationFactory() {

    }

    /**
     * Builds a pending SET mutation for the payment terms of service.
     *
     * <p>The caller passes a fully populated {@link PaymentTosAction} and this method wraps it
     * into a pending mutation ready for outbound app-state sync. The index carries only the action
     * name because the action is a singleton per account.
     *
     * @implNote
     * This implementation stamps {@link Instant#now()} on both the outer mutation timestamp and
     * the inner {@code SyncActionValue.timestamp}, matching
     * {@code WAWebPaymentTosSync.getPaymentTosSetMutation}'s single call to
     * {@code WATimeUtils.unixTimeMs}.
     *
     * @param action the payment terms of service action to wrap into the outgoing mutation; the
     *               inner shape is forwarded verbatim to the receiver
     * @return the pending mutation ready for sync upload
     */
    @WhatsAppWebExport(moduleName = "WAWebPaymentTosSync", exports = "getPaymentTosSetMutation", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPendingMutation getPaymentTosSetMutation(PaymentTosAction action) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building payment tos set mutation");
        var timestamp = Instant.now();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .paymentTosAction(action)
                .build();
        var index = JSON.toJSONString(List.of(PaymentTosAction.ACTION_NAME));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                PaymentTosAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
