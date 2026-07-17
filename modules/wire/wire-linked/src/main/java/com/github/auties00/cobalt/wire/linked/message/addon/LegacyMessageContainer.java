package com.github.auties00.cobalt.wire.linked.message.addon;

import com.github.auties00.cobalt.wire.linked.message.event.EventResponseMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollVoteMessage;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A container that holds legacy add-on payloads attached to a message.
 *
 * <p>Older WhatsApp clients represented certain interactive content, such as
 * event responses and poll votes, as a single embedded payload rather than
 * through the add-on framework used by newer clients. This container preserves
 * backward compatibility by carrying either an {@link EventResponseMessage} or
 * a {@link PollVoteMessage} alongside the message that would otherwise hold
 * them natively.
 *
 * <p>At most one of the two fields is expected to be populated for a given
 * message, depending on the legacy interaction that was encoded.
 */
@ProtobufMessage(name = "LegacyMessage")
public final class LegacyMessageContainer {
    /**
     * The legacy event response payload, if the original message represented
     * a response to a calendar or scheduled event.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    EventResponseMessage eventResponseMessage;

    /**
     * The legacy poll vote payload, if the original message represented a
     * vote cast on a poll.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    PollVoteMessage pollVote;


    /**
     * Constructs a new legacy message container with the provided payloads.
     *
     * @param eventResponseMessage the legacy event response payload, or {@code null} if not applicable
     * @param pollVote the legacy poll vote payload, or {@code null} if not applicable
     */
    LegacyMessageContainer(EventResponseMessage eventResponseMessage, PollVoteMessage pollVote) {
        this.eventResponseMessage = eventResponseMessage;
        this.pollVote = pollVote;
    }

    /**
     * Returns the legacy event response payload carried by this container.
     *
     * @return an {@link Optional} containing the event response message, or empty if none was provided
     */
    public Optional<EventResponseMessage> eventResponseMessage() {
        return Optional.ofNullable(eventResponseMessage);
    }

    /**
     * Returns the legacy poll vote payload carried by this container.
     *
     * @return an {@link Optional} containing the poll vote message, or empty if none was provided
     */
    public Optional<PollVoteMessage> pollVote() {
        return Optional.ofNullable(pollVote);
    }

    /**
     * Sets the legacy event response payload.
     *
     * @param eventResponseMessage the new event response message, or {@code null} to clear the field
     */
    public void setEventResponseMessage(EventResponseMessage eventResponseMessage) {
        this.eventResponseMessage = eventResponseMessage;
    }

    /**
     * Sets the legacy poll vote payload.
     *
     * @param pollVote the new poll vote message, or {@code null} to clear the field
     */
    public void setPollVote(PollVoteMessage pollVote) {
        this.pollVote = pollVote;
    }
}
