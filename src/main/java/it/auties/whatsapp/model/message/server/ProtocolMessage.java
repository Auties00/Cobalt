package it.auties.whatsapp.model.message.server;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.chat.ChatDisappear;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.ServerMessage;
import it.auties.whatsapp.model.sync.*;

import java.util.Optional;


/**
 * A model class that represents a message sent by a WhatsappWeb.
 */
@ProtobufMessage(name = "Message.ProtocolMessage")
public record ProtocolMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        Optional<ChatMessageKey> key,
        @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
        Type protocolType,
        @ProtobufProperty(index = 4, type = ProtobufType.UINT64)
        long ephemeralExpiration,
        @ProtobufProperty(index = 5, type = ProtobufType.UINT64)
        long ephemeralSettingTimestampSeconds,
        @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
        Optional<HistorySyncNotification> historySyncNotification,
        @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
        Optional<AppStateSyncKeyShare> appStateSyncKeyShare,
        @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
        Optional<AppStateSyncKeyRequest> appStateSyncKeyRequest,
        @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
        Optional<InitialSecurityNotificationSettingSync> initialSecurityNotificationSettingSync,
        @ProtobufProperty(index = 10, type = ProtobufType.MESSAGE)
        Optional<AppStateFatalExceptionNotification> appStateFatalExceptionNotification,
        @ProtobufProperty(index = 11, type = ProtobufType.MESSAGE)
        Optional<ChatDisappear> disappearingMode,
        @ProtobufProperty(index = 14, type = ProtobufType.MESSAGE)
        Optional<MessageContainer> editedMessage,
        @ProtobufProperty(index = 15, type = ProtobufType.INT64)
        long timestampMilliseconds
) implements ServerMessage {
    @Override
    public MessageType type() {
        return MessageType.PROTOCOL;
    }

    /**
     * The constants of this enumerated type describe the various type of data that a
     * {@link ProtocolMessage} can wrap
     */
    @ProtobufEnum(name = "Message.ProtocolMessage.Type")
    public enum Type {
        /**
         * A {@link ProtocolMessage} that notifies that a message was deleted for everyone in a chat
         */
        REVOKE(0),
        /**
         * A {@link ProtocolMessage} that notifies that the ephemeral settings in a chat have changed
         */
        EPHEMERAL_SETTING(3),
        /**
         * A {@link ProtocolMessage} that notifies that a dataSync in an ephemeral chat
         */
        EPHEMERAL_SYNC_RESPONSE(4),
        /**
         * A {@link ProtocolMessage} that notifies that a history dataSync in any chat
         */
        HISTORY_SYNC_NOTIFICATION(5),
        /**
         * App state dataSync key share
         */
        APP_STATE_SYNC_KEY_SHARE(6),
        /**
         * App state dataSync key request
         */
        APP_STATE_SYNC_KEY_REQUEST(7),
        /**
         * Message back-fill request
         */
        MESSAGE_BACK_FILL_REQUEST(8),
        /**
         * Initial security notification setting dataSync
         */
        INITIAL_SECURITY_NOTIFICATION_SETTING_SYNC(9),
        /**
         * App state fatal exception notification
         */
        EXCEPTION_NOTIFICATION(10),
        /**
         * Share phone number
         */
        SHARE_PHONE_NUMBER(11),
        /**
         * Message edit
         */
        MESSAGE_EDIT(14);

        final int index;

        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return this.index;
        }
    }
}