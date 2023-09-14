package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.PastParticipants;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.setting.GlobalSettings;

import java.util.List;
import java.util.Objects;

import static it.auties.protobuf.model.ProtobufType.*;

public record HistorySync(@ProtobufProperty(index = 1, type = OBJECT, required = true) HistorySyncType syncType,
                          @ProtobufProperty(index = 2, type = OBJECT, repeated = true) List<Chat> conversations,
                          @ProtobufProperty(index = 3, type = OBJECT, repeated = true) List<MessageInfo> statusV3Messages,
                          @ProtobufProperty(index = 5, type = UINT32) int chunkOrder,
                          @ProtobufProperty(index = 6, type = UINT32) Integer progress,
                          @ProtobufProperty(index = 7, type = OBJECT, repeated = true) List<PushName> pushNames,
                          @ProtobufProperty(index = 8, type = OBJECT) GlobalSettings globalSettings,
                          @ProtobufProperty(index = 9, type = BYTES) byte[] threadIdUserSecret,
                          @ProtobufProperty(index = 10, type = UINT32) int threadDsTimeframeOffset,
                          @ProtobufProperty(index = 11, type = OBJECT, repeated = true)
                          List<RecentStickerMetadata> recentStickers,
                          @ProtobufProperty(index = 12, type = OBJECT, repeated = true)
                          List<PastParticipants> pastParticipants) implements ProtobufMessage {
    public HistorySync {
        Objects.requireNonNull(syncType, "Missing mandatory field: syncType");
    }
}
