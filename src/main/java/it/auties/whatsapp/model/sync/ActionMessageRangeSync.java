package it.auties.whatsapp.model.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.message.model.ChatMessageKey;

import java.util.Collections;
import java.util.List;

@ProtobufMessage(name = "SyncActionValue.SyncActionMessageRange")
public final class ActionMessageRangeSync {
    @ProtobufProperty(index = 1, type = ProtobufType.INT64)
    private Long lastMessageTimestamp;
    @ProtobufProperty(index = 2, type = ProtobufType.INT64)
    private Long lastSystemMessageTimestamp;
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    private final List<SyncActionMessage> messages;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ActionMessageRangeSync(Long lastMessageTimestamp, Long lastSystemMessageTimestamp, List<SyncActionMessage> messages) {
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.lastSystemMessageTimestamp = lastSystemMessageTimestamp;
        this.messages = messages;
    }

    public ActionMessageRangeSync(Chat chat, boolean allMessages) {
        chat.newestMessage().ifPresent(message -> this.lastMessageTimestamp = message.timestampSeconds().orElse(0L));
        chat.newestServerMessage().ifPresent(message -> this.lastSystemMessageTimestamp = message.timestampSeconds().orElse(0L));
        this.messages = createMessages(chat, allMessages);
    }

    private List<SyncActionMessage> createMessages(Chat chat, boolean allMessages) {
        if (allMessages) {
            return chat.messages()
                    .stream()
                    .map(HistorySyncMessage::messageInfo)
                    .map(this::createActionMessage)
                    .toList();
        }

        return chat.newestMessage()
                .map(this::createActionMessage)
                .stream()
                .toList();
    }

    private SyncActionMessage createActionMessage(ChatMessageInfo info) {
        var timestamp = info != null ? info.timestampSeconds().isPresent() ? info.timestampSeconds().getAsLong() : null : null;
        var key = info != null ? checkSenderKey(info.key()) : null;
        return new SyncActionMessage(key, timestamp);
    }

    private ChatMessageKey checkSenderKey(ChatMessageKey key) {
        return key.senderJid()
                .map(entry -> new ChatMessageKey(key.chatJid(), key.fromMe(), key.id(), entry.toSimpleJid()))
                .orElse(key);
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