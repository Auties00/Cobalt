package com.github.auties00.cobalt.model.proto.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

@ProtobufMessage(name = "SyncActionValue.SyncActionMessageRange")
public final class ActionMessageRangeSync {
    @ProtobufProperty(index = 1, type = ProtobufType.INT64)
    Long lastMessageTimestamp;
    
    @ProtobufProperty(index = 2, type = ProtobufType.INT64)
    Long lastSystemMessageTimestamp;

    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final List<SyncActionMessage> messages;

    public ActionMessageRangeSync(Long lastMessageTimestamp, Long lastSystemMessageTimestamp, List<SyncActionMessage> messages) {
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.lastSystemMessageTimestamp = lastSystemMessageTimestamp;
        this.messages = messages;
    }

    public Long lastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public Long lastSystemMessageTimestamp() {
        return lastSystemMessageTimestamp == null ? 0 : lastSystemMessageTimestamp;
    }

    public List<SyncActionMessage> messages() {
        return Collections.unmodifiableList(messages == null ? List.of() : messages);
    }
}