package com.github.auties00.cobalt.wire.linked.sync.action.chat;

import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import java.time.Instant;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Sync action that mutes or unmutes a chat for the current account.
 *
 * <p>Muting suppresses notifications for incoming messages until an optional
 * mute-end timestamp elapses. WhatsApp supports two flavours of mute that
 * can coexist:
 * <ul>
 *   <li>a regular mute controlled by {@code muted} and bounded by
 *       {@code muteEndTimestamp};</li>
 *   <li>an automatic mute for chats the user has not interacted with
 *       recently, flagged by {@code autoMuted};</li>
 *   <li>a dedicated mention-mute that only silences
 *       "mention everyone" alerts, bounded by
 *       {@code muteEveryoneMentionEndTimestamp}.</li>
 * </ul>
 * All four fields are replicated together across linked devices so that the
 * notification state stays consistent.
 */
@ProtobufMessage(name = "SyncActionValue.MuteAction")
public final class MuteAction implements SyncAction<MuteActionArgs> {
    /**
     * Canonical action name written as the first element of the sync index.
     */
    public static final String ACTION_NAME = "mute";

    /**
     * Schema version declared by this action.
     */
    public static final int ACTION_VERSION = 2;

    /**
     * Collection this action is stored in when encoded into an app state patch.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_HIGH;

    /**
     * Returns the canonical action name for this action type.
     *
     * @return the string {@code "mute"}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version of this action type.
     *
     * @return the integer value {@code 2}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Whether the chat is currently muted ({@code true}) or unmuted
     * ({@code false}). {@code null} is treated as {@code false}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean muted;

    /**
     * Optional timestamp marking when the mute expires, serialised in
     * milliseconds via {@link InstantMillisMixin}. An absent value typically
     * represents a mute with no scheduled end.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant muteEndTimestamp;

    /**
     * Whether the chat was auto-muted by WhatsApp (for example because the
     * user has not interacted with it). {@code null} is treated as
     * {@code false}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    Boolean autoMuted;

    /**
     * Optional timestamp marking when a dedicated "mute mentions of
     * everyone" window expires, serialised in milliseconds via
     * {@link InstantMillisMixin}.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant muteEveryoneMentionEndTimestamp;


    /**
     * Constructs a new {@code MuteAction} with the given fields.
     *
     * @param muted                            whether the chat is muted
     * @param muteEndTimestamp                 optional regular mute end
     *                                         timestamp
     * @param autoMuted                        whether the chat was auto-muted
     * @param muteEveryoneMentionEndTimestamp  optional mention-mute end
     *                                         timestamp
     */
    MuteAction(Boolean muted, Instant muteEndTimestamp, Boolean autoMuted, Instant muteEveryoneMentionEndTimestamp) {
        this.muted = muted;
        this.muteEndTimestamp = muteEndTimestamp;
        this.autoMuted = autoMuted;
        this.muteEveryoneMentionEndTimestamp = muteEveryoneMentionEndTimestamp;
    }

    /**
     * Returns whether the chat is muted.
     *
     * <p>If the underlying field is {@code null} this method returns
     * {@code false}.
     *
     * @return {@code true} when the chat is muted, {@code false} otherwise
     */
    public boolean muted() {
        return muted != null && muted;
    }

    /**
     * Returns the regular mute end timestamp.
     *
     * @return an {@link Optional} containing the mute-end {@link Instant},
     *         or an empty {@code Optional} when the mute has no scheduled end
     */
    public Optional<Instant> muteEndTimestamp() {
        return Optional.ofNullable(muteEndTimestamp);
    }

    /**
     * Returns whether the chat was auto-muted by WhatsApp.
     *
     * <p>If the underlying field is {@code null} this method returns
     * {@code false}.
     *
     * @return {@code true} when the mute was set automatically by WhatsApp
     */
    public boolean autoMuted() {
        return autoMuted != null && autoMuted;
    }

    /**
     * Returns the end timestamp of the "mute everyone mentions" window.
     *
     * @return an {@link Optional} containing the mention-mute end
     *         {@link Instant}, or an empty {@code Optional} when no such
     *         window is active
     */
    public Optional<Instant> muteEveryoneMentionEndTimestamp() {
        return Optional.ofNullable(muteEveryoneMentionEndTimestamp);
    }

    /**
     * Sets whether the chat is muted.
     *
     * @param muted the new muted flag, or {@code null} to clear it
     */
    public void setMuted(Boolean muted) {
        this.muted = muted;
    }

    /**
     * Sets the regular mute end timestamp.
     *
     * @param muteEndTimestamp the new end timestamp, or {@code null} to clear
     */
    public void setMuteEndTimestamp(Instant muteEndTimestamp) {
        this.muteEndTimestamp = muteEndTimestamp;
    }

    /**
     * Sets the auto-muted flag.
     *
     * @param autoMuted the new auto-muted flag, or {@code null} to clear it
     */
    public void setAutoMuted(Boolean autoMuted) {
        this.autoMuted = autoMuted;
    }

    /**
     * Sets the "mute everyone mentions" end timestamp.
     *
     * @param muteEveryoneMentionEndTimestamp the new end timestamp, or
     *                                        {@code null} to clear it
     */
    public void setMuteEveryoneMentionEndTimestamp(Instant muteEveryoneMentionEndTimestamp) {
        this.muteEveryoneMentionEndTimestamp = muteEveryoneMentionEndTimestamp;
    }


}
