package com.github.auties00.cobalt.wire.linked.sync.action.media;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import java.util.Collections;
import java.util.List;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A sync action that announces a change to the user's personalised avatar and
 * propagates the updated recent-avatar-sticker set across linked devices.
 *
 * <p>The action is emitted whenever the user creates, updates or deletes their
 * avatar so that every linked device stays in sync with the current avatar
 * state. Along with the event type, it carries the list of avatar stickers
 * that are now considered recent so that sticker pickers on other devices
 * can refresh their recent-avatar row.
 */
@ProtobufMessage(name = "SyncActionValue.AvatarUpdatedAction")
public final class AvatarUpdatedAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * The app-state action name that identifies this action type on the wire.
     */
    public static final String ACTION_NAME = "avatar_updated_action";

    /**
     * The app-state action version that identifies this action revision on the
     * wire.
     */
    public static final int ACTION_VERSION = 7;

    /**
     * The app-state collection that stores this action type.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR;

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
     * The kind of avatar change this action represents (create, update, or
     * delete).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    AvatarEventType eventType;

    /**
     * The current recent-avatar-sticker set that should replace the one
     * cached on receiving devices.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<StickerAction> recentAvatarStickers;


    /**
     * Constructs a new {@code AvatarUpdatedAction} carrying the supplied
     * event type and recent avatar stickers.
     *
     * @param eventType            the kind of avatar change
     * @param recentAvatarStickers the current recent-avatar-sticker set
     */
    AvatarUpdatedAction(AvatarEventType eventType, List<StickerAction> recentAvatarStickers) {
        this.eventType = eventType;
        this.recentAvatarStickers = recentAvatarStickers;
    }

    /**
     * Returns the kind of avatar change this action represents.
     *
     * @return the event type, or {@link Optional#empty()} if unset
     */
    public Optional<AvatarEventType> eventType() {
        return Optional.ofNullable(eventType);
    }

    /**
     * Returns the current recent-avatar-sticker set carried by this action.
     *
     * @return an unmodifiable view of the recent avatar stickers, never {@code null}
     */
    public List<StickerAction> recentAvatarStickers() {
        return recentAvatarStickers == null ? List.of() : Collections.unmodifiableList(recentAvatarStickers);
    }

    /**
     * Sets the kind of avatar change this action represents.
     *
     * @param eventType the new event type, or {@code null} to clear it
     */
    public void setEventType(AvatarEventType eventType) {
        this.eventType = eventType;
    }

    /**
     * Sets the current recent-avatar-sticker set carried by this action.
     *
     * @param recentAvatarStickers the new recent-avatar-sticker set, or
     *                             {@code null} to clear it
     */
    public void setRecentAvatarStickers(List<StickerAction> recentAvatarStickers) {
        this.recentAvatarStickers = recentAvatarStickers;
    }

    /**
     * The kind of change an {@link AvatarUpdatedAction} represents.
     */
    @ProtobufEnum(name = "SyncActionValue.AvatarUpdatedAction.AvatarEventType")
    public static enum AvatarEventType {
        /**
         * The user modified an existing avatar.
         */
        UPDATED(0),
        /**
         * The user created a new avatar for the first time.
         */
        CREATED(1),
        /**
         * The user removed their existing avatar.
         */
        DELETED(2);

        /**
         * Constructs a new {@code AvatarEventType} constant with the supplied
         * wire index.
         *
         * @param index the protobuf wire index
         */
        AvatarEventType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index for this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index for this constant.
         *
         * @return the wire index
         */
        public int index() {
            return this.index;
        }
    }
}
