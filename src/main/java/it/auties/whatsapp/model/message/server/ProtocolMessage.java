package it.auties.whatsapp.model.message.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.message.model.ServerMessage;
import it.auties.whatsapp.model.sync.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.UINT64;

/**
 * A model class that represents a message sent by a WhatsappWeb.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(builderMethodName = "newProtocolMessage", buildMethodName = "create")
@Jacksonized
@Accessors(fluent = true)
public final class ProtocolMessage implements ServerMessage {
    /**
     * The key of message that this server message regards
     */
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = MessageKey.class)
    private MessageKey key;

    /**
     * The type of this server message
     */
    @ProtobufProperty(index = 2, type = MESSAGE, concreteType = ProtocolMessageType.class)
    private ProtocolMessageType type;

    /**
     * The expiration, that is the endTimeStamp in seconds after which a message is automatically deleted, of messages in an ephemeral chat.
     * This property is defined only if {@link ProtocolMessage#type} == {@link ProtocolMessageType#EPHEMERAL_SETTING} || @link ProtocolMessageType#EPHEMERAL_SYNC_RESPONSE}.
     */
    @ProtobufProperty(index = 4, type = UINT64)
    private long ephemeralExpiration;

    /**
     * The timestamp, that is the endTimeStamp in seconds since {@link java.time.Instant#EPOCH}, of the last modification to the ephemeral settings of a chat.
     * This property is defined only if {@link ProtocolMessage#type} == {@link ProtocolMessageType#EPHEMERAL_SETTING} || @link ProtocolMessageType#EPHEMERAL_SYNC_RESPONSE}.
     */
    @ProtobufProperty(index = 5, type = UINT64)
    private long ephemeralSettingTimestamp;

    /**
     * History dataSync notification.
     * This property is defined only if {@link ProtocolMessage#type} == {@link ProtocolMessageType#HISTORY_SYNC_NOTIFICATION}.
     */
    @ProtobufProperty(index = 6, type = MESSAGE, concreteType = HistorySyncNotification.class)
    private HistorySyncNotification historySyncNotification;

    /**
     * The app state keys
     */
    @ProtobufProperty(index = 7, type = MESSAGE, concreteType = AppStateSyncKeyShare.class)
    private AppStateSyncKeyShare appStateSyncKeyShare;

    /**
     * An app state sync key
     */
    @ProtobufProperty(index = 8, type = MESSAGE, concreteType = AppStateSyncKeyRequest.class)
    private AppStateSyncKeyRequest appStateSyncKeyRequest;

    /**
     * Initial security settings sent by Whatsapp
     */
    @ProtobufProperty(index = 9, type = MESSAGE, concreteType = InitialSecurityNotificationSettingSync.class)
    private InitialSecurityNotificationSettingSync initialSecurityNotificationSettingSync;

    /**
     * App state exception notification
     */
    @ProtobufProperty(index = 10, type = MESSAGE, concreteType = AppStateFatalExceptionNotification.class)
    private AppStateFatalExceptionNotification appStateFatalExceptionNotification;

    /**
     * The constants of this enumerated type describe the various type of data that a {@link ProtocolMessage} can wrap
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum ProtocolMessageType {
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
        public static ProtocolMessageType forIndex(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(null);
        }
    }
}
