package com.github.auties00.cobalt.wire.linked.message.event;

import com.github.auties00.cobalt.wire.linked.message.Message;

import java.time.Instant;

import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Represents a reply to a WhatsApp event inviting the user to attend.
 *
 * <p>When a user in a chat receives an {@link EventMessage} (for example a
 * group meeting or scheduled gathering), they can reply with one of three
 * RSVP statuses: going, not going, or maybe. An {@code EventResponseMessage}
 * encodes that reply: it carries the chosen {@link EventResponseType}, the
 * moment at which the response was recorded, and, when the event permits
 * extra attendees, the number of additional guests the responder is bringing
 * along.
 *
 * <p>This type is also one of the permitted variants of the sealed
 * {@link Message} hierarchy, which means it can appear directly as the
 * content of a message container. In most cases, however, it is wrapped in an
 * {@link EncEventResponseMessage} so that the response is encrypted end to
 * end before being sent to the event creator, and aggregated into
 * {@link EventResponse} entries that associate responses with specific event
 * messages.
 */
@ProtobufMessage(name = "Message.EventResponseMessage")
public final class EventResponseMessage implements Message {
    /**
     * The RSVP choice selected by the responder.
     *
     * <p>The value is one of the constants declared in
     * {@link EventResponseType}. A value of {@code null} (represented by
     * {@code Optional.empty()}) indicates that the response has not yet been
     * decoded or that the original payload did not specify a choice.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    EventResponseType response;

    /**
     * The timestamp at which this response was produced, in milliseconds
     * since the Unix epoch.
     *
     * <p>The raw wire value is stored as a {@code long} and exposed as an
     * {@link Instant} through {@link InstantMillisMixin}. It is used by
     * clients to order concurrent responses and, when a user changes their
     * RSVP, to determine which of several replies is the most recent.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant timestampMs;

    /**
     * The number of extra guests the responder is bringing, when the parent
     * event permits additional attendees.
     *
     * <p>This field is only meaningful when the original {@link EventMessage}
     * was created with {@code extraGuestsAllowed} set to {@code true}. If the
     * event forbids extra guests, or the responder did not supply a count,
     * the field is {@code null}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    Integer extraGuestCount;

    /**
     * Constructs a new {@code EventResponseMessage} with the supplied RSVP
     * data.
     *
     * <p>The constructor is package-private. Application code should build
     * instances through the generated {@code EventResponseMessageBuilder}.
     *
     * @param response         the RSVP choice, or {@code null} if not set
     * @param timestampMs      the instant at which the response was produced,
     *                         or {@code null} if not set
     * @param extraGuestCount  the number of extra guests the responder is
     *                         bringing, or {@code null} if not supplied
     */
    EventResponseMessage(EventResponseType response, Instant timestampMs, Integer extraGuestCount) {
        this.response = response;
        this.timestampMs = timestampMs;
        this.extraGuestCount = extraGuestCount;
    }

    /**
     * Returns the RSVP choice selected by the responder.
     *
     * @return an {@link Optional} containing the {@link EventResponseType}
     *         when one was supplied, otherwise {@code Optional.empty()}
     */
    public Optional<EventResponseType> response() {
        return Optional.ofNullable(response);
    }

    /**
     * Returns the instant at which this response was produced.
     *
     * @return an {@link Optional} containing the response timestamp, or
     *         {@code Optional.empty()} when no timestamp was supplied
     */
    public Optional<Instant> timestampMs() {
        return Optional.ofNullable(timestampMs);
    }

    /**
     * Returns the number of extra guests the responder is bringing.
     *
     * <p>The result is only populated when the parent {@link EventMessage}
     * allows extra guests and the responder provided a count.
     *
     * @return an {@link OptionalInt} containing the extra guest count, or
     *         {@link OptionalInt#empty()} when the field was not supplied
     */
    public OptionalInt extraGuestCount() {
        return extraGuestCount == null ? OptionalInt.empty() : OptionalInt.of(extraGuestCount);
    }

    /**
     * Sets the RSVP choice for this response.
     *
     * @param response the new RSVP choice, or {@code null} to clear the field
     */
    public void setResponse(EventResponseType response) {
        this.response = response;
    }

    /**
     * Sets the timestamp at which this response was produced.
     *
     * @param timestampMs the new response timestamp, or {@code null} to clear
     *                    the field
     */
    public void setTimestampMs(Instant timestampMs) {
        this.timestampMs = timestampMs;
    }

    /**
     * Sets the number of extra guests the responder is bringing.
     *
     * @param extraGuestCount the new extra guest count, or {@code null} to
     *                        clear the field
     */
    public void setExtraGuestCount(Integer extraGuestCount) {
        this.extraGuestCount = extraGuestCount;
    }

    /**
     * Enumerates the RSVP choices available when a user replies to a WhatsApp
     * event.
     *
     * <p>The underlying wire values are integers in the range {@code 0} to
     * {@code 3} inclusive and are exchanged inside an
     * {@link EventResponseMessage}. The {@link #UNKNOWN} constant is used
     * when the server transmits a value that the current client does not
     * recognize, so that forward-compatible decoding is always possible.
     */
    @ProtobufEnum(name = "Message.EventResponseMessage.EventResponseType")
    public static enum EventResponseType {
        /**
         * The response was not classified or carries a value that the current
         * client does not recognize. Used as a forward-compatible fallback.
         */
        UNKNOWN(0),

        /**
         * The responder has confirmed that they will attend the event.
         */
        GOING(1),

        /**
         * The responder has declined the invitation and will not attend.
         */
        NOT_GOING(2),

        /**
         * The responder is unsure and has selected the tentative option.
         */
        MAYBE(3);

        /**
         * Constructs a new {@code EventResponseType} constant with the
         * supplied protobuf wire index.
         *
         * @param index the integer value used on the wire to identify this
         *              constant
         */
        EventResponseType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index of this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this constant.
         *
         * @return the integer value used to identify this constant on the wire
         */
        public int index() {
            return this.index;
        }
    }
}
