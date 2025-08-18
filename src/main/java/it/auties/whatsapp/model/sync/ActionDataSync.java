package it.auties.whatsapp.model.sync;

import com.alibaba.fastjson2.JSON;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.MessageIndexInfo;
import it.auties.whatsapp.model.info.MessageIndexInfoBuilder;

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