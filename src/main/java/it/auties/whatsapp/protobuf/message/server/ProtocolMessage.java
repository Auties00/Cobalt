package it.auties.whatsapp.protobuf.message.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.message.model.MessageKey;
import it.auties.whatsapp.protobuf.message.model.ServerMessage;
import it.auties.whatsapp.protobuf.sync.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

/**
 * A model class that represents a WhatsappMessage sent by a WhatsappWeb.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder(builderMethodName = "newProtocolMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class ProtocolMessage implements ServerMessage {
  /**
   * The key of message that this server message regards
   */
  @JsonProperty("1")
  @JsonPropertyDescription("key")
  private MessageKey key;

  /**
   * The type of this server message
   */
  @JsonProperty("2")
  @JsonPropertyDescription("type")
  private ProtocolMessageType type;

  /**
   * The expiration, that is the endTimeStamp in seconds after which a message is automatically deleted, of messages in an ephemeral chat.
   * This property is defined only if {@link ProtocolMessage#type} == {@link ProtocolMessageType#EPHEMERAL_SETTING} || @link ProtocolMessageType#EPHEMERAL_SYNC_RESPONSE}.
   */
  @JsonProperty("4")
  @JsonPropertyDescription("uint64")
  private long ephemeralExpiration;

  /**
   * The timestamp, that is the endTimeStamp in seconds since {@link java.time.Instant#EPOCH}, of the last modification to the ephemeral settings of a chat.
   * This property is defined only if {@link ProtocolMessage#type} == {@link ProtocolMessageType#EPHEMERAL_SETTING} || @link ProtocolMessageType#EPHEMERAL_SYNC_RESPONSE}.
   */
  @JsonProperty("5")
  @JsonPropertyDescription("uint64")
  private long ephemeralSettingTimestamp;

  /**
   * History dataSync notification.
   * This property is defined only if {@link ProtocolMessage#type} == {@link ProtocolMessageType#HISTORY_SYNC_NOTIFICATION}.
   */
  @JsonProperty("6")
  @JsonPropertyDescription("historySyncNotification")
  private HistorySyncNotification historySyncNotification;

  /**
   * Unknown
   */
  @JsonProperty("7")
  @JsonPropertyDescription("appStateSyncKeyShare")
  private AppStateSyncKeyShare appStateSyncKeyShare;

  /**
   * Unknown
   */
  @JsonProperty("8")
  @JsonPropertyDescription("appStateSyncKeyRequest")
  private AppStateSyncKeyRequest appStateSyncKeyRequest;

  /**
   * Unknown
   */
  @JsonProperty("9")
  @JsonPropertyDescription("initialSecurityNotificationSettingSync")
  private InitialSecurityNotificationSettingSync initialSecurityNotificationSettingSync;

  /**
   * Unknown
   */
  @JsonProperty("10")
  @JsonPropertyDescription("appStateFatalExceptionNotification")
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
     * Message back fill request
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
