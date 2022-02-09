package it.auties.whatsapp.protobuf.info;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.protobuf.message.model.ContextualMessage;
import it.auties.whatsapp.protobuf.message.model.MessageContainer;
import it.auties.whatsapp.protobuf.message.model.MessageKey;
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
public non-sealed class ContextInfo implements WhatsappInfo {
  /**
   * The jid of the message that this ContextualMessage quotes
   */
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String quotedMessageId;

  /**
   * The jid of the contact that sent the message that this ContextualMessage quotes
   */
  @JsonProperty("2")
  @JsonPropertyDescription("string")
  private ContactJid quotedMessageSenderId;

  /**
   * The message that this ContextualMessage quotes
   */
  @JsonProperty("3")
  @JsonPropertyDescription("MessageContainer")
  private MessageContainer quotedMessageContainer;

  /**
   * A list of the contacts' jids mentioned in this ContextualMessage
   */
  @JsonProperty("15")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  @JsonPropertyDescription("string")
  private List<String> mentionedJid;

  /**
   * Conversation source
   */
  @JsonProperty("18")
  @JsonPropertyDescription("string")
  private String conversionSource;

  /**
   * Conversation data
   */
  @JsonProperty("19")
  @JsonPropertyDescription("bytes")
  private byte[] conversionData;

  /**
   * Conversation delay in seconds
   */
  @JsonProperty("20")
  @JsonPropertyDescription("uint32")
  private int conversionDelaySeconds;

  /**
   * Forwarding score
   */
  @JsonProperty("21")
  @JsonPropertyDescription("uint32")
  private int forwardingScore;

  /**
   * Whether this ContextualMessage is forwarded
   */
  @JsonProperty("22")
  @JsonPropertyDescription("bool")
  private boolean forwarded;

  /**
   * The ad that this ContextualMessage quotes
   */
  @JsonProperty("23")
  @JsonPropertyDescription("AdReplyInfo")
  private AdReplyInfo quotedAd;

  /**
   * Placeholder key
   */
  @JsonProperty("24")
  @JsonPropertyDescription("MessageKey")
  private MessageKey placeholderKey;

  /**
   * The expiration in seconds since {@link java.time.Instant#EPOCH} for this ContextualMessage.
   * Only valid if the chat where this message was sent is ephemeral.
   */
  @JsonProperty("25")
  @JsonPropertyDescription("uint32")
  private int expiration;

  /**
   * The timestamp, that is the endTimeStamp in seconds since {@link java.time.Instant#EPOCH}, of the last modification to the ephemeral settings
   * for the chat where this ContextualMessage was sent.
   */
  @JsonProperty("26")
  @JsonPropertyDescription("int64")
  private long ephemeralSettingTimestamp;

  /**
   * Ephemeral shared secret
   */
  @JsonProperty("27")
  @JsonPropertyDescription("bytes")
  private byte[] ephemeralSharedSecret;

  /**
   * External ad reply
   */
  @JsonProperty("28")
  @JsonPropertyDescription("ExternalAdReplyInfo")
  private ExternalAdReplyInfo externalAdReply;

  /**
   * Entry point conversion source
   */
  @JsonProperty("29")
  @JsonPropertyDescription("string")
  private String entryPointConversionSource;

  /**
   * Entry point conversion app
   */
  @JsonProperty("30")
  @JsonPropertyDescription("string")
  private String entryPointConversionApp;

  /**
   * Entry point conversion delay in seconds
   */
  @JsonProperty("31")
  @JsonPropertyDescription("uint32")
  private int entryPointConversionDelaySeconds;

  /**
   * Constructs a ContextInfo from a quoted message
   *
   * @param quotedMessage the message to quote
   */
  public ContextInfo(@NonNull MessageInfo quotedMessage){
    this.quotedMessageContainer = quotedMessage.message();
    this.quotedMessageId = quotedMessage.key().id();
    this.quotedMessageSenderId = quotedMessage.senderJid();
  }
}
