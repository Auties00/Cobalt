package com.github.auties00.cobalt.wire.linked.setting;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Holds the user preferences that control notifications for a class of chats.
 *
 * <p>WhatsApp stores two independent profiles of this settings object, one
 * applied to direct (one-to-one) chats and one applied to group chats, so
 * that the user can tune visual, tactile and auditory feedback differently
 * depending on the chat type. The string fields carry implementation-defined
 * identifiers (for example the name of a vibration pattern or the ring-tone
 * filename) that are meaningful to the official clients.
 *
 * @see GlobalSettings
 */
@ProtobufMessage(name = "NotificationSettings")
public final class NotificationSettings {
    /**
     * Identifier of the vibration pattern played when a new message arrives.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String messageVibrate;

    /**
     * Identifier of the pop-up style shown when a new message arrives.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String messagePopup;

    /**
     * Identifier of the LED colour or animation shown when a new message arrives.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String messageLight;

    /**
     * Whether notifications from these chats are delivered at low priority,
     * typically silencing them on the lock screen.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    Boolean lowPriorityNotifications;

    /**
     * Whether reaction notifications from these chats are muted.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    Boolean reactionsMuted;

    /**
     * Identifier of the vibration pattern played when an incoming call is received.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String callVibrate;


    /**
     * Constructs a new notification settings instance with the given values.
     *
     * @param messageVibrate           the message vibration pattern identifier, may be {@code null}
     * @param messagePopup             the message pop-up style identifier, may be {@code null}
     * @param messageLight             the message light identifier, may be {@code null}
     * @param lowPriorityNotifications whether to use low priority notifications, may be {@code null}
     * @param reactionsMuted           whether reaction notifications are muted, may be {@code null}
     * @param callVibrate              the call vibration pattern identifier, may be {@code null}
     */
    NotificationSettings(String messageVibrate, String messagePopup, String messageLight, Boolean lowPriorityNotifications, Boolean reactionsMuted, String callVibrate) {
        this.messageVibrate = messageVibrate;
        this.messagePopup = messagePopup;
        this.messageLight = messageLight;
        this.lowPriorityNotifications = lowPriorityNotifications;
        this.reactionsMuted = reactionsMuted;
        this.callVibrate = callVibrate;
    }

    /**
     * Returns the identifier of the vibration pattern for incoming messages.
     *
     * @return an {@link Optional} containing the identifier, or empty if not set
     */
    public Optional<String> messageVibrate() {
        return Optional.ofNullable(messageVibrate);
    }

    /**
     * Returns the identifier of the pop-up style for incoming messages.
     *
     * @return an {@link Optional} containing the identifier, or empty if not set
     */
    public Optional<String> messagePopup() {
        return Optional.ofNullable(messagePopup);
    }

    /**
     * Returns the identifier of the light or LED animation for incoming messages.
     *
     * @return an {@link Optional} containing the identifier, or empty if not set
     */
    public Optional<String> messageLight() {
        return Optional.ofNullable(messageLight);
    }

    /**
     * Returns whether notifications are delivered at low priority.
     *
     * <p>A missing value is interpreted as {@code false}.
     *
     * @return {@code true} if notifications are low priority, {@code false} otherwise
     */
    public boolean lowPriorityNotifications() {
        return lowPriorityNotifications != null && lowPriorityNotifications;
    }

    /**
     * Returns whether reaction notifications are muted.
     *
     * <p>A missing value is interpreted as {@code false}.
     *
     * @return {@code true} if reactions are muted, {@code false} otherwise
     */
    public boolean reactionsMuted() {
        return reactionsMuted != null && reactionsMuted;
    }

    /**
     * Returns the identifier of the vibration pattern for incoming calls.
     *
     * @return an {@link Optional} containing the identifier, or empty if not set
     */
    public Optional<String> callVibrate() {
        return Optional.ofNullable(callVibrate);
    }

    /**
     * Updates the vibration pattern identifier for incoming messages.
     *
     * @param messageVibrate the new identifier, or {@code null} to unset the field
     */
    public void setMessageVibrate(String messageVibrate) {
        this.messageVibrate = messageVibrate;
    }

    /**
     * Updates the pop-up style identifier for incoming messages.
     *
     * @param messagePopup the new identifier, or {@code null} to unset the field
     */
    public void setMessagePopup(String messagePopup) {
        this.messagePopup = messagePopup;
    }

    /**
     * Updates the light or LED animation identifier for incoming messages.
     *
     * @param messageLight the new identifier, or {@code null} to unset the field
     */
    public void setMessageLight(String messageLight) {
        this.messageLight = messageLight;
    }

    /**
     * Updates the low-priority notification flag.
     *
     * @param lowPriorityNotifications the new value, or {@code null} to unset the field
     */
    public void setLowPriorityNotifications(Boolean lowPriorityNotifications) {
        this.lowPriorityNotifications = lowPriorityNotifications;
    }

    /**
     * Updates the reactions-muted flag.
     *
     * @param reactionsMuted the new value, or {@code null} to unset the field
     */
    public void setReactionsMuted(Boolean reactionsMuted) {
        this.reactionsMuted = reactionsMuted;
    }

    /**
     * Updates the vibration pattern identifier for incoming calls.
     *
     * @param callVibrate the new identifier, or {@code null} to unset the field
     */
    public void setCallVibrate(String callVibrate) {
        this.callVibrate = callVibrate;
    }
}
