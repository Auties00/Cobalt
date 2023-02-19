package it.auties.whatsapp.model.message.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.chat.ChatDisappear;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.ServerMessage;
import it.auties.whatsapp.model.sync.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model class that represents a message sent by a WhatsappWeb.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class ProtocolMessage implements ServerMessage {
    /**
     * The key of message that this server message regards
     */
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = MessageKey.class)
    private MessageKey key;

    /**
     * The type of this server message
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = ProtocolMessage.ProtocolMessageType.class)
    private ProtocolMessageType protocolType;

    /**
     * The expiration, that is the seconds in seconds after which a message is automatically deleted,
     * of messages in an ephemeral chat. This property is defined only if {@link ProtocolMessage#type}
     * == {@link ProtocolMessageType#EPHEMERAL_SETTING} || @link
     * ProtocolMessageType#EPHEMERAL_SYNC_RESPONSE}.
     */
    @ProtobufProperty(index = 4, type = UINT64)
    private long ephemeralExpiration;

    /**
     * The timestamp, that is the seconds in seconds since {@link java.time.Instant#EPOCH}, of the
     * last modification to the ephemeral settings of a chat. This property is defined only if
     * {@link ProtocolMessage#type} == {@link ProtocolMessageType#EPHEMERAL_SETTING} || @link
     * ProtocolMessageType#EPHEMERAL_SYNC_RESPONSE}.
     */
    @ProtobufProperty(index = 5, type = UINT64)
    private long ephemeralSettingTimestamp;

    /**
     * History dataSync notification. This property is defined only if {@link ProtocolMessage#type} ==
     * {@link ProtocolMessageType#HISTORY_SYNC_NOTIFICATION}.
     */
    @ProtobufProperty(index = 6, type = MESSAGE, implementation = HistorySyncNotification.class)
    private HistorySyncNotification historySyncNotification;

    /**
     * The app state keys
     */
    @ProtobufProperty(index = 7, type = MESSAGE, implementation = AppStateSyncKeyShare.class)
    private AppStateSyncKeyShare appStateSyncKeyShare;

    /**
     * An app state sync key
     */
    @ProtobufProperty(index = 8, type = MESSAGE, implementation = AppStateSyncKeyRequest.class)
    private AppStateSyncKeyRequest appStateSyncKeyRequest;

    /**
     * Initial security settings sent by Whatsapp
     */
    @ProtobufProperty(index = 9, type = MESSAGE, implementation = InitialSecurityNotificationSettingSync.class)
    private InitialSecurityNotificationSettingSync initialSecurityNotificationSettingSync;

    /**
     * App state exception notification
     */
    @ProtobufProperty(index = 10, type = MESSAGE, implementation = AppStateFatalExceptionNotification.class)
    private AppStateFatalExceptionNotification appStateFatalExceptionNotification;

    @ProtobufProperty(index = 11, name = "disappearingMode", type = MESSAGE)
    private ChatDisappear disappearingMode;

    @ProtobufProperty(index = 14, name = "editedMessage", type = MESSAGE)
    private MessageContainer editedMessage;

    @ProtobufProperty(index = 15, name = "timestampMs", type = INT64)
    private long timestampMs;

    @Override
    public MessageType type() {
        return MessageType.PROTOCOL;
    }

    /**
     * The constants of this enumerated type describe the various type of data that a
     * {@link ProtocolMessage} can wrap
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    @ProtobufName("Type")
    public enum ProtocolMessageType implements ProtobufMessage {
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
        EXCEPTION_NOTIFICATION(10);
        
        @Getter
        private final int index;

        @JsonCreator
        public static ProtocolMessageType of(int index) {
            return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst().orElse(null);
        }
    }
}