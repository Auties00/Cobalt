package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.media.RecentEmojiWeight;
import com.github.auties00.cobalt.wire.linked.sync.action.media.RecentEmojiWeightsAction;
import com.github.auties00.cobalt.wire.linked.sync.action.media.RecentEmojiWeightsActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Builds outgoing app-state mutations that publish the local recent-emoji usage ranking to every
 * linked device.
 *
 * <p>This factory drives cross-device convergence of the emoji-picker ordering: each time the user
 * picks an emoji on one device the local weight is bumped, and the entire weight snapshot is
 * broadcast so companions see the same suggestion order.
 *
 * @implNote
 * WA Web marks the recent-emoji collection dirty and publishes the full snapshot through the
 * standard syncd dirty-flag pipeline. Cobalt does not run a dirty-flag scheduler at this layer;
 * callers re-publish on demand via the public client setter.
 */
public final class RecentEmojiWeightsMutationFactory {
    /**
     * The logger for {@link RecentEmojiWeightsMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(RecentEmojiWeightsMutationFactory.class);

    /**
     * Constructs a recent-emoji-weights mutation factory.
     *
     * <p>The factory is stateless; a single instance may be shared across the lifetime of the
     * client.
     */
    public RecentEmojiWeightsMutationFactory() {

    }

    /**
     * Builds a pending SET mutation that overwrites the recent-emoji weight snapshot with the given
     * list.
     *
     * <p>The index carries only the action name because the snapshot is a singleton per account.
     *
     * @implNote
     * This implementation captures the timestamp via {@link Instant#now()}; WA Web emits the
     * mutation from the dirty-flag scheduler, so its timestamp matches the scheduler tick rather
     * than the user gesture.
     *
     * @param usage the per-emoji weight snapshot to publish; an empty list is valid and represents
     *              "no recent usage"
     * @return the pending mutation ready to be queued for outbound app-state sync
     * @throws NullPointerException if {@code usage} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebRecentEmojiCollection", exports = "increment", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getRecentEmojiWeightsMutation(List<RecentEmojiWeight> usage) {
        Objects.requireNonNull(usage, "usage cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building recent emoji weights mutation, count={0}", usage.size());
        var timestamp = Instant.now();
        var action = new RecentEmojiWeightsActionBuilder()
                .weights(usage)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .recentEmojiWeightsAction(action)
                .build();
        var index = JSON.toJSONString(List.of(RecentEmojiWeightsAction.ACTION_NAME));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                RecentEmojiWeightsAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
