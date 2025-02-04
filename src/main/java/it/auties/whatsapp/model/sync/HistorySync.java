package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.GroupPastParticipants;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.setting.GlobalSettings;

import java.util.List;
import java.util.Objects;

import static it.auties.protobuf.model.ProtobufType.*;

@ProtobufMessage(name = "HistorySync")
public record HistorySync(@ProtobufProperty(index = 1, type = ENUM, required = true) Type syncType,
                          @ProtobufProperty(index = 2, type = MESSAGE) List<Chat> conversations,
                          @ProtobufProperty(index = 3, type = MESSAGE) List<ChatMessageInfo> statusV3Messages,
                          @ProtobufProperty(index = 5, type = UINT32) int chunkOrder,
                          @ProtobufProperty(index = 6, type = UINT32) Integer progress,
                          @ProtobufProperty(index = 7, type = MESSAGE) List<PushName> pushNames,
                          @ProtobufProperty(index = 8, type = MESSAGE) GlobalSettings globalSettings,
                          @ProtobufProperty(index = 9, type = BYTES) byte[] threadIdUserSecret,
                          @ProtobufProperty(index = 10, type = UINT32) int threadDsTimeframeOffset,
                          @ProtobufProperty(index = 11, type = MESSAGE)
                          List<StickerMetadata> recentStickers,
                          @ProtobufProperty(index = 12, type = MESSAGE)
                          List<GroupPastParticipants> pastParticipants) {
    public HistorySync {
        Objects.requireNonNull(syncType, "Missing mandatory field: syncType");
    }

    @ProtobufEnum(name = "HistorySync.HistorySyncType")
    public enum Type {
        INITIAL_BOOTSTRAP(0),
        INITIAL_STATUS_V3(1),
        FULL(2),
        RECENT(3),
        PUSH_NAME(4),
        NON_BLOCKING_DATA(5);

        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        final int index;

        public int index() {
            return this.index;
        }
    }
}
