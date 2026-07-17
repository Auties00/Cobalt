package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.device.KeyExpirationAction;
import com.github.auties00.cobalt.wire.linked.sync.action.device.KeyExpirationActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.key.SyncKeyUtils;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds outgoing sentinel sync mutations.
 *
 * <p>This factory drives the key-rotation flow which seeds one sentinel mutation per app-state
 * collection so that subsequent mutations encrypted under the freshly rotated sync key have a
 * predecessor MAC chain anchored on the new key epoch. Mutations produced here are consumed on
 * receiving devices by {@link com.github.auties00.cobalt.sync.handler.SentinelHandler}, which
 * expires the matching key epoch in a transaction.
 *
 * @implNote
 * WA Web iterates over its collection-name enum and emits one mutation per collection name; Cobalt
 * iterates over {@link SyncPatchType#values()}, which lists the same set of names. Each mutation's
 * index follows the standard {@code [actionName, collectionName]} shape and carries the newest key
 * pair's epoch as the {@code keyExpiration.expiredKeyEpoch} field.
 */
public final class SentinelMutationFactory {
    /**
     * The logger for {@link SentinelMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(SentinelMutationFactory.class);

    /**
     * Constructs a sentinel mutation factory.
     *
     * <p>The factory keeps no state, so a single instance is sufficient per client.
     */
    public SentinelMutationFactory() {

    }

    /**
     * Creates sentinel pending mutations for every sync collection type.
     *
     * <p>The returned list contains one mutation per {@link SyncPatchType}; receiving devices
     * expire the matching key epoch and mark the collection as ready for the next sync cycle. The
     * list is {@link Collections#emptyList()} when no sync key pairs are available, matching WA
     * Web's no-key-pair early return.
     *
     * @implNote
     * This implementation reads the newest sync-key pair via
     * {@link SyncKeyUtils#findNewestKey(java.util.Collection)} and its epoch via
     * {@link SyncKeyUtils#getKeyEpoch(byte[])}, then emits one {@code SET} mutation per
     * {@link SyncPatchType}. The {@code SyncActionValue} (and therefore the inner
     * {@code keyExpiration.expiredKeyEpoch}) is shared across every mutation because the epoch is
     * per-account, not per-collection.
     *
     * @param client the WhatsApp client whose store is consulted for the app-state-keys map; the
     *               newest key pair becomes the epoch source
     * @return a list of pending mutations, one per {@link SyncPatchType}, or
     *         {@link Collections#emptyList()} if no sync key pairs exist
     */
    @WhatsAppWebExport(moduleName = "WAWebSentinelMutationSync", exports = "getSentinelMutations", adaptation = WhatsAppAdaptation.DIRECT)
    public List<SyncPendingMutation> getSentinelMutations(LinkedWhatsAppClient client) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "preparing sentinel mutations");

        var timestamp = Instant.now();
        var collections = SyncPatchType.values();
        var newestKey = SyncKeyUtils.findNewestKey(client.store().syncStore().appStateKeys());
        if (newestKey == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "sentinel mutation sync: no key pairs available");
            return Collections.emptyList();
        }

        var keyEpoch = SyncKeyUtils.getKeyEpoch(newestKey);
        var keyExpirationAction = new KeyExpirationActionBuilder()
                .expiredKeyEpoch(keyEpoch)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .keyExpirationAction(keyExpirationAction)
                .build();

        var mutations = new ArrayList<SyncPendingMutation>(collections.length);
        for (var collection : collections) {
            var index = JSON.toJSONString(List.of(KeyExpirationAction.ACTION_NAME, collection.toString()));
            var mutation = new DecryptedMutation.Trusted(
                    index,
                    value,
                    SyncdOperation.SET,
                    timestamp,
                    KeyExpirationAction.ACTION_VERSION
            );
            mutations.add(new SyncPendingMutation(mutation, 0));
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "built {0} sentinel mutations, keyEpoch={1}", mutations.size(), keyEpoch);
        return mutations;
    }
}
