package it.auties.whatsapp.model.sync;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.MessageKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Collections;
import java.util.List;

import static it.auties.protobuf.base.ProtobufType.INT64;
import static it.auties.protobuf.base.ProtobufType.MESSAGE;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("SyncActionMessageRange")
public class ActionMessageRangeSync implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = INT64)
    private Long lastMessageTimestamp;

    @ProtobufProperty(index = 2, type = INT64)
    private Long lastSystemMessageTimestamp;

    @ProtobufProperty(index = 3, type = MESSAGE, implementation = SyncActionMessage.class, repeated = true)
    private List<SyncActionMessage> messages;

    public ActionMessageRangeSync(@NonNull Chat chat, boolean allMessages) {
        chat.newestMessage().ifPresent(message -> this.lastMessageTimestamp = message.timestampSeconds());
        chat.newestServerMessage().ifPresent(message -> this.lastSystemMessageTimestamp = message.timestampSeconds());
        this.messages = createMessages(chat, allMessages);
    }

    private List<SyncActionMessage> createMessages(Chat chat, boolean allMessages) {
        if (allMessages) {
            return chat.messages()
                    .stream()
                    .map(HistorySyncMessage::message)
                    .map(this::createActionMessage)
                    .toList();
        }

        return chat.newestMessage()
                .map(this::createActionMessage)
                .stream()
                .toList();
    }

    private SyncActionMessage createActionMessage(MessageInfo info) {
        var timestamp = (info != null) ? info.timestampSeconds() : null;
        var key = (info != null) ? checkSenderKey(info.key().copy()) : null;
        return new SyncActionMessage(key, timestamp);
    }

    private MessageKey checkSenderKey(MessageKey key) {
        key.senderJid().ifPresent(jid -> key.senderJid(jid.toWhatsappJid()));
        return key;
    }

    public long lastMessageTimestamp() {
        return lastMessageTimestamp == null ? 0 : lastMessageTimestamp;
    }

    public long lastSystemMessageTimestamp() {
        return lastSystemMessageTimestamp == null ? 0 : lastSystemMessageTimestamp;
    }

    public List<SyncActionMessage> messages() {
        return Collections.unmodifiableList(messages == null ? List.of() : messages);
    }
}