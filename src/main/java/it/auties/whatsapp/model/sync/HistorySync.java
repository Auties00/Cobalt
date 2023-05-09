package it.auties.whatsapp.model.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.PastParticipants;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.setting.GlobalSettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static it.auties.protobuf.base.ProtobufType.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class HistorySync implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = Type.class)
    private Type syncType;

    @ProtobufProperty(index = 2, type = MESSAGE, implementation = Chat.class, repeated = true)
    @Default
    private List<Chat> conversations = new ArrayList<>();

    @ProtobufProperty(index = 3, type = MESSAGE, implementation = MessageInfo.class, repeated = true)
    @Default
    private List<MessageInfo> statusV3Messages = new ArrayList<>();

    @ProtobufProperty(index = 5, type = UINT32)
    private Integer chunkOrder;

    @ProtobufProperty(index = 6, type = UINT32)
    private Integer progress;

    @ProtobufProperty(index = 7, type = MESSAGE, implementation = PushName.class, repeated = true)
    @Default
    private List<PushName> pushNames = new ArrayList<>();

    @ProtobufProperty(index = 8, type = MESSAGE, implementation = GlobalSettings.class)
    private GlobalSettings globalSettings;

    @ProtobufProperty(index = 9, type = BYTES)
    private byte[] threadIdUserSecret;

    @ProtobufProperty(index = 10, type = UINT32)
    private Integer threadDsTimeframeOffset;

    @ProtobufProperty(implementation = StickerMetadata.class, index = 11, name = "recentStickers", repeated = true, type = MESSAGE)
    @Default
    private List<StickerMetadata> recentStickers = new ArrayList<>();

    @ProtobufProperty(implementation = PastParticipants.class, index = 12, name = "pastParticipants", repeated = true, type = MESSAGE)
    @Default
    private List<PastParticipants> pastParticipants = new ArrayList<>();

    @AllArgsConstructor
    @Accessors(fluent = true)
    @ProtobufName("HistorySyncType")
    public enum Type implements ProtobufMessage{
        INITIAL_BOOTSTRAP(0),
        INITIAL_STATUS_V3(1),
        FULL(2),
        RECENT(3),
        PUSH_NAME(4),
        NON_BLOCKING_DATA(5);

        @Getter
        private final int index;

        @JsonCreator
        public static Type of(int index) {
            return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst().orElse(null);
        }
    }
}
