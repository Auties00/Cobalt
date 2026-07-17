package com.github.auties00.cobalt.wire.linked.message.call;

import com.github.auties00.cobalt.wire.linked.message.Message;

import java.time.Instant;

import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A message that announces the creation of a scheduled call inside a chat.
 *
 * <p>Scheduled calls let WhatsApp users plan a voice or video call ahead of time. When a user
 * creates a scheduled call, this message is posted in the target conversation so that the other
 * participants receive a notification and can mark the event on their calendar. The announcement
 * carries the scheduled start time, the chosen media type and an optional human readable title.
 *
 * <p>The actual call is started separately at the scheduled time; this message is only the
 * announcement. Subsequent lifecycle updates, such as cancellations, travel as
 * {@link ScheduledCallEditMessage} instances that reference the creation message by key.
 */
@ProtobufMessage(name = "Message.ScheduledCallCreationMessage")
public final class ScheduledCallCreationMessage implements Message {
    /**
     * Moment at which the scheduled call is planned to start.
     *
     * <p>The wire format stores the value as epoch milliseconds; Cobalt exposes it as an
     * {@link Instant} through the {@link InstantMillisMixin}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant scheduledTimestampMs;

    /**
     * Media type planned for the scheduled call.
     *
     * <p>Indicates whether the planned call is a voice only call or a video call.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    CallType callType;

    /**
     * Human readable title assigned to the scheduled call.
     *
     * <p>Displayed to participants in the chat and in the call details screen, typically used to
     * summarise the purpose of the meeting.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String title;

    /**
     * Constructs a new scheduled call creation announcement.
     *
     * <p>This constructor is package-private: use the generated
     * {@code ScheduledCallCreationMessageBuilder} to create instances.
     *
     * @param scheduledTimestampMs the scheduled start time, may be {@code null}
     * @param callType             the planned media type, may be {@code null}
     * @param title                the human readable title, may be {@code null}
     */
    ScheduledCallCreationMessage(Instant scheduledTimestampMs, CallType callType, String title) {
        this.scheduledTimestampMs = scheduledTimestampMs;
        this.callType = callType;
        this.title = title;
    }

    /**
     * Returns the moment at which the scheduled call is planned to start.
     *
     * @return an {@link Optional} containing the start {@link Instant}, or empty if not set
     */
    public Optional<Instant> scheduledTimestampMs() {
        return Optional.ofNullable(scheduledTimestampMs);
    }

    /**
     * Returns the media type planned for the scheduled call.
     *
     * @return an {@link Optional} containing the {@link CallType}, or empty if not set
     */
    public Optional<CallType> callType() {
        return Optional.ofNullable(callType);
    }

    /**
     * Returns the human readable title assigned to the scheduled call.
     *
     * @return an {@link Optional} containing the title, or empty if not set
     */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /**
     * Sets the moment at which the scheduled call is planned to start.
     *
     * @param scheduledTimestampMs the planned start {@link Instant}, or {@code null} to clear the field
     */
    public void setScheduledTimestampMs(Instant scheduledTimestampMs) {
        this.scheduledTimestampMs = scheduledTimestampMs;
    }

    /**
     * Sets the media type planned for the scheduled call.
     *
     * @param callType the {@link CallType}, or {@code null} to clear the field
     */
    public void setCallType(CallType callType) {
        this.callType = callType;
    }

    /**
     * Sets the human readable title assigned to the scheduled call.
     *
     * @param title the title, or {@code null} to clear the field
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Describes the kind of media planned for a scheduled call.
     *
     * <p>Used by {@link ScheduledCallCreationMessage} to announce whether the upcoming call is
     * intended to be voice only or to include video.
     */
    @ProtobufEnum(name = "Message.ScheduledCallCreationMessage.CallType")
    public static enum CallType {
        /**
         * The media type for the scheduled call is not specified.
         */
        UNKNOWN(0),
        /**
         * The scheduled call is intended to be voice only.
         */
        VOICE(1),
        /**
         * The scheduled call is intended to include video.
         */
        VIDEO(2);

        /**
         * Creates a call type constant with the given protobuf index.
         *
         * @param index the wire value assigned to this constant by the protobuf schema
         */
        CallType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Wire value assigned to this constant by the protobuf schema.
         */
        final int index;

        /**
         * Returns the wire value of this constant.
         *
         * @return the protobuf index of this enum constant
         */
        public int index() {
            return this.index;
        }
    }
}
