package com.github.auties00.cobalt.wire.linked.bot.feedback;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;

/**
 * Carries metadata for an AI bot reminder that the user has created, updated,
 * or that is being triggered.
 *
 * <p>Users can ask Meta AI to set reminders (for example, "Remind me to call Mom
 * tomorrow at 5 PM"). This metadata captures the reminder's
 * {@linkplain #name() human-readable name}, the {@linkplain #action() lifecycle action}
 * being performed ({@link ReminderAction#CREATE CREATE},
 * {@link ReminderAction#UPDATE UPDATE}, {@link ReminderAction#DELETE DELETE}, or
 * {@link ReminderAction#NOTIFY NOTIFY}), the
 * {@linkplain #nextTriggerTimestamp() next trigger time}, and the
 * {@linkplain #frequency() recurrence frequency}.
 *
 * <p>The {@linkplain #requestMessageKey() request message key} links this metadata
 * back to the original user message that initiated the reminder request.
 */
@ProtobufMessage(name = "BotReminderMetadata")
public final class BotReminderMetadata {
    /**
     * The key of the message that originally requested this reminder.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey requestMessageKey;

    /**
     * The action being performed on this reminder.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    ReminderAction action;

    /**
     * The human-readable name of the reminder, for example
     * {@code "Call Mom"}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String name;

    /**
     * The timestamp at which this reminder will next trigger.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant nextTriggerTimestamp;

    /**
     * The recurrence frequency of this reminder.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
    ReminderFrequency frequency;


    /**
     * Constructs a new {@code BotReminderMetadata} with the specified values.
     *
     * @param requestMessageKey    the key of the requesting message, or {@code null}
     * @param action               the reminder action, or {@code null}
     * @param name                 the reminder name, or {@code null}
     * @param nextTriggerTimestamp the next trigger timestamp, or {@code null}
     * @param frequency            the recurrence frequency, or {@code null}
     */
    BotReminderMetadata(MessageKey requestMessageKey, ReminderAction action, String name, Instant nextTriggerTimestamp, ReminderFrequency frequency) {
        this.requestMessageKey = requestMessageKey;
        this.action = action;
        this.name = name;
        this.nextTriggerTimestamp = nextTriggerTimestamp;
        this.frequency = frequency;
    }

    /**
     * Returns the key of the message that originally requested this reminder.
     *
     * @return an {@code Optional} describing the request message key, or an empty
     *         {@code Optional} if not set
     */
    public Optional<MessageKey> requestMessageKey() {
        return Optional.ofNullable(requestMessageKey);
    }

    /**
     * Returns the action being performed on this reminder.
     *
     * @return an {@code Optional} describing the action, or an empty
     *         {@code Optional} if not set
     */
    public Optional<ReminderAction> action() {
        return Optional.ofNullable(action);
    }

    /**
     * Returns the human-readable name of the reminder.
     *
     * @return an {@code Optional} describing the name, or an empty
     *         {@code Optional} if not set
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the timestamp at which this reminder will next trigger.
     *
     * @return an {@code Optional} describing the next trigger timestamp, or an empty
     *         {@code Optional} if not set
     */
    public Optional<Instant> nextTriggerTimestamp() {
        return Optional.ofNullable(nextTriggerTimestamp);
    }

    /**
     * Returns the recurrence frequency of this reminder.
     *
     * @return an {@code Optional} describing the frequency, or an empty
     *         {@code Optional} if not set
     */
    public Optional<ReminderFrequency> frequency() {
        return Optional.ofNullable(frequency);
    }

    /**
     * Sets the key of the message that originally requested this reminder.
     *
     * @param requestMessageKey the new request message key, or {@code null}
     */
    public void setRequestMessageKey(MessageKey requestMessageKey) {
        this.requestMessageKey = requestMessageKey;
    }

    /**
     * Sets the action being performed on this reminder.
     *
     * @param action the new action, or {@code null}
     */
    public void setAction(ReminderAction action) {
        this.action = action;
    }

    /**
     * Sets the human-readable name of the reminder.
     *
     * @param name the new name, or {@code null}
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the timestamp at which this reminder will next trigger.
     *
     * @param nextTriggerTimestamp the new trigger timestamp, or {@code null}
     */
    public void setNextTriggerTimestamp(Instant nextTriggerTimestamp) {
        this.nextTriggerTimestamp = nextTriggerTimestamp;
    }

    /**
     * Sets the recurrence frequency of this reminder.
     *
     * @param frequency the new frequency, or {@code null}
     */
    public void setFrequency(ReminderFrequency frequency) {
        this.frequency = frequency;
    }

    /**
     * Enumerates the lifecycle actions that can be performed on an AI bot reminder.
     */
    @ProtobufEnum(name = "BotReminderMetadata.ReminderAction")
    public static enum ReminderAction {
        /**
         * The reminder is firing and notifying the user.
         */
        NOTIFY(1),

        /**
         * A new reminder is being created.
         */
        CREATE(2),

        /**
         * An existing reminder is being deleted.
         */
        DELETE(3),

        /**
         * An existing reminder is being updated.
         */
        UPDATE(4);

        /**
         * Constructs a new {@code ReminderAction} with the specified protobuf index.
         *
         * @param index the protobuf index value
         */
        ReminderAction(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf index of this enum constant.
         */
        final int index;

        /**
         * Returns the protobuf index of this enum constant.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Enumerates the recurrence frequencies supported for AI bot reminders.
     */
    @ProtobufEnum(name = "BotReminderMetadata.ReminderFrequency")
    public static enum ReminderFrequency {
        /**
         * The reminder triggers only once.
         */
        ONCE(1),

        /**
         * The reminder triggers every day.
         */
        DAILY(2),

        /**
         * The reminder triggers every week.
         */
        WEEKLY(3),

        /**
         * The reminder triggers every two weeks.
         */
        BIWEEKLY(4),

        /**
         * The reminder triggers every month.
         */
        MONTHLY(5);

        /**
         * Constructs a new {@code ReminderFrequency} with the specified protobuf index.
         *
         * @param index the protobuf index value
         */
        ReminderFrequency(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf index of this enum constant.
         */
        final int index;

        /**
         * Returns the protobuf index of this enum constant.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }
}
