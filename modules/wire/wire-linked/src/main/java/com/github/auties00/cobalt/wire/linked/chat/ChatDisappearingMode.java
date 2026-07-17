package com.github.auties00.cobalt.wire.linked.chat;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Describes how disappearing messages were enabled in a WhatsApp chat and who
 * initiated the change.
 *
 * <p>When disappearing messages are turned on in a conversation, WhatsApp records
 * not only the ephemeral timer duration (see {@link ChatEphemeralTimer}) but also
 * contextual information about the change itself: who initiated it, what triggered
 * the change (per-chat setting, account-wide default, bulk update, or a business
 * hosting migration), and which device performed the action. This class is the
 * protobuf message (wire name {@code DisappearingMode}) that carries this context.
 *
 * <p>The disappearing mode is stored on the {@link Chat} record and is included in
 * history sync payloads so that companion devices can display accurate attribution
 * for the current ephemeral setting.
 */
@ProtobufMessage(name = "DisappearingMode")
public final class ChatDisappearingMode {
    /**
     * Identifies who initiated the disappearing messages change (the current
     * user, another participant, or an in-chat toggle).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    Initiator initiator;

    /**
     * Identifies what triggered the disappearing messages change (a per-chat
     * setting, an account-wide default, a bulk change, a business hosting
     * migration, or an unknown groups trigger).
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    Trigger trigger;

    /**
     * The JID of the device that initiated the change, if available. This
     * allows identifying the specific companion device (phone, desktop, or
     * web) that toggled the setting.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    Jid initiatorDeviceJid;

    /**
     * Whether the current user is the one who initiated the disappearing
     * messages change.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    Boolean initiatedByMe;

    /**
     * Constructs a new {@code ChatDisappearingMode} with the specified values.
     *
     * @param initiator        the initiator role, or {@code null}
     * @param trigger          the trigger type, or {@code null}
     * @param initiatorDeviceJid the JID of the initiating device, or {@code null}
     * @param initiatedByMe    whether the current user initiated the change, or {@code null}
     */
    ChatDisappearingMode(Initiator initiator, Trigger trigger, Jid initiatorDeviceJid, Boolean initiatedByMe) {
        this.initiator = initiator;
        this.trigger = trigger;
        this.initiatorDeviceJid = initiatorDeviceJid;
        this.initiatedByMe = initiatedByMe;
    }

    /**
     * Returns who initiated the disappearing messages change.
     *
     * @return an {@link Optional} containing the initiator, or empty if not set
     */
    public Optional<Initiator> initiator() {
        return Optional.ofNullable(initiator);
    }

    /**
     * Returns what triggered the disappearing messages change.
     *
     * @return an {@link Optional} containing the trigger, or empty if not set
     */
    public Optional<Trigger> trigger() {
        return Optional.ofNullable(trigger);
    }

    /**
     * Returns the JID of the device that initiated the change.
     *
     * @return an {@link Optional} containing the device JID, or empty if not
     *         available
     */
    public Optional<Jid> initiatorDeviceJid() {
        return Optional.ofNullable(initiatorDeviceJid);
    }

    /**
     * Returns whether the current user initiated the disappearing messages change.
     *
     * @return {@code true} if the current user initiated the change,
     *         {@code false} if another participant initiated it or the value is unset
     */
    public boolean initiatedByMe() {
        return initiatedByMe != null && initiatedByMe;
    }

    /**
     * Sets who initiated the disappearing messages change.
     *
     * @param initiator the initiator, or {@code null} to clear
     */
    public void setInitiator(Initiator initiator) {
        this.initiator = initiator;
    }

    /**
     * Sets the trigger for the disappearing messages change.
     *
     * @param trigger the trigger, or {@code null} to clear
     */
    public void setTrigger(Trigger trigger) {
        this.trigger = trigger;
    }

    /**
     * Sets the JID of the device that initiated the change.
     *
     * @param initiatorDeviceJid the device JID, or {@code null} to clear
     */
    public void setInitiatorDeviceJid(Jid initiatorDeviceJid) {
        this.initiatorDeviceJid = initiatorDeviceJid;
    }

    /**
     * Sets whether the current user initiated the disappearing messages change.
     *
     * @param initiatedByMe {@code true} if initiated by the current user,
     *                      or {@code null} to clear
     */
    public void setInitiatedByMe(Boolean initiatedByMe) {
        this.initiatedByMe = initiatedByMe;
    }

    /**
     * Identifies the role of the participant who initiated a disappearing messages
     * change in a WhatsApp chat.
     *
     * <p>This enum distinguishes between changes made directly in the chat settings
     * versus changes attributed to a specific participant (the current user or another
     * member). The {@link #BIZ_UPGRADE_FB_HOSTING} value covers transitions triggered
     * automatically when a business account migrates to Facebook hosting.
     */
    @ProtobufEnum(name = "DisappearingMode.Initiator")
    public static enum Initiator {
        /**
         * The setting was changed via the in-chat toggle, without attributing
         * the change to a specific participant.
         */
        CHANGED_IN_CHAT(0),

        /**
         * The current user initiated the disappearing messages change.
         */
        INITIATED_BY_ME(1),

        /**
         * Another participant in the chat initiated the change.
         */
        INITIATED_BY_OTHER(2),

        /**
         * The change was triggered automatically as part of a business account
         * migration to Facebook hosting.
         */
        BIZ_UPGRADE_FB_HOSTING(3);

        /**
         * Constructs an {@code Initiator} with the specified protobuf index.
         *
         * @param index the protobuf enum index
         */
        Initiator(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf-assigned index for this enum constant.
         */
        final int index;

        /**
         * Returns the protobuf index of this initiator type.
         *
         * @return the integer index used for wire serialization
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Identifies what mechanism triggered a disappearing messages change in a
     * WhatsApp chat.
     *
     * <p>The trigger captures the context of the change: whether it was a manual
     * per-chat setting, an account-wide default, a bulk update across multiple
     * chats, a business hosting migration, or an unknown groups-related trigger.
     */
    @ProtobufEnum(name = "DisappearingMode.Trigger")
    public static enum Trigger {
        /**
         * The trigger is unknown or unspecified.
         */
        UNKNOWN(0),

        /**
         * The change was made via the per-chat disappearing messages setting.
         */
        CHAT_SETTING(1),

        /**
         * The change was applied via the account-wide default disappearing
         * messages setting in the privacy settings.
         */
        ACCOUNT_SETTING(2),

        /**
         * The change was applied as part of a bulk update across multiple
         * chats (for example, when the user enables a default timer that
         * applies to all existing chats).
         */
        BULK_CHANGE(3),

        /**
         * The change was triggered automatically when a business chat
         * transitioned to Facebook hosting.
         */
        BIZ_SUPPORTS_FB_HOSTING(4),

        /**
         * The change was triggered by an unknown groups-related mechanism.
         */
        UNKNOWN_GROUPS(5);

        /**
         * Constructs a {@code Trigger} with the specified protobuf index.
         *
         * @param index the protobuf enum index
         */
        Trigger(@ProtobufEnumIndex int index) {
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
