package it.auties.whatsapp.protobuf.message.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.message.model.MessageKey;
import it.auties.whatsapp.protobuf.message.model.ServerMessage;
import it.auties.whatsapp.protobuf.temp.*;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * A model class that represents a WhatsappMessage sent by a WhatsappWeb.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(builderMethodName = "newProtocolMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class ProtocolMessage implements ServerMessage {
  /**
   * The key of message that this server message regards
   */
  @JsonProperty(value = "1")
  private MessageKey key;

  /**
   * The type of this server message
   */
  @JsonProperty(value = "2")
  private ProtocolMessageType type;

  /**
   * The expiration, that is the time in seconds after which a message is automatically deleted, of messages in an ephemeral chat.
   * This property is defined only if {@link ProtocolMessage#type} == {@link ProtocolMessageType#EPHEMERAL_SETTING} || @link ProtocolMessageType#EPHEMERAL_SYNC_RESPONSE}.
   */
  @JsonProperty(value = "4")
  private long ephemeralExpiration;

  /**
   * The timestamp, that is the time in seconds since {@link java.time.Instant#EPOCH}, of the last modification to the ephemeral settings of a chat.
   * This property is defined only if {@link ProtocolMessage#type} == {@link ProtocolMessageType#EPHEMERAL_SETTING} || @link ProtocolMessageType#EPHEMERAL_SYNC_RESPONSE}.
   */
  @JsonProperty(value = "5")
  private long ephemeralSettingTimestamp;

  /**
   * History sync notification.
   * This property is defined only if {@link ProtocolMessage#type} == {@link ProtocolMessageType#HISTORY_SYNC_NOTIFICATION}.
   */
  @JsonProperty(value = "6")
  private HistorySyncNotification historySyncNotification;

  /**
   * Unknown
   */
  @JsonProperty(value = "7")
  private AppStateSyncKeyShare appStateSyncKeyShare;

  /**
   * Unknown
   */
  @JsonProperty(value = "8")
  private AppStateSyncKeyRequest appStateSyncKeyRequest;

  /**
   * Unknown
   */
  @JsonProperty(value = "9")
  private InitialSecurityNotificationSettingSync initialSecurityNotificationSettingSync;

  /**
   * Unknown
   */
  @JsonProperty(value = "10")
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
     * Message back fill request
     */
    MESSAGE_BACK_FILL_REQUEST(8),

    /**
     * Initial security notification setting sync
     */
    INITIAL_SECURITY_NOTIFICATION_SETTING_SYNC(9),

    /**
     * App state fatal exception notification
     */
    EXCEPTION_NOTIFICATION(10);

    private final @Getter int index;

    @JsonCreator
    public static ProtocolMessageType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
