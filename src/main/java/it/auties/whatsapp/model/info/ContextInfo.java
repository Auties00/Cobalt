package it.auties.whatsapp.model.info;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.chat.ChatDisappear;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.message.model.ActionLink;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.message.payment.PaymentOrderMessage;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

/**
 * A model class that holds the information related to a {@link ContextualMessage}.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder(builderMethodName = "newContextInfo", buildMethodName = "create")
@Accessors(fluent = true)
public sealed class ContextInfo implements Info permits PaymentOrderMessage {
  /**
   * The jid of the message that this ContextualMessage quotes
   */
  @ProtobufProperty(index = 1, type = STRING)
  private String quotedMessageId;

  /**
   * The jid of the contact that sent the message that this ContextualMessage quotes
   */
  @ProtobufProperty(index = 2, type = STRING,
          concreteType = ContactJid.class, requiresConversion = true)
  private ContactJid quotedMessageSenderId;

  /**
   * The message that this ContextualMessage quotes
   */
  @ProtobufProperty(index = 3, type = MESSAGE, concreteType = MessageContainer.class)
  private MessageContainer quotedMessageContainer;

  /**
   * A list of the contacts' jids mentioned in this ContextualMessage
   */
  @ProtobufProperty(index = 15, type = STRING, repeated = true)
  private List<String> mentionedJids;

  /**
   * Conversation source
   */
  @ProtobufProperty(index = 18, type = STRING)
  private String conversionSource;

  /**
   * Conversation data
   */
  @ProtobufProperty(index = 19, type = BYTES)
  private byte[] conversionData;

  /**
   * Conversation delay in seconds
   */
  @ProtobufProperty(index = 20, type = UINT32)
  private Integer conversionDelaySeconds;

  /**
   * Forwarding score
   */
  @ProtobufProperty(index = 21, type = UINT32)
  private Integer forwardingScore;

  /**
   * Whether this ContextualMessage is forwarded
   */
  @ProtobufProperty(index = 22, type = BOOLEAN)
  private boolean forwarded;

  /**
   * The ad that this ContextualMessage quotes
   */
  @ProtobufProperty(index = 23, type = MESSAGE, concreteType = AdReplyInfo.class)
  private AdReplyInfo quotedAd;

  /**
   * Placeholder key
   */
  @ProtobufProperty(index = 24, type = MESSAGE, concreteType = MessageKey.class)
  private MessageKey placeholderKey;

  /**
   * The expiration in seconds since {@link java.time.Instant#EPOCH} for this ContextualMessage.
   * Only valid if the chat where this message was sent is ephemeral.
   */
  @ProtobufProperty(index = 25, type = UINT32)
  private Integer expiration;

  /**
   * The timestamp, that is the endTimeStamp in seconds since {@link java.time.Instant#EPOCH}, of the last modification to the ephemeral settings
   * for the chat where this ContextualMessage was sent.
   */
  @ProtobufProperty(index = 26, type = INT64)
  private Long ephemeralSettingTimestamp;

  /**
   * Ephemeral shared secret
   */
  @ProtobufProperty(index = 27, type = BYTES)
  private byte[] ephemeralSharedSecret;

  /**
   * External ad reply
   */
  @ProtobufProperty(index = 28, type = MESSAGE, concreteType = ExternalAdReplyInfo.class)
  private ExternalAdReplyInfo externalAdReply;

  /**
   * Entry point conversion source
   */
  @ProtobufProperty(index = 29, type = STRING)
  private String entryPointConversionSource;

  /**
   * Entry point conversion app
   */
  @ProtobufProperty(index = 30, type = STRING)
  private String entryPointConversionApp;

  /**
   * Entry point conversion delay in seconds
   */
  @ProtobufProperty(index = 31, type = UINT32)
  private Integer entryPointConversionDelaySeconds;

  /*
  FIXME: 02/06/2022 Bugged?
  @ProtobufProperty(index = 32, type = MESSAGE, concreteType = ChatDisappear.Linker.class)
  private ChatDisappear disappearingMode;
  @ProtobufProperty(index = 33, type = STRING)
  private ActionLink actionLink;
  @ProtobufProperty(index = 34, type = STRING)
  private String groupSubject;
  @ProtobufProperty(index = 35, type = STRING)
  private String parentGroupJid;
   */

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
