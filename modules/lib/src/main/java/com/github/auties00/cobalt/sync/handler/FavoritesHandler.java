package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.media.FavoritesAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppChatStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppContactStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Applies the {@code favorites} app-state sync action that replaces the user's
 * pinned-favourites collection across linked devices.
 *
 * <p>The action carries the entire ordered favourites list fanned out across
 * the {@link SyncPatchType#REGULAR_HIGH} collection. The action replaces rather
 * than merges, and only the most recent mutation in a batch is applied so
 * intermediate edits performed in quick succession on the primary do not
 * surface partially.
 *
 * @implNote
 * This implementation resolves each favourite JID against the chat table
 * first, then through
 * {@link LinkedWhatsAppContactStore#findPhoneByLid(Jid)}
 * when the raw JID carries the LID server, then falls back to the raw JID, and
 * persists the resolved list directly through
 * {@link LinkedWhatsAppChatStore#setFavoriteChats(List)}.
 */
@WhatsAppWebModule(moduleName = "WAWebFavoritesSync")
public final class FavoritesHandler implements WebAppStateActionHandler {

    /**
     * The logger for {@link FavoritesHandler}.
     */
    private static final System.Logger LOGGER = Log.get(FavoritesHandler.class);

    /**
     * Constructs a new singleton {@link FavoritesHandler}.
     */
    public FavoritesHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @WhatsAppWebExport(moduleName = "WAWebFavoritesSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public String actionName() {
        return FavoritesAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @WhatsAppWebExport(moduleName = "WAWebFavoritesSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public SyncPatchType collectionName() {
        return FavoritesAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @WhatsAppWebExport(moduleName = "WAWebFavoritesSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public int version() {
        return FavoritesAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Walks the batch classifying each entry, tracks the
     * {@link DecryptedMutation.Trusted} with the highest timestamp, and applies
     * only that one through
     * {@link #applyLatestMutation(LinkedWhatsAppClient, DecryptedMutation.Trusted)};
     * every other valid entry still receives
     * {@link MutationApplicationResult#success()}. Non-{@link SyncdOperation#SET}
     * operations and entries whose payload is not a {@link FavoritesAction} are
     * tallied and reported as malformed.
     *
     * @implNote
     * This implementation applies only the latest mutation because the action
     * replaces the whole collection, so superseded entries would have no
     * observable effect. The unsupported and malformed counts are logged
     * through {@link #LOGGER} after the loop.
     */
    @WhatsAppWebExport(moduleName = "WAWebFavoritesSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public List<MutationApplicationResult> applyMutationBatch(LinkedWhatsAppClient client, List<DecryptedMutation.Trusted> mutations) {
        DecryptedMutation.Trusted latest = null;
        var unsupportedCount = 0;
        var malformedCount = 0;
        var results = new ArrayList<MutationApplicationResult>(mutations.size());
        for (var mutation : mutations) {
            if (mutation.operation() != SyncdOperation.SET) {
                unsupportedCount++;
                results.add(SyncdIndexUtils.malformedActionValue(collectionName().name()));
                continue;
            }

            if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof FavoritesAction)) {
                malformedCount++;
                results.add(SyncdIndexUtils.malformedActionValue(collectionName().name()));
                continue;
            }

            if (latest == null || mutation.timestamp().compareTo(latest.timestamp()) > 0) {
                latest = mutation;
            }
            results.add(MutationApplicationResult.success());
        }

        if (unsupportedCount > 0 && Log.WARNING) {
            LOGGER.log(Level.WARNING, "favorites sync: {0} operations not supported", unsupportedCount);
        }
        if (malformedCount > 0 && Log.WARNING) {
            LOGGER.log(Level.WARNING, "favorites sync: {0} malformed mutations", malformedCount);
        }

        if (latest != null) {
            applyLatestMutation(client, latest);
        }

        return results;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Runs the same validation as the batch path but unconditionally applies
     * the supplied mutation through
     * {@link #applyLatestMutation(LinkedWhatsAppClient, DecryptedMutation.Trusted)},
     * so a single {@link FavoritesAction} dispatched outside a batch still
     * mutates the favourites collection. Non-{@link SyncdOperation#SET}
     * operations and payloads that are not a {@link FavoritesAction} are
     * reported as malformed.
     */
    @WhatsAppWebExport(moduleName = "WAWebFavoritesSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof FavoritesAction)) {
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        applyLatestMutation(client, mutation);
        return MutationApplicationResult.success();
    }

    /**
     * Replaces the favourites collection with the entries carried by the
     * supplied mutation.
     *
     * <p>Resolves each entry in iteration order: entries whose raw id is absent
     * are skipped, the chat table is consulted first, then a LID-to-phone
     * fallback via
     * {@link LinkedWhatsAppContactStore#findPhoneByLid(Jid)}
     * is attempted when the raw {@link Jid} carries the LID server, and the raw
     * JID is preserved as a final fallback. The resolved list is persisted
     * through {@link LinkedWhatsAppChatStore#setFavoriteChats(List)}.
     *
     * @param client   the {@link LinkedWhatsAppClient} whose store will be mutated
     * @param mutation the source mutation whose
     *                 {@link FavoritesAction#favorites()} list is resolved and
     *                 persisted in iteration order
     */
    @WhatsAppWebExport(moduleName = "WAWebFavoritesSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    private void applyLatestMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var action = (FavoritesAction) mutation.value().flatMap(sav -> sav.action()).orElseThrow();
        var favorites = new ArrayList<Jid>();
        for (var favorite : action.favorites()) {
            var rawId = favorite.id().orElse(null);
            if (rawId == null) {
                continue;
            }

            var rawJid = Jid.of(rawId);
            var resolved = client.store().chatStore().findChatByJid(rawJid)
                    .map(entry -> entry.jid())
                    .or(() -> rawJid.hasLidServer() ? client.store().contactStore().findPhoneByLid(rawJid) : Optional.<Jid>empty())
                    .orElse(rawJid);
            favorites.add(resolved);
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "favorites sync: replacing favorites with {0} entries", favorites.size());
        client.store().chatStore().setFavoriteChats(favorites);
    }

}
