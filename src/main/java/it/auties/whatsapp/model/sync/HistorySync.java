package it.auties.whatsapp.model.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.info.MessageInfo;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class HistorySync implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = HistorySyncHistorySyncType.class)
    @NonNull
    private HistorySyncHistorySyncType syncType;

    @ProtobufProperty(index = 2, type = MESSAGE, concreteType = Chat.class, repeated = true)
    @Default
    private List<Chat> conversations = new ArrayList<>();

    @ProtobufProperty(index = 3, type = MESSAGE, concreteType = MessageInfo.class, repeated = true)
    @Default
    private List<MessageInfo> statusV3Messages = new ArrayList<>();

    @ProtobufProperty(index = 5, type = UINT32)
    private Integer chunkOrder;

    @ProtobufProperty(index = 6, type = UINT32)
    private Integer progress;

    @ProtobufProperty(index = 7, type = MESSAGE, concreteType = PushName.class, repeated = true)
    @Default
    private List<PushName> pushNames = new ArrayList<>();

    @ProtobufProperty(index = 8, type = MESSAGE, concreteType = GlobalSettings.class)
    private GlobalSettings globalSettings;

    @ProtobufProperty(index = 9, type = BYTES)
    private byte[] threadIdUserSecret;

    @ProtobufProperty(index = 10, type = UINT32)
    private Integer threadDsTimeframeOffset;

    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum HistorySyncHistorySyncType {
        INITIAL_BOOTSTRAP(0),
        INITIAL_STATUS_V3(1),
        FULL(2),
        RECENT(3),
        PUSH_NAME(4);

        @Getter
        private final int index;

        @JsonCreator
        public static HistorySyncHistorySyncType forIndex(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(null);
        }
    }

    public static class HistorySyncBuilder {
        public HistorySyncBuilder conversations(List<Chat> conversations) {
            if (!conversations$set) {
                this.conversations$value = conversations;
                this.conversations$set = true;
                return this;
            }

            this.conversations$value.addAll(conversations);
            return this;
        }

        public HistorySyncBuilder statusV3Messages(List<MessageInfo> statusV3Messages) {
            if (!statusV3Messages$set) {
                this.statusV3Messages$value = statusV3Messages;
                this.statusV3Messages$set = true;
                return this;
            }

            this.statusV3Messages$value.addAll(statusV3Messages);
            return this;
        }

        public HistorySyncBuilder pushNames(List<PushName> pushNames) {
            if (!pushNames$set) {
                this.pushNames$value = pushNames;
                this.pushNames$set = true;
                return this;
            }

            this.pushNames$value.addAll(pushNames);
            return this;
        }
    }
}
