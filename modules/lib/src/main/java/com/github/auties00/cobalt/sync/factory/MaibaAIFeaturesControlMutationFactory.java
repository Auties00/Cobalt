package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.bot.MaibaAIFeaturesControlAction;
import com.github.auties00.cobalt.wire.linked.sync.action.bot.MaibaAIFeaturesControlActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Builds outgoing Maiba-AI-features-control sync mutations.
 *
 * <p>Backs the Meta-AI feature-status toggle on the Settings privacy surface. A single call
 * produces one {@link SyncPendingMutation} that propagates the chosen feature status to every
 * linked device, where it is consumed by
 * {@link com.github.auties00.cobalt.sync.handler.MaibaAIFeaturesControlHandler}.
 *
 * @implNote
 * This implementation has no dedicated WA Web counterpart module: the
 * {@code maiba_ai_features_control} action is declared only as a protobuf shape in
 * {@code WAWebProtobufSyncAction.pb} with no {@code WAWebMaibaAIFeaturesControlSync} module in the
 * current bundle. The shape follows {@code WAWebSyncdActionUtils.buildPendingMutation} as used by
 * every sibling {@code AccountSyncdActionBase} subclass.
 */
public final class MaibaAIFeaturesControlMutationFactory {
    /**
     * The logger for {@link MaibaAIFeaturesControlMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(MaibaAIFeaturesControlMutationFactory.class);

    /**
     * Constructs a Maiba-AI-features-control mutation factory.
     *
     * <p>The factory keeps no state, so a single instance is sufficient per client.
     */
    public MaibaAIFeaturesControlMutationFactory() {

    }

    /**
     * Builds a pending {@code maiba_ai_features_control} mutation carrying the given AI feature
     * status.
     *
     * <p>Receiving devices store the {@link MaibaAIFeaturesControlAction.MaibaAIFeatureStatus}
     * value verbatim in their local prefs. The index carries only the action name because the
     * action is a singleton per account.
     *
     * @implNote
     * This implementation models the {@code SyncActionValue.maibaAiFeaturesControlAction} protobuf
     * shape as used by {@code WAWebSyncdActionUtils.buildPendingMutation}, routing the result
     * through the {@code Regular} collection alongside the other account-scoped settings.
     *
     * @param timestamp the mutation timestamp recorded on both the outer mutation and the inner
     *                  {@code SyncActionValue}
     * @param status    the new {@link MaibaAIFeaturesControlAction.MaibaAIFeatureStatus}
     * @return a pending mutation carrying the {@code maiba_ai_features_control} action
     * @throws NullPointerException if {@code timestamp} or {@code status} is {@code null}
     */
    public SyncPendingMutation getMaibaAiFeatureStatusMutation(Instant timestamp, MaibaAIFeaturesControlAction.MaibaAIFeatureStatus status) {
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building maiba ai features control mutation status={0}", status);
        var action = new MaibaAIFeaturesControlActionBuilder()
                .aiFeatureStatus(status)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .maibaAiFeaturesControlAction(action)
                .build();
        var index = JSON.toJSONString(List.of(MaibaAIFeaturesControlAction.ACTION_NAME));
        var pending = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                MaibaAIFeaturesControlAction.ACTION_VERSION
        );
        return new SyncPendingMutation(pending, 0);
    }
}
