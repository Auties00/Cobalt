package com.github.auties00.cobalt.model.message.server;

import com.github.auties00.cobalt.model.chat.ChatDisappear;
import com.github.auties00.cobalt.model.message.model.ChatMessageKey;
import com.github.auties00.cobalt.model.message.model.Message;
import com.github.auties00.cobalt.model.message.model.MessageContainer;
import com.github.auties00.cobalt.model.message.model.ServerMessage;
import com.github.auties00.cobalt.model.sync.*;
import com.github.auties00.cobalt.util.Clock;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.ZonedDateTime;
import java.util.Arrays;
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

    @ProtobufProperty(index = 16, type = ProtobufType.MESSAGE)
    final PeerDataOperationRequestMessage peerDataOperationRequestMessage;

    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    final PeerDataOperationRequestResponseMessage peerDataOperationRequestResponseMessage;

    @ProtobufProperty(index = 18, type = ProtobufType.MESSAGE)
    final BotFeedbackMessage botFeedbackMessage;

    @ProtobufProperty(index = 19, type = ProtobufType.STRING)
    final String invokerJid;

    @ProtobufProperty(index = 20, type = ProtobufType.MESSAGE)
    final RequestWelcomeMessageMetadata requestWelcomeMessageMetadata;

    @ProtobufProperty(index = 21, type = ProtobufType.MESSAGE)
    final MediaNotifyMessage mediaNotifyMessage;

    @ProtobufProperty(index = 22, type = ProtobufType.MESSAGE)
    final CloudAPIThreadControlNotification cloudApiThreadControlNotification;

    @ProtobufProperty(index = 23, type = ProtobufType.MESSAGE)
    final LIDMigrationMappingSyncMessage lidMigrationMappingSyncMessage;

    @ProtobufProperty(index = 24, type = ProtobufType.MESSAGE)
    final LimitSharing limitSharing;

    @ProtobufProperty(index = 25, type = ProtobufType.BYTES)
    final byte[] aiPsiMetadata;

    @ProtobufProperty(index = 26, type = ProtobufType.MESSAGE)
    final AIQueryFanout aiQueryFanout;

    @ProtobufProperty(index = 27, type = ProtobufType.MESSAGE)
    final MemberLabel memberLabel;

    ProtocolMessage(ChatMessageKey key, Type protocolType, long ephemeralExpirationSeconds, long ephemeralSettingTimestampSeconds, HistorySyncNotification historySyncNotification, AppStateSyncKeyShare appStateSyncKeyShare, AppStateSyncKeyRequest appStateSyncKeyRequest, InitialSecurityNotificationSettingSync initialSecurityNotificationSettingSync, AppStateFatalExceptionNotification appStateFatalExceptionNotification, ChatDisappear disappearingMode, MessageContainer editedMessage, long timestampMilliseconds, PeerDataOperationRequestMessage peerDataOperationRequestMessage, PeerDataOperationRequestResponseMessage peerDataOperationRequestResponseMessage, BotFeedbackMessage botFeedbackMessage, String invokerJid, RequestWelcomeMessageMetadata requestWelcomeMessageMetadata, MediaNotifyMessage mediaNotifyMessage, CloudAPIThreadControlNotification cloudApiThreadControlNotification, LIDMigrationMappingSyncMessage lidMigrationMappingSyncMessage, LimitSharing limitSharing, byte[] aiPsiMetadata, AIQueryFanout aiQueryFanout, MemberLabel memberLabel) {
        this.key = key;
        this.protocolType = protocolType;
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
        this.peerDataOperationRequestMessage = peerDataOperationRequestMessage;
        this.peerDataOperationRequestResponseMessage = peerDataOperationRequestResponseMessage;
        this.botFeedbackMessage = botFeedbackMessage;
        this.invokerJid = invokerJid;
        this.requestWelcomeMessageMetadata = requestWelcomeMessageMetadata;
        this.mediaNotifyMessage = mediaNotifyMessage;
        this.cloudApiThreadControlNotification = cloudApiThreadControlNotification;
        this.lidMigrationMappingSyncMessage = lidMigrationMappingSyncMessage;
        this.limitSharing = limitSharing;
        this.aiPsiMetadata = aiPsiMetadata;
        this.aiQueryFanout = aiQueryFanout;
        this.memberLabel = memberLabel;
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

    public Optional<PeerDataOperationRequestMessage> peerDataOperationRequestMessage() {
        return Optional.ofNullable(peerDataOperationRequestMessage);
    }

    public Optional<PeerDataOperationRequestResponseMessage> peerDataOperationRequestResponseMessage() {
        return Optional.ofNullable(peerDataOperationRequestResponseMessage);
    }

    public Optional<BotFeedbackMessage> botFeedbackMessage() {
        return Optional.ofNullable(botFeedbackMessage);
    }

    public Optional<String> invokerJid() {
        return Optional.ofNullable(invokerJid);
    }

    public Optional<RequestWelcomeMessageMetadata> requestWelcomeMessageMetadata() {
        return Optional.ofNullable(requestWelcomeMessageMetadata);
    }

    public Optional<MediaNotifyMessage> mediaNotifyMessage() {
        return Optional.ofNullable(mediaNotifyMessage);
    }

    public Optional<CloudAPIThreadControlNotification> cloudApiThreadControlNotification() {
        return Optional.ofNullable(cloudApiThreadControlNotification);
    }

    public Optional<LIDMigrationMappingSyncMessage> lidMigrationMappingSyncMessage() {
        return Optional.ofNullable(lidMigrationMappingSyncMessage);
    }

    public Optional<LimitSharing> limitSharing() {
        return Optional.ofNullable(limitSharing);
    }

    public Optional<byte[]> aiPsiMetadata() {
        return Optional.ofNullable(aiPsiMetadata);
    }

    public Optional<AIQueryFanout> aiQueryFanout() {
        return Optional.ofNullable(aiQueryFanout);
    }

    public Optional<MemberLabel> memberLabel() {
        return Optional.ofNullable(memberLabel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProtocolMessage that = (ProtocolMessage) o;
        return ephemeralExpirationSeconds == that.ephemeralExpirationSeconds &&
                ephemeralSettingTimestampSeconds == that.ephemeralSettingTimestampSeconds &&
                timestampMilliseconds == that.timestampMilliseconds &&
                Objects.equals(key, that.key) &&
                protocolType == that.protocolType &&
                Objects.equals(historySyncNotification, that.historySyncNotification) &&
                Objects.equals(appStateSyncKeyShare, that.appStateSyncKeyShare) &&
                Objects.equals(appStateSyncKeyRequest, that.appStateSyncKeyRequest) &&
                Objects.equals(initialSecurityNotificationSettingSync, that.initialSecurityNotificationSettingSync) &&
                Objects.equals(appStateFatalExceptionNotification, that.appStateFatalExceptionNotification) &&
                Objects.equals(disappearingMode, that.disappearingMode) &&
                Objects.equals(editedMessage, that.editedMessage) &&
                Objects.equals(peerDataOperationRequestMessage, that.peerDataOperationRequestMessage) &&
                Objects.equals(peerDataOperationRequestResponseMessage, that.peerDataOperationRequestResponseMessage) &&
                Objects.equals(botFeedbackMessage, that.botFeedbackMessage) &&
                Objects.equals(invokerJid, that.invokerJid) &&
                Objects.equals(requestWelcomeMessageMetadata, that.requestWelcomeMessageMetadata) &&
                Objects.equals(mediaNotifyMessage, that.mediaNotifyMessage) &&
                Objects.equals(cloudApiThreadControlNotification, that.cloudApiThreadControlNotification) &&
                Objects.equals(lidMigrationMappingSyncMessage, that.lidMigrationMappingSyncMessage) &&
                Objects.equals(limitSharing, that.limitSharing) &&
                Arrays.equals(aiPsiMetadata, that.aiPsiMetadata) &&
                Objects.equals(aiQueryFanout, that.aiQueryFanout) &&
                Objects.equals(memberLabel, that.memberLabel);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(key, protocolType, ephemeralExpirationSeconds, ephemeralSettingTimestampSeconds,
                historySyncNotification, appStateSyncKeyShare, appStateSyncKeyRequest,
                initialSecurityNotificationSettingSync, appStateFatalExceptionNotification, disappearingMode,
                editedMessage, timestampMilliseconds, peerDataOperationRequestMessage,
                peerDataOperationRequestResponseMessage, botFeedbackMessage, invokerJid,
                requestWelcomeMessageMetadata, mediaNotifyMessage, cloudApiThreadControlNotification,
                lidMigrationMappingSyncMessage, limitSharing, aiQueryFanout, memberLabel);
        result = 31 * result + Arrays.hashCode(aiPsiMetadata);
        return result;
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
                "timestampMilliseconds=" + timestampMilliseconds + ", " +
                "peerDataOperationRequestMessage=" + peerDataOperationRequestMessage + ", " +
                "peerDataOperationRequestResponseMessage=" + peerDataOperationRequestResponseMessage + ", " +
                "botFeedbackMessage=" + botFeedbackMessage + ", " +
                "invokerJid=" + invokerJid + ", " +
                "requestWelcomeMessageMetadata=" + requestWelcomeMessageMetadata + ", " +
                "mediaNotifyMessage=" + mediaNotifyMessage + ", " +
                "cloudApiThreadControlNotification=" + cloudApiThreadControlNotification + ", " +
                "lidMigrationMappingSyncMessage=" + lidMigrationMappingSyncMessage + ", " +
                "limitSharing=" + limitSharing + ", " +
                "aiPsiMetadata=" + Arrays.toString(aiPsiMetadata) + ", " +
                "aiQueryFanout=" + aiQueryFanout + ", " +
                "memberLabel=" + memberLabel + ']';
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
         * A {@link ProtocolMessage} that notifies that a sync in an ephemeral chat
         */
        EPHEMERAL_SYNC_RESPONSE(4),
        /**
         * A {@link ProtocolMessage} that notifies that a history sync in any chat
         */
        HISTORY_SYNC_NOTIFICATION(5),
        /**
         * App state sync key share
         */
        APP_STATE_SYNC_KEY_SHARE(6),
        /**
         * App state sync key request
         */
        APP_STATE_SYNC_KEY_REQUEST(7),
        /**
         * Message fanout back-fill request
         */
        MSG_FANOUT_BACKFILL_REQUEST(8),
        /**
         * Initial security notification setting sync
         */
        INITIAL_SECURITY_NOTIFICATION_SETTING_SYNC(9),
        /**
         * App state fatal exception notification
         */
        APP_STATE_FATAL_EXCEPTION_NOTIFICATION(10),
        /**
         * Share phone value
         */
        SHARE_PHONE_NUMBER(11),
        /**
         * Message edit
         */
        MESSAGE_EDIT(14),
        /**
         * Peer data operation request
         */
        PEER_DATA_OPERATION_REQUEST_MESSAGE(16),
        /**
         * Peer data operation request response
         */
        PEER_DATA_OPERATION_REQUEST_RESPONSE_MESSAGE(17),
        /**
         * Request welcome message
         */
        REQUEST_WELCOME_MESSAGE(18),
        /**
         * Bot feedback message
         */
        BOT_FEEDBACK_MESSAGE(19),
        /**
         * Media notify message
         */
        MEDIA_NOTIFY_MESSAGE(20),
        /**
         * Cloud API thread control notification
         */
        CLOUD_API_THREAD_CONTROL_NOTIFICATION(21),
        /**
         * LID migration mapping sync
         */
        LID_MIGRATION_MAPPING_SYNC(22),
        /**
         * Reminder message
         */
        REMINDER_MESSAGE(23),
        /**
         * Bot memu onboarding message
         */
        BOT_MEMU_ONBOARDING_MESSAGE(24),
        /**
         * Status mention message
         */
        STATUS_MENTION_MESSAGE(25),
        /**
         * Stop generation message
         */
        STOP_GENERATION_MESSAGE(26),
        /**
         * Limit sharing
         */
        LIMIT_SHARING(27),
        /**
         * AI PSI Metadata
         */
        AI_PSI_METADATA(28),
        /**
         * AI Query Fanout
         */
        AI_QUERY_FANOUT(29),
        /**
         * Group member label change
         */
        GROUP_MEMBER_LABEL_CHANGE(30);

        final int index;

        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }
    }
}
