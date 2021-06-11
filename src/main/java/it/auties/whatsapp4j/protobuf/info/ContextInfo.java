package it.auties.whatsapp4j.protobuf.info;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.protobuf.message.MessageContainer;
import it.auties.whatsapp4j.protobuf.message.MessageKey;
import it.auties.whatsapp4j.protobuf.message.ProtocolMessage;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * A model class that holds the information related to a {@link it.auties.whatsapp4j.protobuf.message.ContextualMessage}.
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
   * The jid of the chat where the message that this ContextualMessage quotes was sent
   */
  @JsonProperty(value = "4")
  private String quotedMessageChatJid;

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
  public ContextInfo(@NotNull MessageInfo quotedMessage){
    this.quotedMessageContainer = quotedMessage.container();
    this.quotedMessageId = quotedMessage.key().id();
    this.quotedMessageChatJid = quotedMessage.key().chatJid();
    this.quotedMessageSenderJid = quotedMessage.key().senderJid();
  }

  /**
   * Returns an optional {@link MessageInfo} representing the message quoted by this message if said message is in memory
   *
   * @return a non empty optional {@link MessageInfo} if this message quotes a message in memory
   */
  public Optional<MessageInfo> quotedMessage(){
    var manager = WhatsappDataManager.singletonInstance();
    return manager.findMessageById(manager.findChatByJid(quotedMessageChatJid).orElseThrow(), quotedMessageId);
  }
}
