package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.device.NuxAction;
import com.github.auties00.cobalt.wire.linked.sync.action.device.NuxActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Builds outgoing NUX-action sync mutations.
 *
 * <p>Backs the new-user-experience acknowledgement system: every dismissible tooltip, banner, or
 * one-time tip on the WhatsApp Web surface broadcasts its dismissal across linked devices through
 * a NUX mutation. Mutations produced here are consumed on the inbound side by
 * {@link com.github.auties00.cobalt.sync.handler.NuxActionHandler}, which updates the receiver's
 * NUX preferences.
 *
 * @implNote
 * This implementation mirrors {@code WAWebNuxSync.$NuxSync$p_1} (the private mutation builder
 * consumed by {@code acknowledgeNux} and {@code unAcknowledgeNux}) and adds defensive null checks
 * on the caller-supplied identifier and timestamp.
 */
public final class NuxActionMutationFactory {
    /**
     * The logger for {@link NuxActionMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(NuxActionMutationFactory.class);

    /**
     * Constructs a NUX-action mutation factory.
     *
     * <p>The factory keeps no state, so a single instance is sufficient per client.
     */
    public NuxActionMutationFactory() {

    }

    /**
     * Builds a pending mutation for acknowledging or unacknowledging a NUX item.
     *
     * <p>Receiving devices update their local map from NUX key to acknowledged flag. The same
     * builder serves both the acknowledge and unacknowledge paths via the {@code acknowledged}
     * flag.
     *
     * @implNote
     * This implementation adds two defensive null checks not present in WA Web
     * ({@code WAWebNuxSync} relies on the caller never passing {@code null}). The index follows the
     * standard {@code [actionName, nuxKey]} shape and writes into the {@code RegularLow}
     * collection alongside the other account-scoped NUX state.
     *
     * @param nuxKey       the NUX identifier (the {@code indexArgs[0]} entry); the receiver
     *                     requires it to be a non-empty string
     * @param timestamp    the mutation timestamp recorded on both the outer mutation and the inner
     *                     {@code SyncActionValue}
     * @param acknowledged {@code true} when the NUX item is acknowledged, {@code false} to revert
     * @return the pending mutation for the NUX action
     * @throws NullPointerException if {@code nuxKey} or {@code timestamp} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebNuxSync", exports = "$NuxSync$p_1", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getNuxMutation(String nuxKey, Instant timestamp, boolean acknowledged) {
        Objects.requireNonNull(nuxKey, "nuxKey cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building nux mutation key={0} acknowledged={1}", nuxKey, acknowledged);
        var action = new NuxActionBuilder()
                .acknowledged(acknowledged)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .nuxAction(action)
                .build();
        var index = JSON.toJSONString(List.of(NuxAction.ACTION_NAME, nuxKey));
        var pendingMutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                NuxAction.ACTION_VERSION
        );
        return new SyncPendingMutation(pendingMutation, 0);
    }
}
