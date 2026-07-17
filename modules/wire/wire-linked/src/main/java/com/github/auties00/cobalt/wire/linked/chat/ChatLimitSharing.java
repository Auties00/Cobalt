package com.github.auties00.cobalt.wire.linked.chat;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;

/**
 * Represents the "limit sharing" privacy setting for a WhatsApp chat.
 *
 * <p>When limit sharing is enabled on a conversation, the client restricts what
 * personal information (such as profile photo, about, and status) is shared with
 * the other participant. This feature is typically used in business-to-consumer
 * conversations or in chats where the user wants to limit their exposure to
 * unknown contacts.
 *
 * <p>This class is a protobuf message (wire name {@code LimitSharing}) that can
 * appear both on the {@link Chat} record (as individual fields) and inside
 * {@link ChatMessageContextInfo} as a structured object. It records whether sharing
 * is limited, what triggered the restriction, when it was applied, and whether the
 * current user initiated it.
 */
@ProtobufMessage(name = "LimitSharing")
public final class ChatLimitSharing {
    /**
     * Whether sharing is currently limited in this chat. When {@code true},
     * personal information is restricted from the other participant.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean sharingLimited;

    /**
     * The event or mechanism that triggered the limit sharing state.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    TriggerType trigger;

    /**
     * The timestamp (in epoch seconds) at which the limit sharing setting
     * was last changed.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant limitSharingSettingTimestamp;

    /**
     * Whether the current user is the one who enabled or disabled limit
     * sharing in this chat.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    Boolean initiatedByMe;

    /**
     * Constructs a new {@code ChatLimitSharing} with the specified values.
     *
     * @param sharingLimited               whether sharing is limited
     * @param trigger                      the trigger type
     * @param limitSharingSettingTimestamp  when the setting was changed
     * @param initiatedByMe                whether the current user initiated it
     */
    ChatLimitSharing(Boolean sharingLimited, TriggerType trigger, Instant limitSharingSettingTimestamp, Boolean initiatedByMe) {
        this.sharingLimited = sharingLimited;
        this.trigger = trigger;
        this.limitSharingSettingTimestamp = limitSharingSettingTimestamp;
        this.initiatedByMe = initiatedByMe;
    }

    /**
     * Returns whether sharing is currently limited in this chat.
     *
     * @return {@code true} if personal information sharing is restricted,
     *         {@code false} otherwise
     */
    public boolean sharingLimited() {
        return sharingLimited != null && sharingLimited;
    }

    /**
     * Returns the mechanism that triggered the limit sharing state.
     *
     * @return an {@link Optional} containing the trigger type, or empty if
     *         not set
     */
    public Optional<TriggerType> trigger() {
        return Optional.ofNullable(trigger);
    }

    /**
     * Returns the timestamp at which the limit sharing setting was last changed.
     *
     * @return an {@link Optional} containing the timestamp, or empty if not set
     */
    public Optional<Instant> limitSharingSettingTimestamp() {
        return Optional.ofNullable(limitSharingSettingTimestamp);
    }

    /**
     * Returns whether the current user initiated the limit sharing change.
     *
     * @return {@code true} if the current user initiated it, {@code false}
     *         otherwise
     */
    public boolean initiatedByMe() {
        return initiatedByMe != null && initiatedByMe;
    }

    /**
     * Sets whether sharing is limited in this chat.
     *
     * @param sharingLimited {@code true} to restrict sharing, or {@code null}
     *                       to clear
     */
    public void setSharingLimited(Boolean sharingLimited) {
        this.sharingLimited = sharingLimited;
    }

    /**
     * Sets the trigger type for the limit sharing state.
     *
     * @param trigger the trigger type, or {@code null} to clear
     */
    public void setTrigger(TriggerType trigger) {
        this.trigger = trigger;
    }

    /**
     * Sets the timestamp at which the limit sharing setting was changed.
     *
     * @param limitSharingSettingTimestamp the timestamp, or {@code null} to clear
     */
    public void setLimitSharingSettingTimestamp(Instant limitSharingSettingTimestamp) {
        this.limitSharingSettingTimestamp = limitSharingSettingTimestamp;
    }

    /**
     * Sets whether the current user initiated the limit sharing change.
     *
     * @param initiatedByMe {@code true} if initiated by the current user,
     *                      or {@code null} to clear
     */
    public void setInitiatedByMe(Boolean initiatedByMe) {
        this.initiatedByMe = initiatedByMe;
    }

    /**
     * Identifies the mechanism that triggered a "limit sharing" state change
     * in a WhatsApp chat.
     */
    @ProtobufEnum(name = "LimitSharing.TriggerType")
    public enum TriggerType {
        /**
         * The trigger is unknown or unspecified.
         */
        UNKNOWN(0),

        /**
         * The user manually toggled the limit sharing option in the per-chat
         * privacy settings.
         */
        CHAT_SETTING(1),

        /**
         * Limit sharing was enabled automatically because the business
         * account supports Facebook hosting and the corresponding privacy
         * policy applies.
         */
        BIZ_SUPPORTS_FB_HOSTING(2),

        /**
         * The trigger is related to an unknown group-level mechanism.
         */
        UNKNOWN_GROUP(3);

        /**
         * Constructs a {@code TriggerType} with the specified protobuf index.
         *
         * @param index the protobuf enum index
         */
        TriggerType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf-assigned index for this enum constant.
         */
        final int index;

        /**
         * Returns the protobuf index of this trigger type.
         *
         * @return the integer index used for wire serialization
         */
        public int index() {
            return this.index;
        }
    }
}
