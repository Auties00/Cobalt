package com.github.auties00.cobalt.wire.linked.message.poll;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Describes the behavioural kind of a poll.
 *
 * <p>A standard poll collects opinions from participants with no notion of a
 * correct answer, while a quiz marks one option as correct and typically
 * reveals the answer after voting.
 */
@ProtobufEnum(name = "Message.PollType")
public enum PollType {
    /**
     * A standard poll with no correct answer.
     */
    POLL(0),
    /**
     * A quiz poll where one option is designated as the correct answer.
     */
    QUIZ(1);

    /**
     * Creates a new enum constant for the given protobuf index.
     *
     * @param index the protobuf wire index used for serialization
     */
    PollType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * The protobuf wire index associated with this constant.
     */
    final int index;

    /**
     * Returns the protobuf wire index associated with this constant.
     *
     * @return the protobuf wire index
     */
    public int index() {
        return this.index;
    }
}
