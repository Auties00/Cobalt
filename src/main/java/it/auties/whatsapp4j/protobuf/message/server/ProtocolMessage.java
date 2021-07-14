package it.auties.whatsapp4j.protobuf.message.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.protobuf.message.model.MessageKey;
import it.auties.whatsapp4j.protobuf.message.model.ServerMessage;
import it.auties.whatsapp4j.protobuf.model.HistorySyncNotification;
import it.auties.whatsapp4j.whatsapp.WhatsappAPI;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * A model class that represents a WhatsappMessage sent by a WhatsappWeb.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(builderMethodName = "newProtocolMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class ProtocolMessage implements ServerMessage {
  /**
   * History sync notification.
   * This property is defined only if {@link ProtocolMessage#type} == {@link ProtocolMessageType#HISTORY_SYNC_NOTIFICATION}.
   */
  @JsonProperty(value = "6")
  private HistorySyncNotification historySyncNotification;

  /**
   * The timestamp, that is the time in seconds since {@link java.time.Instant#EPOCH}, of the last modification to the ephemeral settings of a chat.
   * This property is defined only if {@link ProtocolMessage#type} == {@link ProtocolMessageType#EPHEMERAL_SETTING} || @link ProtocolMessageType#EPHEMERAL_SYNC_RESPONSE}.
   */
  @JsonProperty(value = "5")
  private long ephemeralSettingTimestamp;

  /**
   * The expiration, that is the time in seconds after which a message is automatically deleted, of messages in an ephemeral chat.
   * This property is defined only if {@link ProtocolMessage#type} == {@link ProtocolMessageType#EPHEMERAL_SETTING} || @link ProtocolMessageType#EPHEMERAL_SYNC_RESPONSE}.
   */
  @JsonProperty(value = "4")
  private int ephemeralExpiration;

  /**
   * The type of this server message
   */
  @JsonProperty(value = "2")
  private ProtocolMessageType type;

  /**
   * The key of message that this server message regards
   */
  @JsonProperty(value = "1")
  private MessageKey key;

  /**
   * The constants of this enumerated type describe the various type of data that a {@link ProtocolMessage} can wrap
   */
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
     * A {@link ProtocolMessage} that notifies that an history sync in any chat
     */
    HISTORY_SYNC_NOTIFICATION(5);

    private final @Getter int index;

    ProtocolMessageType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static ProtocolMessageType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
