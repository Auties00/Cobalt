package com.github.auties00.cobalt.wire.linked.sync.action.media;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import java.time.Instant;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A sync action that removes a sticker from the user's recent-stickers list
 * across linked devices.
 *
 * <p>The sticker to remove is identified by the file hash carried in the
 * associated {@link RemoveRecentStickerActionArgs}. The action additionally
 * records the timestamp at which the sticker was last sent so that consumers
 * can reconcile concurrent updates and keep the recent list in a consistent
 * order across devices.
 */
@ProtobufMessage(name = "SyncActionValue.RemoveRecentStickerAction")
public final class RemoveRecentStickerAction implements SyncAction<RemoveRecentStickerActionArgs> {
    /**
     * The app-state action name that identifies this action type on the wire.
     */
    public static final String ACTION_NAME = "removeRecentSticker";

    /**
     * The app-state action version that identifies this action revision on the
     * wire.
     */
    public static final int ACTION_VERSION = 7;

    /**
     * The app-state collection that stores this action type.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_LOW;

    /**
     * Returns the action name used to route this action through the app-state
     * sync pipeline.
     *
     * @return the canonical action name
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the action version used to route this action through the
     * app-state sync pipeline.
     *
     * @return the canonical action version
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * The timestamp at which the sticker targeted by this action was last
     * sent. Used to order concurrent removals consistently across devices.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant lastStickerSentTs;


    /**
     * Constructs a new {@code RemoveRecentStickerAction} carrying the supplied
     * last-sent timestamp.
     *
     * @param lastStickerSentTs the last-sent timestamp, or {@code null} if unset
     */
    RemoveRecentStickerAction(Instant lastStickerSentTs) {
        this.lastStickerSentTs = lastStickerSentTs;
    }

    /**
     * Returns the last time the target sticker was sent.
     *
     * @return the last-sent timestamp, or {@link Optional#empty()} if unset
     */
    public Optional<Instant> lastStickerSentTs() {
        return Optional.ofNullable(lastStickerSentTs);
    }

    /**
     * Sets the last time the target sticker was sent.
     *
     * @param lastStickerSentTs the new last-sent timestamp, or {@code null} to clear it
     */
    public void setLastStickerSentTs(Instant lastStickerSentTs) {
        this.lastStickerSentTs = lastStickerSentTs;
    }


}
