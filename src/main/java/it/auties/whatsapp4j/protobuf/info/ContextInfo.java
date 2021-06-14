package it.auties.whatsapp4j.protobuf.info;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.whatsapp.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.message.model.ContextualMessage;
import it.auties.whatsapp4j.protobuf.message.model.MessageContainer;
import it.auties.whatsapp4j.protobuf.message.model.MessageKey;
import lombok.NonNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * A model class that holds the information related to a {@link ContextualMessage}.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(builderMethodName = "newContextInfo", buildMethodName = "create")
@Accessors(fluent = true)
public class ContextInfo {
  /**
   * The timestamp, that is the time in seconds since {@link java.time.Instant#EPOCH}, of the last modification to the ephemeral settings
   * for the chat where this ContextualMessage was sent.
   */
  @JsonProperty(value = "26")
  private long ephemeralSettingTimestamp;

  /**
   * The expiration in seconds since {@link java.time.Instant#EPOCH} for this ContextualMessage.
   * Only valid if the chat where this message was sent is ephemeral.
   */
  @JsonProperty(value = "25")
  private int expiration;

  /**
   * Placeholder key
   */
  @JsonProperty(value = "24")
  private MessageKey placeholderKey;

  /**
   * The ad that this ContextualMessage quotes
   */
  @JsonProperty(value = "23")
  private AdReplyInfo quotedAd;

  /**
   * Whether this ContextualMessage is forwarded
   */
  @JsonProperty(value = "22")
  private boolean isForwarded;

  /**
   * Forwarding score
   */
  @JsonProperty(value = "21")
  private int forwardingScore;

  /**
   * Conversation delay in seconds
   */
  @JsonProperty(value = "20")
  private int conversionDelaySeconds;

  /**
   * Conversation data
   */
  @JsonProperty(value = "19")
  private byte[] conversionData;

  /**
   * Conversation source
   */
  @JsonProperty(value = "18")
  private String conversionSource;

  /**
   * A list of the contacts' jids mentioned in this ContextualMessage
   */
  @JsonProperty(value = "15")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> mentionedJid;

  /**
   * The message that this ContextualMessage quotes
   */
  @JsonProperty(value = "3")
  private MessageContainer quotedMessageContainer;

  /**
   * The jid of the contact that sent the message that this ContextualMessage quotes
   */
  @JsonProperty(value = "2")
  private String quotedMessageSenderJid;

  /**
   * The id of the message that this ContextualMessage quotes
   */
  @JsonProperty(value = "1")
  private String quotedMessageId;

  /**
   * Constructs a ContextInfo from a quoted message
   *
   * @param quotedMessage the message to quote
   */
  public ContextInfo(@NonNull MessageInfo quotedMessage){
    this.quotedMessageContainer = quotedMessage.container();
    this.quotedMessageId = quotedMessage.key().id();
    this.quotedMessageSenderJid = quotedMessage.senderJid();
  }
}
