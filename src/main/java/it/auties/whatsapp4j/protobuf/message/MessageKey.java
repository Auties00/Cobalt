package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.protobuf.chat.Chat;
import it.auties.whatsapp4j.protobuf.contact.Contact;
import it.auties.whatsapp4j.utils.WhatsappUtils;
import it.auties.whatsapp4j.utils.internal.Validate;
import jakarta.validation.Validation;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * A container for unique identifiers and metadata linked to a {@link Message} and contained in {@link it.auties.whatsapp4j.protobuf.info.MessageInfo}.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(builderMethodName = "newMessageKey", buildMethodName = "create")
@Accessors(fluent = true)
public class MessageKey {
  /**
   * The id of the message
   */
  @JsonProperty(value = "3")
  @Builder.Default
  private String id = WhatsappUtils.randomId();

  /**
   * The jid of the contact or group that sent the message.
   */
  @JsonProperty(value = "1")
  private String chatJid;

  /**
   * The jid of the participant that sent the message in a group.
   * This property is only populated if {@link MessageKey#chatJid} refers to a group.
   */
  @JsonProperty(value = "4")
  private String senderJid;

  /**
   * Determines whether the message was sent by you or by someone else
   */
  @JsonProperty(value = "2")
  private boolean fromMe;

  public MessageKey(Chat chat){
    this(chat, false);
  }

  //TODO: Done for today
  public MessageKey(Chat chat, boolean fromMe){
    this(WhatsappUtils.randomId(), chat.jid(), null, fromMe);
  }

  public MessageKey(Chat chat, Contact contact){
    this(chat, contact, false);
  }

  public MessageKey(Chat chat, Contact contact, boolean fromMe){
    this(chat, contact.jid(), fromMe);
  }

  public MessageKey(Chat chat, String contactJid){
    this(chat, contactJid, false);
  }

  public MessageKey(Chat chat, String contactJid, boolean fromMe){
    this(WhatsappUtils.randomId(), chat.jid(), contactJid, fromMe);
  }

  /**
   * Returns the chat where the message was sent
   *
   * @return an optional wrapping a {@link Chat}
   */
  public Optional<Chat> chat(){
    return WhatsappDataManager.singletonInstance().findChatByJid(chatJid);
  }

  /**
   * Returns the contact that sent the message
   *
   * @return an optional wrapping a {@link Contact}
   */
  public Optional<Contact> sender(){
    return WhatsappDataManager.singletonInstance().findContactByJid(Optional.ofNullable(senderJid).orElse(chatJid));
  }
}
