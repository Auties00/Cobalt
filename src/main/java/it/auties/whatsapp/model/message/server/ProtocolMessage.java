package it.auties.whatsapp.model.message.server;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.chat.ChatDisappear;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.ServerMessage;
import it.auties.whatsapp.model.sync.*;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;


/**
 * A model class that represents a message sent by a WhatsappWeb.
 */
public record ProtocolMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        Optional<MessageKey> key,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        @NonNull
        ProtocolMessageType protocolType,
        @ProtobufProperty(index = 4, type = ProtobufType.UINT64)
        long ephemeralExpiration,
        @ProtobufProperty(index = 5, type = ProtobufType.UINT64)
        long ephemeralSettingTimestampSeconds,
        @ProtobufProperty(index = 6, type = ProtobufType.OBJECT)
        Optional<HistorySyncNotification> historySyncNotification,
        @ProtobufProperty(index = 7, type = ProtobufType.OBJECT)
        Optional<AppStateSyncKeyShare> appStateSyncKeyShare,
        @ProtobufProperty(index = 8, type = ProtobufType.OBJECT)
        Optional<AppStateSyncKeyRequest> appStateSyncKeyRequest,
        @ProtobufProperty(index = 9, type = ProtobufType.OBJECT)
        Optional<InitialSecurityNotificationSettingSync> initialSecurityNotificationSettingSync,
        @ProtobufProperty(index = 10, type = ProtobufType.OBJECT)
        Optional<AppStateFatalExceptionNotification> appStateFatalExceptionNotification,
        @ProtobufProperty(index = 11, type = ProtobufType.OBJECT)
        Optional<ChatDisappear> disappearingMode,
        @ProtobufProperty(index = 14, type = ProtobufType.OBJECT)
        Optional<MessageContainer> editedMessage,
        @ProtobufProperty(index = 15, type = ProtobufType.INT64)
        long timestampMilliseconds
) implements ServerMessage {
    @Override
    public MessageType type() {
        return MessageType.PROTOCOL;
    }

}