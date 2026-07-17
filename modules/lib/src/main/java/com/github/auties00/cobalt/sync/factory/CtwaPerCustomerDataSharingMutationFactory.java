package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.business.CtwaPerCustomerDataSharingAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.CtwaPerCustomerDataSharingActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing app-state mutations that record a Click-To-WhatsApp per-customer data-sharing preference.
 *
 * <p>The mutation captures the SMB "share my data with the advertiser" toggle
 * per customer LID, populating the per-customer data-sharing table on the
 * primary device and on every linked device. This factory builds the outgoing
 * mutation; the inbound counterpart is
 * {@link com.github.auties00.cobalt.sync.handler.CtwaPerCustomerDataSharingHandler}.
 */
public final class CtwaPerCustomerDataSharingMutationFactory {
    /**
     * The logger for {@link CtwaPerCustomerDataSharingMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(CtwaPerCustomerDataSharingMutationFactory.class);

    /**
     * Creates a stateless factory with no collaborators.
     *
     * <p>A single instance may be shared across the lifetime of the client.
     */
    public CtwaPerCustomerDataSharingMutationFactory() {

    }

    /**
     * Returns a SET mutation that records the per-customer data-sharing preference for the given account LID.
     *
     * <p>The mutation index follows
     * {@snippet :
     *     ["adsCtwaPerCustomerDataSharing", accountLid.toString()]
     * }
     * and the {@link CtwaPerCustomerDataSharingAction} sub-message carries
     * {@code isCtwaPerCustomerDataSharingEnabled}. The receive-side handler
     * upserts the row and emits a CTWA system message.
     *
     * @implNote
     * This implementation captures the timestamp via {@link Instant#now()}.
     *
     * @param accountLid the customer's LID-form {@link Jid}
     * @param isEnabled  {@code true} to opt the customer into per-customer data sharing, {@code false} to opt them out
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebCtwaPerCustomerDataSharingSync", exports = "getCtwaPerCustomerDataSharingMutation", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPendingMutation getCtwaPerCustomerDataSharingMutation(Jid accountLid, boolean isEnabled) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building ctwa per-customer data sharing mutation account={0} enabled={1}", accountLid, isEnabled);
        var timestamp = Instant.now();
        var action = new CtwaPerCustomerDataSharingActionBuilder()
                .isCtwaPerCustomerDataSharingEnabled(isEnabled)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .ctwaPerCustomerDataSharingAction(action)
                .build();
        var index = JSON.toJSONString(List.of(CtwaPerCustomerDataSharingAction.ACTION_NAME, accountLid.toString()));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                CtwaPerCustomerDataSharingAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
