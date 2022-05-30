package it.auties.whatsapp.model.sync;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.info.MessageInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
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

    public ActionMessageRangeSync(@NonNull MessageInfo info){
        this.lastMessageTimestamp = info.timestamp();
        var syncMessage = new SyncActionMessage(info.key(), lastMessageTimestamp);
        this.messages = List.of(syncMessage);
    }

    public static class ActionMessageRangeSyncBuilder {
        public ActionMessageRangeSyncBuilder messages(List<SyncActionMessage> messages) {
            if (this.messages == null) this.messages = new ArrayList<>();
            this.messages.addAll(messages);
            return this;
        }
    }
}
