package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.media.RemoveRecentStickerAction;
import com.github.auties00.cobalt.wire.linked.sync.action.media.RemoveRecentStickerActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing remove-recent-sticker sync mutations.
 *
 * <p>This factory backs the long-press "remove from recents" gesture on the stickers tray.
 * Mutations produced here are consumed on receiving devices by
 * {@link com.github.auties00.cobalt.sync.handler.RemoveRecentStickerHandler}, whose application is
 * gated and only removes the local entry when its timestamp is at most the carried
 * {@link RemoveRecentStickerAction#lastStickerSentTs()} value.
 */
public final class RemoveRecentStickerMutationFactory {
    /**
     * The logger for {@link RemoveRecentStickerMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(RemoveRecentStickerMutationFactory.class);

    /**
     * Constructs a remove-recent-sticker mutation factory.
     *
     * <p>The factory keeps no state, so a single instance is sufficient per client.
     */
    public RemoveRecentStickerMutationFactory() {

    }

    /**
     * Builds a pending outgoing mutation that removes a sticker from the recent-stickers collection
     * across linked devices.
     *
     * <p>The receiver compares the carried {@link RemoveRecentStickerAction#lastStickerSentTs()}
     * against its own recent-sticker entry's timestamp and only removes it when the local entry is
     * at most as recent as the carried value, so a sticker sent on another device after this remove
     * arrives is not retroactively dropped. The index follows the standard
     * {@code [actionName, stickerFileHash]} shape.
     *
     * @implNote
     * This implementation stamps {@link Instant#now()} on both the outer mutation timestamp and the
     * inner {@link RemoveRecentStickerAction#lastStickerSentTs()}, matching WA Web's single
     * timestamp capture.
     *
     * @param stickerHash the sticker file hash used as the mutation index; matches the
     *                    recent-sticker key on the receiver
     * @return the pending mutation ready to be pushed via
     *         {@link com.github.auties00.cobalt.sync.WebAppStateService#pushPatches}
     */
    @WhatsAppWebExport(moduleName = "WAWebStickersRemoveRecentSyncAction", exports = "generateRemoveStickerMutation", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getRemoveRecentStickerMutation(String stickerHash) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building remove-recent-sticker mutation, hash={0}", stickerHash);
        var timestamp = Instant.now();
        var action = new RemoveRecentStickerActionBuilder()
                .lastStickerSentTs(timestamp)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .removeRecentStickerAction(action)
                .build();
        var index = JSON.toJSONString(List.of(RemoveRecentStickerAction.ACTION_NAME, stickerHash));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                RemoveRecentStickerAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
