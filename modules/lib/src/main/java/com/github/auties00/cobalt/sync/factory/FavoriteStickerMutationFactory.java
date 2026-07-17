package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.media.StickerAction;
import com.github.auties00.cobalt.wire.linked.sync.action.media.StickerActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing app-state mutations that favourite or unfavourite a sticker.
 *
 * Drives the sticker-tray "add to favourites" affordance: the resulting mutation flips the sticker's
 * {@code isFavorite} flag on every linked device. This factory is the outgoing-mutation counterpart
 * of {@link com.github.auties00.cobalt.sync.handler.FavoriteStickerHandler}, which reconciles the
 * flag on receiving devices.
 *
 * @implNote
 * This implementation only sets the {@code isFavorite} flag on the {@link StickerAction}. WA Web's
 * {@code generateFavoriteSyncMutation} additionally serializes the full media descriptor
 * ({@code fileEncSha256}, {@code mediaKey}, {@code mimetype}, dimensions, {@code directPath},
 * {@code deviceIdHint}); Cobalt omits those because the receive-side path falls back to the existing
 * sticker record on the primary device when the mutation round-trips, and the downstream
 * {@code addOrUpdateStickers} call still has the descriptor locally available.
 */
public final class FavoriteStickerMutationFactory {
    /**
     * The logger for {@link FavoriteStickerMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(FavoriteStickerMutationFactory.class);

    /**
     * Creates an instance with no collaborators.
     *
     * The factory is stateless, so a single instance may be shared across the lifetime of the
     * client.
     */
    public FavoriteStickerMutationFactory() {

    }

    /**
     * Returns a SET mutation that favourites or unfavourites the sticker with the given file hash.
     *
     * The mutation index follows
     * {@snippet :
     *     ["favoriteSticker", stickerHash]
     * }
     * where {@code stickerHash} is the sticker's {@code filehash}; the {@link StickerAction}
     * sub-message carries only the {@code isFavorite} flag.
     *
     * @implNote
     * This implementation captures the timestamp via {@link Instant#now()}; WA Web's
     * {@code generateFavoriteSyncMutation} accepts the timestamp as a parameter (caller passes
     * {@code WATimeUtils.unixTime()}).
     *
     * @param stickerHash the sticker's file hash (Base64 string) used as the mutation index
     * @param favorite    {@code true} to favourite the sticker, {@code false} to unfavourite it
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebStickersFavoriteSyncAction", exports = "generateFavoriteSyncMutation", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getFavoriteStickerMutation(String stickerHash, boolean favorite) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building favorite sticker mutation hash={0} favorite={1}", stickerHash, favorite);
        var action = new StickerActionBuilder()
                .isFavorite(favorite)
                .build();
        var timestamp = Instant.now();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .stickerAction(action)
                .build();
        var index = JSON.toJSONString(List.of(StickerAction.ACTION_NAME, stickerHash));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                StickerAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
