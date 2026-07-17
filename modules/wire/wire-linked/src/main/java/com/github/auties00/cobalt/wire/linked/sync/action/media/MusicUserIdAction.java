package com.github.auties00.cobalt.wire.linked.sync.action.media;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;

import java.util.Collections;
import java.util.Map;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A sync action that propagates the user's music streaming service identifier
 * across linked devices.
 *
 * <p>The action carries both a primary music user identifier and a map of
 * provider-specific identifiers so that the music linking state established
 * on one device becomes visible to every other device linked to the same
 * account. Consumers typically use the action to render the user's connected
 * music provider inside profile and sharing UIs.
 */
@ProtobufMessage(name = "SyncActionValue.MusicUserIdAction")
public final class MusicUserIdAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * The app-state action name that identifies this action type on the wire.
     */
    public static final String ACTION_NAME = "music_user_id";

    /**
     * The app-state action version that identifies this action revision on the
     * wire.
     */
    public static final int ACTION_VERSION = 1;

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
     * The user's primary music user identifier.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String musicUserId;

    /**
     * Map of music provider identifiers (service name to user identifier) that
     * supplement the primary {@link #musicUserId} field.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.STRING)
    Map<String, String> musicUserIdMap;


    /**
     * Constructs a new {@code MusicUserIdAction} carrying the supplied primary
     * music identifier and provider identifier map.
     *
     * @param musicUserId    the primary music user identifier, or {@code null} if unset
     * @param musicUserIdMap the provider identifier map, or {@code null} if unset
     */
    MusicUserIdAction(String musicUserId, Map<String, String> musicUserIdMap) {
        this.musicUserId = musicUserId;
        this.musicUserIdMap = musicUserIdMap;
    }

    /**
     * Returns the primary music user identifier carried by this action.
     *
     * @return the music user identifier, or {@link Optional#empty()} if unset
     */
    public Optional<String> musicUserId() {
        return Optional.ofNullable(musicUserId);
    }

    /**
     * Returns the provider identifier map carried by this action.
     *
     * @return an unmodifiable view of the provider identifier map, never {@code null}
     */
    public Map<String, String> musicUserIdMap() {
        return musicUserIdMap == null ? Map.of() : Collections.unmodifiableMap(musicUserIdMap);
    }

    /**
     * Sets the primary music user identifier carried by this action.
     *
     * @param musicUserId the new music user identifier, or {@code null} to clear it
     */
    public void setMusicUserId(String musicUserId) {
        this.musicUserId = musicUserId;
    }

    /**
     * Sets the provider identifier map carried by this action.
     *
     * @param musicUserIdMap the new provider identifier map, or {@code null} to clear it
     */
    public void setMusicUserIdMap(Map<String, String> musicUserIdMap) {
        this.musicUserIdMap = musicUserIdMap;
    }
}
