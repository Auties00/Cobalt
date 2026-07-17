package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.media.StickerAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSyncStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Applies the {@code favoriteSticker} app-state sync action that adds or
 * removes a sticker from the user's favourites collection.
 *
 * <p>Each toggle fans out across the {@link SyncPatchType#REGULAR_LOW}
 * collection so companions render the same favourites set. The mutation index
 * keys each entry by the sticker's filehash, formatted as
 * {@snippet :
 *     ["favoriteSticker", stickerFileHash]
 * }
 * The apply path is gated by the primary device advertising the
 * {@value #FAVORITE_STICKER_FEATURE} feature in
 * {@link LinkedWhatsAppSyncStore#primaryFeatures()};
 * when it does not, the mutation is reported as
 * {@link MutationApplicationResult#orphan(String, String)}.
 */
@WhatsAppWebModule(moduleName = "WAWebStickersFavoriteSyncAction")
public final class FavoriteStickerHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link FavoriteStickerHandler}.
     */
    private static final System.Logger LOGGER = Log.get(FavoriteStickerHandler.class);

    /**
     * The primary-feature id consulted to gate favourite-sticker sync.
     */
    private static final String FAVORITE_STICKER_FEATURE = "favorite_sticker";

    /**
     * Constructs a new singleton {@link FavoriteStickerHandler}.
     */
    @WhatsAppWebExport(moduleName = "WAWebStickersFavoriteSyncAction", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public FavoriteStickerHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebStickersFavoriteSyncAction", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return StickerAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebStickersFavoriteSyncAction", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return StickerAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebStickersFavoriteSyncAction", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return StickerAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Rejects non-{@link SyncdOperation#SET} operations as
     * {@link MutationApplicationResult#unsupported()}, a missing sticker hash
     * slot or absent action payload as malformed, and an absent
     * {@value #FAVORITE_STICKER_FEATURE} gate as
     * {@link MutationApplicationResult#orphan(String, String)}. When
     * {@link StickerAction#isFavorite()} is set the sticker is timestamped and
     * added via
     * {@link LinkedWhatsAppSettingsStore#addFavouriteSticker(String, com.github.auties00.cobalt.wire.linked.preference.Sticker)};
     * otherwise it is removed via
     * {@link LinkedWhatsAppSettingsStore#removeFavouriteSticker(String)}.
     *
     * @implNote
     * This implementation classifies a missing index slot as
     * {@link MutationApplicationResult#malformed()} before parsing so the outer
     * try/catch does not turn it into {@link MutationApplicationResult#failed()}
     * via an {@code IndexOutOfBoundsException} from {@code JSON.parseArray}.
     * Because {@link StickerAction#isFavorite()} coalesces a {@code null}
     * protobuf field to {@code false}, a {@code null} flag is treated as a
     * removal rather than a malformed value. An add fast-paths an entry that is
     * already present through
     * {@link LinkedWhatsAppSettingsStore#findFavouriteSticker(String)},
     * and a removal is unconditional because the store call is idempotent.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebStickersFavoriteSyncAction", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        try {
            var indexArray = JSON.parseArray(mutation.index());
            if (indexArray.size() <= 1) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "favorite sticker: malformed mutation index");
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }
            var stickerHash = indexArray.getString(1);
            if (stickerHash == null || stickerHash.isEmpty()) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "favorite sticker: missing sticker hash");
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof StickerAction action)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "favorite sticker: mutation value is not a StickerAction");
                return SyncdIndexUtils.malformedActionValue(collectionName().name());
            }
            if (!client.store().syncStore().primaryFeatures().contains(FAVORITE_STICKER_FEATURE)) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "favorite sticker: orphan, primary feature not advertised");
                return MutationApplicationResult.orphan(stickerHash, "FavoriteSticker");
            }

            if (action.isFavorite()) {
                if (client.store().settingsStore().findFavouriteSticker(stickerHash).isPresent()) {
                    if (Log.TRACE) LOGGER.log(Level.TRACE, "favorite sticker: already favourited, skipping");
                    return MutationApplicationResult.success();
                }
                var sticker = action.toSticker();
                sticker.setTimestamp(mutation.timestamp().getEpochSecond());
                client.store().settingsStore().addFavouriteSticker(stickerHash, sticker);
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "favorite sticker: added");
            } else {
                client.store().settingsStore().removeFavouriteSticker(stickerHash);
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "favorite sticker: removed");
            }

            return MutationApplicationResult.success();
        } catch (Exception e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "favorite sticker: failed to apply mutation", e);
            return MutationApplicationResult.failed();
        }
    }

}
