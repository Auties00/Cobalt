package it.auties.whatsapp.protobuf.info;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.protobuf.message.model.ContextualMessage;
import it.auties.whatsapp.protobuf.message.model.MessageContainer;
import it.auties.whatsapp.protobuf.message.model.MessageKey;
import it.auties.whatsapp.util.Unsupported;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * A model class that holds the information related to a {@link ContextualMessage}.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(builderMethodName = "newContextInfo", buildMethodName = "create")
@Accessors(fluent = true)
public class ContextInfo {
  /**
   * The id of the message that this ContextualMessage quotes
   */
  @JsonProperty(value = "1")
  @JsonPropertyDescription("string")
  private String quotedMessageId;

  /**
   * The jid of the contact that sent the message that this ContextualMessage quotes
   */
  @JsonProperty(value = "2")
  @JsonPropertyDescription("string")
  private ContactJid quotedMessageSenderId;

  /**
   * The message that this ContextualMessage quotes
   */
  @JsonProperty(value = "3")
  @JsonPropertyDescription("MessageContainer")
  private MessageContainer quotedMessageContainer;

  /**
   * A list of the contacts' jids mentioned in this ContextualMessage
   */
  @JsonProperty(value = "15")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  @JsonPropertyDescription("string")
  private List<String> mentionedJid;

  /**
   * Conversation source
   */
  @JsonProperty(value = "18")
  @JsonPropertyDescription("string")
  @Unsupported
  private String conversionSource;

  /**
   * Conversation data
   */
  @JsonProperty(value = "19")
  @JsonPropertyDescription("bytes")
  @Unsupported
  private byte[] conversionData;

  /**
   * Conversation delay in seconds
   */
  @JsonProperty(value = "20")
  @JsonPropertyDescription("uint32")
  @Unsupported
  private int conversionDelaySeconds;

  /**
   * Forwarding score
   */
  @JsonProperty(value = "21")
  @JsonPropertyDescription("uint32")
  private int forwardingScore;

  /**
   * Whether this ContextualMessage is forwarded
   */
  @JsonProperty(value = "22")
  @JsonPropertyDescription("bool")
  private boolean forwarded;

  /**
   * The ad that this ContextualMessage quotes
   */
  @JsonProperty(value = "23")
  @JsonPropertyDescription("AdReplyInfo")
  @Unsupported
  private AdReplyInfo quotedAd;

  /**
   * Placeholder key
   */
  @JsonProperty(value = "24")
  @JsonPropertyDescription("MessageKey")
  @Unsupported
  private MessageKey placeholderKey;

  /**
   * The expiration in seconds since {@link java.time.Instant#EPOCH} for this ContextualMessage.
   * Only valid if the chat where this message was sent is ephemeral.
   */
  @JsonProperty(value = "25")
  @JsonPropertyDescription("uint32")
  private int expiration;

  /**
   * The timestamp, that is the time in seconds since {@link java.time.Instant#EPOCH}, of the last modification to the ephemeral settings
   * for the chat where this ContextualMessage was sent.
   */
  @JsonProperty(value = "26")
  @JsonPropertyDescription("int64")
  private long ephemeralSettingTimestamp;

  /**
   * Ephemeral shared secret
   */
  @JsonProperty(value = "27")
  @JsonPropertyDescription("bytes")
  @Unsupported
  private byte[] ephemeralSharedSecret;

  /**
   * External ad reply
   */
  @JsonProperty(value = "28")
  @JsonPropertyDescription("ExternalAdReplyInfo")
  @Unsupported
  private ExternalAdReplyInfo externalAdReply;

  /**
   * Entry point conversion source
   */
  @JsonProperty(value = "29")
  @JsonPropertyDescription("string")
  @Unsupported
  private String entryPointConversionSource;

  /**
   * Entry point conversion app
   */
  @JsonProperty(value = "30")
  @JsonPropertyDescription("string")
  @Unsupported
  private String entryPointConversionApp;

  /**
   * Entry point conversion delay in seconds
   */
  @JsonProperty(value = "31")
  @JsonPropertyDescription("uint32")
  @Unsupported
  private int entryPointConversionDelaySeconds;

  /**
   * Constructs a ContextInfo from a quoted message
   *
   * @param quotedMessage the message to quote
   */
  public ContextInfo(@NonNull MessageInfo quotedMessage){
    this.quotedMessageContainer = quotedMessage.content();
    this.quotedMessageId = quotedMessage.key().id();
    this.quotedMessageSenderId = quotedMessage.senderId();
  }
}
