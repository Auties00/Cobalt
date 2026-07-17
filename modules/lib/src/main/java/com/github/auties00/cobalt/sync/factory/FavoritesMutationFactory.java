package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.media.FavoritesAction;
import com.github.auties00.cobalt.wire.linked.sync.action.media.FavoritesActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.media.FavoritesActionFavoriteBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing app-state mutations that overwrite the favourites chat list (pinned-conversations short list).
 *
 * Drives the favourites surface: every add or remove computes the new full favourites list and
 * pushes the snapshot via this factory so linked devices replace their stored favourites in one
 * shot. This factory is the outgoing-mutation counterpart of
 * {@link com.github.auties00.cobalt.sync.handler.FavoritesHandler}.
 *
 * @implNote
 * This implementation uses {@link Jid#toString()} verbatim for each favourite entry's id. WA Web's
 * {@code getFavoritesMutation} runs every JID through
 * {@code WAWebSyncdGetChat.getWidMutationIndexForWid} to translate user JIDs into their LID
 * mutation-index form during the LID 1x1 migration; Cobalt's store handles LID resolution at a
 * higher layer so this factory does not duplicate that translation.
 */
public final class FavoritesMutationFactory {
    /**
     * The logger for {@link FavoritesMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(FavoritesMutationFactory.class);

    /**
     * Creates an instance with no collaborators.
     *
     * The factory is stateless, so a single instance may be shared across the lifetime of the
     * client.
     */
    public FavoritesMutationFactory() {

    }

    /**
     * Returns a SET mutation that replaces the favourites list with the supplied ordered snapshot.
     *
     * The mutation index follows
     * {@snippet :
     *     ["favorites"]
     * }
     * with no per-row segment; the action carries the complete ordered list so the receive side
     * picks the latest-timestamp mutation per batch and replaces the local favourites collection
     * wholesale.
     *
     * @implNote
     * This implementation derives each entry's {@code orderIndex} implicitly from list position by
     * relying on the upstream order; WA Web preserves order through an explicit {@code orderIndex}
     * field on every favourite which it sorts on the receive side. Cobalt's
     * {@link FavoritesActionFavoriteBuilder} writes only {@code id}, so the order is the wire order,
     * not a per-entry numeric tag.
     *
     * @param favoriteJids the new full snapshot of favourite chat {@link Jid}s, in display order
     * @param timestamp    the mutation timestamp
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebFavoritesSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getFavoritesMutation(List<Jid> favoriteJids, Instant timestamp) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building favorites mutation count={0}", favoriteJids.size());
        var favoriteEntries = favoriteJids.stream()
                .map(jid -> new FavoritesActionFavoriteBuilder()
                        .id(jid.toString())
                        .build())
                .toList();
        var action = new FavoritesActionBuilder()
                .favorites(favoriteEntries)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .favoritesAction(action)
                .build();
        var index = JSON.toJSONString(List.of(FavoritesAction.ACTION_NAME));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                FavoritesAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
