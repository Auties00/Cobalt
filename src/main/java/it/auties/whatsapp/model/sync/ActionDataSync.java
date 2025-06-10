package it.auties.whatsapp.model.sync;

import io.avaje.jsonb.Jsonb;
import io.avaje.jsonb.Types;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.MessageIndexInfo;
import it.auties.whatsapp.model.info.MessageIndexInfoBuilder;
import it.auties.whatsapp.model.jid.Jid;

import java.util.List;

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
    @SuppressWarnings("unchecked")
    public MessageIndexInfo messageIndex() {
        var array = (List<String>) Jsonb.builder()
                .build()
                .type(Types.listOf(String.class))
                .fromJson(index);
        var iterator = array.iterator();
        var type = iterator.hasNext() ? iterator.next() : null;
        var chatJid = iterator.hasNext() ? Jid.of(iterator.next()) : null;
        var messageId = iterator.hasNext() ? iterator.next() : null;
        var fromMe = iterator.hasNext() && Boolean.parseBoolean(iterator.next());
        return new MessageIndexInfoBuilder()
                .type(type)
                .chatJid(chatJid)
                .messageId(messageId)
                .fromMe(fromMe)
                .build();
    }
}