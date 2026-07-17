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
 * A sync action that propagates the user's list of favorite chats and contacts
 * across linked devices.
 *
 * <p>The action carries the ordered set of pinned favorites the user has
 * curated for quick access from the chat list. Updating the action replaces
 * the full favorite list: consumers should treat an incoming action as a
 * wholesale replacement of the previously known favorites.
 */
@ProtobufMessage(name = "SyncActionValue.FavoritesAction")
public final class FavoritesAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * The app-state action name that identifies this action type on the wire.
     */
    public static final String ACTION_NAME = "favorites";

    /**
     * The app-state action version that identifies this action revision on the
     * wire.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * The app-state collection that stores this action type.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_HIGH;

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
     * The ordered list of {@link Favorite} entries that compose the user's
     * curated favorites.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<Favorite> favorites;


    /**
     * Constructs a new {@code FavoritesAction} carrying the supplied favorites.
     *
     * @param favorites the ordered list of favorites, or {@code null} if unset
     */
    FavoritesAction(List<Favorite> favorites) {
        this.favorites = favorites;
    }

    /**
     * Returns the ordered list of favorites carried by this action.
     *
     * @return an unmodifiable view of the favorites, never {@code null}
     */
    public List<Favorite> favorites() {
        return favorites == null ? List.of() : Collections.unmodifiableList(favorites);
    }

    /**
     * Sets the ordered list of favorites carried by this action.
     *
     * @param favorites the new favorites list, or {@code null} to clear it
     */
    public void setFavorites(List<Favorite> favorites) {
        this.favorites = favorites;
    }

    /**
     * A single entry in the user's favorites list.
     *
     * <p>Each entry identifies a favorited target (typically a chat or contact)
     * by an opaque identifier assigned by WhatsApp.
     */
    @ProtobufMessage(name = "SyncActionValue.FavoritesAction.Favorite")
    public static final class Favorite {
        /**
         * The opaque identifier of the favorited target.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String id;


        /**
         * Constructs a new favorite entry for the supplied identifier.
         *
         * @param id the target identifier, or {@code null} if unset
         */
        Favorite(String id) {
            this.id = id;
        }

        /**
         * Returns the opaque identifier of the favorited target.
         *
         * @return the target identifier, or {@link Optional#empty()} if unset
         */
        public Optional<String> id() {
            return Optional.ofNullable(id);
        }

        /**
         * Sets the opaque identifier of the favorited target.
         *
         * @param id the new target identifier, or {@code null} to clear it
         */
        public void setId(String id) {
            this.id = id;
    }
    }
}
