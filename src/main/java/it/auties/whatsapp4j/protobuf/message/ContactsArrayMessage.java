package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a list of contacts inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Accessors(fluent = true)
public final class ContactsArrayMessage extends ContextualMessage {
  /**
   * A list of {@link ContactMessage} that this message wraps
   */
  @JsonProperty(value = "2")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<ContactMessage> contacts;

  /**
   * The name of the contact the first contact that this message wraps
   */
  @JsonProperty(value = "1")
  private String displayName;

  /**
   * Constructs a new builder to create a ContactsArrayMessage.
   * The result can be later sent using {@link WhatsappAPI#sendMessage(it.auties.whatsapp4j.protobuf.info.MessageInfo)}
   *
   * @param displayName the display name of the first contact that the new message wraps
   * @param contacts    the list of contacts that the new message wraps
   * @param contextInfo the context info that the new message wraps
   *
   * @return a non null new message
   */
  @Builder(builderClassName = "NewContactsArrayMessageBuilder", builderMethodName = "newContactMessage", buildMethodName = "create")
  public ContactsArrayMessage newContactMessage(String displayName, List<ContactMessage> contacts, ContextInfo contextInfo) {
    return ContactsArrayMessage.builder()
            .contacts(contacts)
            .displayName(displayName)
            .contextInfo(contextInfo)
            .build();
  }
}
