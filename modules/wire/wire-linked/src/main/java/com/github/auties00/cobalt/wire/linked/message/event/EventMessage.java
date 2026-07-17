package com.github.auties00.cobalt.wire.linked.message.event;

import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;
import com.github.auties00.cobalt.wire.linked.message.location.LocationMessage;

import java.time.Instant;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Represents a WhatsApp event posted in a chat and open for RSVPs.
 *
 * <p>An event is a lightweight calendar entry that can be shared inside
 * one-to-one or group conversations. It describes a real or virtual gathering
 * (meetings, meetups, birthdays, scheduled calls, and so on) and invites
 * every participant of the chat to RSVP. Each event is defined by:
 * <ul>
 *   <li>A human-readable {@link #name() name} and optional
 *       {@link #description() description}.</li>
 *   <li>A {@link #startTime() start time} and optional
 *       {@link #endTime() end time} describing when the event takes place.</li>
 *   <li>An optional physical {@link #location() location} or virtual
 *       {@link #joinLink() join link} (for example a video call URL).</li>
 *   <li>Flags controlling RSVP behaviour, in particular whether guests may
 *       bring extra attendees ({@link #extraGuestsAllowed()}) and whether
 *       the entry represents a scheduled call
 *       ({@link #isScheduleCall()}).</li>
 *   <li>An optional reminder that fires before the event starts, controlled
 *       by {@link #hasReminder()} and {@link #reminderOffsetSec()}.</li>
 *   <li>A {@link #isCanceled() canceled} flag set when the organiser
 *       cancels the event without deleting the message.</li>
 * </ul>
 *
 * <p>Because {@code EventMessage} implements {@link ContextualMessage}, it
 * can carry {@link ContextInfo} metadata describing quoted messages,
 * mentions, forwarding history, and other context attached to the post.
 * Replies to the event are delivered as {@link EncEventResponseMessage}
 * payloads that the server aggregates into {@link EventResponse} entries.
 */
@ProtobufMessage(name = "Message.EventMessage")
public final class EventMessage implements ContextualMessage {
    /**
     * Optional context information attached to this event message.
     *
     * <p>Holds data such as quoted messages, mentions, forwarding history
     * and click-to-WhatsApp ad attribution. Absent when the event was
     * posted without any surrounding context.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    /**
     * Raw cancellation flag set by the organiser when an event is called off.
     *
     * <p>A value of {@code null} is treated as {@code false} by
     * {@link #isCanceled()}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    Boolean isCanceled;

    /**
     * The user-visible name of the event as entered by the organiser.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String name;

    /**
     * An optional free-form description of the event.
     *
     * <p>Typically used to provide agenda information, dress codes, or any
     * other details that do not fit into the structured fields.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String description;

    /**
     * An optional physical location associated with the event.
     *
     * <p>Encoded as a {@link LocationMessage} so that clients can display
     * the location inline (and, where supported, open it in a maps
     * application).
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    LocationMessage location;

    /**
     * An optional URL participants can follow to join the event remotely.
     *
     * <p>Typically a video conference link (for example Google Meet, Zoom,
     * or a WhatsApp call link) for virtual events.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String joinLink;

    /**
     * The moment at which the event starts.
     *
     * <p>Serialised on the wire as seconds since the Unix epoch via
     * {@link InstantSecondsMixin} and exposed as an {@link Instant}.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant startTime;

    /**
     * The moment at which the event ends, when the organiser specifies one.
     *
     * <p>Serialised on the wire as seconds since the Unix epoch via
     * {@link InstantSecondsMixin}. Absent for events that do not have a
     * defined end time.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant endTime;

    /**
     * Raw flag indicating whether responders may bring additional guests.
     *
     * <p>When {@code true}, an {@link EventResponseMessage} may specify a
     * non-negative extra guest count alongside the RSVP choice. A value of
     * {@code null} is treated as {@code false} by
     * {@link #extraGuestsAllowed()}.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.BOOL)
    Boolean extraGuestsAllowed;

    /**
     * Raw flag indicating whether the event represents a scheduled WhatsApp
     * call rather than a generic gathering.
     *
     * <p>A value of {@code null} is treated as {@code false} by
     * {@link #isScheduleCall()}.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.BOOL)
    Boolean isScheduleCall;

    /**
     * Raw flag indicating whether a reminder has been configured for the
     * event.
     *
     * <p>When {@code true}, the reminder fires {@link #reminderOffsetSec()}
     * seconds before {@link #startTime()}. A value of {@code null} is
     * treated as {@code false} by {@link #hasReminder()}.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
    Boolean hasReminder;

    /**
     * Number of seconds before the event start at which a reminder should
     * fire, when {@link #hasReminder} is {@code true}.
     *
     * <p>Ignored when {@link #hasReminder} is {@code false} or {@code null}.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.INT64)
    Long reminderOffsetSec;

    /**
     * Constructs a new {@code EventMessage} with the supplied event details.
     *
     * <p>The constructor is package-private. Application code should build
     * instances through the generated {@code EventMessageBuilder}.
     *
     * @param contextInfo         optional surrounding {@link ContextInfo}
     * @param isCanceled          raw cancellation flag, or {@code null}
     * @param name                user-visible event name, or {@code null}
     * @param description         optional free-form description
     * @param location            optional physical location as a
     *                            {@link LocationMessage}
     * @param joinLink            optional URL to join the event remotely
     * @param startTime           the event start {@link Instant}, or
     *                            {@code null}
     * @param endTime             the event end {@link Instant}, or
     *                            {@code null}
     * @param extraGuestsAllowed  raw flag controlling extra guests, or
     *                            {@code null}
     * @param isScheduleCall      raw flag marking this as a scheduled call,
     *                            or {@code null}
     * @param hasReminder         raw flag controlling the reminder, or
     *                            {@code null}
     * @param reminderOffsetSec   reminder lead time in seconds, or
     *                            {@code null}
     */
    EventMessage(ContextInfo contextInfo, Boolean isCanceled, String name, String description, LocationMessage location, String joinLink, Instant startTime, Instant endTime, Boolean extraGuestsAllowed, Boolean isScheduleCall, Boolean hasReminder, Long reminderOffsetSec) {
        this.contextInfo = contextInfo;
        this.isCanceled = isCanceled;
        this.name = name;
        this.description = description;
        this.location = location;
        this.joinLink = joinLink;
        this.startTime = startTime;
        this.endTime = endTime;
        this.extraGuestsAllowed = extraGuestsAllowed;
        this.isScheduleCall = isScheduleCall;
        this.hasReminder = hasReminder;
        this.reminderOffsetSec = reminderOffsetSec;
    }

    /**
     * Returns the context information attached to this event message.
     *
     * @return an {@link Optional} containing the {@link ContextInfo} when
     *         present, otherwise {@code Optional.empty()}
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Returns whether the organiser has cancelled this event.
     *
     * <p>A missing flag is treated as {@code false}.
     *
     * @return {@code true} when the event has been cancelled,
     *         {@code false} otherwise
     */
    public boolean isCanceled() {
        return isCanceled != null && isCanceled;
    }

    /**
     * Returns the user-visible name of the event.
     *
     * @return an {@link Optional} containing the event name, or
     *         {@code Optional.empty()} when none was supplied
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the free-form description of the event.
     *
     * @return an {@link Optional} containing the description, or
     *         {@code Optional.empty()} when none was supplied
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the physical location associated with the event.
     *
     * @return an {@link Optional} containing the {@link LocationMessage},
     *         or {@code Optional.empty()} when none was supplied
     */
    public Optional<LocationMessage> location() {
        return Optional.ofNullable(location);
    }

    /**
     * Returns the URL participants can follow to join the event remotely.
     *
     * @return an {@link Optional} containing the join link, or
     *         {@code Optional.empty()} when none was supplied
     */
    public Optional<String> joinLink() {
        return Optional.ofNullable(joinLink);
    }

    /**
     * Returns the moment at which the event starts.
     *
     * @return an {@link Optional} containing the start {@link Instant}, or
     *         {@code Optional.empty()} when none was supplied
     */
    public Optional<Instant> startTime() {
        return Optional.ofNullable(startTime);
    }

    /**
     * Returns the moment at which the event ends.
     *
     * @return an {@link Optional} containing the end {@link Instant}, or
     *         {@code Optional.empty()} when the event does not have a
     *         defined end time
     */
    public Optional<Instant> endTime() {
        return Optional.ofNullable(endTime);
    }

    /**
     * Returns whether responders may bring additional guests when RSVPing.
     *
     * <p>A missing flag is treated as {@code false}.
     *
     * @return {@code true} when extra guests are permitted, {@code false}
     *         otherwise
     */
    public boolean extraGuestsAllowed() {
        return extraGuestsAllowed != null && extraGuestsAllowed;
    }

    /**
     * Returns whether this event represents a scheduled WhatsApp call.
     *
     * <p>A missing flag is treated as {@code false}.
     *
     * @return {@code true} when the event is a scheduled call,
     *         {@code false} otherwise
     */
    public boolean isScheduleCall() {
        return isScheduleCall != null && isScheduleCall;
    }

    /**
     * Returns whether a reminder has been configured for this event.
     *
     * <p>A missing flag is treated as {@code false}.
     *
     * @return {@code true} when a reminder is configured, {@code false}
     *         otherwise
     */
    public boolean hasReminder() {
        return hasReminder != null && hasReminder;
    }

    /**
     * Returns the number of seconds before the event start at which a
     * reminder should fire.
     *
     * <p>The value is only meaningful when {@link #hasReminder()} returns
     * {@code true}.
     *
     * @return an {@link OptionalLong} containing the reminder lead time in
     *         seconds, or {@link OptionalLong#empty()} when none was
     *         supplied
     */
    public OptionalLong reminderOffsetSec() {
        return reminderOffsetSec == null ? OptionalLong.empty() : OptionalLong.of(reminderOffsetSec);
    }

    /**
     * Sets the context information attached to this event message.
     *
     * @param contextInfo the new {@link ContextInfo}, or {@code null} to
     *                    clear the field
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Sets the cancellation flag for this event.
     *
     * @param isCanceled the new cancellation flag, or {@code null} to clear
     *                   the field
     */
    public void setCanceled(Boolean isCanceled) {
        this.isCanceled = isCanceled;
    }

    /**
     * Sets the user-visible name of the event.
     *
     * @param name the new event name, or {@code null} to clear the field
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the free-form description of the event.
     *
     * @param description the new description, or {@code null} to clear the
     *                    field
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the physical location associated with the event.
     *
     * @param location the new {@link LocationMessage}, or {@code null} to
     *                 clear the field
     */
    public void setLocation(LocationMessage location) {
        this.location = location;
    }

    /**
     * Sets the URL participants can follow to join the event remotely.
     *
     * @param joinLink the new join link, or {@code null} to clear the field
     */
    public void setJoinLink(String joinLink) {
        this.joinLink = joinLink;
    }

    /**
     * Sets the moment at which the event starts.
     *
     * @param startTime the new start {@link Instant}, or {@code null} to
     *                  clear the field
     */
    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    /**
     * Sets the moment at which the event ends.
     *
     * @param endTime the new end {@link Instant}, or {@code null} to clear
     *                the field
     */
    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    /**
     * Sets the flag controlling whether responders may bring additional
     * guests.
     *
     * @param extraGuestsAllowed the new flag, or {@code null} to clear the
     *                           field
     */
    public void setExtraGuestsAllowed(Boolean extraGuestsAllowed) {
        this.extraGuestsAllowed = extraGuestsAllowed;
    }

    /**
     * Sets the flag marking this event as a scheduled WhatsApp call.
     *
     * @param isScheduleCall the new flag, or {@code null} to clear the field
     */
    public void setScheduleCall(Boolean isScheduleCall) {
        this.isScheduleCall = isScheduleCall;
    }

    /**
     * Sets the flag controlling whether a reminder is configured for the
     * event.
     *
     * @param hasReminder the new flag, or {@code null} to clear the field
     */
    public void setHasReminder(Boolean hasReminder) {
        this.hasReminder = hasReminder;
    }

    /**
     * Sets the number of seconds before the event start at which a reminder
     * should fire.
     *
     * @param reminderOffsetSec the new reminder lead time in seconds, or
     *                          {@code null} to clear the field
     */
    public void setReminderOffsetSec(Long reminderOffsetSec) {
        this.reminderOffsetSec = reminderOffsetSec;
    }
}
