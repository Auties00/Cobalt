package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.protobuf.chat.Chat;
import it.auties.whatsapp4j.protobuf.contact.Contact;
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
@Builder
@Accessors(fluent = true)
public class MessageKey {
  /**
   * The id of the message
   */
  @JsonProperty(value = "3")
  private String id;

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

  /**
   * Returns the chat where the message was sent
   *
   * @return an optional wrapping a {@link Chat}
   */
  public Optional<Chat> chat(){
    return WhatsappDataManager.singletonInstance().findChatByJid(senderJid);
  }

  /**
   * Returns the contact that sent the message
   *
   * @return an optional wrapping a {@link Contact}
   */
  public Optional<Contact> sender(){
    return WhatsappDataManager.singletonInstance().findContactByJid(senderJid);
  }
}
