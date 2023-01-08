package it.auties.whatsapp.model.sync;

import static it.auties.protobuf.base.ProtobufType.INT64;
import static it.auties.protobuf.base.ProtobufType.MESSAGE;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.MessageKey;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ActionMessageRangeSync
    implements ProtobufMessage {

  @ProtobufProperty(index = 1, type = INT64)
  private Long lastMessageTimestamp;

  @ProtobufProperty(index = 2, type = INT64)
  private Long lastSystemMessageTimestamp;

  @ProtobufProperty(index = 3, type = MESSAGE, implementation = SyncActionMessage.class, repeated = true)
  private List<SyncActionMessage> messages;

  public ActionMessageRangeSync(@NonNull Chat chat, boolean allMessages) {
    chat.lastMessage()
        .ifPresent(message -> this.lastMessageTimestamp = message.timestampInSeconds());
    chat.lastServerMessage()
        .ifPresent(message -> this.lastSystemMessageTimestamp = message.timestampInSeconds());
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

  private SyncActionMessage createActionMessage(MessageInfo info) {
    var timestamp = info != null ?
        info.timestampInSeconds() :
        null;
    var key = info != null ?
        checkSenderKey(info.key()
            .copy()) :
        null;
    return new SyncActionMessage(key, timestamp);
  }

  private MessageKey checkSenderKey(MessageKey key) {
    key.senderJid()
        .ifPresent(jid -> key.senderJid(jid.toUserJid()));
    return key;
  }

  public static class ActionMessageRangeSyncBuilder {
    public ActionMessageRangeSyncBuilder messages(List<SyncActionMessage> messages) {
      if (this.messages == null) {
        this.messages = new ArrayList<>();
      }
      this.messages.addAll(messages);
      return this;
    }
  }
}
