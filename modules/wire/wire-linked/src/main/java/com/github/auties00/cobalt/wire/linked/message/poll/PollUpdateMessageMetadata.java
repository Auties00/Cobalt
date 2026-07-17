package com.github.auties00.cobalt.wire.linked.message.poll;

import com.github.auties00.cobalt.wire.linked.message.Message;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Holds additional metadata attached to a {@link PollUpdateMessage}.
 *
 * <p>The message currently has no declared fields and acts as a placeholder
 * for future extensions of the poll vote schema. Clients that receive a
 * populated instance can still recognise and forward the payload without
 * interpreting its contents.
 */
@ProtobufMessage(name = "Message.PollUpdateMessageMetadata")
public final class PollUpdateMessageMetadata implements Message {

    /**
     * Creates a new, empty metadata container.
     */
    PollUpdateMessageMetadata() {
    }
}
