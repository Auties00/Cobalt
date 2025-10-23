package com.github.auties00.cobalt.model.proto.sync;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.model.proto.info.MessageIndexInfo;
import com.github.auties00.cobalt.model.proto.info.MessageIndexInfoBuilder;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessage(name = "SyncActionData")
public record ActionDataSync(
        @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
        byte[] index,
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        ActionValueSync value,
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte[] padding,
        @ProtobufProperty(index = 4, type = ProtobufType.INT32)
        Integer version
) {
    public MessageIndexInfo messageIndex() {
        var array = JSON.parseArray(index)
                .toJavaList(String.class);
        var iterator = array.iterator();
        var type = iterator.hasNext() ? iterator.next() : null;
        var targetId = iterator.hasNext() ? iterator.next() : null;
        var messageId = iterator.hasNext() ? iterator.next() : null;
        var fromMe = iterator.hasNext() && Boolean.parseBoolean(iterator.next());
        return new MessageIndexInfoBuilder()
                .type(type)
                .targetId(targetId)
                .messageId(messageId)
                .fromMe(fromMe)
                .build();
    }
}