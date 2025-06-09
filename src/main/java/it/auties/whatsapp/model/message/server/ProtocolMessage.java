package it.auties.whatsapp.model.message.server;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.chat.ChatDisappear;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.message.model.Message;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.message.model.ServerMessage;
import it.auties.whatsapp.model.sync.*;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents a message sent by a WhatsappWeb.
 */
@ProtobufMessage(name = "Message.ProtocolMessage")
public final class ProtocolMessage implements ServerMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final ChatMessageKey key;

    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    final Type protocolType;

    @ProtobufProperty(index = 4, type = ProtobufType.UINT64)
    final long ephemeralExpirationSeconds;

    @ProtobufProperty(index = 5, type = ProtobufType.UINT64)
    final long ephemeralSettingTimestampSeconds;

    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    final HistorySyncNotification historySyncNotification;

    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    final AppStateSyncKeyShare appStateSyncKeyShare;

    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    final AppStateSyncKeyRequest appStateSyncKeyRequest;

    @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
    final InitialSecurityNotificationSettingSync initialSecurityNotificationSettingSync;

    @ProtobufProperty(index = 10, type = ProtobufType.MESSAGE)
    final AppStateFatalExceptionNotification appStateFatalExceptionNotification;

    @ProtobufProperty(index = 11, type = ProtobufType.MESSAGE)
    final ChatDisappear disappearingMode;

    @ProtobufProperty(index = 14, type = ProtobufType.MESSAGE)
    final MessageContainer editedMessage;

    @ProtobufProperty(index = 15, type = ProtobufType.INT64)
    final long timestampMilliseconds;

    ProtocolMessage(ChatMessageKey key, Type protocolType, long ephemeralExpirationSeconds, long ephemeralSettingTimestampSeconds, HistorySyncNotification historySyncNotification, AppStateSyncKeyShare appStateSyncKeyShare, AppStateSyncKeyRequest appStateSyncKeyRequest, InitialSecurityNotificationSettingSync initialSecurityNotificationSettingSync, AppStateFatalExceptionNotification appStateFatalExceptionNotification, ChatDisappear disappearingMode, MessageContainer editedMessage, long timestampMilliseconds) {
        this.key = key;
        this.protocolType = Objects.requireNonNull(protocolType, "protocolType cannot be null");
        this.ephemeralExpirationSeconds = ephemeralExpirationSeconds;
        this.ephemeralSettingTimestampSeconds = ephemeralSettingTimestampSeconds;
        this.historySyncNotification = historySyncNotification;
        this.appStateSyncKeyShare = appStateSyncKeyShare;
        this.appStateSyncKeyRequest = appStateSyncKeyRequest;
        this.initialSecurityNotificationSettingSync = initialSecurityNotificationSettingSync;
        this.appStateFatalExceptionNotification = appStateFatalExceptionNotification;
        this.disappearingMode = disappearingMode;
        this.editedMessage = editedMessage;
        this.timestampMilliseconds = timestampMilliseconds;
    }

    @Override
    public Message.Type type() {
        return Message.Type.PROTOCOL;
    }

    public Optional<ChatMessageKey> key() {
        return Optional.ofNullable(key);
    }

    public Type protocolType() {
        return protocolType;
    }

    public long ephemeralExpirationSeconds() {
        return ephemeralExpirationSeconds;
    }

    public Optional<ZonedDateTime> ephemeralExpiration() {
        return Clock.parseSeconds(ephemeralExpirationSeconds);
    }

    public long ephemeralSettingTimestampSeconds() {
        return ephemeralSettingTimestampSeconds;
    }

    public Optional<ZonedDateTime> ephemeralSettingTimestamp() {
        return Clock.parseSeconds(ephemeralSettingTimestampSeconds);
    }

    public Optional<HistorySyncNotification> historySyncNotification() {
        return Optional.ofNullable(historySyncNotification);
    }

    public Optional<AppStateSyncKeyShare> appStateSyncKeyShare() {
        return Optional.ofNullable(appStateSyncKeyShare);
    }

    public Optional<AppStateSyncKeyRequest> appStateSyncKeyRequest() {
        return Optional.ofNullable(appStateSyncKeyRequest);
    }

    public Optional<InitialSecurityNotificationSettingSync> initialSecurityNotificationSettingSync() {
        return Optional.ofNullable(initialSecurityNotificationSettingSync);
    }

    public Optional<AppStateFatalExceptionNotification> appStateFatalExceptionNotification() {
        return Optional.ofNullable(appStateFatalExceptionNotification);
    }

    public Optional<ChatDisappear> disappearingMode() {
        return Optional.ofNullable(disappearingMode);
    }

    public Optional<MessageContainer> editedMessage() {
        return Optional.ofNullable(editedMessage);
    }

    public long timestampMilliseconds() {
        return timestampMilliseconds;
    }

    public Optional<ZonedDateTime> timestamp() {
        return Clock.parseMilliseconds(timestampMilliseconds);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ProtocolMessage that
                && Objects.equals(key, that.key)
                && Objects.equals(protocolType, that.protocolType)
                && ephemeralExpirationSeconds == that.ephemeralExpirationSeconds
                && ephemeralSettingTimestampSeconds == that.ephemeralSettingTimestampSeconds
                && Objects.equals(historySyncNotification, that.historySyncNotification)
                && Objects.equals(appStateSyncKeyShare, that.appStateSyncKeyShare)
                && Objects.equals(appStateSyncKeyRequest, that.appStateSyncKeyRequest)
                && Objects.equals(initialSecurityNotificationSettingSync, that.initialSecurityNotificationSettingSync)
                && Objects.equals(appStateFatalExceptionNotification, that.appStateFatalExceptionNotification)
                && Objects.equals(disappearingMode, that.disappearingMode)
                && Objects.equals(editedMessage, that.editedMessage)
                && timestampMilliseconds == that.timestampMilliseconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, protocolType, ephemeralExpirationSeconds, ephemeralSettingTimestampSeconds,
                historySyncNotification, appStateSyncKeyShare, appStateSyncKeyRequest,
                initialSecurityNotificationSettingSync, appStateFatalExceptionNotification, disappearingMode,
                editedMessage, timestampMilliseconds);
    }

    @Override
    public String toString() {
        return "ProtocolMessage[" +
                "key=" + key + ", " +
                "protocolType=" + protocolType + ", " +
                "ephemeralExpiration=" + ephemeralExpirationSeconds + ", " +
                "ephemeralSettingTimestampSeconds=" + ephemeralSettingTimestampSeconds + ", " +
                "historySyncNotification=" + historySyncNotification + ", " +
                "appStateSyncKeyShare=" + appStateSyncKeyShare + ", " +
                "appStateSyncKeyRequest=" + appStateSyncKeyRequest + ", " +
                "initialSecurityNotificationSettingSync=" + initialSecurityNotificationSettingSync + ", " +
                "appStateFatalExceptionNotification=" + appStateFatalExceptionNotification + ", " +
                "disappearingMode=" + disappearingMode + ", " +
                "editedMessage=" + editedMessage + ", " +
                "timestampMilliseconds=" + timestampMilliseconds + ']';
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
    }
}