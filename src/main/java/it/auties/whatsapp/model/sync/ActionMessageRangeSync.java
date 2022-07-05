package it.auties.whatsapp.model.sync;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.MessageKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.INT64;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ActionMessageRangeSync implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = INT64)
    private Long lastMessageTimestamp;

    @ProtobufProperty(index = 2, type = INT64)
    private Long lastSystemMessageTimestamp;

    @ProtobufProperty(index = 3, type = MESSAGE, concreteType = SyncActionMessage.class, repeated = true)
    private List<SyncActionMessage> messages;

    public ActionMessageRangeSync(@NonNull Chat chat, boolean allMessages) {
        chat.lastMessage()
                .ifPresent(this::setTimestamp);
        this.messages = createMessages(chat, allMessages);
    }

    private List<SyncActionMessage> createMessages(Chat chat, boolean allMessages) {
        return allMessages ?
                chat.messages()
                        .stream()
                        .map(this::createActionMessage)
                        .toList() :
                chat.lastMessage()
                        .map(this::createActionMessage)
                        .stream()
                        .toList();
    }

    private void setTimestamp(MessageInfo message) {
        if(message.message().isServer()){
            this.lastSystemMessageTimestamp = message.timestamp();
            return;
        }

        this.lastMessageTimestamp = message.timestamp();
    }

    private SyncActionMessage createActionMessage(MessageInfo info) {
        var timestamp = info != null ?
                info.timestamp() :
                null;
        var key = info != null ?
                checkSenderKey(info.key()
                        .copy()) :
                null;
        return new SyncActionMessage(key, timestamp);
    }

    private MessageKey checkSenderKey(MessageKey key) {
        if (key.senderJid() == null) {
            return key;
        }

        return key.senderJid(key.senderJid()
                .toUserJid());
    }

    public static class ActionMessageRangeSyncBuilder {
        public ActionMessageRangeSyncBuilder messages(List<SyncActionMessage> messages) {
            if (this.messages == null)
                this.messages = new ArrayList<>();
            this.messages.addAll(messages);
            return this;
        }
    }
}
